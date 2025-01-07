/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring

import android.view.View
import android.view.ViewTreeObserver
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.util.TreeIterables
import org.hamcrest.Matcher
import org.hamcrest.Matchers.any
import org.hamcrest.StringDescription

/**
 * https://stackoverflow.com/questions/59141757/android-espresso-ui-test-use-idling-resource-to-wait-for-an-element-on-the-scr
 */
class ViewPropertyChangeCallback(
    private val matcher: Matcher<View>,
    private val view: View
) : IdlingResource, ViewTreeObserver.OnDrawListener {

    private lateinit var callback: IdlingResource.ResourceCallback
    private var matched = false

    override fun getName() = "View property change callback"

    override fun isIdleNow() = matched

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback) {
        this.callback = callback
    }

    override fun onDraw() {
        matched = matcher.matches(view)
        callback.onTransitionToIdle()
    }
}

fun waitUntil(matcher: Matcher<View>) = object : ViewAction {

    override fun getConstraints(): Matcher<View> {
        return any(View::class.java)
    }

    override fun getDescription(): String {
        return StringDescription().let {
            matcher.describeTo(it)
            "wait until: $it"
        }
    }

    override fun perform(uiController: UiController, view: View) {
        if (!matcher.matches(view)) {
            ViewPropertyChangeCallback(matcher, view).run {
                try {
                    IdlingRegistry.getInstance().register(this)
                    view.viewTreeObserver.addOnDrawListener(this)
                    uiController.loopMainThreadUntilIdle()
                } finally {
                    view.viewTreeObserver.removeOnDrawListener(this)
                    IdlingRegistry.getInstance().unregister(this)
                }
            }
        }
    }
}

/**
 * Code from: https://stackoverflow.com/a/56499223/12911704
 * Perform action of waiting for a certain view within a single root view
 * @param matcher Generic Matcher used to find our view
 */
fun searchFor(matcher: Matcher<View>): ViewAction {
    return object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            return isRoot()
        }

        override fun getDescription(): String {
            return "searching for view $matcher in the root view"
        }

        override fun perform(uiController: UiController, view: View) {
            var tries = 0
            val childViews: Iterable<View> = TreeIterables.breadthFirstViewTraversal(view)
            // Look for the match in the tree of child views
            childViews.forEach {
                tries++
                if (matcher.matches(it)) {
                    // found the view
                    return
                }
            }
            throw NoMatchingViewException.Builder()
                .withRootView(view)
                .withViewMatcher(matcher)
                .build()
        }
    }
}

/**
 * Code from: https://stackoverflow.com/a/56499223/12911704
 * Perform action of implicitly waiting for a certain view.
 * This differs from EspressoExtensions.searchFor in that,
 * upon failure to locate an element, it will fetch a new root view
 * in which to traverse searching for our @param match
 *
 * @param viewMatcher ViewMatcher used to find our view
 */
fun waitForView(
    viewMatcher: Matcher<View>,
    waitMillis: Int = 5000,
    waitMillisPerTry: Long = 100,
): ViewInteraction {
    // Derive the max tries
    val maxTries = waitMillis / waitMillisPerTry.toInt()
    var tries = 0
    for (i in 0..maxTries)
        try {
            // Track the amount of times we've tried
            tries++
            // Search the root for the view
            Espresso.onView(isRoot()).perform(searchFor(viewMatcher))
            // If we're here, we found our view. Now return it
            return Espresso.onView(viewMatcher)
        } catch (e: Exception) {
            if (tries == maxTries) {
                throw e
            }
            Thread.sleep(waitMillisPerTry)
        }
    throw Exception("Error finding a view matching $viewMatcher")
}