package cx.ring

import android.view.View
import androidx.test.espresso.Espresso
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.util.TreeIterables
import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.hamcrest.Factory
import org.hamcrest.Matcher


/**
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

fun doOnView(matcher: Matcher<View>, vararg actions: ViewAction) {
    actions.forEach {
        waitForView(matcher).perform(it)
    }
}

fun assertOnView(matcher: Matcher<View>, vararg assertions: ViewAssertion) {
    assertions.forEach {
        waitForView(matcher).check(it)
    }
}

/**
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

/**
 * Created by stost on 15.05.14.
 * Matches any view. But only on first match()-call.
 */
class FirstViewMatcher : BaseMatcher<View?>() {
    init {
        matchedBefore = false
    }

    override fun matches(o: Any): Boolean {
        if (matchedBefore) {
            return false
        } else {
            matchedBefore = true
            return true
        }
    }

    override fun describeTo(description: Description?) {
        description?.appendText(" is the first view that comes along ")
    }

    companion object {
        var matchedBefore: Boolean = false

        @Factory
        fun firstView(): Matcher<View> {
            return FirstViewMatcher() as Matcher<View>
        }
    }
}

/**
 * Created by stost on 15.05.14.
 * Matches any view. But only on first match()-call.
 */
class SecondViewMatcher : BaseMatcher<View?>() {
    init {
        matchedBefore = false
    }

    override fun matches(o: Any): Boolean {
        if (matchedBefore) {
            return true
        } else {
            matchedBefore = true
            return false
        }
    }

    override fun describeTo(description: Description?) {
        description?.appendText(" is the first view that comes along ")
    }

    companion object {
        var matchedBefore: Boolean = false

        @Factory
        fun secondView(): Matcher<View> {
            return SecondViewMatcher() as Matcher<View>
        }
    }
}
