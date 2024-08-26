package kr.blugon.displayeditor

import com.mojang.brigadier.context.CommandContext
import com.sk89q.worldedit.bukkit.WorldEditPlugin
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.argument.resolvers.selector.EntitySelectorArgumentResolver
import kr.blugon.displayeditor.commands.registerCommand
import kr.blugon.displayeditor.commands.worldedit.registerWorldEditExtensionCommand
import kr.blugon.kotlinbrigadier.get
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.minecraft.world.phys.Vec2
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.command.BlockCommandSender
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import kotlin.reflect.KProperty

fun Component.append(text: String): Component = this.append(text(text))

inline fun <reified T> CommandContext<CommandSourceStack>.getEntities(name: String): List<T> {
    return ArrayList<T>().apply {
        this@getEntities.get<EntitySelectorArgumentResolver>(name).resolve(this@getEntities.source).forEach {
            this.add(it as T)
        }
    }
}

fun Entity.setRotation(rotation: Vec2) = this.setRotation(rotation.y, rotation.x)

var worldedit: WorldEditPlugin? = null
class DisplayEditor : JavaPlugin(), Listener {

    override fun onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this)
        logger.info("Plugin enabled")
        val worldeditPlugin = server.pluginManager.getPlugin("WorldEdit")
        if(worldeditPlugin != null) {
            worldedit = worldeditPlugin as WorldEditPlugin
            this.registerWorldEditExtensionCommand()
        }
        this.registerCommand()
    }

    override fun onDisable() {
        logger.info("Plugin disabled")
    }
}