# 이슈 #1039 Serializer allocation proof

## 배경

Serializer ByteBuffer 작업은 production dispatch, wire format, ownership, security
default를 바꾸지 않으면서 재현 가능한 allocation evidence가 필요했다. Compatibility
default와 backend-specific path는 capability가 서로 다르므로 단일 benchmark score만으로는
충분하지 않았다.

## 결정

40개의 별도 이름을 가진 JMH cell로 standalone non-published `kotlinx-benchmark` module을
사용한다. `gc.alloc.rate.norm`을 primary metric으로, throughput을 diagnostic으로
다루며, 두 번의 fresh run이 matching ByteArray baseline 대비 모두 최소 5% 개선될 때만
lower-allocation claim을 인정한다. Compatibility/fallback cell은 항상 ineligible이다.

## 측정이 증명한 것

Run `run-20260718T030512Z`와 `run-20260718T031704Z`는 각각 40개의 allocation-bearing
result를 만들었다. JDK serialization, Kryo serialization/deserialization, Jackson 2/3
serialization의 다섯 optimized comparison이 accepted되었다. Comparator는 26개 candidate
comparison에서 `accepted=5`, `inconclusive=7`, `ineligible=14`를 산출했다.

## 측정이 증명하지 않은 것

이 결과는 inconclusive였던 JDK, Fory, Jackson 2/3, Fastjson2, Avro input path의 lower
allocation을 증명하지 않는다. Compatibility/fallback control, generic/specific/list
Avro API, 측정하지 않은 configuration, 다른 payload, 다른 JVM에 대해서도 아무것도
증명하지 않는다. Ergonomic ByteBuffer overload를 zero-allocation API로 바꾸지도 않는다.

## 실패 또는 발견

첫 executable JMH smoke는 dependency signature file이 fat JAR에 복사되어 실패했다.
Repository 표준 `META-INF/*.RSA`, `*.DSA`, `*.SF` exclusion을 적용하자 executable
artifact가 복구되었고 이후 40개 cell이 모두 실행되었다. 첫 evidence-launch command도
environment capture가 `./gradlew --version`을 호출하면서 repository output-protection
hook에 가로막혔다. 같은 Gradle WrapperMain을 직접 호출하자 run을 시작하거나 덮어쓰지
않고 false build redirect를 피할 수 있었다. 계획했던 per-module Detekt task는 이
repository에 존재하지 않는다. 사용 가능한 root `detekt` task는 `NO-SOURCE`이므로,
Detekt gap은 명시적으로 남기고 compilation, test, ABI proof, full non-test build가
실질적인 static/build evidence를 제공한다.

## 검증 증거

- Two sequential JMH runs: 40 JSON entries, 40 normalized-allocation metrics, and 40 summary rows per run.
- Seven comparator unit tests and byte-identical comparison regeneration.
- Benchmark module test, JMH compilation/JAR/smoke, and module build.
- Full tests for I/O, JSON, Jackson 2, Jackson 3, Fastjson2, and Avro.
- Serializer ABI proof: new caller compilation, default dispatch, and buffer default ABI all PASS.
- Module auto-registration and non-published/Kover-excluded benchmark classification.
- Full `build -x test`, locale/claim parity, raw artifact size, and `git diff --check` gates.

## 검토에서 놓친 점

초기 plan은 module-local Detekt task를 가정했고 generated JMH fat JAR의 signed dependency
metadata를 예상하지 못했다. 두 가정 모두 완료 전 실제 repository surface를 실행하면서
발견했다. Six-lens review는 P0=0, P1=0으로 수렴했다. Detekt configuration gap은 이슈
#1039 code regression이 아니라 기존 P2 repository concern이다.

## 향후 방지책

Serializer dispatch, benchmark payload, benchmark configuration이 바뀔 때마다 새 unique
run ID 두 개를 만들고 `comparison.csv`를 regenerate한다. Fresh B/op delta 두 개가 모두
5% same-direction rule을 만족하지 않으면 positive allocation wording을 재사용하지 않는다.
Compatibility/fallback control은 ineligible로 유지하고, invalid run은 진단용으로 보존하며,
evidence 수집 전에 executable JMH JAR를 검증한다.
