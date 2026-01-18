package io.bluetape4k.collections.eclipse.stream

import io.bluetape4k.collections.asIterable
import io.bluetape4k.collections.eclipse.primitives.toDoubleArrayList
import io.bluetape4k.collections.eclipse.primitives.toFloatArrayList
import io.bluetape4k.collections.eclipse.primitives.toIntArrayList
import io.bluetape4k.collections.eclipse.primitives.toLongArrayList
import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.collections.eclipse.toUnifiedSet
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList
import java.util.stream.DoubleStream
import java.util.stream.IntStream
import java.util.stream.LongStream
import java.util.stream.Stream

fun <T> Stream<T>.toFastList() = asIterable().toFastList()
fun <T> Stream<T>.toUnifiedSet() = asIterable().toUnifiedSet()

fun IntStream.toIntArrayList(): IntArrayList = asIterable().toIntArrayList()
fun LongStream.toLongArrayList(): LongArrayList = asIterable().toLongArrayList()
fun DoubleStream.toFloatArrayList(): FloatArrayList = asIterable().map { it.toFloat() }.toFloatArrayList()
fun DoubleStream.toDoubleArrayList(): DoubleArrayList = asIterable().toDoubleArrayList()

fun IntArrayList.toIntStream(): IntStream =
    IntStream.builder()
        .also { builder ->
            this@toIntStream.forEach { builder.accept(it) }
        }
        .build()

fun LongArrayList.toLongStream(): LongStream =
    LongStream.builder()
        .also { builder ->
            this@toLongStream.forEach { builder.accept(it) }
        }
        .build()

fun FloatArrayList.toDoubleStream(): DoubleStream =
    DoubleStream.builder()
        .also { builder ->
            this@toDoubleStream.forEach { builder.accept(it.toDouble()) }
        }
        .build()


fun DoubleArrayList.toDoubleStream(): DoubleStream =
    DoubleStream.builder()
        .also { builder ->
            this@toDoubleStream.forEach { builder.accept(it) }
        }
        .build()
