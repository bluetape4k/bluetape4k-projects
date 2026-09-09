package io.bluetape4k.measured

/**
 * Issue #1724의 BASE consumer를 컴파일하기 위한 기존 특수 연산자 fixture입니다.
 * 이 파일은 테스트 전용이며 현재 main 소스의 API를 대체하지 않습니다.
 */
@JvmName("binarySizeDivTimeToDataRate")
operator fun Measure<BinarySize>.div(other: Measure<Time>): Measure<DataRate> =
    ((this `in` BinarySize.bytes) / (other `in` Time.seconds)) * DataRate.bytesPerSecond
