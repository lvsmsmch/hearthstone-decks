package com.cyberquick.hearthstonedecks.presentation.fragments

import android.animation.LayoutTransition
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ShareCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.cyberquick.hearthstonedecks.R
import com.cyberquick.hearthstonedecks.databinding.FragmentDeckBinding
import com.cyberquick.hearthstonedecks.domain.common.deckPreviewFromJson
import com.cyberquick.hearthstonedecks.domain.common.toCardsCountable
import com.cyberquick.hearthstonedecks.domain.entities.DeckPreview
import com.cyberquick.hearthstonedecks.domain.repositories.SetsRepository
import com.cyberquick.hearthstonedecks.presentation.adapters.CardSmallAdapter
import com.cyberquick.hearthstonedecks.presentation.adapters.DeckViewHolder
import com.cyberquick.hearthstonedecks.presentation.fragments.base.BaseFragment
import com.cyberquick.hearthstonedecks.presentation.viewmodels.DeckViewModel
import com.cyberquick.hearthstonedecks.presentation.viewmodels.LoadingState
import com.cyberquick.hearthstonedecks.presentation.viewmodels.SavedState.Saved
import com.cyberquick.hearthstonedecks.utils.Event
import com.cyberquick.hearthstonedecks.utils.drawable
import com.cyberquick.hearthstonedecks.utils.expand
import com.cyberquick.hearthstonedecks.utils.logFirebaseEvent
import com.cyberquick.hearthstonedecks.utils.toast
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class DeckFragment : BaseFragment() {

    companion object {
        const val KEY_DECK_PREVIEW = "deck_preview"
    }

    @Inject
    lateinit var setsRepository: SetsRepository

    private val viewModel: DeckViewModel by viewModels()

    private var _binding: FragmentDeckBinding? = null
    private val binding get() = _binding!!

    private lateinit var deckPreview: DeckPreview

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeckBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        restoreExtras()
        initView()
        initData()
    }

    private fun restoreExtras() {
        deckPreview = deckPreviewFromJson(arguments?.getString(KEY_DECK_PREVIEW)!!)
    }

    private fun initView() {
        toolbarHolder.showToolbar()
        toolbarHolder.showHomeButtonAsBack()
        toolbarHolder.setText(deckPreview.title)

        DeckViewHolder(DeckViewHolder.Content.fromView(binding.deckPreview)).bind(deckPreview)

        binding.deckPreview.cardView.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun initData() {
        binding.layoutProgressBar.layoutProgressBar.isVisible = true

        viewModel.stateDeck.observe(viewLifecycleOwner) { state ->
            if (state is LoadingState.Loading) {
                binding.layoutProgressBar.layoutProgressBar.isVisible = true
            }

            if (state is LoadingState.Failed) {
                binding.layoutProgressBar.layoutProgressBar.isVisible = false
                binding.layoutFailed.layoutFailed.isVisible = true
                binding.layoutFailed.tvErrorLoadingDataSmall.text =
                    state.exception.message.toString()
                binding.layoutFailed.btnReloadData.setOnClickListener {
                    binding.layoutFailed.layoutFailed.isVisible = false
                    viewModel.loadDeck(deckPreview)
                }
            }

            if (state is LoadingState.Loaded) {
                val deck = state.result

                binding.btnCopyDeck.setOnClickListener {
                    logFirebaseEvent(context, Event.DECK_COPY_CODE)
                    binding.btnCopyDeckIcon.setImageDrawable(drawable(R.drawable.ic_copy_filled))
                    copyToClipboard(deck.code)
                }
                binding.btnShare.setOnClickListener {
                    logFirebaseEvent(context, Event.DECK_SHARE)
                    shareLink(deck.deckPreview.deckUrl)
                }

                val cards = state.result.cards

                binding.recycleViewCards.apply {
                    layoutManager = GridLayoutManager(
                        context, CardSmallAdapter.TOTAL_ITEMS_HORIZONTAL,
                        LinearLayoutManager.VERTICAL, false
                    )
                    adapter = CardSmallAdapter(setsRepository).apply {
                        set(cards.toCardsCountable())
                    }
                    isNestedScrollingEnabled = false
                }

                binding.btnFavorite.layoutTransition
                    .enableTransitionType(LayoutTransition.CHANGING)

                binding.btnFavorite.setOnClickListener {
                    logFirebaseEvent(context, Event.DECK_CLICK_SAVE_BUTTON)
                    viewModel.clickedOnSaveButton(deck, cards)
                }

                binding.layoutProgressBar.layoutProgressBar.isVisible = false
                binding.deckHolder.expand()
            }
        }

        viewModel.stateDeckSaved.observe(viewLifecycleOwner) { state ->
            val isSaved = state is Saved

            val newIconRes = when (isSaved) {
                true -> R.drawable.ic_star_filled
                false -> R.drawable.ic_star_not_filled
            }
            val newTextRes = when (isSaved) {
                true -> R.string.saved
                false -> R.string.save
            }

            binding.btnFavoriteIcon.setImageDrawable(drawable(newIconRes))
            binding.btnFavoriteText.setText(newTextRes)
        }

        viewModel.error.observe(viewLifecycleOwner) { message ->
            if (message == null) return@observe
            toast(message)
            viewModel.consumeError()
        }

        viewModel.updateIsDeckSaved(deckPreview)
        viewModel.loadDeck(deckPreview)
    }

    private fun copyToClipboard(text: String) {
        (requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("copy", text))
        toast(getString(R.string.copied_to_clipboard))
    }

    private fun shareLink(url: String) {
        ShareCompat.IntentBuilder(requireContext())
            .setType("text/plain")
            .setChooserTitle(getString(R.string.share_url))
            .setText(url)
            .startChooser()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}