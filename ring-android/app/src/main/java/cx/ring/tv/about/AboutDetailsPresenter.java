package cx.ring.tv.about;

import android.content.Context;
import android.support.v17.leanback.widget.Presenter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import cx.ring.R;
import cx.ring.tv.cards.about.AboutCard;

public class AboutDetailsPresenter
        extends Presenter {
    @BindView(R.id.primary_text)
    TextView mPrimaryText;
    @BindView(R.id.extra_text)
    TextView mExtraText;
    private Context mContext;

    public AboutDetailsPresenter(Context context) {
        mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.detail_view_content, null);
        ButterKnife.bind(this, view);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object itemData) {
        AboutCard card = (AboutCard) itemData;

        mPrimaryText.setText(card.getTitle());
        mExtraText.setText(card.getDescription());
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {

    }
}
