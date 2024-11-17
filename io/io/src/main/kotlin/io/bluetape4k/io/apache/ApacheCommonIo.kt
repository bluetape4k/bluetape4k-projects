package io.bluetape4k.io.apache

/**
 * Apache Commons의 ByteArrayOutputStream은 Buffer Size의 Array를 사용하므로,
 * 대용량인 경우 JDK 기본인 [java.io.ByteArrayOutputStream] 보다 성능이 좋습니다.
 */
typealias ApacheByteArrayOutputStream = org.apache.commons.io.output.ByteArrayOutputStream
