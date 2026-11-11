package `in`.tuwi.yogaphone.focus

import `in`.tuwi.yogaphone.pro.*
import android.content.Context
import `in`.tuwi.yogaphone.data.db.FocusScheduleEntity
import `in`.tuwi.yogaphone.data.db.FocusSessionEntity
import `in`.tuwi.yogaphone.data.db.FocusStatus
import `in`.tuwi.yogaphone.data.db.InterventionEventEntity
import `in`.tuwi.yogaphone.data.db.TimeboxSessionEntity
import `in`.tuwi.yogaphone.data.db.TimeboxStatus
import `in`.tuwi.yogaphone.data.db.YogaDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

fun FocusScheduleEntity.toWindow() = FocusWindow(
    id = id,
    name = name,
    days = days.split(',').mapNotNull { it.trim().toIntOrNull()?.takeIf { d -> d in 1..7 }?.let(DayOfWeek::of) }.toSet(),
    start = LocalTime.of(startMinuteOfDay / 60, startMinuteOfDay % 60),
    end = LocalTime.of(endMinuteOfDay / 60, endMinuteOfDay % 60),
    enabled = enabled,
)

val TimeboxSessionEntity.clock get() = TimeboxClock(endAt, pausedRemainingMs)
val TimeboxSessionEntity.isLive get() = status == TimeboxStatus.RUNNING || status == TimeboxStatus.PAUSED
val TimeboxSessionEntity.label get() = purpose?.takeIf(String::isNotBlank) ?: "Timebox"

