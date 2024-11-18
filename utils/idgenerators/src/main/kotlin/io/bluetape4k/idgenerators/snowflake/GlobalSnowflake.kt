package io.bluetape4k.idgenerators.snowflake

import io.bluetape4k.idgenerators.snowflake.sequencer.GlobalSequencer


/**
 * Twitter의 Snowflake 알고리즘을 사용하여 Id 값을 생성합니다.
 * 기존 Snowflake 알고리즘은 machine 별로 1 msec 당 최대 4096개의 Id를 생성할 수 있지만,
 *
 * Global Snowflake 알고리즘은 1 msec 당 4096 * 1024 개의 Id를 생성할 수 있습니다.
 * Global Snowflake는 machineId 구분 없이 id 값을 생성해 주므로, 중앙집중식으로 Id 생성 서비스에서 사용할 수 있습니다.
 *
 * ```
 * val snowflake = GlobalSnowflake()
 *
 * val id = snowflake.nextId()      // generate id
 * val ids = snowflake.nextIds(10)  // generate ten id
 * ```
 */
class GlobalSnowflake: AbstractSnowflake(GlobalSequencer())
