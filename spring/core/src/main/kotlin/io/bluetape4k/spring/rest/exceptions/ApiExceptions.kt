package io.bluetape4k.spring.rest.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

abstract class ApiException: RuntimeException {
    abstract val httpStatus: HttpStatus

    constructor(message: String, cause: Throwable? = null): super(message, cause)
    constructor(cause: Throwable?): super(cause)
}

@ResponseStatus(value = HttpStatus.NOT_FOUND)
open class ApiEntityNotFoundException: ApiException {
    override val httpStatus: HttpStatus = HttpStatus.NOT_FOUND

    constructor(message: String, cause: Throwable? = null): super(message, cause)
    constructor(cause: Throwable): this(cause.message ?: "Entity not found", cause)
}

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
open class ApiBadRequestException: ApiException {
    override val httpStatus: HttpStatus = HttpStatus.BAD_REQUEST

    constructor(message: String, cause: Throwable? = null): super(message, cause)
    constructor(cause: Throwable): this(cause.message ?: "Bad request")
}


@ResponseStatus(value = HttpStatus.TOO_MANY_REQUESTS)
open class ApiTooManyRequestsException: ApiException {
    override val httpStatus: HttpStatus = HttpStatus.TOO_MANY_REQUESTS

    constructor(message: String, cause: Throwable? = null): super(message, cause)
    constructor(cause: Throwable): this(cause.message ?: "Too many requests")
}

@ResponseStatus(value = HttpStatus.FORBIDDEN)
open class ApiForbiddenException: ApiException {
    override val httpStatus: HttpStatus = HttpStatus.FORBIDDEN

    constructor(message: String, cause: Throwable? = null): super(message, cause)
    constructor(cause: Throwable): this(cause.message ?: "Forbidden")
}

@ResponseStatus(value = HttpStatus.UNAUTHORIZED)
open class ApiUnauthorizedException: ApiException {
    override val httpStatus: HttpStatus = HttpStatus.UNAUTHORIZED

    constructor(message: String, cause: Throwable? = null): super(message, cause)
    constructor(cause: Throwable): this(cause.message ?: "Unauthorized")
}

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
open class ApiInternalServerErrorException: ApiException {
    override val httpStatus: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR

    constructor(message: String, cause: Throwable? = null): super(message, cause)
    constructor(cause: Throwable): this(cause.message ?: "Internal server error")
}

@ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE)
open class ApiServiceUnavailableException: ApiException {
    override val httpStatus: HttpStatus = HttpStatus.SERVICE_UNAVAILABLE

    constructor(message: String, cause: Throwable? = null): super(message, cause)
    constructor(cause: Throwable): this(cause.message ?: "Service unavailable")
}
