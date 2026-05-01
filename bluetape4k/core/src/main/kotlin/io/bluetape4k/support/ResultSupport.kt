package io.bluetape4k.support

/**
 * `Collection<Result<T>>` 컬렉션에 대한 편의 확장입니다.
 * `List<Result<T>>`, `Set<Result<T>>` 등 모든 `Collection` 구현체에 적용됩니다.
 *
 * ## 사용 예
 * ```kotlin
 * val results: List<Result<Int>> = listOf(
 *     Result.success(1),
 *     Result.failure(RuntimeException("fail")),
 *     Result.success(3)
 * )
 *
 * results.allSuccess   // false
 * results.hasFailure   // true
 * results.successes    // [1, 3]
 * results.failures     // [RuntimeException("fail")]
 * ```
 */

/** 모든 항목이 성공이면 `true`입니다. */
val <T> Collection<Result<T>>.allSuccess: Boolean get() = all { it.isSuccess }

/** 모든 항목이 실패이면 `true`입니다. */
val <T> Collection<Result<T>>.allFailure: Boolean get() = all { it.isFailure }

/** 하나라도 실패 항목이 있으면 `true`입니다. */
val <T> Collection<Result<T>>.hasFailure: Boolean get() = any { it.isFailure }

/** 하나라도 성공 항목이 있으면 `true`입니다. */
val <T> Collection<Result<T>>.hasSuccess: Boolean get() = any { it.isSuccess }

/**
 * 성공 결과 값 리스트를 반환합니다. 순서는 원본 컬렉션의 이터레이션 순서 기준입니다.
 *
 * **주의**: `T`가 nullable 타입일 때 `null` 성공 결과도 포함됩니다.
 */
val <T> Collection<Result<T>>.successes: List<T> get() = filter { it.isSuccess }.map { it.getOrThrow() }

/** 실패 예외 리스트를 반환합니다. 순서는 원본 컬렉션의 이터레이션 순서 기준입니다. */
val <T> Collection<Result<T>>.failures: List<Throwable> get() = mapNotNull { it.exceptionOrNull() }
