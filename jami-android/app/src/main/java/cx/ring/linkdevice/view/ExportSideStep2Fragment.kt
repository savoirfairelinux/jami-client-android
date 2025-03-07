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

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.R
import cx.ring.databinding.FragmentExportSideStep2Binding

class ExportSideStep2Fragment : Fragment() {
    private var _binding: FragmentExportSideStep2Binding? = null
    private val binding get() = _binding!!
    private var _callback: OnReviewCallback? = null
    private val callback get() = _callback!!

    interface OnReviewCallback {
        fun onIdentityConfirmation(confirm: Boolean)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        _callback = requireActivity() as OnReviewCallback
    }

    // Silencing the warning about accessibility. We just want to disable the view.
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentExportSideStep2Binding.inflate(inflater, container, false)
        .apply { _binding = this }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cancel.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(resources.getString(R.string.link_device_dialog_exit_title))
                .setMessage(resources.getString(R.string.link_device_dialog_exit_export_body_1))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    Log.i(TAG, "Cancel button clicked.")
                    callback.onIdentityConfirmation(false)
                }
                .setNegativeButton(android.R.string.cancel) { _, _ -> }
                .show()
        }

        binding.confirm.setOnClickListener {
            Log.i(TAG, "Confirm button clicked.")
            showLoading()
            callback.onIdentityConfirmation(true)
        }
    }

    fun showIP(ip: String) {
        Log.i(TAG, "Showing IP: $ip")
        binding.passwordContainer.visibility = View.GONE
        binding.confirm.visibility = View.VISIBLE
        binding.cancel.visibility = View.VISIBLE
        binding.progress.visibility = View.INVISIBLE
        binding.locationContainer.visibility = View.VISIBLE
        binding.advice.text =
            getString(R.string.export_side_step2_advice_ip_only)
        binding.location.text =
            getString(R.string.export_side_step2_ip_only, ip)
    }

    fun showPasswordProtection() {
        Log.i(TAG, "Showing password protection.")
        binding.passwordContainer.visibility = View.VISIBLE
        binding.locationContainer.visibility = View.GONE
    }

    private fun showLoading() {
        binding.confirm.visibility = View.INVISIBLE
        binding.cancel.visibility = View.INVISIBLE
        binding.progress.visibility = View.VISIBLE
    }

    companion object {
        private val TAG = ExportSideStep2Fragment::class.java.simpleName
    }
}