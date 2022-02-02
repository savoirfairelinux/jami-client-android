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
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class Test0001AccountCreation {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityScenarioRule(HomeActivity::class.java)

    @Test
    /**
     * This test creates an account with "Foo" as a display name and check the MainView
     */
    fun testAccountCreation() {
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

        val textInputEditText = onView(
                allOf(withId(R.id.username)))
        textInputEditText.perform(click())
        textInputEditText.perform(replaceText("Foo"), closeSoftKeyboard())

        val materialButton4 = onView(
                allOf(withId(R.id.next_create_account), withText("Save Profile"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(`is`("android.widget.LinearLayout")),
                                        3),
                                0),
                        isDisplayed()))
        materialButton4.perform(click())

        waitUntilViewIsDisplayed(withId(R.id.title))

        val textView = onView(
                allOf(withId(R.id.title), withText("Foo"),
                        withParent(withParent(withId(R.id.spinner_toolbar))),
                        isDisplayed()))
        textView.check(matches(withText("Foo")))
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
