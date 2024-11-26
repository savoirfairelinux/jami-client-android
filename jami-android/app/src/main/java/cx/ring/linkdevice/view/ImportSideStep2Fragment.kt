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
package cx.ring.linkdevice.view

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import cx.ring.R
import cx.ring.databinding.FragmentImportSideStep2Binding
import cx.ring.linkdevice.viewmodel.ImportSideViewModel


class ImportSideStep2Fragment : Fragment() {
    private var _binding: FragmentImportSideStep2Binding? = null
    private val binding get() = _binding!!
    private var _callback: OnAuthenticationCallback? = null
    private val callback get() = _callback!!

    interface OnAuthenticationCallback {
        fun onAuthentication(password: String)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (requireActivity() is OnAuthenticationCallback) {
            _callback = requireActivity() as OnAuthenticationCallback
        } else {
            throw RuntimeException("Parent fragment must implement ${OnAuthenticationCallback::class.java.simpleName}")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        FragmentImportSideStep2Binding.inflate(inflater, container, false)
            .apply { _binding = this }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showActionRequired()

        binding.connect.setOnClickListener {
            Log.i(TAG, "Connect button clicked.")
            showLoading()
            callback.onAuthentication(binding.password.text.toString())
        }
    }

    fun showActionRequired() {
        Log.i(TAG, "Showing action required.")
        binding.actionRequired.visibility = View.VISIBLE
        binding.passwordContainer.visibility = View.GONE
        binding.unlockingContainer.visibility = View.GONE
        binding.identityContainer.visibility = View.GONE
        binding.connect.visibility = View.GONE
    }

    fun showAuthentication(
        needPassword: Boolean,
        jamiId: String,
        registeredName: String?,
        error: ImportSideViewModel.InputError?
    ) {
        binding.unlockingContainer.visibility = View.GONE
        binding.identityContainer.visibility = View.VISIBLE
        binding.connect.visibility = View.VISIBLE
        binding.registeredName.isGone = registeredName.isNullOrEmpty()
        binding.actionRequired.visibility = View.GONE
        binding.passwordContainer.isGone = !needPassword

        binding.jamiId.text = jamiId
        binding.registeredName.text = registeredName

        binding.passwordError.isVisible = error != null

        if (error != null && needPassword) {
            Log.i(TAG, "Showing password.")
            binding.passwordError.text =
                when (error) {
                    ImportSideViewModel.InputError.BAD_PASSWORD -> getString(R.string.link_device_error_bad_password)
                    ImportSideViewModel.InputError.UNKNOWN -> getString(R.string.link_device_error_unknown)
                }
        }
    }

    private fun showLoading() {
        binding.identityContainer.visibility = View.GONE
        binding.actionRequired.visibility = View.INVISIBLE
        binding.passwordContainer.visibility = View.INVISIBLE
        binding.unlockingContainer.visibility = View.VISIBLE
        binding.connect.visibility = View.GONE
    }

    companion object {
        private val TAG = ImportSideStep2Fragment::class.java.simpleName
    }
}