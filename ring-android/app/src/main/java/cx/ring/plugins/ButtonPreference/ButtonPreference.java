package cx.ring.plugins.ButtonPreference;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import cx.ring.R;

public class ButtonPreference extends Preference {
    Context mContext;
    Button mButton;
    private String text = "";

    public void setClickListener(View.OnClickListener clickListener) {
        this.clickListener = clickListener;
    }

    private View.OnClickListener clickListener;
    public ButtonPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.single_button_preference);
    }

    public ButtonPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayoutResource(R.layout.single_button_preference);
    }

    public ButtonPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.single_button_preference);
    }

    public ButtonPreference(Context context) {
        super(context);
        mContext = context;
        setLayoutResource(R.layout.single_button_preference);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mButton = (Button) holder.findViewById(R.id.button);
        TextView titleView = ((TextView) holder.findViewById(R.id.title));
        TextView summaryView = ((TextView) holder.findViewById(R.id.summary));
        ImageView iconView = ((ImageView) holder.findViewById(R.id.icon));
        CharSequence title = getTitle();
        CharSequence summary = getSummary();
        Drawable icon = this.getIcon();

        if(title != null && title.length() > 0) {
            titleView.setText(title);
        } else {
            titleView.setVisibility(View.GONE);
        }

        if(summary != null &&  summary.length() > 0) {
            summaryView.setText(summary);
        } else {
            summaryView.setVisibility(View.GONE);
        }


        if(icon != null) {
            iconView.setImageDrawable(icon);
        } else {
            iconView.setVisibility(View.GONE);
        }

        mButton.setText(getText());

        if(clickListener != null) {
            mButton.setOnClickListener(clickListener);
        }
    }

}
