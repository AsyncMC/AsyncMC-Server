package org.asyncmc.worldgen.remote.client.powernukkit

import cn.nukkit.block.Block
import cn.nukkit.block.BlockEntityHolder
import cn.nukkit.block.BlockFlowerPot
import cn.nukkit.block.BlockID
import cn.nukkit.blockentity.*
import cn.nukkit.blockproperty.BooleanBlockProperty
import cn.nukkit.blockstate.BlockState
import cn.nukkit.blockstate.BlockStateRegistry
import cn.nukkit.entity.Entity
import cn.nukkit.entity.passive.EntityBee
import cn.nukkit.inventory.InventoryHolder
import cn.nukkit.item.Item
import cn.nukkit.item.ItemID
import cn.nukkit.item.ItemPotion
import cn.nukkit.item.MinecraftItemID
import cn.nukkit.item.enchantment.Enchantment
import cn.nukkit.level.format.generic.BaseFullChunk
import cn.nukkit.math.BlockFace
import cn.nukkit.nbt.tag.CompoundTag
import cn.nukkit.nbt.tag.ListTag
import cn.nukkit.nbt.tag.Tag
import cn.nukkit.potion.Effect
import cn.nukkit.utils.DyeColor
import cn.nukkit.utils.ServerException
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import org.asyncmc.worldgen.remote.client.powernukkit.entities.EntityFactory
import org.asyncmc.worldgen.remote.data.RemoteBlockEntity
import org.asyncmc.worldgen.remote.data.RemoteBlockState
import org.asyncmc.worldgen.remote.data.RemoteEntity
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.isSuperclassOf

internal object RemoteToPowerNukkitConverter {
    private val blockStateCache = ConcurrentHashMap<RemoteBlockState, LayeredBlockState>()
    private val fallback = LayeredBlockState(BlockState.of(BlockID.STONE))

    private val biomeIds = ConcurrentHashMap<String, UByte>()

    private val blocksWithEntity = Int2ObjectOpenHashMap<String>()

    private val entityFactories = hashMapOf<String, EntityFactory>()

    private val itemIdDataMappings = hashMapOf<String, ItemIdData>()

    private val enchantmentMappings = hashMapOf<String, Int>()

    private val explosionColors by lazy {
        Int2ObjectOpenHashMap<DyeColor>(16).also {
            DyeColor.values().forEach { dye ->
                it[dye.color.rgb] = dye
            }
        }
    }

    private val ominousBannerPatterns = ListTag<CompoundTag>("Patterns")
        .add(CompoundTag()
            .putString("Pattern", "mr").putInt("Color", 9)
            .putString("Pattern", "bs").putInt("Color", 8)
            .putString("Pattern", "cs").putInt("Color", 7)
            .putString("Pattern", "bo").putInt("Color", 8)
            .putString("Pattern", "ms").putInt("Color", 15)
            .putString("Pattern", "hh").putInt("Color", 8)
            .putString("Pattern", "mc").putInt("Color", 8)
            .putString("Pattern", "bo").putInt("Color", 15)
        )

    private val allSlotRemappings = mapOf(
        BlockEntity.BREWING_STAND to mapOf(
            3 to 0,
            0 to 1,
            1 to 2,
            2 to 3,
            4 to 4,
        )
    )

    fun convert(blockState: RemoteBlockState): LayeredBlockState {
        return blockStateCache.computeIfAbsent(blockState) { state ->
            plugin.log.warn { "Got a state that was not cached: $state" }
            val originalId = state.id
            val baseState = LayeredBlockState(BlockState.of(originalId))
            if (baseState.main.runtimeId == BlockStateRegistry.getUpdateBlockRegistration()) {
                plugin.log.error {
                    "Found an unmapped block id: $originalId, Original State: $state"
                }
                return@computeIfAbsent fallback
            }
            state.properties.entries.fold(baseState) { current, (name, value) ->
                blockStatePropertyConverter(originalId, name, value, current)
            }
        }
    }


    internal fun detectBlockStatesWithEntity() {
        @Suppress("DEPRECATION")
        val blockClasses = Block.list
        blockClasses.asSequence()
            .filterNotNull()
            .map { it.kotlin }
            .filter { BlockEntityHolder::class.isSuperclassOf(it) }
            .map { it.createInstance() }
            .forEach {
                blocksWithEntity[it.id] = (it as BlockEntityHolder<*>).blockEntityType
            }

        blocksWithEntity[BlockID.BREWING_STAND_BLOCK] = BlockEntity.BREWING_STAND
    }

