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

import android.animation.LayoutTransition
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.core.view.MenuProvider
import androidx.core.view.doOnNextLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.card.MaterialCardView
import cx.ring.R
import cx.ring.adapters.SmartListAdapter
import cx.ring.client.HomeActivity
import cx.ring.databinding.FragSharewithBinding
import cx.ring.utils.BitmapUtils
import cx.ring.utils.BitmapUtils.getColorFromAttribute
import cx.ring.utils.ContentUri
import cx.ring.utils.ContentUri.getShareItems
import cx.ring.utils.ConversationPath
import cx.ring.viewholders.SmartListViewHolder.SmartListListeners
import cx.ring.views.PreviewVideoView
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.Subject
import net.jami.model.Conversation
import net.jami.services.ContactService
import net.jami.services.ConversationFacade
import javax.inject.Inject
import javax.inject.Singleton

@AndroidEntryPoint
class ShareWithFragment : Fragment() {
    private val mDisposable = CompositeDisposable()

    @Inject
    @Singleton
    lateinit var mConversationFacade: ConversationFacade

    @Inject
    @Singleton
    lateinit var mContactService: ContactService

    private var mPendingIntent: Intent? = null
    private var adapter: SmartListAdapter? = null
    private var binding: FragSharewithBinding? = null
    private var searchView: SearchView? = null

    private val query: Subject<String> = BehaviorSubject.createDefault("")

