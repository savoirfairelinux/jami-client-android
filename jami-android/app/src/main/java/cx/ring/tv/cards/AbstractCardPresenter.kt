/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package cx.ring.tv.cards

import android.content.Context
import androidx.leanback.widget.BaseCardView
import androidx.leanback.widget.Presenter
import android.view.ViewGroup
import io.reactivex.rxjava3.disposables.CompositeDisposable

/**
 * This abstract, generic class will create and manage the
 * ViewHolder and will provide typed Presenter callbacks such that you do not have to perform casts
 * on your own.
 *
 * @param <T> View type for the card.
</T> */
abstract class AbstractCardPresenter<T : BaseCardView>(val context: Context) : Presenter() {
    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        return CardViewHolder(onCreateView())
    }

    public class CardViewHolder<T : BaseCardView>(view: T) : ViewHolder(view) {
        val disposable = CompositeDisposable()
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val card = item as Card
        onBindViewHolder(card, viewHolder.view as T, (viewHolder as CardViewHolder<T>).disposable)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        onUnbindViewHolder(viewHolder.view as T)
        (viewHolder as CardViewHolder<T>).disposable.clear()
    }

    fun onUnbindViewHolder(cardView: T) {
        // Nothing to clean up. Override if necessary.
    }

    /**
     * Invoked when a new view is created.
     *
     * @return Returns the newly created view.
     */
    protected abstract fun onCreateView(): T

    /**
     * Implement this method to update your card's view with the data bound to it.
     *
     * @param card     The model containing the data for the card.
     * @param cardView The view the card is bound to.
     * @see Presenter.onBindViewHolder
     */
    abstract fun onBindViewHolder(card: Card, cardView: T, disposable: CompositeDisposable)

    companion object {
        private const val TAG = "AbstractCardPresenter"
    }
}