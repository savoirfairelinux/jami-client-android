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
package cx.ring.tv.contact

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.leanback.widget.*
import cx.ring.R
import cx.ring.fragments.CallFragment
import cx.ring.tv.call.TVCallActivity
import cx.ring.tv.contact.more.TVContactMoreActivity
import cx.ring.tv.contact.more.TVContactMoreFragment
import cx.ring.tv.contactrequest.TVContactRequestDetailPresenter
import cx.ring.tv.main.BaseDetailFragment
import cx.ring.utils.ConversationPath
import cx.ring.views.AvatarDrawable
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.Account
import net.jami.model.Conversation
import net.jami.model.Uri
import net.jami.services.NotificationService
import net.jami.smartlist.ConversationItemViewModel

@AndroidEntryPoint
class TVContactFragment : BaseDetailFragment<TVContactPresenter>(), TVContactView {
    private val mDisposableBag = CompositeDisposable()
    private var mAdapter: ArrayObjectAdapter? = null
    private var iconSize = -1
    private lateinit var mConversationPath: ConversationPath

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mConversationPath = if (arguments != null)
            ConversationPath.fromBundle(arguments)!!
        else
            ConversationPath.fromIntent(requireActivity().intent)!!
        iconSize = resources.getDimensionPixelSize(R.dimen.tv_avatar_size)
        setupAdapter()
        presenter.setContact(mConversationPath)
    }

    private fun setupAdapter() {
        // Set detail background and style.
        val detailsPresenter =  FullWidthDetailsOverviewRowPresenter(TVContactDetailPresenter(), DetailsOverviewLogoPresenter()).apply {
            backgroundColor = ContextCompat.getColor(requireContext(), R.color.tv_contact_background)
            actionsBackgroundColor = ContextCompat.getColor(requireContext(), R.color.tv_contact_row_background)
            initialState = FullWidthDetailsOverviewRowPresenter.STATE_HALF
        }

        // Hook up transition element.
        val activity: Activity? = activity
        if (activity != null) {
            val mHelper = FullWidthDetailsOverviewSharedElementHelper()
            mHelper.setSharedElementEnterTransition(activity, TVContactActivity.SHARED_ELEMENT_NAME)
            detailsPresenter.setListener(mHelper)
            detailsPresenter.isParticipatingEntranceTransition = false
            prepareEntranceTransition()
        }
        detailsPresenter.onActionClickedListener = OnActionClickedListener { action: Action ->
            when (action.id) {
                ACTION_CALL -> presenter.contactClicked()
                ACTION_ADD_CONTACT -> presenter.onAddContact()
                ACTION_ACCEPT -> presenter.acceptTrustRequest()
                ACTION_REFUSE -> presenter.refuseTrustRequest()
                ACTION_BLOCK -> presenter.blockTrustRequest()
                ACTION_MORE -> startActivityForResult(Intent(getActivity(), TVContactMoreActivity::class.java)
                        .setDataAndType(mConversationPath.toUri(), TVContactMoreActivity.CONTACT_REQUEST_URI), REQUEST_CODE)
            }
        }
        mAdapter?.clear()
        mAdapter = ArrayObjectAdapter(ClassPresenterSelector().apply {
            addClassPresenter(DetailsOverviewRow::class.java, detailsPresenter)
            addClassPresenter(ListRow::class.java, ListRowPresenter())
        })
        adapter = mAdapter
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE) {
            if (resultCode == TVContactMoreFragment.DELETE) finishView()
        }
    }

    override fun showContact(account: Account, model: ConversationItemViewModel) {
        val context = requireContext()
        val row = DetailsOverviewRow(model)
        val avatar = AvatarDrawable.Builder()
            .withViewModel(model) //.withPresence(false)
            .withCircleCrop(false)
            .build(context)
        avatar.setInSize(iconSize)
        row.imageDrawable = avatar
        val adapter = ArrayObjectAdapter()
        if (model.mode == Conversation.Mode.Request) {
            adapter.add(Action(ACTION_ACCEPT, resources.getString(R.string.accept)))
            adapter.add(Action(ACTION_REFUSE, resources.getString(R.string.decline)))
            adapter.add(Action(ACTION_BLOCK, resources.getString(R.string.block)))
        } else if (model.isSwarm || account.isContact(model.uri)) {
            adapter.add(Action(ACTION_CALL, resources.getString(R.string.ab_action_video_call), null, context.getDrawable(R.drawable.baseline_videocam_24)))
            adapter.add(Action(ACTION_MORE, resources.getString(R.string.tv_action_more), null, context.getDrawable(R.drawable.baseline_more_vert_24)))
        } else {
            if (model.request == null) {
                adapter.add(Action(ACTION_ADD_CONTACT, resources.getString(R.string.ab_action_contact_add)))
            } else {
                adapter.add(Action(ACTION_ACCEPT, resources.getString(R.string.accept)))
                adapter.add(Action(ACTION_REFUSE, resources.getString(R.string.decline)))
                adapter.add(Action(ACTION_BLOCK, resources.getString(R.string.block)))
            }
        }
        row.actionsAdapter = adapter
        if (mAdapter?.size() == 0) mAdapter?.add(row)
        else mAdapter?.replace(0, row)
    }

    override fun callContact(accountId: String, conversationUri: Uri, uri: Uri) {
        startActivity(Intent(Intent.ACTION_CALL)
            .setClass(requireContext(), TVCallActivity::class.java)
            .putExtras(ConversationPath.toBundle(accountId, conversationUri))
            .putExtra(Intent.EXTRA_PHONE_NUMBER, uri.uri)
            .putExtra(CallFragment.KEY_HAS_VIDEO, true))
    }

    override fun goToCallActivity(id: String) {
        startActivity(Intent(requireContext(), TVCallActivity::class.java)
            .putExtra(NotificationService.KEY_CALL_ID, id))
    }

    override fun switchToConversationView() {
        presenter.setContact(mConversationPath)
    }

    override fun finishView() {
        activity?.onBackPressedDispatcher?.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        mDisposableBag.dispose()
    }

    companion object {
        val TAG = TVContactFragment::class.simpleName!!
        private const val ACTION_CALL = 0L
        private const val ACTION_ACCEPT = 1L
        private const val ACTION_REFUSE = 2L
        private const val ACTION_BLOCK = 3L
        private const val ACTION_ADD_CONTACT = 4L
        private const val ACTION_MORE = 5L
        private const val REQUEST_CODE = 100
    }
}