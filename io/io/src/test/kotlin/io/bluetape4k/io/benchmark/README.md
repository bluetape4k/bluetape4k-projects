# Benchmark 결과

## Binary Serializers

직렬화 할 대상은 다음과 같은 단순한 data class 에 랜덤한 값을 주입하고, 20개의 컬렉션을 직렬화/역직렬화 하는 작업에 대한 성능테스트입니다.

```kotlin
data class SimpleData(
    val id: Long,
    val name: String,
    val age: Int,
    val birth: LocalDate,
    val biography: String,
    val zip: String,
    val address: String,
    val price: BigDecimal? = null,
    val uuid: UUID? = null,         // NOTE: Kryo 는 JDK에 따라 UUID 를 지원하지 않을 수 있음 
    val bytes: ByteArray? = null,
): Serializable
```

### Byte Array 가 없는 경우

1. Byte Array 속성에 값이 없는 경우

| Library | ops/s   |
|---------|---------|
| Fory    | 305,821 |
| kryo    | 81,823  |
| jdk     | 22,249  |
| jackson | 39,510  |

> Fory 가 홍보하는 정도까지는 아니지만, 기존에 가장 많이 사용하는 Kryo 보다 3배 이상 빠르다.
> 단순 수형에 대한 처리는 의외로 JDK 가 가장 느리다.
>

2. Byte Array 에 랜덤한 4096 바이트를 넣은 경우

| Library | ops/s  |
|---------|--------|
| Fory    | 59,192 |
| kryo    | 29,329 |
| jdk     | 8,431  |
| jackson | 4,323  |

![Binary serializer throughput chart](../../../../../../../../../docs/images/readme-charts/io-serializer-throughput-chart-01.png)

> Fory 가 홍보하는 정도까지는 아니지만, 기존에 가장 많이 사용하는 Kryo 보다 3배 이상 빠르다.
> ByteArray 직렬화 하는 경우, Jackson 이 가장 느리다.

## Compressor

resources/files/Utf8Samples.txt (40kb) 파일을 압축/복원 성능 테스트

| Compressor | ops/s |
|------------|-------|
| gzip       | 1,195 |
| deflate    | 1,084 |
| lz4        | 6,769 |
| snappy     | 8,073 |
| zstd       | 5,103 |

![Compressor throughput chart](../../../../../../../../../docs/images/readme-charts/io-compressor-throughput-chart-01.png)

## SHA-256 다이제스트 특성 측정 (2026-11-24)

이 결과는 단일 thread의 raw-digest microbenchmark 기록이다. Workshop 소비자 성능, API 간 우열, 성능 향상이나 회귀를 주장하지 않는다.

- 실행 명령: `./gradlew -Didea.active=false :bluetape4k-io:testDigestBenchmark --console=plain`
- Git HEAD: `9122e244669f1667551d4964eb2867a62e547521`
- `DigestBenchmark.kt` SHA-256: `61796235ba9c9549cd90f5f753495b1a2a6786e0cde9ec12fb4f2955e113459e`
- Gradle Wrapper `9.8.2`, Kotlin `2.4.10`, kotlinx-benchmark `0.4.17`, JMH `1.37`
- JDK: Oracle GraalVM `25.0.5.0.3+1-LTS-jvmci-25.5-b26`; VM `Java HotSpot(TM) 64-Bit Server VM`
- OS/kernel/architecture: macOS `27.2.1`, Darwin kernel `27.2.0`, `arm64`
- CPU: Apple M4 Pro, 12 logical cores
- JMH fork JVM arguments: `-XX:ThreadPriorityPolicy=1 -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCIProduct -XX:+EnableJVMCI -XX:-UnlockExperimentalVMOptions -Dfile.encoding=UTF-8 -Duser.country=KR -Duser.language=ko -Duser.variant`
- 구성: throughput (`ops/s`), 2 forks, fork당 warmup 2회 × 1초, 측정 5회 × 1초, 1 thread. 모든 행의 `Cnt=10`은 두 fork의 측정 표본을 합친 값이다. 오차는 JMH의 99.9% confidence error다.
- 입력 payload: `ByteArray(size) { index -> ((index * 31 + size) % 256).toByte() }`

| 크기 | Payload SHA-256 |
|---:|---|
| 64 B | `3ea97ec766b8247739939247b4d4cb362cf13c100deb0cc2ba5391f762023852` |
| 1 KiB | `b54e72e83904b84eb645c6c4b7a55f73b85871cdc9a74645979cd7c1be411b40` |
| 64 KiB | `58f414c587d599b6fa1678097a7459ce669c6e0fe894d81be9c7ed2879bd6bcb` |

