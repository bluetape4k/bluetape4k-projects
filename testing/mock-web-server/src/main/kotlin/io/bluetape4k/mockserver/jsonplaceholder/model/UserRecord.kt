package io.bluetape4k.mockserver.jsonplaceholder.model

import io.bluetape4k.logging.KLogging
import java.io.Serializable

/**
 * jsonplaceholder /users 엔드포인트의 사용자 데이터 모델.
 *
 * address/company 중첩 객체는 평탄화하여 단순 String 필드로 처리한다.
 *
 * @property id 사용자 ID
 * @property name 사용자 전체 이름
 * @property username 사용자 닉네임
 * @property email 이메일 주소
 * @property phone 전화번호
 * @property website 웹사이트 URL
 */
data class UserRecord(
    val id: Long = 0L,
    val name: String = "",
    val username: String = "",
    val email: String = "",
    val phone: String = "",
    val website: String = "",
) : Serializable {
    companion object : KLogging() {
        private const val serialVersionUID = 1L
    }
}
