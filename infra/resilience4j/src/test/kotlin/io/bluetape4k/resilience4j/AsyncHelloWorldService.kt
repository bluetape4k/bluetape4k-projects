package io.bluetape4k.resilience4j

import java.util.concurrent.CompletionStage

interface AsyncHelloWorldService {

    fun returnHelloWorld(): CompletionStage<String>
    fun returnHelloWorldWithName(name: String): CompletionStage<String>

    fun sayHelloWorld(): CompletionStage<Void?>
    fun sayHelloWorldWithName(name: String): CompletionStage<Void?>

}
