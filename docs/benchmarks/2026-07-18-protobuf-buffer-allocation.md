# Protobuf Buffer Allocation Evidence

## Scope

Issue #757 allocation evidence generated from the committed delivery manifest.

## Provenance

- Measurement commit: `22b155fbed160e95a259a7c6695620139bbedda8`
- Measurement tree: `7a68ec6eff87f4e35446fa5096655e9822141a9f`
- Delivery commit: `22b155fbed160e95a259a7c6695620139bbedda8`

## Recorded commands

- `java -jar <PINNED_JAR_SHA256:e8731752bf7fb3177f4069552f74a209d152b7df38980bd80bc4e09ab0797042> -t 1 -f 2 -wi 3 -i 5 -w 1s -r 1s -prof gc -rf json -rff docs/benchmarks/raw/issue-757/run-20260718T210358.790209Z-d5bbb3c1/jmh.json -jvmArgsAppend -Xms1g -Xmx1g -XX:+UseG1GC`
- `java -jar <PINNED_JAR_SHA256:e8731752bf7fb3177f4069552f74a209d152b7df38980bd80bc4e09ab0797042> -t 1 -f 2 -wi 3 -i 5 -w 1s -r 1s -prof gc -rf json -rff docs/benchmarks/raw/issue-757/run-20260718T210757.236799Z-cd971cfd/jmh.json -jvmArgsAppend -Xms1g -Xmx1g -XX:+UseG1GC`

## Measurements

