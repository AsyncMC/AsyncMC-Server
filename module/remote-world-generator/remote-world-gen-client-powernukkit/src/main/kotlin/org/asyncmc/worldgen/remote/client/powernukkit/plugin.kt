package org.asyncmc.worldgen.remote.client.powernukkit

import cn.nukkit.Server

internal val plugin get() = Server.getInstance().pluginManager.getPlugin("AsyncMcRemoteWorldGenClient") as AsyncMcWorldGenPowerNukkitClientPlugin