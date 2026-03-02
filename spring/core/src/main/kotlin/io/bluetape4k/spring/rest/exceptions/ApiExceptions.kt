package io.bluetape4k.spring.rest.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/**
 * API 계열 예외의 공통 기반 타입입니다.
 *
 * ## 동작/계약
 * - 모든 하위 예외는 [httpStatus]를 통해 HTTP 상태를 노출합니다.
 * - [RuntimeException]을 상속하므로 체크 예외가 아닙니다.
 *
 * ```kotlin
 * val ex: ApiException = ApiBadRequestException("invalid")
 * // ex.httpStatus == HttpStatus.BAD_REQUEST
 * ```
 */
abstract class ApiException: RuntimeException {
    /**
     * 예외와 매핑되는 HTTP 상태 코드입니다.
     *
     * ## 동작/계약
     * - 하위 클래스에서 상태 코드를 고정해 제공합니다.
     *
     * ```kotlin
     * val status = ApiUnauthorizedException("no auth").httpStatus
     * // status == HttpStatus.UNAUTHORIZED
     * ```
     */
    abstract val httpStatus: HttpStatus

    /**
     * 메시지와 원인 예외를 지정해 예외를 생성합니다.
     *
     * ## 동작/계약
     * - [message], [cause]를 상위 [RuntimeException] 생성자로 전달합니다.
     *
     * ```kotlin
     * val cause = IllegalArgumentException("invalid")
     * val ex = ApiBadRequestException("bad request", cause)
     * // ex.cause === cause
     * ```
     */
    constructor(message: String, cause: Throwable? = null): super(message, cause)

    /**
     * 원인 예외만으로 예외를 생성합니다.
     *
     * ## 동작/계약
     * - [cause]를 상위 [RuntimeException] 생성자로 전달합니다.
     *
     * ```kotlin
     * val cause = IllegalStateException("failed")
     * val ex = ApiInternalServerErrorException(cause)
     * // ex.cause === cause
     * ```
     */
    constructor(cause: Throwable?): super(cause)
}

/**
 * `404 Not Found`에 매핑되는 API 예외입니다.
 *
 * ## 동작/계약
 * - [httpStatus]는 항상 [HttpStatus.NOT_FOUND]입니다.
 *
 * ```kotlin
 * val ex = ApiEntityNotFoundException("entity not found")
 * // ex.httpStatus == HttpStatus.NOT_FOUND
 * ```
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND)
open class ApiEntityNotFoundException: ApiException {
    override val httpStatus: HttpStatus = HttpStatus.NOT_FOUND

    /**
     * 메시지와 원인 예외를 지정해 예외를 생성합니다.
     *
     * ## 동작/계약
     * - 전달받은 [message], [cause]를 그대로 사용합니다.
     *
     * ```kotlin
     * val ex = ApiEntityNotFoundException("missing")
     * // ex.message == "missing"
     * ```
     */
    constructor(message: String, cause: Throwable? = null): super(message, cause)

    /**
     * 원인 예외를 기반으로 예외를 생성합니다.
     *
     * ## 동작/계약
     * - 원인 메시지가 있으면 이를 사용하고, 없으면 `"Entity not found"`를 메시지로 사용합니다.
     *
     * ```kotlin
     * val ex = ApiEntityNotFoundException(IllegalArgumentException("missing"))
     * // ex.message == "missing"
     * ```
     */
    constructor(cause: Throwable): this(cause.message ?: "Entity not found", cause)
}

