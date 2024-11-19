@file:JvmMultifileClass
@file:JvmName("KStreamKt")

package io.bluetape4k.kafka.streams.kstream

import org.apache.kafka.streams.kstream.Window
import org.apache.kafka.streams.kstream.Windowed

fun <K> windowedOf(key: K, window: Window): Windowed<K> = Windowed(key, window)
