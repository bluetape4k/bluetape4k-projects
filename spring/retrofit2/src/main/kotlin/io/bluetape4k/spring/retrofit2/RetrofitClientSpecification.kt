package io.bluetape4k.spring.retrofit2

import org.springframework.cloud.context.named.NamedContextFactory

class RetrofitClientSpecification(
    private val name: String,
    private val configs: Array<Class<*>>,
): NamedContextFactory.Specification {

    override fun getName(): String = name

    override fun getConfiguration(): Array<Class<*>> = configs

}
