package cx.ring.tv.about;

import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v17.leanback.app.DetailsFragment;
import android.support.v17.leanback.app.DetailsFragmentBackgroundController;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.DetailsOverviewRow;
import android.support.v17.leanback.widget.FullWidthDetailsOverviewRowPresenter;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.RowPresenter;
import android.view.View;
import android.view.ViewGroup;

import cx.ring.R;
import cx.ring.tv.cards.Card;
import cx.ring.tv.cards.iconcards.IconCard;
import cx.ring.tv.cards.iconcards.IconCardHelper;
import cx.ring.utils.Log;

public class AboutDetailsFragment extends DetailsFragment {
    private static final String TAG = "AboutDetailsFragment";
    private final DetailsFragmentBackgroundController mDetailsBackground =
            new DetailsFragmentBackgroundController(this);
    private ArrayObjectAdapter mRowsAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setupUi();
    }

    private void setupUi() {
        Bundle extras = getActivity().getIntent().getExtras();
        Card.Type cardType = Card.Type.DEFAULT;
        if (extras != null && extras.containsKey("abouttype")) {
            int ordinal = extras.getInt("abouttype", 0);
            cardType = Card.Type.values()[ordinal];
        }

        IconCard card = IconCardHelper.getAboutCardByType(getActivity(), cardType);

        ClassPresenterSelector selector = new ClassPresenterSelector();

        FullWidthDetailsOverviewRowPresenter rowPresenter = new FullWidthDetailsOverviewRowPresenter(
                new AboutDetailsPresenter(getActivity())) {

            @Override
            protected RowPresenter.ViewHolder createRowViewHolder(ViewGroup parent) {
                // Customize Actionbar and Content by using custom colors.
                RowPresenter.ViewHolder viewHolder = super.createRowViewHolder(parent);

                View actionsView = viewHolder.view.
                        findViewById(R.id.details_overview_actions_background);
                actionsView.setBackgroundColor(getActivity().getResources().
                        getColor(R.color.color_primary_dark));

                View detailsView = viewHolder.view.findViewById(R.id.details_frame);
                detailsView.setBackgroundColor(
                        getResources().getColor(R.color.color_primary_dark));
                return viewHolder;
            }
        };
        selector.addClassPresenter(DetailsOverviewRow.class, rowPresenter);
        selector.addClassPresenter(ListRow.class,
                new ListRowPresenter());
        mRowsAdapter = new ArrayObjectAdapter(selector);

        Resources res = getActivity().getResources();
        DetailsOverviewRow detailsOverview = new DetailsOverviewRow(
                card);

        // Add images and action buttons to the details view
        detailsOverview.setImageDrawable(res.getDrawable(R.drawable.ic_logo_ring_white));
        /*detailsOverview.addAction(new Action(1, getString(R.string.section_license)));
        detailsOverview.addAction(new Action(2, getString(R.string.credits)));*/
        mRowsAdapter.add(detailsOverview);

        setAdapter(mRowsAdapter);
        initializeBackground();
    }


    private void initializeBackground() {
        mDetailsBackground.enableParallax();
        mDetailsBackground.setCoverBitmap(BitmapFactory.decodeResource(getResources(),
                R.drawable.contrib_background));
    }
}
