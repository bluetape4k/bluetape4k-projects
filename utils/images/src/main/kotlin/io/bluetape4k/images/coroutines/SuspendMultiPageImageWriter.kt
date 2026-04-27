package io.bluetape4k.images.coroutines

import com.sksamuel.scrimage.ImmutableImage
import java.io.OutputStream

/**
 * 복수의 이미지 페이지를 단일 [OutputStream]에 순서대로 기록하는 코루틴 기반 writer 인터페이스입니다.
 *
 * ## 동작/계약
 * - [suspendWrite]는 `images` 리스트를 순서대로 단일 출력 스트림에 씁니다.
 * - 단일 이미지 쓰기는 확장 함수 `suspendWrite(image, out)`를 사용합니다.
 * - [SuspendImageWriter]를 상속하지 않습니다. 단일 이미지 writer와 혼용 시 명시적 캐스팅이 필요합니다.
 *
 * ```kotlin
 * val writer: SuspendMultiPageImageWriter = SuspendTiffMultiPageWriter()
 * val images = listOf(image1, image2, image3)
 * val bos = ByteArrayOutputStream()
 * writer.suspendWrite(images, bos)
 * ```
 *
 * @see SuspendTiffMultiPageWriter
 */
interface SuspendMultiPageImageWriter {

    /**
     * [images] 리스트를 단일 [out] 스트림에 다중 페이지 형식으로 씁니다.
     *
     * @param images 쓸 이미지 리스트 (순서 보장)
     * @param out    쓰기 대상 [OutputStream]
     */
    suspend fun suspendWrite(images: List<ImmutableImage>, out: OutputStream)
}

/**
 * 단일 [image]를 단일 페이지 다중 페이지 포맷으로 [out]에 씁니다.
 *
 * ## 동작/계약
 * - `listOf(image)`를 사용해 [SuspendMultiPageImageWriter.suspendWrite]를 위임합니다.
 *
 * ```kotlin
 * val writer: SuspendMultiPageImageWriter = SuspendTiffMultiPageWriter()
 * val bos = ByteArrayOutputStream()
 * writer.suspendWrite(singleImage, bos)
 * ```
 *
 * @param image 쓸 단일 이미지
 * @param out   쓰기 대상 [OutputStream]
 */
suspend fun SuspendMultiPageImageWriter.suspendWrite(image: ImmutableImage, out: OutputStream) {
    suspendWrite(listOf(image), out)
}
