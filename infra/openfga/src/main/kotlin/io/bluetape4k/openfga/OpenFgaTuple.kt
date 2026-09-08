package io.bluetape4k.openfga

import dev.openfga.sdk.api.model.CheckRequestTupleKey
import dev.openfga.sdk.api.model.ReadRequestTupleKey
import dev.openfga.sdk.api.model.TupleKey
import dev.openfga.sdk.api.model.TupleKeyWithoutCondition
import io.bluetape4k.support.requireNotBlank
import java.io.Serializable

/**
 * OpenFGA 관계 tuple의 필수 식별자입니다.
 *
 * SDK가 생성한 mutable DTO를 애플리케이션 경계에 노출하지 않고, 검증된 값으로 필요한 SDK 요청 타입을
 * 매번 새로 만들 수 있도록 합니다.
 *
 * @property user 관계의 주체
 * @property relation 관계 이름
 * @property objectId 관계의 대상 object
 */
data class OpenFgaTuple(
    val user: String,
    val relation: String,
    val objectId: String,
): Serializable {
    init {
        user.requireNotBlank("user")
        relation.requireNotBlank("relation")
        objectId.requireNotBlank("objectId")
    }

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * 필수 필드를 검증한 불변 OpenFGA tuple을 만듭니다.
 */
fun openFgaTuple(user: String, relation: String, objectId: String): OpenFgaTuple =
    OpenFgaTuple(user, relation, objectId)

/**
 * 검사 요청에 사용할 SDK tuple key를 만듭니다.
 */
fun OpenFgaTuple.toCheckRequestTupleKey(): CheckRequestTupleKey =
    CheckRequestTupleKey()
        .user(user)
        .relation(relation)
        ._object(objectId)

/**
 * Read 요청에 사용할 SDK tuple key를 만듭니다.
 */
fun OpenFgaTuple.toReadRequestTupleKey(): ReadRequestTupleKey =
    ReadRequestTupleKey()
        .user(user)
        .relation(relation)
        ._object(objectId)

/**
 * 쓰기 요청에 사용할 SDK tuple key를 만듭니다.
 */
fun OpenFgaTuple.toTupleKey(): TupleKey =
    TupleKey()
        .user(user)
        .relation(relation)
        ._object(objectId)

/**
 * 삭제 요청에 사용할 조건 없는 SDK tuple key를 만듭니다.
 */
fun OpenFgaTuple.toTupleKeyWithoutCondition(): TupleKeyWithoutCondition =
    TupleKeyWithoutCondition()
        .user(user)
        .relation(relation)
        ._object(objectId)
