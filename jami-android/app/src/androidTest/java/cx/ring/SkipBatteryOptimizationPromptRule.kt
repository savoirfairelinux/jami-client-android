/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 USA.
 */
package cx.ring

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import cx.ring.client.HomeActivity
import org.junit.rules.ExternalResource

/**
 * The flavors without Firebase ask the user for an exemption from battery optimizations the
 * first time [cx.ring.client.HomeActivity] is resumed. The resulting dialog covers the activity
 * and makes every UI test fail. Runtime permissions are neutralized by `GrantPermissionRule`,
 * but the exemption is not a runtime permission, so mark the prompt as already shown instead.
 *
 * Declare this rule with a lower `order` than the `ActivityScenarioRule` so that it runs before
 * the activity is launched.
 */
class SkipBatteryOptimizationPromptRule : ExternalResource() {

    override fun before() {
        InstrumentationRegistry.getInstrumentation().targetContext
            .getSharedPreferences(HomeActivity.PREFS_BATTERY_OPT, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(HomeActivity.PREF_BATTERY_OPT_ASKED, true)
            .commit()
    }
}
