package com.cyberquick.hearthstonedecks.data.server.hearthpwn

import android.util.Log
import com.cyberquick.hearthstonedecks.data.server.entities.DeckDetails
import com.cyberquick.hearthstonedecks.domain.common.Result
import com.cyberquick.hearthstonedecks.domain.entities.DeckPreview
import com.cyberquick.hearthstonedecks.domain.entities.GameFormat
import com.cyberquick.hearthstonedecks.domain.entities.DecksFilter
import com.cyberquick.hearthstonedecks.domain.entities.Page
import com.cyberquick.hearthstonedecks.domain.exceptions.LoadFailedException
import com.cyberquick.hearthstonedecks.domain.exceptions.NoOnlineDecksFoundException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class HearthpwnApi @Inject constructor() {

    companion object {
        private const val MAX_TIMEOUT_LOADING_MS = 10_000L
        private const val URL_ROOT = "https://www.hearthpwn.com"
        private const val URL_STANDARD_DECKS = URL_ROOT +
                "/decks?filter-show-standard=1&filter-show-constructed-only=y"
        private const val URL_WILD_DECKS = URL_ROOT +
                "/decks?filter-show-standard=2&filter-show-constructed-only=y"

        // Tested against hearthpwn's Cloudflare: these UAs are classified as
        // benign scripts and pass through; "real" Chrome/Safari UAs get a
        // Managed Challenge (cf-mitigated: challenge) which Jsoup can't solve.
        private val USER_AGENTS = listOf(
            "okhttp/4.12.0",
            "Mozilla/5.0 (jsoup)",
            "Java/17.0.10",
        )
    }

    private val cookieJar = InMemoryCookieJar()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(MAX_TIMEOUT_LOADING_MS, TimeUnit.MILLISECONDS)
            .readTimeout(MAX_TIMEOUT_LOADING_MS, TimeUnit.MILLISECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .cookieJar(cookieJar)
            .build()
    }

    private suspend fun getDocument(url: String): Document {
        var lastFailure: Throwable? = null

        for (ua in USER_AGENTS) {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", ua)
                .header(
                    "Accept",
                    "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
                )
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Referer", "$URL_ROOT/")
                .build()

            Log.d("tag_hp_403", "REQ ua=$ua -> $url")
            try {
                httpClient.newCall(request).awaitResponse().use { response ->
                    val status = response.code
                    val server = response.header("server")
                    val cfMitigated = response.header("cf-mitigated")
                    val cfRay = response.header("cf-ray")
                    val contentType = response.header("content-type")
                    val body = response.body?.string().orEmpty()
                    val sample = body.take(400).replace("\n", " ")

                    Log.d(
                        "tag_hp_403",
                        "RES ua=$ua status=$status server=$server cf-mitigated=$cfMitigated " +
                                "cf-ray=$cfRay content-type=$contentType cookies=${cookieJar.snapshotForHost(request.url.host).size}"
                    )

                    if (status in 200..299) {
                        Log.d("tag_hp_403", "OK ua=$ua bodyLen=${body.length}")
                        return Jsoup.parse(body, url)
                    }

                    Log.d("tag_hp_403", "BODY[0..400]: $sample")
                    lastFailure = HttpStatusException(
                        "HTTP error fetching URL. Status=$status, URL=[$url]",
                        status,
                        url
                    )
                }
            } catch (e: IOException) {
                Log.d("tag_hp_403", "IO failure ua=$ua: ${e.javaClass.simpleName}: ${e.message}")
                lastFailure = e
            }
        }

        throw lastFailure
            ?: IOException("HTTP error fetching URL. Status=-1, URL=[$url] (all UA attempts failed)")
    }

    /**
     * Bridge OkHttp's async API into a suspend function whose cancellation
     * actually aborts the in-flight HTTP request (vs. blocking execute(),
     * which keeps running and just discards the result).
     */
    private suspend fun Call.awaitResponse(): Response =
        suspendCancellableCoroutine { cont ->
            enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (cont.isActive) cont.resumeWithException(e)
                }
                override fun onResponse(call: Call, response: Response) {
                    if (cont.isActive) cont.resume(response)
                    else response.close()
                }
            })
            cont.invokeOnCancellation {
                runCatching { cancel() }
            }
        }

    private class InMemoryCookieJar : CookieJar {
        private val store = ConcurrentHashMap<String, MutableList<Cookie>>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            if (cookies.isEmpty()) return
            val bucket = store.getOrPut(url.host) { mutableListOf() }
            synchronized(bucket) {
                bucket.removeAll { existing -> cookies.any { it.name == existing.name } }
                bucket.addAll(cookies)
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val bucket = store[url.host] ?: return emptyList()
            val now = System.currentTimeMillis()
            return synchronized(bucket) {
                bucket.removeAll { it.expiresAt < now }
                bucket.toList()
            }
        }

        fun snapshotForHost(host: String): List<Cookie> =
            store[host]?.toList().orEmpty()
    }

    /**
     * Example url:
     * https://www.hearthpwn.com/decks?filter-search=pirate&filter-show-standard=1&filter-show-constructed-only=y&filter-deck-tag=2&filter-class=128
     */

    suspend fun getPage(
        pageNumber: Int,
        gameFormatToLoad: GameFormat,
        filter: DecksFilter
    ): Result<Page> {
        val deckPreviews = mutableListOf<DeckPreview>()

        val heroesFilterIndex = filter.heroes.sumOf { it.filterIndex }

        val url = when (gameFormatToLoad) {
            GameFormat.Standard -> URL_STANDARD_DECKS
            GameFormat.Wild -> URL_WILD_DECKS
        } +
                "&page=$pageNumber" +
                "&filter-class=$heroesFilterIndex" +
                "&filter-search=${filter.prompt}" +
                "&filter-deck-tag=2" // "new" filter


        val document = try {
            getDocument(url = url)
        } catch (e: IOException) {
            return Result.Error(LoadFailedException(message = e.message.toString()))
        }

        val element = document
            .select("table[class=listing listing-decks b-table b-table-a]")
            .select("tbody")
            .select("tr")

        if (element.size == 0) {
            return Result.Error(NoOnlineDecksFoundException())
        }

        if (element.size == 1 && element[0].select("td").attr("class") == "alert no-results") {
            return Result.Error(NoOnlineDecksFoundException())
        }

        val totalPages = document
            .select("ul[class=b-pagination-list paging-list j-tablesorter-pager j-listing-pagination]")
            .select("li[class=b-pagination-item]")
            .let { paginationNumbers ->
                if (paginationNumbers.size == 0) {
                    return@let 1
                }
                val lastNumber = paginationNumbers.eq(paginationNumbers.size - 1)
                var result = lastNumber.select("a").text()
                if (result.isBlank()) result = lastNumber.select("span").text()
                return@let result.toIntOrNull() ?: 1
            }


        for (i in 0 until element.size) {
            val currentElement = element.eq(i)
            try {
                val title = currentElement
                    .select("td.col-name")
                    .select("div")
                    .select("span.tip")
                    .select("a")
                    .text()

                val gameClass = currentElement
                    .select("td.col-class")
                    .text()

                val dust = currentElement
                    .select("td.col-dust-cost")
                    .text()

                val timeCreated = currentElement
                    .select("td.col-updated")
                    .select("abbr")
                    .attr("title")
                    .let { return@let formatToCorrectDate(it) }

                val timeEpoch = currentElement
                    .select("td.col-updated")
                    .select("abbr")
                    .attr("data-epoch")

                Log.d("tag_api", "timeEpoch $timeEpoch")

                val detailsUrl = URL_ROOT + currentElement
                    .select("td.col-name")
                    .select("div")
                    .select("span.tip")
                    .select("a")
                    .attr("href")

                val gameFormat = currentElement
                    .select("td.col-deck-type")
                    .select("span")
                    .attr("class")

                val views = currentElement
                    .select("td.col-views")
                    .text()
                    .toIntOrNull() ?: 0

                val author = currentElement
                    .select("td.col-name")
                    .select("div")
                    .select("small")
                    .select("a")
                    .text()

                val rating = currentElement
                    .select("td.col-ratings")
                    .select("div")
                    .text()

                val deckType = currentElement
                    .select("td.col-deck-type")
                    .select("span")
                    .text()

                Log.i("tag_fix_crash", "Details url $i: $detailsUrl")
                val id = detailsUrl.substringAfterLast("/").substringBefore("-").toIntOrNull()
                    ?: continue

                deckPreviews.add(
                    DeckPreview(
                        id = id,
                        title = title,
                        gameClass = gameClass,
                        dust = dust,
                        timeCreated = timeCreated,
                        deckUrl = detailsUrl,
                        gameFormat = gameFormat,
                        views = views,
                        author = author,
                        rating = rating,
                        deckType = deckType,
                    ),
                )
            } catch (e: Exception) {
                Log.w("tag_api", "Skipping malformed deck row $i: ${e.javaClass.simpleName}: ${e.message}")
            }
        }

        return Result.Success(Page(totalPages, pageNumber, deckPreviews))
    }

    suspend fun getDeckDetails(deckPreview: DeckPreview): Result<DeckDetails> {
        val document = try {
            getDocument(url = deckPreview.deckUrl)
        } catch (e: IOException) {
            return Result.Error(e)
        }

        // code
        val deckCode = document
            .select("button[class=copy-button button]")
            .attr("data-clipboard-text")

        // description
        val descriptionElements = document
            .select("div[class=u-typography-format deck-description]")
            .select("div")
            .select("p")

        var description = ""
        for (i in 0 until descriptionElements.size) {
            description += descriptionElements.eq(i).text() + "\n\n"
        }
        description = description.trim()

        return Result.Success(DeckDetails(deckCode, description))
    }

    /**
     * Original format:
     * 08 28 2022 02:57:50 (CDT) (UTC-5:00)
     * Corrected format:
     * 28.08.2022
     */
    private fun formatToCorrectDate(source: String): String {
        var sourceCorrectable = source
        val month = sourceCorrectable.take(2).trim()
        sourceCorrectable = sourceCorrectable.substringAfter(" ")
        val day = sourceCorrectable.take(2).trim()
        sourceCorrectable = sourceCorrectable.substringAfter(" ")
        val year = sourceCorrectable.take(4).trim()
        return "$day.$month.$year"
    }
}