package io.bluetape4k.exposed.dao

import io.bluetape4k.ToStringBuilder
import org.jetbrains.exposed.dao.Entity

inline val <ID: Any> Entity<ID>.idValue: Any? get() = id._value

fun Entity<*>.idEquals(other: Any?): Boolean = when {
    other == null -> false
    this === other -> true
    other.javaClass == this.javaClass -> idValue == (other as Entity<*>).idValue
    else -> false
}

fun <ID: Any> Entity<ID>.idHashCode(): Int = idValue.hashCode()

fun <ID: Any> Entity<ID>.toStringBuilder(): ToStringBuilder =
    ToStringBuilder(this).add("id", idValue)
