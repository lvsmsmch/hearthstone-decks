package com.cyberquick.hearthstonedecks.presentation.adapters

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.cyberquick.hearthstonedecks.R
import com.cyberquick.hearthstonedecks.domain.entities.CardCountable
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.lang.Exception

class CardSmallViewHolder(val view: View) : RecyclerView.ViewHolder(view) {

    @SuppressLint("SetTextI18n")
    fun bind(
        cardCountable: CardCountable,
        onImageLoaded: (Drawable) -> Unit,
        onClicked: () -> Unit,
    ) {
        val imageSmall = view.findViewById<ImageView>(R.id.img_card_small)
        val imageUrl = cardCountable.card.image

        if (imageUrl.isBlank()) {
            imageSmall.setImageResource(R.drawable.card_failed)
        } else {
            Picasso.get()
                .load(imageUrl)
                .placeholder(R.drawable.card_loading)
                .error(R.drawable.card_failed)
                .fit()
                .centerInside()
                .into(imageSmall, object : Callback {
                    override fun onSuccess() {
                        imageSmall.drawable.constantState?.newDrawable()?.let { onImageLoaded(it) }
                    }

                    override fun onError(e: Exception?) {
                        e?.printStackTrace()
                    }
                })
        }


        val amount = view.findViewById<TextView>(R.id.tv_amount)
        amount.isVisible = cardCountable.amount > 1
        amount.text = "x" + cardCountable.amount

        imageSmall.setOnClickListener {
            onClicked()
        }
    }
}
