package com.cyberquick.hearthstonedecks.presentation.adapters

import android.graphics.drawable.Drawable
import android.view.View
import com.cyberquick.hearthstonedecks.R
import com.cyberquick.hearthstonedecks.domain.entities.CardCountable
import com.cyberquick.hearthstonedecks.domain.repositories.SetsRepository
import com.cyberquick.hearthstonedecks.presentation.adapters.base.BaseRvAdapter
import com.cyberquick.hearthstonedecks.presentation.common.entities.CardFullSizeData
import com.cyberquick.hearthstonedecks.presentation.dialogs.DialogPreviewCard
import java.util.concurrent.atomic.AtomicBoolean

class CardSmallAdapter(
    private val setsRepository: SetsRepository,
) : BaseRvAdapter<CardCountable, CardSmallViewHolder>() {

    companion object {
        const val TOTAL_ITEMS_HORIZONTAL = 4
    }

    private val images = mutableMapOf<CardCountable, Drawable>()
    private var clicksBlocked = AtomicBoolean(false)

    override val layoutRes: Int = R.layout.item_card_small

    override fun createViewHolder(view: View) = CardSmallViewHolder(view)

    override fun onBind(holder: CardSmallViewHolder, item: CardCountable) {
        holder.bind(
            item,
            onImageLoaded = {
                images[item] = it
            },
            onClicked = {
                if (clicksBlocked.getAndSet(true)) {
                    return@bind
                }

                DialogPreviewCard(
                    holder.view.context,
                    sourceScreen = holder.view,
                    cards = items.map {
                        CardFullSizeData(
                            it, images[it], setsRepository.getDataAboutSet(it.card.cardSetId)
                        )
                    },
                    selectedCardIndex = items.indexOf(item),
                    onClosed = {
                        clicksBlocked.set(false)
                    },
                ).show()
            },
        )
    }
}