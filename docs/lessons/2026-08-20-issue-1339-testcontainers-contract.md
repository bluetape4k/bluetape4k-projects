# 이슈 #1339 Testcontainers 태그·문서 계약 교훈 (2026-08-20)

관련 이슈: #1339 · Epic #1418 Slot 2
영향 module: `:bluetape4k-testcontainers`, `:bluetape4k-http`

## 맥락

Testcontainers 서버의 `IMAGE`·`TAG` 상수와 EN/KO README 기본 태그 표는 같은
기본 이미지를 설명하지만, KDoc 예제에는 이전 태그가 남아 있었다. MiniStack
`1.4.14`의 KMS Grant API 제한도 실제 `@Disabled` 테스트와 공개 KDoc/README의
지원 주장 사이에 불일치를 만들었다. HTTP 프로파일러 문서는 Java 25 전환 뒤에도
JDK 21을 기준으로 안내하고 있었다.

## 결정 또는 발견

1. 기본 태그 예제는 하드코딩하지 않고 각 서버 companion object의 `TAG`를
   참조한다. `@param tag` 기본값도 `[TAG]`로 연결해 태그 갱신 시 KDoc이 함께
   검토되도록 한다.
2. `scripts/test_testcontainers_contract.py`가 `*Server.kt`의 `IMAGE`·`TAG`와
   EN/KO README 표를 직접 비교하고, KDoc의 stale literal을 차단한다.
3. 고정된 MiniStack 태그는 KMS 핵심 기능만 지원하며 `CreateGrant`,
   `ListGrants`, `RevokeGrant`는 지원하지 않는다. 이 경계를 KDoc, EN/KO
   README, 비활성화 테스트의 설명과 동일하게 유지한다.
4. `io/http` 프로파일러 요구 사항은 프로젝트 기본 툴체인인 JDK 25로 맞춘다.

## 결과

문서와 KDoc은 현재 이미지 태그 및 MiniStack capability를 설명하고, README
두 locale은 같은 기본 태그와 제한 문구를 제공한다. 생산 코드의 실행 동작과
컨테이너 이미지는 변경하지 않는다.

## 검증

`python3 -m unittest scripts/test_testcontainers_contract.py -v`로 Kotlin
상수·README 표·KDoc·JDK baseline·MiniStack 제한을 검증한다. `git diff --check`,
Kotlin 컴파일, 한국어 용어 감사를 추가로 실행하고 결과를 PR DoD에 기록한다.

## 향후 지침

- 새 `*Server`를 추가하거나 `TAG`를 변경할 때 Kotlin 상수, EN/KO 표, KDoc
  예제를 함께 갱신하고 계약 검사를 실행한다.
- 에뮬레이터 capability를 “전체 지원”으로 확대해 쓰기 전에 고정 이미지에서
  실제 API 테스트가 활성화되는지 확인한다.
- Java toolchain을 올릴 때 모듈 README의 실행 요구 사항과 예제도 같은
  stacked slot에서 갱신한다.
