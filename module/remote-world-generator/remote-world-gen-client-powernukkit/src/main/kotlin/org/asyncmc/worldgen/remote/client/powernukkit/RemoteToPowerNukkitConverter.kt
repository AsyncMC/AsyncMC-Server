package org.asyncmc.worldgen.remote.client.powernukkit

import cn.nukkit.block.Block
import cn.nukkit.block.BlockEntityHolder
import cn.nukkit.block.BlockID
import cn.nukkit.blockentity.BlockEntity
import cn.nukkit.blockentity.BlockEntityBanner
import cn.nukkit.blockentity.BlockEntityBrewingStand
import cn.nukkit.blockentity.BlockEntityItemFrame
import cn.nukkit.blockproperty.BooleanBlockProperty
import cn.nukkit.blockstate.BlockState
import cn.nukkit.blockstate.BlockStateRegistry
import cn.nukkit.inventory.InventoryHolder
import cn.nukkit.item.Item
import cn.nukkit.item.ItemID
import cn.nukkit.item.ItemPotion
import cn.nukkit.item.enchantment.Enchantment
import cn.nukkit.level.format.generic.BaseFullChunk
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
import org.asyncmc.worldgen.remote.data.RemoteChunk
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
        remoteChunk: RemoteChunk,
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
                applyBlockEntityData(remoteChunk, remoteBlockEntity, blockEntity)
            } catch (e: Exception) {
                plugin.log.error(e) { "Error while applying block entity data for $blockState" }
            }
            chunk.addBlockEntity(blockEntity)
        }
    }

    private fun applyBlockEntityData(remoteChunk: RemoteChunk, remoteBlockEntity: RemoteBlockEntity, entity: BlockEntity) {
        val x = remoteBlockEntity.x
        val y = remoteBlockEntity.y
        val z = remoteBlockEntity.z
        val remoteBlock = remoteChunk.blockLayers[0][(z and 0xF) or (x and 0xF shl 4) or ((y + remoteChunk.minY) shl 8)]
        val entityNbt = remoteBlockEntity.nbt.deserializeForNukkit()
        if (entity is InventoryHolder && entityNbt.containsList("Items", Tag.TAG_Compound)) {
            val inventory = entity.inventory
            val inventoryTag = entityNbt.getList("Items", CompoundTag::class.java)
            for (i in 0 until inventoryTag.size()) {
                val itemTag = inventoryTag[i]
                val slot = itemTag.getByte("Slot")
                val item = convertItem(itemTag) ?: continue
                inventory.setItem(slot, item)
            }
        }
        if (entity is BlockEntityBrewingStand) {
            plugin.log.info {
                "Brewing"
            }
        }
        if (entity is BlockEntityBanner) {
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
    }

    fun createNukkitEntity(remoteChunk: RemoteChunk, chunk: BaseFullChunk, remoteEntity: RemoteEntity) {
        val factory = entityFactories[remoteEntity.id] ?: return
        factory.createEntity(remoteChunk, remoteEntity, chunk)
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
            item.damage = convertPotionId(baseItem.getString("Potion"))
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
            "empty" -> ItemPotion.MUNDANE
            "water" -> ItemPotion.NO_EFFECTS
            "mundane" -> ItemPotion.MUNDANE
            "thick" -> ItemPotion.THICK
            "awkward" -> ItemPotion.AWKWARD
            "night_vision" -> ItemPotion.NIGHT_VISION
            "long_night_vision" -> ItemPotion.NIGHT_VISION_LONG
            "invisibility" -> ItemPotion.INVISIBLE
            "long_invisibility" -> ItemPotion.INVISIBLE_LONG
            "leaping" -> ItemPotion.LEAPING
            "strong_leaping" -> ItemPotion.LEAPING_II
            "long_leaping" -> ItemPotion.LEAPING_LONG
            "fire_resistance" -> ItemPotion.FIRE_RESISTANCE
            "long_fire_resistance" -> ItemPotion.FIRE_RESISTANCE_LONG
            "swiftness" -> ItemPotion.SPEED
            "strong_swiftness" -> ItemPotion.SPEED_II
            "long_swiftness" -> ItemPotion.SPEED_LONG
            "slowness" -> ItemPotion.SLOWNESS
            "strong_slowness" -> 42
            "long_slowness" -> ItemPotion.SLOWNESS_LONG
            "water_breathing" -> ItemPotion.WATER_BREATHING
            "long_water_breathing" -> ItemPotion.WATER_BREATHING_LONG
            "healing" -> ItemPotion.INSTANT_HEALTH
            "strong_healing" -> ItemPotion.INSTANT_HEALTH_II
            "harming" -> ItemPotion.HARMING
            "strong_harming" -> ItemPotion.HARMING_II
            "poison" -> ItemPotion.POISON
            "strong_poison" -> ItemPotion.POISON_II
            "long_poison" -> ItemPotion.POISON_LONG
            "regeneration" -> ItemPotion.REGENERATION
            "strong_regeneration" -> ItemPotion.REGENERATION_II
            "long_regeneration" -> ItemPotion.REGENERATION_LONG
            "strength" -> ItemPotion.STRENGTH
            "strong_strength" -> ItemPotion.STRENGTH_II
            "long_strength" -> ItemPotion.STRENGTH_LONG
            "weakness" -> ItemPotion.WEAKNESS
            "long_weakness" -> ItemPotion.WEAKNESS_LONG
            "luck" -> ItemPotion.MUNDANE
            "turtle_master" -> 37
            "strong_turtle_master" -> 39
            "long_turtle_master" -> 38
            "slow_falling" -> 40
            "long_slow_falling" -> 41
            else -> ItemPotion.MUNDANE
        }
    }

    internal data class LayeredBlockState(
        val main: BlockState,
        val fluid: BlockState = BlockState.AIR
    )

    internal data class ItemIdData(val id: Int, val data: Int)
}
