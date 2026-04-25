package com.cyberquick.hearthstonedecks.presentation.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.viewpager2.widget.ViewPager2
import com.cyberquick.hearthstonedecks.R
import com.cyberquick.hearthstonedecks.databinding.DialogCardFullSizeBinding
import com.cyberquick.hearthstonedecks.presentation.adapters.CardFullSizeAdapter
import com.cyberquick.hearthstonedecks.presentation.common.entities.CardFullSizeData
import com.cyberquick.hearthstonedecks.utils.Event
import com.cyberquick.hearthstonedecks.utils.bold
import com.cyberquick.hearthstonedecks.utils.fromHtml
import com.cyberquick.hearthstonedecks.utils.logFirebaseEvent
import com.cyberquick.hearthstonedecks.utils.navBarHeightPixels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import jp.wasabeef.blurry.Blurry

class DialogPreviewCard(
    context: Context,
    private val sourceScreen: View,
    private val cards: List<CardFullSizeData>,
    private val selectedCardIndex: Int,
    private val onClosed: () -> Unit,
) : Dialog(context, R.style.ImagePreviewerTheme) {

    private lateinit var binding: DialogCardFullSizeBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogCardFullSizeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Keep the dialog window fully edge-to-edge (matches the activity
        // behind 1:1, no white bars at top/bottom). Insets are handled
        // surgically below — only on the elements that must stay above the
        // nav bar (the bottom sheet and its gradient overlay). The blur is
        // intentionally drawn under the system bars so its colour matches
        // the activity behind through the translucent bars.
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.navigationBarColor = Color.TRANSPARENT

        logFirebaseEvent(context, Event.CARDS_START_VIEWING)

        val screenshot = screenShot(sourceScreen.rootView)
        Blurry.with(context)
            .radius(5)  // default 25
            .sampling(1)    // default 1
            .from(screenshot).into(binding.blurredBackground)

        binding.blurredBackground.setOnClickListener {
            dismiss()
        }

        val bottomSheet = binding.bottomSheet
        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheet.setOnClickListener {
            bottomSheetBehavior.state = when (bottomSheetBehavior.state) {
                BottomSheetBehavior.STATE_EXPANDED -> BottomSheetBehavior.STATE_COLLAPSED
                else -> BottomSheetBehavior.STATE_EXPANDED
            }
        }

        // Push the bottom-sheet content up by the real nav-bar inset so the
        // text (Expansion / Quote / Artist) doesn't end up behind the 3-button
        // bar or the gesture pill. We read the bottom inset from the activity's
        // window (via sourceScreen.rootWindowInsets) — the framework dimen
        // android:navigation_bar_height is unreliable on modern Android (often
        // hard-coded to 48dp regardless of gesture/3-button mode).
        //
        // The bottom_sheet_overlay (40dp dark gradient at gravity=bottom) is
        // intentionally NOT shifted up: it lives in the nav-bar zone where the
        // system draws its buttons on top, so it stays out of the bottom-sheet
        // text. Pushing it up would darken the last line of the quote text.
        val navBarHeight = activityNavBarBottomInset()
        bottomSheet.updatePadding(bottom = navBarHeight)
        bottomSheetBehavior.peekHeight =
            bottomSheetBehavior.peekHeight + navBarHeight

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateTexts(selectedId = position)
                logFirebaseEvent(context, Event.CARD_VIEW)
            }
        })

        binding.viewPager.offscreenPageLimit = cards.size
        binding.viewPager.adapter = CardFullSizeAdapter(
            onPreviousItemClicked = { instantOpenPage(binding.viewPager.currentItem - 1) },
            onNextItemClicked = { instantOpenPage(binding.viewPager.currentItem + 1) },
            onCenterClicked = { dismiss() },
        ).apply { set(cards) }

        instantOpenPage(selectedCardIndex)
        updateTexts(selectedId = selectedCardIndex)
    }

    @SuppressLint("SetTextI18n")
    private fun updateTexts(selectedId: Int) {
        val card = cards[selectedId].cardCountable.card
        val dataAboutSet = cards[selectedId].dataAboutSet

        val expansion = dataAboutSet.setName
        val year = dataAboutSet.year
        val flavor = card.flavorText
        val artist = card.artistName

        binding.expansion.text = (context.getString(R.string.expansion).bold() + ": " + expansion)
            .fromHtml()

        binding.year.isVisible = year != null
        year?.let { nonNullYear ->
            binding.year.text = (context.getString(R.string.year).bold() + ": " + nonNullYear)
                .fromHtml()
        }

        binding.flavor.text = (context.getString(R.string.flavor).bold() + ": " + flavor)
            .fromHtml()
        binding.artist.text = (context.getString(R.string.artist).bold() + ": " + artist)
            .fromHtml()
    }

    private fun activityNavBarBottomInset(): Int {
        val raw = sourceScreen.rootWindowInsets ?: return context.navBarHeightPixels()
        val insets = WindowInsetsCompat.toWindowInsetsCompat(raw, sourceScreen)
            .getInsets(WindowInsetsCompat.Type.navigationBars())
        return insets.bottom.takeIf { it > 0 } ?: context.navBarHeightPixels()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        onClosed()
    }

    private fun instantOpenPage(index: Int) {
        binding.viewPager.setCurrentItem(index, false)
    }

    private fun screenShot(view: View): Bitmap? {
        val width = view.width
        val height = view.height
        if (width <= 0 || height <= 0) return null

        // Activity (edge-to-edge under targetSdk 35) and the dialog window
        // share the same full-screen geometry — capture rootView 1:1 so the
        // blurred backdrop aligns with what's behind the dialog including
        // the status bar and nav bar zones.
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))

        val scaleRatio = 6
        return Bitmap.createScaledBitmap(
            bitmap,
            bitmap.width / scaleRatio,
            bitmap.height / scaleRatio,
            false
        )
    }
}