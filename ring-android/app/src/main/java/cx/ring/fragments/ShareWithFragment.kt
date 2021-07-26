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
package cx.ring.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.InflateException
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import cx.ring.adapters.SmartListAdapter
import cx.ring.client.ConversationActivity
import cx.ring.databinding.FragSharewithBinding
import cx.ring.utils.ConversationPath
import cx.ring.viewholders.SmartListViewHolder.SmartListListeners
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.facades.ConversationFacade
import net.jami.model.Account
import net.jami.services.ContactService
import net.jami.smartlist.SmartListViewModel
import javax.inject.Inject
import javax.inject.Singleton

@AndroidEntryPoint
class ShareWithFragment : Fragment() {
    private val mDisposable = CompositeDisposable()

    @JvmField
    @Inject
    @Singleton
    var mConversationFacade: ConversationFacade? = null

    @JvmField
    @Inject
    @Singleton
    var mContactService: ContactService? = null
    private var mPendingIntent: Intent? = null
    private var adapter: SmartListAdapter? = null
    private var binding: FragSharewithBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragSharewithBinding.inflate(inflater)
        val context = binding!!.root.context
        val activity: Activity? = activity
        if (activity is AppCompatActivity) {
            activity.setSupportActionBar(binding!!.toolbar)
            val ab = activity.supportActionBar
            ab?.setDisplayHomeAsUpEnabled(true)
        }
        if (mPendingIntent != null) {
            val type = mPendingIntent!!.type!!
            val clip = mPendingIntent!!.clipData
            when {
                type.startsWith("text/") -> {
                    binding!!.previewText.setText(mPendingIntent!!.getStringExtra(Intent.EXTRA_TEXT))
                    binding!!.previewText.visibility = View.VISIBLE
                }
                type.startsWith("image/") -> {
                    var data = mPendingIntent!!.data
                    if (data == null && clip != null && clip.itemCount > 0) data = clip.getItemAt(0).uri
                    binding!!.previewImage.setImageURI(data)
                    binding!!.previewImage.visibility = View.VISIBLE
                }
                type.startsWith("video/") -> {
                    var data = mPendingIntent!!.data
                    if (data == null && clip != null && clip.itemCount > 0) data = clip.getItemAt(0).uri
                    try {
                        binding!!.previewVideo.setVideoURI(data)
                        binding!!.previewVideo.visibility = View.VISIBLE
                    } catch (e: NullPointerException) {
                        Log.e(TAG, e.message!!)
                    } catch (e: InflateException) {
                        Log.e(TAG, e.message!!)
                    } catch (e: NumberFormatException) {
                        Log.e(TAG, e.message!!)
                    }
                    binding!!.previewVideo.setOnCompletionListener { binding!!.previewVideo.start() }
                }
            }
        }
        adapter = SmartListAdapter(null, object : SmartListListeners {
            override fun onItemClick(smartListViewModel: SmartListViewModel) {
                mPendingIntent?.let { intent ->
                    mPendingIntent = null
                    val type = intent.type
                    if (type != null && type.startsWith("text/")) {
                        intent.putExtra(Intent.EXTRA_TEXT, binding!!.previewText.text.toString())
                    }
                    intent.putExtras(
                        ConversationPath.toBundle(
                            smartListViewModel.accountId,
                            smartListViewModel.uri
                        )
                    )
                    intent.setClass(requireActivity(), ConversationActivity::class.java)
                    startActivity(intent)
                }
            }

            override fun onItemLongClick(smartListViewModel: SmartListViewModel) {}
        }, mDisposable)
        binding!!.shareList.layoutManager = LinearLayoutManager(context)
        binding!!.shareList.adapter = adapter
        return binding!!.root
    }

    override fun onStart() {
        super.onStart()
        if (mPendingIntent == null) requireActivity().finish()
        mDisposable.add(mConversationFacade!!
            .currentAccountSubject
            .switchMap { a: Account -> a.getConversationsViewModels(false) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { list: MutableList<SmartListViewModel> ->
                adapter?.update(list)
            })
        if (binding != null && binding!!.previewVideo.visibility != View.GONE) {
            binding!!.previewVideo.start()
        }
    }

    override fun onStop() {
        super.onStop()
        mDisposable.clear()
    }

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        /*Intent intent = getActivity().getIntent();
        Bundle extra = intent.getExtras();
        if (ConversationPath.fromBundle(extra) != null) {
            intent.setClass(getActivity(), ConversationActivity.class);
            startActivity(intent);
            return;
        }*/mPendingIntent = requireActivity().intent
    }

    override fun onDestroy() {
        super.onDestroy()
        mPendingIntent = null
        adapter = null
    }

    companion object {
        private val TAG = ShareWithFragment::class.java.simpleName

        /**
         * Mandatory empty constructor for the fragment manager to instantiate the
         * fragment (e.g. upon screen orientation changes).
         */
        /*public ShareWithFragment() {
        JamiApplication.getInstance().getInjectionComponent().inject(this);
    }*/
        fun newInstance(): ShareWithFragment {
            return ShareWithFragment()
        }
    }
}