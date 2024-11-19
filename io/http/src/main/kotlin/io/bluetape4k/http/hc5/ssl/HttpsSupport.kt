package io.bluetape4k.http.hc5.ssl

import org.apache.hc.client5.http.ssl.HttpsSupport
import javax.net.ssl.HostnameVerifier

/**
 * Default [HostnameVerifier] for HTTPS.
 */
val defaultHostnameVerifier: HostnameVerifier by lazy {
    HttpsSupport.getDefaultHostnameVerifier()
}
