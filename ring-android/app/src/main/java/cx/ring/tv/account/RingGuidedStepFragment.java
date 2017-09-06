package cx.ring.tv.account;

import android.os.Bundle;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v17.leanback.widget.GuidedAction;
import android.text.InputType;
import android.view.View;

import java.util.List;

import javax.inject.Inject;

import cx.ring.mvp.RootPresenter;
import cx.ring.utils.Log;

public abstract class RingGuidedStepFragment<T extends RootPresenter> extends GuidedStepFragment {

    protected static final String TAG = RingGuidedStepFragment.class.getSimpleName();

    @Inject
    protected T presenter;

    protected static void addAction(List<GuidedAction> actions, long id, String title, String desc) {
        actions.add(new GuidedAction.Builder()
                .id(id)
                .title(title)
                .description(desc)
                .build());
    }

    protected static void addDisabledAction(List<GuidedAction> actions, long id, String title, String desc) {
        actions.add(new GuidedAction.Builder()
                .id(id)
                .title(title)
                .description(desc)
                .enabled(false)
                .build());
    }

    protected static void addEditTextAction(List<GuidedAction> actions, int id,
                                            String title, String desc, String editdesc) {
        actions.add(
                new GuidedAction.Builder()
                        .id(id)
                        .title(title)
                        .description(desc)
                        .editDescription(editdesc)
                        .descriptionEditInputType(InputType.TYPE_CLASS_TEXT)
                        .descriptionEditable(true)
                        .build());
    }

    protected static void addPasswordAction(List<GuidedAction> actions, int id,
                                            String title, String desc, String editdesc) {
        actions.add(
                new GuidedAction.Builder()
                        .id(id)
                        .title(title)
                        .description(desc)
                        .editDescription(editdesc)
                        .descriptionEditInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)
                        .descriptionEditable(true)
                        .build());
    }

    protected static void addMultilineActions(List<GuidedAction> actions,
                                              String title, String desc) {
        actions.add(new GuidedAction.Builder()
                .title(title)
                .description(desc)
                .multilineDescription(true)
                .infoOnly(true)
                .enabled(false)
                .focusable(false)
                .build());
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //Be sure to do the injection in onCreateView method
        presenter.bindView(this);
        initPresenter(presenter);
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView");
        super.onDestroyView();
        presenter.unbindView();
    }

    protected void initPresenter(T presenter) {

    }
}
