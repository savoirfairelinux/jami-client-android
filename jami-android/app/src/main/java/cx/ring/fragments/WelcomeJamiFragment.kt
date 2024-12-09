/*
 *  Copyright (C) 2004-2024 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring.fragments

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import cx.ring.R
import cx.ring.databinding.WelcomeJamiLayoutBinding
import cx.ring.utils.BackgroundType
import cx.ring.viewmodel.WelcomeJamiViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.jami.utils.Log


@AndroidEntryPoint
class WelcomeJamiFragment : Fragment() {

    private lateinit var binding: WelcomeJamiLayoutBinding
    private val welcomeJamiViewModel: WelcomeJamiViewModel by viewModels({ requireActivity() })

    companion object {
        private val TAG = WelcomeJamiFragment::class.simpleName!!
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        WelcomeJamiLayoutBinding.inflate(inflater, container, false).apply {
            binding = this

            if (!welcomeJamiViewModel.uiState.value.isJamiAccount) {
                Log.d(TAG, "Not a Jami account")
                welcomeJamiDescription.visibility = View.GONE
                return@apply
            }

            // Create the JamiIdFragment
            childFragmentManager.beginTransaction()
                .replace(R.id.jamiIdFragmentContainerView, JamiIdFragment())
                .commit()
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observe the uiState and update the UI
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                welcomeJamiViewModel.uiState.collect { uiState ->
                    uiState.uiCustomization?.apply {
                        title?.let {
                            binding.welcomeJamiTitle.visibility = View.VISIBLE
                            binding.welcomeJamiTitle.text = it
                        }
                        description?.let { binding.welcomeJamiDescription.text = it }
                        when (backgroundType) {
                            BackgroundType.COLOR -> {
                                backgroundColor?.let {
                                    binding.welcomeJamiBackground.setImageDrawable(null)
                                    binding.welcomeJamiBackground.setBackgroundColor(it)
                                }
                            }

                            BackgroundType.IMAGE -> {
                                backgroundUrl?.let {
                                    Glide.with(binding.welcomeJamiBackground.context)
                                        .load(it)
                                        .error(R.drawable.background_welcome_jami)
                                        .into(binding.welcomeJamiBackground)
                                }
                                binding.welcomeJamiBackground.setBackgroundColor(0)
                            }

                            else -> {} /* Nothing to do */

                        }
                        logoUrl?.let {
                            Glide.with(binding.welcomeJamiLogo.context)
                                .load(it)
                                .error(R.drawable.jami_full_logo)
                                .into(binding.welcomeJamiLogo)
                        }
                        logoSize?.let { size ->
                            // Value is a ratio, so multiply actual logo size by it
                            val defaultSize = requireContext().resources
                                .getDimensionPixelSize(R.dimen.welcome_jami_logo_default_size)
                            val newSize = size * defaultSize / 100
                            binding.welcomeJamiLogo.layoutParams?.height = newSize
                        }
                        mainBoxColor?.let {
                            binding.welcomeJamiMainBox.backgroundTintList =
                                ColorStateList.valueOf(it)
                        }
                        areTipsEnabled.let { /* TODO Not yet implemented */ }
                        tipBoxAndIdColor?.let { /* TODO Not yet implemented */ }
                    }
                }
            }
        }
    }
}
