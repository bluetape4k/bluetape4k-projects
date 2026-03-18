package io.bluetape4k.spring.retrofit2.services.httpbin

import io.bluetape4k.LibraryName
import io.bluetape4k.spring.retrofit2.Retrofit2Client
import retrofit2.http.GET
import java.io.Serializable

data class IpAddress(val origin: String): Serializable

@Retrofit2Client(name = "httpbin", baseUrl = "\${$LibraryName.retrofit2.services.httpbin}")
interface HttpbinApi {

    @GET("/httpbin/ip")
    suspend fun getLocalIpAddress(): IpAddress
}
