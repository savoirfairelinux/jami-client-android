package cx.ring.welcomejami

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import cx.ring.databinding.WelcomeJamiCardBinding
import cx.ring.utils.ActionHelper.copyAndShow
import cx.ring.utils.ActionHelper.shareAccount

class WelcomeJamiFragment(
    private val jamiId: String = "",
    private val isJamiAccount: Boolean = true,
) : Fragment() {
    private var binding: WelcomeJamiCardBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        WelcomeJamiCardBinding.inflate(inflater, container, false).apply {
//            jamiIdCard.editButton.setOnClickListener { }
            jamiIdCard.copyButton.setOnClickListener {
                copyAndShow(requireContext(), binding!!.root, jamiId)
            }
            if (isJamiAccount) {
                jamiIdCard.root.visibility = View.VISIBLE

            } else {
                jamiIdCard.root.visibility = View.GONE
                welcomeJamiBody.text =
                    "Jami is a universal communication platform, with privacy as its foundation that relies on a free distributed network for everyone."
            }
            jamiIdCard.shareButton.setOnClickListener { shareAccount(requireContext(), jamiId) }
            jamiIdCard.jamiIdTextView.text = jamiId
            binding = this
        }.root
}