package cx.ring.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import cx.ring.R
import cx.ring.databinding.WelcomeJamiLayoutBinding
import cx.ring.viewmodel.JamiIdStatus
import cx.ring.viewmodel.JamiIdViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WelcomeJamiFragment : Fragment() {

    private var binding: WelcomeJamiLayoutBinding? = null
    private val viewModel by lazy { ViewModelProvider(this)[JamiIdViewModel::class.java] }

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

            viewModel.init(jamiId, JamiIdStatus.USERNAME_NOT_DEFINED)

            // Create the JamiIdFragment
            childFragmentManager.beginTransaction()
                .replace(R.id.jamiIdFragmentContainerView, JamiIdFragment())
                .commit()

            binding = this

        }.root
}