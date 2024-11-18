package io.bluetape4k.junit5.tempfolder

import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

/**
 * 테스트 시 임시폴더를 생성해주는 Extension 입니다.
 *
 * 임시 폴더를 테스트 클래스 단위로 생성 ( `@BeforeAll` 에서 temporary folder 생성하기 )
 * ```
 * @ExtendWith(TempFolderExtension::class)
 * class TemporaryFolderExtensionBeforeAllTest {
 *
 * lateinit var tempFolder: TempFolder
 *
 * @BeforeAll
 * fun beforeAll(tempFolder: TempFolder) {
 *     this.tempFolder = tempFolder
 * }
 * ```
 *
 * `@BeforeEach` 에서 temporary folder 생성하기
 *
 * ```
 * @ExtendWith(TempFolderExtension::class)
 * class TemporaryFolderExtensionBeforeEachTest {
 *
 * lateinit var tempFolder: TempFolder
 *
 * @BeforeEach
 * fun setup(tempFolder: TempFolder) {
 *     this.tempFolder = tempFolder
 * }
 * ```
 *
 * 함수별로 임시폴더를 생성
 * ```
 * @Test
 * @ExtendWith(TempFolderExtension::class)
 * fun `인자로 temporary folder를 받을 수 있다`(tempFolder: TempFolder) {
 *     tempFolder.createFile("foo.txt").exists().shouldBeTrue()
 *     tempFolder.createDirectory("bar").exists().shouldBeTrue()
 * }
 * ```
 */
class TempFolderExtension: ParameterResolver {

    companion object: KLogging() {
        private val NAMESPACE = ExtensionContext.Namespace.create(TempFolderExtension::class.java)
    }

    override fun supportsParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext,
    ): Boolean {
        return parameterContext.parameter.type == TempFolder::class.java
    }

    override fun resolveParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext,
    ): Any? {
        return extensionContext.getStore(NAMESPACE)
            .getOrComputeIfAbsent(
                parameterContext,
                { TempFolder() },
                TempFolder::class.java
            )
    }
}
