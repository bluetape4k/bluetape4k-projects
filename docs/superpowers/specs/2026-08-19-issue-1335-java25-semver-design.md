# #1335 Java 25 기본 classfile과 2.0.0 SemVer 호환성 계약 설계

- Epic: [#1418](https://github.com/bluetape4k/bluetape4k-projects/issues/1418)
- Slot: 1/4
- Issue: [#1335](https://github.com/bluetape4k/bluetape4k-projects/issues/1335)
- 선행 슬롯: 없음
- 후속 슬롯: [#1339](https://github.com/bluetape4k/bluetape4k-projects/issues/1339)
- 기준 브랜치: `develop`
- 작업 브랜치: `chore/1418-01-java25-semver`

## 결정 요약

현재 일반 모듈은 Java 25 classfile(major 69)을 생성하고, 다섯 개의 명시적인 Java 21 호환성 섬만 Java 21 classfile(major 65)을 생성한다. 이 런타임 바닥선 상승을 호환성 변경으로 분류하고 `baseVersion=2.0.0`으로 전환한다. Java 21 호환성 섬은 유지하지만 일반 모듈 전체의 Java 21 호환성을 약속하지 않는다.

이번 Slot은 실제 release tag, Maven Central 배포, 새 API, 새 모듈을 만들지 않는다. 버전·문서·검증·release workflow 계약을 고정해 후속 stacked slot이 같은 경계를 재해석하지 않도록 하는 것이 목적이다.

## 배경과 근거

- 현재 `gradle.properties`의 `baseVersion`은 `1.13.0`이다.
- 루트 Gradle 설정은 Java 25를 기본 target으로 사용한다.
- 다음 다섯 모듈은 Java 21 호환성 섬으로 중앙 설정에 명시되어 있다.
  - `bluetape4k-assertions`
  - `bluetape4k-junit5`
  - `bluetape4k-logging`
  - `bluetape4k-virtualthread-api`
  - `bluetape4k-virtualthread-jdk21`
- 기준 커밋에서 대표 classfile은 일반 모듈 major 69, Java 21 섬 major 65로 확인된다.
- #1323에서 Java 25 기본 toolchain과 Java 21 호환성 섬의 target 경계를 이미 중앙화했다. 이번 변경은 그 결정을 되돌리거나 섬을 넓히지 않는다.

이 설계에서 SemVer major bump는 source API의 변경만을 의미하지 않는다. 일반 published artifact를 로드할 수 있는 최소 JVM이 Java 21–24에서 Java 25로 올라가므로, 해당 소비자에게 명시적인 migration이 필요한 공개 호환성 경계로 취급한다.

## 목표

1. `2.0.0` 릴리스 계약을 Gradle version source of truth에 반영한다.
2. 일반 artifact와 Java 21 호환성 섬의 런타임 경계를 영어·한국어 문서에 동일하게 설명한다.
3. version, module target, classfile major, 문서 marker의 drift를 CI에서 조기에 차단한다.
4. release workflow가 publish 전에 같은 JVM compatibility contract를 재검증하도록 한다.
5. 다음 stacked slot(#1339)이 이 Slot의 merge head를 정확히 기준으로 작업할 수 있게 변경 범위를 작게 유지한다.

## 비목표

- 실제 `2.0.0` Git tag 생성 또는 Maven Central release publish
- 일반 모듈을 Java 21 target으로 되돌리거나 Java 21 섬을 추가·확장하는 작업
- Testcontainers TAG/KDoc 수정(#1339)
- `PropertyExportingServer`의 Spring `DynamicPropertyRegistry` 연동(#1321)
- image family startup/workload gate(#1337)
- 새 public API, dependency, module 추가
- 기존 1.13.x artifact의 backport 또는 별도 compatibility variant publish

## 설계

### 1. 버전 및 JVM target 계약

`gradle.properties`의 `baseVersion`을 `2.0.0`으로 설정하고 `snapshotVersion`은 빈 값으로 유지한다. publication 메커니즘은 기존 `baseVersion + snapshotVersion` 계산을 그대로 사용한다.

루트 `build.gradle.kts`의 중앙 target 계산과 Java 21 호환성 섬 목록은 유지한다.

| 대상 | target | 기대 classfile | 공개 의미 |
| --- | ---: | ---: | --- |
| 일반 library module | Java 25 | major 69 | `2.0.0`부터 Java 25 runtime 필요 |
| Java 21 호환성 섬 5개 | Java 21 | major 65 | Java 21 runtime 소비자를 위한 명시적 예외 |

새로운 모듈이 호환성 섬에 들어가려면 별도 이슈와 compatibility 근거가 필요하며, 이번 Slot에서 암묵적으로 추가하지 않는다.

### 2. 독자-facing 문서

다음 문서를 함께 갱신한다.

- `README.md`
- `README.ko.md`
- `CHANGELOG.md`

두 README에는 다음 내용을 같은 의미와 marker로 둔다.

- 기본 개발·실행 baseline은 Java 25, Kotlin 2.4, Gradle 9.7이다.
- 일반 artifact는 Java 25 runtime에서 실행한다.
- Java 21 호환성 섬은 다섯 모듈로 한정되며 일반 artifact 전체의 Java 21 호환성을 뜻하지 않는다.
- Java 21–24에서 일반 artifact를 소비하던 사용자는 JDK 25로 이동하거나 1.13.x를 유지한다.
- Java 21을 유지해야 하는 사용자는 호환성 섬 모듈만 선택하고 Java 25 target artifact를 classpath에 섞지 않는다.

기존 publishing 예제의 stale `baseVersion=1.11.0`은 `2.0.0` 기준으로 바꾼다. README의 실행 명령과 publication 명령은 변경하지 않는다.

`CHANGELOG.md`에는 실제 배포일을 넣지 않는 `Unreleased` 항목을 추가한다. 항목에는 #1335 링크, runtime floor 상승, Java 21 섬 유지, migration 선택지를 기록한다. 실제 release heading/date는 tag 작업에서 별도로 결정한다.

### 3. 정적 release contract test

새 `scripts/test_jvm_release_contract.py`는 표준 library test runner 없이 실행할 수 있는 Python `unittest`로 작성한다. 최소 검사는 다음과 같다.

- `gradle.properties`가 정확히 `baseVersion=2.0.0`이고 `snapshotVersion`이 비어 있는지 확인
- `build.gradle.kts`의 Java 21 호환성 섬이 정확히 다섯 모듈인지 확인
- root target 계산이 일반 모듈 Java 25, 섬 Java 21을 선택하는지 확인
- `README.md`와 `README.ko.md`가 동일한 migration marker와 version contract를 포함하는지 확인
- `CHANGELOG.md`의 `Unreleased`에 #1335와 Java 25/Java 21 migration contract가 있는지 확인
- stale `baseVersion=1.11.0` publication 예제가 남아 있지 않은지 확인
- `.github/workflows/ci.yml`와 `.github/workflows/release.yml`이 이 계약 검증을 호출하는지 확인

검사는 파일 전체의 문구 수를 임의로 고정하지 않고, 의미가 있는 marker와 정확한 version·module 집합을 검증한다. 따라서 문서 표현을 자연스럽게 다듬어도 계약 검사는 유지된다.

### 4. classfile release contract check

새 `scripts/check-jvm-release-contract.sh`는 다음을 순서대로 수행한다.

1. 대표 일반 모듈과 Java 21 섬, `virtualthread-jdk25`의 compile task를 실행한다.
2. 안정적인 대표 class 파일을 찾는다.
3. `javap -verbose` 출력의 `major version`을 읽어 일반 모듈/Java 25 구현은 69, Java 21 섬은 65인지 확인한다.
4. 누락 class, 복수 후보, 예상 외 major는 오류로 종료한다.

대표 class 선택은 현재 source의 안정적인 public implementation을 사용한다. 검사는 모든 모듈을 중복 compile하지 않고, 중앙 JVM target 경계가 실제 산출물에 반영되는 최소 representative set만 확인한다.

### 5. CI 및 release workflow 연계

`.github/workflows/ci.yml`에는 정적 Python contract test와 classfile check를 실행하는 검증 단계를 추가한다. 기존 release workflow policy, publication inventory, metadata 검사는 그대로 유지한다.

`.github/workflows/release.yml`의 Java 25 setup과 tag↔`baseVersion` 일치 검사는 유지한다. Maven Central publish task보다 앞에 JVM release contract check를 두어, 버전이 맞더라도 classfile/document contract가 깨진 artifact는 publish하지 않는다.

검증 단계는 signing credential, GitHub release 생성, tag 생성 등의 side effect를 수행하지 않는다. 실패하면 publish step에 도달하지 않는다.

## 마이그레이션 계약

### 일반 artifact 소비자

- Java 25 runtime으로 이동한 뒤 `2.0.0`을 사용한다.
- 당장 Java 25로 이동할 수 없으면 `1.13.x`를 유지한다.
- `2.0.0` 일반 artifact를 Java 21–24 runtime에서 실행할 수 있다고 가정하지 않는다.

### Java 21 유지 소비자

- Java 21 호환성 섬 다섯 모듈만 선택한다.
- Java 25 target의 일반 artifact를 같은 classpath에 섞지 않는다.
- 다른 bluetape4k 모듈이 필요하면 해당 모듈의 published classfile baseline을 확인한다.

이 Slot은 dependency resolution을 자동으로 바꾸지 않는다. 소비자의 버전 고정·JDK 전환·모듈 선택은 application migration 작업으로 남긴다.

## Stacked PR train 경계

- PR branch: `chore/1418-01-java25-semver`
- base: `develop`의 Slot 시작 시점 exact head
- issue: #1335만 연결
- PR #1 merge 전에는 #1339 branch/PR을 만들거나 구현하지 않는다.
- PR #1 merge 후 successor branch는 merge commit exact head에서 생성한다.
- 각 PR은 자신이 소유한 issue와 DoD만 수정하며 후속 issue의 파일을 선점하지 않는다.

## 검증 계획

구현 후 다음 명령을 순서대로 실행한다.

```bash
python3 -m unittest scripts/test_jvm_release_contract.py -v
./scripts/check-jvm-release-contract.sh
python3 -m unittest scripts/test_release_workflow_policy.py -v
./gradlew :bluetape4k-testcontainers:compileKotlin \
  :bluetape4k-virtualthread-jdk21:compileKotlin \
  :bluetape4k-virtualthread-jdk25:compileKotlin \
  --no-configuration-cache
git diff --check
node ~/.codex/skills/bluetape-writer/scripts/audit-korean-terms.mjs \
  README.md README.ko.md CHANGELOG.md
```

실행 환경이 Docker/Testcontainers를 요구하지 않는 Slot이므로, 해당 모듈의 integration test를 성공 증거로 가장하지 않는다. classfile compile과 static contract가 이 변경의 직접적인 검증이다.

## Slot 1 DoD

- [ ] `baseVersion=2.0.0`, empty `snapshotVersion`
- [ ] 일반/Java 21 섬 target과 representative classfile major 검증
- [ ] README EN/KO parity 및 migration 문단
- [ ] `CHANGELOG.md` Unreleased/#1335 기록
- [ ] CI/release workflow contract hook
- [ ] 정적 test, classfile check, release policy test, compile, diff check, 한국어 용어 감사 통과
- [ ] PR body 마지막 섹션이 `## DoD Status`이고 `Required checks: X/Y; N/A: N; Blocked: N`을 포함
- [ ] Epic #1418 진행률을 1/4로 보고

## 위험과 대응

| 위험 | 탐지 | 대응 |
| --- | --- | --- |
| version source와 tag 불일치 | release workflow tag check | publish 중단, tag 작업 전 version 수정 |
| 일반 모듈이 Java 21로 잘못 생성됨 | representative major check | 중앙 target 설정을 복구하고 재검증 |
| 호환성 섬이 무심코 확대됨 | 정확한 module-set static test | 섬 외부 target 변경을 revert하거나 별도 이슈로 분리 |
| EN/KO migration 문구 drift | marker parity test 및 writer audit | 두 locale을 같은 변경에서 수정 |
| 후속 stacked slot이 미merge base에서 시작됨 | PR base/head live read-back | PR 생성·구현을 hold하고 선행 merge head에서 재생성 |

## 대안과 기각 이유

### `1.13.0` 유지 + published target 분리

Java 21 artifact를 별도 target으로 배포하면 runtime floor를 유지할 수 있지만, 일반 모듈 전체의 target 분리·artifact variant·publication matrix가 새로 필요하다. 현재 #1323이 이미 Java 25 기본 target과 제한된 Java 21 섬을 중앙화했으므로, 이번 Slot에서 이 구조를 뒤집는 것은 범위를 넓히고 소비자 계약을 더 모호하게 만든다.

### 모든 모듈을 Java 21 target으로 회귀

Java 25 API·toolchain을 사용하는 현재 개발 baseline과 충돌하고, 이미 확정된 Java 25 기본 classfile 계약을 훼손한다. 선택하지 않는다.

### release tag에서만 version 변경

tag 단계까지 version·문서·CI contract drift를 발견하지 못하므로, stacked train의 선행 검증 증거가 약해진다. source of truth를 PR에서 먼저 고정한다.

## 완료 조건

이 설계 문서와 구현 계획이 승인되고, Slot 1의 모든 검증이 fresh exact head에서 통과하며, PR #1335의 최신 review blocker가 없고 merge approval이 새로 확인될 때만 후속 #1339 슬롯으로 이동한다.
