package io.bluetape4k.io.artifact

/** 공용 artifact 경로 또는 원자적 쓰기 신뢰 경계를 통과하지 못했을 때 사용하는 예외입니다. */
class ArtifactPrimitiveException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)
