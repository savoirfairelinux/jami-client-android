/*
 *  Copyright (C) 2004-2024 Savoir-faire Linux Inc.
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
package cx.ring.account

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import cx.ring.databinding.FragAccJamiBackupBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class JamiImportBackupFragment : Fragment() {
    private val model: AccountCreationViewModel by activityViewModels()
    private var binding: FragAccJamiBackupBinding? = null

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View = FragAccJamiBackupBinding.inflate(inflater, container, false).apply {

        linkButton.setOnClickListener { createAccount() }
        existingPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                model.model.password = s.toString()
            }
        })

        binding = this
    }.root

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }


//    override fun showPin(show: Boolean) {
//        val binding = binding ?: return
//        binding.linkButton.setText(if (show) R.string.account_link_device else R.string.account_link_archive_button)
//        binding!!.linkButton.isEnabled = enable
//    }

    fun createAccount() {
        (activity as AccountWizardActivity?)?.createAccount()
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(binding!!.existingPassword.windowToken, 0)
    }

    companion object {
        val TAG = JamiImportBackupFragment::class.simpleName!!
    }
}