| 경로 | 측정 구간 |
|---|---|
| `jdkOneShot` | timed method 안의 `MessageDigest.getInstance("SHA-256")` 생성과 digest |
| `apiOneShot` | `Digesters.SHA256.digest(payload)`; 호출마다 새 session 생성 포함 |
| `apiIncrementalNewSession` | 새 session 생성, 두 조각 `update`, `digest` |
| `apiIncrementalReusedSession` | Trial setup에서 생성한 thread-local session의 두 조각 `update`와 `digest`; session 생성 제외 |

| 경로 | 크기 | Score ± error (ops/s) |
|---|---:|---:|
| `apiIncrementalNewSession` | 64 B | 17,970,867.452 ± 194,901.657 |
| `apiIncrementalNewSession` | 1 KiB | 2,943,671.408 ± 22,275.364 |
| `apiIncrementalNewSession` | 64 KiB | 50,885.657 ± 286.377 |
| `apiIncrementalReusedSession` | 64 B | 22,915,962.675 ± 1,876,825.681 |
| `apiIncrementalReusedSession` | 1 KiB | 3,039,237.832 ± 136,261.044 |
| `apiIncrementalReusedSession` | 64 KiB | 50,752.375 ± 843.827 |
| `apiOneShot` | 64 B | 18,172,871.496 ± 162,334.692 |
| `apiOneShot` | 1 KiB | 2,969,419.765 ± 14,981.968 |
| `apiOneShot` | 64 KiB | 50,787.250 ± 573.906 |
| `jdkOneShot` | 64 B | 18,063,706.628 ± 253,143.188 |
| `jdkOneShot` | 1 KiB | 2,952,911.384 ± 33,169.473 |
| `jdkOneShot` | 64 KiB | 50,857.755 ± 540.793 |

처음 task 실행은 plugin 기본 `jvmForks=1`이 class의 `@Fork(2)`를 덮어써서 측정 기준을 충족하지 못했다. 해당 수치는 채택하지 않았다. named `digest` configuration에 `advanced("jvmForks", "definedByJmh")`를 설정한 뒤 재실행했고, 결과 JSON에서 12개 조합 각각 `forks=2`, fork별 5개 측정값을 확인했다. JMH 1.37은 JDK 25에서 `sun.misc.Unsafe.objectFieldOffset`의 terminal deprecation warning을 출력했으며 task는 성공했다.

아래는 최종 실행이 생성한 JMH JSON report 전체다. fork별 원시 측정값과 실행 환경을 포함한다.

