package cx.ring.welcomejami

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import cx.ring.databinding.WelcomeJamiCardBinding

class WelcomeJamiFragment() : Fragment() {
    private var binding: WelcomeJamiCardBinding? = null
    private var jamiId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        WelcomeJamiCardBinding.inflate(inflater, container, false).apply {
//            jamiIdCard.editButton.setOnClickListener { }
            jamiIdCard.copyButton.setOnClickListener { }
            jamiIdCard.shareButton.setOnClickListener { }
            jamiIdCard.jamiIdTextView.text = jamiId
            binding = this
        }.root

    fun setJamiId(newJamiId: String) {
        jamiId = newJamiId
        binding?.let { it.jamiIdCard.jamiIdTextView.text = jamiId }
    }
}