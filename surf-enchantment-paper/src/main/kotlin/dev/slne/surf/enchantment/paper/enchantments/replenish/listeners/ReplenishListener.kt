package dev.slne.surf.enchantment.paper.enchantments.replenish.listeners

import dev.slne.surf.enchantment.api.enchantments.replenish.ReplenishBlockEvent
import dev.slne.surf.enchantment.api.enchantments.replenish.ReplenishEnchantment
import dev.slne.surf.enchantment.api.utils.hasThisEnchantmentActive
import dev.slne.surf.surfapi.core.api.util.object2ObjectMapOf
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.block.data.Ageable
import org.bukkit.block.data.Bisected
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.inventory.ItemStack
object ReplenishListener : Listener {

    /**
     * Maps harvested blocks to their corresponding replanting logic.
     *
     * Key (Material):
     *   The block type that was harvested by the player.
     *
     * Value (Pair):
     *   first  – The seed item that must be taken from the player's inventory.
     *   second – The block type that should be placed back into the world.
     *
     * This mapping also covers special cases where the harvested block differs
     * from the block used for planting. For example:
     *   - TORCHFLOWER (harvested) → TORCHFLOWER_CROP (planted)
     */
    private val seedMap = object2ObjectMapOf(
        Material.WHEAT to (Material.WHEAT_SEEDS to Material.WHEAT),
        Material.POTATOES to (Material.POTATO to Material.POTATOES),
        Material.CARROTS to (Material.CARROT to Material.CARROTS),
        Material.BEETROOTS to (Material.BEETROOT_SEEDS to Material.BEETROOTS),
        Material.NETHER_WART to (Material.NETHER_WART to Material.NETHER_WART),

        Material.TORCHFLOWER_CROP to (Material.TORCHFLOWER_SEEDS to Material.TORCHFLOWER_CROP),
        Material.TORCHFLOWER to (Material.TORCHFLOWER_SEEDS to Material.TORCHFLOWER_CROP),

        Material.PITCHER_CROP to (Material.PITCHER_POD to Material.PITCHER_CROP)
    )

    @EventHandler(priority = EventPriority.LOWEST)
    fun onBlockBreak(event: BlockDropItemEvent) {
        val player = event.player
        if (!player.hasThisEnchantmentActive<ReplenishEnchantment>()) return

        val blockState = event.blockState
        val mapping = seedMap[blockState.type] ?: return

        val seedType = mapping.first
        val blockToPlace = mapping.second

        val data = blockState.blockData
        if (data is Bisected && data.half == Bisected.Half.TOP) {
            return
        }

        val seedItemStack = ItemStack.of(seedType)
        val isCreative = player.gameMode == GameMode.CREATIVE
        val hasSeedInInventory = player.inventory.containsAtLeast(seedItemStack, 1) || isCreative

        if (hasSeedInInventory) {
            val replenishBlockEvent = ReplenishBlockEvent(
                block = event.block,
                seed = seedItemStack.clone(),
                items = event.items.toMutableList(),
                player = player,
                shouldConsumeSeed = !isCreative,
            )

            if (!replenishBlockEvent.callEvent()) return

            if (replenishBlockEvent.shouldConsumeSeed) {
                player.inventory.removeItem(seedItemStack)
            }

            event.items.clear()
            event.items.addAll(replenishBlockEvent.items)

            val newBlockData = blockToPlace.createBlockData { blockData ->
                if (blockData is Ageable) blockData.age = 0
            }

            event.block.setBlockData(newBlockData, true)

            Particle.COMPOSTER.builder()
                .location(event.block.location.add(0.5, 0.5, 0.5))
                .count(8)
                .offset(0.2, 0.2, 0.2)
                .spawn()
        }
    }
}