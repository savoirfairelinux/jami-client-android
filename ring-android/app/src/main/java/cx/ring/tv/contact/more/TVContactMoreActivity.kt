/*
 * Copyright (C) 2004-2020 Savoir-faire Linux Inc.
 *
 * Author: AmirHossein Naghshzan <amirhossein.naghshzan@savoirfairelinux.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.tv.contact.more

import dagger.hilt.android.AndroidEntryPoint
import androidx.fragment.app.FragmentActivity
import android.os.Bundle
import cx.ring.R

@AndroidEntryPoint
class TVContactMoreActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tv_activity_contact_more)
    }

    companion object {
        const val CONTACT_REQUEST_URI = "uri"
    }
}