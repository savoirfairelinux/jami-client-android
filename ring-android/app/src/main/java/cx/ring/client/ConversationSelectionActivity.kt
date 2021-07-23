/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
package cx.ring.client

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cx.ring.R
import cx.ring.adapters.SmartListAdapter
import cx.ring.fragments.CallFragment
import cx.ring.utils.ConversationPath
import cx.ring.viewholders.SmartListViewHolder.SmartListListeners
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.facades.ConversationFacade
import net.jami.model.Account
import net.jami.model.Conference
import net.jami.services.CallService
import net.jami.smartlist.SmartListViewModel
import javax.inject.Inject
import javax.inject.Singleton

@AndroidEntryPoint
class ConversationSelectionActivity : AppCompatActivity() {
    private val mDisposable = CompositeDisposable()

    @Inject
    @Singleton lateinit
    var mConversationFacade: ConversationFacade

    @Inject
    @Singleton lateinit
    var mCallService: CallService

    private val adapter: SmartListAdapter = SmartListAdapter(null, object : SmartListListeners {
        override fun onItemClick(smartListViewModel: SmartListViewModel) {
            val intent = Intent()
            intent.data = ConversationPath.toUri(smartListViewModel.accountId, smartListViewModel.uri)
            setResult(RESULT_OK, intent)
            finish()
        }

        override fun onItemLongClick(smartListViewModel: SmartListViewModel) {}
    }, mDisposable)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.frag_selectconv)
        val list = findViewById<RecyclerView>(R.id.conversationList)
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter
    }

    public override fun onStart() {
        super.onStart()
        var conference: Conference? = null
        val intent = intent
        if (intent != null) {
            val confId = intent.getStringExtra(CallFragment.KEY_CONF_ID)
            if (!TextUtils.isEmpty(confId)) {
                conference = mCallService.getConference(confId)
            }
        }
        val conf = conference
        mDisposable.add(mConversationFacade
            .currentAccountSubject
            .switchMap { a: Account -> a.getConversationsViewModels(false) }
            .map { vm: MutableList<SmartListViewModel> ->
                if (conf == null) return@map vm
                val filteredVms: MutableList<SmartListViewModel> = ArrayList(vm.size)
                models@ for (v in vm) {
                    val contacts = v.contacts
                    if (contacts.size != 1) continue
                    for (call in conf.participants) {
                        if (call.contact === v.contacts[0]) {
                            continue@models
                        }
                    }
                    filteredVms.add(v)
                }
                filteredVms
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { list -> adapter.update(list) })
    }

    public override fun onStop() {
        super.onStop()
        mDisposable.clear()
    }

    public override fun onDestroy() {
        super.onDestroy()
        findViewById<RecyclerView>(R.id.conversationList).adapter = null
        adapter.update(ArrayList())
    }
}
