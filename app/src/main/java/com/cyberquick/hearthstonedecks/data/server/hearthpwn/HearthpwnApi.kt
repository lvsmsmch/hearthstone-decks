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
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import javax.inject.Inject

class HearthpwnApi @Inject constructor() {

    companion object {
        private const val MAX_TIMEOUT_LOADING = 10 * 1000   // 10s
        private const val URL_ROOT = "https://www.hearthpwn.com"
        private const val URL_STANDARD_DECKS = URL_ROOT +
                "/decks?filter-show-standard=1&filter-show-constructed-only=y"
        private const val URL_WILD_DECKS = URL_ROOT +
                "/decks?filter-show-standard=2&filter-show-constructed-only=y"
    }

    private fun getDocument(url: String): Document {
        Log.d("tag_hp_403", "REQ -> $url")
        val response = Jsoup
            .connect(url)
            .maxBodySize(0)
            .timeout(MAX_TIMEOUT_LOADING)
            .ignoreHttpErrors(true)
            .execute()

        val status = response.statusCode()
        val headersOfInterest = listOf(
            "server", "cf-ray", "cf-mitigated", "cf-cache-status",
            "x-bot-score", "x-bot-type", "content-type", "content-length",
            "set-cookie", "location"
        ).joinToString(separator = " | ") { name ->
            "$name=${response.header(name) ?: "-"}"
        }
        val bodySample = (response.body() ?: "").take(800).replace("\n", " ")
        Log.d(
            "tag_hp_403",
            "RES <- status=$status url=${response.url()} headers[$headersOfInterest]"
        )
        Log.d("tag_hp_403", "BODY[0..800]: $bodySample")

        if (status !in 200..299) {
            throw org.jsoup.HttpStatusException(
                "HTTP error fetching URL. Status=$status, URL=[$url]",
                status,
                url
            )
        }
        return response.parse()
    }

    /**
     * Example url:
     * https://www.hearthpwn.com/decks?filter-search=pirate&filter-show-standard=1&filter-show-constructed-only=y&filter-deck-tag=2&filter-class=128
     */

    fun getPage(
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
                return@let result.toInt()
            }


        for (i in 0 until element.size) {
            val currentElement = element.eq(i)
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
            val id = detailsUrl.substringAfterLast("/").substringBefore("-").toInt()

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
        }

        return Result.Success(Page(totalPages, pageNumber, deckPreviews))
    }

    fun getDeckDetails(deckPreview: DeckPreview): Result<DeckDetails> {
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