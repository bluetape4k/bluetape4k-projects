# Protobuf Buffer Allocation Evidence

## Scope

Issue #757 allocation evidence generated from the committed delivery manifest.

## Provenance

- Measurement commit: `df1bed0ca5ca00720a7ee79a996d862938751b4e`
- Measurement tree: `751c356c39a06a74b0af6eb15814da8dea4be824`
- Delivery commit: `df1bed0ca5ca00720a7ee79a996d862938751b4e`

## Recorded commands

- `java -jar <PINNED_JAR_SHA256:170730d955cf701de135bbfbf6ce139efb653bfdef7d064f21a01ef2577881e7> -t 1 -f 2 -wi 3 -i 5 -w 1s -r 1s -prof gc -rf json -rff docs/benchmarks/raw/issue-757/run-20260718T193619.552772Z-8889a2f0/jmh.json -jvmArgsAppend -Xms1g -Xmx1g -XX:+UseG1GC`
- `java -jar <PINNED_JAR_SHA256:170730d955cf701de135bbfbf6ce139efb653bfdef7d064f21a01ef2577881e7> -t 1 -f 2 -wi 3 -i 5 -w 1s -r 1s -prof gc -rf json -rff docs/benchmarks/raw/issue-757/run-20260718T194015.848425Z-931b7876/jmh.json -jvmArgsAppend -Xms1g -Xmx1g -XX:+UseG1GC`

## Measurements

| Method | Run | B/op | ops/s | Delta | Verdict | Reason | Claim |
|---|---|---:|---:|---:|---|---|---|
| redissonDecodeCompositeCompatibility | run-20260718T193619.552772Z-8889a2f0 | 8208.0031810235923 | 2230105.6069773682 | n/a | ineligible | compatibility_control | No positive reduction claim |
| redissonDecodeContiguousOptimized | run-20260718T193619.552772Z-8889a2f0 | 5512.0023753781843 | 3051233.2176875076 | -28.22916143112514% | accepted | candidate | measured allocation reduction |
| redissonDecodeCopiedByteArray | run-20260718T193619.552772Z-8889a2f0 | 7680.0027494294818 | 2612574.5858231261 | n/a | ineligible | baseline | No positive reduction claim |
| serializerDecodeByteArray | run-20260718T193619.552772Z-8889a2f0 | 5408.0026555957502 | 2711042.2988752834 | n/a | ineligible | baseline | No positive reduction claim |
| serializerDecodeDirectOptimized | run-20260718T193619.552772Z-8889a2f0 | 7688.0030833417568 | 2310104.3974763611 | 42.15975052058919% | ineligible | removed_after_regression | No positive reduction claim |
| serializerDecodeHeapOptimized | run-20260718T193619.552772Z-8889a2f0 | 7688.0030837551949 | 2302333.6497612679 | 42.15975052823413% | ineligible | removed_after_regression | No positive reduction claim |
| serializerEncodeByteArray | run-20260718T193619.552772Z-8889a2f0 | 6976.0018657906503 | 3987192.8086186931 | n/a | ineligible | baseline | No positive reduction claim |
| serializerEncodeDirectOptimized | run-20260718T193619.552772Z-8889a2f0 | 4832.0020372032623 | 3578944.0864914083 | -30.733934276899593% | accepted | candidate | measured allocation reduction |
| serializerEncodeHeapOptimized | run-20260718T193619.552772Z-8889a2f0 | 4784.0019994698932 | 3622458.4492854187 | -31.422008028266475% | accepted | candidate | measured allocation reduction |
| trustedFallbackDecodeBufferCompatibility | run-20260718T193619.552772Z-8889a2f0 | 5472.0103429957489 | 669992.18514418579 | n/a | ineligible | compatibility_control | No positive reduction claim |
| trustedFallbackDecodeByteArray | run-20260718T193619.552772Z-8889a2f0 | 4864.0097138898464 | 714959.50720903103 | n/a | ineligible | compatibility_control | No positive reduction claim |
| trustedFallbackEncodeBufferCompatibility | run-20260718T193619.552772Z-8889a2f0 | 680.00437454604867 | 1610487.17309367 | n/a | ineligible | compatibility_control | No positive reduction claim |
| trustedFallbackEncodeByteArray | run-20260718T193619.552772Z-8889a2f0 | 656.00434687897689 | 1619689.236059298 | n/a | ineligible | compatibility_control | No positive reduction claim |
| redissonDecodeCompositeCompatibility | run-20260718T194015.848425Z-931b7876 | 8208.0031610706228 | 2252394.6633121828 | n/a | ineligible | compatibility_control | No positive reduction claim |
| redissonDecodeContiguousOptimized | run-20260718T194015.848425Z-931b7876 | 5512.0024370881683 | 2963101.7876539617 | -28.2291611947455% | accepted | candidate | measured allocation reduction |
| redissonDecodeCopiedByteArray | run-20260718T194015.848425Z-931b7876 | 7680.0028101171119 | 2532268.6959403465 | n/a | ineligible | baseline | No positive reduction claim |
| serializerDecodeByteArray | run-20260718T194015.848425Z-931b7876 | 5408.0025969458638 | 2757909.4658573871 | n/a | ineligible | baseline | No positive reduction claim |
| serializerDecodeDirectOptimized | run-20260718T194015.848425Z-931b7876 | 7688.0031269816291 | 2268371.1168481535 | 42.15975286926418% | ineligible | removed_after_regression | No positive reduction claim |
| serializerDecodeHeapOptimized | run-20260718T194015.848425Z-931b7876 | 7688.00328404641 | 2156094.051381968 | 42.15975577356717% | ineligible | removed_after_regression | No positive reduction claim |
| serializerEncodeByteArray | run-20260718T194015.848425Z-931b7876 | 6976.0018683408243 | 3971183.013536158 | n/a | ineligible | baseline | No positive reduction claim |
| serializerEncodeDirectOptimized | run-20260718T194015.848425Z-931b7876 | 4832.0019924988528 | 3667526.0084177861 | -30.733934943052148% | accepted | candidate | measured allocation reduction |
| serializerEncodeHeapOptimized | run-20260718T194015.848425Z-931b7876 | 4784.0019258931961 | 3745935.3984881318 | -31.42200910804765% | accepted | candidate | measured allocation reduction |
| trustedFallbackDecodeBufferCompatibility | run-20260718T194015.848425Z-931b7876 | 5472.0101394775465 | 683827.14706226788 | n/a | ineligible | compatibility_control | No positive reduction claim |
| trustedFallbackDecodeByteArray | run-20260718T194015.848425Z-931b7876 | 4864.0095286567557 | 727671.82457436912 | n/a | ineligible | compatibility_control | No positive reduction claim |
| trustedFallbackEncodeBufferCompatibility | run-20260718T194015.848425Z-931b7876 | 680.00437594150753 | 1609647.7685469375 | n/a | ineligible | compatibility_control | No positive reduction claim |
| trustedFallbackEncodeByteArray | run-20260718T194015.848425Z-931b7876 | 656.00434346063878 | 1622527.7124317368 | n/a | ineligible | compatibility_control | No positive reduction claim |

## Rollback decisions

- `serializer_decode` removed after regression; triggering cells: serializerDecodeDirectOptimized; ineligible removed cells: serializerDecodeDirectOptimized, serializerDecodeHeapOptimized.

## Compatibility controls

Fallback and composite controls remain claim-ineligible and are reported without a positive claim.

## Limitations

JMH GC allocation is environment-sensitive; throughput is diagnostic and not the allocation acceptance criterion.
