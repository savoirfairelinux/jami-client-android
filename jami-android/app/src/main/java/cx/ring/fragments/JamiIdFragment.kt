package cx.ring.fragments

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import cx.ring.R
import cx.ring.databinding.JamiIdLayoutBinding
import cx.ring.utils.ActionHelper.copyAndShow
import cx.ring.utils.ActionHelper.shareAccount


class JamiIdFragment : Fragment() {

    private var binding: JamiIdLayoutBinding? = null

    var button:Int = 0

    companion object {
        private const val ARG_JAMI_ID = "jami_id"

        fun newInstance(jamiId: String) =
            JamiIdFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_JAMI_ID, jamiId)
                }
            }
    }

    /**
     * Resize a drawable to the given width and height (not yet converted to dp)
     * @param drawable The drawable to resize
     * @param width The new width (not yet converted to dp)
     * @param height The new height (not yet converted to dp)
     */
    private fun resizeDrawable(drawable: Drawable, width: Int, height: Int): Drawable {
        return drawable.apply {
            setBounds(
                0,
                0,
                (resources.displayMetrics.density * 24).toInt(),
                (resources.displayMetrics.density * 24).toInt()
            )
        }
    }

    /**
     * Set the right drawable of the jamiIdTextView to the given drawable.
     * Resize it to 24dp x 24dp.
     */
    private fun setEditTextRightDrawable(@DrawableRes id: Int? = null) {
        binding?.jamiIdTextView?.setCompoundDrawables(
            null, null,
            id?.let {
                resizeDrawable(
                    drawable = requireContext().getDrawable(id)!!,
                    width = 24, height = 24
                )
            } ?: null,
            null
        )
    }
    private fun setEditTextError() {
        val redColorStateList = requireContext().getColorStateList(cx.ring.R.color.red_500)

        binding?.jamiIdTextView?.backgroundTintList = redColorStateList
        setEditTextRightDrawable(cx.ring.R.drawable.ic_error_red)

        binding?.textView9?.visibility = View.VISIBLE
        binding?.textView9?.setTextColor(redColorStateList)

        binding?.textView9?.text = context?.getString(R.string.jami_id_not_available)

    }

    private fun setEditTextGood(){
        val greenColorStateList = requireContext().getColorStateList(cx.ring.R.color.green_500)

        binding?.jamiIdTextView?.backgroundTintList = greenColorStateList
        setEditTextRightDrawable(cx.ring.R.drawable.ic_good_green)

        binding?.textView9?.visibility = View.VISIBLE
        binding?.textView9?.setTextColor(greenColorStateList)
        binding?.textView9?.text = context?.getString(R.string.jami_id_available)
    }

    private fun setEditTextNeutral(){
        binding?.jamiIdTextView?.backgroundTintList =
            requireContext().getColorStateList(cx.ring.R.color.transparent)
        setEditTextRightDrawable()

        binding?.textView9?.visibility = View.INVISIBLE
    }

    private fun showKeyboard(target:View){
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm!!.showSoftInput(target, InputMethodManager.SHOW_IMPLICIT)
    }
    fun doSomething() {

        if (button == 0) {
            setEditTextError()
            button = 1
        } else if (button == 1) {
            setEditTextGood()
            button = 2
        } else {
            setEditTextNeutral()
            button = 0
        }
        // Enable text field
        binding?.jamiIdTextView?.isEnabled = true

        // Move user cursor into it, at the end and open keyboard
        binding?.jamiIdTextView?.requestFocus()
        binding?.jamiIdTextView?.setSelection(binding?.jamiIdTextView?.text?.length ?: 0)

        // Show the keyboard
        showKeyboard(binding?.jamiIdTextView!!)

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
            jamiIdTextView.setText(jamiId)

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


            button2.setOnClickListener {
                doSomething()
            }

            binding = this
            setEditTextNeutral()


        }.root
}