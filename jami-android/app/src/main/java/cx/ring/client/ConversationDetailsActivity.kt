/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
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
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.activity.result.PickVisualMediaRequest
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import cx.ring.R
import cx.ring.application.JamiApplication
import cx.ring.databinding.ActivityConversationDetailsBinding
import cx.ring.databinding.DialogProfileBinding
import cx.ring.databinding.DialogSwarmTitleBinding
import cx.ring.fragments.CallFragment
import cx.ring.fragments.ContactPickerFragment
import cx.ring.fragments.ConversationActionsFragment
import cx.ring.fragments.ConversationGalleryFragment
import cx.ring.fragments.ConversationMembersFragment
import cx.ring.utils.*
import cx.ring.views.AvatarDrawable
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import net.jami.model.Call
import net.jami.model.Contact
import net.jami.model.Conversation
import net.jami.model.Profile
import net.jami.model.Uri
import net.jami.services.AccountService
import net.jami.services.ContactService
import net.jami.services.ConversationFacade
import net.jami.services.DeviceRuntimeService
import net.jami.services.HardwareService
import net.jami.services.NotificationService
import net.jami.utils.VCardUtils
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@AndroidEntryPoint
class ConversationDetailsActivity : AppCompatActivity(), ContactPickerFragment.OnContactedPicked {

    @Inject
    @Singleton lateinit
    var mConversationFacade: ConversationFacade

    @Inject
    lateinit var mContactService: ContactService

    @Inject
    @Singleton
    lateinit var mAccountService: AccountService

    @Inject
    @Singleton
    lateinit var mDeviceRuntimeService: DeviceRuntimeService

    @Inject
    lateinit var hardwareService: HardwareService

    private var binding: ActivityConversationDetailsBinding? = null
    private var path: ConversationPath? = null
    private var mProfilePhoto: ImageView? = null
    private var mSourcePhoto: Bitmap? = null
    private var tmpProfilePhotoUri: android.net.Uri? = null
    private var name = ""

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

