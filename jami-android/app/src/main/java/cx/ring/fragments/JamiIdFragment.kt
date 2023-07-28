package cx.ring.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cx.ring.databinding.JamiIdLayoutBinding
import cx.ring.utils.ActionHelper.copyAndShow
import cx.ring.utils.ActionHelper.shareAccount

class JamiIdFragment : Fragment() {

    private var binding: JamiIdLayoutBinding? = null

    companion object {
        private const val ARG_JAMI_ID = "jami_id"

        fun newInstance(jamiId: String) =
            JamiIdFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_JAMI_ID, jamiId)
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        JamiIdLayoutBinding.inflate(inflater, container, false).apply {

            val jamiId: String

            // Get the jamiId from the arguments
            requireArguments().let {
                jamiId = it.getString(ARG_JAMI_ID, "")
            }

            // Set the jamiId
            jamiIdTextView?.text = jamiId

            // Adapt the size to the length of the jamiId
            // If the jamiId is too long (more than 16 characters), use a smaller font size
            jamiIdTextView?.textSize =
                if (jamiId.length > 16) {
                    context?.resources?.getDimensionPixelSize(
                        cx.ring.R.dimen.jami_id_small_font_size
                    )!!.toFloat()
                } else {
                    context?.resources?.getDimensionPixelSize(
                        cx.ring.R.dimen.jami_id_regular_font_size
                    )!!.toFloat()
                }

            // Connect the copy and share buttons
            jamiIdCopyButton.setOnClickListener {
                copyAndShow(requireContext(), binding!!.root, jamiId)
            }
            jamiIdShareButton.setOnClickListener {
                shareAccount(requireContext(), jamiId)
            }

            binding = this

        }.root
}