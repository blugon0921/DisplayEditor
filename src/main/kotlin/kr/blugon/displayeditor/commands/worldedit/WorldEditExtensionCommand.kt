package kr.blugon.displayeditor.commands.worldedit

import com.sk89q.worldedit.EmptyClipboardException
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.math.BlockVector3
import kr.blugon.displayeditor.relative
import kr.blugon.displayeditor.unRelative
import kr.blugon.displayeditor.worldedit
import kr.blugon.kotlinbrigadier.player
import kr.blugon.kotlinbrigadier.registerCommandHandler
import kr.blugon.minicolor.MiniColor
import kr.blugon.minicolor.MiniColor.Companion.miniMessage
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

fun JavaPlugin.registerWorldEditExtensionCommand() {
    this.lifecycleManager.registerCommandHandler {
        register("/pastedisplay", aliases = arrayOf("/paste-d")) {
            requires {listOf(
                sender is Player,
                sender.isOp
            )}

            fun pasteDisplay(player: Player, location: Location, removeBlock: Boolean = true) {
                val world = location.world
                val session = worldedit!!.getSession(player)
                val clipboard = try {
                    session.clipboard?.clipboard
                } catch (e: EmptyClipboardException) {
                    player.sendRichMessage("${MiniColor.RED}현재 클립보드가 비어있습니다. //copy를 먼저 사용하세요.")
                    return
                }
                if(clipboard == null) return
                player.sendMessage("${clipboard.region.minimumPoint} ${clipboard.region.maximumPoint}".miniMessage)
                player.sendMessage("${clipboard.origin.x()} ${clipboard.origin.y()} ${clipboard.origin.z()}".miniMessage)
                val relatively = (clipboard.minimumPoint to clipboard.maximumPoint).relative(clipboard.origin, player.location)
                val minimum = relatively.minimumPoint
                val maximum = relatively.maximumPoint

                var count = 0
                for (x in minimum.x()..maximum.x()) {
                    for (y in minimum.y()..maximum.y()) {
                        for (z in minimum.z()..maximum.z()) {
                            val state = clipboard.getBlock(BlockVector3(x, y, z).unRelative(clipboard.origin, location))
                            val block = world.getBlockAt(x, y, z)
                            when(BukkitAdapter.adapt(state.blockType)) {
                                Material.AIR,
                                Material.VOID_AIR,
                                Material.CAVE_AIR,
                                Material.WATER,
                                Material.LAVA -> {
                                    continue
                                }
                                else -> {
                                    if(removeBlock) block.type = Material.AIR
//                                    block.type = BukkitAdapter.adapt(state.blockType)
//                                    block.blockData = BukkitAdapter.adapt(state)
                                    world.spawn(block.location, BlockDisplay::class.java).also {
                                        it.block = BukkitAdapter.adapt(state)
                                    }
                                    count++
                                }
                            }
                        }
                    }
                }
                player.sendRichMessage("${MiniColor.LIGHT_PURPLE}클립보드를 (${location.blockX}, ${location.blockY}, ${location.blockZ}) 에 블럭 표시로 붙여졌습니다. ($count) ")
            }

            executes {
                pasteDisplay(player, location)
            }

            then("-not-remove") {
                executes {
                    pasteDisplay(player, location, false)
                }
            }
        }
    }
}