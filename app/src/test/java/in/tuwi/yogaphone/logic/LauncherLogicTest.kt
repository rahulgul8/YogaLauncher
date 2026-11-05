package `in`.tuwi.yogaphone.logic

import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherLogicTest {
    private val apps = listOf(
        SearchableApp("Camera", "camera"),
        SearchableApp("Café Notes", "cafe"),
        SearchableApp("My Calendar", "calendar"),
        SearchableApp("Scam Detector", "scam"),
    )

    @Test
    fun search_isCaseAndDiacriticInsensitive() {
        assertEquals(listOf("cafe"), rankAppSearch(apps, "CAFE"))
    }

    @Test
    fun search_ranksWordPrefixAheadOfSubstring() {
        assertEquals(listOf("camera", "scam"), rankAppSearch(apps, "cam"))
    }

    @Test
    fun recentSearches_areNewestDistinctAndCapped() {
        assertEquals(listOf("Maps", "Mail", "Music"), updateRecentSearches(listOf("Mail", "Maps", "Music"), "Maps"))
        assertEquals(listOf("Clock", "Maps", "Mail"), updateRecentSearches(listOf("Maps", "Mail", "Music"), "Clock"))
    }

    @Test
    fun favoritesMoveAndInvalidMoveIsSafe() {
        assertEquals(listOf("b", "c", "a"), reorderFavoriteList(listOf("a", "b", "c"), 0, 2))
        assertEquals(listOf("a", "b", "c"), reorderFavoriteList(listOf("a", "b", "c"), -1, 2))
    }
}
