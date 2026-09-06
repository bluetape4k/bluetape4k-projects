# #1639 Fory 1.7.1 Kotlin metadata와 Redis 저장 바이트 호환성 설계

## 목적과 승인 범위

- 이슈: [#1639](https://github.com/bluetape4k/bluetape4k-projects/issues/1639), milestone `2.1.0`, 담당자 `debop`.
- 저장소: `bluetape4k/bluetape4k-projects`.
- 기준 브랜치와 커밋: `develop@9811932427aac4c6cfba7b16ae3def215e93a2fe`.
- 작업 브랜치: `test/issue-1639-fory-wire-compatibility`.
- 승인 범위: production serializer 구현은 변경하지 않고, 현재 Fory `1.7.1`의 Kotlin metadata round-trip, Fory `1.6.0` 고정 fixture 읽기, 손상 입력 실패, Redis serializer 사용 경로를 테스트와 provenance 문서로 고정한다.
- 이 작업은 현재 제품 장애를 재현하는 버그 수정이 아니다. 중앙 catalog PR #241의 `1.6.0 → 1.7.1` 변경에 대한 호환성 증거를 추가한다.

## 현재 근거

1. `io/io`의 `FastForyCompatibilityTest`는 기본 Fory(`COMPATIBLE`)와 FastFory(`SCHEMA_CONSISTENT`)의 round-trip 및 상호 비호환성만 확인한다.
2. `io/io/src/test/resources/compat/issue-754/pre-change/manifest.json`의 Fory fixture는 `1.3.0`으로 생성되었으므로 #1639의 `1.6.0 → 1.7.1` 근거로 사용할 수 없다.
3. 중앙 catalog ref `9698c9d66bea6fcba373143ee8fa5bfbd9812d4b`는 Fory/Fory Kotlin `1.7.1`, Kotlin `2.4.10`을 선택한다. 직전 catalog commit은 Fory/Fory Kotlin `1.6.0`이다.
4. `RedisBinarySerializers.Fory`, `LZ4Fory`, `FastFory`는 현재 `BinarySerializers`를 그대로 감싸며, `spring-boot/redis`에는 기본·압축·FastFory round-trip 테스트가 이미 있다. 새 검증은 저장 바이트 경계와 Kotlin metadata 사례를 명시적으로 연결한다.

## 선택한 설계

### 1. Kotlin metadata payload

`io/io/src/test/kotlin/io/bluetape4k/io/serializer/ForyWireCompatibilityTest.kt`에 동일한 이름과 구조를 유지하는 test fixture payload를 둔다.

- Kotlin `2.4.10`으로 컴파일되는 `Serializable` data class를 사용한다.
- nullable property, default property, collection, 중첩 객체, 지원되는 `@JvmInline value class` property를 포함한다.
- 현재 `BinarySerializers.Fory`로 쓴 바이트를 같은 serializer로 읽고 모든 필드를 비교한다.
- fixture에 포함한 value class가 Fory의 현재 지원 범위에서 실패하면 실패를 숨기지 않고 지원 범위의 결론과 gap으로 기록한다.

### 2. Fory `1.6.0` frozen fixture

`io/io/src/test/resources/compat/issue-1639/fory-1.6.0/` 아래에 binary fixture와 `manifest.json`을 둔다.

- fixture producer는 Fory/Fory Kotlin `1.6.0`, Kotlin `2.4.10`, 현재 payload의 동일한 fully-qualified class name과 동일한 builder 옵션을 사용한다.
- manifest에는 producer commit/tree, central catalog ref, Java/Gradle/Kotlin/Fory 버전, payload 필드, fixture size와 SHA-256, 생성 명령을 기록한다.
- generation source 또는 명령을 함께 보존하여 fixture를 다시 만들 수 있게 한다. fixture 자체를 매 테스트마다 생성하지 않는다.
- 현재 `BinarySerializers.Fory`(`1.7.1`)가 fixture를 읽어 기대 payload를 반환하는지 검증한다.
- 현재 `1.7.1`로 새로 쓴 바이트도 읽히는지 별도 test로 검증한다. 두 결과는 한 fixture로 합치지 않는다.

### 3. 손상 입력과 경계

- 빈 입력은 기존 `BinarySerializer` null 정책대로 `null`을 반환하는 기존 계약을 유지한다.
- non-empty malformed 입력과 frozen fixture의 앞부분만 남긴 truncated 입력은 `BinarySerializationException`으로 실패해야 한다.
- malformed/truncated fixture는 작게 유지하고, 테스트에는 제한 시간 경계를 둔다. 이 검증은 임의의 untrusted payload 전체를 안전하다고 선언하지 않으며, 현재 serializer의 bounded failure 관찰만 기록한다.
- Fory와 FastFory의 wire compatibility를 새로 약속하지 않는다. `SCHEMA_CONSISTENT`는 기존 비대칭·휘발성 캐시 전용 계약을 유지한다.

### 4. Redis와 Hibernate/Lettuce 사용 범위

- `RedisBinarySerializers.Fory`와 `RedisBinarySerializers.LZ4Fory`로 동일 payload를 serialize/deserialize하여 저장 바이트 경계를 검증한다.
- `RedisBinarySerializers.FastFory`는 현재 metadata round-trip과 함께 별도 확인하고, 기존 Fory 바이트와의 대칭 호환성을 주장하지 않는다.
- `spring-boot/hibernate-lettuce`는 Fory Kotlin runtime을 사용하지만, 이 PR에서 Hibernate cache runtime 설정이나 codec 구현은 변경하지 않는다. 기존 dependency 선언과 해당 모듈의 설정 경로를 source inspection 및 필요한 module test로 확인한다.

## 대안과 제외

- 기존 issue-754 fixture 재사용: 채택하지 않는다. manifest가 Fory `1.3.0`을 명시하여 변경 구간을 증명하지 못한다.
- catalog를 `1.6.0`으로 되돌린 뒤 테스트: 채택하지 않는다. current `1.7.1` 소비자 검증이 아니며 중앙 catalog 원본을 변경할 권한도 이 PR 범위에 없다.
- FastFory fallback 또는 wire migration 구현: 채택하지 않는다. #1639는 저장 바이트 호환성의 검증 이슈이고, `SCHEMA_CONSISTENT`/`COMPATIBLE` 계약 변경은 별도 설계가 필요하다.
- Redis/Testcontainers 실서버 통합: 채택하지 않는다. serializer가 생성하는 저장 바이트는 in-process `RedisSerializer`로 증명할 수 있고, 실제 Redis 서버를 추가해도 wire compatibility 결론이 넓어지지 않는다.

## 수락 기준

| ID | 완료 조건 | 증거 |
|---|---|---|
| AC-01 | Kotlin `2.4.10` data class의 nullable/default/collection/nested/value-class 사례가 현재 Fory `1.7.1`에서 round-trip된다 | `ForyWireCompatibilityTest` 결과와 필드 assertion |
| AC-02 | Fory `1.6.0`으로 생성한 고정 fixture를 `1.7.1`이 읽는다 | fixture, manifest, SHA-256, compatibility test |
| AC-03 | current `1.7.1` write/read와 old fixture read를 구분한다 | 별도 test와 manifest provenance |
| AC-04 | malformed/truncated small input이 제한된 테스트 시간 안에 `BinarySerializationException`으로 실패한다 | bounded failure tests |
| AC-05 | Fory/LZ4Fory/FastFory Redis serializer 경로가 payload round-trip을 유지한다 | `spring-boot/redis` targeted test |
| AC-06 | FastFory와 Fory 사이의 wire compatibility를 과장하지 않는다 | KDoc/테스트/lesson의 명시적 경계 |
| AC-07 | 변경 범위가 tests/fixture/provenance 문서로 제한되고 production serializer API는 불변이다 | `git diff`, compile/test, self-review |

## DoD와 후속 경계

- Type B Fast Track으로 수행한다. 새 모듈·의존성·production API는 추가하지 않는다.
- RED에서 새 테스트가 fixture/API/검증 누락으로 실패함을 확인한 뒤 최소 test/resource 변경으로 GREEN을 만든다.
- Kotlin testing/checklist, `git diff --check`, `:bluetape4k-io:test`,
  `:bluetape4k-spring-boot-redis:test`를 실행하고 실패·미실행 범위를 기록한다.
- P0/P1 finding은 0건이어야 한다. PR 생성까지는 부모 실행자가 담당하며 merge는 별도 exact-head 승인 후 진행한다.
