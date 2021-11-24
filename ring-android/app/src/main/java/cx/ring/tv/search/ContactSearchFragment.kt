/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Michel Schmit <michel.schmit@savoirfairelinux.com>
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
package cx.ring.tv.search

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.leanback.app.SearchSupportFragment
import androidx.leanback.widget.*
import androidx.leanback.widget.SearchBar.SearchBarListener
import cx.ring.R
import cx.ring.tv.call.TVCallActivity
import cx.ring.tv.cards.Card
import cx.ring.tv.cards.CardPresenterSelector
import cx.ring.tv.cards.contacts.ContactCard
import cx.ring.tv.contact.TVContactActivity
import cx.ring.utils.ConversationPath
import dagger.hilt.android.AndroidEntryPoint
import net.jami.smartlist.ConversationItemViewModel

@AndroidEntryPoint
class ContactSearchFragment : BaseSearchFragment<ContactSearchPresenter>(),
    SearchSupportFragment.SearchResultProvider, ContactSearchView {

    private var mTextEditor: SearchEditText? = null
    private val mRowsAdapter = ArrayObjectAdapter(ListRowPresenter())

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
        mTextEditor = view.findViewById(R.id.lb_search_text_editor)
        val mSearchBar: SearchBar = view.findViewById(R.id.lb_search_bar)
        mSearchBar.setSearchBarListener(object : SearchBarListener {
            override fun onSearchQueryChange(query: String) {
                onQueryTextChange(query)
            }

            override fun onSearchQuerySubmit(query: String) {
                onQueryTextSubmit(query)
            }

            override fun onKeyboardDismiss(query: String) {
                mSearchBar.postDelayed({ rowsSupportFragment.verticalGridView.requestFocus() }, 200)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        mTextEditor?.requestFocus()
    }

    override fun getResultsAdapter(): ObjectAdapter {
        return mRowsAdapter
    }

    override fun onQueryTextChange(newQuery: String): Boolean {
        presenter.queryTextChanged(newQuery)
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        presenter.queryTextChanged(query)
        return true
    }

    override fun displayResults(contacts: List<ConversationItemViewModel>) {
        mRowsAdapter.clear()
        var listRow: ListRow? = null
        for (item in contacts) {
            if (item.headerTitle != ConversationItemViewModel.Title.None) {
                if (listRow != null)
                    mRowsAdapter.add(listRow)
                val header = HeaderItem(getString(when(item.headerTitle) {
                    ConversationItemViewModel.Title.PublicDirectory -> R.string.search_results
                    ConversationItemViewModel.Title.Conversations -> R.string.navigation_item_conversation
                    else -> -1
                }))
                listRow = ListRow(header, ArrayObjectAdapter(CardPresenterSelector(activity)))
            } else {
                if (listRow == null) {
                    listRow = ListRow(HeaderItem(getString(R.string.search_results)), ArrayObjectAdapter(CardPresenterSelector(activity)))
                }
                (listRow.adapter as ArrayObjectAdapter).add(ContactCard(item, Card.Type.SEARCH_RESULT))
            }
        }
        if (listRow != null)
            mRowsAdapter.add(listRow)
    }

    override fun clearSearch() {
        mRowsAdapter.clear()
    }

    override fun startCall(accountID: String, number: String) {
        val intent = Intent(Intent.ACTION_CALL, ConversationPath.toUri(accountID, number), activity, TVCallActivity::class.java)
        intent.putExtra(Intent.EXTRA_PHONE_NUMBER, number)
        startActivity(intent)
        activity?.finish()
    }

    override fun displayContactDetails(model: ConversationItemViewModel) {
        val intent = Intent(activity, TVContactActivity::class.java)
        //intent.putExtra(TVContactActivity.CONTACT_REQUEST_URI, model.getContact().getPrimaryUri());
        intent.setDataAndType(ConversationPath.toUri(model.accountId, model.uri), TVContactActivity.TYPE_CONTACT_REQUEST_OUTGOING)
        startActivity(intent)
        activity?.finish()
    }
}