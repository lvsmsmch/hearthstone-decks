package com.cyberquick.hearthstonedecks.presentation.adapters.base

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

abstract class BaseRvAdapter<T : Any, VH : RecyclerView.ViewHolder>
    : RecyclerView.Adapter<VH>() {

    abstract val layoutRes: Int
    abstract fun createViewHolder(view: View): VH
    abstract fun onBind(holder: VH, item: T)

    protected val items = mutableListOf<T>()

    /**
     * Replace the entire list and force a full re-bind without per-item
     * animations. notifyDataSetChanged is intentional here: ItemAnimator
     * fade-in/fade-out on full-page swaps causes a visible "ghost frame"
     * of stale items when the host container's visibility flips
     * (Loading → Loaded). A blunt full rebind avoids that entirely.
     */
    @SuppressLint("NotifyDataSetChanged")
    open fun set(newItems: List<T>) {
        if (items == newItems) return
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun clear() = set(emptyList())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return createViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        onBind(holder, items[position])
    }
}
