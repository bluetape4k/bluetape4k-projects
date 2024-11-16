package io.bluetape4k.junit5.tempfolder

import org.junit.jupiter.api.extension.ExtendWith

/**
 * 테스트 시 임시폴더를 사용할 수 있도록 합니다.
 *
 * 다음은 `@BeforeAll` 에서 임시 폴더를 설정하는 방식입니다.
 * ```
 * @TempFolderTest
 * @TestInstance(TestInstance.Lifecycle.PER_CLASS)
 * class TempFolderExtensionBeforeAllTest {
 *
 *     private lateinit var tempFolder: TempFolder
 *
 *     @BeforeAll
 *     fun beforeAll(tempFolder: TempFolder) {
 *         this.tempFolder = tempFolder
 *     }
 *
 *     // 테스트 코드
 * }
 * ```
 *
 * 다음은 각 테스트 메소드마다 [TempFolder] 인스턴스를 받아 사용하는 방식입니다.
 * ```
 * // 메소드 단위로 임시 폴더를 사용합니다.
 * @Test
 * @TempFolderTest
 * fun `새로 생성된 임시 폴더의 부모 폴더는 root 폴더입니다`(tempFolder: TempFolder) {
 *     val root = tempFolder.root
 *     root.exists().shouldBeTrue()
 *     val dir = tempFolder.createDirectory("bar")
 *     dir.parentFile shouldBeEqualTo root
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
