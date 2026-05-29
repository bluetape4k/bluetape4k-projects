# Issue 649 Protobuf Codec Allocation Plan

## Steps

1. Add focused protobuf codec benchmarks to `benchmark/protobuf-codec-benchmark`.
2. Replace the Redisson protobuf encode `ByteArray` wrap with direct `ByteBuf`
   writing.
3. Add round-trip and direct buffer-size regression tests.
4. Run targeted tests, benchmark compile, a short benchmark sample, and diff
   hygiene.
5. Post benchmark evidence on #649 before closing.
