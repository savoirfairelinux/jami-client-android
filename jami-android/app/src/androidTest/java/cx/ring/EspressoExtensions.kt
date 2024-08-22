package cx.ring

import android.content.Intent
import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.net.toFile
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

fun imageFileHasMetadata(tag: String, metadata: String): Matcher<Intent> = object : TypeSafeMatcher<Intent>() {

    override fun describeTo(description: Description) {
        description.appendText("Intent with URI: ")
    }

    override fun matchesSafely(intent: Intent): Boolean {
        val uri = intent.data ?: return false

        // todo: get file by uri and check the exif metadata. if contains the specified string: return true

        //val metadata = AndroidFileUtils.getExifMetadata(intent, uri, tag)

        //val file = uri.toFile()

//        try {
//            val outStream = file.inputStream()
//        }
        return false
    }

}