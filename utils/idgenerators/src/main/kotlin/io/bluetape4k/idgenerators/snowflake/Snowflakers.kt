package io.bluetape4k.idgenerators.snowflake

object Snowflakers {

    /**
     * [DefaultSnowflake] 인스턴스
     */
    val Default: Snowflake by lazy { DefaultSnowflake() }

    /**
     * [GlobalSnowflake] 인스턴스
     */
    val Global: Snowflake by lazy { GlobalSnowflake() }

}
