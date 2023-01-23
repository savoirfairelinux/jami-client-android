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
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.ViewFinder
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matcher
import java.lang.reflect.Field

// Cf https://stackoverflow.com/questions/50628219/is-it-possible-to-use-espressos-idlingresource-to-wait-until-a-certain-view-app
/**
 * @param viewMatcher The matcher to find the view.
 * @param idleMatcher The matcher condition to be fulfilled to be considered idle.
 */
class ViewIdlingResource(
        private val viewMatcher: Matcher<View?>?,
        private val idleMatcher: Matcher<View?>?
) : IdlingResource {

    private var resourceCallback: IdlingResource.ResourceCallback? = null

    override fun isIdleNow(): Boolean {
        val view: View? = getView(viewMatcher)
        val isIdle: Boolean = idleMatcher?.matches(view) ?: false
        if (isIdle) {
            resourceCallback?.onTransitionToIdle()
        }
        return isIdle
    }

    override fun registerIdleTransitionCallback(resourceCallback: IdlingResource.ResourceCallback?) {
        this.resourceCallback = resourceCallback
    }

    override fun getName(): String = "$this ${viewMatcher.toString()}"

    private fun getView(viewMatcher: Matcher<View?>?): View? {
        return try {
            val viewInteraction = Espresso.onView(viewMatcher)
            val finderField: Field? = viewInteraction.javaClass.getDeclaredField("viewFinder")
            finderField?.isAccessible = true
            val finder = finderField?.get(viewInteraction) as ViewFinder
            finder.view
        } catch (e: Exception) {
            null
        }
    }

}


/**
 * Waits for a matching View or throws an error if it's taking too long.
 */
fun waitUntilViewIsDisplayed(matcher: Matcher<View?>) {
    val idlingResource: IdlingResource = ViewIdlingResource(matcher, ViewMatchers.isDisplayed())
    try {
        IdlingRegistry.getInstance().register(idlingResource)
        // First call to onView is to trigger the idler.
        Espresso.onView(ViewMatchers.withId(0)).check(ViewAssertions.doesNotExist())
    } finally {
        IdlingRegistry.getInstance().unregister(idlingResource)
    }
}