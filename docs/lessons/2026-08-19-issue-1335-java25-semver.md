# 이슈 #1335 Java 25 SemVer 계약 교훈 (2026-08-19)

## 맥락

`baseVersion`을 `1.13.0`에서 `2.0.0`으로 올리면서 기본 JVM 실행 기준을
Java 25로 전환했다. 그러나 `virtualthread/jdk21`을 사용하는 소비자는
Java 21 classfile을 계속 필요로 하므로, 버전 문자열만 바꾸는 작업으로는
호환성 계약을 증명할 수 없었다.

## 결정과 발견

1. 기본 artifact는 Java 25 classfile major `69`를 생성하고, 다음 다섯 모듈만
   Java 21 compatibility island major `65`를 유지한다: `bluetape4k-assertions`,
   `bluetape4k-junit5`, `bluetape4k-logging`, `bluetape4k-virtualthread-api`,
   `bluetape4k-virtualthread-jdk21`.
2. `gradle.properties`, EN/KO README, `CHANGELOG.md`, 정적 contract test를
   하나의 계약으로 묶는다. README에는 `2.0.0` 또는 Java 21 island 선택이라는
   migration 경계를 명시하고, stale한 `1.11.0` publishing 예제는 제거한다.
3. 문서 검증만으로는 잘못된 target을 놓칠 수 있으므로 `javap -verbose`로
   representative classfile을 직접 읽는다. `scripts/check-jvm-release-contract.sh`
   는 publish 전에 이 검사를 수행하고 CI와 release workflow 양쪽에서 호출한다.
4. mock-web-server와 mock-webflux-server의 Testcontainers 기본 이미지 태그도
   `baseVersion`과 일치해야 한다. Jib가 생성하는 `2.0.0` 태그와 오래된
   `1.13.0` 상수가 어긋나면 Docker 이미지를 찾지 못해 HTTP 테스트가 실패한다.

## 결과와 검증

- `baseVersion=2.0.0`, `snapshotVersion` 빈 값, EN/KO marker parity가 고정됐다.
- `python3 -m unittest scripts/test_jvm_release_contract.py -v`: 6/6 PASS.
- `python3 -m unittest scripts/test_release_workflow_policy.py -v`: 7/7 PASS.
- `./scripts/check-jvm-release-contract.sh`: Gradle `BUILD SUCCESSFUL`,
  representative major `69/65/69` PASS.
- `actionlint`, `git diff --check`, 한국어 용어 감사가 PASS했다. hosted CI와
  merge는 이 lesson 작성 시점에 아직 완료되지 않았으므로 이 문서는 로컬
  계약 검증만 증명한다.

## 놓친 점과 보완

초기 문서 초안에는 historical Maven `snapshot` 용어가 일반 문장처럼 남아
있어 용어 감사에서 기술 토큰과 독자용 문장을 구분하지 못했다. 의미는 바꾸지
않고 해당 토큰만 code span으로 고정한 뒤 전체 감사와 read-back을 다시 실행했다.
또한 첫 hosted CI에서 `BluetapeHttpServer.TAG`와 Jib의 `project.version`이
각각 `1.13.0`과 `2.0.0`을 가리키는 불일치를 확인했다. 두 기본 태그를
`2.0.0`으로 맞추고 정적 contract test가 두 소스와 `baseVersion`을 함께
검증하도록 보완했다.

## 향후 지침

- JVM 기본값이나 `baseVersion`을 바꿀 때는 dependency closure로 계산한 Java 21
  island, 실제 classfile major, migration 문서를 함께 갱신한다.
- Docker 이미지를 Jib로 version tag하는 모듈은 Testcontainers 기본 tag와
  `baseVersion`의 parity를 함께 검증한다.
- 정적 문서 검증과 `javap` classfile 검증을 CI와 release workflow의 publish 전
  경계에 모두 둔다.
- Slot 1의 exact head가 merge되고 fresh CI/review 증거가 생기기 전에는
  후속 #1339 branch와 PR을 만들지 않는다.
