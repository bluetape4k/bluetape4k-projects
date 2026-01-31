package io.bluetape4k.images

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.collections.eclipse.toUnifiedSet
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
     */
    val imageReaderFormatNames: Set<String> by lazy { getReadImageFormatNames() }

    /**
     * Write 를 지원하는 Image Format Names
     */
    val imageWriterFormatNames: Set<String> by lazy { getWriteImageFormatNames() }

    private inline fun <reified T> getServiceProviders(): Sequence<T> {
        return registry.getServiceProviders(T::class.java, false).asSequence()
    }

    fun getImageReaderSpis(): List<ImageReaderSpi> {
        return getServiceProviders<ImageReaderSpi>().toFastList()
    }

    fun getImageWriterSpis(): List<ImageWriterSpi> {
        return getServiceProviders<ImageWriterSpi>().toFastList()
    }

    /**
     * Read를 지원하는 Image Format Names
     */
    fun getReadImageFormatNames(): Set<String> {
        return getImageReaderSpis().flatMap { it.formatNames.toList() }.toUnifiedSet()
    }

    /**
     * Write 를 지원하는 Image Format Names
     */
    fun getWriteImageFormatNames(): Set<String> {
        return getImageWriterSpis().flatMap { it.formatNames.toList() }.toUnifiedSet()
    }
}
