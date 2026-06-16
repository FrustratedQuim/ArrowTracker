package com.ratger.arrowtracker

import com.github.retrooper.packetevents.PacketEvents
import com.ratger.arrowtracker.api.ArrowTrackerApi
import com.ratger.arrowtracker.api.internal.TrackerApiProvider
import com.ratger.arrowtracker.command.TrackerCommand
import com.ratger.arrowtracker.tracker.PlayerStateListener
import com.ratger.arrowtracker.tracker.SessionStore
import com.ratger.arrowtracker.tracker.TrackerService
import com.ratger.arrowtracker.tracker.TrackerVisualRuntime
import com.ratger.arrowtracker.tracker.arrow.PoseResolver
import com.ratger.arrowtracker.tracker.arrow.VisualStateResolver
import com.ratger.arrowtracker.tracker.display.DisplayController
import com.ratger.arrowtracker.tracker.display.DisplayFactory
import com.ratger.arrowtracker.tracker.display.DisplayUpdater
import com.ratger.arrowtracker.tracker.display.MountController
import me.tofaa.entitylib.APIConfig
import me.tofaa.entitylib.EntityLib
import me.tofaa.entitylib.spigot.SpigotEntityLibPlatform
import org.bukkit.plugin.ServicePriority
import org.bukkit.plugin.java.JavaPlugin

class ArrowTrackerPlugin : JavaPlugin() {
    private lateinit var trackerService: TrackerService

    override fun onEnable() {
        val packetEventsApi = PacketEvents.getAPI() ?: run {
            logger.severe("PacketEvents API is not available. ArrowTracker cannot start.")
            server.pluginManager.disablePlugin(this)
            return
        }

        EntityLib.init(
            SpigotEntityLibPlatform(this),
            APIConfig(packetEventsApi).usePlatformLogger()
        )

        val sessionStore = SessionStore()
        val displayController = DisplayController(
            displayFactory = DisplayFactory(),
            mountController = MountController()
        )
        val visualRuntime = TrackerVisualRuntime(
            plugin = this,
            sessions = sessionStore,
            poseResolver = PoseResolver(),
            visualStateResolver = VisualStateResolver(),
            displayController = displayController,
            displayUpdater = DisplayUpdater()
        )
        trackerService = TrackerService(
            sessions = sessionStore,
            visualRuntime = visualRuntime
        )
        trackerService.start()
        server.servicesManager.register(
            ArrowTrackerApi::class.java,
            TrackerApiProvider(trackerService),
            this,
            ServicePriority.Normal
        )

        server.pluginManager.registerEvents(PlayerStateListener(trackerService), this)

        val trackerCommand = TrackerCommand(trackerService)
        getCommand("tracker")?.let { command ->
            command.setExecutor(trackerCommand)
            command.tabCompleter = trackerCommand
        } ?: logger.warning("Command /tracker is missing from plugin.yml")

        logger.info("ArrowTracker enabled")
    }

    override fun onDisable() {
        server.servicesManager.unregisterAll(this)
        if (::trackerService.isInitialized) {
            trackerService.shutdown()
        }
        logger.info("ArrowTracker disabled")
    }
}
