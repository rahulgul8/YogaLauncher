package `in`.tuwi.yogaphone.profiles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryHeuristicsTest {
    @Test fun knownPackagesWinOverAndroidCategory() {
        assertEquals(Categories.SOCIAL, Categories.autoCategory("com.instagram.android", 2))
        assertEquals(Categories.FINANCE, Categories.autoCategory("com.phonepe.app", -1))
    }

    @Test fun androidCategoryThenKeywordsThenOther() {
        assertEquals(Categories.GAMES, Categories.autoCategory("com.example.puzzle", 0))
        assertEquals(Categories.WORK, Categories.autoCategory("com.example.x", 7))
        assertEquals(Categories.FINANCE, Categories.autoCategory("com.hdfcbank.app", -1))
        assertEquals(Categories.COMMUNICATION, Categories.autoCategory("com.example.x", -1, "Proton Mail"))
        assertEquals(Categories.OTHER, Categories.autoCategory("com.example.x", -1, "Zebra"))
    }

    @Test fun essentialAlwaysEssentials() {
        assertEquals(Categories.ESSENTIALS, Categories.autoCategory("com.instagram.android", 4, essential = true))
    }

    @Test fun slugs() {
        assertEquals("custom_side_projects", Categories.slug(" Side projects! "))
    }
}

class HomeListTest {
    @Test fun addReplaceMove() {
        assertEquals(listOf("a", "b", "c"), HomeList.add(listOf("a", "b"), "c", 3))
        assertEquals(listOf("a", "b"), HomeList.add(listOf("a", "b"), "a", 3))
        assertEquals(listOf("a", "b"), HomeList.add(listOf("a", "b"), "c", 2))
        assertEquals(listOf("a", "x"), HomeList.replace(listOf("a", "b"), "b", "x"))
        assertEquals(listOf("a"), HomeList.replace(listOf("a", "b"), "b", "a"))
        assertEquals(listOf("b", "a", "c"), HomeList.move(listOf("a", "b", "c"), "b", -1))
        assertEquals(listOf("a", "b", "c"), HomeList.move(listOf("a", "b", "c"), "c", 1))
        assertEquals(listOf("a", "b"), HomeList.decode(" a, ,b,a"))
    }
}

class ProfileSwitchPlannerTest {
    private val work = ProfileLink(7, "1|1000")

    @Test fun scheduleActivatesAndRestores() {
        val start = ProfileSwitchPlanner.decide(null, work, AutoSwitchState())
        assertEquals(7L, start.activeProfileId)
        assertNull(start.state.previousProfileId)
        // Same span again: nothing changes.
        assertEquals(start, ProfileSwitchPlanner.decide(7, work, start.state))
        val end = ProfileSwitchPlanner.decide(7, null, start.state)
        assertNull(end.activeProfileId)
        assertEquals(AutoSwitchState(), end.state)
    }

    @Test fun manualSwitchWinsUntilNextTransition() {
        val start = ProfileSwitchPlanner.decide(3, work, AutoSwitchState())
        val manual = ProfileSwitchPlanner.manual(start.state)
        // User picked profile 5 during the window: not switched back while the window lasts.
        assertEquals(5L, ProfileSwitchPlanner.decide(5, work, manual).activeProfileId)
        // When the window ends the user's choice stays.
        val end = ProfileSwitchPlanner.decide(5, null, manual)
        assertEquals(5L, end.activeProfileId)
        // The next occurrence switches again.
        assertEquals(7L, ProfileSwitchPlanner.decide(5, ProfileLink(7, "1|2000"), end.state).activeProfileId)
    }

    @Test fun userChangeDuringWindowIsKeptAtEnd() {
        val start = ProfileSwitchPlanner.decide(3, work, AutoSwitchState())
        // The profile changed without going through manual() (e.g. deleted): end keeps the current one.
        assertEquals(9L, ProfileSwitchPlanner.decide(9, null, start.state).activeProfileId)
    }

    @Test fun backToBackWindowsRestoreTheOriginal() {
        val first = ProfileSwitchPlanner.decide(3, work, AutoSwitchState())
        val second = ProfileSwitchPlanner.decide(7, ProfileLink(8, "2|5000"), first.state)
        assertEquals(8L, second.activeProfileId)
        assertEquals(3L, second.state.previousProfileId)
    }
}

class CategoryRuleTest {
    @Test fun categoryRuleSumsAppsInThatCategory() {
        val rule = `in`.tuwi.yogaphone.usage.UsageRule("category:social", `in`.tuwi.yogaphone.usage.RuleScope.CATEGORY, "social", 30)
        val result = `in`.tuwi.yogaphone.usage.RuleEvaluator().evaluate(
            listOf(rule), eligibleMinutes = emptyMap(),
            appMinutes = mapOf("insta" to 20L, "x" to 15L, "docs" to 40L),
            categoryOf = mapOf("insta" to "social", "x" to "social", "docs" to "work"),
        ).rules.single()
        assertEquals(35L, result.used)
        assertEquals(`in`.tuwi.yogaphone.usage.RuleState.OVER, result.state)
        assertEquals(5L, result.excessMinutes)
    }
}
