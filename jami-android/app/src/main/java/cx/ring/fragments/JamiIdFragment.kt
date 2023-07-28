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
    private var jamiId: String = ""

    companion object {
        private const val ARG_JAMI_ID = "jami_id"

        fun newInstance() =
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
            requireArguments().let {
                jamiId = it.getString(ARG_JAMI_ID, "")
            }

            binding?.jamiIdTextView?.text = jamiId

            binding?.jamiIdTextView?.textSize =
                if (jamiId.length > 16) {
                    context?.resources?.getDimensionPixelSize(
                        cx.ring.R.dimen.jami_id_small_font_size
                    )!!.toFloat()
                } else {
                    context?.resources?.getDimensionPixelSize(
                        cx.ring.R.dimen.jami_id_regular_font_size
                    )!!.toFloat()
                }

            jamiIdCopyButton.setOnClickListener {
                copyAndShow(requireContext(), binding!!.root, jamiId)
            }
            jamiIdShareButton.setOnClickListener {
                shareAccount(requireContext(), jamiId)
            }
            binding = this
        }.root
}