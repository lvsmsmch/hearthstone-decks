package com.cyberquick.hearthstonedecks.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.cyberquick.hearthstonedecks.data.db.DATABASE_NAME
import com.cyberquick.hearthstonedecks.data.db.RoomDB
import com.cyberquick.hearthstonedecks.data.repository.OnlineDecksImpl
import com.cyberquick.hearthstonedecks.data.repository.FavoriteDecksImpl
import com.cyberquick.hearthstonedecks.data.repository.SetsImpl
import com.cyberquick.hearthstonedecks.data.server.battlenet.hearthstone.BattleNetApi
import com.cyberquick.hearthstonedecks.data.server.battlenet.oauth.OAuthApi
import com.cyberquick.hearthstonedecks.domain.repositories.OnlineDecksRepository
import com.cyberquick.hearthstonedecks.domain.repositories.FavoriteDecksRepository
import com.cyberquick.hearthstonedecks.domain.repositories.SetsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DataModule {

    @Provides
    @Singleton
    fun provideContext(appInstance: Application): Context = appInstance.applicationContext

    // database
    @Provides
    @Singleton
    fun provideAppDatabase(application: Application) = Room
        .databaseBuilder(application, RoomDB::class.java, DATABASE_NAME)
//        .fallbackToDestructiveMigration()
        .build()


    // retrofit
    companion object {
        private const val BLIZZARD_API_URL = "https://eu.api.blizzard.com/"
        private const val BLIZZARD_OAUTH_URL = "https://us.battle.net/"
        private const val HTTP_TIMEOUT_SECONDS = 15L
    }

    @Provides
    @Singleton
    fun provideHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    private fun <API_TYPE> retrofit(
        client: OkHttpClient,
        baseUrl: String,
        apiClass: Class<API_TYPE>,
    ): API_TYPE =
        Retrofit.Builder()
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(baseUrl)
            .build()
            .create(apiClass)

    @Provides
    @Singleton
    fun provideOAuthApi(client: OkHttpClient): OAuthApi =
        retrofit(client, BLIZZARD_OAUTH_URL, OAuthApi::class.java)

    @Provides
    @Singleton
    fun provideCardsApi(client: OkHttpClient): BattleNetApi =
        retrofit(client, BLIZZARD_API_URL, BattleNetApi::class.java)



    // other
//    @Provides
//    @Singleton
//    fun provideBattleNetApi(
//        oAuthApi: OAuthApi,
//        deckApi: DeckApi,
//        metadataApi: MetadataApi,
//    ) = BattleNetApi(oAuthApi, deckApi, metadataApi)

    @Provides
    fun provideDeckDao(appDatabase: RoomDB) = appDatabase.deckDao()

    @Provides
    @Singleton
    fun provideServerDataRepository(
        onlineDecksImpl: OnlineDecksImpl
    ): OnlineDecksRepository = onlineDecksImpl

    @Provides
    @Singleton
    fun provideFavoriteDataRepository(
        favoriteDecksImpl: FavoriteDecksImpl
    ): FavoriteDecksRepository = favoriteDecksImpl


    @Provides
    @Singleton
    fun provideSetsRepository(
        setsImpl: SetsImpl
    ): SetsRepository = setsImpl
}