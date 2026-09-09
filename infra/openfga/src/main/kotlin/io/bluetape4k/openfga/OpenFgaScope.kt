package io.bluetape4k.openfga

import io.bluetape4k.support.requireNotBlank
import java.io.Serializable

/**
 * OpenFGA 요청에 사용할 불변 store 및 authorization model 범위입니다.
 *
 * 상위 SDK client가 공유 설정을 변경하는 방식 대신 요청마다 범위를 전달하므로, 여러 store와 authorization
 * model을 동시에 사용하는 호출에서도 설정이 서로 섞이지 않습니다.
 *
 * @property storeId 요청 대상 store ID
 * @property authorizationModelId 검사·일괄 검사·쓰기에서 사용할 authorization model ID
 */
data class OpenFgaScope(
    val storeId: String,
    val authorizationModelId: String? = null,
): Serializable {
    init {
        storeId.requireNotBlank("storeId")
        authorizationModelId?.requireNotBlank("authorizationModelId")
    }

    /**
     * authorization model이 필요한 요청에 사용할 ID를 반환합니다.
     *
     * Read 요청은 authorization model ID를 사용하지 않으므로 model ID를 선택적으로 둘 수 있지만, 검사와 쓰기는
     * 호출 범위를 명시하도록 반드시 model ID를 요구합니다.
     */
    internal fun requireAuthorizationModelId(): String =
        authorizationModelId.requireNotBlank("authorizationModelId")

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}
