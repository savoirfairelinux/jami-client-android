package cx.ring.client

import android.app.Application
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import cx.ring.R
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28, 34])
class ConversationChooserTest {
    private lateinit var controller: ActivityController<AppCompatActivity>

    @Before
    fun setUp() {
        controller = Robolectric.buildActivity(AppCompatActivity::class.java)
        controller.get().setTheme(R.style.AppTheme)
        controller.setup()
    }

    @After
    fun tearDown() {
        controller.pause().stop().destroy()
    }

    @Test
    fun selectingColorReturnsSwatchColorAndDismisses() {
        val colors = mutableListOf<Int>()
        val fragment = ColorChooserBottomSheet { colors.add(it) }
        fragment.showNow(controller.get().supportFragmentManager, "color")
        shadowOf(Looper.getMainLooper()).idle()
        val dialog = fragment.requireDialog()
        val item = item(fragment.requireView() as RecyclerView, 12)
        val expected = controller.get().getColor(R.color.conversation_palette_red)
        assertEquals(expected, ImageViewCompat.getImageTintList(item.itemView as ImageView)!!.defaultColor)
        item.itemView.performClick()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(listOf(expected), colors)
        assertFalse(dialog.isShowing)
    }

    @Test
    fun cancellingColorDoesNotSelectAnything() {
        val colors = mutableListOf<Int>()
        val fragment = ColorChooserBottomSheet { colors.add(it) }
        fragment.showNow(controller.get().supportFragmentManager, "color")
        fragment.dismiss()
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(colors.isEmpty())
    }

    @Test
    fun selectingEmojiReturnsDisplayedSymbolAndDismisses() {
        val emojis = mutableListOf<String?>()
        val fragment = EmojiChooserBottomSheet { emojis.add(it) }
        fragment.showNow(controller.get().supportFragmentManager, "emoji")
        shadowOf(Looper.getMainLooper()).idle()
        val dialog = fragment.requireDialog()
        val item = item(fragment.requireView() as RecyclerView, 4)
        val expected = controller.get().getString(R.string.default_emoji_5)
        assertEquals(expected, (item.itemView as TextView).text.toString())
        item.itemView.performClick()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(listOf(expected), emojis)
        assertFalse(dialog.isShowing)
    }

    @Test
    fun cancellingEmojiDoesNotSelectAnything() {
        val emojis = mutableListOf<String?>()
        val fragment = EmojiChooserBottomSheet { emojis.add(it) }
        fragment.showNow(controller.get().supportFragmentManager, "emoji")
        fragment.dismiss()
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(emojis.isEmpty())
    }

    private fun item(recycler: RecyclerView, position: Int): RecyclerView.ViewHolder {
        @Suppress("UNCHECKED_CAST")
        val adapter = recycler.adapter as RecyclerView.Adapter<RecyclerView.ViewHolder>
        return adapter.createViewHolder(recycler, adapter.getItemViewType(position)).also {
            adapter.bindViewHolder(it, position)
        }
    }
}