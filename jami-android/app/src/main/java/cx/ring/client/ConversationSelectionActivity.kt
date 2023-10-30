/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
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

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cx.ring.R
import cx.ring.adapters.SmartListAdapter
import cx.ring.application.JamiApplication
import cx.ring.utils.ConversationPath
import cx.ring.viewholders.SmartListViewHolder.SmartListListeners
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.services.ConversationFacade
import net.jami.model.Conference
import net.jami.model.Conversation
import net.jami.services.CallService
import net.jami.services.NotificationService
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

    var adapter: SmartListAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.frag_selectconv)
        val list = findViewById<RecyclerView>(R.id.conversationList)
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = SmartListAdapter(null, object : SmartListListeners {
            override fun onItemClick(item: Conversation) {
                setResult(RESULT_OK, Intent().apply {
                    data = ConversationPath.toUri(item.accountId, item.uri)
                })
                finish()
            }

            override fun onItemLongClick(item: Conversation) {}
        }, mConversationFacade, mDisposable)
            .apply { adapter = this }
        JamiApplication.instance?.startDaemon(this)
    }

    public override fun onStart() {
        super.onStart()
        val conference: Conference? = intent?.getStringExtra(NotificationService.KEY_CALL_ID)?.let { confId -> mCallService.getConference(confId) }
        mDisposable.add(mConversationFacade
            .getConversationSmartlist()
            .map { vm: List<Conversation> ->
                if (conference == null) return@map vm

                val participantOrPendingUri =
                    conference.participants.map { it.contact?.uri } +
                            conference.pendingCalls.blockingFirst().map { it.contact.contact.uri }

                return@map vm.filter { v ->
                    try {
                        val contact = v.contact ?: return@filter false
                        if (participantOrPendingUri.contains(contact.uri)) return@filter false
                    } catch (e: Exception) {
                        return@filter false
                    }
                    return@filter true
                }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { list -> adapter?.update(list) })
    }

    public override fun onStop() {
        super.onStop()
        mDisposable.clear()
    }

    public override fun onDestroy() {
        super.onDestroy()
        adapter = null
    }
}
