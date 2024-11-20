package io.bluetape4k.opentelemetry.common

import io.opentelemetry.api.common.AttributeKey

fun stringAttributeKeyOf(key: String): AttributeKey<String> = AttributeKey.stringKey(key)
fun stringArrayAttributeKeyOf(vararg keys: String): AttributeKey<List<String>> =
    AttributeKey.stringArrayKey(keys.joinToString(","))

fun booleanAttributeKeyOf(key: String): AttributeKey<Boolean> = AttributeKey.booleanKey(key)
fun booleanArrayAttributeKeyOf(vararg keys: String): AttributeKey<List<Boolean>> =
    AttributeKey.booleanArrayKey(keys.joinToString(","))

fun longAttributeKeyOf(key: String): AttributeKey<Long> = AttributeKey.longKey(key)
fun longArrayAttributeKeyOf(vararg keys: String): AttributeKey<MutableList<Long>> =
    AttributeKey.longArrayKey(keys.joinToString(","))

fun doubleAttributeKeyOf(key: String): AttributeKey<Double> = AttributeKey.doubleKey(key)
fun doubleArrayAttributeKeyOf(vararg keys: String): AttributeKey<MutableList<Double>> =
    AttributeKey.doubleArrayKey(keys.joinToString(","))

fun String.toAttributeKey(): AttributeKey<String> = AttributeKey.stringKey(this)
fun String.toStringAttributeKey(): AttributeKey<String> = AttributeKey.stringKey(this)

fun String.toStringArrayAttributeKey(): AttributeKey<List<String>> = AttributeKey.stringArrayKey(this)
fun Array<String>.toAttributeKey(): AttributeKey<List<String>> =
    AttributeKey.stringArrayKey(this.joinToString(","))

fun String.toBooleanAttributeKey(): AttributeKey<Boolean> = AttributeKey.booleanKey(this)
fun String.toBooleanArrayAttributeKey(): AttributeKey<List<Boolean>> = AttributeKey.booleanArrayKey(this)

fun String.toLongAttributeKey(): AttributeKey<Long> = AttributeKey.longKey(this)
fun String.toLongArrayAttributeKey(): AttributeKey<List<Long>> = AttributeKey.longArrayKey(this)

fun String.toDoubleAttributeKey(): AttributeKey<Double> = AttributeKey.doubleKey(this)
fun String.toDoubleArrayAttributeKey(): AttributeKey<List<Double>> = AttributeKey.doubleArrayKey(this)

fun Boolean.toAttributeKey(): AttributeKey<Boolean> = AttributeKey.booleanKey(this.toString())
fun BooleanArray.toAttributeKey(): AttributeKey<List<Boolean>> = AttributeKey.booleanArrayKey(this.joinToString(","))

fun Long.toAttributeKey(): AttributeKey<Long> = AttributeKey.longKey(this.toString())
fun LongArray.toAttributeKey(): AttributeKey<List<Long>> = AttributeKey.longArrayKey(this.joinToString(","))

fun Double.toAttributeKey(): AttributeKey<Double> = AttributeKey.doubleKey(this.toString())
fun DoubleArray.toAttributeKey(): AttributeKey<List<Double>> = AttributeKey.doubleArrayKey(this.joinToString(","))
