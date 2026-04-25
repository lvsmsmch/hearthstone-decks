package com.cyberquick.hearthstonedecks.presentation.dialogs

import android.app.Activity
import android.graphics.Rect
import android.text.InputFilter
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cyberquick.hearthstonedecks.R
import com.cyberquick.hearthstonedecks.databinding.DialogFilterBinding
import com.cyberquick.hearthstonedecks.domain.entities.DecksFilter
import com.cyberquick.hearthstonedecks.domain.entities.Hero
import com.cyberquick.hearthstonedecks.presentation.adapters.HeroAdapter
import com.cyberquick.hearthstonedecks.utils.doOnLengthChange
import com.cyberquick.hearthstonedecks.utils.hideKeyboard
import com.cyberquick.hearthstonedecks.utils.setupFullHeight
import com.cyberquick.hearthstonedecks.utils.showKeyboard
import com.cyberquick.hearthstonedecks.utils.statusBarHeightPixels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog


class DialogFilter(
    val activity: Activity
) : BottomSheetDialog(activity, R.style.DialogHeroFilterStyle) {

    /**
     * Hide keyboard on outside-of-edit-text touch.
     */
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) {
            return super.dispatchTouchEvent(event)
        }

        val viewClicked: View? = currentFocus
        if (viewClicked !is EditText) {
            return super.dispatchTouchEvent(event)
        }

        val outRect = Rect()
        viewClicked.getGlobalVisibleRect(outRect)

        val pointX = event.rawX.toInt()
        val pointY = event.rawY.toInt() - context.statusBarHeightPixels()
        val isEditTextClicked = outRect.contains(pointX, pointY)
        if (isEditTextClicked) {
            return super.dispatchTouchEvent(event)
        }

        viewClicked.clearFocus()
        viewClicked.hideKeyboard()

        return super.dispatchTouchEvent(event)
    }

    companion object {
        fun show(
            activity: Activity,
            previousFilter: DecksFilter,
            onNewSelected: (DecksFilter) -> Unit,
        ) {
            var isAllItemsSelected: Boolean

            val dialog = DialogFilter(activity)
            val binding = DialogFilterBinding.inflate(dialog.layoutInflater)
            dialog.setContentView(binding.root)

            dialog.setOnDismissListener {
            }

            val adapter = HeroAdapter(
                previouslySelected = previousFilter.heroes,
                isAllSelectedListener = { isAllSelected ->
                    binding.checkboxSelectAll.isChecked = isAllSelected
                    binding.checkboxSelectAll.jumpDrawablesToCurrentState()
                    isAllItemsSelected = isAllSelected
                }
            )

            binding.recycleViewHero.adapter = adapter
            binding.recycleViewHero.layoutManager = GridLayoutManager(
                activity, 6, RecyclerView.VERTICAL, false
            )

            isAllItemsSelected = previousFilter.heroes.size == Hero.entries.size
            binding.checkboxSelectAll.isChecked = isAllItemsSelected

            val selectAllListener = OnClickListener {
                val isCheckedOld = isAllItemsSelected
                val isCheckedNew = isCheckedOld.not()
                binding.checkboxSelectAll.isChecked = isCheckedNew
                when (isCheckedNew) {
                    true -> adapter.selectAll()
                    false -> adapter.unselectAll()
                }
                isAllItemsSelected = isCheckedNew
            }

            binding.checkboxSelectAll.setOnClickListener(selectAllListener)
            binding.textSelectAll.setOnClickListener(selectAllListener)

            binding.btnClose.setOnClickListener {
                dialog.dismiss()
            }

            binding.btnSearch.setOnClickListener {
                val text = binding.etPrompt.text.toString()
                val heroes = adapter.getSelected()
                val filter = DecksFilter(text, heroes)
                onNewSelected(filter)
                dialog.dismiss()
            }

            binding.btnClearText.setOnClickListener { binding.etPrompt.setText("") }
            binding.etPrompt.filters += InputFilter.LengthFilter(100)
            binding.etPrompt.doOnLengthChange { binding.btnClearText.isVisible = it != 0 }
            binding.etPrompt.setText(previousFilter.prompt, TextView.BufferType.EDITABLE)
            binding.etPrompt.requestFocus()

            dialog.setOnShowListener {
                val bottomSheet = dialog.findViewById<View>(
                    com.google.android.material.R.id.design_bottom_sheet
                ) as? FrameLayout ?: return@setOnShowListener

                val behavior = BottomSheetBehavior.from(bottomSheet)
                bottomSheet.setupFullHeight()
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }

            dialog.show()
            activity.showKeyboard()
        }
    }
}