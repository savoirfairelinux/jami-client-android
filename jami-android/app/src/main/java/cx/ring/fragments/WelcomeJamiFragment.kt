package cx.ring.fragments

import android.content.res.Resources
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import cx.ring.R
import cx.ring.databinding.WelcomeJamiLayoutBinding
import cx.ring.utils.AndroidFileUtils.loadBitmap
import cx.ring.utils.BackgroundType
import cx.ring.viewmodel.JamiIdViewModel
import cx.ring.viewmodel.WelcomeJamiViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
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

            if (!welcomeJamiViewModel.uiState.value.isJamiAccount) {
                Log.d(TAG, "Not a Jami account")
                welcomeJamiDescription.visibility = View.GONE
                return@apply
            }

            welcomeJamiViewModel.initJamiIdViewModel(jamiIdViewModel)
            // Create the JamiIdFragment
            childFragmentManager.beginTransaction()
                .replace(R.id.jamiIdFragmentContainerView, JamiIdFragment())
                .commit()

            binding = this
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observe the uiState and update the UI
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                welcomeJamiViewModel.uiState.collect { uiState ->
                    uiState.uiCustomization?.apply {
                        title?.let { binding?.welcomeJamiTitle?.text = it }
                        description?.let { binding?.welcomeJamiDescription?.text = it }
                        when (backgroundType) {
                            BackgroundType.COLOR -> {
                                backgroundColor?.let {
                                    binding?.welcomeJamiBackground?.setImageDrawable(null)
                                    binding?.welcomeJamiBackground?.setBackgroundColor(it)
                                }
                            }
                            BackgroundType.IMAGE -> { /* TODO Not yet implemented */ }
                            else -> { /* Nothing to do */ }
                        }
                        logoUrl?.let {
                            // Todo: asynchronous needed
                            // Todo: check if the url is valid
                            // Todo: check file size
                            Log.w("WelcomeJamiFragment", "Logo URL: $it")

                            
                            Log.w("devdebug", "Logo URL: $it")
                            Log.w("devdebug", "Logo URI: ${Uri.parse(logoUrl)}")

                            loadBitmap(requireContext(), Uri.parse(logoUrl))
                                .subscribe { image->
                                binding?.welcomeJamiLogo?.setImageDrawable(
                                    BitmapDrawable( Resources.getSystem(), image )
                                )
                            }


                            logoSize?.let {size->
                                // Value is a ratio, so multiply actual logo size by it
                                val defaultSize = context?.resources
                                    ?.getDimensionPixelSize(R.dimen.welcome_jami_logo_default_size)
                                val newSize = size / 100 * (defaultSize ?: 0)
                                binding?.welcomeJamiLogo?.layoutParams?.height = newSize
                                binding?.welcomeJamiLogo?.layoutParams?.width = newSize
                            }
                        }
                        areTipsEnabled.let { /* TODO Not yet implemented */ }
                        tipBoxAndIdColor?.let { /* TODO Not yet implemented */ }
                        mainBoxColor?.let { binding?.welcomeJamiMainBox?.setBackgroundColor(it)}
                    }
                }
            }
        }
    }
    }
