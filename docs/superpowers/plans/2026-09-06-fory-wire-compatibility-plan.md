# #1639 Fory 1.7.1 Kotlin metadata와 Redis 저장 바이트 호환성 실행 계획

## 실행 기준

- 저장소: `bluetape4k/bluetape4k-projects`
- base: `develop@9811932427aac4c6cfba7b16ae3def215e93a2fe`
- branch: `test/issue-1639-fory-wire-compatibility`
- issue: `#1639`, milestone `2.1.0`, assignee `debop`
- 승인 설계: `docs/superpowers/specs/2026-09-06-fory-wire-compatibility-design.md`
- PR 권한: 부모 실행자가 `bluetape4k/bluetape4k-projects`의 base `develop`, 이 branch를 head로 PR을 생성한다. 이 lane에서는 push/PR/merge를 수행하지 않는다.

## 변경 파일과 작업 순서

### 1. 테스트 계약을 먼저 작성하고 RED 확인

생성/수정 대상:

- `io/io/src/test/kotlin/io/bluetape4k/io/serializer/ForyWireCompatibilityTest.kt`
- `spring-boot/redis/src/test/kotlin/io/bluetape4k/spring/redis/serializer/RedisForyWireCompatibilityTest.kt`

테스트 계약:

- 현재 `BinarySerializers.Fory`의 Kotlin metadata payload round-trip
- nullable/default/collection/nested/value-class field equality
- `1.6.0` fixture manifest 로드와 `1.7.1` 역직렬화
- current write/read와 old fixture read의 분리
- malformed bytes와 fixture truncation의 `BinarySerializationException`
- `RedisBinarySerializers.Fory`, `LZ4Fory`, `FastFory` 저장 바이트 round-trip

실행:

```bash
./gradlew :bluetape4k-io:test \
  --tests 'io.bluetape4k.io.serializer.ForyWireCompatibilityTest' \
  --no-configuration-cache
```

예상 RED: 새 fixture 또는 테스트 대상 계약이 아직 없어 compile/resource assertion이 실패한다. 실패가 typo나 classpath 오류라면 테스트를 고친 뒤 feature-missing RED를 다시 확인한다.

### 2. Fory `1.6.0` fixture를 고정하고 provenance 기록

생성 대상:

- `io/io/src/test/resources/compat/issue-1639/fory-1.6.0/fory-kotlin-metadata.bin`
- `io/io/src/test/resources/compat/issue-1639/fory-1.6.0/manifest.json`
- 필요한 재생성 source/command 기록 파일

직전 중앙 catalog commit에서 Fory/Fory Kotlin `1.6.0`임을 확인하고, current payload와 동일한 class name/field shape 및 `ForyBinarySerializer` builder 옵션으로 단 한 번 생성한다. binary bytes는 manifest의 size/SHA-256과 일치해야 한다.

검증:

```bash
sha256sum io/io/src/test/resources/compat/issue-1639/fory-1.6.0/fory-kotlin-metadata.bin
```

### 3. 최소 test support만 추가하고 GREEN 확인

production serializer와 catalog는 변경하지 않는다. fixture manifest reader, SHA-256 helper, test payload 및 Redis test adapter는 테스트 source 안에 둔다. 새로운 public API, FastFory fallback, cache migration policy는 추가하지 않는다.

실행 순서:

```bash
./gradlew :bluetape4k-io:test \
  --tests 'io.bluetape4k.io.serializer.ForyWireCompatibilityTest' \
  --no-configuration-cache

./gradlew :bluetape4k-spring-boot-redis:test \
  --tests 'io.bluetape4k.spring.redis.serializer.RedisForyWireCompatibilityTest' \
  --no-configuration-cache
```

기대 결과는 두 module의 선택된 테스트가 모두 통과하는 것이다. Testcontainers는 사용하지 않으며, 실제 Redis 서버를 시작하지 않는다.

### 4. 전체 영향과 정적 검증

```bash
./gradlew :bluetape4k-io:compileTestKotlin :bluetape4k-spring-boot-redis:compileTestKotlin \
  --no-configuration-cache

./gradlew :bluetape4k-io:test :bluetape4k-spring-boot-redis:test \
  --no-configuration-cache --no-parallel

./gradlew :bluetape4k-io:detekt :bluetape4k-spring-boot-redis:detekt \
  --no-configuration-cache

git diff --check
```

실패 시 최초 실패 task와 예외를 기록하고 관련 scope만 수정한다. 전체 module 또는 detekt를 실행할 수 없는 경우 명령·환경·대체 검증을 lesson과 부모 보고에 남긴다.

### 5. 최종 review와 handoff

- 변경 파일이 위 scope와 정확히 일치하는지 확인한다.
- `FastFory`의 `SCHEMA_CONSISTENT`와 Fory `COMPATIBLE` 사이에 wire compatibility를 주장하는 문장이나 assertion이 없는지 확인한다.
- old fixture read 결과가 manifest의 payload와 일치하고 fixture hash가 재현되는지 확인한다.
- Kotlin final checklist에서 P0/P1=0, diagnostics/deprecation=0, targeted test와 diff check 결과를 기록한다.
- lesson을 실제 RED/GREEN 관찰과 남은 gap으로 갱신한다.
- 한국어 Lore commit을 생성하되 push/PR/merge는 부모 실행자에게 넘긴다.

## 복구와 재실행

- fixture hash 또는 class shape가 바뀌면 해당 compatibility evidence를 폐기하고 old generator를 같은 version pin으로 재실행한다.
- 테스트 실패를 숨기기 위해 current catalog를 old version으로 바꾸지 않는다.
- RED가 feature missing이 아니라 환경 오류면 환경을 진단하고, 테스트 계약을 유지한 채 재실행한다.
- 이 lane에서 branch deletion, remote force-push, 외부 repository 변경은 수행하지 않는다.
