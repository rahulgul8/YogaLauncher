package `in`.tuwi.yogaphone.privacy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupCodecTest {
    private val payload = BackupPayload(
        version = 1,
        createdAt = 1_700_000_000_000,
        includesHistory = false,
        prefs = listOf(
            PrefEntry("theme_mode_v2", "s", "DARK"),
            PrefEntry("show_clock", "b", true),
            PrefEntry("home_app_count", "i", 8),
            PrefEntry("profile_active", "l", 3L),
            PrefEntry("focus_allowed", "ss", setOf("a", "b")),
        ),
        tables = mapOf(
            "app_entries" to listOf(mapOf("packageName" to "com.a", "customLabel" to null, "isFavorite" to 1L, "favoriteOrder" to 0L)),
            "profiles" to emptyList(),
        ),
    )

    @Test fun roundTrip() {
        val decoded = BackupCodec.decode(BackupCodec.encode(payload))
        assertEquals(payload.prefs, decoded.prefs)
        assertEquals(payload.tables, decoded.tables)
        assertEquals(payload.createdAt, decoded.createdAt)
        assertEquals("5 settings, 1 app preferences", BackupCodec.summary(decoded))
    }

    @Test fun rejectsNewerVersionsAndOtherFiles() {
        val newer = BackupCodec.encode(payload.copy(version = BackupCodec.VERSION + 1))
        assertThrows(BackupException::class.java) { BackupCodec.decode(newer) }
        assertThrows(BackupException::class.java) { BackupCodec.decode("{\"hello\": 1}") }
        assertThrows(BackupException::class.java) { BackupCodec.decode("not json") }
    }
}
