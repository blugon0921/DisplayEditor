package kr.blugon.displayeditor.commands

import com.mojang.brigadier.arguments.IntegerArgumentType.integer
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.PaperCommandSourceStack
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.ArgumentTypes.*
import io.papermc.paper.command.brigadier.argument.resolvers.BlockPositionResolver
import io.papermc.paper.command.brigadier.argument.resolvers.FinePositionResolver
import io.papermc.paper.command.brigadier.argument.resolvers.selector.EntitySelectorArgumentResolver
import io.papermc.paper.math.FinePosition
import kr.blugon.displayeditor.getEntities
import kr.blugon.displayeditor.setRotation
import kr.blugon.kotlinbrigadier.*
import kr.blugon.minicolor.MiniColor
import kr.blugon.minicolor.MiniColor.Companion.miniMessage
import net.kyori.adventure.text.Component.text
import net.minecraft.commands.arguments.SlotArgument
import net.minecraft.commands.arguments.SlotArgument.slot
import net.minecraft.commands.arguments.coordinates.Coordinates
import net.minecraft.commands.arguments.coordinates.RotationArgument.rotation
import net.minecraft.world.phys.Vec2
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Rotation
import org.bukkit.block.BlockState
import org.bukkit.command.CommandSender
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Entity
import org.bukkit.entity.Item
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Villager
import org.bukkit.entity.Zombie
import org.bukkit.inventory.BlockInventoryHolder
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Transformation
import org.joml.Vector3f


fun Inventory.getItemOrNull(slot: Int): ItemStack? {
    return if(this.size <= slot) this.getItem(slot)
    else null
}

fun Entity.getItem(slot: Int): ItemStack? {
    val equipmentSlot = when(slot) {
        98 -> EquipmentSlot.HAND
        99 -> EquipmentSlot.OFF_HAND
        100 -> EquipmentSlot.FEET
        101 -> EquipmentSlot.LEGS
        102 -> EquipmentSlot.CHEST
        103 -> EquipmentSlot.HEAD
        105 -> EquipmentSlot.BODY
        else -> null
    }

    return when(this) {
        is LivingEntity -> {
            if(equipmentSlot != null) this.equipment?.getItem(equipmentSlot)
            else null
        }
        is InventoryHolder -> return this.inventory.getItemOrNull(slot)
        is ItemFrame -> return if(slot == 0) this.item else null
        is ItemDisplay -> return if(slot == 0) this.itemStack else null
        is Item -> return if(slot == 0) this.itemStack else null
        else -> null
    }
}

fun editItem(sender: CommandSender, item: ItemStack, entities: List<Entity>) {
    entities.forEach { display ->
        (display as ItemDisplay).setItemStack(item)
    }
    sender.sendMessage(text("아이템 표시 ${entities.size}개의 아이템을 ").append(item.displayName()).append("(으)로 바꿨습니다".miniMessage))
}

class ItemDisplayEdit(node: BrigadierNode) {

    init {
        node.then("from") {
            then("entity") {
                then("target" to entity()) {
                    then("slot" to slot()) {
                        executes {
                            val entities = it.getEntities<Entity>("entities")
                            if(!entities.isItemDisplayList(sender)) return@executes
                            val slot: Int by it
                            val target: EntitySelectorArgumentResolver by it
                            val entity = target.resolve(this).getOrNull(0) ?: return@executes sender.sendRichMessage("${MiniColor.RED}개체를 찾을 수 없습니다")

                            val item = entity.getItem(slot)?: return@executes sender.sendRichMessage("${MiniColor.RED}원본에게 $slot 슬롯이 없습니다")
                            editItem(sender, item, entities)
                        }
                    }
                }
            }
            then("block") {
                then("position" to blockPosition()) {
                    then("slot" to slot()) {
                        executes {
                            val entities = it.getEntities<Entity>("entities")
                            if(!entities.isItemDisplayList(sender)) return@executes
                            val positionResolver: BlockPositionResolver = it["position"]
                            val position = positionResolver.resolve(this)
                            val slot: Int by it
                            val block = world.getBlockAt(position.blockX(), position.blockY(), position.blockZ())
                            if(block.state !is BlockInventoryHolder) {
                                sender.sendRichMessage("${MiniColor.RED}원본 위치 ${block.x}, ${block.y}, ${block.z}은(는) 용기가 아닙니다")
                                return@executes
                            }
                            val inventory = (block.state as BlockInventoryHolder).inventory
                            val item = inventory.getItemOrNull(slot)?: return@executes sender.sendRichMessage("${MiniColor.RED}원본에게 $slot 슬롯이 없습니다")
                            editItem(sender, item, entities)
                        }
                    }
                }
            }
            executes {
                val entities = it.getEntities<Entity>("entities")
                if(!entities.isItemDisplayList(sender)) return@executes
                val item = player.inventory.itemInMainHand
                entities.forEach { display ->
                    (display as ItemDisplay).setItemStack(item)
                }
                sender.sendMessage(text("아이템 표시 ${entities.size}개의 아이템을 ").append(item.displayName()).append("(으)로 바꿨습니다".miniMessage))
            }
        }
        node.then("with") {
            then("item" to itemStack()) {
                executes {
                    val entities = it.getEntities<Entity>("entities")
                    if(!entities.isItemDisplayList(sender)) return@executes
                    val item: ItemStack by it
                    entities.forEach { display ->
                        (display as ItemDisplay).setItemStack(item)
                    }
                    sender.sendMessage(text("아이템 표시 ${entities.size}개의 아이템을 ").append(item.displayName()).append("(으)로 바꿨습니다".miniMessage))
                }
            }
        }
    }
}

