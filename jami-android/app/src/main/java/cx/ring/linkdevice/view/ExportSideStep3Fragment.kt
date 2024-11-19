package cx.ring.linkdevice.view

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import cx.ring.R
import cx.ring.databinding.FragmentExportSideStep3Binding
import net.jami.linkdevice.presenter.AuthError

class ExportSideStep3Fragment : Fragment() {
    private var _binding: FragmentExportSideStep3Binding? = null
    private val binding get() = _binding!!
    private var _callback: OnResultCallback? = null
    private val callback get() = _callback!!

    interface OnResultCallback {
        fun onExit()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (requireActivity() is OnResultCallback) {
            _callback = requireActivity() as OnResultCallback
        } else {
            throw RuntimeException("Parent fragment must implement ${OnResultCallback::class.java.simpleName}")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentExportSideStep3Binding.inflate(inflater, container, false)
        .apply { _binding = this }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.exit.setOnClickListener {
            Log.i(TAG, "Exit button clicked.")
            callback.onExit()
        }
    }

    fun showLoading() {
        Log.i(TAG, "Showing loading...")
        binding.details.text = getString(R.string.export_side_step3_body_loading)
        binding.exit.visibility = View.GONE
        binding.status.visibility = View.GONE
        binding.loading.visibility = View.VISIBLE
    }

    fun showDone() {
        Log.i(TAG, "Showing done.")
        binding.details.text = getString(R.string.export_side_step3_body_done)
        binding.status.visibility = View.VISIBLE
        binding.exit.visibility = View.VISIBLE
        binding.loading.visibility = View.GONE
        binding.status.setImageResource(R.drawable.baseline_done_24)
        binding.status.setColorFilter(
            ContextCompat.getColor(
                requireContext(),
                android.R.color.holo_green_dark
            )
        )
        binding.exit.setOnClickListener {}
    }

    fun showError(error: AuthError) {
        Log.i(TAG, "Showing error ($error).")
        binding.details.text =
            when (error) {
                AuthError.AUTHENTICATION -> getString(R.string.link_device_error_authentication)
                AuthError.NETWORK -> getString(R.string.link_device_error_network)
                AuthError.UNKNOWN -> getString(R.string.link_device_error_unknown)
            }
        binding.status.visibility = View.VISIBLE
        binding.exit.visibility = View.VISIBLE
        binding.loading.visibility = View.GONE
        binding.status.setImageResource(R.drawable.baseline_error_24)
        binding.status.setColorFilter(
            ContextCompat.getColor(
                requireContext(),
                android.R.color.holo_red_dark
            )
        )
        binding.exit.setOnClickListener {}
    }

    companion object {
        private val TAG = ExportSideStep3Fragment::class.java.simpleName
    }
}