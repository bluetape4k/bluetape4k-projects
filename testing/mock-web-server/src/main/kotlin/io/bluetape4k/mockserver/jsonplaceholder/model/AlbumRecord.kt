package io.bluetape4k.mockserver.jsonplaceholder.model

import io.bluetape4k.logging.KLogging
import java.io.Serializable

/**
 * jsonplaceholder /albums 엔드포인트의 앨범 데이터 모델.
 *
 * @property userId 앨범 소유자 사용자 ID
 * @property id 앨범 ID
 * @property title 앨범 제목
 */
data class AlbumRecord(
    val userId: Long = 0L,
    val id: Long = 0L,
    val title: String = "",
) : Serializable {
    companion object : KLogging() {
        private const val serialVersionUID = 1L
    }
}
