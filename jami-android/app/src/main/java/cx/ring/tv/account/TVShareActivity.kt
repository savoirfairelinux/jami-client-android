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
package cx.ring.tv.account

import dagger.hilt.android.AndroidEntryPoint
import androidx.fragment.app.FragmentActivity
import android.os.Bundle
import cx.ring.R
import net.jami.model.Uri
import net.jami.services.AccountService
import javax.inject.Inject

@AndroidEntryPoint
class TVShareActivity : FragmentActivity() {

    @Inject
    lateinit
    var mAccountService: AccountService

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tv_activity_share)
        val f = TVShareFragment.newInstance(Uri.fromString(mAccountService.currentAccount?.uri!!))
        supportFragmentManager.beginTransaction()
            .replace(R.id.share_frag, f)
            .setReorderingAllowed(true)
            .commitNow()
    }

    companion object {
        const val SHARED_ELEMENT_NAME = "photo"
    }
}