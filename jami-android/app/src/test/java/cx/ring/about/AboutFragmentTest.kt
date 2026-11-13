/*
 *  Copyright (C) 2004-2026 Savoir-faire Linux Inc.
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
package cx.ring.about

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import cx.ring.R
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AboutFragmentTest {

    @Test
    fun testAboutFragmentInflatesComposeView() {
        val controller = Robolectric.buildActivity(TestActivity::class.java).create()
        val activity = controller.get()
        activity.setTheme(R.style.AppTheme)
        controller.start().resume()

        val fragment = AboutFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(fragment, AboutFragment.TAG)
            .commitNow()

        val view = fragment.view
        assertNotNull(view)
        assertTrue("Fragment view must be a ComposeView", view is ComposeView)
    }

    class TestActivity : AppCompatActivity()
}
