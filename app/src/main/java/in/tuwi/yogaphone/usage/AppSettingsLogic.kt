package `in`.tuwi.yogaphone.usage

import `in`.tuwi.yogaphone.logic.SearchableApp
import `in`.tuwi.yogaphone.logic.rankAppSearch
import java.util.Locale

/** How Yoga treats an app, from loosest to strictest. */
enum class AppType(val label: String) {
    /** Never counted, never paused. */
    ESSENTIAL("Essential"),
    NORMAL("Normal"),
    /** Counted and paused before opening. */
    DISTRACTING("Distracting"),
}

/** Pure rules behind the per-app settings sheet and the Your apps list (L-F1). */
object AppSettingsLogic {
    fun typeOf(essential: Boolean, distracting: Boolean): AppType = when {
        essential -> AppType.ESSENTIAL
        distracting -> AppType.DISTRACTING
        else -> AppType.NORMAL
    }

    /** The (essential, distracting) pair that [UsageViewModel.classify] takes. */
    fun flags(type: AppType): Pair<Boolean, Boolean> = when (type) {
        AppType.ESSENTIAL -> true to false
        AppType.NORMAL -> false to false
        AppType.DISTRACTING -> false to true
    }

    /** Moving towards Essential is loosening (ADR 0015): it goes through the loosen sheet. */
    fun isLoosening(from: AppType, to: AppType): Boolean = to.ordinal < from.ordinal

    /** Removing a limit or raising it is loosening; adding or lowering one is instant. */
    fun limitIsLoosening(current: Int?, next: Int?): Boolean = current != null && (next == null || next > current)

    /** One line under an app's name, e.g. "Distracting · pause · 30 min a day". */
    fun summary(type: AppType, pauseOn: Boolean, limitMinutes: Int?): String = buildList {
        add(type.label)
        if (type == AppType.DISTRACTING) add(if (pauseOn) "pause" else "no pause")
        if (type != AppType.ESSENTIAL && limitMinutes != null) add("$limitMinutes min a day")
    }.joinToString(" · ")

    /**
     * The Your apps list. With no search: distracting first, then normal, then essential, each alphabetical.
     * With a search: the drawer's ranking, so the same words find the same apps.
     */
    fun <T> order(apps: List<T>, query: String, label: (T) -> String, type: (T) -> AppType): List<T> =
        if (query.isNotBlank()) rankAppSearch(apps.map { SearchableApp(label(it), it) }, query)
        else apps.sortedWith(compareBy<T> { -type(it).ordinal }.thenBy { label(it).lowercase(Locale.getDefault()) })
}
