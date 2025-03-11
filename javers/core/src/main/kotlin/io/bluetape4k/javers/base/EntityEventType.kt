package io.bluetape4k.javers.base

enum class EntityEventType(val status: String) {
    UNKNOWN("UNKNOWN"),
    SAVED("SAVED"),
    DELETED("DELETED");

    override fun toString(): String = status

    companion object {
        val VALS: Array<EntityEventType> = values()

        fun valueOf(status: String): EntityEventType? {
            return VALS.firstOrNull { it.status == status }
        }
    }
}
