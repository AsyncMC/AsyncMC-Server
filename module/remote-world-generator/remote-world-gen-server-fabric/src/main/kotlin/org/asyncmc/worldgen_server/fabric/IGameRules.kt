package org.asyncmc.worldgen_server.fabric

import net.minecraft.world.GameRules

interface IGameRules {
    val theRules: Map<GameRules.Key<*>, GameRules.Rule<*>>
}
