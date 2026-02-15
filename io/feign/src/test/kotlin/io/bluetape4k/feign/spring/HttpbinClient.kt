package io.bluetape4k.feign.spring

import io.bluetape4k.feign.services.HttpbinAnythingResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod

@FeignClient(
    name = "httpbin",
    url = "\${test.feign.httpbin-url}",
    configuration = [HttpbinClientConfiguration::class]
)
interface HttpbinClient {

    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/anything/posts"]
    )
    fun posts(): HttpbinAnythingResponse

    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/anything/post/{id}/comments"]
    )
    fun getPostComments(@PathVariable("id") postId: Int): HttpbinAnythingResponse

}
