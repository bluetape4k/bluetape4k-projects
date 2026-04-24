package io.bluetape4k.mockserver.jsonplaceholder.model

import io.bluetape4k.logging.KLogging
import java.io.Serializable

/**
 * jsonplaceholder /todos 엔드포인트의 할 일 데이터 모델.
 *
 * @property userId 사용자 ID
 * @property id 할 일 ID
 * @property title 할 일 제목
 * @property completed 완료 여부
 */
data class TodoRecord(
    val userId: Long = 0L,
    val id: Long = 0L,
    val title: String = "",
    val completed: Boolean = false,
) : Serializable {
    companion object : KLogging() {
        private const val serialVersionUID = 1L
    }
}
