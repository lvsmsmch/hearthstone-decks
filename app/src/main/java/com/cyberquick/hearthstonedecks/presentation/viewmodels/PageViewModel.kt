package com.cyberquick.hearthstonedecks.presentation.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cyberquick.hearthstonedecks.domain.entities.DecksFilter
import com.cyberquick.hearthstonedecks.domain.entities.Page
import com.cyberquick.hearthstonedecks.domain.usecases.base.GetPageUseCase
import com.cyberquick.hearthstonedecks.domain.usecases.favorite.GetFavoritePageUseCase
import com.cyberquick.hearthstonedecks.domain.usecases.online.GetOnlineStandardPageUseCase
import com.cyberquick.hearthstonedecks.domain.usecases.online.GetOnlineWildPageUseCase
import com.cyberquick.hearthstonedecks.utils.transform
import com.cyberquick.hearthstonedecks.utils.transformWithDefault
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import javax.inject.Inject


@HiltViewModel
class OnlineStandardPageViewModel @Inject constructor(
    getPageUseCase: GetOnlineStandardPageUseCase,
) : PageViewModel(getPageUseCase)

@HiltViewModel
class OnlineWildPageViewModel @Inject constructor(
    getPageUseCase: GetOnlineWildPageUseCase,
) : PageViewModel(getPageUseCase)

@HiltViewModel
class FavoritePageViewModel @Inject constructor(
    getPageUseCase: GetFavoritePageUseCase,
) : PageViewModel(getPageUseCase)

open class PageViewModel(
    private val getPageUseCase: GetPageUseCase,
) : BaseViewModel() {

    data class Position(val current: Int, val total: Int?)
    data class AllowNavigation(val previous: Boolean, val next: Boolean)

    private var filter: DecksFilter = DecksFilter.default

    private val _pageState = MutableLiveData<LoadingState<Page>>()
    val pageState: LiveData<LoadingState<Page>> = _pageState

    val position: LiveData<Position> = transformWithDefault(
        source = pageState,
        defaultValue = Position(FIRST_PAGE_INDEX, null)
    ) {
        return@transformWithDefault it.asLoaded()?.result?.let { page ->
            Position(page.number, page.totalPagesAmount)
        }
    }

    val allowNavigation: LiveData<AllowNavigation> = transform(position) {
        val previousAllowed = it.current > 1
        val nextAllowed = (it.total != null) && (it.current < it.total)
        return@transform AllowNavigation(previousAllowed, nextAllowed)
    }

    private var loadingPageJob: Job? = null

    fun updateCurrentPage(evenIfLoaded: Boolean) {
        loadPage(position.value!!.current, evenIfLoaded)
    }

    fun loadNextPage() {
        loadPage(position.value!!.current + 1)
    }

    fun loadPreviousPage() {
        loadPage(position.value!!.current - 1)
    }

    private fun loadPage(pageNumber: Int, evenIfLoaded: Boolean = false) {
        if (isPageLoaded(pageNumber) && !evenIfLoaded) return
        position.postValue(Position(pageNumber, position.value!!.total))
        loadingPageJob?.cancel()
        loadingPageJob = makeLoadingRequest(pageState, allowInterrupt = true) {
            getPageUseCase(pageNumber, filter)
        }
    }

    fun applyNewFilter(filter: DecksFilter) {
        if (this.filter == filter) {
            return
        }

        this.filter = filter
        updateCurrentPage(evenIfLoaded = true)
    }

    fun getCurrentFilter(): DecksFilter {
        return filter
    }

    private fun isPageLoaded(pageNumber: Int) : Boolean {
        return pageState.value.asLoaded()?.result?.number == pageNumber
    }

    companion object {
        private const val FIRST_PAGE_INDEX = 1
    }
}