    internal fun addItemMappings(mappings: Map<String, ItemIdData>) {
        itemIdDataMappings += mappings
    }

    internal fun addEntityFactories(factories: Map<String, EntityFactory>) {
        entityFactories += factories
    }

    internal fun addToBlockCache(mappings: Map<RemoteBlockState, LayeredBlockState>) {
        blockStateCache += mappings
    }

    internal fun addBiomeMappings(mappings: Map<String, UByte>) {
        biomeIds += mappings
    }

    internal fun addEnchantmentMappings(mappings: Map<String, Int>) {
        enchantmentMappings += mappings
    }

    private fun blockStatePropertyConverter(id: String, name: String, value: String, current: LayeredBlockState): LayeredBlockState {
        if (name == "waterlogged") {
            return if (value == "true") {
                current.copy(fluid = BlockState.of(BlockID.WATER))
            } else {
                current
            }
        }
        return try {
            val main = current.main
            val property = main.getProperty(name)
            val updated = if (property is BooleanBlockProperty) {
                main.withProperty(property, value.toBoolean())
            } else {
                main.withProperty(name, value)
            }
            current.copy(main = updated)
        } catch (e: Exception) {
            plugin.log.error(e) {
                "Attempted to apply an unsupported property. Original id: $id, Property: $name, Value: $value, current state: $current"
            }
            current
        }
    }

    fun convertBiomeId(biome: String, fallback: UByte): UByte {
        return biomeIds.getOrDefault(biome, fallback)
    }

    fun isMap(itemId: String): Boolean {
        return itemId == "minecraft:filled_map"
    }

    @Suppress("UNUSED_PARAMETER")
    fun isPhoto(itemId: String): Boolean {
        return false
    }

    fun createDefaultBlockEntity(
        blockState: BlockState,
        chunk: BaseFullChunk,
        cx: Int,
        cy: Int,
        cz: Int,
        remoteBlockEntity: RemoteBlockEntity?,
        remoteBlockState: RemoteBlockState?,
        nbt: CompoundTag = CompoundTag()
    ): BlockEntity? {
        val blockEntityType = blocksWithEntity[blockState.blockId] ?: return null
        val bx = (chunk.x shl 4) + cx
        val bz = (chunk.z shl 4) + cz
        return BlockEntity.createBlockEntity(
            blockEntityType,
            chunk,
            nbt.putString("id", blockEntityType)
                .putInt("x", bx)
                .putInt("y", cy)
                .putInt("z", bz)
        ).also { blockEntity ->
            if (blockEntity == null) {
                plugin.log.error { "Could not create the block entity for $blockState" }
                return null
            }
            if (remoteBlockEntity == null) {
                if (blockEntity !is BlockEntityItemFrame) {
                    plugin.log.warn { "There's no remote block entity for the block at ${blockEntity.location}" }
                }
                return blockEntity
            }
            try {
                applyBlockEntityData(remoteBlockState, remoteBlockEntity, blockEntity)
            } catch (e: Exception) {
                plugin.log.error(e) { "Error while applying block entity data for $blockState" }
            }
            chunk.addBlockEntity(blockEntity)
        }
    }

