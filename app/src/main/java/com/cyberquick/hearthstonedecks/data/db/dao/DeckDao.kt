package com.cyberquick.hearthstonedecks.data.db.dao

import androidx.room.*
import com.cyberquick.hearthstonedecks.data.db.entities.DeckEntity

@Dao
interface DeckDao {
    @Query("SELECT COUNT(*) FROM decks")
    fun amountDecks(): Int

    @Query(
        "SELECT COUNT(*) FROM decks " +
                "WHERE gameClass IN (:heroesNames) " +
                "AND LOWER(title) LIKE '%' || LOWER(:prompt) || '%'"
    )
    fun amountDecksFiltered(heroesNames: List<String>, prompt: String): Int

    @Query("SELECT * FROM decks LIMIT :count OFFSET :offset")
    fun getDeckEntities(offset: Int, count: Int): List<DeckEntity>

    @Query(
        "SELECT * FROM decks " +
                "WHERE gameClass IN (:heroesNames) " +
                "AND LOWER(title) LIKE '%' || LOWER(:prompt) || '%' " +
                "ORDER BY rowid DESC " +
                "LIMIT :count OFFSET :offset"
    )
    fun getDeckEntitiesFiltered(
        heroesNames: List<String>,
        prompt: String,
        offset: Int,
        count: Int,
    ): List<DeckEntity>

    @Query("SELECT * FROM decks WHERE id = :id")
    fun getDeckEntity(id: Int): DeckEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(deckEntities: DeckEntity): Long

    @Delete
    fun delete(deckEntity: DeckEntity)
}