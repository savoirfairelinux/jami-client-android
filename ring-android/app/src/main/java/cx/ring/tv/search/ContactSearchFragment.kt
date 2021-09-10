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

import cx.ring.utils.ConversationPath.Companion.toUri
import dagger.hilt.android.AndroidEntryPoint
import cx.ring.tv.search.BaseSearchFragment
import cx.ring.tv.search.ContactSearchPresenter
import androidx.leanback.app.SearchSupportFragment
import cx.ring.tv.search.ContactSearchView
import android.os.Bundle
import cx.ring.tv.cards.contacts.ContactCard
import androidx.core.content.ContextCompat
import cx.ring.R
import androidx.leanback.widget.SearchBar.SearchBarListener
import net.jami.model.Contact
import cx.ring.tv.cards.CardPresenterSelector
import android.content.Intent
import android.view.View
import androidx.leanback.widget.*
import cx.ring.client.CallActivity
import cx.ring.utils.ConversationPath
import cx.ring.tv.call.TVCallActivity
import cx.ring.tv.cards.Card
import net.jami.smartlist.SmartListViewModel
import cx.ring.tv.contact.TVContactActivity

@AndroidEntryPoint
class ContactSearchFragment : BaseSearchFragment<ContactSearchPresenter>(),
    SearchSupportFragment.SearchResultProvider, ContactSearchView {

    private var mTextEditor: SearchEditText? = null
    private val mRowsAdapter = ArrayObjectAdapter(ListRowPresenter())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSearchResultProvider(this)
        setOnItemViewClickedListener { _, item: Any, _, _ ->
            presenter.contactClicked((item as ContactCard).model!!)
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

    override fun displayContact(accountId: String, contact: Contact) {
        mRowsAdapter.clear()
        val listRowAdapter = ArrayObjectAdapter(CardPresenterSelector(activity))
        listRowAdapter.add(ContactCard(accountId, contact, Card.Type.SEARCH_RESULT))
        val header = HeaderItem(getString(R.string.search_results))
        mRowsAdapter.add(ListRow(header, listRowAdapter))
    }

    override fun clearSearch() {
        mRowsAdapter.clear()
    }

    override fun startCall(accountID: String, number: String) {
        val intent = Intent(CallActivity.ACTION_CALL, toUri(accountID, number), activity, TVCallActivity::class.java)
        intent.putExtra(Intent.EXTRA_PHONE_NUMBER, number)
        startActivity(intent)
        activity?.finish()
    }

    override fun displayContactDetails(model: SmartListViewModel) {
        val intent = Intent(activity, TVContactActivity::class.java)
        //intent.putExtra(TVContactActivity.CONTACT_REQUEST_URI, model.getContact().getPrimaryUri());
        intent.setDataAndType(toUri(model.accountId, model.uri), TVContactActivity.TYPE_CONTACT_REQUEST_OUTGOING)
        startActivity(intent)
        activity?.finish()
    }
}