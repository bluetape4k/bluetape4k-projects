package io.bluetape4k.junit5.random

import kotlin.reflect.KClass

/**
 * 테스트 필드/파라미터에 랜덤 값을 주입하기 위한 설정 어노테이션입니다.
 *
 * ## 동작/계약
 * - [RandomExtension]이 이 어노테이션을 읽어 random-beans 기반 값을 생성합니다.
 * - `excludes`는 생성에서 제외할 필드 경로 목록입니다.
 * - 컬렉션/배열 계열 파라미터에서는 `size`, `type`이 생성 정책에 사용됩니다.
 *
 * ```kotlin
 * @Test
 * fun `랜덤 사용자`(@RandomValue(type = User::class, size = 3) users: List<User>) {
 *   // users.size == 3
 * }
 * ```
 *
 * @property excludes 랜덤 생성에서 제외할 필드 경로 목록입니다.
 * @property size 컬렉션형 랜덤 생성 시 기본 생성 개수입니다.
 * @property type 컬렉션형 랜덤 생성 시 원소 타입입니다.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.ANNOTATION_CLASS
)
@MustBeDocumented
annotation class RandomValue(
    val excludes: Array<String> = [],
    val size: Int = 10,
    val type: KClass<*> = Any::class,
)