    private fun applyBlockEntityData(remoteBlock: RemoteBlockState?, remoteBlockEntity: RemoteBlockEntity, entity: BlockEntity) {
        val entityNbt = remoteBlockEntity.nbt.deserializeForNukkit()
        if (entity is InventoryHolder && entityNbt.containsList("Items", Tag.TAG_Compound)) {
            val slotRemapping = allSlotRemappings[entity.saveId]
            val inventory = entity.inventory
            val inventoryTag = entityNbt.getList("Items", CompoundTag::class.java)
            for (i in 0 until inventoryTag.size()) {
                val itemTag = inventoryTag[i]
                val slot = itemTag.getByte("Slot")
                val remapped = slotRemapping?.get(slot) ?: slot
                val item = convertItem(itemTag) ?: continue
                inventory.setItem(remapped, item)
            }
        }
        when (entity) {
            is BlockEntityBrewingStand -> {
                if (entityNbt.containsByte("Fuel")) {
                    entity.fuelAmount = entityNbt.getByte("Fuel")
                }
            }
            is BlockEntityBanner -> {
                requireNotNull(remoteBlock)
                val colorName = remoteBlock.id.substringAfter(':').replace("_banner", "").replace("_wall", "").uppercase()
                val color = DyeColor.valueOf(colorName)
                entity.color = color.dyeData
                entity.setBaseColor(color)
                if (entityNbt.containsList("Patterns", Tag.TAG_Compound)) {
                    val patterns = entityNbt.getList("Patterns", CompoundTag::class.java)
                    if (patterns.equals(ominousBannerPatterns)) {
                        entity.type = 1
                    } else {
                        entity.namedTag.put("Patterns", patterns.copy())
                    }
                }
            }
            is BlockEntitySkull -> {
                requireNotNull(remoteBlock)
                entity.namedTag.putByte("SkullType", when (remoteBlock.id) {
                    "skeleton_skull", "skeleton_wall_skull" -> 0
                    "wither_skeleton_skull", "wither_skeleton_wall_skull" -> 1
                    "zombie_head", "zombie_wall_head" -> 2
                    "player_head", "player_wall_head" -> 3
                    "creeper_head", "creeper_wall_head" -> 4
                    "dragon_head", "dragon_wall_head" -> 5
                    else -> 0
                })
            }
            is BlockEntityBed -> {
                requireNotNull(remoteBlock)
                val colorName = remoteBlock.id.substringAfter(':').substringBeforeLast("_bed")
                val color = DyeColor.valueOf(colorName)
                entity.color = color.dyeData
            }
            is BlockEntityMusic -> {
                requireNotNull(remoteBlock)
                entity.namedTag.putByte("note", remoteBlock.properties["note"]?.toIntOrNull() ?: 0)
            }
            is BlockEntityPistonArm -> {
                requireNotNull(remoteBlock)
                if (remoteBlock.id.startsWith("minecraft:sticky")) {
                    entity.sticky = true
                }
            }
            is BlockEntityShulkerBox -> {
                requireNotNull(remoteBlock)
                val facing = remoteBlock.properties["facing"]?.uppercase()?.let { BlockFace.valueOf(it) }
                if (facing != null) {
                    entity.namedTag.putByte("facing", facing.index)
                }
            }
            is BlockEntityLectern -> {
                if (entityNbt.containsCompound("Book")) {
                    convertItem(entityNbt.getCompound("Book"))?.let { book ->
                        entity.book = book
                        if (entityNbt.containsInt("Page")) {
                            entity.rawPage = entityNbt.getInt("Page") / 2
                        }
                    }
                }
            }
            is BlockEntityJukebox -> {
                if (entityNbt.containsCompound("RecordItem")) {
                    convertItem(entityNbt.getCompound("RecordItem"))?.let { record ->
                        entity.recordItem = record
                    }
                }
            }
            is BlockEntityHopper -> {
                if (entityNbt.containsInt("TransferCooldown")) {
                    entity.transferCooldown = entityNbt.getInt("TransferCooldown")
                }
            }
            is BlockEntityBeehive -> {
                if (entityNbt.containsList("Bees", Tag.TAG_Compound)) {
                    val beesListTag = entityNbt.getList("Bees", CompoundTag::class.java)
                    for (i in 0 until beesListTag.size()) {
                        val beeTag = beesListTag[i]
                        val minOccupationTime = beeTag.getInt("MinOccupationTicks")
                        val ticksInHive = beeTag.getInt("TicksInHive")
                        val ticksLeftToStay = maxOf(0, minOccupationTime - ticksInHive)
                        val remoteBeeData = beeTag.getCompound("EntityData")
                        val bee = createNukkitEntity(entity.chunk as BaseFullChunk,
                            RemoteEntity(
                                id = remoteBeeData.getString("id"),
                                x = entity.x.toFloat(),
                                y = entity.y.toFloat(),
                                z = entity.z.toFloat(),
                                nbt = remoteBeeData.serialize()
                            )
                        )
                        val hasNectar = (bee as? EntityBee)?.hasNectar == true
                        entity.addOccupant(bee, ticksLeftToStay, hasNectar, false)
                    }
                }
            }
            is BlockFlowerPot -> {
                requireNotNull(remoteBlock)
                val item: Item? = when (remoteBlock.id) {
                    "minecraft:potted_dandelion" -> MinecraftItemID.YELLOW_FLOWER.get(1)
                    "minecraft:potted_poppy" -> MinecraftItemID.RED_FLOWER.get(1)
                    "minecraft:potted_blue_orchid" -> Item.getBlock(BlockID.RED_FLOWER, 1)
                    "minecraft:potted_allium" -> Item.getBlock(BlockID.RED_FLOWER, 2)
                    "minecraft:potted_azure_bluet" -> Item.getBlock(BlockID.RED_FLOWER, 3)
                    "minecraft:potted_red_tulip" -> Item.getBlock(BlockID.RED_FLOWER, 4)
                    "minecraft:potted_orange_tulip" -> Item.getBlock(BlockID.RED_FLOWER, 5)
                    "minecraft:potted_white_tulip" -> Item.getBlock(BlockID.RED_FLOWER, 6)
                    "minecraft:potted_pink_tulip" -> Item.getBlock(BlockID.RED_FLOWER, 7)
                    "minecraft:potted_oxeye_daisy" -> Item.getBlock(BlockID.RED_FLOWER, 8)
                    "minecraft:potted_cornflower" -> Item.getBlock(BlockID.RED_FLOWER, 9)
                    "minecraft:potted_lily_of_the_valley" -> Item.getBlock(BlockID.RED_FLOWER, 10)
                    "minecraft:potted_wither_rose" -> MinecraftItemID.WITHER_ROSE.get(1)
                    "minecraft:potted_oak_sapling" -> Item.getBlock(BlockID.SAPLING, 0)
                    "minecraft:potted_spruce_sapling" -> Item.getBlock(BlockID.SAPLING, 1)
                    "minecraft:potted_birch_sapling" -> Item.getBlock(BlockID.SAPLING, 2)
                    "minecraft:potted_jungle_sapling" -> Item.getBlock(BlockID.SAPLING, 3)
                    "minecraft:potted_acacia_sapling" -> Item.getBlock(BlockID.SAPLING, 4)
                    "minecraft:potted_dark_oak_sapling" -> Item.getBlock(BlockID.SAPLING, 4)
                    "minecraft:potted_red_mushroom" -> MinecraftItemID.RED_MUSHROOM.get(1)
                    "minecraft:potted_brown_mushroom" -> MinecraftItemID.BROWN_MUSHROOM.get(1)
                    "minecraft:potted_fern" -> Item.getBlock(BlockID.TALL_GRASS, 1)
                    "minecraft:potted_dead_bush" -> MinecraftItemID.DEADBUSH.get(1)
                    "minecraft:potted_cactus" -> MinecraftItemID.CACTUS.get(1)
                    "minecraft:potted_bamboo" -> MinecraftItemID.BAMBOO.get(1)
                    // "minecraft:potted_azalea_bush" -> TODO
                    //"minecraft:potted_flowering_azalea_bush" -> TODO
                    "minecraft:potted_crimson_fungus" -> MinecraftItemID.CRIMSON_FUNGUS.get(1)
                    "minecraft:potted_warped_fungus" -> MinecraftItemID.WARPED_FUNGUS.get(1)
                    "minecraft:potted_crimson_roots" -> MinecraftItemID.CRIMSON_ROOTS.get(1)
                    "minecraft:potted_warped_roots" -> MinecraftItemID.WARPED_ROOTS.get(1)
                    else -> null
                }
                entity.setFlower(item)
            }
        }
    }

