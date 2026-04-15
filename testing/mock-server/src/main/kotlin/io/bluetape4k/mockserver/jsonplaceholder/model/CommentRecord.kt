package io.bluetape4k.mockserver.jsonplaceholder.model

import io.bluetape4k.logging.KLogging
import java.io.Serializable

/**
 * jsonplaceholder /comments 엔드포인트의 댓글 데이터 모델.
 *
 * @property postId 연관된 게시글 ID
 * @property id 댓글 ID
 * @property name 댓글 작성자 이름
 * @property email 댓글 작성자 이메일
 * @property body 댓글 본문
 */
data class CommentRecord(
    val postId: Long = 0L,
    val id: Long = 0L,
    val name: String = "",
    val email: String = "",
    val body: String = "",
) : Serializable {
    companion object : KLogging() {
        private const val serialVersionUID = 1L
    }
}
