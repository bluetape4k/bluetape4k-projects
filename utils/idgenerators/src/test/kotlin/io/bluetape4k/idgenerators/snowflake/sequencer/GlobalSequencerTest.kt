package io.bluetape4k.idgenerators.snowflake.sequencer

class GlobalSequencerTest: AbstractSequencerTest() {

    override val sequencer: Sequencer = GlobalSequencer()
}
