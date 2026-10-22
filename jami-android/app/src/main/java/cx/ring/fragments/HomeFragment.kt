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
package cx.ring.fragments

import android.animation.Animator
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.SearchView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.doOnLayout
import androidx.core.view.doOnNextLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.Slide
import androidx.transition.TransitionManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.Behavior.DragCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.R
import cx.ring.account.AccountWizardActivity
import cx.ring.adapters.SmartListAdapter
import cx.ring.client.AccountAdapter
import cx.ring.client.BroadcastActivity
import cx.ring.client.CallActivity
import cx.ring.client.CollabDocuments
import cx.ring.client.HomeActivity
import cx.ring.databinding.LayoutFunctionsPanelBinding
import cx.ring.mvp.BaseSupportFragment
import cx.ring.navigation.BoardBottomSheet
import cx.ring.navigation.CirclesBottomSheet
import cx.ring.navigation.DocumentsBottomSheet
import cx.ring.navigation.GroupsBottomSheet
import cx.ring.navigation.ServicesBottomSheet
import cx.ring.navigation.TargetPickerBottomSheet
import cx.ring.utils.BitmapUtils
import cx.ring.utils.ConversationPath
import cx.ring.utils.DeviceUtils
import cx.ring.viewholders.SmartListViewHolder
import cx.ring.views.AvatarDrawable
import cx.ring.views.Pulse
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import net.jami.home.HomePresenter
import net.jami.home.HomeView
import net.jami.model.Conversation
import net.jami.model.Target
import net.jami.model.TargetCapability
import net.jami.services.AccountService
import net.jami.services.CollaborationService
import net.jami.services.ConversationFacade
import net.jami.smartlist.ConversationItemViewModel
import java.text.Collator
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import javax.inject.Inject
import com.google.android.material.search.SearchView.TransitionState
import com.google.android.material.shape.MaterialShapeDrawable
import cx.ring.databinding.FragHomeBinding
import cx.ring.utils.ActionHelper.openJamiDonateWebPage
import io.reactivex.rxjava3.disposables.Disposable
import net.jami.model.Uri
import net.jami.services.NotificationService

