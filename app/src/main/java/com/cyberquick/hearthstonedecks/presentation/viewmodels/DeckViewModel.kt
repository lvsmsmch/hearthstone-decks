package com.cyberquick.hearthstonedecks.presentation.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.cyberquick.hearthstonedecks.domain.common.Result
import com.cyberquick.hearthstonedecks.domain.entities.Card
import com.cyberquick.hearthstonedecks.domain.entities.Deck
import com.cyberquick.hearthstonedecks.domain.entities.DeckPreview
import com.cyberquick.hearthstonedecks.domain.usecases.common.GetDeckUseCase
import com.cyberquick.hearthstonedecks.domain.usecases.favorite.AddDeckToFavoriteUseCase
import com.cyberquick.hearthstonedecks.domain.usecases.favorite.IsDeckInFavoriteUseCase
import com.cyberquick.hearthstonedecks.domain.usecases.favorite.RemoveDeckFromFavoriteUseCase
import com.cyberquick.hearthstonedecks.utils.Event
import com.cyberquick.hearthstonedecks.utils.logFirebaseEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeckViewModel @Inject constructor(
    private val getDeckUseCase: GetDeckUseCase,
    private val isDeckFavoriteUseCase: IsDeckInFavoriteUseCase,
    private val addDeckToFavoriteUseCase: AddDeckToFavoriteUseCase,
    private val removeDeckFromFavoriteUseCase: RemoveDeckFromFavoriteUseCase,
) : BaseViewModel() {

    val stateDeck: LiveData<LoadingState<Deck>> = MutableLiveData()

    private val _stateDeckSaved = MutableLiveData<SavedState>()
    val stateDeckSaved: LiveData<SavedState> = _stateDeckSaved

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun consumeError() {
        _error.value = null
    }

    fun loadDeck(deckPreview: DeckPreview) {
        if (isDeckLoaded(deckPreview)) return
        makeLoadingRequest(stateDeck) {
            return@makeLoadingRequest getDeckUseCase(deckPreview)
        }
    }

    fun clickedOnSaveButton(deck: Deck, cards: List<Card>) {
        // Synchronous main-thread guard: rapid double-tap sees Loading immediately
        // and the second click bails before launching another coroutine.
        val oldSavedState = _stateDeckSaved.value ?: return
        if (oldSavedState == SavedState.Loading) return
        _stateDeckSaved.value = SavedState.Loading

        viewModelScope.launch(createJob() + Dispatchers.IO) {
            val newSavingRequest = when (oldSavedState) {
                SavedState.NotSaved -> addDeckToFavoriteUseCase(deck.deckPreview)
                SavedState.Saved -> removeDeckFromFavoriteUseCase(deck.deckPreview)
                SavedState.Loading -> return@launch
            }

            when (newSavingRequest) {
                is Result.Success -> _stateDeckSaved.postValue(SavedState.opposite(oldSavedState))
                is Result.Error -> {
                    _stateDeckSaved.postValue(oldSavedState)
                    _error.postValue(newSavingRequest.exception.message.toString())
                }
            }
        }
    }

    fun updateIsDeckSaved(deckPreview: DeckPreview) {
        viewModelScope.launch(createJob() + Dispatchers.IO) {
            _stateDeckSaved.postValue(SavedState.fromResult(isDeckFavoriteUseCase(deckPreview)))
        }
    }

    private fun isDeckLoaded(deckPreview: DeckPreview): Boolean {
        return stateDeck.value.asLoaded()?.result?.deckPreview == deckPreview
    }

    override fun onCleared() {
        super.onCleared()
        stateDeck.setToDefault()
        _stateDeckSaved.value = null
        _error.value = null
    }
}