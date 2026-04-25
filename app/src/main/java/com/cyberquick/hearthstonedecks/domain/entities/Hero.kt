package com.cyberquick.hearthstonedecks.domain.entities

import com.cyberquick.hearthstonedecks.R

enum class Hero(
    val filterIndex: Int,
    val nameInApi: String,
    val nameRes: Int,
    val iconRes: Int,
) {
    DeathKnight(
        2,
        "Death Knight",
        R.string.class_death_knight,
        R.drawable.deathknight,
    ),
    DemonHunter(
        16384,
        "Demon Hunter",
        R.string.class_demon_hunter,
        R.drawable.demonhunter,
    ),
    Druid(
        4,
        "Druid",
        R.string.class_druid,
        R.drawable.druid,
    ),
    Hunter(
        8,
        "Hunter",
        R.string.class_hunter,
        R.drawable.hunter,
    ),
    Mage(
        16,
        "Mage",
        R.string.class_mage,
        R.drawable.mage,
    ),
    Paladin(
        32,
        "Paladin",
        R.string.class_paladin,
        R.drawable.paladin,
    ),
    Priest(
        64,
        "Priest",
        R.string.class_priest,
        R.drawable.priest,
    ),
    Rogue(
        128,
        "Rogue",
        R.string.class_rogue,
        R.drawable.rogue,
    ),
    Shaman(
        256,
        "Shaman",
        R.string.class_shaman,
        R.drawable.shaman,
    ),
    Warlock(
        512,
        "Warlock",
        R.string.class_warlock,
        R.drawable.warlock,
    ),
    Warrior(
        1024,
        "Warrior",
        R.string.class_warrior,
        R.drawable.warrior,
    );

    companion object {
        fun from(deckPreview: DeckPreview) = entries.firstOrNull {
            it.nameInApi == deckPreview.gameClass
        }
    }
}