package com.cyberquick.hearthstonedecks.presentation.adapters.base

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

abstract class BaseRvAdapter<T : Any, VH : RecyclerView.ViewHolder>(
    diffCallback: DiffUtil.ItemCallback<T> = SimpleEqualityDiffCallback(),
) : ListAdapter<T, VH>(diffCallback) {

    abstract val layoutRes: Int
    abstract fun createViewHolder(view: View): VH
    abstract fun onBind(holder: VH, item: T)

    fun set(newItems: List<T>) = submitList(newItems.toList())

    fun clear() = submitList(emptyList())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return createViewHolder(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        onBind(holder, getItem(position))
    }
}

private class SimpleEqualityDiffCallback<T : Any> : DiffUtil.ItemCallback<T>() {
    override fun areItemsTheSame(oldItem: T, newItem: T) = oldItem == newItem
    override fun areContentsTheSame(oldItem: T, newItem: T) = oldItem == newItem
}