class FocusRepository(
    private val context: Context,
    private val database: YogaDatabase,
    val prefs: FocusPrefs,
) {
    private val dao = database.focusDao()
    private val system = FocusSystem(context)
    private val mutex = Mutex()

    val latestTimebox = dao.observeLatestTimebox()
    val activeFocus = dao.observeActiveFocus()
    val schedules = dao.observeSchedules()

    private suspend fun pro() = (context.applicationContext as `in`.tuwi.yogaphone.YogaApplication).proRepository.allows(ProFeature.FOCUS)

    private fun now() = System.currentTimeMillis()
    private suspend fun <T> locked(block: suspend () -> T): T = withContext(Dispatchers.IO) { mutex.withLock { block() } }

    // ---- Timebox ----

    suspend fun startTimebox(minutes: Int, purpose: String?, boundPackage: String?) = locked {
        if (!pro()) return@locked
        dao.latestTimebox()?.takeIf { it.isLive }?.let { dao.saveTimebox(it.copy(status = TimeboxStatus.CANCELLED, endedAt = now())) }
        val start = now()
        val clean = purpose?.trim()?.takeIf(String::isNotEmpty)
        val session = TimeboxSessionEntity(purpose = clean, boundPackage = boundPackage, plannedMinutes = minutes,
            startedAt = start, endAt = TimeboxClock.start(minutes, start).endAt)
        dao.insertTimebox(session)
        prefs.setLastTimebox(minutes, clean)
        system.cancelTimeboxNotifications()
        syncTimebox(dao.latestTimebox())
    }

    suspend fun pauseTimebox() = updateLive { it.copy(status = TimeboxStatus.PAUSED, pausedRemainingMs = it.clock.pause(now()).pausedRemainingMs) }

    suspend fun resumeTimebox() = updateLive { session ->
        val resumed = session.clock.resume(now())
        session.copy(status = TimeboxStatus.RUNNING, endAt = resumed.endAt, pausedRemainingMs = null)
    }

    suspend fun stopTimebox() = locked {
        val session = dao.latestTimebox()?.takeIf { it.isLive } ?: return@locked
        dao.saveTimebox(session.copy(status = TimeboxStatus.CANCELLED, endedAt = now(), acknowledged = true))
        system.cancelAlarm(FocusSystem.ACTION_TIMEBOX_END)
        system.cancelTimeboxNotifications()
    }

    /** Works on a running, paused or just-finished timebox. */
    suspend fun extendTimebox(minutes: Int) = locked {
        if (!pro()) return@locked
        val session = dao.latestTimebox() ?: return@locked
        if (session.status == TimeboxStatus.CANCELLED) return@locked
        val clock = session.clock.extend(minutes, now())
        val updated = session.copy(
            endAt = clock.endAt, pausedRemainingMs = clock.pausedRemainingMs, endedAt = null, acknowledged = false,
            status = if (clock.isPaused) TimeboxStatus.PAUSED else TimeboxStatus.RUNNING,
            extendedMinutes = session.extendedMinutes + minutes,
        )
        dao.saveTimebox(updated)
        system.cancelTimeboxNotifications()
        syncTimebox(updated)
    }

    suspend fun acknowledgeTimebox() = locked {
        dao.latestTimebox()?.let { if (!it.isLive) dao.saveTimebox(it.copy(acknowledged = true)) }
        system.cancelTimeboxNotifications()
    }

    private suspend fun updateLive(transform: (TimeboxSessionEntity) -> TimeboxSessionEntity) = locked {
        if (!pro()) return@locked
        val session = dao.latestTimebox()?.takeIf { it.isLive } ?: return@locked
        val updated = transform(session)
        dao.saveTimebox(updated)
        syncTimebox(updated)
    }

    /** Brings notification and alarm in line with the stored session; completes it when its time is up. */
    private suspend fun syncTimebox(session: TimeboxSessionEntity?) {
        if (session == null || !session.isLive) {
            system.cancelAlarm(FocusSystem.ACTION_TIMEBOX_END)
            return
        }
        val now = now()
        if (session.status == TimeboxStatus.RUNNING && session.endAt <= now) {
            dao.saveTimebox(session.copy(status = TimeboxStatus.COMPLETED, endedAt = session.endAt))
            (context.applicationContext as `in`.tuwi.yogaphone.YogaApplication).reviewController.recordCompletedSession()
            system.cancelAlarm(FocusSystem.ACTION_TIMEBOX_END)
            system.showTimeboxDone(session.label, session.plannedMinutes + session.extendedMinutes)
            return
        }
        if (session.status == TimeboxStatus.PAUSED) system.cancelAlarm(FocusSystem.ACTION_TIMEBOX_END)
        else system.setAlarm(FocusSystem.ACTION_TIMEBOX_END, session.endAt, exactIfAllowed = true)
        system.showTimeboxRunning(session.label, session.endAt, session.pausedRemainingMs)
    }

    // ---- Focus ----

    /** [minutes] null means "until I stop". */
    suspend fun startFocus(minutes: Int?) = locked {
        if (!pro()) return@locked
        prefs.setLastFocus(minutes ?: 0)
        val start = now()
        val active = dao.activeFocus()
        if (active != null) {
            dao.saveFocus(active.copy(plannedEndAt = minutes?.let { start + it * 60_000L }))
        } else {
            dao.insertFocus(FocusSessionEntity(startAt = start, plannedEndAt = minutes?.let { start + it * 60_000L }))
            applyFocusSideEffects()
        }
        scheduleNext()
    }

    suspend fun endFocus() = locked {
        val active = dao.activeFocus() ?: return@locked
        if (active.scheduleId != null) {
            ScheduleEvaluator.activeAt(dao.schedules().filter { it.startsFocus }.map { it.toWindow() }, Instant.now(), ZoneId.systemDefault())
                ?.takeIf { it.window.id == active.scheduleId }
                ?.let { prefs.setSuppressedSpan("${it.window.id}|${it.start.toEpochMilli()}") }
        }
        finishFocus(active, early = true)
        scheduleNext()
    }

    suspend fun recordFocusOverride() = locked {
        dao.activeFocus()?.let { dao.saveFocus(it.copy(overrideCount = it.overrideCount + 1)) }
    }

    private suspend fun finishFocus(session: FocusSessionEntity, early: Boolean) {
        val end = if (early) now() else session.plannedEndAt ?: now()
        dao.saveFocus(session.copy(endedAt = end, status = if (early) FocusStatus.ENDED_EARLY else FocusStatus.COMPLETED))
        restoreFocusSideEffects()
        // Held notifications are summarised as soon as focus ends.
        runCatching { (context.applicationContext as `in`.tuwi.yogaphone.YogaApplication).quietRepository.deliverDigest() }
        if (!early) {
            (context.applicationContext as `in`.tuwi.yogaphone.YogaApplication).reviewController.recordCompletedSession()
            val minutes = ((end - session.startAt) / 60_000).coerceAtLeast(1)
            val streak = streak()
            system.showFocusDone("$minutes minutes of focus done." + if (streak > 1) " $streak-day streak." else "")
        }
    }

    private suspend fun applyFocusSideEffects() {
        if (!prefs.preferences.first().focusDnd || prefs.dndState().first) return
        val previous = system.enableDnd() ?: return
        prefs.setDndState(true, previous)
    }

    private suspend fun restoreFocusSideEffects() {
        val (changed, previous) = prefs.dndState()
        if (changed && previous != null) system.restoreDnd(previous)
        prefs.setDndState(false, null)
    }

    suspend fun saveSchedule(schedule: FocusScheduleEntity) { dao.saveSchedule(schedule); reconcile() }
    suspend fun deleteSchedule(id: Long) { dao.deleteSchedule(id); reconcile() }

    /**
     * Called on launcher resume, alarms, boot, and clock or timezone changes: completes what has ended,
     * starts or ends scheduled focus, and sets the next alarms.
     */
    suspend fun reconcile() = locked {
        if (!pro()) {
            system.cancelAlarm(FocusSystem.ACTION_TIMEBOX_END)
            system.cancelAlarm(FocusSystem.ACTION_FOCUS_END)
            system.cancelAlarm(FocusSystem.ACTION_SCHEDULE)
            system.cancelTimeboxNotifications()
            restoreFocusSideEffects()
            return@locked
        }
        syncTimebox(dao.latestTimebox())
        val now = now()
        val zone = ZoneId.systemDefault()
        val windows = dao.schedules().filter { it.startsFocus }.map { it.toWindow() }
        val span = ScheduleEvaluator.activeAt(windows, Instant.ofEpochMilli(now), zone)
        var active = dao.activeFocus()
        if (active != null) {
            val plannedEnd = active.plannedEndAt
            val scheduleOver = active.scheduleId != null && span?.window?.id != active.scheduleId
            if ((plannedEnd != null && plannedEnd <= now) || scheduleOver) {
                finishFocus(active, early = false)
                active = null
            }
        }
        if (active == null && span != null) {
            val key = "${span.window.id}|${span.start.toEpochMilli()}"
            if (prefs.suppressedSpan() != key) {
                dao.insertFocus(FocusSessionEntity(startAt = now, plannedEndAt = span.end.toEpochMilli(),
                    mode = "SCHEDULED", scheduleId = span.window.id))
                applyFocusSideEffects()
            }
        }
        if (dao.activeFocus() != null) applyFocusSideEffects()
        scheduleNext()
    }

    private suspend fun scheduleNext() {
        val zone = ZoneId.systemDefault()
        val now = Instant.now()
        val windows = dao.schedules().map { it.toWindow() }
        ScheduleEvaluator.nextTransition(windows, now, zone)?.let {
            system.setAlarm(FocusSystem.ACTION_SCHEDULE, it.toEpochMilli(), exactIfAllowed = false)
        } ?: system.cancelAlarm(FocusSystem.ACTION_SCHEDULE)
        val plannedEnd = dao.activeFocus()?.plannedEndAt
        if (plannedEnd != null) system.setAlarm(FocusSystem.ACTION_FOCUS_END, plannedEnd, exactIfAllowed = true)
        else system.cancelAlarm(FocusSystem.ACTION_FOCUS_END)
    }

    suspend fun streak(): Int = withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val days = (dao.completedFocus().mapNotNull { it.endedAt } + dao.completedTimeboxes().mapNotNull { it.endedAt })
            .map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }.toSet()
        StreakCalculator.streak(days, java.time.LocalDate.now(zone))
    }

    // ---- Interventions ----

    suspend fun recordIntervention(type: String, packageName: String?, action: String, purpose: String?) =
        withContext(Dispatchers.IO) {
            dao.insertIntervention(InterventionEventEntity(type = type, packageName = packageName, shownAt = now(),
                actionTaken = action, purpose = purpose?.trim()?.takeIf(String::isNotEmpty)))
        }

    fun system() = system
}
