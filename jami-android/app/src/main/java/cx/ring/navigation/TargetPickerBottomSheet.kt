/*
 *  Copyright (C) 2004-2026 Savoir-faire Linux Inc.
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
package cx.ring.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import cx.ring.R
import cx.ring.databinding.SheetTargetPickerBinding
import cx.ring.repositories.TargetRepository
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.Target
import net.jami.model.TargetCapability
import javax.inject.Inject

@AndroidEntryPoint
class TargetPickerBottomSheet : BottomSheetDialogFragment() {

    @Inject
    lateinit var targetRepository: TargetRepository

    private var binding: SheetTargetPickerBinding? = null
    private val disposable = CompositeDisposable()
    private var capability: TargetCapability = TargetCapability.CAN_MESSAGE
    private var customTitle: String? = null
    private var onTargetSelectedListener: ((Target) -> Unit)? = null
    private var onCallSelectedListener: ((Target, Boolean) -> Unit)? = null

    private val adapter = TargetAdapter(
        onTargetClick = { target ->
            onTargetSelectedListener?.invoke(target)
            dismiss()
        },
        onCallTargetClick = { target, hasVideo ->
            if (onCallSelectedListener != null) {
                onCallSelectedListener?.invoke(target, hasVideo)
            } else {
                onTargetSelectedListener?.invoke(target)
            }
            dismiss()
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val capName = it.getString(ARG_CAPABILITY)
            if (capName != null) {
                capability = TargetCapability.valueOf(capName)
            }
            customTitle = it.getString(ARG_TITLE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val b = SheetTargetPickerBinding.inflate(inflater, container, false)
        binding = b
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val b = binding ?: return

        customTitle?.let { b.pickerTitle.text = it }
        adapter.setCapability(capability)

        // Configure chip visibility and default selection based on capability
        when (capability) {
            TargetCapability.CAN_CALL -> {
                b.chipCircles.isVisible = false
                b.chipBoards.isVisible = false
                b.chipGroupTargets.check(R.id.chip_contacts)
                adapter.setFilterType(TargetFilterType.CONTACTS)
            }
            TargetCapability.CAN_MESSAGE -> {
                b.chipGroupTargets.check(R.id.chip_contacts)
                adapter.setFilterType(TargetFilterType.CONTACTS)
            }
            TargetCapability.CAN_POST -> {
                b.chipContacts.isVisible = false
            }
            TargetCapability.CAN_SHARE_DOC -> {
                // A document is written in one conversation: a contact's or a group's
                b.chipCircles.isVisible = false
                b.chipBoards.isVisible = false
            }
            TargetCapability.CAN_SHARE_LOCATION -> {
                // A location is shared with a contact or a group
                b.chipCircles.isVisible = false
                b.chipBoards.isVisible = false
            }
        }

        b.recyclerTargets.adapter = adapter

        b.searchInput.addTextChangedListener { text ->
            adapter.setQuery(text?.toString().orEmpty())
            updateEmptyState()
        }

        b.chipGroupTargets.setOnCheckedStateChangeListener { _, checkedIds ->
            val filter = when (checkedIds.firstOrNull()) {
                R.id.chip_contacts -> TargetFilterType.CONTACTS
                R.id.chip_groups -> TargetFilterType.GROUPS
                R.id.chip_circles -> TargetFilterType.CIRCLES
                R.id.chip_boards -> TargetFilterType.BOARDS
                else -> TargetFilterType.ALL
            }
            adapter.setFilterType(filter)
            updateEmptyState()
        }

        disposable.add(
            targetRepository.observeTargets(capability)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ targets ->
                    adapter.updateTargets(targets)
                    updateEmptyState()
                }, {
                    updateEmptyState()
                })
        )
    }

    private fun updateEmptyState() {
        binding?.let { b ->
            b.emptyHint.isVisible = adapter.itemCount == 0
        }
    }

    fun setOnTargetSelectedListener(listener: (Target) -> Unit) {
        onTargetSelectedListener = listener
    }

    fun setOnCallSelectedListener(listener: (Target, Boolean) -> Unit) {
        onCallSelectedListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        disposable.clear()
        binding = null
    }

    companion object {
        const val TAG = "TargetPickerBottomSheet"
        private const val ARG_CAPABILITY = "arg_capability"
        private const val ARG_TITLE = "arg_title"

        fun newInstance(
            capability: TargetCapability,
            title: String? = null
        ): TargetPickerBottomSheet {
            return TargetPickerBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_CAPABILITY, capability.name)
                    putString(ARG_TITLE, title)
                }
            }
        }
    }
}
