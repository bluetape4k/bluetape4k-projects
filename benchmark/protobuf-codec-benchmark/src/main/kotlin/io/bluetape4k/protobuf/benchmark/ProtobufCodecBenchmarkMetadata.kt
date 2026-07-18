package io.bluetape4k.protobuf.benchmark

object ProtobufCodecBenchmarkMetadata {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.contentEquals(arrayOf("--json")))
        val stdout = System.out
        val fixture = try {
            System.setOut(System.err)
            ProtobufCodecBenchmarkFixture().also { it.validate() }
        } finally {
            System.setOut(stdout)
        }
        println(
            buildString {
                append("{\"config_json\":").append(jsonString(fixture.configIdentity))
                append(",\"config_sha256\":").append(jsonString(fixture.configSha256))
                append(",\"matrix_version\":").append(jsonString(ProtobufBenchmarkMatrix.VERSION))
                append(",\"payload_sha256\":").append(jsonString(fixture.payloadSha256))
                append(",\"payload_size\":").append(fixture.wireSize)
                append(",\"schema_version\":1")
                append(",\"target_headroom\":").append(ProtobufBenchmarkMatrix.TARGET_HEADROOM)
                append(",\"target_start\":").append(ProtobufBenchmarkMatrix.TARGET_START)
                append('}')
            }
        )
    }
}