    fun createNukkitEntity(chunk: BaseFullChunk, remoteEntity: RemoteEntity): Entity? {
        val factory = entityFactories[remoteEntity.id] ?: return null
        return factory.createEntity(remoteEntity, chunk)
    }

    fun convertItem(baseItem: CompoundTag): Item? {
        val remoteId = baseItem.getString("id")
        val mapping = itemIdDataMappings[remoteId] ?: return null
        val count = baseItem.getByte("Count")
        var item = Item.get(mapping.id, mapping.data, count)
        val remoteItem = baseItem.getCompound("tag")
        if (remoteItem.containsShort("Damage")) {
            item.damage = remoteItem.getShort("Damage")
        }
        val enchantments = mutableListOf<Enchantment>()
        if (remoteItem.containsList("Enchantments", Tag.TAG_Compound)) {
            val list = remoteItem.getList("Enchantments", CompoundTag::class.java)
            for (i in 0 until list.size()) {
                val enchantmentTag = list[i]
                val enchantment = convertEnchantment(enchantmentTag) ?: continue
                enchantments += enchantment
            }
        }
        val itemId = item.id
        if (itemId == ItemID.ENCHANTED_BOOK && remoteItem.containsList("StoredEnchantments", Tag.TAG_Compound)) {
            val list = remoteItem.getList("StoredEnchantments", CompoundTag::class.java)
            for (i in 0 until list.size()) {
                val enchantmentTag = list[i]
                val enchantment = convertEnchantment(enchantmentTag) ?: continue
                enchantments += enchantment
            }
        }
        if (itemId == ItemID.POTION || itemId == ItemID.LINGERING_POTION || itemId == ItemID.SPLASH_POTION) {
            item.damage = convertPotionId(remoteItem.getString("Potion"))
        }
        val nbt = CompoundTag()
        if (remoteItem.containsByte("Unbreakable")) {
            nbt.putBoolean("Unbreakable", remoteItem.getBoolean("Unbreakable"))
        }
        if (baseItem.containsInt("RepairCost")) {
            nbt.putInt("RepairCost", baseItem.getInt("RepairCost"))
        }
        if (remoteItem.containsCompound("Fireworks")) {
            convertFireworks(remoteItem, nbt)
        }
        if (remoteItem.containsCompound("Explosion")) {
            val explosion = convertFireworkExplosion(remoteItem.getCompound("Explosion"))
            nbt.putCompound("Explosion", explosion)
        }
        if (itemId == ItemID.SUSPICIOUS_STEW) {
            val effects = remoteItem.getList("Effects", CompoundTag::class.java)
            val data = effects.all.firstNotNullOfOrNull s@{ effectTag ->
                val id = effectTag.getByte("EffectId")
                val effect = convertNumericEffectId(id) ?: return@s null
                when (effect.id) {
                    Effect.NIGHT_VISION -> 0
                    Effect.JUMP_BOOST -> 1
                    Effect.WEAKNESS -> 2
                    Effect.BLINDNESS -> 3
                    Effect.POISON -> 4
                    Effect.SATURATION -> 6
                    Effect.FIRE_RESISTANCE -> 7
                    Effect.REGENERATION -> 8
                    Effect.WITHER -> 9
                    else -> null
                }
            }
            if (data != null) {
                item.damage = data
            }
        }
        if (!nbt.isEmpty) {
            item.setCompoundTag(nbt)
        }
        if (enchantments.isNotEmpty()) {
            item.addEnchantment(*enchantments.toTypedArray())
        } else if (itemId == ItemID.ENCHANTED_BOOK) {
            item = Item.get(ItemID.BOOK, 0, item.count, item.compoundTag)
        }
        return item
    }

