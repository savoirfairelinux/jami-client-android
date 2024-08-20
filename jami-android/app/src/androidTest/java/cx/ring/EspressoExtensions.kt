package cx.ring

import android.graphics.Bitmap
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.graphics.drawable.toBitmap
import androidx.test.espresso.Root
import com.google.android.material.textfield.TextInputLayout
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher


fun hasTextInputLayoutError(expectedErrorText: String): Matcher<View> = object : TypeSafeMatcher<View>() {

    override fun describeTo(description: Description?) { }

    override fun matchesSafely(item: View?): Boolean {
        if (item !is TextInputLayout) {
            return false
        }

        val error = item.error ?: return false
        return expectedErrorText == error.toString()
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

fun withImage(expectedBitmap: Bitmap): Matcher<View> = object : TypeSafeMatcher<View>() {

    override fun describeTo(description: Description) {
        description.appendText("imageview matcher")
    }

    override fun matchesSafely(view: View): Boolean {
        if (view !is ImageView || view.drawable == null) {
            return false
        }

        val btm = view.drawable.toBitmap()
        android.util.Log.i("devdebug","actual size = ${btm.height} ${btm.width}")
        android.util.Log.i("devdebug","epecetd size = ${expectedBitmap.height} ${expectedBitmap.width}")

        return view.drawable.toBitmap().sameAs(expectedBitmap)
    }

}