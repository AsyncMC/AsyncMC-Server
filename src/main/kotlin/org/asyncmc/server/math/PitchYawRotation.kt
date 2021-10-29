package org.asyncmc.server.math

import kotlinx.serialization.Serializable

@Serializable
public data class PitchYawRotation(public val pitchYaw: PitchYaw, public val rotation: Float) {
    public inline val pitch: Float get() = pitchYaw.pitch
    public inline val yaw: Float get() = pitchYaw.yaw
}
