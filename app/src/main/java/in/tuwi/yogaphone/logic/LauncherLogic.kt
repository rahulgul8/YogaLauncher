package `in`.tuwi.yogaphone.logic

import java.text.Normalizer
import java.util.Locale

data class SearchableApp<T>(val label: String, val value: T)

fun <T> rankAppSearch(apps: List<SearchableApp<T>>, query: String): List<T> {
    val normalizedQuery = query.normalizedSearchText().trim()
    if (normalizedQuery.isEmpty()) return apps.sortedBy { it.label.lowercase(Locale.getDefault()) }.map { it.value }
    return apps.mapNotNull { app ->
        val label = app.label.normalizedSearchText()
        val words = label.split(Regex("\\s+")).filter(String::isNotBlank)
        val rank = when {
            words.any { it.startsWith(normalizedQuery) } -> 0
            label.contains(normalizedQuery) -> 1
            else -> return@mapNotNull null
        }
        Triple(rank, label, app.value)
    }.sortedWith(compareBy<Triple<Int, String, T>> { it.first }.thenBy { it.second }).map { it.third }
}

fun updateRecentSearches(current: List<String>, launchedTerm: String): List<String> {
    val clean = launchedTerm.trim()
    if (clean.isEmpty()) return current.take(3)
    return buildList {
        add(clean)
        current.filterNot { it.equals(clean, ignoreCase = true) }.forEach(::add)
    }.take(3)
}

fun <T> reorderFavoriteList(items: List<T>, fromIndex: Int, toIndex: Int): List<T> {
    if (fromIndex !in items.indices || toIndex !in items.indices || fromIndex == toIndex) return items
    return items.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
}

private fun String.normalizedSearchText(): String = Normalizer.normalize(this, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "")
    .lowercase(Locale.getDefault())
