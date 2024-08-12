package cx.ring

import android.view.View
import androidx.annotation.StringRes
import com.google.android.material.textfield.TextInputLayout
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher


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
