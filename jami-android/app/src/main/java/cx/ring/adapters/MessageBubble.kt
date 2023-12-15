//package cx.ring.adapters
//
//import android.util.Log
//import android.view.View
//import android.view.ViewGroup
//import android.view.ViewGroup.MarginLayoutParams
//import android.widget.TextView
//import androidx.constraintlayout.widget.ConstraintLayout
//import androidx.core.view.doOnLayout
//import androidx.core.view.doOnNextLayout
//import androidx.core.view.isVisible
//import androidx.core.view.updateLayoutParams
//import androidx.fragment.app.Fragment
//import com.bumptech.glide.Glide
//import cx.ring.R
//import io.noties.markwon.Markwon
//
//
//object MessageBubble : Fragment() {
//    private val TAG = "MessageFragment"
//
//
////    private var binding: NewItemConvMsgMeBinding? = null
//    private var isMessageEdited = false
//
//
////    override fun onCreateView(
////        inflater: android.view.LayoutInflater,
////        container: android.view.ViewGroup?,
////        savedInstanceState: Bundle?,
////    ): View {
////        NewItemConvMsgMeBinding.inflate(layoutInflater).let { messageLayoutBinding ->
////            binding = messageLayoutBinding
////            return messageLayoutBinding.root
////        }
////    }
//
//    fun setMessageText(
//        markwon: Markwon,
//        messageText: BaselineLastLineTextView,
//        messageTime: TextView,
//        text: String,
//        time: String,
//        mainBubbleContainer: ViewGroup,
//        messageEdited: TextView,
//        msgTextAndTime: ViewGroup,
//        bubbleMessageLayout: ViewGroup
//    ) {
////        val convViewHolder = convViewHolder ?: return
//        messageText.text = markwon.toMarkdown(text)
//        messageTime.text = time
////        showTime(
////            mainBubbleContainer,
////            messageText,
////            messageTime,
////            messageEdited,
////            msgTextAndTime,
////            bubbleMessageLayout)
//    }
//
//    fun showTime(
//        mainBubbleContainer: ViewGroup,
//        messageText: BaselineLastLineTextView,
//        messageTime: TextView,
//        messageEdited: TextView,
//        msgTextAndTime: ViewGroup,
//        bubbleMessageLayout: ViewGroup
//    ) {
//        Log.w("ConversationAdapter", "$TAG:showTime")
////        val binding = binding ?: return
//        if(messageTime==null) Log.w("devdebug","messageTime est null")
//        mainBubbleContainer.doOnLayout {
//            val bubbleMaximumWidth = mainBubbleContainer.width
//            val messageTextWidth = messageText.width
//            val messageTimeWidth = messageTime.width
//            val messageEditedWidth = messageEdited.width
//            Log.w("devdebug", "$TAG:showTime:widths: $bubbleMaximumWidth $messageTextWidth $messageTimeWidth $messageEditedWidth")
//
//            Log.w("devdebug","avant:\n"+
//            "\tendToEnd=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).endToEnd}\n" +
//                    "\tstartToEnd=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).startToEnd}\n" +
//                    "\tbaselineToBaseline=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).baselineToBaseline}\n" +
//                    "\tbaselineToBottom=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).baselineToBottom}\n" +
//                    "\tbaselineToTop=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).baselineToTop}\n" +
//                    "\ttopToBottom=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).topToBottom}\n" +
//                    "\ttopToTop=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).topToTop}\n" +
//                    "\tbottomToBottom=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).bottomToBottom}\n" +
//                    "\tbottomToTop=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).bottomToTop}\n" +
//                    "\tstartToStart=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).startToStart}\n" +
//                    "\tendToStart=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).endToStart}\n")
//            val messageLineCount = messageText.lineCount
//            val paddingWidth = msgTextAndTime.paddingStart +
//                    msgTextAndTime.paddingEnd
//
//            val messageInfoWidth = messageTimeWidth + messageEditedWidth + paddingWidth
//            if (bubbleMaximumWidth < messageTextWidth + messageInfoWidth) {
//                // Case 1: message is too long and time should be displayed on a new line.
//                if (bubbleMaximumWidth < (messageText.layout.getLineWidth(messageLineCount - 1)) + messageInfoWidth) {
//                    Log.w("devdebug", "$TAG:showTime:case1")
//
//                    messageTime.updateLayoutParams<ConstraintLayout.LayoutParams> {
//                        topToBottom = messageText.id
//                        baselineToBaseline = ConstraintLayout.LayoutParams.UNSET
//                        startToEnd = ConstraintLayout.LayoutParams.UNSET
//                        endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
//                    }
//                    Log.w("devdebug","après:\n"+
//                            "\tendToEnd=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).endToEnd}\n" +
//                            "\tstartToEnd=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).startToEnd}\n" +
//                            "\tbaselineToBaseline=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).baselineToBaseline}\n" +
//                            "\tbaselineToBottom=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).baselineToBottom}\n" +
//                            "\tbaselineToTop=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).baselineToTop}\n" +
//                            "\ttopToBottom=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).topToBottom}\n" +
//                            "\ttopToTop=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).topToTop}\n" +
//                            "\tbottomToBottom=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).bottomToBottom}\n" +
//                            "\tbottomToTop=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).bottomToTop}\n" +
//                            "\tstartToStart=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).startToStart}\n" +
//                            "\tendToStart=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).endToStart}\n")
//                    /*messageEdited.updateLayoutParams<ConstraintLayout.LayoutParams> {
//                        topToBottom = messageText.id
//                        startToEnd = ConstraintLayout.LayoutParams.UNSET
//                        baselineToBaseline = ConstraintLayout.LayoutParams.UNSET
//                    }*/
//                } else {
//                    // Case 2: message is too long but time can be displayed next to the last line.
//                    Log.w("devdebug", "$TAG:showTime:case2")
//                    messageTime.updateLayoutParams<ConstraintLayout.LayoutParams> {
//                        topToBottom = ConstraintLayout.LayoutParams.UNSET
//                        baselineToBaseline = messageText.id
//                        startToEnd = ConstraintLayout.LayoutParams.UNSET
//                        endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
//                    }
//                    Log.w("devdebug","après:\n"+
//                            "\tendToEnd=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).endToEnd}\n" +
//                            "\tstartToEnd=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).startToEnd}\n" +
//                            "\tbaselineToBaseline=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).baselineToBaseline}\n" +
//                            "\tbaselineToBottom=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).baselineToBottom}\n" +
//                            "\tbaselineToTop=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).baselineToTop}\n" +
//                            "\ttopToBottom=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).topToBottom}\n" +
//                            "\ttopToTop=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).topToTop}\n" +
//                            "\tbottomToBottom=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).bottomToBottom}\n" +
//                            "\tbottomToTop=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).bottomToTop}\n" +
//                            "\tstartToStart=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).startToStart}\n" +
//                            "\tendToStart=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).endToStart}\n")
//                    /*messageEdited.updateLayoutParams<ConstraintLayout.LayoutParams> {
//                        topToBottom = ConstraintLayout.LayoutParams.UNSET
//                        startToEnd = ConstraintLayout.LayoutParams.UNSET
//                        baselineToBaseline = messageText.id
//                    }*/
//
//                }
//            } else {
//                // Case 3: message is not too long and time can be displayed on the same line.
//                Log.w("devdebug", "$TAG:showTime:case3 ")
//
//
//                messageTime.updateLayoutParams<ConstraintLayout.LayoutParams> {
//                    topToBottom = ConstraintLayout.LayoutParams.UNSET
//                    baselineToBaseline = messageText.id
//                    endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
//                    startToEnd =
//                        /*if (isMessageEdited) ConstraintLayout.LayoutParams.UNSET
//                        else */ messageText.id
//                }
//                Log.w("devdebug","après:\n"+
//                        "\tendToEnd=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).endToEnd}\n" +
//                        "\tstartToEnd=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).startToEnd}\n" +
//                        "\tbaselineToBaseline=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).baselineToBaseline}\n" +
//                        "\tbaselineToBottom=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).baselineToBottom}\n" +
//                        "\tbaselineToTop=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).baselineToTop}\n" +
//                        "\ttopToBottom=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).topToBottom}\n" +
//                        "\ttopToTop=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).topToTop}\n" +
//                        "\tbottomToBottom=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).bottomToBottom}\n" +
//                        "\tbottomToTop=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).bottomToTop}\n" +
//                        "\tstartToStart=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).startToStart}\n" +
//                        "\tendToStart=${(messageTime.layoutParams as ConstraintLayout.LayoutParams).endToStart}\n")
//
//
//                /*messageEdited.updateLayoutParams<ConstraintLayout.LayoutParams> {
//                    topToBottom = ConstraintLayout.LayoutParams.UNSET
//                    startToEnd = messageText.id
//                    baselineToBaseline = messageText.id
//                }*/
//            }
//        }
//    }
//
//
////
////    fun showReplyMessage(
////        replyMessageTxt: TextView,
////        msgReplyContent: ViewGroup,
////        mainBubbleContainer: ViewGroup,
////        text: String? = null
////    ) {
//////        val binding = binding ?: return
////
//////        replyMessageTxt.text = text
////        val isRepliedMessage = text != null
////        msgReplyContent.isVisible = isRepliedMessage
////        if (isRepliedMessage) {
////            mainBubbleContainer.updateLayoutParams<MarginLayoutParams> {
////                topMargin = resources.getDimensionPixelSize(R.dimen.replied_shift)
////            }
////        } else {
////            mainBubbleContainer.updateLayoutParams<MarginLayoutParams> {
////                topMargin = 0
////            }
////        }
////    }
//
//
////    fun showReactions(reactions: List<String> = emptyList()) {
//////        val binding = binding ?: return
////        val isReactions = reactions.isNotEmpty()
////        reactionChip.text = reactions.joinToString(separator = "")
////        reactionChip.isVisible = isReactions
////        showTime()
////    }
//
////    fun showLinkPreview(
////        domain: String? = null,
////        title: String? = null,
////        description: String? = null,
////    ) {
//////        val binding = binding ?: return
//
////        val isLinkMessage = domain != null
//
////        if (isLinkMessage) {
////
////            binding.linkPreview.linkPreview.visibility = View.VISIBLE
////            Glide.with(requireContext())
////                .load(resources.getString(R.string.link_preview_image_url))
////                .into(binding.linkPreview.linkPreviewImg)
//////            binding.linkPreview.linkPreviewTitle.text = "Title"
//////            binding.linkPreview.linkPreviewDescription.text =
////                getString(R.string.message_long)
//////            binding.linkPreview.linkPreviewDomain.text = "www.example.com"
////        } else {
////            binding.linkPreview.linkPreview.visibility = View.GONE
////        }
////        showTime()
//////    }
//
//
//    // ici je suis fonction qui est appelée dans le fragment
//    // comment accéder au view holder depuis le fragment ?
//    fun showMessageEdited(checked: Boolean) {
////        val binding = binding ?: return
////        isMessageEdited = checked
////        binding.messageEdited.isVisible = checked
////        showTime()
//    }
//}