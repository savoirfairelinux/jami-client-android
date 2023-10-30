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
package cx.ring.tv.about

import android.content.Context
import androidx.leanback.widget.Presenter
import android.view.ViewGroup
import android.view.LayoutInflater
import cx.ring.databinding.DetailViewContentBinding
import cx.ring.tv.cards.iconcards.IconCard

class AboutDetailsPresenter(private val context: Context) : Presenter() {
    private var binding: DetailViewContentBinding? = null

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        return ViewHolder(DetailViewContentBinding.inflate(LayoutInflater.from(context))
            .apply { binding = this }
            .root)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, itemData: Any?) {
        val card = itemData as IconCard? ?: return
        binding?.apply {
            primaryText.text = card.title
            extraText.text = card.description
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {}
}