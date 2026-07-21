# Protobuf Buffer Allocation Evidence

## Scope

Issue #757 allocation evidence generated from the committed delivery manifest.

## Provenance

- Measurement commit: `93d8f39e83f2a6e650a7eb70f5e843b7cd232d66`
- Measurement tree: `deb33ba349af9aa74f0757034c3089aafecf10be`
- Delivery commit: `93d8f39e83f2a6e650a7eb70f5e843b7cd232d66`

## Recorded commands

- `java -jar <PINNED_JAR_SHA256:99a80d85a5d3f5dabd1a504c58fd0abbec2499315e64f81a79c3c0ec7fb94a3c> -t 1 -f 2 -wi 3 -i 5 -w 1s -r 1s -prof gc -rf json -rff docs/benchmarks/raw/issue-757/generations/g-e11070a28a64d8e4fd62dc942b93601d/run-20260720T233154.066805Z-a9ec6e4f/jmh.json -jvmArgsAppend -Xms1g -Xmx1g -XX:+UseG1GC`
- `java -jar <PINNED_JAR_SHA256:99a80d85a5d3f5dabd1a504c58fd0abbec2499315e64f81a79c3c0ec7fb94a3c> -t 1 -f 2 -wi 3 -i 5 -w 1s -r 1s -prof gc -rf json -rff docs/benchmarks/raw/issue-757/generations/g-e11070a28a64d8e4fd62dc942b93601d/run-20260720T233655.039662Z-5ce2db11/jmh.json -jvmArgsAppend -Xms1g -Xmx1g -XX:+UseG1GC`

## Measurements

