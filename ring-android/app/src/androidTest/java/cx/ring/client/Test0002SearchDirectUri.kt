/*
 *  Copyright (C) 20022 Savoir-faire Linux Inc.
 *
 *  Authors: Sébastien Blin <sebastien.blin@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.client
import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import cx.ring.R
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class Test0002SearchDirectUri {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityScenarioRule(HomeActivity::class.java)

    @Test
    /**
     * This test search a random URI in the search bar. This should return an item from the public directory
     * (valid because it's a valid uri)
     */
    fun searchDirectUri() {
        /*val actionMenuItemView = onView(
                allOf(withId(R.id.menu_contact_search), withContentDescription("Search name or phone number…"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.main_toolbar),
                                        2),
                                0),
                        isDisplayed()))
        actionMenuItemView.perform(click())

        val editText = onView(
                allOf(withId(R.id.search_src_text)))
        editText.perform(longClick())
        editText.perform(replaceText("000000069ecabfecf731e1c98eafc4b592ab0000"), closeSoftKeyboard())
        editText.check(matches(withText("000000069ecabfecf731e1c98eafc4b592ab0000")))

        waitUntilViewIsDisplayed(allOf(withId(R.id.conv_participant), withText("000000069ecabfecf731e1c98eafc4b592ab0000")))
        val textView = onView(
                allOf(withId(R.id.conv_participant), withText("000000069ecabfecf731e1c98eafc4b592ab0000"),
                        withParent(withParent(withId(R.id.item_layout))),
                        isDisplayed()))
        textView.check(matches(withText("000000069ecabfecf731e1c98eafc4b592ab0000")))*/
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
