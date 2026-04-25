package com.cyberquick.hearthstonedecks.data.server.battlenet.oauth

import android.util.Base64
import com.cyberquick.hearthstonedecks.BuildConfig
import retrofit2.http.*


interface OAuthApi {

    companion object {
        private val AUTH_HEADER: String by lazy {
            val raw = "${BuildConfig.BLIZZARD_CLIENT_ID}:${BuildConfig.BLIZZARD_CLIENT_SECRET}"
            "Basic " + Base64.encodeToString(raw.toByteArray(), Base64.NO_WRAP)
        }
    }

    @FormUrlEncoded
    @POST("oauth/token")
    suspend fun retrieveOAuth(
        @Header("Authorization") authorization: String = AUTH_HEADER,
        @Field("grant_type") grantType: String = "client_credentials"
    ): OAuthResponse
}