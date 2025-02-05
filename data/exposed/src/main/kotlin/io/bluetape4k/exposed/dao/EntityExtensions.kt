package io.bluetape4k.exposed.dao

import io.bluetape4k.ToStringBuilder
import org.jetbrains.exposed.dao.Entity

inline val <ID: Any> Entity<ID>.idValue: Any? get() = id._value

inline fun <reified T: Entity<ID>, ID: Any> Entity<ID>.idEquals(other: Any?): Boolean = when {
    other == null -> false
    this === other -> true
    other is T -> idValue == other.idValue
    else -> false
}

//fun <ID: Any> Entity<ID>.idEquals(other: Any?): Boolean {
//    if (other == null) {
//        return false
//    }
//    if (this === other) {
//        return true
//    }
//
//    if (other !is Entity<*>) {
//        return false
//    }
//    if (this.javaClass.isAssignableFrom(other.javaClass)) {
//        return idValue == other.idValue
//    }
//    return false
//}

fun <ID: Any> Entity<ID>.idHashCode(): Int = idValue.hashCode()

fun <ID: Any> Entity<ID>.toStringBuilder(): ToStringBuilder =
    ToStringBuilder(this).add("id", idValue)
