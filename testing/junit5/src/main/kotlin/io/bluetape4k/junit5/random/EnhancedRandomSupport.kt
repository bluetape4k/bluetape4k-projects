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

/**
 * 지정 타입의 랜덤 객체 1개를 생성합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `nextObject(T::class.java, *excludeFields)`를 호출합니다.
 * - `excludeFields`에 지정한 필드는 랜덤 채움 대상에서 제외됩니다.
 * - 생성 실패나 필드 접근 오류는 random-beans 예외로 전파됩니다.
 *
 * ```kotlin
 * val user: User = random.newObject("id")
 * // user.id는 채워지지 않을 수 있다.
 * ```
 *
 * @param excludeFields 랜덤 생성에서 제외할 필드 경로입니다.
 */
inline fun <reified T: Any> EnhancedRandom.newObject(vararg excludeFields: String): T =
    nextObject(T::class.java, *excludeFields)

/**
 * 지정 타입의 랜덤 객체 리스트를 생성합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `objects(...).toList()`를 호출해 새 리스트를 할당합니다.
 * - `size`만큼 생성을 시도하며 제외 필드 규칙은 [newObject]와 동일합니다.
 * - `size`가 0이면 빈 리스트를 반환할 수 있습니다(라이브러리 동작 따름).
 *
 * ```kotlin
 * val users: List<User> = random.newList(size = 3, "password")
 * // users.size == 3
 * ```
 *
 * @param size 생성할 객체 개수입니다.
 * @param excludeFields 랜덤 생성에서 제외할 필드 경로입니다.
 */
inline fun <reified T: Any> EnhancedRandom.newList(size: Int, vararg excludeFields: String): List<T> =
    objects(T::class.java, size, *excludeFields).toList()
