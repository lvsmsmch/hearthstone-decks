package com.cyberquick.hearthstonedecks.presentation.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyberquick.hearthstonedecks.domain.common.Result
import com.cyberquick.hearthstonedecks.utils.delayIfExecutionTimeIsSmall
import kotlinx.coroutines.*

open class BaseViewModel : ViewModel() {

    /**
     * We need this jobs to cancel them in 'onCleared()',
     * so all of them will stop their work and they will not change
     * the live data anymore after 'onCleared()' is called.
     */

    private var jobs = mutableListOf<Job>()

    protected fun createJob() = Job().apply { jobs.add(this) }

    override fun onCleared() {
        super.onCleared()
        jobs.forEach { it.cancel() }
        jobs.clear()
    }

    protected fun <T> LiveData<T>.postValue(value: T?) {
        (this as? MutableLiveData)?.postValue(value)
    }

    protected fun <T> LiveData<T>.setToDefault() {
        (this as? MutableLiveData)?.value = null
    }

    /**
     * Delay if we get an error and a response time was very small, because a user
     * might get an idea that the app has not reacted on his click.
     */

    protected fun <T : Any> makeLoadingRequest(
        liveData: LiveData<LoadingState<T>>,
        allowInterrupt: Boolean = false,
        source: suspend (() -> Result<T>),
    ): Job {
        val job = createJob()
        viewModelScope.launch(job + Dispatchers.IO) {
            if (liveData.value.isLoading() && !allowInterrupt) return@launch
            liveData.postValue(LoadingState.Loading)
            val startTime = System.currentTimeMillis()
            val response = try {
                source()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (e: Exception) {
                Result.Error(e)
            }
            yield()
            if (response is Result.Error) {
                val executionTime = System.currentTimeMillis() - startTime
                delayIfExecutionTimeIsSmall(executionTime)
            }
            liveData.postValue(LoadingState.fromResult(response))
        }
        return job
    }
}