package io.bluetape4k.spring.config

import org.springframework.context.annotation.Profile

/**
 * `local` 프로파일 활성화용 메타 애너테이션입니다.
 *
 * ## 동작/계약
 * - 타입에 선언하면 Spring의 `local` 프로파일과 매핑됩니다.
 * - [name] 기본값은 `"local"`입니다.
 *
 * ```kotlin
 * @LocalProfile
 * class LocalOnlyConfig
 * // local 프로파일에서만 활성화
 * ```
 *
 * @property name 프로파일 이름 메타데이터
 */
@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
@Profile("local")
annotation class LocalProfile(val name: String = "local")

/**
 * `dev`, `develop`, `development` 프로파일 활성화용 메타 애너테이션입니다.
 *
 * ## 동작/계약
 * - 타입에 선언하면 지정된 세 프로파일 중 하나가 활성화될 때 적용됩니다.
 * - [name] 기본값은 `"development"`입니다.
 *
 * ```kotlin
 * @DevelopProfile
 * class DevConfig
 * // dev/develop/development 중 하나에서 활성화
 * ```
 *
 * @property name 프로파일 이름 메타데이터
 */
@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
@Profile("dev", "develop", "development")
annotation class DevelopProfile(val name: String = "development")

/**
 * `feature` 프로파일 활성화용 메타 애너테이션입니다.
 *
 * ## 동작/계약
 * - 타입에 선언하면 Spring의 `feature` 프로파일과 매핑됩니다.
 * - [name] 기본값은 `"feature"`입니다.
 *
 * ```kotlin
 * @FeatureProfile
 * class FeatureToggleConfig
 * // feature 프로파일에서만 활성화
 * ```
 *
 * @property name 프로파일 이름 메타데이터
 */
@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
@Profile("feature")
annotation class FeatureProfile(val name: String = "feature")

/**
 * `test`, `testing` 프로파일 활성화용 메타 애너테이션입니다.
 *
 * ## 동작/계약
 * - 타입에 선언하면 지정된 두 프로파일 중 하나가 활성화될 때 적용됩니다.
 * - [name] 기본값은 `"test"`입니다.
 *
 * ```kotlin
 * @TestProfile
 * class TestConfig
 * // test/testing 프로파일에서만 활성화
 * ```
 *
 * @property name 프로파일 이름 메타데이터
 */
@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
@Profile("test", "testing")
annotation class TestProfile(val name: String = "test")

/**
 * `qa` 프로파일 활성화용 메타 애너테이션입니다.
 *
 * ## 동작/계약
 * - 타입에 선언하면 Spring의 `qa` 프로파일과 매핑됩니다.
 * - [name] 기본값은 `"qa"`입니다.
 *
 * ```kotlin
 * @QaProfile
 * class QaConfig
 * // qa 프로파일에서만 활성화
 * ```
 *
 * @property name 프로파일 이름 메타데이터
 */
@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
@Profile("qa")
annotation class QaProfile(val name: String = "qa")

/**
 * `stage`, `staging` 프로파일 활성화용 메타 애너테이션입니다.
 *
 * ## 동작/계약
 * - 타입에 선언하면 지정된 두 프로파일 중 하나가 활성화될 때 적용됩니다.
 * - [name] 기본값은 `"staging"`입니다.
 *
 * ```kotlin
 * @StageProfile
 * class StageConfig
 * // stage/staging 프로파일에서만 활성화
 * ```
 *
 * @property name 프로파일 이름 메타데이터
 */
@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
@Profile("stage", "staging")
annotation class StageProfile(val name: String = "staging")

/**
 * `prod`, `product`, `production` 프로파일 활성화용 메타 애너테이션입니다.
 *
 * ## 동작/계약
 * - 타입에 선언하면 지정된 세 프로파일 중 하나가 활성화될 때 적용됩니다.
 * - [name] 기본값은 `"production"`입니다.
 *
 * ```kotlin
 * @ProductionProfile
 * class ProductionConfig
 * // prod/product/production 중 하나에서 활성화
 * ```
 *
 * @property name 프로파일 이름 메타데이터
 */
@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
@Profile("prod", "product", "production")
annotation class ProductionProfile(val name: String = "production")
