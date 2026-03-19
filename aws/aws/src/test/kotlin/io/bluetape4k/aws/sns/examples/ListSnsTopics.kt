package io.bluetape4k.aws.sns.examples

import io.bluetape4k.aws.sns.AbstractSnsTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.junit.jupiter.api.Test

class ListSnsTopics: AbstractSnsTest() {

    companion object: KLogging()

    @Test
    fun `list sns topics`() {
        val topics = client.listTopicsPaginator()

        topics
            .flatMap { it.topics() }
            .forEach { topic ->
                log.debug { "topicArn=${topic.topicArn()}" }
            }
    }
}
