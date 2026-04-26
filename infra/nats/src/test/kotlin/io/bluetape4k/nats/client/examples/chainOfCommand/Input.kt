package io.bluetape4k.nats.client.examples.chainOfCommand

import java.io.Serializable

data class Input(
    val aId: Int = -1,
    val bId: Int = -1,
): Serializable {
    override fun toString(): String {
        return "Worker A$aId, Workder B$bId"
    }
}
