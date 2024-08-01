package cx.ring

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.widget.ImageView
import androidx.annotation.StringRes
import androidx.core.graphics.drawable.toBitmap
import com.google.android.material.textfield.TextInputLayout
import net.jami.utils.Log
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import java.io.InputStream


/**
 * Check if a TextInputLayout has an error with the expected text
 * @param resourceId Resource id of the expected error. If null, check if there is no error.
 */
fun hasTextInputLayoutError(@StringRes resourceId: Int?): Matcher<View> =
    object : TypeSafeMatcher<View>() {

        override fun describeTo(description: Description?) {}

        override fun matchesSafely(item: View?): Boolean {
            if (item !is TextInputLayout) {
                return false
            }
            // If resourceId is null, we just check if there is no error
            if (resourceId == null) return item.error == null
            // Else we check if the error is the expected one
            val expectedErrorText = item.context.getString(resourceId)
            return expectedErrorText == item.error.toString()
        }
    }

fun withImageUri(uri: Uri): Matcher<View> {
    return object : TypeSafeMatcher<View>() {

        override fun describeTo(description: Description) {
            description.appendText("with image from URI: ")
            description.appendValue(uri)
        }

        override fun matchesSafely(view: View): Boolean {
            if (view !is ImageView) {
                Log.w("devdebug", "View is not an ImageView")
                return false
            }
            if (view.drawable == null) {
                Log.w("devdebug", "View has no drawable")
                return false
            }

            val actualBitmap = getBitmapFromDrawable(view.drawable)
            val expectedBitmap = loadBitmapFromUri(view.context, uri)
            return expectedBitmap.sameAs(actualBitmap)
        }

        private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap {
            val inputStream: InputStream = context.contentResolver.openInputStream(uri)!!
            return BitmapFactory.decodeStream(inputStream)
        }

        private fun getBitmapFromDrawable(drawable: Drawable): Bitmap {
            return if (drawable is BitmapDrawable) drawable.bitmap
            else drawable.toBitmap()
        }
    }
}

class DrawableMatcher internal constructor(private val expectedId: Int) :
    TypeSafeMatcher<View?>(View::class.java) {
    private var resourceName: String? = null

    override fun matchesSafely(target: View?): Boolean {
        if (target !is ImageView) {
            return false
        }
        val resources = target.getContext().resources
        val expectedDrawable = resources.getDrawable(expectedId)
        resourceName = resources.getResourceEntryName(expectedId)

        if (expectedDrawable == null) {
            return false
        }

        val bitmap = getBitmap(target.drawable)
        val otherBitmap = getBitmap(expectedDrawable)
        return bitmap.sameAs(otherBitmap)
    }

    private fun getBitmap(drawable: Drawable): Bitmap {
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    override fun describeTo(description: Description) {
        description.appendText("with drawable from resource id: ")
        description.appendValue(expectedId)
        if (resourceName != null) {
            description.appendText("[")
            description.appendText(resourceName)
            description.appendText("]")
        }
    }
}