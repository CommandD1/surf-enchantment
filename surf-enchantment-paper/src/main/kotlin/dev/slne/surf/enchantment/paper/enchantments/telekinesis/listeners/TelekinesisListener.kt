package dev.slne.surf.enchantment.paper.enchantments.telekinesis.listeners

import dev.slne.surf.enchantment.api.enchantments.telekinesis.PostTelekinesisItemEvent
import dev.slne.surf.enchantment.api.enchantments.telekinesis.TelekinesisEnchantment
import dev.slne.surf.enchantment.api.utils.hasCustomEnchantment
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Boat
import org.bukkit.entity.ChestBoat
import org.bukkit.entity.CommandMinecart
import org.bukkit.entity.Entity
import org.bukkit.entity.ExplosiveMinecart
import org.bukkit.entity.HopperMinecart
import org.bukkit.entity.Minecart
import org.bukkit.entity.Player
import org.bukkit.entity.PoweredMinecart
import org.bukkit.entity.StorageMinecart
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerShearEntityEvent
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

object TelekinesisListener : Listener {

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player

        if (!player.inventory.itemInMainHand.hasCustomEnchantment<TelekinesisEnchantment>()) return

        player.giveExp(event.expToDrop, true)
    }

    @EventHandler
    fun onBlockDrop(event: BlockDropItemEvent) {
        val player = event.player
        if (!player.inventory.itemInMainHand.hasCustomEnchantment<TelekinesisEnchantment>()) return

        val dropLocation = event.block.location.clone().add(0.5, 0.5, 0.5)
        val drops = event.items.map { it.itemStack }

        addDropsToInventory(player, drops, event, dropLocation)

        event.items.clear()
    }

    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        val player = event.entity.killer ?: return
        if (!player.inventory.itemInMainHand.hasCustomEnchantment<TelekinesisEnchantment>()) return

        val dropLocation = event.entity.location.clone()
        val drops = event.drops.toList()

        addDropsToInventory(player, drops, event, dropLocation)

        player.giveExp(event.droppedExp, true)
        event.droppedExp = 0
        event.drops.clear()
    }
    
    @EventHandler
    fun onVehicleDestroy(event: VehicleDestroyEvent) {
        val player = event.attacker as? Player ?: return
        if (!player.inventory.itemInMainHand.hasCustomEnchantment<TelekinesisEnchantment>()) return
    
        val vehicle = event.vehicle
        val dropLocation = vehicle.location.clone()
    
        val drops = getVehicleDrops(vehicle) ?: return
        
        vehicle.remove()
    
        addDropsToInventory(player, drops, event, dropLocation)
    }
    
    @EventHandler
    fun onPlayerShearEntity(event: PlayerShearEntityEvent) {
        val player = event.player
        if (!player.inventory.itemInMainHand.hasCustomEnchantment<TelekinesisEnchantment>()) return

        val dropLocation = event.entity.location.clone()
        val drops = event.drops.toList()

        addDropsToInventory(player, drops, event, dropLocation)

        event.drops.clear()
    }

    private fun addDropsToInventory(
        player: Player,
        drops: List<ItemStack>,
        originEvent: Event,
        dropLocation: Location
    ) {
        drops.forEach { drop ->
            val notAdded = player.inventory.addItem(drop)

            val postTelekinesisItemEvent = PostTelekinesisItemEvent(
                player = player,
                itemStack = drop,
                notAddedToInventory = notAdded.toMap(),
                originEvent = originEvent
            )

            notAdded.clear()
            notAdded.putAll(postTelekinesisItemEvent.notAddedToInventory)


            notAdded.values.forEach { item ->
                dropLocation.world.dropItemNaturally(dropLocation, item)
            }
        }
    }
    
    private fun getVehicleDrops(entity: Entity): List<ItemStack>? {
        val drops = mutableListOf<ItemStack>()

        when (entity) {
            is Boat -> drops += ItemStack(boatMaterial(entity.boatType, false))
            is ChestBoat -> drops += ItemStack(boatMaterial(entity.boatType, true))
            is Minecart -> {
                drops += when (entity) {
                    is StorageMinecart -> ItemStack(Material.CHEST_MINECART)
                    is PoweredMinecart -> ItemStack(Material.FURNACE_MINECART)
                    is HopperMinecart -> ItemStack(Material.HOPPER_MINECART)
                    is ExplosiveMinecart -> ItemStack(Material.TNT_MINECART)
                    is CommandMinecart -> ItemStack(Material.COMMAND_BLOCK_MINECART)
                    else -> ItemStack(Material.MINECART)
                }
            }
            else -> return null
        }
        
        if (entity is InventoryHolder) {
            entity.inventory.contents
                .filterNotNull()
                .forEach { drops += it }
        }
        
        return drops
    }

    private fun boatMaterial(type: Boat.Type, chest: Boolean): Material {
        return when (type) {
            Boat.Type.OAK -> if (chest) Material.OAK_CHEST_BOAT else Material.OAK_BOAT
            Boat.Type.SPRUCE -> if (chest) Material.SPRUCE_CHEST_BOAT else Material.SPRUCE_BOAT
            Boat.Type.BIRCH -> if (chest) Material.BIRCH_CHEST_BOAT else Material.BIRCH_BOAT
            Boat.Type.JUNGLE -> if (chest) Material.JUNGLE_CHEST_BOAT else Material.JUNGLE_BOAT
            Boat.Type.ACACIA -> if (chest) Material.ACACIA_CHEST_BOAT else Material.ACACIA_BOAT
            Boat.Type.DARK_OAK -> if (chest) Material.DARK_OAK_CHEST_BOAT else Material.DARK_OAK_BOAT
            Boat.Type.MANGROVE -> if (chest) Material.MANGROVE_CHEST_BOAT else Material.MANGROVE_BOAT
            Boat.Type.CHERRY -> if (chest) Material.CHERRY_CHEST_BOAT else Material.CHERRY_BOAT
            Boat.Type.BAMBOO -> if (chest) Material.BAMBOO_CHEST_RAFT else Material.BAMBOO_RAFT
        }
    }
}
