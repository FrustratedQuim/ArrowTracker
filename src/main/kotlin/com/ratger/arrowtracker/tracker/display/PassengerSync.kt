package com.ratger.arrowtracker.tracker.display

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerCommon
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers
import org.bukkit.entity.Player
import java.util.UUID

internal class PassengerSync {
    private val desiredPassengers = HashMap<UUID, Int>()
    private val cachedPassengers = HashMap<UUID, IntArray>()
    private val stateLock = Any()
    private var listener: PacketListenerCommon? = null

    fun start() {
        if (listener != null) {
            return
        }

        val packetListener = object : PacketListenerAbstract(PacketListenerPriority.HIGHEST) {
            override fun onPacketSend(event: PacketSendEvent) {
                when (event.packetType) {
                    PacketType.Play.Server.SET_PASSENGERS -> handleSetPassengers(event)
                    PacketType.Play.Server.DESTROY_ENTITIES -> handleDestroyEntities(event)
                    PacketType.Play.Server.JOIN_GAME,
                    PacketType.Play.Server.RESPAWN -> handleStateReset(event)
                }
            }
        }

        PacketEvents.getAPI().eventManager.registerListener(packetListener)
        listener = packetListener
    }

    fun shutdown() {
        listener?.let { packetListener ->
            PacketEvents.getAPI()?.eventManager?.unregisterListener(packetListener)
        }
        listener = null
        synchronized(stateLock) {
            desiredPassengers.clear()
            cachedPassengers.clear()
        }
    }

    fun attach(player: Player, passengerId: Int): IntArray {
        val playerId = player.uniqueId
        val livePassengers = livePassengers(player)
        return synchronized(stateLock) {
            val previousPassengerId = desiredPassengers.put(playerId, passengerId)
            val basePassengers = if (previousPassengerId != null && previousPassengerId != passengerId) {
                removePassenger(currentPassengers(playerId, livePassengers), previousPassengerId)
            } else {
                currentPassengers(playerId, livePassengers)
            }
            cachePassengers(playerId, ensurePassenger(basePassengers, passengerId))
        }
    }

    fun detach(player: Player, passengerId: Int): IntArray {
        val playerId = player.uniqueId
        val livePassengers = livePassengers(player)
        return synchronized(stateLock) {
            desiredPassengers.remove(playerId, passengerId)
            cachePassengers(playerId, removePassenger(currentPassengers(playerId, livePassengers), passengerId))
        }
    }

    fun forget(playerId: UUID) {
        synchronized(stateLock) {
            desiredPassengers.remove(playerId)
            cachedPassengers.remove(playerId)
        }
    }

    fun passengerSignature(player: Player): Int {
        val livePassengers = livePassengers(player)
        return synchronized(stateLock) {
            currentPassengers(player.uniqueId, livePassengers)
                .sortedArray()
                .fold(1) { hash, entityId -> 31 * hash + entityId }
        }
    }

    private fun handleSetPassengers(event: PacketSendEvent) {
        val player = event.getPlayer<Player>()
        val wrapper = WrapperPlayServerSetPassengers(event)
        if (wrapper.entityId != player.entityId) {
            return
        }

        val mergedPassengers = synchronized(stateLock) {
            val merged = mergePassengers(player.uniqueId, wrapper.passengers)
            cachePassengers(player.uniqueId, merged)
        }
        if (!wrapper.passengers.contentEquals(mergedPassengers)) {
            wrapper.passengers = mergedPassengers
            event.markForReEncode(true)
        }
    }

    private fun handleDestroyEntities(event: PacketSendEvent) {
        val player = event.getPlayer<Player>()
        val currentPassengers = synchronized(stateLock) {
            cachedPassengers[player.uniqueId]?.copyOf()
        } ?: return
        val destroyedIds = WrapperPlayServerDestroyEntities(event).entityIds.toHashSet()
        val remainingPassengers = currentPassengers.filterNot { it in destroyedIds }.toIntArray()
        if (!currentPassengers.contentEquals(remainingPassengers)) {
            synchronized(stateLock) {
                cachedPassengers[player.uniqueId] = remainingPassengers
            }
        }
    }

    private fun handleStateReset(event: PacketSendEvent) {
        val player = event.getPlayer<Player>()
        synchronized(stateLock) {
            cachedPassengers.remove(player.uniqueId)
        }
    }

    private fun mergePassengers(playerId: UUID, passengers: IntArray): IntArray {
        val desiredPassengerId = desiredPassengers[playerId] ?: return normalizePassengers(passengers)
        return ensurePassenger(passengers, desiredPassengerId)
    }

    private fun currentPassengers(playerId: UUID, livePassengers: IntArray): IntArray {
        return cachedPassengers[playerId] ?: livePassengers
    }

    private fun ensurePassenger(passengers: IntArray, passengerId: Int): IntArray {
        val normalizedPassengers = normalizePassengers(passengers)
        if (normalizedPassengers.contains(passengerId)) {
            return normalizedPassengers
        }
        return normalizedPassengers + passengerId
    }

    private fun removePassenger(passengers: IntArray, passengerId: Int): IntArray {
        return normalizePassengers(passengers)
            .filterNot { it == passengerId }
            .toIntArray()
    }

    private fun normalizePassengers(passengers: IntArray): IntArray {
        if (passengers.isEmpty()) {
            return passengers
        }

        val uniquePassengers = LinkedHashSet<Int>(passengers.size)
        passengers.forEach(uniquePassengers::add)
        return uniquePassengers.toIntArray()
    }

    private fun cachePassengers(playerId: UUID, passengers: IntArray): IntArray {
        val snapshot = passengers.copyOf()
        cachedPassengers[playerId] = snapshot
        return snapshot
    }

    private fun livePassengers(player: Player): IntArray {
        return player.passengers.map { it.entityId }.toIntArray()
    }
}