/**
 * `400 Bad Request`에 매핑되는 API 예외입니다.
 *
 * ## 동작/계약
 * - [httpStatus]는 항상 [HttpStatus.BAD_REQUEST]입니다.
 *
 * ```kotlin
 * val ex = ApiBadRequestException("bad request")
 * // ex.httpStatus == HttpStatus.BAD_REQUEST
 * ```
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
open class ApiBadRequestException: ApiException {
    override val httpStatus: HttpStatus = HttpStatus.BAD_REQUEST

    /**
     * 메시지와 원인 예외를 지정해 예외를 생성합니다.
     *
     * ## 동작/계약
     * - 전달받은 [message], [cause]를 그대로 사용합니다.
     *
     * ```kotlin
     * val ex = ApiBadRequestException("bad")
     * // ex.message == "bad"
     * ```
     */
    constructor(message: String, cause: Throwable? = null): super(message, cause)

    /**
     * 원인 예외를 기반으로 예외를 생성합니다.
     *
     * ## 동작/계약
     * - 원인 메시지가 있으면 이를 사용하고, 없으면 `"Bad request"`를 메시지로 사용합니다.
     *
     * ```kotlin
     * val ex = ApiBadRequestException(IllegalArgumentException("wrong format"))
     * // ex.message == "wrong format"
     * ```
     */
    constructor(cause: Throwable): this(cause.message ?: "Bad request")
}


/**
 * `429 Too Many Requests`에 매핑되는 API 예외입니다.
 *
 * ## 동작/계약
 * - [httpStatus]는 항상 [HttpStatus.TOO_MANY_REQUESTS]입니다.
 *
 * ```kotlin
 * val ex = ApiTooManyRequestsException("too many")
 * // ex.httpStatus == HttpStatus.TOO_MANY_REQUESTS
 * ```
 */
@ResponseStatus(value = HttpStatus.TOO_MANY_REQUESTS)
open class ApiTooManyRequestsException: ApiException {
    override val httpStatus: HttpStatus = HttpStatus.TOO_MANY_REQUESTS

    /**
     * 메시지와 원인 예외를 지정해 예외를 생성합니다.
     *
     * ## 동작/계약
     * - 전달받은 [message], [cause]를 그대로 사용합니다.
     *
     * ```kotlin
     * val ex = ApiTooManyRequestsException("retry later")
     * // ex.message == "retry later"
     * ```
     */
    constructor(message: String, cause: Throwable? = null): super(message, cause)

    /**
     * 원인 예외를 기반으로 예외를 생성합니다.
     *
     * ## 동작/계약
     * - 원인 메시지가 있으면 이를 사용하고, 없으면 `"Too many requests"`를 메시지로 사용합니다.
     *
     * ```kotlin
     * val ex = ApiTooManyRequestsException(IllegalStateException("limit exceeded"))
     * // ex.message == "limit exceeded"
     * ```
     */
    constructor(cause: Throwable): this(cause.message ?: "Too many requests")
}

/**
 * `403 Forbidden`에 매핑되는 API 예외입니다.
 *
 * ## 동작/계약
 * - [httpStatus]는 항상 [HttpStatus.FORBIDDEN]입니다.
 *
 * ```kotlin
 * val ex = ApiForbiddenException("forbidden")
 * // ex.httpStatus == HttpStatus.FORBIDDEN
 * ```
 */
@ResponseStatus(value = HttpStatus.FORBIDDEN)
open class ApiForbiddenException: ApiException {
    override val httpStatus: HttpStatus = HttpStatus.FORBIDDEN

    /**
     * 메시지와 원인 예외를 지정해 예외를 생성합니다.
     *
     * ## 동작/계약
     * - 전달받은 [message], [cause]를 그대로 사용합니다.
     *
     * ```kotlin
     * val ex = ApiForbiddenException("denied")
     * // ex.message == "denied"
     * ```
     */
    constructor(message: String, cause: Throwable? = null): super(message, cause)

    /**
     * 원인 예외를 기반으로 예외를 생성합니다.
     *
     * ## 동작/계약
     * - 원인 메시지가 있으면 이를 사용하고, 없으면 `"Forbidden"`을 메시지로 사용합니다.
     *
     * ```kotlin
     * val ex = ApiForbiddenException(IllegalAccessException("not allowed"))
     * // ex.message == "not allowed"
     * ```
     */
    constructor(cause: Throwable): this(cause.message ?: "Forbidden")
}

/**
 * `401 Unauthorized`에 매핑되는 API 예외입니다.
 *
 * ## 동작/계약
 * - [httpStatus]는 항상 [HttpStatus.UNAUTHORIZED]입니다.
 *
 * ```kotlin
 * val ex = ApiUnauthorizedException("unauthorized")
 * // ex.httpStatus == HttpStatus.UNAUTHORIZED
 * ```
 */
