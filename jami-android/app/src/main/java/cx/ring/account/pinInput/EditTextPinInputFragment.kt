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
package cx.ring.account.pinInput

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.textfield.TextInputEditText
import cx.ring.R
import cx.ring.databinding.EditTextPinInputBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditTextPinInputFragment : Fragment() {

    private val viewModel: EditTextPinInputViewModel by viewModels({ requireParentFragment() })
    private var binding: EditTextPinInputBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = EditTextPinInputBinding.inflate(inflater, container, false).apply {
        binding = this
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // to have the text entered in the text field
        val enterPinEditText: TextInputEditText = view.findViewById(R.id.enter_pin)
        val startingAt = 17
        enterPinEditText.doOnTextChanged { pin, _, _, _ ->
            viewModel.checkPin(pin.toString()).let {
                // if the pin is not valid and it is at length 17 (format of the pin) there is an
                // error
                if (it == PinValidity.ERROR && enterPinEditText.length() == startingAt) {
                    showErrorPanel()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun showErrorPanel() {
        binding?.enterPin?.error = getString(R.string.error_format_not_supported)
        binding?.enterPin?.requestFocus()
    }

}