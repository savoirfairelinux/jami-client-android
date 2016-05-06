package cx.ring.views;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import cx.ring.R;
import cx.ring.model.Conversation;

public class ConversationViewHolder extends RecyclerView.ViewHolder {
    public ViewGroup mTxtEntry;
    public TextView mMsgTxt;
    public TextView mMsgDetailTxt;
    public ImageView mPhoto;
    public ViewGroup mCallEntry;
    public TextView mHistTxt;
    public TextView mHistDetailTxt;
    public long mCid = -1;

    public ConversationViewHolder(ViewGroup v, int type) {
        super(v);
        if (type == Conversation.ConversationMessageType.CALL_INFORMATION_TEXT_MESSAGE.getType()) {
            mCallEntry = (ViewGroup) v.findViewById(R.id.call_entry);
            mHistTxt = (TextView) v.findViewById(R.id.call_hist_txt);
            mHistDetailTxt = (TextView) v.findViewById(R.id.call_details_txt);
            mPhoto = (ImageView) v.findViewById(R.id.call_icon);
        } else {
            mTxtEntry = (ViewGroup) v.findViewById(R.id.txt_entry);
            mMsgTxt = (TextView) v.findViewById(R.id.msg_txt);
            mMsgDetailTxt = (TextView) v.findViewById(R.id.msg_details_txt);
            if (type == Conversation.ConversationMessageType.INCOMING_TEXT_MESSAGE.getType()) {
                mPhoto = (ImageView) v.findViewById(R.id.photo);
            }
        }
    }
}
