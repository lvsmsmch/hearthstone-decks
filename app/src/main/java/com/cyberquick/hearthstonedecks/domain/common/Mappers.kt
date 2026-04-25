package com.cyberquick.hearthstonedecks.domain.common

import com.cyberquick.hearthstonedecks.domain.entities.Card
import com.cyberquick.hearthstonedecks.domain.entities.CardCountable
import com.cyberquick.hearthstonedecks.domain.entities.DeckPreview
import com.google.gson.Gson

private val gson = Gson()

fun List<Card>.toCardsCountable(): List<CardCountable> {
    return this.groupBy { it }.map { CardCountable(it.key, amount = it.value.size) }
}

fun deckPreviewFromJson(json: String): DeckPreview {
    return gson.fromJson(json, DeckPreview::class.java)
}

fun deckPreviewToJson(deckPreview: DeckPreview): String {
    return gson.toJson(deckPreview)
}