package io.bluetape4k.junit5.tempfolder

import org.junit.jupiter.api.extension.ExtendWith

/**
 * 테스트에서 [TempFolder] 파라미터 주입 확장을 활성화합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `@ExtendWith(TempFolderExtension::class)`를 적용합니다.
 * - 클래스/파일/함수 수준에서 사용할 수 있습니다.
 * - 실제 폴더 생성/정리 규칙은 [TempFolderExtension], [TempFolder] 구현을 따릅니다.
 *
 * ```kotlin
 * @TempFolderTest
 * class TempSpec {
 *   @org.junit.jupiter.api.Test fun create(tf: TempFolder) { /* tf.createFile().exists() == true */ }
 * }
 * ```
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FILE,
    AnnotationTarget.FUNCTION
)
@MustBeDocumented
@Repeatable
@ExtendWith(TempFolderExtension::class)
annotation class TempFolderTest
