package cx.ring.tv.cards;

import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.RowPresenter;

/**
 * Created by lsiret on 17-09-26.
 */

public class NoDimListRowPresenter extends ListRowPresenter {
    @Override
    public boolean isUsingDefaultListSelectEffect() {
        return false;
    }

    @Override
    protected void onSelectLevelChanged(RowPresenter.ViewHolder holder) {
//        /super.onSelectLevelChanged(holder);
    }

}
