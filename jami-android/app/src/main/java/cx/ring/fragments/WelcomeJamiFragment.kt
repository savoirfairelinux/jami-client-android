package cx.ring.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cx.ring.R
import cx.ring.databinding.WelcomeJamiLayoutBinding

class WelcomeJamiFragment : Fragment() {

    private var binding: WelcomeJamiLayoutBinding? = null

    companion object {
        private const val ARG_JAMI_ID = "jami_id"

        fun newInstance(jamiId: String) =
            WelcomeJamiFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_JAMI_ID, jamiId)
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        WelcomeJamiLayoutBinding.inflate(inflater, container, false).apply {

            val jamiId: String

            // Get the jamiId from the arguments
            requireArguments().let {
                jamiId = it.getString(ARG_JAMI_ID, "")
            }

            // Create the JamiIdFragment
            val fragment = JamiIdFragment.newInstance(jamiId)
            childFragmentManager.beginTransaction()
                .replace(R.id.jamiIdFragmentContainerView, fragment)
                .commit()

            binding = this

        }.root
}