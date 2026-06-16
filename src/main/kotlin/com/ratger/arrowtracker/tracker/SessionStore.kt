package com.ratger.arrowtracker.tracker

import com.ratger.arrowtracker.tracker.model.Session
import com.ratger.arrowtracker.tracker.model.Target
import java.util.Collections
import java.util.LinkedHashMap
import java.util.UUID

internal class SessionStore {
    private val sessions = LinkedHashMap<UUID, Session>()

    operator fun get(playerId: UUID): Session? {
        return sessions[playerId]
    }

    operator fun set(playerId: UUID, session: Session) {
        sessions[playerId] = session
    }

    fun remove(playerId: UUID): Session? {
        return sessions.remove(playerId)
    }

    fun clear() {
        sessions.clear()
    }

    fun forEach(action: (UUID, Session) -> Unit) {
        sessions.forEach(action)
    }

    fun removeIf(predicate: (UUID, Session) -> Boolean) {
        val iterator = sessions.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (predicate(entry.key, entry.value)) {
                iterator.remove()
            }
        }
    }

    fun isTracking(playerId: UUID): Boolean {
        return sessions[playerId]?.clearing == false
    }

    fun trackedTarget(playerId: UUID): Target? {
        return sessions[playerId]
            ?.takeIf { !it.clearing }
            ?.target
    }

    fun activeTargets(): Map<UUID, Target> {
        val snapshot = LinkedHashMap<UUID, Target>()
        sessions.forEach { (playerId, session) ->
            if (!session.clearing) {
                snapshot[playerId] = session.target
            }
        }
        return Collections.unmodifiableMap(snapshot)
    }
}
