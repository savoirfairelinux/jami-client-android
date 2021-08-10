/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *  Author: AmirHossein Naghshzan <amirhossein.naghshzan@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.tv.contact

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.leanback.widget.*
import cx.ring.R
import cx.ring.tv.call.TVCallActivity
import cx.ring.tv.contact.more.TVContactMoreActivity
import cx.ring.tv.contact.more.TVContactMoreFragment
import cx.ring.tv.contactrequest.TVContactRequestDetailPresenter
import cx.ring.tv.main.BaseDetailFragment
import cx.ring.utils.ConversationPath
import cx.ring.views.AvatarDrawable
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.Uri
import net.jami.services.NotificationService
import net.jami.smartlist.SmartListViewModel

@AndroidEntryPoint
class TVContactFragment : BaseDetailFragment<TVContactPresenter>(), TVContactView {
    private val mDisposableBag = CompositeDisposable()
    private var mAdapter: ArrayObjectAdapter? = null
    private var iconSize = -1
    private var isIncomingRequest = false
    private var isOutgoingRequest = false
    private var mConversationPath: ConversationPath? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val type: String?
        if (arguments != null) {
            mConversationPath = ConversationPath.fromBundle(arguments)
            type = arguments?.getString("type")
        } else {
            mConversationPath = ConversationPath.fromIntent(requireActivity().intent)
            type = activity?.intent?.type
        }
        if (type != null) {
            when (type) {
                TVContactActivity.TYPE_CONTACT_REQUEST_INCOMING -> isIncomingRequest = true
                TVContactActivity.TYPE_CONTACT_REQUEST_OUTGOING -> isOutgoingRequest = true
            }
        }
        setupAdapter()
        val res = resources
        iconSize = res.getDimensionPixelSize(R.dimen.tv_avatar_size)
        presenter!!.setContact(mConversationPath)
    }

    private fun setupAdapter() {
        // Set detail background and style.
        val detailsPresenter: FullWidthDetailsOverviewRowPresenter = if (isIncomingRequest || isOutgoingRequest) {
            FullWidthDetailsOverviewRowPresenter(
                TVContactRequestDetailPresenter(),
                DetailsOverviewLogoPresenter()
            )
        } else {
            FullWidthDetailsOverviewRowPresenter(
                TVContactDetailPresenter(),
                DetailsOverviewLogoPresenter()
            )
        }
        detailsPresenter.backgroundColor =
            ContextCompat.getColor(requireContext(), R.color.tv_contact_background)
        detailsPresenter.actionsBackgroundColor =
            ContextCompat.getColor(requireContext(), R.color.tv_contact_row_background)
        detailsPresenter.initialState = FullWidthDetailsOverviewRowPresenter.STATE_HALF

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
            if (action.id == ACTION_CALL.toLong()) {
                presenter!!.contactClicked()
            } else if (action.id == ACTION_ADD_CONTACT.toLong()) {
                presenter!!.onAddContact()
            } else if (action.id == ACTION_ACCEPT.toLong()) {
                presenter!!.acceptTrustRequest()
            } else if (action.id == ACTION_REFUSE.toLong()) {
                presenter!!.refuseTrustRequest()
            } else if (action.id == ACTION_BLOCK.toLong()) {
                presenter!!.blockTrustRequest()
            } else if (action.id == ACTION_MORE.toLong()) {
                startActivityForResult(
                    Intent(getActivity(), TVContactMoreActivity::class.java)
                        .setDataAndType(
                            mConversationPath!!.toUri(),
                            TVContactMoreActivity.CONTACT_REQUEST_URI
                        ),
                    REQUEST_CODE
                )
            }
        }
        val mPresenterSelector = ClassPresenterSelector()
        mPresenterSelector.addClassPresenter(DetailsOverviewRow::class.java, detailsPresenter)
        mPresenterSelector.addClassPresenter(ListRow::class.java, ListRowPresenter())
        mAdapter = ArrayObjectAdapter(mPresenterSelector)
        adapter = mAdapter
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE) {
            if (resultCode == TVContactMoreFragment.DELETE) finishView()
        }
    }

    override fun showContact(model: SmartListViewModel) {
        val context = requireContext()
        val row = DetailsOverviewRow(model)
        val avatar = AvatarDrawable.Builder()
            .withViewModel(model) //.withPresence(false)
            .withCircleCrop(false)
            .build(context)
        avatar.setInSize(iconSize)
        row.imageDrawable = avatar
        val adapter = SparseArrayObjectAdapter()
        if (isIncomingRequest) {
            adapter[ACTION_ACCEPT] = Action(ACTION_ACCEPT.toLong(), resources
                    .getString(R.string.accept))
            adapter[ACTION_REFUSE] = Action(ACTION_REFUSE.toLong(), resources.getString(R.string.refuse))
            adapter[ACTION_BLOCK] = Action(ACTION_BLOCK.toLong(), resources.getString(R.string.block))
        } else if (isOutgoingRequest) {
            adapter[ACTION_ADD_CONTACT] = Action(ACTION_ADD_CONTACT.toLong(), resources.getString(R.string.ab_action_contact_add))
        } else {
            adapter[ACTION_CALL] = Action(ACTION_CALL.toLong(), resources.getString(R.string.ab_action_video_call), null, context.getDrawable(R.drawable.baseline_videocam_24))
            adapter[ACTION_MORE] = Action(ACTION_MORE.toLong(), resources.getString(R.string.tv_action_more), null, context.getDrawable(R.drawable.baseline_more_vert_24))
        }
        row.actionsAdapter = adapter
        mAdapter!!.add(row)
    }

    override fun callContact(accountId: String, conversationUri: Uri, uri: Uri) {
        startActivity(
            Intent(Intent.ACTION_CALL)
                .setClass(requireContext(), TVCallActivity::class.java)
                .putExtras(ConversationPath.toBundle(accountId, conversationUri))
                .putExtra(Intent.EXTRA_PHONE_NUMBER, uri.uri)
        )
    }

    override fun goToCallActivity(id: String) {
        startActivity(
            Intent(requireContext(), TVCallActivity::class.java)
                .putExtra(NotificationService.KEY_CALL_ID, id)
        )
    }

    override fun switchToConversationView() {
        isIncomingRequest = false
        isOutgoingRequest = false
        setupAdapter()
        presenter!!.setContact(mConversationPath)
    }

    override fun finishView() {
        val activity: Activity? = activity
        activity?.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        mDisposableBag.dispose()
    }

    companion object {
        @JvmField
        val TAG = TVContactFragment::class.simpleName!!
        private const val ACTION_CALL = 0
        private const val ACTION_ACCEPT = 1
        private const val ACTION_REFUSE = 2
        private const val ACTION_BLOCK = 3
        private const val ACTION_ADD_CONTACT = 4
        private const val ACTION_MORE = 5
        private const val REQUEST_CODE = 100
    }
}