package io.bluetape4k.examples.coroutines.guide

sealed class Event {

    object Created: Event() {
        override fun toString(): String = "Created"
    }

    object Deleted: Event() {
        override fun toString(): String = "Deleted"
    }
}
