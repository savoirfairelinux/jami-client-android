/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring.tv.account

import android.content.Context
import android.graphics.drawable.Drawable
import net.jami.mvp.RootPresenter
import androidx.leanback.app.GuidedStepSupportFragment
import javax.inject.Inject
import android.os.Bundle
import androidx.leanback.widget.GuidedAction
import android.text.InputType
import android.view.View
import androidx.annotation.StringRes

abstract class JamiGuidedStepFragment<T : RootPresenter<in V>, in V> : GuidedStepSupportFragment() {
    @Inject
    lateinit var presenter: T

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Be sure to do the injection in onCreateView method
        presenter.bindView(this as V)
        initPresenter(presenter)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.unbindView()
    }

    protected fun initPresenter(presenter: T) {}

    companion object {
        fun addAction(context: Context, actions: MutableList<GuidedAction>, id: Long, @StringRes title: Int) {
            actions.add(GuidedAction.Builder(context)
                .id(id)
                .title(title)
                .build())
        }

        fun addAction(context: Context, actions: MutableList<GuidedAction>, id: Long, title: String = "", desc: String = "") {
            actions.add(GuidedAction.Builder(context)
                .id(id)
                .title(title)
                .description(desc)
                .build())
        }

        fun addAction(
            context: Context,
            actions: MutableList<GuidedAction>,
            id: Long,
            title: String?,
            desc: String? = "",
            next: Boolean = true
        ) {
            actions.add(GuidedAction.Builder(context)
                .id(id)
                .title(title)
                .description(desc)
                .hasNext(next)
                .build())
        }

        fun addDisabledAction(
            context: Context,
            actions: MutableList<GuidedAction>,
            id: Long,
            title: String?,
            desc: String?
        ) {
            actions.add(GuidedAction.Builder(context)
                .id(id)
                .title(title)
                .description(desc)
                .enabled(false)
                .build())
        }

        protected fun addDisabledAction(
            context: Context,
            actions: MutableList<GuidedAction>,
            id: Long,
            title: String?,
            desc: String?,
            icon: Drawable? = null
        ) {
            actions.add(GuidedAction.Builder(context)
                .id(id)
                .title(title)
                .description(desc)
                .enabled(false)
                .icon(icon)
                .build())
        }

        fun addDisabledNonFocusableAction(
            context: Context,
            actions: MutableList<GuidedAction>,
            id: Long,
            title: String? = "",
            desc: String? = "",
            icon: Drawable? = null
        ) {
            actions.add(GuidedAction.Builder(context)
                .id(id)
                .title(title)
                .description(desc)
                .enabled(false)
                .focusable(false)
                .icon(icon)
                .build())
        }

        fun addDisabledAction(context: Context, actions: MutableList<GuidedAction>, id: Long,
            title: String?,
            desc: String?,
            icon: Drawable?,
            next: Boolean
        ) {
            actions.add(GuidedAction.Builder(context)
                .id(id)
                .title(title)
                .description(desc)
                .enabled(false)
                .icon(icon)
                .hasNext(next)
                .build())
        }

        fun addEditTextAction(
            context: Context, actions: MutableList<GuidedAction>, id: Long,
            @StringRes title: Int, @StringRes desc: Int): GuidedAction {
            return GuidedAction.Builder(context)
                .id(id)
                .title(title)
                .editTitle("")
                .description(desc)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .editable(true)
                .build()
                .apply { actions.add(this) }
        }

        fun addPasswordAction(context: Context, actions: MutableList<GuidedAction>, id: Long,
                                        title: String?, desc: String?, editdesc: String? = "") {
            actions.add(GuidedAction.Builder(context)
                .id(id)
                .title(title)
                .description(desc)
                .editDescription(editdesc)
                .descriptionEditInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
                .descriptionEditable(true)
                .build())
        }
    }
}