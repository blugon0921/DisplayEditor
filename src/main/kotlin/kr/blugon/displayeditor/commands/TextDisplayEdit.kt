package kr.blugon.displayeditor.commands

import com.mojang.brigadier.arguments.IntegerArgumentType.integer
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.PaperCommandSourceStack
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.ArgumentTypes.component
import io.papermc.paper.command.brigadier.argument.ArgumentTypes.finePosition
import io.papermc.paper.command.brigadier.argument.resolvers.FinePositionResolver
import io.papermc.paper.math.FinePosition
import kr.blugon.displayeditor.getEntities
import kr.blugon.displayeditor.setRotation
import kr.blugon.kotlinbrigadier.BrigadierNode
import kr.blugon.kotlinbrigadier.get
import kr.blugon.kotlinbrigadier.getValue
import kr.blugon.kotlinbrigadier.world
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.minecraft.ChatFormatting
import net.minecraft.commands.arguments.ColorArgument.color
import net.minecraft.commands.arguments.coordinates.Coordinates
import net.minecraft.commands.arguments.coordinates.RotationArgument.rotation
import net.minecraft.network.chat.TextColor
import net.minecraft.world.phys.Vec2
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.TextDisplay

class TextDisplayEdit(node: BrigadierNode) {

    init {
        node.then("text" to component()) {
            then("background" to integer()) {
                executes {
                    val entities = it.getEntities<Entity>("entities")
                    if(!entities.isTextDisplayList(sender)) return@executes
                    val text: Component by it
                    val background: Int by it
                    val backgroundColor = Color.fromARGB(background)
                    entities.forEach { display ->
                        (display as TextDisplay).text(text)
                        display.backgroundColor = backgroundColor
                    }
                    sender.sendMessage(text("문자 표시 ${entities.size}개의 텍스트를 [").append(text).append(text("](으)로, 배경을 ").append(text("$background").color(
                        net.kyori.adventure.text.format.TextColor.color(backgroundColor.red, backgroundColor.green, backgroundColor.blue))).append(text("(으)로 바꿨습니다"))))
                }
            }

            executes {
                val entities = it.getEntities<Entity>("entities")
                if(!entities.isTextDisplayList(sender)) return@executes
                val text: Component by it
                entities.forEach { display ->
                    (display as TextDisplay).text(text)
                }
                sender.sendMessage(text("문자 표시 ${entities.size}개의 텍스트를 [").append(text).append(text("](으)로 바꿨습니다")))
            }
        }
    }
}

class TextDisplaySpawn(node: BrigadierNode) {
    private fun BrigadierNode.runWithPosition(run: CommandSourceStack.(CommandContext<CommandSourceStack>, FinePosition?, Vec2?) -> TextDisplay) {
        this.executes {
            run(this, it, null, null)
        }
        this.then("location" to finePosition()) {
            this.then("rotation" to rotation()) {
                then("background" to integer()) {
                    executes {
                        val positionResolver: FinePositionResolver = it["location"]
                        val position = positionResolver.resolve(this)
                        val rotation = it.get<Coordinates>("rotation").getRotation((this as PaperCommandSourceStack) as net.minecraft.commands.CommandSourceStack)
                        val background: Int by it
                        run(this, it, position, rotation).also {
                            it.backgroundColor = Color.fromARGB(background)
                        }
                    }
                }
                executes {
                    val positionResolver: FinePositionResolver = it["location"]
                    val position = positionResolver.resolve(this)
                    val rotation = it.get<Coordinates>("rotation").getRotation((this as PaperCommandSourceStack) as net.minecraft.commands.CommandSourceStack)
                    run(this, it, position, rotation)
                }
            }
            executes {
                val positionResolver: FinePositionResolver = it["location"]
                val position = positionResolver.resolve(this)
                run(this, it, position, null)
            }
        }
    }

    init {
        node.then("text" to component()) {
            runWithPosition { it, position, rotation ->
                val text: Component by it
                val location = if(position != null) Location(it.source.world, position.x(), position.y(), position.z())
                else it.source.location
                sender.sendMessage(text("새로운 문자 표시를 소환했습니다"))
                world.spawn(location, TextDisplay::class.java).also {
                    it.text(text)
                    it.setRotation(rotation ?: Vec2(0f, 0f))
                }
            }
        }
    }
}