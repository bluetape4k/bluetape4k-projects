package io.bluetape4k.http.hc5.http

import org.apache.hc.core5.http.ContentType

/** 자주 사용하는 추가 [ContentType] 모음을 제공합니다. */
object ContentTypes {

    /**
     * `text/plain; charset=UTF-8` [ContentType]
     *
     * ```kotlin
     * val entity = StringEntity("hello", ContentTypes.TEXT_PLAIN_UTF8)
     * // entity.contentType == "text/plain; charset=UTF-8"
     * ```
     */
    @JvmField
    val TEXT_PLAIN_UTF8: ContentType = ContentType.create("text/plain", Charsets.UTF_8)

}
