/*
 * Copyright (C) 2026 Savoir-faire Linux Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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
package cx.ring.client

import android.content.Context
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.R
import cx.ring.databinding.DialogContactLabelsBinding
import cx.ring.utils.DeviceUtils
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.ContactLabels
import net.jami.utils.Log

internal fun showCreateContactLabelDialog(context: Context, onCreate: (String) -> Unit): AlertDialog {
    val binding = DialogContactLabelsBinding.inflate(LayoutInflater.from(context))
    binding.labels.visibility = android.view.View.GONE
    binding.newLabelBox.endIconMode = com.google.android.material.textfield.TextInputLayout.END_ICON_NONE
    val dialog = MaterialAlertDialogBuilder(context)
        .setTitle(R.string.contact_labels_add)
        .setView(binding.root)
        .setPositiveButton(R.string.wizard_next, null)
        .setNegativeButton(android.R.string.cancel, null)
        .show()
    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
        val label = binding.newLabel.text.toString().trim()
        if (label.isEmpty()) {
            binding.newLabelBox.error = context.getString(R.string.contact_labels_name_required)
            return@setOnClickListener
        }
        try {
            ContactLabels.encode(setOf(label))
        } catch (e: IllegalArgumentException) {
            binding.newLabelBox.error = context.getString(R.string.contact_labels_invalid)
            return@setOnClickListener
        }
        onCreate(label)
        dialog.dismiss()
    }
    return dialog
}

internal fun showContactLabelsDialog(
    context: Context,
    currentLabels: Set<String>,
    availableLabels: List<String>,
    save: (Set<String>) -> Completable
): AlertDialog {
    val binding = DialogContactLabelsBinding.inflate(LayoutInflater.from(context))
    val selected = currentLabels.toMutableSet()
    val chips = mutableMapOf<String, Chip>()
    val disposables = CompositeDisposable()
    val dialog = MaterialAlertDialogBuilder(context)
        .setTitle(R.string.contact_labels)
        .setView(binding.root)
        .setPositiveButton(R.string.contact_labels_save, null)
        .setNegativeButton(android.R.string.cancel, null)
        .create()

    fun addChip(label: String) {
        val chip = (LayoutInflater.from(context).inflate(R.layout.item_contact_label, binding.labels, false) as Chip).apply {
            text = label
            isCheckable = true
            isChecked = label in selected
            setOnCheckedChangeListener { _, checked ->
                if (checked) selected.add(label) else selected.remove(label)
            }
        }
        chips[label] = chip
        binding.labels.addView(chip)
    }

    fun addInput(): Boolean {
        val input = binding.newLabel.text.toString().trim()
        if (input.isEmpty()) return true
        try {
            ContactLabels.encode(selected + input)
        } catch (e: IllegalArgumentException) {
            binding.newLabelBox.error = context.getString(R.string.contact_labels_invalid)
            return false
        }
        selected.add(input)
        chips[input]?.isChecked = true
        if (input !in chips) addChip(input)
        binding.newLabel.setText("")
        binding.newLabelBox.error = null
        return true
    }

    (availableLabels + currentLabels).distinct().sorted().forEach(::addChip)
    fun setSaving(saving: Boolean) {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = !saving
        binding.newLabel.isEnabled = !saving
        binding.newLabelBox.isEndIconVisible = !saving
        chips.values.forEach { it.isEnabled = !saving }
    }

    binding.newLabelBox.setEndIconOnClickListener { addInput() }
    binding.newLabel.setOnEditorActionListener { _, action, _ ->
        if (action == EditorInfo.IME_ACTION_DONE) {
            addInput()
            true
        } else false
    }
    dialog.setOnDismissListener { disposables.dispose() }
    dialog.show()
    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
        if (!addInput()) return@setOnClickListener
        val labels = selected.toSet()
        setSaving(true)
        disposables.add(Completable.defer { save(labels) }
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe({ dialog.dismiss() }, { error ->
                Log.e("ContactLabelsDialog", "Cannot save contact labels", error)
                binding.newLabelBox.error = context.getString(R.string.contact_labels_save_error)
                setSaving(false)
            }))
    }
    return dialog
}
