package io.bluetape4k.retrofit2.services

import retrofit2.http.GET
import retrofit2.http.Path
import java.io.Serializable

object DetectTempEmailService {

    const val BASE_URL = "https://open.kickbox.com"

    interface DetectTempEmailApi {
        /**
         * 지정한 email 이 일회용 이메일인지 검증합니다.
         *
         * @param email 검증할 email, email 정보에 `@`, `.`, `-`, `+` 등이 포함되어 있으므로, `encoded=true` 를 해주어야 한다.
         * @return
         */
        @GET("/v1/disposable/{email}")
        suspend fun detect(@Path("email", encoded = true) email: String): EmailResult
    }

    data class EmailResult(
        val disposable: Boolean,
    ): Serializable
}
