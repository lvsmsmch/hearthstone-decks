package com.cyberquick.hearthstonedecks.utils

import android.util.Log
import com.cyberquick.hearthstonedecks.BuildConfig
import com.google.firebase.messaging.FirebaseMessagingService

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (BuildConfig.DEBUG) Log.d("tag_token", token)
    }
}