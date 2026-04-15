package io.bluetape4k.mockserver.jsonplaceholder.model

import io.bluetape4k.logging.KLogging
import java.io.Serializable

/**
 * jsonplaceholder /photos 엔드포인트의 사진 데이터 모델.
 *
 * @property albumId 연관된 앨범 ID
 * @property id 사진 ID
 * @property title 사진 제목
 * @property url 원본 사진 URL
 * @property thumbnailUrl 썸네일 사진 URL
 */
data class PhotoRecord(
    val albumId: Long = 0L,
    val id: Long = 0L,
    val title: String = "",
    val url: String = "",
    val thumbnailUrl: String = "",
) : Serializable {
    companion object : KLogging() {
        private const val serialVersionUID = 1L
    }
}
