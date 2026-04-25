package com.cyberquick.hearthstonedecks.presentation.adapters

import android.annotation.SuppressLint
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.recyclerview.widget.RecyclerView
import com.cyberquick.hearthstonedecks.R
import com.cyberquick.hearthstonedecks.domain.entities.Hero
import com.cyberquick.hearthstonedecks.utils.drawable

class HeroViewHolder(val view: View) : RecyclerView.ViewHolder(view) {

    @SuppressLint("SetTextI18n")
    fun bind(
        item: Hero,
        isSelected: Boolean,
        onSelectedListener: (Boolean) -> Unit,
    ) {
        view.findViewById<TextView>(R.id.item_hero_name).setText(item.nameRes)
        val imageView = view.findViewById<ImageView>(R.id.item_hero_icon)
        imageView.setImageDrawable(view.context.drawable(item.iconRes))
        imageView.alpha = if (isSelected) 1f else 0.3f

        val checkBox = view.findViewById<AppCompatCheckBox>(R.id.item_hero_checkbox)
        checkBox.apply {
            isChecked = isSelected
            jumpDrawablesToCurrentState()
            setOnCheckedChangeListener { _, isChecked ->
                imageView.alpha = if (isChecked) 1f else 0.3f
                onSelectedListener(isChecked)
            }
        }

        view.findViewById<View>(R.id.item_hero_background).setOnClickListener {
            checkBox.performClick()
        }
    }
}
