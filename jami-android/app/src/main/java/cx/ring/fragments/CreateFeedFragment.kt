/*
 * Copyright (C) 2026 Savoir-faire Linux Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.view.ViewCompat
import androidx.core.view.isInvisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.R
import cx.ring.client.HomeActivity
import cx.ring.databinding.DialogCreateFeedBinding
import cx.ring.viewmodel.CreateFeedViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CreateFeedFragment : AppCompatDialogFragment() {
    private val viewModel: CreateFeedViewModel by viewModels()
    private var binding: DialogCreateFeedBinding? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext(), R.style.CreateFeedDialog)
        val binding = DialogCreateFeedBinding.inflate(LayoutInflater.from(builder.context))
        this.binding = binding
        ViewCompat.setAccessibilityPaneTitle(binding.root, getString(R.string.new_feed))
        binding.feedName.setText(viewModel.name.value)
        binding.feedDescription.setText(viewModel.description.value)
        binding.feedName.doAfterTextChanged {
            viewModel.setName(it?.toString().orEmpty())
            binding.feedNameLayout.error = null
        }
        binding.feedDescription.doAfterTextChanged {
            viewModel.setDescription(it?.toString().orEmpty())
            binding.feedNameLayout.error = null
        }
        binding.feedDescription.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                confirm()
                true
            } else false
        }
        binding.createFeed.setOnClickListener { confirm() }
        binding.cancel.setOnClickListener { dismiss() }
        return builder
            .setView(binding.root)
            .create()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    val binding = binding ?: return@collect
                    val creating = state == CreateFeedViewModel.State.Creating
                    isCancelable = !creating
                    binding.feedName.isEnabled = !creating
                    binding.feedDescription.isEnabled = !creating
                    binding.createFeed.isEnabled = !creating
                    binding.cancel.isEnabled = !creating
                    binding.creationProgress.isInvisible = !creating
                    when (state) {
                        is CreateFeedViewModel.State.Created -> {
                            (requireActivity() as HomeActivity)
                                .startConversation(viewModel.accountId, state.uri)
                            dismiss()
                        }
                        CreateFeedViewModel.State.Failed ->
                            binding.feedNameLayout.error = getString(R.string.generic_error)
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun confirm() {
        val binding = binding ?: return
        if (binding.feedName.text.isNullOrBlank()) {
            binding.feedNameLayout.error = getString(R.string.rename_error)
            binding.feedName.requestFocus()
            return
        }
        viewModel.create()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "CreateFeed"

        fun newInstance(accountId: String) = CreateFeedFragment().apply {
            arguments = Bundle().apply {
                putString(CreateFeedViewModel.ARG_ACCOUNT_ID, accountId)
            }
        }
    }
}
