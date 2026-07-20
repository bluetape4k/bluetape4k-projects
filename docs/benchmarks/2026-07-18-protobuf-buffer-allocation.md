# Protobuf Buffer Allocation Evidence

## Scope

Issue #757 allocation evidence generated from the committed delivery manifest.

## Provenance

- Measurement commit: `b0166d6dd00e50d636d52d4f74d7eebef2da1945`
- Measurement tree: `f7383b5ea6181f015fffbe9a61b4158c764ddc75`
- Delivery commit: `b0166d6dd00e50d636d52d4f74d7eebef2da1945`

## Recorded commands

- `java -jar <PINNED_JAR_SHA256:df01ceb32ce7cf51e0efbdabf94dc8920d09e568d95ed0a16b075658f9a58484> -t 1 -f 2 -wi 3 -i 5 -w 1s -r 1s -prof gc -rf json -rff docs/benchmarks/raw/issue-757/canonical-a/jmh.json -jvmArgsAppend -Xms1g -Xmx1g -XX:+UseG1GC`
- `java -jar <PINNED_JAR_SHA256:df01ceb32ce7cf51e0efbdabf94dc8920d09e568d95ed0a16b075658f9a58484> -t 1 -f 2 -wi 3 -i 5 -w 1s -r 1s -prof gc -rf json -rff docs/benchmarks/raw/issue-757/canonical-b/jmh.json -jvmArgsAppend -Xms1g -Xmx1g -XX:+UseG1GC`

## Measurements

