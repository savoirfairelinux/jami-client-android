/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
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
package cx.ring.tv.main

import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Bundle
import android.widget.FrameLayout
import android.view.Gravity
import android.view.View
import android.widget.ProgressBar
import androidx.fragment.app.Fragment

class SpinnerFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val progressBar = ProgressBar(container!!.context)
        if (container is FrameLayout) {
            val layoutParams = FrameLayout.LayoutParams(SPINNER_WIDTH, SPINNER_HEIGHT, Gravity.CENTER)
            progressBar.layoutParams = layoutParams
        }
        return progressBar
    }

    companion object {
        private const val SPINNER_WIDTH = 100
        private const val SPINNER_HEIGHT = 100
    }
}