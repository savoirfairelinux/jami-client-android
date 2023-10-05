/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
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
 *  along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.fragments

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import cx.ring.R
import cx.ring.adapters.SmartListAdapter
import cx.ring.client.CallActivity
import cx.ring.client.HomeActivity
import cx.ring.databinding.FragSmartlistBinding
import cx.ring.mvp.BaseSupportFragment
import cx.ring.utils.ActionHelper
import cx.ring.utils.ConversationPath
import cx.ring.utils.TextUtils
import cx.ring.viewholders.SmartListViewHolder.SmartListListeners
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import net.jami.model.Conversation
import net.jami.model.Conversation.ConversationActionCallback
import net.jami.model.Uri
import net.jami.services.AccountService
import net.jami.services.ConversationFacade
import net.jami.smartlist.SmartListPresenter
import net.jami.smartlist.SmartListView
import javax.inject.Inject


@AndroidEntryPoint
class SmartListFragment : BaseSupportFragment<SmartListPresenter, SmartListView>(),
    SmartListListeners, ConversationActionCallback, SmartListView {
    private var mSmartListAdapter: SmartListAdapter? = null
    private var binding: FragSmartlistBinding? = null
    private var mHomeFragment: HomeFragment? = null

    override fun onSaveInstanceState(outState: Bundle) {
        binding?.apply { outState.putBoolean(STATE_LOADING, loadingIndicator.isShown) }
        super.onSaveInstanceState(outState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragSmartlistBinding.inflate(inflater, container, false).apply {
            (confsList.itemAnimator as DefaultItemAnimator?)?.supportsChangeAnimations = false
            binding = this
        }.root

    fun getRecyclerView(): RecyclerView? = binding?.confsList

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onStart() {
        super.onStart()
        if (mHomeFragment == null)
            mHomeFragment =
                    requireActivity().supportFragmentManager.findFragmentById(R.id.home_fragment) as HomeFragment?
    }

    override fun setLoading(loading: Boolean) {
        binding!!.loadingIndicator.visibility = if (loading) View.VISIBLE else View.GONE
    }

    override fun removeConversation(accountId: String, conversationUri: Uri) {
        presenter.removeConversation(accountId, conversationUri)
    }

    override fun clearConversation(accountId: String, conversationUri: Uri) {
        presenter.clearConversation(accountId, conversationUri)
    }

    override fun copyContactNumberToClipboard(contactNumber: String) {
        TextUtils.copyToClipboard(requireContext(), contactNumber)
    }

    override fun displayChooseNumberDialog(numbers: Array<CharSequence>) {
        val context = requireContext()
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.choose_number)
            .setItems(numbers) { _: DialogInterface?, which: Int ->
                val selected = numbers[which]
                val intent = Intent(Intent.ACTION_CALL)
                    .setClass(context, CallActivity::class.java)
                    .setData(android.net.Uri.parse(selected.toString()))
                startActivityForResult(intent, HomeActivity.REQUEST_CODE_CALL)
            }
            .show()
    }

    override fun displayNoConversationMessage() {
        binding!!.placeholder.visibility = View.VISIBLE
        //(activity as HomeActivity).toggleConversationVisibility(false)
    }

    override fun hideNoConversationMessage() {
        binding!!.placeholder.visibility = View.GONE
        //(activity as HomeActivity).toggleConversationVisibility(true)
    }

    override fun displayConversationDialog(conversationItemViewModel: Conversation) {
        if (conversationItemViewModel.isSwarm) {
            MaterialAlertDialogBuilder(requireContext())
                .setItems(R.array.swarm_actions) { dialog, which ->
                    when (which) {
                        0 -> presenter.copyNumber(conversationItemViewModel)
                        1 -> presenter.removeConversation(conversationItemViewModel)
                        2 -> presenter.banContact(conversationItemViewModel)
                    }
                }
                .show()
        } else {
            MaterialAlertDialogBuilder(requireContext())
                .setItems(R.array.conversation_actions) { dialog, which ->
                    when (which) {
                        ActionHelper.ACTION_COPY -> presenter.copyNumber(conversationItemViewModel)
                        ActionHelper.ACTION_CLEAR -> presenter.clearConversation(conversationItemViewModel)
                        ActionHelper.ACTION_DELETE -> presenter.removeConversation(conversationItemViewModel)
                        ActionHelper.ACTION_BLOCK -> presenter.banContact(conversationItemViewModel)
                    }
                }
                .show()
        }
    }

    override fun displayClearDialog(accountId: String, conversationUri: Uri) {
        ActionHelper.launchClearAction(requireContext(), accountId, conversationUri, this@SmartListFragment)
    }

    override fun displayDeleteDialog(accountId: String, conversationUri: Uri) {
        ActionHelper.launchDeleteAction(requireContext(), accountId, conversationUri, this@SmartListFragment)
    }

    override fun copyNumber(uri: Uri) {
        copyContactNumberToClipboard(uri.toString())
    }

    override fun hideList() {
        binding!!.confsList.visibility = View.GONE
        mSmartListAdapter?.update(ConversationFacade.ConversationList())
    }

    override fun updateList(conversations: ConversationFacade.ConversationList, conversationFacade: ConversationFacade, parentDisposable: CompositeDisposable) {
        //Log.w(TAG, "updateList ${conversations.publicDirectory.size} ${conversations.conversations.size}")
        binding?.apply {
            if (confsList.adapter == null) {
                confsList.adapter = SmartListAdapter(conversations, this@SmartListFragment, conversationFacade, parentDisposable).apply {
                    mSmartListAdapter = this
                }
                confsList.setHasFixedSize(true)
                confsList.layoutManager = LinearLayoutManager(requireContext()).apply {
                    orientation = RecyclerView.VERTICAL
                }
            } else {
                mSmartListAdapter?.update(conversations)
            }
            confsList.visibility = View.VISIBLE
        }
    }

    override fun update(position: Int) {
        Log.w(TAG, "update $position $mSmartListAdapter")
        mSmartListAdapter?.notifyItemChanged(position)
    }

    override fun update(model: Conversation) {
        //mSmartListAdapter?.update(model)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == HomeActivity.REQUEST_CODE_QR_CONVERSATION && data != null && resultCode == Activity.RESULT_OK) {
            data.getStringExtra(ConversationPath.KEY_CONVERSATION_URI)?.let { contactId ->
                presenter.startConversation(Uri.fromString(contactId))
            }
        }
    }

    override fun goToConversation(accountId: String, conversationUri: Uri) {
        Log.w(TAG, "goToConversation $accountId $conversationUri")
        (mHomeFragment as HomeFragment).collapseSearchActionView()
        (requireActivity() as HomeActivity).startConversation(accountId, conversationUri)
    }

    override fun goToCallActivity(accountId: String, conversationUri: Uri, contactId: String) {
        val intent = Intent(Intent.ACTION_CALL)
            .setClass(requireContext(), CallActivity::class.java)
            .putExtras(ConversationPath.toBundle(accountId, conversationUri))
            .putExtra(CallFragment.KEY_HAS_VIDEO, true)
            .putExtra(Intent.EXTRA_PHONE_NUMBER, contactId)
        startActivityForResult(intent, HomeActivity.REQUEST_CODE_CALL)
    }

    override fun scrollToTop() {
        binding?.apply { confsList.scrollToPosition(0) }
    }

    override fun onItemClick(item: Conversation) {
        presenter.conversationClicked(item)
    }

    override fun onItemLongClick(item: Conversation) {
        presenter.conversationLongClicked(item)
    }

    companion object {
        val TAG = SmartListFragment::class.simpleName!!
        private val STATE_LOADING = "$TAG.STATE_LOADING"
        private const val SCROLL_DIRECTION_UP = -1
    }

}