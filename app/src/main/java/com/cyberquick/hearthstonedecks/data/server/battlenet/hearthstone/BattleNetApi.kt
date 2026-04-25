package com.cyberquick.hearthstonedecks.data.server.battlenet.hearthstone

import com.cyberquick.hearthstonedecks.domain.entities.Expansion
import com.cyberquick.hearthstonedecks.domain.entities.ExpansionYear
import retrofit2.Call
import retrofit2.http.*

interface BattleNetApi {
    @GET("hearthstone/deck")
    fun getDeck(
        @Header("Authorization") token: String,
        @Query("locale") locale: String,
        @Query("code") code: String,
    ): Call<DeckResponse>

    @GET("hearthstone/metadata/sets")
    fun getSets(
        @Header("Authorization") token: String,
        @Query("locale") locale: String,
    ): Call<List<Expansion>>

    @GET("hearthstone/metadata/setGroups")
    fun getSetGroups(
        @Header("Authorization") token: String,
        @Query("locale") locale: String,
    ): Call<List<ExpansionYear>>
}