@ResponseStatus(value = HttpStatus.UNAUTHORIZED)
open class ApiUnauthorizedException: ApiException {
    override val httpStatus: HttpStatus = HttpStatus.UNAUTHORIZED

    /**
     * 메시지와 원인 예외를 지정해 예외를 생성합니다.
     *
     * ## 동작/계약
     * - 전달받은 [message], [cause]를 그대로 사용합니다.
     *
     * ```kotlin
     * val ex = ApiUnauthorizedException("no token")
     * // ex.message == "no token"
     * ```
     */
    constructor(message: String, cause: Throwable? = null): super(message, cause)

    /**
     * 원인 예외를 기반으로 예외를 생성합니다.
     *
     * ## 동작/계약
     * - 원인 메시지가 있으면 이를 사용하고, 없으면 `"Unauthorized"`를 메시지로 사용합니다.
     *
     * ```kotlin
     * val ex = ApiUnauthorizedException(IllegalStateException("expired"))
     * // ex.message == "expired"
     * ```
     */
    constructor(cause: Throwable): this(cause.message ?: "Unauthorized")
}

/**
 * `500 Internal Server Error`에 매핑되는 API 예외입니다.
 *
 * ## 동작/계약
 * - [httpStatus]는 항상 [HttpStatus.INTERNAL_SERVER_ERROR]입니다.
 *
 * ```kotlin
 * val ex = ApiInternalServerErrorException("failed")
 * // ex.httpStatus == HttpStatus.INTERNAL_SERVER_ERROR
 * ```
 */
@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
open class ApiInternalServerErrorException: ApiException {
    override val httpStatus: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR

    /**
     * 메시지와 원인 예외를 지정해 예외를 생성합니다.
     *
     * ## 동작/계약
     * - 전달받은 [message], [cause]를 그대로 사용합니다.
     *
     * ```kotlin
     * val ex = ApiInternalServerErrorException("internal")
     * // ex.message == "internal"
     * ```
     */
    constructor(message: String, cause: Throwable? = null): super(message, cause)

    /**
     * 원인 예외를 기반으로 예외를 생성합니다.
     *
     * ## 동작/계약
     * - 원인 메시지가 있으면 이를 사용하고, 없으면 `"Internal server error"`를 메시지로 사용합니다.
     *
     * ```kotlin
     * val ex = ApiInternalServerErrorException(IllegalStateException("db down"))
     * // ex.message == "db down"
     * ```
     */
    constructor(cause: Throwable): this(cause.message ?: "Internal server error")
}

/**
 * `503 Service Unavailable`에 매핑되는 API 예외입니다.
 *
 * ## 동작/계약
 * - [httpStatus]는 항상 [HttpStatus.SERVICE_UNAVAILABLE]입니다.
 *
 * ```kotlin
 * val ex = ApiServiceUnavailableException("maintenance")
 * // ex.httpStatus == HttpStatus.SERVICE_UNAVAILABLE
 * ```
 */
@ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE)
open class ApiServiceUnavailableException: ApiException {
    override val httpStatus: HttpStatus = HttpStatus.SERVICE_UNAVAILABLE

    /**
     * 메시지와 원인 예외를 지정해 예외를 생성합니다.
     *
     * ## 동작/계약
     * - 전달받은 [message], [cause]를 그대로 사용합니다.
     *
     * ```kotlin
     * val ex = ApiServiceUnavailableException("maintenance")
     * // ex.message == "maintenance"
     * ```
     */
    constructor(message: String, cause: Throwable? = null): super(message, cause)

    /**
     * 원인 예외를 기반으로 예외를 생성합니다.
     *
     * ## 동작/계약
     * - 원인 메시지가 있으면 이를 사용하고, 없으면 `"Service unavailable"`을 메시지로 사용합니다.
     *
     * ```kotlin
     * val ex = ApiServiceUnavailableException(IllegalStateException("temporarily down"))
     * // ex.message == "temporarily down"
     * ```
     */
    constructor(cause: Throwable): this(cause.message ?: "Service unavailable")
}
