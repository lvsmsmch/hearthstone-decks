package com.cyberquick.hearthstonedecks.data.server.battlenet

import android.util.Log
import com.cyberquick.hearthstonedecks.BuildConfig
import com.cyberquick.hearthstonedecks.data.server.battlenet.hearthstone.BattleNetApi
import com.cyberquick.hearthstonedecks.data.server.battlenet.oauth.OAuthApi
import com.cyberquick.hearthstonedecks.domain.common.Result
import com.cyberquick.hearthstonedecks.domain.entities.Card
import com.cyberquick.hearthstonedecks.domain.entities.Expansion
import com.cyberquick.hearthstonedecks.domain.entities.ExpansionYear
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Call
import retrofit2.HttpException
import java.lang.Exception
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BattleNetRepository @Inject constructor(
    private val oAuthApi: OAuthApi,
    private val battleNetApi: BattleNetApi,
) {

    private val tokenMutex = Mutex()
    @Volatile private var currentToken: String? = null

    suspend fun retrieveCards(code: String): Result<List<Card>> {
        if (BuildConfig.DEBUG) {
            Log.d("tag_network", "retrieveCards...")
            Log.d("tag_network", "getUserRegion = ${getUserRegion()}")
            Log.d("tag_network", "getUserLanguage = ${getUserLanguage()}")
        }
        return doCall(
            createNetworkCall = {
                battleNetApi.getDeck(
                    token = "Bearer $currentToken",
                    locale = getUserLanguage(),
                    code = code,
                )
            }
        ).map { deckResponse -> deckResponse.cards.sortedBy { it.manaCost } }
    }

    suspend fun retrieveSets(): Result<List<Expansion>> {
        return doCall(
            createNetworkCall = {
                battleNetApi.getSets(
                    token = "Bearer $currentToken",
                    locale = getUserLanguage(),
                )
            }
        )
    }

    suspend fun retrieveSetGroups(): Result<List<ExpansionYear>> {
        return doCall(
            createNetworkCall = {
                battleNetApi.getSetGroups(
                    token = "Bearer $currentToken",
                    locale = getUserLanguage(),
                )
            }
        )
    }

    private suspend fun <T> doCall(
        createNetworkCall: suspend () -> Call<T>
    ): Result<T> {
        if (currentToken == null) {
            when (val tokenResult = ensureFreshToken(staleToken = null)) {
                is Result.Success -> Unit
                is Result.Error -> return Result.Error(tokenResult.exception)
            }
        }

        val firstAttempt = createNetworkCall().getResult()
        if (firstAttempt is Result.Success) return firstAttempt

        // Retry only if the failure looks like an auth issue (401/403 from Blizzard).
        // Other failures (network, 5xx, parse) are returned as-is to avoid wasting an
        // OAuth round-trip on every transient hiccup.
        val staleToken = currentToken
        val firstError = (firstAttempt as Result.Error).exception
        if (!firstError.looksLikeAuthFailure()) return firstAttempt

        return when (val tokenResult = ensureFreshToken(staleToken = staleToken)) {
            is Result.Success -> createNetworkCall().getResult()
            is Result.Error -> Result.Error(tokenResult.exception)
        }
    }

    private suspend fun ensureFreshToken(staleToken: String?): Result<String> = tokenMutex.withLock {
        // If another caller already replaced the stale token while we were waiting,
        // skip the network call and reuse what's there.
        currentToken?.let { existing ->
            if (existing != staleToken) return@withLock Result.Success(existing)
        }
        when (val fetched = fetchToken()) {
            is Result.Success -> {
                currentToken = fetched.data
                Result.Success(fetched.data)
            }
            is Result.Error -> Result.Error(fetched.exception)
        }
    }

    private suspend fun fetchToken(): Result<String> {
        return try {
            val token = oAuthApi.retrieveOAuth().access_token
            Result.Success(token)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private fun Throwable.looksLikeAuthFailure(): Boolean {
        if (this is HttpException) return code() == 401 || code() == 403
        val msg = message.orEmpty()
        return msg.startsWith("401 ") || msg.startsWith("403 ")
    }

    /**
     * Might be US, EU, KR, TW, CN. The hostname is fixed in DataModule
     * (BLIZZARD_API_URL) — this is kept for logging only.
     */
    private fun getUserRegion(): String {
        return "eu"
    }

    // Locales supported by the Hearthstone API. Anything else falls back to en_US.
    // See battle_net_api.txt (Localization).
    private fun getUserLanguage(): String {
        return when (Locale.getDefault().language) {
            "de" -> "de_DE"
            "en" -> "en_US"
            "es" -> if (Locale.getDefault().country == "MX") "es_MX" else "es_ES"
            "fr" -> "fr_FR"
            "it" -> "it_IT"
            "ja" -> "ja_JP"
            "ko" -> "ko_KR"
            "pl" -> "pl_PL"
            "pt" -> "pt_BR"
            "ru" -> "ru_RU"
            "th" -> "th_TH"
            "zh" -> "zh_TW"
            else -> "en_US"
        }
    }

    private fun <T> Call<T>.getResult(): Result<T> {
        return try {
            val response = this.execute()
            when {
                !response.isSuccessful ->
                    Result.Error(Exception("${response.code()} ${response.message()}"))
                else -> response.body()
                    ?.let { Result.Success(it) }
                    ?: Result.Error(Exception("Empty response body (HTTP ${response.code()})"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}