package com.cyberquick.hearthstonedecks.presentation.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cyberquick.hearthstonedecks.presentation.common.entities.AboutAppItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AboutAppViewModel @Inject constructor() : BaseViewModel() {

    val items: LiveData<List<AboutAppItem>> = MutableLiveData(AboutAppItem.entries)
}