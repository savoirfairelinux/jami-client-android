package cx.ring

import android.view.View
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.test.espresso.Root
import com.google.android.material.textfield.TextInputLayout
import cx.ring.views.MessageBubble
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher


fun hasTextInputLayoutError(expectedErrorText: String): Matcher<View> =
    object : TypeSafeMatcher<View>() {

        override fun describeTo(description: Description?) {}

        override fun matchesSafely(item: View?): Boolean =
            if (item !is TextInputLayout) false
            else if (item.error == null) false
            else expectedErrorText == item.error.toString()
    }

fun isDialogWithTitle(@StringRes dialogTitle: Int): Matcher<Root> =
    object : TypeSafeMatcher<Root>() {

        override fun describeTo(description: Description) {}

        override fun matchesSafely(root: Root): Boolean {
            val rootView: View = root.decorView
            val str = rootView.context.getString(dialogTitle)
            val tv = rootView.findViewById<TextView>(androidx.appcompat.R.id.alertTitle)
            return if (tv == null) false else tv.text == str
        }
    }

fun hasBackgroundColor(@ColorRes conversationColor: Int): Matcher<in View> =
    object : TypeSafeMatcher<View>() {

        override fun describeTo(description: Description?) {}

        override fun matchesSafely(item: View): Boolean {
            if (item !is MessageBubble) return false
            val messageBubbleBackgroundColor = item.backgroundTintList?.defaultColor ?: return false
            val expectedColor = item.context!!.getColor(conversationColor)
            return messageBubbleBackgroundColor == expectedColor
        }
    }