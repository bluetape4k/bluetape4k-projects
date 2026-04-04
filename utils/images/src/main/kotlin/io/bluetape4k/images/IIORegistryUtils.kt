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
        return getServiceProviders<ImageReaderSpi>().toList()
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
        return getServiceProviders<ImageWriterSpi>().toList()
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
