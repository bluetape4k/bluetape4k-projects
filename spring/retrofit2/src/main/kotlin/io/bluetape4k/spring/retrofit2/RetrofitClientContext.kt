package io.bluetape4k.spring.retrofit2

import org.springframework.cloud.context.named.NamedContextFactory

/**
 * Retrofit2 Client를 위한 환경설정 정보를 담은 Context
 */
class RetrofitClientContext: NamedContextFactory<RetrofitClientSpecification>(
    DefaultRetrofitClientConfiguration::class.java,
    "retrofit2",
    "retrofit2.client.name"
)
