package io.bluetape4k.javers.base

enum class EntityEventType(val status: String) {
    UNKNOWN("UNKNOWN"),
    SAVED("SAVED"),
    DELETED("DELETED");

    override fun toString(): String = status

    companion object {
        fun valueOf(status: String): EntityEventType? {
            return entries.firstOrNull { it.status == status }
        }
    }
}
