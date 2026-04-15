package io.bluetape4k.mockserver.jsonplaceholder.model

import io.bluetape4k.logging.KLogging
import java.io.Serializable

/**
 * jsonplaceholder /posts 엔드포인트의 게시글 데이터 모델.
 *
 * @property id 게시글 ID
 * @property userId 작성자 사용자 ID
 * @property title 게시글 제목
 * @property body 게시글 본문
 */
data class PostRecord(
    val id: Long = 0L,
    val userId: Long = 0L,
    val title: String = "",
    val body: String = "",
) : Serializable {
    companion object : KLogging() {
        private const val serialVersionUID = 1L
    }
}