| Method | Run | B/op | ops/s | Delta | Verdict | Reason | Claim |
|---|---|---:|---:|---:|---|---|---|
| lettuceEncodeDirectCopied | canonical-a | 6888.0022077091198 | 3361313.536135593 | n/a | ineligible | baseline | No positive reduction claim |
| lettuceEncodeDirectOptimized | canonical-a | 4904.0016411301785 | 4472686.0566680608 | -28.803715602158608% | accepted | candidate | measured allocation reduction |
| lettuceEncodeHeapCopied | canonical-a | 6888.0023141260335 | 3136338.6069624377 | n/a | ineligible | baseline | No positive reduction claim |
| lettuceEncodeHeapOptimized | canonical-a | 4904.0017881045915 | 4199388.5130471839 | -28.80371456833892% | accepted | candidate | measured allocation reduction |
| redissonDecodeCompositeCompatibility | canonical-a | 7984.0032628536082 | 2192802.6875351137 | n/a | ineligible | compatibility_control | No positive reduction claim |
| redissonDecodeContiguousOptimized | canonical-a | 5264.0024656149608 | 2954185.5010888996 | -29.77587591824627% | accepted | candidate | measured allocation reduction |
| redissonDecodeCopiedByteArray | canonical-a | 7496.0030252377355 | 2381381.5499590943 | n/a | ineligible | baseline | No positive reduction claim |
| serializerDecodeByteArray | canonical-a | 5232.0023987497025 | 3047700.4096576883 | n/a | ineligible | baseline | No positive reduction claim |
| serializerDecodeDirectOptimized | canonical-a | 7528.2542003091612 | 2414244.6990463426 | 43.88858464797716% | ineligible | removed_after_regression | No positive reduction claim |
| serializerDecodeHeapOptimized | canonical-a | 7512.0030642548518 | 2352337.486309465 | 43.57797439179316% | ineligible | removed_after_regression | No positive reduction claim |
| serializerEncodeByteArray | canonical-a | 6888.0020000056293 | 3629178.8669064147 | n/a | ineligible | baseline | No positive reduction claim |
| serializerEncodeDirectOptimized | canonical-a | 4608.0016075101348 | 4573539.2585487599 | -33.10104138317078% | accepted | candidate | measured allocation reduction |
| serializerEncodeHeapOptimized | canonical-a | 4608.0017807231252 | 4185961.3739894582 | -33.10103886846492% | accepted | candidate | measured allocation reduction |
| trustedFallbackDecodeBufferCompatibility | canonical-a | 5440.0120717753389 | 577410.3716755677 | n/a | ineligible | compatibility_control | No positive reduction claim |
| trustedFallbackDecodeByteArray | canonical-a | 4832.01156307357 | 604545.36885234865 | n/a | ineligible | compatibility_control | No positive reduction claim |
| trustedFallbackEncodeBufferCompatibility | canonical-a | 656.00484403984888 | 1464427.8528835326 | n/a | ineligible | compatibility_control | No positive reduction claim |
| trustedFallbackEncodeByteArray | canonical-a | 656.00477730245166 | 1468442.2611277709 | n/a | ineligible | compatibility_control | No positive reduction claim |
| lettuceEncodeDirectCopied | canonical-b | 6888.0022031805911 | 3363857.96867088 | n/a | ineligible | baseline | No positive reduction claim |
| lettuceEncodeDirectOptimized | canonical-b | 4904.0016309886032 | 4484617.6493494269 | -28.80371570258586% | accepted | candidate | measured allocation reduction |
| lettuceEncodeHeapCopied | canonical-b | 6900.0023082067364 | 3136200.9743110342 | n/a | ineligible | baseline | No positive reduction claim |
| lettuceEncodeHeapOptimized | canonical-b | 4904.0017847892759 | 4204641.9904903676 | -28.927534140727083% | accepted | candidate | measured allocation reduction |
| redissonDecodeCompositeCompatibility | canonical-b | 7984.0032964447091 | 2173589.1646821112 | n/a | ineligible | compatibility_control | No positive reduction claim |
| redissonDecodeContiguousOptimized | canonical-b | 5264.0024729244142 | 2927634.0396610615 | -29.77587538755605% | accepted | candidate | measured allocation reduction |
| redissonDecodeCopiedByteArray | canonical-b | 7496.0029789984956 | 2412587.9153042538 | n/a | ineligible | baseline | No positive reduction claim |
| serializerDecodeByteArray | canonical-b | 5232.4926357837649 | 3061203.5604348499 | n/a | ineligible | baseline | No positive reduction claim |
| serializerDecodeDirectOptimized | canonical-b | 7528.2806896951024 | 2409559.3842003224 | 43.87560984244856% | ineligible | removed_after_regression | No positive reduction claim |
| serializerDecodeHeapOptimized | canonical-b | 7512.0030837091135 | 2337901.7518928973 | 43.5645228114861% | ineligible | removed_after_regression | No positive reduction claim |
| serializerEncodeByteArray | canonical-b | 6888.0020408936107 | 3591970.6536042257 | n/a | ineligible | baseline | No positive reduction claim |
| serializerEncodeDirectOptimized | canonical-b | 4608.0016245395163 | 4514172.1357707391 | -33.10104153305826% | accepted | candidate | measured allocation reduction |
| serializerEncodeHeapOptimized | canonical-b | 4620.0018361395432 | 4084644.4322161758 | -32.926822484794585% | accepted | candidate | measured allocation reduction |
| trustedFallbackDecodeBufferCompatibility | canonical-b | 5440.0122221852598 | 570482.5749631105 | n/a | ineligible | compatibility_control | No positive reduction claim |
| trustedFallbackDecodeByteArray | canonical-b | 4832.0114376902238 | 602434.80048251175 | n/a | ineligible | compatibility_control | No positive reduction claim |
| trustedFallbackEncodeBufferCompatibility | canonical-b | 656.0048888290064 | 1447964.6492407662 | n/a | ineligible | compatibility_control | No positive reduction claim |
| trustedFallbackEncodeByteArray | canonical-b | 656.00474923839067 | 1481717.1410418458 | n/a | ineligible | compatibility_control | No positive reduction claim |

## Rollback decisions

- `serializer_decode` removed after regression; triggering cells: serializerDecodeDirectOptimized; ineligible removed cells: serializerDecodeDirectOptimized, serializerDecodeHeapOptimized.

## Compatibility controls

Fallback and composite controls remain claim-ineligible and are reported without a positive claim.

## Limitations

JMH GC allocation is environment-sensitive; throughput is diagnostic and not the allocation acceptance criterion.
