package io.bluetape4k.io.compressor

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.warn
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipOutputStream

/**
 * ZIP 파일을 빌더 패턴으로 생성하는 유틸리티 클래스입니다.
 *
 * 파일 기반 또는 인메모리(바이트 배열) ZIP 생성을 지원합니다.
 *
 * ```kotlin
 * // 인메모리 ZIP 생성
 * val bytes = ZipBuilder()
 *     .add("hello.txt".toByteArray()).path("hello.txt").save()
 *     .toBytes()
 *
 * // 파일 기반 ZIP 생성
 * val file = ZipBuilder(targetFile)
 *     .add(sourceFile).path("docs/readme.txt").save()
 *     .toZipFile()
 * ```
 *
 * @param targetZipFile 대상 ZIP 파일. null 이면 인메모리로 생성합니다.
 */
class ZipBuilder @JvmOverloads constructor(val targetZipFile: File? = null) {

    companion object: KLogging() {
        /**
         * 파일 기반 [ZipBuilder]를 생성합니다.
         */
        @JvmStatic
        fun of(zipFile: File): ZipBuilder = ZipBuilder(zipFile)

        /**
         * 파일 경로 기반 [ZipBuilder]를 생성합니다.
         */
        @JvmStatic
        fun of(zipFilename: String): ZipBuilder = of(File(zipFilename))

        /**
         * 인메모리 [ZipBuilder]를 생성합니다.
         */
        @JvmStatic
        fun ofInMemory(): ZipBuilder = ZipBuilder(null)
    }

    private val zos: ZipOutputStream
    private val targetBos: ByteArrayOutputStream?

    init {
        if (targetZipFile == null) {
            targetBos = ByteArrayOutputStream()
            zos = ZipOutputStream(targetBos)
        } else {
            if (!targetZipFile.exists()) {
                targetZipFile.parentFile?.mkdirs()
                targetZipFile.createNewFile()
            }
            targetBos = null
            zos = ZipOutputStream(FileOutputStream(targetZipFile))
        }
    }

    /**
     * ZIP 스트림을 닫고 대상 파일을 반환합니다.
     *
     * @return 대상 ZIP 파일. 인메모리 모드에서는 null 을 반환합니다.
     */
    fun toZipFile(): File? {
        runCatching { zos.close() }
            .onFailure { log.warn(it) { "ZipOutputStream 닫기 실패" } }
        return targetZipFile
    }

    /**
     * ZIP 데이터를 바이트 배열로 반환합니다.
     *
     * @return ZIP 데이터의 바이트 배열
     */
    fun toBytes(): ByteArray {
        runCatching { zos.close() }
            .onFailure { log.warn(it) { "ZipOutputStream 닫기 실패" } }

        return try {
            targetZipFile?.readBytes()
                ?: targetBos?.toByteArray()
                ?: byteArrayOf()
        } catch (e: Throwable) {
            log.warn(e) { "ZIP 바이트 배열 변환 실패" }
            byteArrayOf()
        }
    }

    /**
     * 파일을 ZIP에 추가합니다.
     *
     * @param source 추가할 소스 파일
     * @return [AddFileToZip] 빌더
     */
    fun add(source: File): AddFileToZip = AddFileToZip(this, source)

    /**
     * 문자열 콘텐트를 ZIP에 추가합니다.
     *
     * @param content 추가할 문자열 (UTF-8 인코딩)
     * @return [AddContentToZip] 빌더
     */
    fun add(content: String): AddContentToZip = AddContentToZip(this, content.toByteArray(Charsets.UTF_8))

    /**
     * 바이트 배열 콘텐트를 ZIP에 추가합니다.
     *
     * @param content 추가할 바이트 배열
     * @return [AddContentToZip] 빌더
     */
    fun add(content: ByteArray): AddContentToZip = AddContentToZip(this, content)

    /**
     * 빈 폴더 엔트리를 ZIP에 추가합니다.
     *
     * @param folderName 폴더 이름
     * @return 현재 [ZipBuilder] 인스턴스
     */
    fun addFolder(folderName: String): ZipBuilder {
        addFolderToZip(zos, folderName)
        return this
    }

    /**
     * 파일을 ZIP에 추가하기 위한 빌더 클래스입니다.
     *
     * @property zipBuilder 상위 [ZipBuilder]
     * @property file 추가할 소스 파일
     */
    class AddFileToZip(val zipBuilder: ZipBuilder, val file: File) {
        /** ZIP 내 경로 */
        var path: String = ""

        /** 엔트리 코멘트 */
        var comment: String = ""

        /** 디렉토리인 경우 재귀 추가 여부 */
        var recursive: Boolean = true

        /**
         * 경로를 설정합니다.
         */
        fun path(path: String): AddFileToZip = apply { this.path = path }

        /**
         * 코멘트를 설정합니다.
         */
        fun comment(comment: String): AddFileToZip = apply { this.comment = comment }

        /**
         * 재귀 여부를 설정합니다.
         */
        fun recursive(recursive: Boolean): AddFileToZip = apply { this.recursive = recursive }

        /**
         * 파일을 ZIP에 저장합니다.
         *
         * @return 상위 [ZipBuilder] 인스턴스
         */
        fun save(): ZipBuilder {
            addToZip(zipBuilder.zos, file, path, comment, recursive)
            return zipBuilder
        }
    }

    /**
     * 바이트 배열 콘텐트를 ZIP에 추가하기 위한 빌더 클래스입니다.
     *
     * @property zipBuilder 상위 [ZipBuilder]
     * @property content 추가할 콘텐트
     */
    class AddContentToZip(val zipBuilder: ZipBuilder, val content: ByteArray) {
        /** ZIP 내 경로 */
        var path: String = ""

        /** 엔트리 코멘트 */
        var comment: String = ""

        /**
         * 경로를 설정합니다.
         */
        fun path(path: String): AddContentToZip = apply { this.path = path }

        /**
         * 코멘트를 설정합니다.
         */
        fun comment(comment: String): AddContentToZip = apply { this.comment = comment }

        /**
         * 콘텐트를 ZIP에 저장합니다.
         *
         * @return 상위 [ZipBuilder] 인스턴스
         */
        fun save(): ZipBuilder {
            addToZip(zipBuilder.zos, content, path, comment)
            return zipBuilder
        }
    }
}
