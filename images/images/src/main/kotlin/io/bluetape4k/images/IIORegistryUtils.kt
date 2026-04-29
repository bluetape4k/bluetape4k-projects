package io.bluetape4k.images

import io.bluetape4k.collections.toList
import javax.imageio.spi.IIORegistry
import javax.imageio.spi.ImageReaderSpi
import javax.imageio.spi.ImageWriterSpi

/**
 * Image I/O Registry 관련 유틸리티 ([IIORegistry])
 */
object IIORegistryUtils {

    private val registry by lazy { IIORegistry.getDefaultInstance() }

    /**
     * 현재 애플리케이션 클래스패스에 등록된 모든 ImageIO SPI를 [IIORegistry]에 강제 등록합니다.
     *
     * ## 동작/계약
     * - 기본 JVM 클래스로더가 SPI를 자동 감지하지 못하는 경우(예: 테스트 환경, 커스텀 클래스로더)에 유용합니다.
     * - TwelveMonkeys ImageIO TIFF reader/writer가 올바르게 등록되도록 [SuspendTiffWriter] 초기화 시 호출됩니다.
     * - 멱등성: 이미 등록된 SPI는 중복 등록되지 않습니다.
     *
     * ```kotlin
     * IIORegistryUtils.registerApplicationClasspathSpis()
     * val formatNames = IIORegistryUtils.imageWriterFormatNames
     * // formatNames.any { it.equals("tiff", ignoreCase = true) } == true
     * ```
     */
    fun registerApplicationClasspathSpis() {
        registry.registerApplicationClasspathSpis()
    }

    /**
     * Read를 지원하는 Image Format Names
     *
     * ```kotlin
     * val names = IIORegistryUtils.imageReaderFormatNames
     * // names.contains("jpg") == true
     * // names.contains("png") == true
     * ```
     */
    val imageReaderFormatNames: Set<String> by lazy { getReadImageFormatNames() }

    /**
     * Write 를 지원하는 Image Format Names
     *
     * ```kotlin
     * val names = IIORegistryUtils.imageWriterFormatNames
     * // names.contains("png") == true
     * // names.contains("gif") == true
     * ```
     */
    val imageWriterFormatNames: Set<String> by lazy { getWriteImageFormatNames() }

    private inline fun <reified T> getServiceProviders(): List<T> {
        return registry.getServiceProviders(T::class.java, false).toList()
    }

    /**
     * 등록된 모든 [ImageReaderSpi] 목록을 반환합니다.
     *
     * ```kotlin
     * val spis = IIORegistryUtils.getImageReaderSpis()
     * // spis.isNotEmpty() == true
     * ```
     *
     * @return [ImageReaderSpi] 목록
     */
    fun getImageReaderSpis(): List<ImageReaderSpi> {
        return getServiceProviders<ImageReaderSpi>()
    }

    /**
     * 등록된 모든 [ImageWriterSpi] 목록을 반환합니다.
     *
     * ```kotlin
     * val spis = IIORegistryUtils.getImageWriterSpis()
     * // spis.isNotEmpty() == true
     * ```
     *
     * @return [ImageWriterSpi] 목록
     */
    fun getImageWriterSpis(): List<ImageWriterSpi> {
        return getServiceProviders<ImageWriterSpi>()
    }

    /**
     * Read를 지원하는 Image Format Names
     *
     * ```kotlin
     * val names = IIORegistryUtils.getReadImageFormatNames()
     * // names.contains("JPEG") == true
     * ```
     */
    fun getReadImageFormatNames(): Set<String> {
        return getImageReaderSpis().flatMap { it.formatNames.toSet() }.toSet()
    }

    /**
     * Write 를 지원하는 Image Format Names
     *
     * ```kotlin
     * val names = IIORegistryUtils.getWriteImageFormatNames()
     * // names.contains("PNG") == true
     * ```
     */
    fun getWriteImageFormatNames(): Set<String> {
        return getImageWriterSpis().flatMap { it.formatNames.toSet() }.toSet()
    }
}
