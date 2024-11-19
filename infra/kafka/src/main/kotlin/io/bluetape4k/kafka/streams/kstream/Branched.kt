@file:JvmMultifileClass
@file:JvmName("KStreamKt")

package io.bluetape4k.kafka.streams.kstream

import org.apache.kafka.streams.kstream.Branched
import org.apache.kafka.streams.kstream.KStream

fun <K, V> branchedOf(name: String): Branched<K, V> = Branched.`as`(name)

@JvmName("branchedWithFunction")
fun <K, V> branchedOf(chain: (KStream<K, V>) -> KStream<K, V>, name: String? = null): Branched<K, V> =
    Branched.withFunction(chain, name)

@JvmName("branchedWithConsumer")
fun <K, V> branchedOf(chain: (KStream<K, V>) -> Unit, name: String? = null): Branched<K, V> =
    Branched.withConsumer(chain, name)
