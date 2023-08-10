package cx.ring.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import cx.ring.R
import cx.ring.databinding.WelcomeJamiLayoutBinding
import cx.ring.viewmodel.JamiIdStatus
import cx.ring.viewmodel.JamiIdViewModel
import cx.ring.viewmodel.WelcomeJamiViewModel
import dagger.hilt.android.AndroidEntryPoint
import net.jami.utils.Log

@AndroidEntryPoint
class WelcomeJamiFragment : Fragment() {

    private var binding: WelcomeJamiLayoutBinding? = null
    private val welcomeJamiViewModel: WelcomeJamiViewModel by viewModels({ requireActivity() })
    private val jamiIdViewModel by lazy { ViewModelProvider(this)[JamiIdViewModel::class.java] }

    companion object {
        private val TAG = WelcomeJamiViewModel::class.simpleName!!
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        WelcomeJamiLayoutBinding.inflate(inflater, container, false).apply {

            val currentUiState = welcomeJamiViewModel.uiState.value
            binding = this

            if (!currentUiState.isJamiAccount) {
                Log.d(TAG, "Not a Jami account")
                welcomeJamiDescription.visibility = View.GONE
                return@apply
            }

            // When defined, show the JamiId and block the possibility to modify it.
            // Else show the JamiHash and let the user the possibility to define the JamiId.
            if (currentUiState.jamiId != "") {
                Log.d(TAG, "Username is registered : ${currentUiState.jamiId}")
                jamiIdViewModel.init(
                    username = currentUiState.jamiId,
                    jamiIdStatus = JamiIdStatus.USERNAME_DEFINED
                )
            } else {
                Log.d(TAG, "Username is not registered : ${currentUiState.jamiHash}")
                jamiIdViewModel.init(
                    username = currentUiState.jamiHash,
                    jamiIdStatus = JamiIdStatus.USERNAME_NOT_DEFINED
                )
            }

            // Create the JamiIdFragment
            childFragmentManager.beginTransaction()
                .replace(R.id.jamiIdFragmentContainerView, JamiIdFragment())
                .commit()
        }.root
}