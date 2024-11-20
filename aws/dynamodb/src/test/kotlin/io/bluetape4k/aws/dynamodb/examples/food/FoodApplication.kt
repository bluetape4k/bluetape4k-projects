package io.bluetape4k.aws.dynamodb.examples.food

import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FoodApplication

fun main(vararg args: String) {
    runApplication<FoodApplication>(*args) {
        webApplicationType = WebApplicationType.REACTIVE
    }
}