class ItemDisplaySpawn(node: BrigadierNode) {
    private fun spawn(sender: CommandSender, item: ItemStack, location: Location, rotation: Vec2? = Vec2(0f, 0f)) {
        val world = location.world
        world.spawn(location, ItemDisplay::class.java).also {
            it.setItemStack(item)
            it.setRotation(rotation?: Vec2(0f, 0f))
        }
        sender.sendMessage(text("새로운 아이템 표시를 소환했습니다"))
    }

    private fun BrigadierNode.runWithPosition(run: CommandSourceStack.(CommandContext<CommandSourceStack>, FinePosition?, Vec2?) -> Unit) {
        this.executes {
            run(this, it, null, null)
        }
        this.then("location" to finePosition()) {
            this.then("rotation" to rotation()) {
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
        node.then("from") {
            then("entity") {
                then("target" to entity()) {
                    then("slot" to slot()) {
                        runWithPosition { it, position, rotation ->
                            val slot: Int by it
                            val target: EntitySelectorArgumentResolver by it
                            val entity = target.resolve(this).getOrNull(0) ?: return@runWithPosition sender.sendRichMessage("${MiniColor.RED}개체를 찾을 수 없습니다")

                            val item = entity.getItem(slot)?: return@runWithPosition sender.sendRichMessage("${MiniColor.RED}원본에게 $slot 슬롯이 없습니다")
                            val location = if(position != null) Location(it.source.world, position.x(), position.y(), position.z())
                            else it.source.location
                            spawn(sender, item, location, rotation)
                        }
                    }
                }
            }
            then("block") {
                then("position" to blockPosition()) {
                    then("slot" to slot()) {
                        runWithPosition { it, position, rotation ->
                            val positionResolver: BlockPositionResolver = it["position"]
                            val blockPosition = positionResolver.resolve(this)
                            val slot: Int by it
                            val block = world.getBlockAt(blockPosition.blockX(), blockPosition.blockY(), blockPosition.blockZ())
                            if(block.state !is BlockInventoryHolder) {
                                sender.sendRichMessage("${MiniColor.RED}원본 위치 ${block.x}, ${block.y}, ${block.z}은(는) 용기가 아닙니다")
                                return@runWithPosition
                            }
                            val inventory = (block.state as BlockInventoryHolder).inventory
                            if(inventory.size <= slot) {
                                sender.sendRichMessage("${MiniColor.RED}원본에게 $slot 슬롯이 없습니다")
                                return@runWithPosition
                            }
                            val item = inventory.getItem(slot)?: ItemStack(Material.AIR)
                            val location = if(position != null) Location(it.source.world, position.x(), position.y(), position.z())
                            else it.source.location
                            spawn(sender, item, location, rotation)
                        }
                    }
                }
            }
        }
        node.then("with") {
            then("item" to itemStack()) {
                runWithPosition { it, position, rotation ->
                    val item: ItemStack by it
                    val location = if(position != null) Location(it.source.world, position.x(), position.y(), position.z())
                    else it.source.location
                    spawn(sender, item, location, rotation)
                }
            }
        }
    }
}