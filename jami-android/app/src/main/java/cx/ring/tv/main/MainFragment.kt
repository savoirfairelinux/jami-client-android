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
package cx.ring.tv.main

import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.FileProvider
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.widget.*
import androidx.tvprovider.media.tv.Channel
import androidx.tvprovider.media.tv.ChannelLogoUtils
import androidx.tvprovider.media.tv.PreviewProgram
import androidx.tvprovider.media.tv.TvContractCompat
import cx.ring.R
import cx.ring.tv.account.TVAccountExport
import cx.ring.tv.account.TVProfileEditingFragment
import cx.ring.tv.account.TVShareActivity
import cx.ring.tv.call.TVCallActivity
import cx.ring.tv.cards.*
import cx.ring.tv.cards.contacts.ContactCard
import cx.ring.tv.cards.iconcards.IconCard
import cx.ring.tv.cards.iconcards.IconCardHelper
import cx.ring.tv.contact.TVContactFragment
import cx.ring.tv.search.SearchActivity
import cx.ring.tv.settings.TVSettingsActivity
import cx.ring.tv.views.CustomTitleView
import cx.ring.utils.AndroidFileUtils
import cx.ring.utils.BitmapUtils
import cx.ring.utils.ContentUri
import cx.ring.utils.ConversationPath
import cx.ring.views.AvatarDrawable
import cx.ring.views.AvatarFactory
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import net.jami.model.Account
import net.jami.model.Conversation
import net.jami.model.Profile
import net.jami.navigation.HomeNavigationViewModel
import net.jami.smartlist.ConversationItemViewModel
import net.jami.utils.QRCodeUtils
import java.io.BufferedOutputStream
import java.io.FileOutputStream

@AndroidEntryPoint
class MainFragment : BaseBrowseFragment<MainPresenter>(), MainView {
    private val mSpinnerFragment: SpinnerFragment = SpinnerFragment()
    private var cardRowAdapter: ArrayObjectAdapter? = null
    private var contactRequestRowAdapter: ArrayObjectAdapter? = null
    private var mTitleView: CustomTitleView? = null
    private var requestsRow: CardListRow? = null
    private var selector: CardPresenterSelector? = null
    private var qrCard: IconCard? = null
    private var accountSettingsRow: ListRow? = null
    private val mDisposable = CompositeDisposable()
    private val mHomeChannelDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        headersState = HEADERS_DISABLED
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mTitleView = view.findViewById<CustomTitleView>(R.id.browse_title_group).apply {
            settingsButton.setOnClickListener { presenter.onSettingsClicked() }
        }
        onItemViewClickedListener = ItemViewClickedListener()
        setOnSearchClickedListener { startActivity(Intent(context, SearchActivity::class.java)) }
        CardPresenterSelector(requireContext(), presenter.conversationFacade).apply {
            selector = this
            cardRowAdapter = ArrayObjectAdapter(this)
        }
        val contactRow = CardRow(false, getString(R.string.tv_contact_row_header), ArrayList())
        val cardPresenterHeader = HeaderItem(HEADER_CONTACTS, getString(R.string.tv_contact_row_header))
        val contactListRow = CardListRow(cardPresenterHeader, cardRowAdapter, contactRow)
        val settingsRow = createAccountSettingsRow(requireContext())
        accountSettingsRow = settingsRow
        adapter = ArrayObjectAdapter(ShadowRowPresenterSelector()).apply {
            add(contactListRow)
            add(settingsRow)
        }
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        (activity as? HomeActivity)?.enableBlur(true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mDisposable.clear()
    }

    private fun createRow(titleSection: String, cards: List<Card>, shadow: Boolean): ListRow {
        val row = CardRow(shadow, titleSection, cards)
        val listRowAdapter = ArrayObjectAdapter(selector!!)
        for (card in cards) {
            listRowAdapter.add(card)
        }
        return CardListRow(HeaderItem(HEADER_MISC, titleSection), listRowAdapter, row)
    }

    private fun createAccountSettingsRow(context: Context): ListRow {
        val cards = ArrayList<Card>(3).apply {
            add(IconCardHelper.getAccountManagementCard(context))
            add(IconCardHelper.getAccountAddDeviceCard(context))
            add(IconCardHelper.getAccountShareCard(context, null).apply { qrCard = this })
        }
        return createRow(getString(R.string.account_tv_settings_header), cards, false)
    }

