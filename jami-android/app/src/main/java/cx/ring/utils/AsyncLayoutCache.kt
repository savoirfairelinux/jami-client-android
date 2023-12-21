package cx.ring.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.asynclayoutinflater.view.AsyncLayoutInflater
import cx.ring.R
import cx.ring.adapters.MessageType
import cx.ring.databinding.FragConversationBinding
import java.util.concurrent.ConcurrentLinkedQueue

class AsyncLayoutCache(
    context: Context,
    @LayoutRes private val layout: Int,
    private val parent: ViewGroup? = null,
    private val cacheCount : Int = 1
) {
    private val cache = ConcurrentLinkedQueue<View>()
    private val inflater = AsyncLayoutInflater(context)
    private val inflaterSync = LayoutInflater.from(context)

    init {
        for (i in 0 until cacheCount) {
            inflater.inflate(layout, parent) { view, _, _ ->
                recycleView(view)
            }
        }
    }

    fun getView(): View {
        val v = cache.poll() ?: inflaterSync.inflate(layout, parent, false)
        inflater.inflate(layout, parent) { view, _, _ ->
            recycleView(view)
        }
        return v
    }

    fun recycleView(view: View) {
        if (cache.size < cacheCount) {
            cache.add(view)
        }
    }
}

class ConversationCacheData(val binding: FragConversationBinding, val itemCaches: List<AsyncLayoutCache>)

class AsyncConversationCache(
    context: Context,
    @LayoutRes private val layout: Int = R.layout.frag_conversation,
    private val parent: ViewGroup? = null,
    private val cacheCount : Int = 1
) {
    private val cache = ConcurrentLinkedQueue<ConversationCacheData>()
    private val inflater = AsyncLayoutInflater(context)
    private val inflaterSync = LayoutInflater.from(context)

    init {
        for (i in 0 until cacheCount) {
            inflater.inflate(layout, parent) { view, _, _ ->
                recycleView(FragConversationBinding.bind(view), MessageType.entries.filter { it != MessageType.INVALID }
                    .map { AsyncLayoutCache(context, it.layout, view as ViewGroup, 8) })
            }
        }
    }

    fun getData(): ConversationCacheData = cache.poll() ?: ConversationCacheData(
        FragConversationBinding.inflate(inflaterSync, parent, false),
        MessageType.entries.filter { it != MessageType.INVALID }
            .map { AsyncLayoutCache(inflaterSync.context, it.layout, parent, 8) }
    )

    fun recycleView(binding: FragConversationBinding, itemCaches: List<AsyncLayoutCache>) {
        if (cache.size < cacheCount) {
            cache.add(ConversationCacheData(binding, itemCaches))
        }
    }
}