| Method | Run | B/op | ops/s | Delta | Verdict | Reason | Claim |
|---|---|---:|---:|---:|---|---|---|
| lettuceEncodeDirectCopied | run-20260720T233154.066805Z-a9ec6e4f | 6900.0022003670792 | 3350587.9205365246 | n/a | ineligible | baseline | No positive reduction claim |
| lettuceEncodeDirectOptimized | run-20260720T233154.066805Z-a9ec6e4f | 4904.0016563861818 | 4463394.0178128062 | -28.927534890854247% | accepted | candidate | measured allocation reduction |
| lettuceEncodeHeapCopied | run-20260720T233154.066805Z-a9ec6e4f | 6888.0023229695389 | 3136287.0623713462 | n/a | ineligible | baseline | No positive reduction claim |
| lettuceEncodeHeapOptimized | run-20260720T233154.066805Z-a9ec6e4f | 4904.0018172242389 | 4174093.1231783978 | -28.803714236988853% | accepted | candidate | measured allocation reduction |
| redissonDecodeCompositeCompatibility | run-20260720T233154.066805Z-a9ec6e4f | 7984.0032447905733 | 2196932.1754508973 | n/a | ineligible | compatibility_control | No positive reduction claim |
| redissonDecodeContiguousOptimized | run-20260720T233154.066805Z-a9ec6e4f | 5264.0024592769387 | 2956560.2195750484 | -29.775874944988395% | accepted | candidate | measured allocation reduction |
| redissonDecodeCopiedByteArray | run-20260720T233154.066805Z-a9ec6e4f | 7496.0029123228906 | 2466896.8109747423 | n/a | ineligible | baseline | No positive reduction claim |
| serializerDecodeByteArray | run-20260720T233154.066805Z-a9ec6e4f | 5232.5957882341681 | 3017283.699285551 | n/a | ineligible | baseline | No positive reduction claim |
| serializerDecodeDirectOptimized | run-20260720T233154.066805Z-a9ec6e4f | 7528.2849588899335 | 2402555.8307098188 | 43.872855148065746% | ineligible | removed_after_regression | No positive reduction claim |
| serializerDecodeHeapOptimized | run-20260720T233154.066805Z-a9ec6e4f | 7512.0030943060174 | 2322525.4726126874 | 43.56169286374546% | ineligible | removed_after_regression | No positive reduction claim |
| serializerEncodeByteArray | run-20260720T233154.066805Z-a9ec6e4f | 6888.0020063887669 | 3630477.7753723552 | n/a | ineligible | baseline | No positive reduction claim |
| serializerEncodeDirectOptimized | run-20260720T233154.066805Z-a9ec6e4f | 4608.0016081101476 | 4558791.7802842055 | -33.10104143645531% | accepted | candidate | measured allocation reduction |
| serializerEncodeHeapOptimized | run-20260720T233154.066805Z-a9ec6e4f | 4608.0017793271827 | 4187856.5580059504 | -33.101038950726725% | accepted | candidate | measured allocation reduction |
| trustedFallbackDecodeBufferCompatibility | run-20260720T233154.066805Z-a9ec6e4f | 5468.011971908777 | 582900.71712773445 | n/a | ineligible | compatibility_control | No positive reduction claim |
| trustedFallbackDecodeByteArray | run-20260720T233154.066805Z-a9ec6e4f | 4832.0117057224907 | 596760.95951653796 | n/a | ineligible | compatibility_control | No positive reduction claim |
| trustedFallbackEncodeBufferCompatibility | run-20260720T233154.066805Z-a9ec6e4f | 656.0048780153578 | 1451368.2892984007 | n/a | ineligible | compatibility_control | No positive reduction claim |
| trustedFallbackEncodeByteArray | run-20260720T233154.066805Z-a9ec6e4f | 656.00487843498252 | 1444934.7614547692 | n/a | ineligible | compatibility_control | No positive reduction claim |
| lettuceEncodeDirectCopied | run-20260720T233655.039662Z-5ce2db11 | 6888.0021987828968 | 3368759.6055755196 | n/a | ineligible | baseline | No positive reduction claim |
| lettuceEncodeDirectOptimized | run-20260720T233655.039662Z-5ce2db11 | 4904.0016275751941 | 4454779.8100114483 | -28.80371570668595% | accepted | candidate | measured allocation reduction |
| lettuceEncodeHeapCopied | run-20260720T233655.039662Z-5ce2db11 | 6888.0023197093933 | 3137366.4808200705 | n/a | ineligible | baseline | No positive reduction claim |
| lettuceEncodeHeapOptimized | run-20260720T233655.039662Z-5ce2db11 | 4904.0017835729295 | 4206934.2290637437 | -28.803714691840714% | accepted | candidate | measured allocation reduction |
| redissonDecodeCompositeCompatibility | run-20260720T233655.039662Z-5ce2db11 | 7984.0032867659756 | 2176769.7808497651 | n/a | ineligible | compatibility_control | No positive reduction claim |
| redissonDecodeContiguousOptimized | run-20260720T233655.039662Z-5ce2db11 | 5264.0025371675674 | 2829075.1836949559 | -29.77587479029214% | accepted | candidate | measured allocation reduction |
| redissonDecodeCopiedByteArray | run-20260720T233655.039662Z-5ce2db11 | 7496.0030067271891 | 2392428.7050656867 | n/a | ineligible | baseline | No positive reduction claim |
| serializerDecodeByteArray | run-20260720T233655.039662Z-5ce2db11 | 5232.2599554137823 | 2958968.6055422169 | n/a | ineligible | baseline | No positive reduction claim |
| serializerDecodeDirectOptimized | run-20260720T233655.039662Z-5ce2db11 | 7528.2729971284107 | 2370596.0473045097 | 43.88186101760789% | ineligible | removed_after_regression | No positive reduction claim |
| serializerDecodeHeapOptimized | run-20260720T233655.039662Z-5ce2db11 | 7512.0030603454743 | 2356713.3060476738 | 43.5709067278444% | ineligible | removed_after_regression | No positive reduction claim |
| serializerEncodeByteArray | run-20260720T233655.039662Z-5ce2db11 | 6888.0020240346203 | 3600765.1125516309 | n/a | ineligible | baseline | No positive reduction claim |
| serializerEncodeDirectOptimized | run-20260720T233655.039662Z-5ce2db11 | 4608.0016121318222 | 4543893.8601842457 | -33.101041549452056% | accepted | candidate | measured allocation reduction |
| serializerEncodeHeapOptimized | run-20260720T233655.039662Z-5ce2db11 | 4608.0017607342916 | 4218541.0481231529 | -33.10103939204169% | accepted | candidate | measured allocation reduction |
| trustedFallbackDecodeBufferCompatibility | run-20260720T233655.039662Z-5ce2db11 | 5468.0121535715107 | 573711.96194983553 | n/a | ineligible | compatibility_control | No positive reduction claim |
| trustedFallbackDecodeByteArray | run-20260720T233655.039662Z-5ce2db11 | 4832.0116267698695 | 601575.52423516556 | n/a | ineligible | compatibility_control | No positive reduction claim |
| trustedFallbackEncodeBufferCompatibility | run-20260720T233655.039662Z-5ce2db11 | 656.00494568671752 | 1450352.4625205498 | n/a | ineligible | compatibility_control | No positive reduction claim |
| trustedFallbackEncodeByteArray | run-20260720T233655.039662Z-5ce2db11 | 656.00475193762554 | 1482498.9066799234 | n/a | ineligible | compatibility_control | No positive reduction claim |

## Rollback decisions

- `serializer_decode` removed after regression; triggering cells: serializerDecodeDirectOptimized; ineligible removed cells: serializerDecodeDirectOptimized, serializerDecodeHeapOptimized.

## Compatibility controls

Fallback and composite controls remain claim-ineligible and are reported without a positive claim.

## Limitations

JMH GC allocation is environment-sensitive; throughput is diagnostic and not the allocation acceptance criterion.
