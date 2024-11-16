package io.bluetape4k.junit5.random

import io.github.benas.randombeans.EnhancedRandomBuilder
import io.github.benas.randombeans.api.EnhancedRandom


internal inline fun enhancedRandom(action: EnhancedRandomBuilder.() -> Unit): EnhancedRandom =
    EnhancedRandomBuilder.aNewEnhancedRandomBuilder().apply(action).build()

internal val DefaultEnhancedRandom: EnhancedRandom by lazy(LazyThreadSafetyMode.PUBLICATION) {
    enhancedRandom {
        seed(System.currentTimeMillis())
        objectPoolSize(10_000)
        randomizationDepth(5)
        charset(Charsets.UTF_8)
        stringLengthRange(2, 256)
        collectionSizeRange(2, 10)
        scanClasspathForConcreteTypes(true)
        overrideDefaultInitialization(true)
        ignoreRandomizationErrors(true)
    }
}

inline fun <reified T: Any> EnhancedRandom.newObject(vararg excludeFields: String): T =
    nextObject(T::class.java, *excludeFields)

inline fun <reified T: Any> EnhancedRandom.newList(size: Int, vararg excludeFields: String): List<T> =
    objects(T::class.java, size, *excludeFields).toList()
