package kr.blugon.displayeditor

import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.world.block.BlockState
import org.bukkit.Location

data class WorldEditPoint(val minimumPoint: BlockVector3, val maximumPoint: BlockVector3)

fun Clipboard.getBlock(x: Int, y: Int, z: Int): BlockState = this.getBlock(BlockVector3(x, y, z))

fun Pair<BlockVector3, BlockVector3>.relative(origin: BlockVector3, location: Location): WorldEditPoint = WorldEditPoint(this.first, this.second).relative(origin, location)
fun WorldEditPoint.relative(origin: BlockVector3, location: Location): WorldEditPoint {
    return WorldEditPoint(
        BlockVector3(location.blockX-(origin.x()-this.minimumPoint.x()), location.blockY-(origin.y()-this.minimumPoint.y()), location.blockZ-(origin.z()-this.minimumPoint.z())),
        BlockVector3(location.blockX+(this.maximumPoint.x()-origin.x()), location.blockY+(this.maximumPoint.y()-origin.y()), location.blockZ+(this.maximumPoint.z()-origin.z()))
    )
}

fun BlockVector3.unRelative(origin: BlockVector3, location: Location): BlockVector3 {
    return BlockVector3(origin.x()-(location.blockX-this.x()), origin.y()-(location.blockY-this.y()), origin.z()-(location.blockZ-this.z()))
}