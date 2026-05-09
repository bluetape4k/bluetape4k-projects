package io.bluetape4k.cache.nearcache

/**
 * back cache GET 실패 시 동작 전략.
 *
 * Resilient NearCache의 get() 호출 시 back cache에서 예외가 발생할 경우의 처리 방식을 정의한다.
 */
enum class GetFailureStrategy {
    /** front cache에 값이 있으면 반환, 없으면 null 반환 (graceful degradation) */
    RETURN_FRONT_OR_NULL,

    /** 예외를 호출자에게 그대로 전파 */
    PROPAGATE_EXCEPTION,
}
