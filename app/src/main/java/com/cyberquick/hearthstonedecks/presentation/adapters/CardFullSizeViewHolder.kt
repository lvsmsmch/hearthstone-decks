package com.cyberquick.hearthstonedecks.presentation.adapters

import android.annotation.SuppressLint
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.cyberquick.hearthstonedecks.R
import com.cyberquick.hearthstonedecks.presentation.common.entities.CardFullSizeData
import com.cyberquick.hearthstonedecks.utils.drawable
import com.squareup.picasso.Picasso

class CardFullSizeViewHolder(val view: View) : RecyclerView.ViewHolder(view) {

    @SuppressLint("SetTextI18n")
    fun bind(
        cardData: CardFullSizeData,
        onPreviousItemClicked: () -> Unit,
        onNextItemClicked: () -> Unit,
        onCenterClicked: () -> Unit,
    ) {
        val placeholder = cardData.preview ?: view.context.drawable(R.drawable.card_loading)
        val imageView = view.findViewById<ImageView>(R.id.card_full_size_image)
        val imageUrl = cardData.cardCountable.card.image

        if (imageUrl.isBlank()) {
            imageView.setImageResource(R.drawable.card_failed)
        } else {
            Picasso.get()
                .load(imageUrl)
                .placeholder(placeholder)
                .error(R.drawable.card_failed)
                .fit()
                .centerInside()
                .into(imageView)
        }

        view.findViewById<LinearLayout>(R.id.left_side).setOnClickListener {
            onPreviousItemClicked()
        }
        view.findViewById<LinearLayout>(R.id.right_side).setOnClickListener {
            onNextItemClicked()
        }
        view.findViewById<LinearLayout>(R.id.center_side).setOnClickListener {
            onCenterClicked()
        }

        val copies = view.findViewById<TextView>(R.id.copies)
        copies.isVisible = cardData.cardCountable.amount > 1
        copies.text = "x${cardData.cardCountable.amount}"
    }
}
