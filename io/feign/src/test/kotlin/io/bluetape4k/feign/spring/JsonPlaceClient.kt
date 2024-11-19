package io.bluetape4k.feign.spring

import io.bluetape4k.feign.services.Comment
import io.bluetape4k.feign.services.Post
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod

@FeignClient(
    name = "jsonplace",
    url = "https://jsonplaceholder.typicode.com",
    configuration = [JsonPlaceClientConfiguration::class]
)
interface JsonPlaceClient {

    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/posts"]
    )
    fun posts(): List<Post>

    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/post/{id}/comments"]
    )
    fun getPostComments(@PathVariable("id") postId: Int): List<Comment>

}
