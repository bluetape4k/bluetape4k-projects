package io.bluetape4k.csv.v2

import io.bluetape4k.csv.internal.CsvLexer
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import java.io.FileInputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.file.Path

internal class FlowCsvReaderImpl(
    override val config: CsvReaderConfig,
) : FlowCsvReader {

    companion object : KLogging()

    override fun read(
        input: InputStream,
        encoding: Charset,
        skipHeaders: Boolean,
    ): Flow<CsvRow> = channelFlow {
        val settings = config.toCsvSettings()
        CsvLexer(input.reader(encoding), settings, skipHeaders).use { lexer ->
            while (lexer.hasNext()) {
                ensureActive()
                send(lexer.next().toCsvRow())
            }
        }
    }.flowOn(Dispatchers.IO)

    override fun readFile(
        path: Path,
        encoding: Charset,
        skipHeaders: Boolean,
    ): Flow<CsvRow> = channelFlow {
        val settings = config.toCsvSettings()
        FileInputStream(path.toFile()).use { fis ->
            CsvLexer(fis.reader(encoding), settings, skipHeaders).use { lexer ->
                while (lexer.hasNext()) {
                    ensureActive()
                    send(lexer.next().toCsvRow())
                }
            }
        }
    }.flowOn(Dispatchers.IO)
}
