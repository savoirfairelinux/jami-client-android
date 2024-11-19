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
        if (requireActivity() is OnResultCallback) {
            _callback = requireActivity() as OnResultCallback
        } else {
            throw RuntimeException("Parent fragment must implement ${OnResultCallback::class.java.simpleName}")
        }
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

    fun showError() {
        Log.i(TAG, "Showing error.")
        binding.exit.setOnClickListener {
            Log.i(TAG, "Exit button clicked.")
            callback.onExit(1)
        }
        binding.details.text = getString(R.string.import_side_step3_body_error)
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