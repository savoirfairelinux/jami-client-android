/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.tv.account;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidedAction;
import android.text.InputType;
import android.view.View;

import java.util.List;

import javax.inject.Inject;

import cx.ring.mvp.RootPresenter;

public abstract class RingGuidedStepFragment<T extends RootPresenter> extends GuidedStepSupportFragment {

    protected static final String TAG = RingGuidedStepFragment.class.getSimpleName();

    @Inject
    protected T presenter;

    protected static void addAction(Context context, List<GuidedAction> actions, long id, String title, String desc) {
        actions.add(new GuidedAction.Builder(context)
                .id(id)
                .title(title)
                .description(desc)
                .build());
    }
    protected static void addAction(Context context, List<GuidedAction> actions, long id, String title, String desc, boolean next) {
        actions.add(new GuidedAction.Builder(context)
                .id(id)
                .title(title)
                .description(desc)
                .hasNext(next)
                .build());
    }

    protected static void addDisabledAction(Context context, List<GuidedAction> actions, long id, String title, String desc) {
        actions.add(new GuidedAction.Builder(context)
                .id(id)
                .title(title)
                .description(desc)
                .enabled(false)
                .build());
    }

    protected static void addDisabledAction(Context context, List<GuidedAction> actions, long id, String title, String desc, Drawable icon) {
        actions.add(new GuidedAction.Builder(context)
                .id(id)
                .title(title)
                .description(desc)
                .enabled(false)
                .icon(icon)
                .build());
    }

    protected static void addDisabledNonFocusableAction(Context context, List<GuidedAction> actions, long id, String title, String desc, Drawable icon) {
        actions.add(new GuidedAction.Builder(context)
                .id(id)
                .title(title)
                .description(desc)
                .enabled(false)
                .focusable(false)
                .icon(icon)
                .build());
    }

    protected static void addDisabledAction(Context context, List<GuidedAction> actions, long id, String title, String desc, Drawable icon,boolean next) {
        actions.add(new GuidedAction.Builder(context)
                .id(id)
                .title(title)
                .description(desc)
                .enabled(false)
                .icon(icon)
                .hasNext(next)
                .build());
    }

    protected static GuidedAction addEditTextAction(Context context, List<GuidedAction> actions, long id,
                                            int title, int desc) {
        actions.add(
                new GuidedAction.Builder(context)
                        .id(id)
                        .title(title)
                        .editTitle("")
                        .description(desc)
                        .inputType(InputType.TYPE_CLASS_TEXT)
                        .editable(true)
                        .build());
        return actions.get(actions.size()-1);
    }

    protected static void addPasswordAction(Context context, List<GuidedAction> actions, long id,
                                            String title, String desc, String editdesc) {
        actions.add(
                new GuidedAction.Builder(context)
                        .id(id)
                        .title(title)
                        .description(desc)
                        .editDescription(editdesc)
                        .descriptionEditInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)
                        .descriptionEditable(true)
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
        super.onDestroyView();
        presenter.unbindView();
    }

    protected void initPresenter(T presenter) {

    }
}