    private fun convertNumericEffectId(id: Int): Effect? {
        return try {
            val bedrock = when (id) {
                24 -> return null //glowing
                25 -> 24 //levitation
                26 -> return null // luck
                27 -> return null // unluck
                28 -> 27 //slow_falling
                29 -> 26 //conduit_power
                30 -> return null //dolphins_grace
                31 -> 28 //bad_omen
                32 -> 29 //hero_of_the_village
                else -> id
            }
            Effect.getEffect(bedrock)
        } catch (_: ServerException) {
            null
        }
    }

    private fun convertFireworks(remoteItem: CompoundTag, nbt: CompoundTag) {
        val explosions = remoteItem.getCompound("Explosions").getList("Explosions", CompoundTag::class.java)
        val bedrockExplosions = ListTag<CompoundTag>("Explosions")
        for (i in 0 until explosions.size()) {
            val explosion = explosions[i]
            bedrockExplosions.add(convertFireworkExplosion(explosion))
        }
        nbt.putCompound("Fireworks", CompoundTag().putList(bedrockExplosions))
    }

    private fun convertFireworkExplosion(explosion: CompoundTag): CompoundTag? {
        val javaColors = explosion.getIntArray("Colors")
        val javaFadeColors = explosion.getIntArray(" FadeColors")
        val flicker = explosion.getBoolean("Flicker")
        val trail = explosion.getBoolean("Trail")
        val type = explosion.getByte("Type")
        val bedrockColors = ByteArray(javaColors.size) { index ->
            (explosionColors[javaColors[index]] ?: explosionColors.values.random()).dyeData.toByte()
        }
        val bedrockFadeColors = ByteArray(javaFadeColors.size) { index ->
            (explosionColors[javaFadeColors[index]] ?: explosionColors.values.random()).dyeData.toByte()
        }
        return CompoundTag()
            .putByteArray("FireworkColor", bedrockColors)
            .putByteArray("FireworkFade", bedrockFadeColors)
            .putBoolean("FireworkFlicker", flicker)
            .putBoolean("FireworkTrail", trail)
            .putByte("FireworkType", type)
    }

