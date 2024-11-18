package io.bluetape4k.idgenerators.snowflake

class GlobalSnowflakeTest: AbstractSnowflakeTest() {

    override val snowflake: Snowflake = GlobalSnowflake()

}
