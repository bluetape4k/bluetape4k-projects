package io.bluetape4k.http.hc5.ssl

import org.apache.hc.client5.http.ssl.HttpsSupport
import javax.net.ssl.HostnameVerifier

/** HTTPS 기본 [HostnameVerifier]입니다. */
val defaultHostnameVerifier: HostnameVerifier by lazy {
    HttpsSupport.getDefaultHostnameVerifier()
}
