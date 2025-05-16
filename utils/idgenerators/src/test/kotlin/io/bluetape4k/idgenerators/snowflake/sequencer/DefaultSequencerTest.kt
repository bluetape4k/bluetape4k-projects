package io.bluetape4k.idgenerators.snowflake.sequencer

import io.bluetape4k.logging.coroutines.KLoggingChannel

class DefaultSequencerTest: AbstractSequencerTest() {

    companion object: KLoggingChannel()

    override val sequencer: Sequencer = DefaultSequencer()

}
