package com.cyberquick.hearthstonedecks.presentation.adapters

import android.view.View
import com.cyberquick.hearthstonedecks.R
import com.cyberquick.hearthstonedecks.domain.entities.DeckPreview
import com.cyberquick.hearthstonedecks.presentation.adapters.base.BaseRvAdapter

class DeckAdapter(
    private val onClickListener: (DeckViewHolder.ItemData) -> Unit,
) : BaseRvAdapter<DeckPreview, DeckViewHolder>() {

    override val layoutRes: Int = R.layout.item_deck_preview
    override fun createViewHolder(view: View): DeckViewHolder {
        return DeckViewHolder(DeckViewHolder.Content.fromView(view))
    }

    override fun onBind(holder: DeckViewHolder, item: DeckPreview) {
        holder.bind(item, onClickListener)
    }
}