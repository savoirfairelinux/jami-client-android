package cx.ring.welcomejami

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import cx.ring.databinding.WelcomeJamiCardBinding
import cx.ring.utils.ActionHelper.copyAndShow
import cx.ring.utils.ActionHelper.shareAccount

class WelcomeJamiFragment() : Fragment() {
    private var binding: WelcomeJamiCardBinding? = null
    private var jamiId: String = ""
    private var isJamiAccount: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        WelcomeJamiCardBinding.inflate(inflater, container, false).apply {
            requireArguments().let {
                jamiId = it.getString(ARG_JAMI_ID, "")
                isJamiAccount = it.getBoolean(ARG_IS_JAMI_ACCOUNT, true)
            }
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

    companion object {
        private const val ARG_JAMI_ID = "jami_id"
        private const val ARG_IS_JAMI_ACCOUNT = "is_jami_account"

        fun newInstance(jamiId: String, isJamiAccount: Boolean): WelcomeJamiFragment =
            WelcomeJamiFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_JAMI_ID, jamiId)
                    putBoolean(ARG_IS_JAMI_ACCOUNT, isJamiAccount)
                }
            }
    }
}