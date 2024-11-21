package io.bluetape4k.examples.cassandra.projection

import org.springframework.beans.factory.annotation.Value

interface CustomerSummary {

    @get:Value("#{target.firstname + ' ' + target.lastname}")
    val firstname: String

}
