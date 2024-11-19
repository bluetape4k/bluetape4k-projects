package io.bluetape4k.images.coroutines

import com.sksamuel.scrimage.AwtImage
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.metadata.ImageMetadata
import com.sksamuel.scrimage.nio.ImageWriter
import java.io.OutputStream

/**
 * Coroutines 방식으로 이미지를 생성하는 [ImageWriter] 입니다.
 */
interface CoImageWriter: ImageWriter {

    /**
     * Coroutines 방식으로 [image]를 [out]에 씁니다.
     *
     * @param image   쓸 이미지
     * @param metadata 이미지의 메타데이터 ([ImageMetadata])
     * @param out    쓰기 대상 [OutputStream]
     */
    suspend fun writeSuspending(image: AwtImage, metadata: ImageMetadata, out: OutputStream)

    /**
     * Coroutines 방식으로 [image]를 [out]에 씁니다.
     *
     * @param image  쓸 이미지
     * @param out    쓰기 대상 [OutputStream]
     */
    suspend fun writeSuspending(image: ImmutableImage, out: OutputStream) {
        writeSuspending(image, image.metadata, out)
    }

    /**
     * [image]를 [out]에 씁니다.
     *
     * @param image  쓸 이미지
     * @param out    쓰기용 [OutputStream]
     */
    fun write(image: ImmutableImage, out: OutputStream) {
        write(image, image.metadata, out)
    }
}