```json
[
    {
        "jmhVersion" : "1.37",
        "benchmark" : "io.bluetape4k.io.benchmark.DigestBenchmark.apiIncrementalNewSession",
        "mode" : "thrpt",
        "threads" : 1,
        "forks" : 2,
        "jvm" : "/Library/Java/JavaVirtualMachines/graalvm-jdk-25/Contents/Home/bin/java",
        "jvmArgs" : [
            "-XX:ThreadPriorityPolicy=1",
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:+EnableJVMCIProduct",
            "-XX:+EnableJVMCI",
            "-XX:-UnlockExperimentalVMOptions",
            "-Dfile.encoding=UTF-8",
            "-Duser.country=KR",
            "-Duser.language=ko",
            "-Duser.variant"
        ],
        "jdkVersion" : "25.0.5.0.3",
        "vmName" : "Java HotSpot(TM) 64-Bit Server VM",
        "vmVersion" : "25.0.5.0.3+1-LTS-jvmci-25.5-b26",
        "warmupIterations" : 2,
        "warmupTime" : "1 s",
        "warmupBatchSize" : 1,
        "measurementIterations" : 5,
        "measurementTime" : "1 s",
        "measurementBatchSize" : 1,
        "params" : {
            "payloadSize" : "64"
        },
        "primaryMetric" : {
            "score" : 1.7970867452174995E7,
            "scoreError" : 194901.65733210839,
            "scoreConfidence" : [
                1.7775965794842888E7,
                1.8165769109507103E7
            ],
            "scorePercentiles" : {
                "0.0" : 1.7833912124484353E7,
                "50.0" : 1.7936514125888642E7,
                "90.0" : 1.8178932091385856E7,
                "95.0" : 1.8184912302668348E7,
                "99.0" : 1.8184912302668348E7,
                "99.9" : 1.8184912302668348E7,
                "99.99" : 1.8184912302668348E7,
                "99.999" : 1.8184912302668348E7,
                "99.9999" : 1.8184912302668348E7,
                "100.0" : 1.8184912302668348E7
            },
            "scoreUnit" : "ops/s",
            "rawData" : [
                [
                    1.8125110189843424E7,
                    1.8184912302668348E7,
                    1.797527649629736E7,
                    1.809368596497285E7,
                    1.8022605988430806E7
                ],
                [
                    1.786933884623408E7,
                    1.78551273310367E7,
                    1.7897751755479924E7,
                    1.7833912124484353E7,
                    1.7850953522302095E7
                ]
            ]
        },
        "secondaryMetrics" : {
        }
    },
    {
        "jmhVersion" : "1.37",
        "benchmark" : "io.bluetape4k.io.benchmark.DigestBenchmark.apiIncrementalNewSession",
        "mode" : "thrpt",
        "threads" : 1,
        "forks" : 2,
        "jvm" : "/Library/Java/JavaVirtualMachines/graalvm-jdk-25/Contents/Home/bin/java",
        "jvmArgs" : [
            "-XX:ThreadPriorityPolicy=1",
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:+EnableJVMCIProduct",
            "-XX:+EnableJVMCI",
            "-XX:-UnlockExperimentalVMOptions",
            "-Dfile.encoding=UTF-8",
            "-Duser.country=KR",
            "-Duser.language=ko",
            "-Duser.variant"
        ],
        "jdkVersion" : "25.0.5.0.3",
        "vmName" : "Java HotSpot(TM) 64-Bit Server VM",
        "vmVersion" : "25.0.5.0.3+1-LTS-jvmci-25.5-b26",
        "warmupIterations" : 2,
        "warmupTime" : "1 s",
        "warmupBatchSize" : 1,
        "measurementIterations" : 5,
        "measurementTime" : "1 s",
        "measurementBatchSize" : 1,
        "params" : {
            "payloadSize" : "1024"
        },
        "primaryMetric" : {
            "score" : 2943671.4078080594,
            "scoreError" : 22275.36374836508,
            "scoreConfidence" : [
                2921396.0440596943,
                2965946.7715564244
            ],
            "scorePercentiles" : {
                "0.0" : 2920427.816777978,
                "50.0" : 2947738.991506503,
                "90.0" : 2961463.7617524792,
                "95.0" : 2961653.483526358,
                "99.0" : 2961653.483526358,
                "99.9" : 2961653.483526358,
                "99.99" : 2961653.483526358,
                "99.999" : 2961653.483526358,
                "99.9999" : 2961653.483526358,
                "100.0" : 2961653.483526358
            },
            "scoreUnit" : "ops/s",
            "rawData" : [
                [
                    2950222.0644757897,
                    2920427.816777978,
                    2922177.008315852,
                    2937268.3210607376,
                    2959756.265787572
                ],
                [
                    2933926.01862441,
                    2945480.1407446326,
                    2949997.842268374,
                    2961653.483526358,
                    2955805.1164988894
                ]
            ]
        },
        "secondaryMetrics" : {
        }
    },
    {
        "jmhVersion" : "1.37",
        "benchmark" : "io.bluetape4k.io.benchmark.DigestBenchmark.apiIncrementalNewSession",
        "mode" : "thrpt",
        "threads" : 1,
        "forks" : 2,
        "jvm" : "/Library/Java/JavaVirtualMachines/graalvm-jdk-25/Contents/Home/bin/java",
        "jvmArgs" : [
            "-XX:ThreadPriorityPolicy=1",
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:+EnableJVMCIProduct",
            "-XX:+EnableJVMCI",
            "-XX:-UnlockExperimentalVMOptions",
            "-Dfile.encoding=UTF-8",
            "-Duser.country=KR",
            "-Duser.language=ko",
            "-Duser.variant"
        ],
        "jdkVersion" : "25.0.5.0.3",
        "vmName" : "Java HotSpot(TM) 64-Bit Server VM",
        "vmVersion" : "25.0.5.0.3+1-LTS-jvmci-25.5-b26",
        "warmupIterations" : 2,
        "warmupTime" : "1 s",
        "warmupBatchSize" : 1,
        "measurementIterations" : 5,
        "measurementTime" : "1 s",
        "measurementBatchSize" : 1,
        "params" : {
            "payloadSize" : "65536"
        },
        "primaryMetric" : {
            "score" : 50885.657424716555,
            "scoreError" : 286.3765831850008,
            "scoreConfidence" : [
                50599.28084153155,
                51172.03400790156
            ],
            "scorePercentiles" : {
                "0.0" : 50650.83333269668,
                "50.0" : 50908.7736726436,
                "90.0" : 51177.88653452213,
                "95.0" : 51188.10524782528,
                "99.0" : 51188.10524782528,
                "99.9" : 51188.10524782528,
                "99.99" : 51188.10524782528,
                "99.999" : 51188.10524782528,
                "99.9999" : 51188.10524782528,
                "100.0" : 51188.10524782528
            },
            "scoreUnit" : "ops/s",
            "rawData" : [
                [
                    50687.76317404681,
                    51010.20903548018,
                    51188.10524782528,
                    51085.91811479371,
                    50651.406732358344
                ],
                [
                    50778.949291818324,
                    50863.175329044054,
                    50954.372016243135,
                    50650.83333269668,
                    50985.84197285911
                ]
            ]
        },
        "secondaryMetrics" : {
        }
    },
    {
        "jmhVersion" : "1.37",
        "benchmark" : "io.bluetape4k.io.benchmark.DigestBenchmark.apiIncrementalReusedSession",
        "mode" : "thrpt",
        "threads" : 1,
        "forks" : 2,
        "jvm" : "/Library/Java/JavaVirtualMachines/graalvm-jdk-25/Contents/Home/bin/java",
        "jvmArgs" : [
            "-XX:ThreadPriorityPolicy=1",
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:+EnableJVMCIProduct",
            "-XX:+EnableJVMCI",
            "-XX:-UnlockExperimentalVMOptions",
            "-Dfile.encoding=UTF-8",
            "-Duser.country=KR",
            "-Duser.language=ko",
            "-Duser.variant"
        ],
        "jdkVersion" : "25.0.5.0.3",
        "vmName" : "Java HotSpot(TM) 64-Bit Server VM",
        "vmVersion" : "25.0.5.0.3+1-LTS-jvmci-25.5-b26",
        "warmupIterations" : 2,
        "warmupTime" : "1 s",
        "warmupBatchSize" : 1,
        "measurementIterations" : 5,
        "measurementTime" : "1 s",
        "measurementBatchSize" : 1,
        "params" : {
            "payloadSize" : "64"
        },
        "primaryMetric" : {
            "score" : 2.291596267461492E7,
            "scoreError" : 1876825.681160806,
            "scoreConfidence" : [
                2.1039136993454114E7,
                2.479278835577573E7
            ],
            "scorePercentiles" : {
                "0.0" : 2.1629391009613086E7,
                "50.0" : 2.2697734266686887E7,
                "90.0" : 2.4602227763553195E7,
                "95.0" : 2.4615142699457392E7,
                "99.0" : 2.4615142699457392E7,
                "99.9" : 2.4615142699457392E7,
                "99.99" : 2.4615142699457392E7,
                "99.999" : 2.4615142699457392E7,
                "99.9999" : 2.4615142699457392E7,
                "100.0" : 2.4615142699457392E7
            },
            "scoreUnit" : "ops/s",
            "rawData" : [
                [
                    2.3818637844416276E7,
                    2.448599334041541E7,
                    2.4126809772996306E7,
                    2.4615142699457392E7,
                    2.1629391009613086E7
                ],
                [
                    2.260737913394689E7,
                    2.1678056119673546E7,
                    2.1753875427339427E7,
                    2.278808939942688E7,
                    2.165625199886399E7
                ]
            ]
        },
        "secondaryMetrics" : {
        }
    },
    {
        "jmhVersion" : "1.37",
        "benchmark" : "io.bluetape4k.io.benchmark.DigestBenchmark.apiIncrementalReusedSession",
        "mode" : "thrpt",
        "threads" : 1,
        "forks" : 2,
        "jvm" : "/Library/Java/JavaVirtualMachines/graalvm-jdk-25/Contents/Home/bin/java",
        "jvmArgs" : [
            "-XX:ThreadPriorityPolicy=1",
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:+EnableJVMCIProduct",
            "-XX:+EnableJVMCI",
            "-XX:-UnlockExperimentalVMOptions",
            "-Dfile.encoding=UTF-8",
            "-Duser.country=KR",
            "-Duser.language=ko",
            "-Duser.variant"
        ],
        "jdkVersion" : "25.0.5.0.3",
        "vmName" : "Java HotSpot(TM) 64-Bit Server VM",
        "vmVersion" : "25.0.5.0.3+1-LTS-jvmci-25.5-b26",
        "warmupIterations" : 2,
        "warmupTime" : "1 s",
        "warmupBatchSize" : 1,
        "measurementIterations" : 5,
        "measurementTime" : "1 s",
        "measurementBatchSize" : 1,
        "params" : {
            "payloadSize" : "1024"
        },
        "primaryMetric" : {
            "score" : 3039237.832333031,
            "scoreError" : 136261.04400775797,
            "scoreConfidence" : [
                2902976.788325273,
                3175498.8763407893
            ],
            "scorePercentiles" : {
                "0.0" : 2868097.204029833,
                "50.0" : 3021541.0049711904,
                "90.0" : 3141780.197128267,
                "95.0" : 3142488.787678021,
                "99.0" : 3142488.787678021,
                "99.9" : 3142488.787678021,
                "99.99" : 3142488.787678021,
                "99.999" : 3142488.787678021,
                "99.9999" : 3142488.787678021,
                "100.0" : 3142488.787678021
            },
            "scoreUnit" : "ops/s",
            "rawData" : [
                [
                    2868097.204029833,
                    2968743.744230964,
                    2999011.089574785,
                    3002083.5380527247,
                    2992058.5176418102
                ],
                [
                    3040998.4718896565,
                    3142488.787678021,
                    3128737.585307897,
                    3114756.5027441396,
                    3135402.882180482
                ]
            ]
        },
        "secondaryMetrics" : {
        }
    },
    {
        "jmhVersion" : "1.37",
        "benchmark" : "io.bluetape4k.io.benchmark.DigestBenchmark.apiIncrementalReusedSession",
        "mode" : "thrpt",
        "threads" : 1,
        "forks" : 2,
        "jvm" : "/Library/Java/JavaVirtualMachines/graalvm-jdk-25/Contents/Home/bin/java",
        "jvmArgs" : [
            "-XX:ThreadPriorityPolicy=1",
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:+EnableJVMCIProduct",
            "-XX:+EnableJVMCI",
            "-XX:-UnlockExperimentalVMOptions",
            "-Dfile.encoding=UTF-8",
            "-Duser.country=KR",
            "-Duser.language=ko",
            "-Duser.variant"
        ],
        "jdkVersion" : "25.0.5.0.3",
        "vmName" : "Java HotSpot(TM) 64-Bit Server VM",
        "vmVersion" : "25.0.5.0.3+1-LTS-jvmci-25.5-b26",
        "warmupIterations" : 2,
        "warmupTime" : "1 s",
        "warmupBatchSize" : 1,
        "measurementIterations" : 5,
        "measurementTime" : "1 s",
        "measurementBatchSize" : 1,
        "params" : {
            "payloadSize" : "65536"
        },
        "primaryMetric" : {
            "score" : 50752.37491909224,
            "scoreError" : 843.826859688177,
            "scoreConfidence" : [
                49908.548059404064,
                51596.201778780414
            ],
            "scorePercentiles" : {
                "0.0" : 49281.81576814042,
                "50.0" : 50912.83643735291,
                "90.0" : 51196.86712170225,
                "95.0" : 51199.3822095661,
                "99.0" : 51199.3822095661,
                "99.9" : 51199.3822095661,
                "99.99" : 51199.3822095661,
                "99.999" : 51199.3822095661,
                "99.9999" : 51199.3822095661,
                "100.0" : 51199.3822095661
            },
            "scoreUnit" : "ops/s",
            "rawData" : [
                [
                    51069.03083563643,
                    50620.65347935147,
                    50834.47926323263,
                    50661.56311057923,
                    49281.81576814042
                ],
                [
                    51013.46316555104,
                    50677.936416464305,
                    51199.3822095661,
                    51174.23133092755,
                    50991.193611473194
                ]
            ]
        },
        "secondaryMetrics" : {
        }
    },
    {
        "jmhVersion" : "1.37",
        "benchmark" : "io.bluetape4k.io.benchmark.DigestBenchmark.apiOneShot",
        "mode" : "thrpt",
        "threads" : 1,
        "forks" : 2,
        "jvm" : "/Library/Java/JavaVirtualMachines/graalvm-jdk-25/Contents/Home/bin/java",
        "jvmArgs" : [
            "-XX:ThreadPriorityPolicy=1",
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:+EnableJVMCIProduct",
            "-XX:+EnableJVMCI",
            "-XX:-UnlockExperimentalVMOptions",
            "-Dfile.encoding=UTF-8",
            "-Duser.country=KR",
            "-Duser.language=ko",
            "-Duser.variant"
        ],
        "jdkVersion" : "25.0.5.0.3",
        "vmName" : "Java HotSpot(TM) 64-Bit Server VM",
        "vmVersion" : "25.0.5.0.3+1-LTS-jvmci-25.5-b26",
        "warmupIterations" : 2,
        "warmupTime" : "1 s",
        "warmupBatchSize" : 1,
        "measurementIterations" : 5,
        "measurementTime" : "1 s",
        "measurementBatchSize" : 1,
        "params" : {
            "payloadSize" : "64"
        },
        "primaryMetric" : {
            "score" : 1.8172871495991718E7,
            "scoreError" : 162334.69173709187,
            "scoreConfidence" : [
                1.8010536804254625E7,
                1.833520618772881E7
            ],
            "scorePercentiles" : {
                "0.0" : 1.7930753061455958E7,
                "50.0" : 1.82113423584327E7,
                "90.0" : 1.8267464502336524E7,
                "95.0" : 1.826824218706445E7,
                "99.0" : 1.826824218706445E7,
                "99.9" : 1.826824218706445E7,
                "99.99" : 1.826824218706445E7,
                "99.999" : 1.826824218706445E7,
                "99.9999" : 1.826824218706445E7,
                "100.0" : 1.826824218706445E7
            },
            "scoreUnit" : "ops/s",
            "rawData" : [
                [
                    1.8225750860515606E7,
                    1.7930753061455958E7,
                    1.8041188468082443E7,
                    1.8210506905575E7,
                    1.82121778112904E7
                ],
                [
                    1.8151252271678783E7,
                    1.826046533978518E7,
                    1.826824218706445E7,
                    1.8187788811815716E7,
                    1.8240589242653675E7
                ]
            ]
        },
        "secondaryMetrics" : {
        }
    },
    {
        "jmhVersion" : "1.37",
        "benchmark" : "io.bluetape4k.io.benchmark.DigestBenchmark.apiOneShot",
        "mode" : "thrpt",
        "threads" : 1,
        "forks" : 2,
        "jvm" : "/Library/Java/JavaVirtualMachines/graalvm-jdk-25/Contents/Home/bin/java",
        "jvmArgs" : [
            "-XX:ThreadPriorityPolicy=1",
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:+EnableJVMCIProduct",
            "-XX:+EnableJVMCI",
            "-XX:-UnlockExperimentalVMOptions",
            "-Dfile.encoding=UTF-8",
            "-Duser.country=KR",
            "-Duser.language=ko",
            "-Duser.variant"
        ],
        "jdkVersion" : "25.0.5.0.3",
        "vmName" : "Java HotSpot(TM) 64-Bit Server VM",
        "vmVersion" : "25.0.5.0.3+1-LTS-jvmci-25.5-b26",
        "warmupIterations" : 2,
        "warmupTime" : "1 s",
        "warmupBatchSize" : 1,
        "measurementIterations" : 5,
        "measurementTime" : "1 s",
        "measurementBatchSize" : 1,
        "params" : {
            "payloadSize" : "1024"
        },
        "primaryMetric" : {
            "score" : 2969419.7651267005,
            "scoreError" : 14981.968378193687,
            "scoreConfidence" : [
                2954437.796748507,
                2984401.733504894
            ],
            "scorePercentiles" : {
                "0.0" : 2947410.126540799,
                "50.0" : 2969744.3399726115,
                "90.0" : 2982230.1152447034,
                "95.0" : 2982653.7994481893,
                "99.0" : 2982653.7994481893,
                "99.9" : 2982653.7994481893,
                "99.99" : 2982653.7994481893,
                "99.999" : 2982653.7994481893,
                "99.9999" : 2982653.7994481893,
                "100.0" : 2982653.7994481893
            },
            "scoreUnit" : "ops/s",
            "rawData" : [
                [
                    2978416.957413329,
                    2982653.7994481893,
                    2968351.132491473,
                    2971137.5474537495,
                    2965679.220230672
                ],
                [
                    2962628.497668359,
                    2947410.126540799,
                    2974205.6565164044,
                    2976563.839056935,
                    2967150.8744470915
                ]
            ]
        },
        "secondaryMetrics" : {
        }
    },
    {
        "jmhVersion" : "1.37",
        "benchmark" : "io.bluetape4k.io.benchmark.DigestBenchmark.apiOneShot",
        "mode" : "thrpt",
        "threads" : 1,
        "forks" : 2,
        "jvm" : "/Library/Java/JavaVirtualMachines/graalvm-jdk-25/Contents/Home/bin/java",
        "jvmArgs" : [
            "-XX:ThreadPriorityPolicy=1",
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:+EnableJVMCIProduct",
            "-XX:+EnableJVMCI",
            "-XX:-UnlockExperimentalVMOptions",
            "-Dfile.encoding=UTF-8",
            "-Duser.country=KR",
            "-Duser.language=ko",
            "-Duser.variant"
        ],
        "jdkVersion" : "25.0.5.0.3",
        "vmName" : "Java HotSpot(TM) 64-Bit Server VM",
        "vmVersion" : "25.0.5.0.3+1-LTS-jvmci-25.5-b26",
        "warmupIterations" : 2,
        "warmupTime" : "1 s",
        "warmupBatchSize" : 1,
        "measurementIterations" : 5,
        "measurementTime" : "1 s",
        "measurementBatchSize" : 1,
        "params" : {
            "payloadSize" : "65536"
        },
        "primaryMetric" : {
            "score" : 50787.249555864444,
            "scoreError" : 573.9063081465164,
            "scoreConfidence" : [
                50213.34324771793,
                51361.15586401096
            ],
            "scorePercentiles" : {
                "0.0" : 50063.054584078876,
                "50.0" : 50738.65373399676,
                "90.0" : 51212.373020756975,
                "95.0" : 51214.473053965914,
                "99.0" : 51214.473053965914,
                "99.9" : 51214.473053965914,
                "99.99" : 51214.473053965914,
                "99.999" : 51214.473053965914,
                "99.9999" : 51214.473053965914,
                "100.0" : 51214.473053965914
            },
            "scoreUnit" : "ops/s",
            "rawData" : [
                [
                    50063.054584078876,
                    50738.80238329417,
                    51214.473053965914,
                    50631.545907218446,
                    50586.922207115116
                ],
                [
                    50442.665076100944,
                    51193.47272187652,
                    50738.505084699354,
                    51130.26916597233,
                    51132.78537432277
                ]
            ]
        },
        "secondaryMetrics" : {
        }
    },
    {
        "jmhVersion" : "1.37",
        "benchmark" : "io.bluetape4k.io.benchmark.DigestBenchmark.jdkOneShot",
        "mode" : "thrpt",
        "threads" : 1,
        "forks" : 2,
        "jvm" : "/Library/Java/JavaVirtualMachines/graalvm-jdk-25/Contents/Home/bin/java",
        "jvmArgs" : [
            "-XX:ThreadPriorityPolicy=1",
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:+EnableJVMCIProduct",
            "-XX:+EnableJVMCI",
            "-XX:-UnlockExperimentalVMOptions",
            "-Dfile.encoding=UTF-8",
            "-Duser.country=KR",
            "-Duser.language=ko",
            "-Duser.variant"
        ],
        "jdkVersion" : "25.0.5.0.3",
        "vmName" : "Java HotSpot(TM) 64-Bit Server VM",
        "vmVersion" : "25.0.5.0.3+1-LTS-jvmci-25.5-b26",
        "warmupIterations" : 2,
        "warmupTime" : "1 s",
        "warmupBatchSize" : 1,
        "measurementIterations" : 5,
        "measurementTime" : "1 s",
        "measurementBatchSize" : 1,
        "params" : {
            "payloadSize" : "64"
        },
        "primaryMetric" : {
            "score" : 1.8063706628225822E7,
            "scoreError" : 253143.18752627078,
            "scoreConfidence" : [
                1.781056344069955E7,
                1.8316849815752093E7
            ],
            "scorePercentiles" : {
                "0.0" : 1.7769649243411765E7,
                "50.0" : 1.8140884222419895E7,
                "90.0" : 1.820156673660879E7,
                "95.0" : 1.820280412929577E7,
                "99.0" : 1.820280412929577E7,
                "99.9" : 1.820280412929577E7,
                "99.99" : 1.820280412929577E7,
                "99.999" : 1.820280412929577E7,
                "99.9999" : 1.820280412929577E7,
                "100.0" : 1.820280412929577E7
            },
            "scoreUnit" : "ops/s",
            "rawData" : [
                [
                    1.818941387252501E7,
                    1.8190430202425934E7,
                    1.8187072047718637E7,
                    1.8067805054498874E7,
                    1.7769649243411765E7
                ],
                [
                    1.8143594698318023E7,
                    1.796223261822362E7,
                    1.7785890669318818E7,
                    1.8138173746521764E7,
                    1.820280412929577E7
                ]
            ]
        },
        "secondaryMetrics" : {
        }
    },
    {
        "jmhVersion" : "1.37",
        "benchmark" : "io.bluetape4k.io.benchmark.DigestBenchmark.jdkOneShot",
        "mode" : "thrpt",
        "threads" : 1,
        "forks" : 2,
        "jvm" : "/Library/Java/JavaVirtualMachines/graalvm-jdk-25/Contents/Home/bin/java",
        "jvmArgs" : [
            "-XX:ThreadPriorityPolicy=1",
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:+EnableJVMCIProduct",
            "-XX:+EnableJVMCI",
            "-XX:-UnlockExperimentalVMOptions",
            "-Dfile.encoding=UTF-8",
            "-Duser.country=KR",
            "-Duser.language=ko",
            "-Duser.variant"
        ],
        "jdkVersion" : "25.0.5.0.3",
        "vmName" : "Java HotSpot(TM) 64-Bit Server VM",
        "vmVersion" : "25.0.5.0.3+1-LTS-jvmci-25.5-b26",
        "warmupIterations" : 2,
        "warmupTime" : "1 s",
        "warmupBatchSize" : 1,
        "measurementIterations" : 5,
        "measurementTime" : "1 s",
        "measurementBatchSize" : 1,
        "params" : {
            "payloadSize" : "1024"
        },
        "primaryMetric" : {
            "score" : 2952911.383571733,
            "scoreError" : 33169.47331261207,
            "scoreConfidence" : [
                2919741.9102591206,
                2986080.856884345
            ],
            "scorePercentiles" : {
                "0.0" : 2915821.636067421,
                "50.0" : 2954323.7884346764,
                "90.0" : 2977861.842449645,
                "95.0" : 2977867.3925205953,
                "99.0" : 2977867.3925205953,
                "99.9" : 2977867.3925205953,
                "99.99" : 2977867.3925205953,
                "99.999" : 2977867.3925205953,
                "99.9999" : 2977867.3925205953,
                "100.0" : 2977867.3925205953
            },
            "scoreUnit" : "ops/s",
            "rawData" : [
                [
                    2939014.4145516483,
                    2977867.3925205953,
                    2977811.8918110942,
                    2949407.3930915757,
                    2915821.636067421
                ],
                [
                    2947343.49504244,
                    2973472.124024569,
                    2923649.117335665,
                    2959240.183777777,
                    2965486.187494541
                ]
            ]
        },
        "secondaryMetrics" : {
        }
    },
    {
        "jmhVersion" : "1.37",
        "benchmark" : "io.bluetape4k.io.benchmark.DigestBenchmark.jdkOneShot",
        "mode" : "thrpt",
        "threads" : 1,
        "forks" : 2,
        "jvm" : "/Library/Java/JavaVirtualMachines/graalvm-jdk-25/Contents/Home/bin/java",
        "jvmArgs" : [
            "-XX:ThreadPriorityPolicy=1",
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:+EnableJVMCIProduct",
            "-XX:+EnableJVMCI",
            "-XX:-UnlockExperimentalVMOptions",
            "-Dfile.encoding=UTF-8",
            "-Duser.country=KR",
            "-Duser.language=ko",
            "-Duser.variant"
        ],
        "jdkVersion" : "25.0.5.0.3",
        "vmName" : "Java HotSpot(TM) 64-Bit Server VM",
        "vmVersion" : "25.0.5.0.3+1-LTS-jvmci-25.5-b26",
        "warmupIterations" : 2,
        "warmupTime" : "1 s",
        "warmupBatchSize" : 1,
        "measurementIterations" : 5,
        "measurementTime" : "1 s",
        "measurementBatchSize" : 1,
        "params" : {
            "payloadSize" : "65536"
        },
        "primaryMetric" : {
            "score" : 50857.75499396571,
            "scoreError" : 540.7934772413838,
            "scoreConfidence" : [
                50316.961516724325,
                51398.5484712071
            ],
            "scorePercentiles" : {
                "0.0" : 50259.34034955167,
                "50.0" : 50922.996345541,
                "90.0" : 51257.093293978185,
                "95.0" : 51261.35814722051,
                "99.0" : 51261.35814722051,
                "99.9" : 51261.35814722051,
                "99.99" : 51261.35814722051,
                "99.999" : 51261.35814722051,
                "99.9999" : 51261.35814722051,
                "100.0" : 51261.35814722051
            },
            "scoreUnit" : "ops/s",
            "rawData" : [
                [
                    50527.59949505146,
                    50449.17330551369,
                    51261.35814722051,
                    51218.70961479721,
                    51009.761004829976
                ],
                [
                    50259.34034955167,
                    51139.47900615617,
                    51170.89458601174,
                    50705.00274427276,
                    50836.231686252024
                ]
            ]
        },
        "secondaryMetrics" : {
        }
    }
]
```
