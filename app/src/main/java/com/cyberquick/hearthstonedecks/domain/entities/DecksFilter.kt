package com.cyberquick.hearthstonedecks.domain.entities

data class DecksFilter(
    val prompt: String,
    val heroes: Set<Hero>
) {
    companion object {
        val default = DecksFilter("", Hero.entries.toSet())
    }

    fun isCustom() = this != default
}