    private val searchBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            searchView?.setQuery("", false)
            searchView?.isIconified = true
        }
    }

    private val menuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
            inflater.inflate(R.menu.share_actions, menu)
            val searchMenuItem = menu.findItem(R.id.contact_search)
            (searchMenuItem.actionView as SearchView).let {
                searchView = it
                it.setOnCloseListener {
                    query.onNext("")
                    searchBackPressedCallback.isEnabled = false
                    false
                }
                it.setOnSearchClickListener {
                    searchBackPressedCallback.isEnabled = true
                }
                it.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String) = false

                    override fun onQueryTextChange(newText: String): Boolean {
                        query.onNext(newText)
                        return true
                    }
                })
                it.queryHint = getString(R.string.searchbar_hint)
            }
        }

        override fun onMenuItemSelected(item: MenuItem): Boolean = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        adapter = SmartListAdapter(null, object : SmartListListeners {
            override fun onItemClick(item: Conversation) {
                mPendingIntent?.let { intent ->
                    mPendingIntent = null
                    intent.putExtras(ConversationPath.toBundle(item.accountId, item.uri))
                    intent.setClass(requireActivity(), HomeActivity::class.java)
                    startActivity(intent)
                    requireActivity().finish()
                }
            }

            override fun onItemLongClick(item: Conversation) {}
        }, mConversationFacade, mDisposable)

        val binding = FragSharewithBinding.inflate(inflater).apply {
            shareList.layoutManager = LinearLayoutManager(inflater.context)
            shareList.adapter = adapter
            this@ShareWithFragment.binding = this
        }
        val activity: Activity? = activity
        if (activity is AppCompatActivity) {
            activity.setSupportActionBar(binding.toolbar)
            val ab = activity.supportActionBar
            ab?.setDisplayHomeAsUpEnabled(true)
            activity.addMenuProvider(menuProvider, viewLifecycleOwner)
            binding.toolbar.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        }
        val context = binding.root.context
        mPendingIntent?.let { intent ->
            val height = context.resources.getDimensionPixelSize(R.dimen.share_preview_height)
            binding.mediaList.adapter = ShareMediaAdapter(intent.getShareItems(context), height, mDisposable)
        }
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity().onBackPressedDispatcher.addCallback(this, searchBackPressedCallback)
    }

    override fun onStart() {
        super.onStart()
        if (mPendingIntent == null) {
            requireActivity().finish()
            return
        }
        mDisposable.add(mConversationFacade
            .getFullConversationList(mConversationFacade.currentAccountSubject, query)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { list -> adapter?.update(list) })
    }

    override fun onStop() {
        super.onStop()
        mDisposable.clear()
    }

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        mPendingIntent = requireActivity().intent
    }

    override fun onDestroy() {
        super.onDestroy()
        mPendingIntent = null
        adapter = null
    }

    class ShareMediaViewHolder(itemView: View, parentDisposable: CompositeDisposable): RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView as MaterialCardView
        private val mediaImage: ImageView = itemView.findViewById(R.id.previewImage)
        private val mediaText: TextView = itemView.findViewById(R.id.previewText)
        private val mediaVideo: PreviewVideoView = itemView.findViewById(R.id.previewVideo)
        private val mediaDocument: ViewGroup = itemView.findViewById(R.id.previewDocumentLayout)
        private val mediaDocumentTitle: TextView = itemView.findViewById(R.id.previewDocumentTitle)
        private val mediaDocumentSize: TextView = itemView.findViewById(R.id.previewDocumentSize)
        private val colorLow = getColorFromAttribute(cardView.context, com.google.android.material.R.attr.colorSurfaceContainerLow)
        val disposable = CompositeDisposable().apply { parentDisposable.add(this) }

        fun bind(item: ContentUri.ShareItem, maxHeight: Int) {
            disposable.clear()
            mediaVideo.visibility = View.GONE
            mediaImage.visibility = View.GONE
            mediaText.visibility = View.GONE
            mediaDocument.visibility = View.GONE
            cardView.setCardBackgroundColor(colorLow)
            cardView.layoutTransition = null
            when {
                item.type.startsWith("text/") && item.text != null -> {
                    mediaText.visibility = View.VISIBLE
                    mediaText.text = item.text
                }
                item.type.startsWith("image/") -> {
                    mediaDocument.visibility = View.VISIBLE
                    mediaImage.visibility = View.VISIBLE
                    Glide.with(itemView)
                        .load(item.data)
                        .fallback(R.drawable.baseline_warning_24)
                        .addListener(object : RequestListener<Drawable?> {
                            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable?>, isFirstResource: Boolean) = false
                            override fun onResourceReady(resource: Drawable, model: Any, target: Target<Drawable?>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                                mediaDocument.visibility = View.GONE
                                mediaImage.visibility = View.INVISIBLE
                                mediaImage.doOnNextLayout {
                                    cardView.layoutTransition = LayoutTransition()
                                    mediaImage.visibility = View.VISIBLE
                                }
                                return false
                            }
                        })
                        .into(mediaImage)

                }
                item.type.startsWith("video/") -> {
                    mediaVideo.visibility = View.VISIBLE
                    try {
                        mediaVideo.setVideoURI(item.data!!)
                        mediaVideo.start()
                    } catch (_: Exception) {}
                    mediaVideo.setOnCompletionListener { mediaVideo.start() }
                }
                else -> {
                    val documentInfo = try {
                        item.data?.let { itemView.context.contentResolver.query(it, null, null, null, null) }?.use { cursor ->
                            if (cursor.moveToFirst())
                                Pair(cursor.getStringOrNull(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)),
                                    cursor.getLongOrNull(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE)))
                            else null
                        }
                    } catch (e: Exception) { null }

                    if (item.type == "application/pdf" && item.data != null) {
                        mediaDocument.visibility = View.VISIBLE
                        mediaDocumentTitle.text = ""
                        mediaDocumentSize.text = ""
                        mediaImage.visibility = View.INVISIBLE
                        disposable.add(Single.fromCallable { BitmapUtils.documentToBitmap(itemView.context, item.data, maxHeight = maxHeight)!! }
                            .subscribeOn(Schedulers.computation())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({ documentPreview ->
                                mediaImage.setImageBitmap(documentPreview)
                                mediaDocument.visibility = View.GONE
                                mediaImage.doOnNextLayout {
                                    cardView.layoutTransition = LayoutTransition()
                                    mediaImage.visibility = View.VISIBLE
                                }
                            }) { bindDefault(documentInfo) })
                    } else {
                        bindDefault(documentInfo)
                    }
                }
            }
        }

        private fun bindDefault(info: Pair<String?, Long?>?) {
            cardView.setCardBackgroundColor(getColorFromAttribute(cardView.context, com.google.android.material.R.attr.colorSurfaceContainerHighest))
            mediaImage.visibility = View.GONE
            mediaDocument.visibility = View.VISIBLE
            mediaDocumentTitle.text = info?.first
            mediaDocumentSize.text = info?.second?.let { size ->
                Formatter.formatFileSize(itemView.context, size)
            }
        }

        fun recycle() {
            disposable.clear()
            mediaVideo.setOnCompletionListener(null)
            try {
                mediaVideo.stopPlayback()
            } catch (_: Exception) {}
            mediaVideo.setVideoURI(null)
            mediaImage.setImageDrawable(null)
        }
    }

    class ShareMediaAdapter(val mediaList: List<ContentUri.ShareItem>, val maxHeight: Int, private val parentDisposable: CompositeDisposable) : Adapter<ShareMediaViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShareMediaViewHolder =
            ShareMediaViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_share_media, parent, false), parentDisposable)

        override fun onBindViewHolder(holder: ShareMediaViewHolder, position: Int) {
            holder.bind(mediaList[position], maxHeight)
        }

        override fun onViewRecycled(holder: ShareMediaViewHolder) {
            holder.recycle()
        }

        override fun getItemCount(): Int = mediaList.size
    }

    companion object {
        fun newInstance(): ShareWithFragment = ShareWithFragment()
    }
}