package io.bluetape4k.javers.codecs

import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.support.unsafeLazy


object GsonCodecs {

    val Default by unsafeLazy { String }

    // String Codecs

    val String by unsafeLazy { StringGsonCodec() }

    val GZipString by unsafeLazy { CompressableStringGsonCodec(String, Compressors.GZip) }
    val DeflateString by unsafeLazy { CompressableStringGsonCodec(String, Compressors.Deflate) }
    val LZ4String by unsafeLazy { CompressableStringGsonCodec(String, Compressors.LZ4) }
    val SnappyString by unsafeLazy { CompressableStringGsonCodec(String, Compressors.Snappy) }
    val ZstdString by unsafeLazy { CompressableStringGsonCodec(String, Compressors.Zstd) }

    // Binary Codecs

    val Kryo by unsafeLazy { BinaryGsonCodec(BinarySerializers.Kryo) }

    val GZipKryo by unsafeLazy { CompressableBinaryGsonCodec(Kryo, Compressors.GZip) }
    val DeflateKryo by unsafeLazy { CompressableBinaryGsonCodec(Kryo, Compressors.Deflate) }
    val LZ4Kryo by unsafeLazy { CompressableBinaryGsonCodec(Kryo, Compressors.LZ4) }
    val SnappyKryo by unsafeLazy { CompressableBinaryGsonCodec(Kryo, Compressors.Snappy) }
    val ZstdKryo by unsafeLazy { CompressableBinaryGsonCodec(Kryo, Compressors.Zstd) }

    // Map
    val Map by unsafeLazy { MapGsonCodec() }
}
