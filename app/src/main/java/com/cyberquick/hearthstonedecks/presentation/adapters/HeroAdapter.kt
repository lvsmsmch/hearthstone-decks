package com.cyberquick.hearthstonedecks.presentation.adapters

import android.view.View
import com.cyberquick.hearthstonedecks.R
import com.cyberquick.hearthstonedecks.domain.entities.Hero
import com.cyberquick.hearthstonedecks.presentation.adapters.base.BaseRvAdapter

class HeroAdapter(
    previouslySelected: Set<Hero>,
    private val isAllSelectedListener: (Boolean) -> Unit,
) : BaseRvAdapter<Hero, HeroViewHolder>() {

    private val currentlySelected = previouslySelected.toMutableSet()

    override val layoutRes = R.layout.item_hero_choosable

    init {
        set(Hero.entries)
    }

    override fun createViewHolder(view: View) = HeroViewHolder(view)

    override fun onBind(holder: HeroViewHolder, item: Hero) {
        holder.bind(
            item = item,
            isSelected = currentlySelected.contains(item),
            onSelectedListener = { isSelected ->
                when (isSelected) {
                    true -> currentlySelected.add(item)
                    false -> currentlySelected.remove(item)
                }

                val isAllSelected = currentlySelected.size == Hero.entries.size
                isAllSelectedListener(isAllSelected)
            }
        )
    }

    fun getSelected(): Set<Hero> {
        return currentlySelected
    }

    fun selectAll() {
        select(Hero.entries.toSet())
    }

    fun unselectAll() {
        select(emptySet())
    }

    private fun select(items: Set<Hero>) {
        if (currentlySelected == items) {
            return
        }
        currentlySelected.clear()
        currentlySelected.addAll(items)
        notifyDataSetChanged()
    }
}