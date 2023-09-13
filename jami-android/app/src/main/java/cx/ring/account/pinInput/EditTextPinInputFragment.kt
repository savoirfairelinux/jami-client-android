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
import net.jami.utils.Log

@AndroidEntryPoint
class EditTextPinInputFragment : Fragment() {

    private val viewModel: EditTextPinInputViewModel by viewModels({ requireParentFragment() })
    private lateinit var binding: EditTextPinInputBinding

    companion object {
        private val TAG = EditTextPinInputFragment::class.simpleName!!
    }

    // inflate the layout and link with viewModel
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        EditTextPinInputBinding.inflate(inflater, container, false).apply {
            binding = this
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // to have the text entered in the text field
        val enterPinEditText: TextInputEditText = view.findViewById(R.id.enter_pin)
        val startingAt: Int = 17
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

    private fun showErrorPanel() {
        binding.enterPin.error = getString(R.string.error_format_not_supported)
        binding.enterPin.requestFocus()
    }
}