| Method | Run | B/op | ops/s | Delta | Verdict | Reason | Claim |
|---|---|---:|---:|---:|---|---|---|
| redissonDecodeCompositeCompatibility | run-20260718T210358.790209Z-d5bbb3c1 | 8208.0031049678437 | 2283179.0280207801 | n/a | ineligible | compatibility_control | No positive reduction claim |
| redissonDecodeContiguousOptimized | run-20260718T210358.790209Z-d5bbb3c1 | 5512.0023462130948 | 3073395.6491128579 | -28.22916174984595% | accepted | candidate | measured allocation reduction |
| redissonDecodeCopiedByteArray | run-20260718T210358.790209Z-d5bbb3c1 | 7680.002742898525 | 2604907.9583846861 | n/a | ineligible | baseline | No positive reduction claim |
| serializerDecodeByteArray | run-20260718T210358.790209Z-d5bbb3c1 | 5408.0025761987254 | 2783449.5162272095 | n/a | ineligible | baseline | No positive reduction claim |
| serializerDecodeDirectOptimized | run-20260718T210358.790209Z-d5bbb3c1 | 7688.003117985967 | 2283010.4273878522 | 42.159753248302806% | ineligible | removed_after_regression | No positive reduction claim |
| serializerDecodeHeapOptimized | run-20260718T210358.790209Z-d5bbb3c1 | 7688.0030958634916 | 2287434.3632573308 | 42.15975283923356% | ineligible | removed_after_regression | No positive reduction claim |
| serializerEncodeByteArray | run-20260718T210358.790209Z-d5bbb3c1 | 6976.0018440765562 | 4037088.3628994948 | n/a | ineligible | baseline | No positive reduction claim |
| serializerEncodeDirectOptimized | run-20260718T210358.790209Z-d5bbb3c1 | 4832.001960110777 | 3716265.0362028591 | -30.733935166406052% | accepted | candidate | measured allocation reduction |
| serializerEncodeHeapOptimized | run-20260718T210358.790209Z-d5bbb3c1 | 4784.0019137350764 | 3764966.881722345 | -31.422009043801285% | accepted | candidate | measured allocation reduction |
| trustedFallbackDecodeBufferCompatibility | run-20260718T210358.790209Z-d5bbb3c1 | 5472.0100123672364 | 693230.26668167487 | n/a | ineligible | compatibility_control | No positive reduction claim |
| trustedFallbackDecodeByteArray | run-20260718T210358.790209Z-d5bbb3c1 | 4864.0094682211939 | 732427.46314681636 | n/a | ineligible | compatibility_control | No positive reduction claim |
| trustedFallbackEncodeBufferCompatibility | run-20260718T210358.790209Z-d5bbb3c1 | 680.00433304952855 | 1627353.9903959902 | n/a | ineligible | compatibility_control | No positive reduction claim |
| trustedFallbackEncodeByteArray | run-20260718T210358.790209Z-d5bbb3c1 | 656.00441518770856 | 1596987.6895687275 | n/a | ineligible | compatibility_control | No positive reduction claim |
| redissonDecodeCompositeCompatibility | run-20260718T210757.236799Z-cd971cfd | 8208.0031728151444 | 2236728.9421896366 | n/a | ineligible | compatibility_control | No positive reduction claim |
| redissonDecodeContiguousOptimized | run-20260718T210757.236799Z-cd971cfd | 5512.0022908040173 | 3146780.2833217178 | -28.2291619975462% | accepted | candidate | measured allocation reduction |
| redissonDecodeCopiedByteArray | run-20260718T210757.236799Z-cd971cfd | 7680.0026922014831 | 2671363.1397229671 | n/a | ineligible | baseline | No positive reduction claim |
| serializerDecodeByteArray | run-20260718T210757.236799Z-cd971cfd | 5408.002677471738 | 2675586.5937079093 | n/a | ineligible | baseline | No positive reduction claim |
| serializerDecodeDirectOptimized | run-20260718T210757.236799Z-cd971cfd | 7688.0031275427409 | 2261753.6392862187 | 42.15975076286227% | ineligible | removed_after_regression | No positive reduction claim |
| serializerDecodeHeapOptimized | run-20260718T210757.236799Z-cd971cfd | 7688.0030942355388 | 2286784.2542860331 | 42.159750146974964% | ineligible | removed_after_regression | No positive reduction claim |
| serializerEncodeByteArray | run-20260718T210757.236799Z-cd971cfd | 6976.0018241986836 | 4083784.7282593446 | n/a | ineligible | baseline | No positive reduction claim |
| serializerEncodeDirectOptimized | run-20260718T210757.236799Z-cd971cfd | 4832.0019865004315 | 3676905.182693834 | -30.733934590742862% | accepted | candidate | measured allocation reduction |
| serializerEncodeHeapOptimized | run-20260718T210757.236799Z-cd971cfd | 4784.0019608610555 | 3698988.5416742042 | -31.422008172846454% | accepted | candidate | measured allocation reduction |
| trustedFallbackDecodeBufferCompatibility | run-20260718T210757.236799Z-cd971cfd | 5472.010264150962 | 675648.9655588707 | n/a | ineligible | compatibility_control | No positive reduction claim |
| trustedFallbackDecodeByteArray | run-20260718T210757.236799Z-cd971cfd | 4864.0095431904992 | 726909.34922423318 | n/a | ineligible | compatibility_control | No positive reduction claim |
| trustedFallbackEncodeBufferCompatibility | run-20260718T210757.236799Z-cd971cfd | 680.00429845876704 | 1639566.574601402 | n/a | ineligible | compatibility_control | No positive reduction claim |
| trustedFallbackEncodeByteArray | run-20260718T210757.236799Z-cd971cfd | 656.00438386558778 | 1610014.2777594437 | n/a | ineligible | compatibility_control | No positive reduction claim |

## Rollback decisions

- `serializer_decode` removed after regression; triggering cells: serializerDecodeDirectOptimized; ineligible removed cells: serializerDecodeDirectOptimized, serializerDecodeHeapOptimized.

## Compatibility controls

Fallback and composite controls remain claim-ineligible and are reported without a positive claim.

## Limitations

JMH GC allocation is environment-sensitive; throughput is diagnostic and not the allocation acceptance criterion.
