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
package cx.ring.tv.search

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.leanback.app.SearchSupportFragment
import androidx.leanback.widget.*
import cx.ring.R
import cx.ring.tv.call.TVCallActivity
import cx.ring.tv.cards.Card
import cx.ring.tv.cards.CardPresenterSelector
import cx.ring.tv.cards.contacts.ContactCard
import cx.ring.tv.contact.TVContactActivity
import cx.ring.utils.ConversationPath
import dagger.hilt.android.AndroidEntryPoint
import net.jami.model.Conversation
import net.jami.services.ConversationFacade

@AndroidEntryPoint
class ContactSearchFragment : BaseSearchFragment<ContactSearchPresenter>(),
    SearchSupportFragment.SearchResultProvider, ContactSearchView {

    private var mTextEditor: SearchEditText? = null
    private val mRowsAdapter = ArrayObjectAdapter(ListRowPresenter())
    private var directoryRow: ListRow? = null
    private var conversationsRow: ListRow? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSearchResultProvider(this)
        setOnItemViewClickedListener { _, item: Any, _, _ ->
            presenter.contactClicked((item as ContactCard).model)
        }
        badgeDrawable = ContextCompat.getDrawable(requireContext(), R.mipmap.ic_launcher)
        setSearchQuery("", false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mTextEditor = view.findViewById(androidx.leanback.R.id.lb_search_text_editor)
        mTextEditor?.requestFocus()
    }

    override fun getResultsAdapter(): ObjectAdapter = mRowsAdapter

    override fun onQueryTextChange(newQuery: String): Boolean {
        presenter.queryTextChanged(newQuery)
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        presenter.queryTextChanged(query)
        return true
    }

    private val diff = object : DiffCallback<ContactCard>() {
        override fun areItemsTheSame(oldItem: ContactCard, newItem: ContactCard): Boolean {
            return oldItem.model === newItem.model
        }

        override fun areContentsTheSame(oldItem: ContactCard, newItem: ContactCard): Boolean {
            return oldItem.model === newItem.model
        }
    }

    override fun displayResults(contacts: ConversationFacade.ConversationList, conversationFacade: ConversationFacade) {
        var scrollToTop = false
        if (contacts.searchResult.result.isEmpty()) {
            directoryRow?.apply {
                mRowsAdapter.remove(this)
                directoryRow = null
            }
        } else {
            if (directoryRow == null) {
                val adapter = ArrayObjectAdapter(CardPresenterSelector(requireContext(), conversationFacade))
                adapter.addAll(0, contacts.searchResult.result.map { item -> ContactCard(item, Card.Type.SEARCH_RESULT) })
                directoryRow = ListRow(HeaderItem(getString(R.string.search_results)), adapter).apply {
                    mRowsAdapter.add(0, this)
                }
            } else {
                (directoryRow!!.adapter as ArrayObjectAdapter).setItems(
                    contacts.searchResult.result.map { item -> ContactCard(item, Card.Type.SEARCH_RESULT) }, diff)
            }
            scrollToTop = true
        }

        if (contacts.conversations.isEmpty()) {
            conversationsRow?.apply {
                mRowsAdapter.remove(this)
                conversationsRow = null
            }
        } else {
            if (conversationsRow == null) {
                val adapter = ArrayObjectAdapter(CardPresenterSelector(requireContext(), conversationFacade))
                adapter.addAll(0, contacts.conversations.map { item -> ContactCard(item, Card.Type.CONTACT_ONLINE) })
                conversationsRow = ListRow(HeaderItem(getString(R.string.navigation_item_conversation)), adapter).apply {
                    mRowsAdapter.add(this)
                }
            } else {
                (conversationsRow!!.adapter as ArrayObjectAdapter).setItems(
                    contacts.conversations.map { item -> ContactCard(item, Card.Type.CONTACT_ONLINE) }, diff)
            }
        }
    }

    override fun clearSearch() {
        mRowsAdapter.clear()
        directoryRow = null
        conversationsRow = null
    }

    override fun startCall(accountID: String, number: String) {
        try {
            val intent = Intent(Intent.ACTION_CALL, ConversationPath.toUri(accountID, number), activity, TVCallActivity::class.java)
            intent.putExtra(Intent.EXTRA_PHONE_NUMBER, number)
            startActivity(intent)
            activity?.finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting activity", e)
        }
    }

    override fun displayContactDetails(model: Conversation) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW,
                ConversationPath.toUri(model.accountId, model.uri),
                activity, TVContactActivity::class.java))
            activity?.finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting activity", e)
        }
    }

    companion object {
        private val TAG = ContactSearchFragment::class.simpleName!!
    }
}