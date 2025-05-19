package io.bluetape4k.idgenerators.snowflake

import io.bluetape4k.logging.coroutines.KLoggingChannel

class DefaultSnowflakeTest: AbstractSnowflakeTest() {

    companion object: KLoggingChannel()

    override val snowflake: Snowflake = DefaultSnowflake()

}
