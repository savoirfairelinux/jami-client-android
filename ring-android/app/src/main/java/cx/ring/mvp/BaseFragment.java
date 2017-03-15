package cx.ring.mvp;

import android.app.Fragment;
import android.os.Bundle;
import android.view.View;

/**
 * Created by hdsousa on 17-03-15.
 */

public abstract class BaseFragment<T extends RootPresenter> extends Fragment {

    protected T presenter;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        presenter = createPresenter();

        presenter.bindView(this);
        initPresenter(presenter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        presenter.unbindView();
    }

    protected abstract T createPresenter();

    protected void initPresenter(T presenter) {

    }
}
