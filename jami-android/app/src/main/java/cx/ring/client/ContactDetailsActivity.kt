/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring.client

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import cx.ring.R
import cx.ring.account.RenameSwarmDialog
import cx.ring.application.JamiApplication
import cx.ring.databinding.ActivityContactDetailsBinding
import cx.ring.databinding.DialogProfileBinding
import cx.ring.databinding.ItemContactHorizontalBinding
import cx.ring.fragments.CallFragment
import cx.ring.fragments.ContactPickerFragment
import cx.ring.fragments.ConversationActionsFragment
import cx.ring.fragments.ConversationGalleryFragment
import cx.ring.fragments.ConversationMembersFragment
import cx.ring.interfaces.Colorable
import cx.ring.services.SharedPreferencesServiceImpl.Companion.getConversationColor
import cx.ring.utils.*
import cx.ring.views.AvatarDrawable
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import net.jami.model.Call
import net.jami.model.Contact
import net.jami.model.Conversation
import net.jami.model.Uri
import net.jami.services.AccountService
import net.jami.services.ContactService
import net.jami.services.ConversationFacade
import net.jami.services.DeviceRuntimeService
import net.jami.services.HardwareService
import net.jami.services.NotificationService
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@AndroidEntryPoint
class ContactDetailsActivity : AppCompatActivity(), TabLayout.OnTabSelectedListener,
    ContactPickerFragment.OnContactedPicked, RenameSwarmDialog.RenameSwarmListener, Colorable {
    @Inject
    @Singleton lateinit
    var mConversationFacade: ConversationFacade

    @Inject lateinit
    var mContactService: ContactService

    @Inject
    @Singleton lateinit
    var mAccountService: AccountService

    @Inject
    @Singleton lateinit
    var mDeviceRuntimeService: DeviceRuntimeService

    @Inject lateinit
    var hardwareService: HardwareService

    private var binding: ActivityContactDetailsBinding? = null
    private var path: ConversationPath? = null
    private var mProfilePhoto: ImageView? = null
    private var mSourcePhoto: Bitmap? = null
    private var tmpProfilePhotoUri: android.net.Uri? = null

    private val cameraResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            tmpProfilePhotoUri.let { photoUri ->
                    if (photoUri == null) {
                        if (data != null)
                            updatePhoto(Single.just(data.extras!!["data"] as Bitmap))
                    } else {
                        updatePhoto(photoUri)
                    }
                }
            tmpProfilePhotoUri = null
        }
    }

    private val galleryResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            updatePhoto(data!!.data!!)
        }
    }

    internal class ContactView(val binding: ItemContactHorizontalBinding, parentDisposable: CompositeDisposable)
        : RecyclerView.ViewHolder(binding.root) {
        var callback: (() -> Unit)? = null
        val disposable = CompositeDisposable()

        init {
            parentDisposable.add(disposable)
            itemView.setOnClickListener {
                try {
                    callback?.invoke()
                } catch (e: Exception) {
                    Log.w(TAG, "Error performing action", e)
                }
            }
        }
    }

    private val mDisposableBag = CompositeDisposable()
    private var mPagerAdapter: ScreenSlidePagerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        path = ConversationPath.fromIntent(intent)
        if (path == null) {
            finish()
            return
        }
        JamiApplication.instance?.startDaemon(this)
        val conversation = try {
            mConversationFacade
                .startConversation(path!!.accountId, path!!.conversationUri)
                .blockingGet()
        } catch (e: Throwable) {
            finish()
            return
        }

        val binding = ActivityContactDetailsBinding.inflate(layoutInflater).also { this.binding = it }
        setContentView(binding.root)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        mDisposableBag.add(mConversationFacade.observeConversation(conversation)
            .observeOn(DeviceUtils.uiScheduler)
            .doOnComplete { finish() }
            .subscribe({ vm ->
                binding.contactImage.setImageDrawable(AvatarDrawable.Builder()
                    .withViewModel(vm)
                    .withCircleCrop(true)
                    .build(this))
                binding.title.text = vm.title
                if (vm.conversationProfile.description.isNullOrBlank())
                    binding.description.text = getString(R.string.swarm_description)
                else
                    binding.description.text = vm.conversationProfile.description
                if (vm.mode == Conversation.Mode.OneToOne) {
                    binding.title.setOnClickListener(null)
                    binding.description.setOnClickListener(null)
                    binding.contactImage.setOnClickListener(null)
                    binding.tabLayout.removeTabAt(TAB_MEMBER)
                } else {
                    binding.addMember.isVisible = true
                    binding.description.isVisible = true
                    binding.contactImage.setOnClickListener { profileImageClicked() }
                    binding.title.setOnClickListener {
                        val title = getString(R.string.dialogtitle_title)
                        val hint = getString(R.string.dialog_hint_title)
                        RenameSwarmDialog().apply {
                            arguments = Bundle().apply {
                                putString(RenameSwarmDialog.KEY, RenameSwarmDialog.KEY_TITLE)
                            }
                            setTitle(title)
                            setHint(hint)
                            setText(vm.conversationProfile.displayName)
                            setListener(this@ContactDetailsActivity)
                        }.show(supportFragmentManager, TAG)
                    }
                    binding.description.setOnClickListener {
                        val title = getString(R.string.dialogtitle_description)
                        val hint = getString(R.string.dialog_hint_description)
                        RenameSwarmDialog().apply {
                            arguments = Bundle().apply {
                                putString(RenameSwarmDialog.KEY, RenameSwarmDialog.KEY_DESCRIPTION)
                            }
                            setTitle(title)
                            setHint(hint)
                            setText(vm.conversationProfile.description)
                            setListener(this@ContactDetailsActivity)
                        }.show(supportFragmentManager, TAG)
                    }
                }
            }) { e ->
                Log.e(TAG, "e", e)
                finish()
            })

        binding.tabLayout.addOnTabSelectedListener(this)
        binding.back.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.addMember.setOnClickListener { ContactPickerFragment().show(supportFragmentManager, ContactPickerFragment.TAG) }

        mPagerAdapter = ScreenSlidePagerAdapter(this, conversation)
        binding.pager.adapter = mPagerAdapter

        // Update color on RX color signal.
        mDisposableBag.add(
            conversation
                .getColor()
                .observeOn(DeviceUtils.uiScheduler)
                .subscribe { setColor(getConversationColor(this, it)) }
        )

        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.tabLayout.getTabAt(position)!!.select()
            }
        })
    }

    /**
     * Set the color of the activity (appBar and addMember button).
     */
    override fun setColor(color: Int) {
        binding?.appBar?.backgroundTintList = ColorStateList.valueOf(color)
        binding?.addMember?.backgroundTintList = ColorStateList.valueOf(color)
    }

    private fun profileImageClicked() {
        val inflater = LayoutInflater.from(this)
        val view = DialogProfileBinding.inflate(inflater).apply {
            camera.setOnClickListener {
                if (mDeviceRuntimeService.hasVideoPermission())
                    gotToImageCapture()
                else
                    askCameraPermission()
            }
            gallery.setOnClickListener {
                if (mDeviceRuntimeService.hasGalleryPermission())
                    goToGallery()
                else
                    askGalleryPermission()
            }
        }
        mProfilePhoto = view.profilePhoto
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.profile)
            .setView(view.root)
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }
            .setPositiveButton(android.R.string.ok) { dialog, which ->
                mSourcePhoto?.let { source ->
                    val os = ByteArrayOutputStream()
                    BitmapUtils.createScaledBitmap(source, 512)
                        .compress(Bitmap.CompressFormat.JPEG, 90, os)
                    val map: MutableMap<String, String> = HashMap()
                    map["avatar"] = Base64.encodeToString(os.toByteArray(), Base64.NO_WRAP)
                    mAccountService.updateConversationInfo(path!!.accountId, path!!.conversationUri.host, map)
                }
            }
            .setOnDismissListener {
                mProfilePhoto = null
                mSourcePhoto = null
            }
            .show()
    }

    private fun gotToImageCapture() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            val file = AndroidFileUtils.createImageFile(this)
            val uri = FileProvider.getUriForFile(this, ContentUriHandler.AUTHORITY_FILES, file)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                .putExtra("android.intent.extras.CAMERA_FACING", 1)
                .putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
                .putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
            tmpProfilePhotoUri = uri
            cameraResultLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Error starting camera: " + e.localizedMessage, Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Can't create temp file", e)
        }
    }

    private fun askCameraPermission() {
        requestPermissions(arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ), HomeActivity.REQUEST_PERMISSION_CAMERA)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            HomeActivity.REQUEST_PERMISSION_CAMERA -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                hardwareService.initVideo()
                    .onErrorComplete()
                    .subscribe()
                gotToImageCapture()
            }
            HomeActivity.REQUEST_PERMISSION_READ_STORAGE -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                goToGallery()
            }
        }
    }

    private fun goToGallery() {
        try {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            galleryResultLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, R.string.gallery_error_message, Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun askGalleryPermission() {
        requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_PERMISSION_READ_STORAGE)
    }

    private fun updatePhoto(uriImage: android.net.Uri) {
        updatePhoto(AndroidFileUtils.loadBitmap(this, uriImage))
    }

    private fun updatePhoto(image: Single<Bitmap>) {
        mDisposableBag.add(image.subscribeOn(Schedulers.io())
            .map { img ->
                mSourcePhoto = img
                AvatarDrawable.Builder()
                    .withPhoto(img)
                    .withCircleCrop(true)
                    .build(this)
            }
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe({ avatar: AvatarDrawable ->
                mProfilePhoto?.setImageDrawable(avatar)
            }) { e: Throwable ->
                Log.e(TAG, "Error loading image", e)
            })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finishAfterTransition()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        mDisposableBag.dispose()
        super.onDestroy()
        binding = null
    }

    fun goToCallActivity(conversation: Conversation, contactUri: Uri, hasVideo: Boolean) {
        val conf = conversation.currentCall
        if (conf != null && conf.participants.isNotEmpty()
            && conf.participants[0].callStatus != Call.CallStatus.INACTIVE
            && conf.participants[0].callStatus != Call.CallStatus.FAILURE) {
            startActivity(Intent(Intent.ACTION_VIEW)
                .setClass(applicationContext, CallActivity::class.java)
                .putExtra(NotificationService.KEY_CALL_ID, conf.id))
        } else {
            val intent = Intent(Intent.ACTION_CALL)
                .setClass(applicationContext, CallActivity::class.java)
                .putExtras(ConversationPath.toBundle(conversation))
                .putExtra(Intent.EXTRA_PHONE_NUMBER, contactUri.uri)
                .putExtra(CallFragment.KEY_HAS_VIDEO, hasVideo)
            startActivityForResult(intent, REQUEST_CODE_CALL)
        }
    }

    fun goToConversationActivity(accountId: String, conversationUri: Uri) {
        startActivity(Intent(Intent.ACTION_VIEW,
            ConversationPath.toUri(accountId, conversationUri),
            applicationContext,
            ConversationActivity::class.java
        ))
    }

    override fun onTabSelected(tab: TabLayout.Tab?) {
        binding?.pager?.setCurrentItem(tab!!.position, true)
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) {}

    override fun onTabReselected(tab: TabLayout.Tab?) {}

    override fun onContactPicked(accountId: String, contacts: Set<Contact>) {
        mAccountService.addConversationMembers(accountId, path!!.conversationUri.host, contacts.map { contact-> contact.uri.toString() })
    }

    override fun onSwarmRename(key:String, newName: String) {
        val map: MutableMap<String, String> = HashMap()
        if (key == RenameSwarmDialog.KEY_TITLE) {
            map["title"] = newName
        } else if (key == RenameSwarmDialog.KEY_DESCRIPTION) {
            map["description"] = newName
        }
        mAccountService.updateConversationInfo(path!!.accountId, path!!.conversationUri.host, map)
    }

    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity, conversation: Conversation) : FragmentStateAdapter(fa) {

        val accountId = conversation.accountId
        val conversationId = conversation.uri

        val fragments = if (conversation.mode.blockingFirst() != Conversation.Mode.OneToOne) listOf(
            ConversationActionsFragment.newInstance(accountId, conversationId),
            ConversationMembersFragment.newInstance(accountId, conversationId),
            ConversationGalleryFragment.newInstance(accountId, conversationId)
            )
        else listOf(
            ConversationActionsFragment.newInstance(accountId, conversationId),
            ConversationGalleryFragment.newInstance(accountId, conversationId)
            )

        override fun getItemCount(): Int = fragments.size

        override fun createFragment(position: Int): Fragment = fragments[position]
    }

    companion object {
        private val TAG = ContactDetailsActivity::class.simpleName!!
        const val TAB_ABOUT = 0
        const val TAB_MEMBER = 1
        const val TAB_DOCUMENT = 2
        const val REQUEST_CODE_CALL = 3
        const val REQUEST_PERMISSION_READ_STORAGE = 114
    }
}
