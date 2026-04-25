package com.cyberquick.hearthstonedecks.data.db

import com.cyberquick.hearthstonedecks.data.db.dao.DeckDao
import com.cyberquick.hearthstonedecks.data.db.mappers.DBMapper
import com.cyberquick.hearthstonedecks.domain.entities.DeckPreview
import com.cyberquick.hearthstonedecks.domain.entities.DecksFilter
import com.cyberquick.hearthstonedecks.domain.entities.Page
import javax.inject.Inject
import kotlin.math.ceil

class RoomDBApi @Inject constructor(
    private val deckDao: DeckDao,
    private val dbMapper: DBMapper,
) {

    companion object {
        private const val ITEMS_ON_A_PAGE = 25
    }

    fun insert(deckPreview: DeckPreview) {
        deckDao.insert(dbMapper.toDeckEntity(deckPreview))
    }

    fun remove(deckPreview: DeckPreview) {
        deckDao.getDeckEntity(deckPreview.id)?.let {
            deckDao.delete(it)
        }
    }

    fun isSaved(deckPreview: DeckPreview): Boolean {
        return deckDao.getDeckEntity(deckPreview.id) != null
    }

    fun getPage(pageNumber: Int, filter: DecksFilter): Page {
        val heroesNames = filter.heroes.map { it.nameInApi }
        if (heroesNames.isEmpty()) {
            return Page(totalPagesAmount = 1, number = 1, deckPreviews = emptyList())
        }

        val totalMatching = deckDao.amountDecksFiltered(
            heroesNames = heroesNames,
            prompt = filter.prompt,
        )
        var totalPages = ceil(totalMatching / ITEMS_ON_A_PAGE.toFloat()).toInt()
        if (totalPages < 1) totalPages = 1

        val pageNumberToLoad = when {
            pageNumber > totalPages -> totalPages
            pageNumber < 1 -> 1
            else -> pageNumber
        }

        val offset = (pageNumberToLoad - 1) * ITEMS_ON_A_PAGE

        val entities = deckDao.getDeckEntitiesFiltered(
            heroesNames = heroesNames,
            prompt = filter.prompt,
            offset = offset,
            count = ITEMS_ON_A_PAGE,
        )

        val deckPreviews = entities.map { dbMapper.toDeckPreview(it) }

        return Page(totalPages, pageNumberToLoad, deckPreviews)
    }
}