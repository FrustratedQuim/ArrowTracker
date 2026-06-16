package com.ratger.arrowtracker.command

import com.ratger.arrowtracker.tracker.TrackerService
import com.ratger.arrowtracker.tracker.model.ActionResult
import com.ratger.arrowtracker.tracker.model.Target
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.Locale
import kotlin.math.round

internal class TrackerCommand(
    private val trackerService: TrackerService
) : CommandExecutor, TabCompleter {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        val player = sender as? Player ?: run {
            sender.sendMessage(Component.text("Only players can use /tracker.", NamedTextColor.RED))
            return true
        }

        if (args.isEmpty()) {
            player.sendMessage(usageMessage(label))
            return true
        }

        if (args.size == 1 && args[0].equals("clear", ignoreCase = true)) {
            sendResult(player, trackerService.clearTracking(player))
            return true
        }

        if (args.size != 2) {
            player.sendMessage(usageMessage(label))
            return true
        }

        val x = parseFiniteCoordinate(args[0])
        val z = parseFiniteCoordinate(args[1])
        if (x == null || z == null) {
            player.sendMessage(usageMessage(label))
            return true
        }

        val target = Target(x, z)
        val displayCoordinates = formatDisplayCoordinates(args[0], args[1])
        sendResult(player, trackerService.toggleTrackingResult(player, target), displayCoordinates)
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        val player = sender as? Player ?: return emptyList()
        if (args.isEmpty() || args.size > 2) {
            return emptyList()
        }
        if (args[0].equals("clear", ignoreCase = true) && args.size > 1) {
            return emptyList()
        }

        val currentInput = args.last()
        val suggestions = when (args.size) {
            1 -> firstArgumentSuggestions(player)
            2 -> listOf(formatCoordinate(player.location.z))
            else -> emptyList()
        }

        return suggestions.filter { suggestion ->
            suggestion.lowercase(Locale.ROOT).startsWith(currentInput.lowercase(Locale.ROOT))
        }
    }

    private fun sendResult(player: Player, result: ActionResult, displayCoordinates: String? = null) {
        when (result) {
            ActionResult.STARTED -> {
                if (displayCoordinates != null) {
                    player.sendMessage(startedMessage(displayCoordinates))
                }
            }

            ActionResult.CLEARED -> player.sendMessage(clearedMessage())
            ActionResult.MISSING -> player.sendMessage(missingMessage())
        }
    }

    private fun usageMessage(label: String): Component {
        return Component.text("Usage: /$label <x> <z>", NamedTextColor.RED)
    }

    private fun startedMessage(displayCoordinates: String): Component {
        return Component.text("Tracker enabled ", NamedTextColor.GREEN)
            .append(Component.text("($displayCoordinates)", NamedTextColor.GRAY))
    }

    private fun clearedMessage(): Component {
        return Component.text("Tracker disabled", NamedTextColor.YELLOW)
    }

    private fun missingMessage(): Component {
        return Component.text("There is no active tracker right now", NamedTextColor.YELLOW)
    }

    private fun firstArgumentSuggestions(player: Player): List<String> {
        val x = formatCoordinate(player.location.x)
        val z = formatCoordinate(player.location.z)
        return listOf(
            x,
            "$x $z",
            "clear"
        )
    }

    private fun formatDisplayCoordinates(x: String, z: String): String {
        return listOf(x, z)
            .joinToString(" ") { coordinate -> formatDisplayCoordinate(coordinate) }
    }

    private fun formatDisplayCoordinate(input: String): String {
        return if (input.contains('.') || input.contains('e', ignoreCase = true)) {
            input
        } else {
            "$input.0"
        }
    }

    private fun formatCoordinate(value: Double): String {
        val rounded = round(value * 2.0) / 2.0
        return String.format(Locale.ROOT, "%.1f", rounded)
    }

    private fun parseFiniteCoordinate(input: String): Double? {
        return input.toDoubleOrNull()
            ?.takeIf { it.isFinite() }
    }
}