    private val galleryResultLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null)
            updatePhoto(uri)
    }

    private val mDisposableBag = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        path = ConversationPath.fromIntent(intent)
        if (path == null) {
            finish()
            return
        }
        JamiApplication.instance?.startDaemon(this)
        val conversation = try {
            mConversationFacade.startConversation(path!!.accountId, path!!.conversationUri).blockingGet()
        } catch (e: Throwable) {
            finish()
            return
        }

        val binding = ActivityConversationDetailsBinding.inflate(layoutInflater).apply {
            binding = this
            setContentView(root)
        }

        var isShow = true
        var scrollRange = -1
        binding.appBar.addOnOffsetChangedListener { barLayout, verticalOffset ->
            if (scrollRange == -1) {
                scrollRange = barLayout?.totalScrollRange!!
            }
            if (scrollRange + verticalOffset == 0) {
                // Todo: will not be dynamically updated if name changes
                binding.collapsingToolbar.title = name
                isShow = true
            } else if (isShow) {
                binding.collapsingToolbar.title =
                    " " //careful there should a space between double quote otherwise it wont work
                isShow = false
            }
        }

        mDisposableBag.add(mConversationFacade.observeConversation(conversation)
            .observeOn(DeviceUtils.uiScheduler)
            .doOnComplete { finish() }
            .subscribe({ vm ->
                binding.conversationAvatar.setImageDrawable(AvatarDrawable.Builder()
                    .withViewModel(vm)
                    .withCircleCrop(true)
                    .build(this))
                name = vm.title
                binding.conversationTitle.text = vm.title

                // Note: For a random account from search results, mode will be Legacy
                if (vm.request != null){
                    binding.btnPanel.isVisible = false
                    binding.conversationTitle.setOnClickListener(null)
                    binding.conversationAvatar.setOnClickListener(null)
                } else if (vm.mode == Conversation.Mode.OneToOne
                    || vm.mode == Conversation.Mode.Legacy
                ) {
                    if(conversation.contact!!.isBlocked || conversation.isLegacy()) {
                        binding.btnPanel.isVisible = false
                    } else {
                        binding.conversationTitle.setOnClickListener {
                            val dialogBinding = DialogSwarmTitleBinding
                                    .inflate(LayoutInflater.from(this)).apply {
                                titleTxt.setText(vm.conversationProfile.displayName)
                                titleTxtBox.hint = getString(R.string.dialog_hint_title)
                            }
                            MaterialAlertDialogBuilder(this)
                                .setView(dialogBinding.root)
                                .setTitle(getString(R.string.dialog_title_contact))
                                .setPositiveButton(R.string.rename_btn) { d, _ ->
                                    val newName = dialogBinding.titleTxt.text.toString().trim()
                                    if (newName.isNotEmpty()) {
                                        conversation.contact?.let { contact ->
                                            val id = Base64.encodeToString(contact.primaryNumber
                                                .toByteArray(), Base64.NO_WRAP)
                                            VCardUtils.saveToCustomProfiles(newName, null,
                                                path!!.accountId, id, applicationContext.filesDir)
                                            updateCustomProfile(contact, path!!.accountId, id)
                                        }
                                    }
                                    d.dismiss()
                                }
                                .setNegativeButton(android.R.string.cancel, null)
                                .setNeutralButton(R.string.reset) { d, _ ->
                                    conversation.contact?.let { contact ->
                                        val id = Base64.encodeToString(contact.primaryNumber
                                            .toByteArray(), Base64.NO_WRAP)
                                        VCardUtils.resetCustomProfileName(path!!.accountId, id,
                                            applicationContext.filesDir)
                                        updateCustomProfile(contact, path!!.accountId, id)
                                    }
                                    d.dismiss()
                                }
                                .show()
                        }

                        binding.conversationAvatar.setOnClickListener {
                            conversation.contact?.let { contact ->
                                profileImageClicked(contact, false)
                            }
                        }
                    }

                    binding.addMember.isVisible = false
                    val callUri = conversation.contact!!.uri
                    binding.audioCall.setOnClickListener { goToCallActivity(conversation, callUri, false) }
                    binding.videoCall.setOnClickListener { goToCallActivity(conversation, callUri, true) }
                } else if(vm.isGroup() and !conversation.isUserGroupAdmin()) {
                    // Block conversation edition for non-admin users.
                    binding.addMember.isVisible = true
                    fun showNotAdminToast() =
                        Toast.makeText(this, R.string.not_admin_toast, Toast.LENGTH_SHORT).show()
                    binding.conversationTitle.setOnClickListener { showNotAdminToast() }
                    binding.conversationAvatar.setOnClickListener { showNotAdminToast() }
                } else {
                    binding.audioCall.setOnClickListener { goToCallActivity(conversation, conversation.uri, false) }
                    binding.videoCall.setOnClickListener { goToCallActivity(conversation, conversation.uri, true) }

                    binding.conversationAvatar.setOnClickListener {
                        conversation.contact?.let { contact ->
                            profileImageClicked(contact, true)
                        }
                    }
                    binding.conversationTitle.setOnClickListener {
                        val dialogBinding = DialogSwarmTitleBinding.inflate(LayoutInflater.from(this)).apply {
                            titleTxt.setText(vm.conversationProfile.displayName)
                            titleTxtBox.hint = getString(R.string.dialog_hint_title)
                        }
                        MaterialAlertDialogBuilder(this)
                            .setView(dialogBinding.root)
                            .setTitle(getString(R.string.dialogtitle_title))
                            .setPositiveButton(R.string.rename_btn) { d: DialogInterface, i: Int ->
                                val input = dialogBinding.titleTxt.text.toString().trim { it <= ' ' }
                                mAccountService.updateConversationInfo(
                                    path!!.accountId, path!!.conversationUri.host, mapOf("title" to input)
                                )
                                d.dismiss()
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .show()
                    }
                }

            }) { e ->
                Log.e(TAG, "e", e)
                finish()
            })

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                binding.pager.setCurrentItem(tab!!.position, true)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

//        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
//            override fun onPageSelected(position: Int) {
//                super.onPageSelected(position)
//                binding.tabLayout.getTabAt(position)!!.select()
//            }
//        })

        val tabAdapter = ScreenSlidePagerAdapter(this, conversation)
        binding.pager.adapter = tabAdapter
        TabLayoutMediator(binding.tabLayout, binding.pager) { tab, position ->
            tab.text = tabAdapter.getTabTitle(position)
        }.attach()

        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.addMember.setOnClickListener { ContactPickerFragment(conversation.contacts).show(supportFragmentManager, ContactPickerFragment.TAG) }
    }

    private fun profileImageClicked(contact: Contact, isGroup: Boolean) {
        val view = DialogProfileBinding.inflate(LayoutInflater.from(this)).apply {
            camera.setOnClickListener {
                if (mDeviceRuntimeService.hasVideoPermission())
                    gotToImageCapture()
                else
                    askCameraPermission()
            }
            gallery.setOnClickListener {
                goToGallery()
            }
        }
        mProfilePhoto = view.profilePhoto

        val dialogDisposableBag = CompositeDisposable().apply {
            add(mConversationFacade
                .startConversation(path!!.accountId, path!!.conversationUri)
                .flatMapObservable { mConversationFacade.observeConversation(it) }
                .firstOrError()
                .observeOn(DeviceUtils.uiScheduler)
                .subscribe { conversationViewModel ->
                    mProfilePhoto?.setImageDrawable(
                        AvatarDrawable.Builder()
                            .withViewModel(conversationViewModel)
                            .withCircleCrop(true)
                            .build(this@ConversationDetailsActivity)
                    )
                }
            )
            mDisposableBag.add(this) // Should not be needed, but just in case
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.profile)
            .setView(view.root)
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }
            .setPositiveButton(android.R.string.ok) { dialog, which ->
                mSourcePhoto?.let { source ->
                    val os = ByteArrayOutputStream()
                    BitmapUtils.createScaledBitmap(source, 512)
                        .compress(Bitmap.CompressFormat.JPEG, 90, os)
                    val avatarByteArray = os.toByteArray()
                    val avatarBase64 = Base64.encodeToString(avatarByteArray, Base64.NO_WRAP)

                    if (!isGroup) {
                        val id = Base64.encodeToString(
                            contact.primaryNumber.toByteArray(), Base64.NO_WRAP)
                        VCardUtils.saveToCustomProfiles(null, avatarByteArray,
                            path!!.accountId, id, applicationContext.filesDir)
                        updateCustomProfile(contact, path!!.accountId, id)
                    } else {
                        // For group conversations, update the conversation info with Base64
                        val map: MutableMap<String, String> = HashMap()
                        map["avatar"] = avatarBase64
                        mAccountService.updateConversationInfo(path!!.accountId,
                            path!!.conversationUri.host, map)
                    }
                }
            }
            .apply {
                if (!isGroup) {
                    setNeutralButton(R.string.reset) { dialog, _ ->
                        val id = Base64.encodeToString(
                            contact.primaryNumber.toByteArray(), Base64.NO_WRAP)
                        VCardUtils.resetCustomProfilePicture(
                            path!!.accountId, id, applicationContext.filesDir)
                        updateCustomProfile(contact, path!!.accountId, id)
                        dialog.dismiss()
                    }
                }
            }
            .setOnDismissListener {
                dialogDisposableBag.dispose()
                mProfilePhoto = null
                mSourcePhoto = null
            }
            .show()
    }

    private fun updateCustomProfile(contact: Contact, accountId: String, id: String) {
        Single.fromCallable {
            VCardUtils.getCustomProfile(accountId, id, applicationContext.filesDir)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ (name, picture) ->
                val bitmapPicture = picture?.let {
                    BitmapFactory.decodeByteArray(it, 0, it.size)
                }
                contact.customProfile = Single.just(Profile(name, bitmapPicture))
            }, { error ->
                Log.e(TAG, "Error loading custom profile", error)
            })
    }

    private fun gotToImageCapture() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            val file = AndroidFileUtils.createImageFile(this)
            val uri = FileProvider.getUriForFile(this, ContentUri.AUTHORITY_FILES, file)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                .putExtra("android.intent.extras.CAMERA_FACING", 1)
                .putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
                .putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
            tmpProfilePhotoUri = uri
            cameraResultLauncher.launch(intent)
        } catch (e: IOException) {
            Log.e(TAG, "Can't create temp file", e)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "File is outside the paths supported by the provider", e)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "No camera app found", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching camera", e)
        } finally {
            Toast.makeText(this, getString(R.string.camera_error), Toast.LENGTH_SHORT).show()
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
        galleryResultLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
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
            HomeActivity::class.java
        ))
    }

    override fun onContactPicked(accountId: String, contacts: Set<Contact>) =
        mAccountService.addConversationMembers(
            accountId,
            conversationId = path!!.conversationUri.host,
            uris = contacts.map { contact -> contact.uri }
        )

    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity, conversation: Conversation) : FragmentStateAdapter(fa) {

        private val fragments: List<Fragment>
        private val titles: List<String>

        fun getTabTitle(position: Int): String = titles[position]

        init {
            val accountId = conversation.accountId
            val conversationId = conversation.uri
            val mode = conversation.mode.blockingFirst()

            when (mode) {
                Conversation.Mode.OneToOne -> {
                    if (conversation.contact!!.isBlocked) {
                        titles = listOf(getString(R.string.details))
                        fragments = listOf(
                            ConversationActionsFragment.newInstance(accountId, conversationId)
                        )
                    } else {
                        titles = listOf(getString(R.string.details), getString(R.string.tab_files))
                        fragments = listOf(
                            ConversationActionsFragment.newInstance(accountId, conversationId),
                            ConversationGalleryFragment.newInstance(accountId, conversationId)
                        )
                    }

                }

                Conversation.Mode.Legacy -> {
                    titles = listOf(getString(R.string.details))
                    fragments =
                        listOf(ConversationActionsFragment.newInstance(accountId, conversationId))
                }

                Conversation.Mode.Request, Conversation.Mode.Syncing -> {
                    if (conversation.request?.mode == Conversation.Mode.OneToOne) {
                        titles = listOf(getString(R.string.details))
                        fragments = listOf(
                            ConversationActionsFragment.newInstance(accountId, conversationId)
                        )
                    } else {
                        titles = listOf(
                            getString(R.string.details), getString(R.string.tab_members)
                        )
                        fragments = listOf(
                            ConversationActionsFragment.newInstance(accountId, conversationId),
                            ConversationMembersFragment.newInstance(accountId, conversationId)
                        )
                    }
                }

                else -> {
                    titles = listOf(
                        getString(R.string.details),
                        getString(R.string.tab_members),
                        getString(R.string.tab_files)
                    )
                    fragments = listOf(
                        ConversationActionsFragment.newInstance(accountId, conversationId),
                        ConversationMembersFragment.newInstance(accountId, conversationId),
                        ConversationGalleryFragment.newInstance(accountId, conversationId)
                    )
                }
            }

            // Hide tab layout if there is only one tab
            binding?.tabLayout?.isVisible = titles.size > 1
        }

        override fun getItemCount(): Int = fragments.size

        override fun createFragment(position: Int): Fragment = fragments[position]
    }

    companion object {
        val TAG = ConversationDetailsActivity::class.simpleName!!
        const val EXIT_REASON = "exit_reason"
        enum class ExitReason {
            CONTACT_ADDED, CONTACT_DELETED, CONVERSATION_LEFT,
            CONTACT_BLOCKED, CONTACT_UNBLOCKED, INVITATION_ACCEPTED
        }
        const val REQUEST_CODE_CALL = 3
    }
}