@AndroidEntryPoint
class HomeFragment: BaseSupportFragment<HomePresenter, HomeView>(),
    SearchView.OnQueryTextListener, HomeView {

    private var mBinding: FragHomeBinding? = null
    private var mSmartListFragment: SmartListFragment? = null
    private val mDisposable = CompositeDisposable()
    private var mSearchView: SearchView? = null
    private var searchDisposable: Disposable? = null
    private var searchAdapter: SmartListAdapter? = null
    private var pendingAdapter: SmartListAdapter? = null
    private val querySubject = BehaviorSubject.createDefault("")
    private val sortAlphabeticallySubject = BehaviorSubject.createDefault(false)
    /** The search view shows the Contacts screen, opened by the Contacts function, rather than a search. */
    @Volatile private var contactsMode = false
    private var openingContacts = false
    private var functionsBehavior: BottomSheetBehavior<View>? = null
    private val debouncedQuery = querySubject.debounce { item ->
        if (item.isEmpty()) Observable.empty()
        else Observable.timer(350, TimeUnit.MILLISECONDS)
    }.distinctUntilChanged()

    @Inject
    lateinit var mAccountService: AccountService

    @Inject
    lateinit var mConversationFacade: ConversationFacade

    @Inject
    lateinit var mCollaborationService: CollaborationService

    private val searchBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            collapseSearchActionView()
        }
    }

    private val conversationBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            collapsePendingView()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity().onBackPressedDispatcher.let {
            it.addCallback(this, conversationBackPressedCallback)
            it.addCallback(this, searchBackPressedCallback)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = FragHomeBinding.inflate(inflater, container, false).apply {

        qrCode.setOnClickListener { goToQRFragment() }
        newSwarm.setOnClickListener { startNewSwarm() }

        // Contacts screen: the QR code sits before the search field, which shows a magnifier
        contactsQrCode.setOnClickListener { goToQRFragment() }
        (searchView.editText.parent as? LinearLayout)?.let { field ->
            (contactsQrCode.parent as? ViewGroup)?.removeView(contactsQrCode)
            field.addView(contactsQrCode, 0, LinearLayout.LayoutParams(dp(44f), dp(40f)).apply {
                gravity = Gravity.CENTER_VERTICAL
                marginEnd = dp(12f)
            })
        }
        searchView.editText.compoundDrawablePadding = dp(8f)
        contactsSortTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                sortAlphabeticallySubject.onNext(tab.position == 1)
                searchResult.scrollToPosition(0)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}

            override fun onTabReselected(tab: TabLayout.Tab) {
                searchResult.scrollToPosition(0)
            }
        })

        setUpFunctions(functionsPanel)

        // SearchBar is composed of:
        // - Account selection (navigation)
        // - Search bar (search for swarms or for new contacts)
        // - Menu (for settings, about jami)
        searchBar.setNavigationOnClickListener { // Account selection
            mDisposable.add(mAccountService.observableAccountList.firstElement().subscribe { accounts ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.account_selection))
                    .setAdapter(
                            AccountAdapter(
                                requireContext(),
                                accounts,
                                mDisposable, mAccountService, mConversationFacade
                            )
                    ) { _, index ->
                        if (index >= accounts.size) // Add account
                            startActivity(Intent(activity, AccountWizardActivity::class.java))
                        else if (mAccountService.currentAccount != accounts[index]) {
                            // Disable account settings menu option when account is loading
                            searchBar.menu.findItem(R.id.menu_account_settings).isEnabled = false
                            mAccountService.currentAccount = accounts[index]
                        }
                    }.show()
            })
        }
        searchView.editText.addTextChangedListener { // Search bar
            querySubject.onNext(it.toString())
        }

        // Inflate Menu and connect it
        searchBar.inflateMenu(R.menu.smartlist_menu)
        searchBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_account_settings -> (activity as? HomeActivity)?.goToAccountSettings()

                R.id.menu_advanced_settings -> (activity as? HomeActivity)?.goToAdvancedSettings()

                R.id.menu_about -> (activity as? HomeActivity)?.goToAbout()

                R.id.menu_donate -> openJamiDonateWebPage(requireContext())
            }
            true
        }

        // Update padding of the list depending on the AppBarLayout height
        appBar.addOnLayoutChangeListener { _, _, top, _, bottom, _, _, _, _ ->
            mSmartListFragment?.getRecyclerView()?.updatePadding(
                top = appBar.height - DeviceUtils.getStatusBarHeight(requireContext())
            )
        }

        // Make the appBarLayout not going under the status bar.
        appBar.statusBarForeground = MaterialShapeDrawable.createWithElevationOverlay(requireContext())

        // Setup search result list
        searchResult.setHasFixedSize(true)
        searchResult.adapter = SmartListAdapter(null,
                object : SmartListViewHolder.SmartListListeners {
                    override fun onItemClick(item: Conversation) {
                        collapseSearchActionView()
                        (requireActivity() as HomeActivity).startConversation(item.accountId, item.uri)
                    }

                    override fun onItemLongClick(item: Conversation) {}
                },
                mConversationFacade,
                mDisposable
        ).apply { searchAdapter = this }

        searchView.addTransitionListener { _, previousState, newState ->
            // When using the SearchView, we have to :
            // - Manage back press
            // - Manage search results

            if (newState === TransitionState.SHOWN) { // Shown
                // Hide AppBar and SmartList to avoid weird animation
                fragmentContainer.isVisible = false
                appBar.isVisible = false

                searchBackPressedCallback.isEnabled = true
            } else if (previousState === TransitionState.SHOWN) { // Hiding
                // Make SmartList and appbar visible again
                fragmentContainer.isVisible = true
                appBar.isVisible = true

                searchBackPressedCallback.isEnabled = false
            }

            if (newState === TransitionState.HIDDEN) { // Hidden
                updateFunctionsPanelVisibility()

                searchDisposable?.dispose()
                querySubject.onNext("")
                searchAdapter?.update(ConversationFacade.ConversationList())
                searchDisposable = null
            } else if (previousState === TransitionState.HIDDEN) { // Showing
                // From the search bar, the search; from the Contacts function, the Contacts screen.
                if (!openingContacts) applySearchMode(contacts = false)
                openingContacts = false
                updateFunctionsPanelVisibility()
                searchView.toolbar.navigationIcon = AppCompatResources.getDrawable(
                    requireContext(), R.drawable.baseline_arrow_back_24
                )
                startSearch()
            }
        }

        // Setup donation card
        donationCard.donationCard.visibility = View.GONE
        donationCard.donationCard.setOnClickListener {
            openJamiDonateWebPage(requireContext())
        }
        donationCard.donationCardDonateButton.setOnClickListener {
            openJamiDonateWebPage(requireContext())
        }
        donationCard.donationCardNotNowButton.setOnClickListener {
            presenter.setDonationReminderDismissed()
        }

        // Setup invitation card adapter.
        invitationCard.pendingList.adapter = SmartListAdapter(null,
                object : SmartListViewHolder.SmartListListeners {
                    override fun onItemClick(item: Conversation) {
                        (requireActivity() as HomeActivity).startConversation(item.accountId, item.uri)
                    }

                    override fun onItemLongClick(item: Conversation) {
                        displayConversationRequestDialog(item)
                    }
                },
                mConversationFacade,
                mDisposable
        ).apply { pendingAdapter = this }

        // Setup invitation card
        invitationCard.invitationGroup.setOnClickListener {
            expandPendingView()
        }

        // Return to search
        invitationCard.pendingToolbar.setNavigationOnClickListener { collapsePendingView() }

        mBinding = this
    }.root

    private fun displayConversationRequestDialog(conversation: Conversation) {
        val request = conversation.request ?: return
        if (request.mode == Conversation.Mode.OneToOne)
            MaterialAlertDialogBuilder(requireContext())
                .setItems(R.array.swarm_request_one_to_one_actions) { _, which ->
                    when (which) {
                        0 -> mConversationFacade.acceptRequest(conversation)
                        1 -> mConversationFacade
                            .discardRequest(conversation.accountId, conversation.uri)
                        2 -> mConversationFacade
                            .blockConversation(conversation.accountId, conversation.uri)
                    }
                }.show()
        else
            MaterialAlertDialogBuilder(requireContext())
                .setItems(R.array.swarm_request_group_actions) { _, which ->
                    when (which) {
                        0 -> mConversationFacade.acceptRequest(conversation)
                        1 -> mConversationFacade
                            .discardRequest(conversation.accountId, conversation.uri)
                    }
                }.show()
    }

    /**
     * The search view serves two screens: the search, as the search bar opens it,
     * and the Contacts screen, every conversation sorted by date or by name.
     */
    private fun applySearchMode(contacts: Boolean) {
        val binding = mBinding ?: return
        contactsMode = contacts
        binding.contactsSortTabs.isVisible = contacts
        binding.contactsQrCode.isVisible = contacts
        binding.buttons.isVisible = !contacts
        binding.searchView.isAutoShowKeyboard = !contacts
        binding.searchView.hint = if (contacts) "" else getString(R.string.searchbar_hint)
        binding.searchView.editText.setCompoundDrawablesRelativeWithIntrinsicBounds(
            if (contacts) R.drawable.baseline_search_24 else 0, 0, 0, 0
        )
    }

    /** The Contacts function. */
    private fun openContactsScreen() {
        openingContacts = true
        applySearchMode(contacts = true)
        mBinding?.searchView?.show()
    }

    /**
     * The conversations sorted by the titles the list shows, as they resolve. A title that
     * does not come (name lookup) does not hold the list up: it is sorted last meanwhile.
     */
    private fun sortedByTitle(conversations: List<Conversation>): Observable<List<Conversation>> {
        if (conversations.isEmpty()) return Observable.just(conversations)
        val collator = Collator.getInstance().apply { strength = Collator.SECONDARY }
        return Observable.combineLatest(conversations.map { conversation ->
            mConversationFacade.observeConversation(conversation)
                .map { it.title }
                .publish { titles ->
                    Observable.merge(titles, Observable.timer(TITLE_WAIT_MS, TimeUnit.MILLISECONDS)
                        .takeUntil(titles).map { "" })
                }
                .map { title -> Pair(conversation, title) }
        }) { entries ->
            @Suppress("UNCHECKED_CAST")
            (entries.toList() as List<Pair<Conversation, String>>).sortedWith { a, b ->
                when {
                    a.second.isBlank() != b.second.isBlank() -> if (a.second.isBlank()) 1 else -1
                    else -> collator.compare(a.second, b.second).takeIf { it != 0 }
                        ?: a.first.uri.rawUriString.compareTo(b.first.uri.rawUriString)
                }
            }.map { it.first }
        }.distinctUntilChanged()
    }

    private fun startSearch() {
        searchDisposable?.dispose()
        searchDisposable = Observable.combineLatest(
            mConversationFacade.getSearchResults(debouncedQuery),
            sortAlphabeticallySubject
        ) { list, alphabetical -> Pair(list, alphabetical) }
            .switchMap { (list, alphabetical) ->
                // Boards have their own function: they are not listed with the conversations
                val conversations = list.conversations.filterNot { it.isFeed() }
                val result = { items: List<Conversation> ->
                    ConversationFacade.ConversationList(items, list.searchResult, list.latestQuery)
                }
                when {
                    contactsMode && alphabetical -> sortedByTitle(conversations).map(result)
                    contactsMode -> Observable.just(result(conversations.sortedByDescending { it.lastEvent?.timestamp ?: 0L }))
                    // The search shows nothing until something is typed
                    list.latestQuery.isBlank() -> Observable.just(result(emptyList()))
                    else -> Observable.just(result(conversations))
                }
            }
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe { searchAdapter?.update(it) }
            .apply { mDisposable.add(this) }
    }

    /**
     * Expand the appBarLayoutBottom to give fixed space between it and fragmentList.
     */
    private fun updateAppBarLayoutBottomPadding(hasInvites: Boolean) {
        mBinding?.appBarContainer?.updatePadding(top = 0, bottom = if (hasInvites)
            resources.getDimensionPixelSize(R.dimen.bottom_sheet_radius) else 0)
        if (hasInvites)
            mSmartListFragment?.scrollToTop()
    }

    private fun expandPendingView() {
        val binding = mBinding ?: return

        // Transitions to animate the changes
        // Make the search bar slide down
        TransitionManager.beginDelayedTransition(binding.searchBar, Slide())
        // Make the invitation card expand.
        TransitionManager.beginDelayedTransition(
            binding.invitationCard.invitationGroup,
            ChangeBounds().setInterpolator(DecelerateInterpolator())
        )

        // Make the invitation card take all the height.
        binding.appBar.updateLayoutParams {
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }

        // Adapt the margins of the invitation card.
        requireContext().resources.getDimensionPixelSize(R.dimen.bottom_sheet_radius).let {
            (binding.invitationCard.invitationGroup.layoutParams as ViewGroup.MarginLayoutParams)
                .setMargins(it, it, it, 2*it)
        }

        // Enable invitation pending list scroll (remove side effect with appbar behavior).
        (binding.appBar.layoutParams as CoordinatorLayout.LayoutParams).behavior = null

        // Hide everything unneeded.
        binding.donationCard.donationCard.isVisible = false
        binding.searchBar.isVisible = false
        binding.invitationCard.invitationSummary.isVisible = false
        binding.fragmentContainer.isVisible = false

        // Display pending list.
        binding.invitationCard.pendingListGroup.isVisible = true
        updateFunctionsPanelVisibility()

        // Enable back press.
        conversationBackPressedCallback.isEnabled = true

        val insetsCompat = ViewCompat.getRootWindowInsets(binding.invitationCard.invitationGroup) ?: return
        val insets = insetsCompat.getInsets(WindowInsetsCompat.Type.systemBars())
        binding.appBar.updatePadding(bottom = insets.bottom)
    }

    fun collapsePendingView() {
        val binding = mBinding ?: return

        // Animate back to search
        // Make the search bar slide up
        TransitionManager.beginDelayedTransition(
            binding.searchBar,
            Slide().setInterpolator(DecelerateInterpolator())
        )
        // Make the invitation card collapse.
        TransitionManager.beginDelayedTransition(
            binding.appBar,
            ChangeBounds().setInterpolator(DecelerateInterpolator())
        )
        // Make the invitation card text fade in.
        TransitionManager.beginDelayedTransition(
            binding.invitationCard.invitationSummary,
            Fade()
        )

        binding.appBar.updatePadding(bottom = 0)

        // Make the invitation card wrap content (not take all space available anymore).
        binding.appBar.updateLayoutParams {
            height = ViewGroup.LayoutParams.WRAP_CONTENT
        }

        // Adapt the margins of the invitation card.
        requireContext().resources.getDimensionPixelSize(R.dimen.bottom_sheet_radius).let {
            (binding.invitationCard.invitationGroup.layoutParams as ViewGroup.MarginLayoutParams)
                .setMargins(it, 0, it, 0)
        }

        disableAppBarScroll()

        // Show everything needed.
        binding.donationCard.donationCard.isVisible = presenter.donationCardIsVisible
        binding.searchBar.isVisible = true
        binding.invitationCard.invitationSummary.isVisible = true
        binding.fragmentContainer.isVisible = true

        // Hide pending list.
        binding.invitationCard.pendingListGroup.isVisible = false
        updateFunctionsPanelVisibility()

        // Disable back press.
        conversationBackPressedCallback.isEnabled = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(mBinding!!.searchView) { v, insets ->
            if (v.paddingTop > 0) {
                // Ensure searchView ignores top insets for proper fullscreen appearance
                v.setPadding(v.paddingLeft, 0, v.paddingRight, v.paddingBottom)
            }
            insets
        }

        val accessibilityManager =
            requireContext().getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        adjustLayoutForAccessibility(accessibilityManager.isEnabled )

        accessibilityManager.addAccessibilityStateChangeListener { isEnabled ->
            adjustLayoutForAccessibility(isEnabled)
        }

        mSmartListFragment = mBinding!!.fragmentContainer.getFragment()

        disableAppBarScroll()

        // Keep the end of the conversation list above the functions panel.
        mSmartListFragment?.viewLifecycleOwnerLiveData?.observe(viewLifecycleOwner) {
            it.lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onCreate(owner: LifecycleOwner) {
                    mBinding?.root?.post { updateListBottomPadding() }
                }
            })
        }

        setUpFunctionsPanel()

        parentFragmentManager.setFragmentResultListener(DocumentsBottomSheet.REQUEST_NEW_DOCUMENT, viewLifecycleOwner) { _, _ ->
            pickNewDocumentTarget()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // The activity handles rotations itself: lay the functions out again for the new shape.
        mBinding?.root?.let { root ->
            root.doOnNextLayout { arrangeFunctions() }
            root.requestLayout()
        }
    }

    /* -------------------------------------------------------------- functions */

    private fun setUpFunctions(panel: LayoutFunctionsPanelBinding) {
        // 1. Calls: video or audio, straight from the list
        panel.cardFuncCalls.setOnClickListener {
            val sheet = TargetPickerBottomSheet.newInstance(TargetCapability.CAN_CALL, getString(R.string.function_calls))
            sheet.setOnCallSelectedListener { target, hasVideo ->
                val accountId = mAccountService.currentAccount?.accountId ?: return@setOnCallSelectedListener
                val targetUri = Uri.fromString(target.id)
                val conversationUri = target.viewModel?.uri ?: targetUri
                startActivity(Intent(Intent.ACTION_CALL)
                    .setClass(requireContext(), CallActivity::class.java)
                    .putExtras(ConversationPath.toBundle(accountId, conversationUri))
                    .putExtra(Intent.EXTRA_PHONE_NUMBER, targetUri.uri)
                    .putExtra(CallFragment.KEY_HAS_VIDEO, hasVideo))
            }
            sheet.show(parentFragmentManager, TargetPickerBottomSheet.TAG)
        }

        // 2. Messages: open the conversation, or broadcast to a Circle
        panel.cardFuncMessages.setOnClickListener {
            pickConversationTarget(TargetCapability.CAN_MESSAGE, R.string.function_messages)
        }

        // 3. Contacts
        panel.cardFuncContacts.setOnClickListener { openContactsScreen() }

        // 4. Collaborative documents, from every conversation
        panel.cardFuncDocuments.setOnClickListener {
            DocumentsBottomSheet.newInstance().show(parentFragmentManager, DocumentsBottomSheet.TAG)
        }

        // 5. Groups
        panel.cardFuncGroups.setOnClickListener {
            GroupsBottomSheet.newInstance().show(parentFragmentManager, GroupsBottomSheet.TAG)
        }

        // 6. Boards and their publications
        panel.cardFuncBoard.setOnClickListener {
            BoardBottomSheet.newInstance().show(parentFragmentManager, BoardBottomSheet.TAG)
        }

        // 7. Shared services offered by the contacts
        panel.cardFuncServices.setOnClickListener {
            ServicesBottomSheet.newInstance().show(parentFragmentManager, ServicesBottomSheet.TAG)
        }

        // 8. Location
        panel.cardFuncLocation.setOnClickListener {
            val sheet = TargetPickerBottomSheet.newInstance(TargetCapability.CAN_SHARE_LOCATION, getString(R.string.function_location))
            sheet.setOnTargetSelectedListener { target ->
                val accountId = mAccountService.currentAccount?.accountId ?: return@setOnTargetSelectedListener
                (requireActivity() as HomeActivity).startConversation(accountId, Uri.fromString(target.id))
            }
            sheet.show(parentFragmentManager, TargetPickerBottomSheet.TAG)
        }

        // 9. Circles
        panel.cardFuncCircles.setOnClickListener {
            CirclesBottomSheet.newInstance().show(parentFragmentManager, CirclesBottomSheet.TAG)
        }
    }

    private fun pickConversationTarget(capability: TargetCapability, title: Int) {
        val sheet = TargetPickerBottomSheet.newInstance(capability, getString(title))
        sheet.setOnTargetSelectedListener { target ->
            val accountId = mAccountService.currentAccount?.accountId ?: return@setOnTargetSelectedListener
            if (target is Target.CircleTarget)
                startActivity(BroadcastActivity.intent(requireContext(), accountId, target.circleId))
            else
                (requireActivity() as HomeActivity).startConversation(accountId, Uri.fromString(target.id))
        }
        sheet.show(parentFragmentManager, TargetPickerBottomSheet.TAG)
    }

    /**
     * Starts a document with the contact or in the group the user picks. Done here
     * rather than in the documents sheet: the creation outlives both sheets.
     */
    private fun pickNewDocumentTarget() {
        val sheet = TargetPickerBottomSheet.newInstance(TargetCapability.CAN_SHARE_DOC, getString(R.string.function_documents))
        sheet.setOnTargetSelectedListener { target ->
            val accountId = mAccountService.currentAccount?.accountId ?: return@setOnTargetSelectedListener
            // A contact is named by its own id: the document goes into the conversation with it
            val conversationUri = target.viewModel?.uri ?: Uri.fromString(target.id)
            CollabDocuments.promptNew(
                requireContext(), ConversationPath(accountId, conversationUri), mCollaborationService, mDisposable
            )
        }
        sheet.show(parentFragmentManager, TargetPickerBottomSheet.TAG)
    }

    private fun setUpFunctionsPanel() {
        val binding = mBinding ?: return
        val panel = binding.functionsPanel.root
        functionsBehavior = BottomSheetBehavior.from<View>(panel).apply {
            // Always unfolded when the application starts
            state = BottomSheetBehavior.STATE_EXPANDED
            addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    // Folded, only the handle shows: nothing must show through the navigation bar
                    binding.functionsPanel.functionsGrid.isInvisible = newState == BottomSheetBehavior.STATE_COLLAPSED
                    if (newState == BottomSheetBehavior.STATE_EXPANDED)
                        binding.functionsPanel.functionsGrid.alpha = 1f
                    updateListBottomPadding()
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    binding.functionsPanel.functionsGrid.alpha = slideOffset.coerceIn(0f, 1f)
                    updateListBottomPadding()
                }
            })
        }
        panel.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateListBottomPadding() }
        binding.root.doOnLayout { arrangeFunctions() }
    }

    /**
     * Lays the nine functions out so that the panel takes at most a third of the
     * screen: three rows of three, or one row when three do not fit, as in
     * landscape. The names go when the cards become too small for them.
     */
    private fun arrangeFunctions() {
        val binding = mBinding ?: return
        val panel = binding.functionsPanel
        val metrics = resources.displayMetrics
        val heightBudget = resources.configuration.screenHeightDp / 3f
        val widthDp = (binding.root.width.takeIf { it > 0 } ?: metrics.widthPixels) / metrics.density
        fun cellHeight(rows: Int) = (heightBudget - HANDLE_DP - GRID_BOTTOM_PADDING_DP) / rows - 2 * CARD_MARGIN_DP
        val rows = if (cellHeight(3) >= MIN_CELL_DP) 3 else 1
        val columns = FUNCTION_COUNT / rows
        val width = (widthDp - 2 * GRID_SIDE_PADDING_DP) / columns - 2 * CARD_MARGIN_DP
        val withNames = min(MAX_CELL_DP, cellHeight(rows)) >= NAMED_CELL_DP && width >= NAMED_CELL_DP
        // Without their names, the cards stay about square rather than becoming narrow pills
        val height = min(MAX_CELL_DP, cellHeight(rows)).let { if (withNames) it else min(it, max(width, MIN_TOUCH_DP)) }
        val iconSize = dp(min(ICON_DP, min(height, width) - 2 * CARD_PADDING_DP))

        val cards = listOf(
            panel.cardFuncCalls, panel.cardFuncMessages, panel.cardFuncContacts,
            panel.cardFuncDocuments, panel.cardFuncGroups, panel.cardFuncBoard,
            panel.cardFuncServices, panel.cardFuncLocation, panel.cardFuncCircles
        )
        val rowViews = listOf(panel.functionsRow1, panel.functionsRow2, panel.functionsRow3)
        rowViews.forEach { it.removeAllViews() }
        cards.forEachIndexed { index, card ->
            rowViews[index / columns].addView(card, LinearLayout.LayoutParams(0, dp(height), 1f).apply {
                val margin = dp(CARD_MARGIN_DP)
                setMargins(margin, margin, margin, margin)
            })
            (card.getChildAt(0) as? ViewGroup)?.children?.forEach { child ->
                if (child is TextView) child.isVisible = withNames
                else child.updateLayoutParams { this.width = iconSize; this.height = iconSize }
            }
        }
        panel.functionsRow2.isVisible = rows == 3
        panel.functionsRow3.isVisible = rows == 3
    }

    /**
     * The functions panel goes with the conversation list only. It is drawn above the
     * SearchView, so it must not show while the search, the contacts or the pending
     * invitations take the screen.
     */
    private fun updateFunctionsPanelVisibility() {
        val binding = mBinding ?: return
        binding.functionsPanel.root.isVisible =
            binding.searchView.currentTransitionState == TransitionState.HIDDEN
                && !binding.invitationCard.pendingListGroup.isVisible
    }

    /** Keeps the end of the conversation list visible above the functions panel. */
    private fun updateListBottomPadding() {
        val binding = mBinding ?: return
        val list = mSmartListFragment?.getRecyclerView() ?: return
        val panel = binding.functionsPanel.root
        val covered = if (panel.isVisible) max(0, binding.fragmentContainer.bottom - panel.top) else 0
        if (list.paddingBottom != covered) list.updatePadding(bottom = covered)
    }

    private fun dp(value: Float): Int = (value * resources.displayMetrics.density).roundToInt()

    override fun onDestroyView() {
        super.onDestroyView()
        mSmartListFragment = null
        functionsBehavior = null
        pendingAdapter = null
        searchAdapter = null
        mBinding = null
        mDisposable.dispose()
    }

    override fun onStart() {
        super.onStart()
        activity?.intent?.let { handleIntent(it) }

        // Enable account settings menu option when an account is loaded
        mDisposable.add(mAccountService.currentAccountSubject
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe {
                mBinding?.newSwarm?.isVisible = !it.isSip
                mBinding?.searchBar?.menu?.findItem(R.id.menu_account_settings)?.isEnabled = true
            }
        )

        mDisposable.add(mAccountService.currentAccountSubject
            .switchMap { it.locationSharers }
            .map { it.isNotEmpty() }
            .distinctUntilChanged()
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe { showLocationReception(it) })

        // Subscribe on invitation pending list to show a badge counter
        mDisposable.add(mAccountService
            .currentAccountSubject
            .switchMap { account ->
                account.getPendingSubject()
                    .switchMap { list ->
                        Log.w(TAG, "setBadge getPendingSubject ${list.size}")
                        if (list.isEmpty()) Observable.just(Pair(emptyList(), emptyList()))
                        else mConversationFacade.observeConversations(
                            account, list.take(3), false
                        ).map { Pair(it, list) }
                    }
            }
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe { count ->
                // Collapse pending view if there is no more pending invitations
                if(count.first.isEmpty()) {
                    collapsePendingView()
                }
                setInvitationBadge(count.second, count.first)
            }
        )

        // Subscribe on current account to display avatar on navigation (searchbar)
        mDisposable.add(mAccountService
            .currentAccountSubject
            .switchMap { mAccountService.getObservableAccountProfile(it.accountId) }
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe { profile ->
                val binding = mBinding ?: return@subscribe
                binding.searchBar.navigationIcon =
                    BitmapUtils.withPadding(
                        AvatarDrawable.build(
                            binding.root.context,
                            profile.first,
                            profile.second,
                            true,
                            profile.first.presenceStatus
                        ),
                        TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            6f,
                            resources.displayMetrics
                        ).toInt()
                    )
                binding.searchView.toolbar.navigationIcon = AppCompatResources.getDrawable(
                    binding.root.context, R.drawable.baseline_arrow_back_24
                )
            }
        )

        if (mBinding!!.searchView.isShowing)
            startSearch()
    }

    override fun onStop() {
        super.onStop()
        mDisposable.clear()
        showLocationReception(false)
    }

    private var locationPulse: Animator? = null

    // The Location function pulses while a contact sends its position.
    private fun showLocationReception(receiving: Boolean) {
        locationPulse?.cancel()
        locationPulse = null
        val ring = mBinding?.functionsPanel?.locationPulse ?: return
        ring.isVisible = receiving
        if (receiving) locationPulse = Pulse.start(ring)
    }

    private fun adjustLayoutForAccessibility(enabled: Boolean) {
        val params = mBinding?.appBarContainer?.layoutParams as? AppBarLayout.LayoutParams ?: return
        val recyclerView = mSmartListFragment?.getRecyclerView() ?: return

        params.scrollFlags = if (enabled) 0 else (
                AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or
                        AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS or
                        AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP or
                        AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED
                )
        mBinding?.appBarContainer?.layoutParams = params
        recyclerView.clipToPadding = enabled
    }

    /**
     * Set a badge to display how many invitations are pending.
     */
    private fun setInvitationBadge(conversations: List<Conversation>, snip: List<ConversationItemViewModel>) {
        val binding = mBinding ?: return
        pendingAdapter?.update(conversations)
        val hasInvites = conversations.isNotEmpty()
        binding.invitationCard.invitationGroup.isVisible = hasInvites
        if (hasInvites) {
            binding.invitationCard.invitationBadge.text = conversations.size.toString()
            binding.invitationCard.invitationReceivedTxt.text = snip.joinToString(", ") { it.title }
        }
        updateAppBarLayoutBottomPadding(hasInvites)
    }

    fun handleIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_CALL -> {
                expandSearchActionView()
                mSearchView?.setQuery(intent.dataString, true)
            }

            Intent.ACTION_DIAL -> {
                expandSearchActionView()
                mSearchView?.setQuery(intent.dataString, false)
            }

            Intent.ACTION_SEARCH -> {
                expandSearchActionView()
                mSearchView?.setQuery(intent.getStringExtra(SearchManager.QUERY), true)
            }

            NotificationService.NOTIF_TRUST_REQUEST_MULTIPLE -> {
                expandPendingView()
            }

            else -> {}
        }
    }

    override fun showDonationReminder(show: Boolean) {
        mBinding?.appBar?.let {
            TransitionManager.beginDelayedTransition(it, AutoTransition())
        }
        mBinding?.donationCard?.donationCard?.isVisible = show
        mBinding?.fragmentContainer?.getFragment<SmartListFragment>()?.scrollToTop()
    }

    override fun onQueryTextSubmit(query: String?) = true

    override fun onQueryTextChange(newText: String): Boolean {
        querySubject.onNext(newText)
        return true
    }

    private fun goToQRFragment() {
        // Hide keyboard to prevent any glitch.
        val accountUri = mAccountService.currentAccount?.uri ?: return
        (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(requireView().windowToken, 0)

        QRCodeFragment.newInstance(
            QRCodeFragment.MODE_SHARE or QRCodeFragment.MODE_SCAN,
            QRCodeFragment.MODE_SCAN,
            Uri.fromString(accountUri)
        ).show(parentFragmentManager, QRCodeFragment.TAG)

        collapseSearchActionView()
    }

    private fun startNewSwarm() {
        ContactPickerFragment().show(parentFragmentManager, ContactPickerFragment.TAG)
        collapseSearchActionView()
    }

    private fun expandSearchActionView() {
        mBinding?.searchView?.show()
    }

    fun collapseSearchActionView() {
        mBinding?.searchView?.hide()
    }

    /** Prevent appbar to be collapsed by direct scroll. */
    private fun disableAppBarScroll() {
        (mBinding!!.appBar.layoutParams as CoordinatorLayout.LayoutParams).behavior =
            AppBarLayout.Behavior().apply {
                setDragCallback(object : DragCallback() {
                    override fun canDrag(appBarLayout: AppBarLayout): Boolean = false
                })
            }
    }

    companion object {
        private const val TITLE_WAIT_MS = 1500L
        private val TAG = HomeFragment::class.simpleName!!

        private const val FUNCTION_COUNT = 9
        private const val HANDLE_DP = 32f
        private const val GRID_BOTTOM_PADDING_DP = 6f
        private const val GRID_SIDE_PADDING_DP = 9f
        private const val CARD_MARGIN_DP = 3f
        private const val CARD_PADDING_DP = 4f
        private const val ICON_DP = 34f
        /** Below this, three rows do not fit in a third of the screen: one row instead. */
        private const val MIN_CELL_DP = 52f
        private const val MAX_CELL_DP = 76f
        private const val MIN_TOUCH_DP = 48f
        private const val NAMED_CELL_DP = 56f
    }

}