package io.bluetape4k.spring.cassandra.query

import org.springframework.data.cassandra.core.query.Query


fun emptyQuery(): Query = Query.empty()