    private fun createContactRequestRow(): CardListRow {
        val contactRequestRow = CardRow(false, getString(R.string.menu_item_contact_request), ArrayList<ContactCard>())
        contactRequestRowAdapter = ArrayObjectAdapter(selector!!)
        return CardListRow(HeaderItem(HEADER_MISC, getString(R.string.menu_item_contact_request)), contactRequestRowAdapter, contactRequestRow)
    }

    override fun showLoading(show: Boolean) {
        if (show) {
            parentFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.main_browse_fragment, mSpinnerFragment)
                .commitAllowingStateLoss()
        } else {
            parentFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .remove(mSpinnerFragment)
                .commitAllowingStateLoss()
        }
    }

    /*override fun refreshContact(index: Int, contact: ConversationItemViewModel) {
        val contactCard = cardRowAdapter!![index] as ContactCard
        contactCard.model = contact
        cardRowAdapter!!.replace(index, contactCard)
    }*/

    private val diff = object : DiffCallback<Card>() {
        override fun areItemsTheSame(oldItem: Card, newItem: Card): Boolean {
            if (oldItem.type != newItem.type) return false
            if (oldItem is ContactCard && newItem is ContactCard)
                return oldItem.model === newItem.model
            if (oldItem is IconCard && newItem is IconCard)
                return oldItem.title == newItem.title
            return false
        }

        override fun areContentsTheSame(oldItem: Card, newItem: Card): Boolean {
            return areItemsTheSame(oldItem, newItem)
        }
    }

    override fun showContacts(contacts: List<Conversation>) {
        val cards: MutableList<Card> = ArrayList(contacts.size + 1)
        cards.add(IconCardHelper.getAddContactCard(requireContext()))
        for (contact in contacts) cards.add(ContactCard(contact, Card.Type.CONTACT_WITH_USERNAME_ONLINE))
        cardRowAdapter!!.setItems(cards, diff)
        buildHomeChannel(requireContext().applicationContext, contacts)
    }

    private fun buildHomeChannel(context: Context, contacts: List<Conversation>) {
        if (contacts.isEmpty()) return

        // Get launcher package name
        val resolveInfo = context.packageManager.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            PackageManager.MATCH_DEFAULT_ONLY
        )
        val launcherName = resolveInfo?.activityInfo?.packageName
        val cr = context.contentResolver
        mHomeChannelDisposable.clear()
        mHomeChannelDisposable.add(Single.fromCallable { createHomeChannel(context) }
            .doOnEvent { channelId: Long?, error: Throwable? ->
                if (error != null) {
                    Log.w(TAG, "Error creating home channel", error)
                } else {
                    cr.delete(TvContractCompat.buildPreviewProgramsUriForChannel(channelId ?: 0), null, null)
                }
            }
            .flatMapObservable { channelId -> Observable.fromIterable(contacts)
                .concatMapEager({ contact -> presenter.contactService.getLoadedConversation(contact)
                    .flatMap { vm -> buildProgram(context, vm, launcherName, channelId) }
                    .toObservable()
                    .subscribeOn(Schedulers.io())
                }, 8, 1)
            }
            .subscribeOn(Schedulers.io())
            .subscribe({ program -> cr.insert(TvContractCompat.PreviewPrograms.CONTENT_URI, program.toContentValues()) }
            ) { e: Throwable -> Log.w(TAG, "Error updating home channel", e) })
    }

    override fun showContactRequests(contacts: List<Conversation>) {
        val adapter = adapter as ArrayObjectAdapter
        val row = adapter[TRUST_REQUEST_ROW_POSITION] as CardListRow?
        val isRowDisplayed = row === requestsRow
        val cards = contacts.map { contact -> ContactCard(contact, Card.Type.CONTACT_WITH_USERNAME) }
        if (isRowDisplayed && cards.isEmpty()) {
            adapter.removeItems(TRUST_REQUEST_ROW_POSITION, 1)
        } else if (contacts.isNotEmpty()) {
            if (requestsRow == null)
                requestsRow = createContactRequestRow()
            contactRequestRowAdapter!!.setItems(cards, diff)
            if (!isRowDisplayed)
                adapter.add(TRUST_REQUEST_ROW_POSITION, requestsRow!!)
        }
    }

    override fun callContact(accountID: String, number: String) {
        try {
            val intent = Intent(
                Intent.ACTION_CALL,
                ConversationPath.toUri(accountID, number),
                activity,
                TVCallActivity::class.java
            )
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting activity", e)
        }
    }

    override fun displayAccountInfo(viewModel: HomeNavigationViewModel) {
        updateModel(viewModel.account, viewModel.profile)
    }

    private fun updateModel(account: Account, profile: Profile) {
        val context = requireContext()
        val address = account.displayUsername
        val name = profile.displayName
        if (name != null && name.isNotEmpty()) {
            mTitleView?.setAlias(name)
            title = address ?: ""
        } else {
            mTitleView?.setAlias(address)
        }
        val settingsAdapter = accountSettingsRow?.adapter as? ArrayObjectAdapter
        if (account.hasManager()) { // Hide link device for jams account
            settingsAdapter?.let {
                var managementCardIndex: Int? = null
                var addDeviceCardIndex: Int? = null

                for (i in 0 until it.size()) {
                    val card = it[i] as? IconCard
                    when (card?.type) {
                        Card.Type.ACCOUNT_EDIT_PROFILE -> managementCardIndex = i
                        Card.Type.ACCOUNT_ADD_DEVICE -> addDeviceCardIndex = i
                        else -> {}
                    }
                }
                if (addDeviceCardIndex != null) {
                    it.removeItems(addDeviceCardIndex, 1)
                }
                if (managementCardIndex != null) {
                    it.removeItems(managementCardIndex, 1)
                }
            }
        }
        settingsAdapter?.notifyArrayItemRangeChanged(0, settingsAdapter.size())
        mTitleView?.apply {
            settingsButton.visibility = View.VISIBLE
            logoView.visibility = View.VISIBLE
            logoView.setImageDrawable(AvatarDrawable.build(context, account, profile, true))
        }
        prepareAccountQr(context, account.uri)?.let { qr ->
            qrCard?.drawable = qr
            accountSettingsRow?.adapter?.notifyItemRangeChanged(QR_ITEM_POSITION, 1)
        }
    }

    override fun showExportDialog(pAccountID: String, hasPassword: Boolean) {
        val wizard: GuidedStepSupportFragment =
            TVAccountExport.createInstance(pAccountID, hasPassword)
        GuidedStepSupportFragment.add(parentFragmentManager, wizard, R.id.main_browse_fragment)
    }

    override fun showProfileEditing() {
        GuidedStepSupportFragment.add(
            parentFragmentManager,
            TVProfileEditingFragment(),
            R.id.main_browse_fragment
        )
    }

    override fun showAccountShare() {
        try {
            startActivity(Intent(activity, TVShareActivity::class.java))
        } catch (e: Exception) {
            Log.e(TAG, "Error starting activity", e)
        }
    }

    override fun showSettings() {
        try {
            startActivity(Intent(activity, TVSettingsActivity::class.java))
        } catch (e: Exception) {
            Log.e(TAG, "Error starting activity", e)
        }
    }

    private inner class ItemViewClickedListener : OnItemViewClickedListener {
        override fun onItemClicked(itemViewHolder: Presenter.ViewHolder, item: Any, rowViewHolder: RowPresenter.ViewHolder, row: Row) {
            if (item is ContactCard) {
                val model = item.model
                val contactFragment = TVContactFragment().apply {
                    arguments = ConversationPath.toBundle(model.accountId, model.uri)
                }
                parentFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .hide(this@MainFragment)
                    .add(R.id.fragment_container, contactFragment, TVContactFragment.TAG)
                    .addToBackStack(TVContactFragment.TAG)
                    .commit()
            } else if (item is IconCard) {
                try {
                    when (item.type) {
                        Card.Type.ACCOUNT_ADD_DEVICE -> presenter.onExportClicked()
                        Card.Type.ACCOUNT_EDIT_PROFILE -> presenter.onEditProfileClicked()
                        Card.Type.ACCOUNT_SHARE_ACCOUNT -> {
                            val view = (itemViewHolder.view as CardView).mainImageView
                            val intent = Intent(activity, TVShareActivity::class.java)
                            val bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                                requireActivity(),
                                view,
                                TVShareActivity.SHARED_ELEMENT_NAME
                            ).toBundle()
                            startActivity(intent, bundle)
                        }
                        Card.Type.ADD_CONTACT -> startActivity(Intent(activity, SearchActivity::class.java))
                        else -> {}
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting activity", e)
                }
            }
        }
    }

    companion object {
        private val TAG = MainFragment::class.simpleName!!

        // Sections headers ids
        private const val HEADER_CONTACTS: Long = 0
        private const val HEADER_MISC: Long = 1
        private const val TRUST_REQUEST_ROW_POSITION = 1
        private const val QR_ITEM_POSITION = 2
        private const val PREFERENCES_CHANNELS = "channels"
        private const val KEY_CHANNEL_CONVERSATIONS = "conversations"
        private val HOME_URI = Uri.Builder()
            .scheme(ContentUri.SCHEME_TV)
            .authority(ContentUri.AUTHORITY)
            .appendPath(ContentUri.PATH_TV_HOME)
            .build()

        private fun createHomeChannel(context: Context): Long {
            val channel = Channel.Builder()
                .setType(TvContractCompat.Channels.TYPE_PREVIEW)
                .setDisplayName(context.getString(R.string.navigation_item_conversation))
                .setAppLinkIntentUri(HOME_URI)
                .build()
            val cr = context.contentResolver
            val sharedPref = context.getSharedPreferences(PREFERENCES_CHANNELS, Context.MODE_PRIVATE)
            var channelId = sharedPref.getLong(KEY_CHANNEL_CONVERSATIONS, -1)
            if (channelId == -1L) {
                val channelUri = cr.insert(TvContractCompat.Channels.CONTENT_URI, channel.toContentValues())
                channelId = ContentUris.parseId(channelUri!!)
                sharedPref.edit().putLong(KEY_CHANNEL_CONVERSATIONS, channelId).apply()
                val targetSize = (AvatarFactory.SIZE_NOTIF * context.resources.displayMetrics.density).toInt()
                val targetPaddingSize = (AvatarFactory.SIZE_PADDING * context.resources.displayMetrics.density).toInt()
                ChannelLogoUtils.storeChannelLogo(
                    context, channelId, BitmapUtils.drawableToBitmap(context.getDrawable(R.drawable.ic_jami_48)!!, targetSize, targetPaddingSize))
                TvContractCompat.requestChannelBrowsable(context, channelId)
            } else {
                cr.update(TvContractCompat.buildChannelUri(channelId), channel.toContentValues(), null, null)
            }
            return channelId
        }

        @SuppressLint("RestrictedApi")
        private fun buildProgram(context: Context, vm: ConversationItemViewModel, launcherName: String?, channelId: Long): Single<PreviewProgram> {
            return AvatarDrawable.Builder()
                .withViewModel(vm)
                .withPresence(false)
                .buildAsync(context)
                .map { avatar: AvatarDrawable ->
                    val file = AndroidFileUtils.createImageFile(context)
                    val bitmapAvatar = BitmapUtils.drawableToBitmap(avatar, 256)
                    BufferedOutputStream(FileOutputStream(file)).use { os ->
                        bitmapAvatar.compress(Bitmap.CompressFormat.PNG, 100, os)
                    }
                    bitmapAvatar.recycle()
                    val uri = FileProvider.getUriForFile(context, ContentUri.AUTHORITY_FILES, file)

                    // Grant permission to launcher
                    if (launcherName != null)
                        context.grantUriPermission(launcherName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                    val contactBuilder = PreviewProgram.Builder()
                        .setChannelId(channelId)
                        .setType(TvContractCompat.PreviewPrograms.TYPE_CLIP)
                        .setTitle(vm.title)
                        .setAuthor(vm.uriTitle)
                        .setPosterArtAspectRatio(TvContractCompat.PreviewPrograms.ASPECT_RATIO_1_1)
                        .setPosterArtUri(uri)
                        .setIntentUri(Uri.Builder()
                            .scheme(ContentUri.SCHEME_TV)
                            .authority(ContentUri.AUTHORITY)
                            .appendPath(ContentUri.PATH_TV_CONVERSATION)
                            .appendPath(vm.accountId)
                            .appendPath(vm.uri.uri)
                            .build())
                        .setInternalProviderId(vm.uuid)
                    contactBuilder.build()
                }
        }

        private fun prepareAccountQr(context: Context, accountId: String?): BitmapDrawable? {
            Log.w(TAG, "prepareAccountQr $accountId")
            if (accountId == null || accountId.isEmpty()) return null
            val pad = 16
            val qrCodeData = QRCodeUtils.encodeStringAsQRCodeData(accountId, 0X00000000, -0x1)!!
            val bitmap = Bitmap.createBitmap(qrCodeData.width + 2 * pad, qrCodeData.height + 2 * pad, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(
                qrCodeData.data,
                0,
                qrCodeData.width,
                pad,
                pad,
                qrCodeData.width,
                qrCodeData.height
            )
            return BitmapDrawable(context.resources, bitmap)
        }
    }
}