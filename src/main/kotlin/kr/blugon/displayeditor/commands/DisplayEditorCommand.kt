package kr.blugon.displayeditor.commands

import io.papermc.paper.command.brigadier.argument.ArgumentTypes.entities
import kr.blugon.kotlinbrigadier.registerCommandHandler
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Display
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.plugin.java.JavaPlugin



fun JavaPlugin.registerCommand() {
    this.lifecycleManager.registerCommandHandler {
        register("displayentity", aliases = arrayOf("dp")) {
            require { sender.isOp }

            then("edit") {
                then("block") {
                    then("entities" to entities()) {
                        BlockDisplayEdit(this)
                    }
                }
                then("item") {
                    then("entities" to entities()) {
                        ItemDisplayEdit(this)
                    }
                }
                then("text") {
                    then("entities" to entities()) {
                        TextDisplayEdit(this)
                    }
                }
            }

            then("spawn") {
                then("block") {
                    BlockDisplaySpawn(this)
                }
                then("item") {
                    ItemDisplaySpawn(this)
                }
                then("text") {
                    TextDisplaySpawn(this)
                }
            }
        }
    }
}

fun Collection<Entity>.isDisplayList(sender: CommandSender, type: EntityType? = null, typeName: String = "표시"): Boolean {
    this.forEach { entity ->
        if(entity.type != type) {
            sender.sendMessage(text("개체가 ${typeName}가 아닙니다").color(NamedTextColor.RED))
            return false
        }
    }
    return true
}
fun Collection<Entity>.isBlockDisplayList(sender: CommandSender): Boolean {
    return this.isDisplayList(sender, EntityType.BLOCK_DISPLAY, "블럭 표시")
}
fun Collection<Entity>.isItemDisplayList(sender: CommandSender): Boolean {
    return this.isDisplayList(sender, EntityType.ITEM_DISPLAY, "아이템 표시")
}
fun Collection<Entity>.isTextDisplayList(sender: CommandSender): Boolean {
    return this.isDisplayList(sender, EntityType.TEXT_DISPLAY, "텍스트 표시")
}