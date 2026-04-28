package untrusted.payload

import java.io.Serializable

// 역직렬화 필터 거부 검증용 페이로드 클래스.
// 이 클래스는 io.bluetape4k.**, java.base module, kotlin.** 허용 목록 밖에 있으므로
// JDK_DEFAULT_OBJECT_INPUT_FILTER 적용 시 역직렬화가 거부되어야 합니다.
// 테스트 전용 클래스입니다. 프로덕션 코드에서 사용하지 마세요.
data class UntrustedPayload(val data: String = "evil") : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
