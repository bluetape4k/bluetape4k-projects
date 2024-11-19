package io.bluetape4k.http.hc5.http

import org.apache.hc.core5.http.ContentType

/**
 * 추가 [ContentType]을 제공합니다.
 */
object ContentTypes {

    /**
     * `text/plain; charset=UTF-8` [ContentType]
     */
    @JvmField
    val TEXT_PLAIN_UTF8: ContentType = ContentType.create("text/plain", Charsets.UTF_8)

}
