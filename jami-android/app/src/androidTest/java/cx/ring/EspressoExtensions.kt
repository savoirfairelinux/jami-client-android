package cx.ring

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.View
import android.widget.TextView
import android.widget.ImageView
import androidx.annotation.StringRes
import androidx.test.espresso.Root
import com.google.android.material.textfield.TextInputLayout
import cx.ring.views.AvatarDrawable
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

fun isDialogWithTitle(@StringRes dialogTitle: Int): Matcher<Root> = object : TypeSafeMatcher<Root>() {

    override fun describeTo(description: Description) {
        description.appendText("Dialog matcher by title")
    }

    override fun matchesSafely(root: Root): Boolean {
        val rootView: View = root.decorView
        val str = rootView.context.getString(dialogTitle)
        val tv = rootView.findViewById<TextView>(androidx.appcompat.R.id.alertTitle)
        return if (tv == null) false else tv.text == str
    }

}

fun withImageUri(uri: Uri): Matcher<View> {
    return object : TypeSafeMatcher<View>() {

        private val TAG = "EspressoExtensions:withImageUri"

        override fun describeTo(description: Description) {
            description.appendText("with image from URI: `$uri`")
        }

        override fun matchesSafely(view: View): Boolean {
            if (view !is ImageView) {
                Log.e(TAG, "View is not an ImageView.")
                return false
            }
            if (view.drawable == null) {
                Log.e(TAG, "View has no drawable.")
                return false
            }
            if (view.drawable !is AvatarDrawable) {
                Log.e(TAG, "Drawable is not an AvatarDrawable.")
                return false
            }

            val drawable = view.drawable as AvatarDrawable
            val drawableBitmaps = drawable.getBitmap()

            if (drawableBitmaps == null) {
                Log.e(TAG, "Drawable has no bitmap.")
                return false
            }
            if (drawableBitmaps.isEmpty()) {
                Log.e(TAG, "Drawable has no photo.")
                return false
            }

            val actualBitmap = drawableBitmaps[0]
            val expectedBitmap = loadBitmapFromUri(view.context, uri)

            return expectedBitmap.sameAs(actualBitmap)
        }

        private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap {
            val inputStream: InputStream = context.contentResolver.openInputStream(uri)!!
            return BitmapFactory.decodeStream(inputStream)
        }
    }
}