    private fun convertEnchantment(nbt: CompoundTag): Enchantment? {
        val id = nbt.getString("id")
        val nukkitId = enchantmentMappings[id] ?: return null
        val enchantment = Enchantment.getEnchantment(nukkitId)
        val level = nbt.getShort("lvl")
        enchantment.setLevel(level, false)
        return enchantment
    }

    private fun convertPotionId(id: String): Int {
        return when (id) {
            "minecraft:empty" -> ItemPotion.MUNDANE
            "minecraft:water" -> ItemPotion.NO_EFFECTS
            "minecraft:mundane" -> ItemPotion.MUNDANE
            "minecraft:thick" -> ItemPotion.THICK
            "minecraft:awkward" -> ItemPotion.AWKWARD
            "minecraft:night_vision" -> ItemPotion.NIGHT_VISION
            "minecraft:long_night_vision" -> ItemPotion.NIGHT_VISION_LONG
            "minecraft:invisibility" -> ItemPotion.INVISIBLE
            "minecraft:long_invisibility" -> ItemPotion.INVISIBLE_LONG
            "minecraft:leaping" -> ItemPotion.LEAPING
            "minecraft:strong_leaping" -> ItemPotion.LEAPING_II
            "minecraft:long_leaping" -> ItemPotion.LEAPING_LONG
            "minecraft:fire_resistance" -> ItemPotion.FIRE_RESISTANCE
            "minecraft:long_fire_resistance" -> ItemPotion.FIRE_RESISTANCE_LONG
            "minecraft:swiftness" -> ItemPotion.SPEED
            "minecraft:strong_swiftness" -> ItemPotion.SPEED_II
            "minecraft:long_swiftness" -> ItemPotion.SPEED_LONG
            "minecraft:slowness" -> ItemPotion.SLOWNESS
            "minecraft:strong_slowness" -> 42
            "minecraft:long_slowness" -> ItemPotion.SLOWNESS_LONG
            "minecraft:water_breathing" -> ItemPotion.WATER_BREATHING
            "minecraft:long_water_breathing" -> ItemPotion.WATER_BREATHING_LONG
            "minecraft:healing" -> ItemPotion.INSTANT_HEALTH
            "minecraft:strong_healing" -> ItemPotion.INSTANT_HEALTH_II
            "minecraft:harming" -> ItemPotion.HARMING
            "minecraft:strong_harming" -> ItemPotion.HARMING_II
            "minecraft:poison" -> ItemPotion.POISON
            "minecraft:strong_poison" -> ItemPotion.POISON_II
            "minecraft:long_poison" -> ItemPotion.POISON_LONG
            "minecraft:regeneration" -> ItemPotion.REGENERATION
            "minecraft:strong_regeneration" -> ItemPotion.REGENERATION_II
            "minecraft:long_regeneration" -> ItemPotion.REGENERATION_LONG
            "minecraft:strength" -> ItemPotion.STRENGTH
            "minecraft:strong_strength" -> ItemPotion.STRENGTH_II
            "minecraft:long_strength" -> ItemPotion.STRENGTH_LONG
            "minecraft:weakness" -> ItemPotion.WEAKNESS
            "minecraft:long_weakness" -> ItemPotion.WEAKNESS_LONG
            "minecraft:luck" -> ItemPotion.MUNDANE
            "minecraft:turtle_master" -> 37
            "minecraft:strong_turtle_master" -> 39
            "minecraft:long_turtle_master" -> 38
            "minecraft:slow_falling" -> 40
            "minecraft:long_slow_falling" -> 41
            else -> ItemPotion.MUNDANE
        }
    }

    internal data class LayeredBlockState(
        val main: BlockState,
        val fluid: BlockState = BlockState.AIR
    )

    internal data class ItemIdData(val id: Int, val data: Int)
}
