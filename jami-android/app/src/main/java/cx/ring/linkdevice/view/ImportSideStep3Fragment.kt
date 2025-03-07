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

import cx.ring.linkdevice.viewmodel.AuthError
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import cx.ring.R
import cx.ring.databinding.FragmentImportSideStep3Binding

class ImportSideStep3Fragment : Fragment() {
    private var _binding: FragmentImportSideStep3Binding? = null
    private val binding get() = _binding!!
    private var _callback: OnResultCallback? = null
    private val callback get() = _callback!!

    interface OnResultCallback {
        fun onExit(returnCode: Int)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        _callback = requireActivity() as OnResultCallback
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentImportSideStep3Binding.inflate(inflater, container, false)
        .apply { _binding = this }.root

    fun showLoading() {
        Log.i(TAG, "Showing loading...")
        binding.details.text = getString(R.string.import_side_step3_body_loading)
        binding.status.visibility = View.GONE
        binding.loading.visibility = View.VISIBLE
        binding.exit.visibility = View.GONE
    }

    fun showDone() {
        Log.i(TAG, "Showing done.")
        binding.exit.setOnClickListener {
            Log.i(TAG, "Exit button clicked.")
            callback.onExit(0)
        }
        binding.details.text = getString(R.string.import_side_step3_body_done)
        binding.status.visibility = View.VISIBLE
        binding.loading.visibility = View.GONE
        binding.exit.visibility = View.VISIBLE
        binding.exit.text = getString(R.string.import_side_step3_go_to_account)
        binding.status.setImageResource(R.drawable.baseline_done_24)
        binding.status.setColorFilter(
            ContextCompat.getColor(
                requireContext(),
                android.R.color.holo_green_dark
            )
        )
    }

    fun showError(error: AuthError) {
        Log.i(TAG, "Showing error.")
        binding.exit.setOnClickListener {
            Log.i(TAG, "Exit button clicked.")
            callback.onExit(1)
        }
        binding.details.text = when (error) {
            AuthError.AUTHENTICATION -> getString(R.string.link_device_error_authentication)
            AuthError.NETWORK -> getString(R.string.link_device_error_network)
            AuthError.TIMEOUT -> getString(R.string.link_device_error_timeout)
            AuthError.CANCELED -> getString(R.string.link_device_error_canceled)
            AuthError.UNKNOWN -> getString(R.string.link_device_error_unknown)
        }
        binding.status.visibility = View.VISIBLE
        binding.loading.visibility = View.GONE
        binding.exit.visibility = View.VISIBLE
        binding.exit.text = getString(R.string.import_side_step3_exit)
        binding.status.setImageResource(R.drawable.baseline_error_24)
        binding.status.setColorFilter(
            ContextCompat.getColor(
                requireContext(),
                android.R.color.holo_red_dark
            )
        )
    }

    companion object {
        private val TAG = ImportSideStep3Fragment::class.java.simpleName
    }
}