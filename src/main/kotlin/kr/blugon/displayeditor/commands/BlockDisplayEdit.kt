package kr.blugon.displayeditor.commands

import com.mojang.brigadier.arguments.BoolArgumentType.bool
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.math.BlockVector3
import io.papermc.paper.command.brigadier.PaperCommandSourceStack
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.ArgumentTypes.blockState
import io.papermc.paper.command.brigadier.argument.ArgumentTypes.finePosition
import io.papermc.paper.command.brigadier.argument.resolvers.FinePositionResolver
import kr.blugon.displayeditor.*
import kr.blugon.kotlinbrigadier.*
import kr.blugon.minicolor.MiniColor.Companion.miniMessage
import kr.blugon.pluginutils.component.translate
import net.kyori.adventure.text.Component.text
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.AngleArgument.SingleAngle
import net.minecraft.commands.arguments.AngleArgument.angle
import net.minecraft.commands.arguments.coordinates.Coordinates
import net.minecraft.commands.arguments.coordinates.RotationArgument.rotation
import net.minecraft.core.SectionPos.*
import net.minecraft.world.phys.Vec2
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockState
import org.bukkit.block.data.BlockData
import org.bukkit.command.CommandSender
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.util.Transformation
import org.joml.Vector3f


class BlockDisplayEdit(node: BrigadierNode) {

    init {
        node.then("state" to blockState()) {
            executes {
                val entities = it.getEntities<Entity>("entities")
                if(!entities.isBlockDisplayList(sender)) return@executes
                val state: BlockState by it

                entities.forEach { display: Entity ->
                    (display as BlockDisplay).block = state.blockData
                }
                sender.sendMessage(text("블럭 표시 ${entities.size}개의 블록을 [").append(state.type.translate()).append("](으)로 바꿨습니다"))
            }
        }
    }
}

class BlockDisplaySpawn(node: BrigadierNode) {
    private fun spawn(sender: CommandSender, state: BlockState, location: Location, rotation: Vec2 = Vec2(0f, 0f), isCenter: Boolean = false) {
        val world = location.world
        world.spawn(location, BlockDisplay::class.java).also {
            it.block = state.blockData
            if(isCenter) {
                it.transformation = Transformation(
                    Vector3f(-0.5f, -0.5f, -0.5f),
                    it.transformation.leftRotation,
                    it.transformation.scale,
                    it.transformation.rightRotation,
                )
            }
            it.setRotation(rotation)
        }
        sender.sendMessage("새로운 블럭 표시를 소환했습니다".miniMessage)
    }

    init {
        node.then("state" to blockState()) {
            then("position" to finePosition()) {
                then("rotation" to rotation()) {
                    then("-center") {
                        executes {
                            val state: BlockState by it
                            val position: FinePositionResolver by it
                            val resolvedPosition = position.resolve(this)
                            val location = Location(it.source.world, resolvedPosition.x(), resolvedPosition.y(), resolvedPosition.z())
                            val rotation = it.get<Coordinates>("rotation").getRotation((this as PaperCommandSourceStack) as CommandSourceStack)
                            spawn(sender, state, location, rotation, true)
                        }
                    }

                    executes {
                        val state: BlockState by it
                        val position: FinePositionResolver by it
                        val resolvedPosition = position.resolve(this)
                        val location = Location(it.source.world, resolvedPosition.x(), resolvedPosition.y(), resolvedPosition.z())
                        val rotation = it.get<Coordinates>("rotation").getRotation((this as PaperCommandSourceStack) as CommandSourceStack)
                        spawn(sender, state, location, rotation)
                    }
                }

                executes {
                    val state: BlockState by it
                    val position: FinePositionResolver by it
                    val resolvedPosition = position.resolve(this)
                    val location = Location(it.source.world, resolvedPosition.x(), resolvedPosition.y(), resolvedPosition.z())
                    spawn(sender, state, location)
                }
            }

            executes {
                val state: BlockState by it
                spawn(sender, state, it.source.location)
            }
        }
    }
}