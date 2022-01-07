package cx.ring.tv.main


import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import cx.ring.R
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class HomeActivityTest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(HomeActivity::class.java)

    @Test
    fun homeActivityTest() {
        val materialButton = onView(
                allOf(withId(R.id.ring_create_btn), withText("Create a Jami account"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(`is`("android.widget.ScrollView")),
                                        0),
                                2)))
        materialButton.perform(scrollTo(), click())

        val materialButton2 = onView(
                allOf(withId(R.id.skip), withText("Skip choosing username"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(`is`("androidx.cardview.widget.CardView")),
                                        0),
                                3),
                        isDisplayed()))
        materialButton2.perform(click())

        val materialButton3 = onView(
                allOf(withId(R.id.create_account), withText("Create account"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(`is`("androidx.cardview.widget.CardView")),
                                        0),
                                4),
                        isDisplayed()))
        materialButton3.perform(click())

        val materialButton4 = onView(
                allOf(withId(R.id.skip_create_account), withText("Skip and do this later"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(`is`("android.widget.LinearLayout")),
                                        3),
                                1),
                        isDisplayed()))
        materialButton4.perform(click())

        val actionMenuItemView = onView(
                allOf(withId(R.id.menu_contact_search), withContentDescription("Search name or phone number…"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.main_toolbar),
                                        2),
                                0),
                        isDisplayed()))
        actionMenuItemView.perform(click())

        val searchAutoComplete = onView(
                allOf(withId(R.id.search_src_text),
                        childAtPosition(
                                allOf(withId(R.id.search_plate),
                                        childAtPosition(
                                                withId(R.id.search_edit_frame),
                                                1)),
                                0),
                        isDisplayed()))
        searchAutoComplete.perform(longClick())

        val linearLayout = onView(
                allOf(withContentDescription("Paste"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(`is`("android.widget.RelativeLayout")),
                                        1),
                                0),
                        isDisplayed()))
        linearLayout.perform(click())

        val searchAutoComplete2 = onView(
                allOf(withId(R.id.search_src_text),
                        childAtPosition(
                                allOf(withId(R.id.search_plate),
                                        childAtPosition(
                                                withId(R.id.search_edit_frame),
                                                1)),
                                0),
                        isDisplayed()))
        searchAutoComplete2.perform(replaceText("000000069ecabfecf731e1c98eafc4b592ab0000"), closeSoftKeyboard())
    }

    private fun childAtPosition(
            parentMatcher: Matcher<View>, position: Int): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }
}
