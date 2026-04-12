package io.bluetape4k.batch.benchmark.support

import java.nio.file.Paths

/**
 * benchmark 문서 생성기의 진입점입니다.
 */
fun main(args: Array<String>) {
    require(args.isNotEmpty()) { "projectDir 인자가 필요합니다." }
    val projectDir = Paths.get(args[0])
    val reportDir = args.getOrNull(1)?.let { Paths.get(it) }
    BenchmarkMarkdownExporter.writeAll(projectDir, reportDir)
}
