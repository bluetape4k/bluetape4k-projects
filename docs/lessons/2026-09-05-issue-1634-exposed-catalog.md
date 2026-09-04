# #1634: Exposed 문서와 실제 catalog 소비 버전 맞추기

## 배경과 판단 수정

Projects의 개발 버전 `2.1.0-SNAPSHOT`, 최신 배포 버전 `2.0.0`,
JetBrains Exposed 의존성 버전은 서로 다른 값이다.
README는 Exposed `1.2.x`를 안내했고, 기존 기본 catalog `850959d0ea5f76ac7e2c442400f47653d5f95eed`는
Exposed `1.4.0`이었다. 승인된 중앙 catalog `9698c9d66bea6fcba373143ee8fa5bfbd9812d4b`의 값은 `1.5.0`이다.

Codex는 오래된 소비 값을 문서의 목표로 제안했고 버전 대상도 명확히 설명하지 않았다.
사용자의 “Exposed는 catalog의 1.5.0을 사용” 지적으로 이 판단을 수정했다.
오래된 고정값은 현재 상태의 증거이지, 승인된 업데이트 목표를 대체하는 기준이 아니다.

## 결정과 범위

- `settings.gradle.kts` 기본값과 CI의 SHA를 승인된 중앙 catalog에 함께 맞춘다.
- 로컬 `exposed` 버전 override나 중앙 catalog 복사본을 추가하지 않는다.
- 영어·한국어 README를 `1.5.0`으로 맞추고 실제 `batchInsert` 사용 예제로 연결한다.
- Exposed 기능 전체나 hashing API의 Projects 래퍼 지원을 주장하지 않는다.
- 프로젝트 릴리스 버전, 다른 저장소, 배포 설정은 변경하지 않는다.
- catalog SHA 갱신은 여러 의존성에 영향을 주므로 문서 전용 검증만으로 완료하지 않는다.

## 재발 방지 점검

1. 버전을 설명할 때 프로젝트, 외부 bluetape4k artifact, JetBrains Exposed를 구분한다.
2. 승인된 중앙 catalog와 소비 저장소의 기본 SHA, 환경·Gradle override, CI SHA를 따로 확인한다.
3. 두 README의 Exposed 값과 실제 소비 catalog의 `versions.exposed`를 비교한다.
4. 예제 Gradle 경로는 `settings.gradle.kts`의 등록 규칙이나 기존 CI에서 확인한다.
   첫 검증의 `:bluetape4k-redisson-demo`는 존재하지 않았으며, 실제 이름은
   `:bluetape4k-examples-redisson-demo`다. 수정한 명령을 실행한 뒤 문서에 기록한다.
5. workflow helper의 lane scope는 저장소 상대 경로다. 절대 경로 입력 거부 후
   상대 경로와 필수 agent/timestamp 인자로 복구하고 mutation-check를 확인한다.

## 검증

- 중앙 adoption checker: 통과. 로컬 alias catalog는 원본과 동일하며 변경하지 않았다.
- `actionlint .github/workflows/ci.yml`, `git diff --check`: 통과.
- 두 README에 catalog 갱신 시 SHA·`versions.exposed` 대조 절차를 추가했다.
- 실제 소비 버전 확인:

  ```bash
  ./gradlew :bluetape4k-examples-redisson-demo:dependencyInsight \
    --configuration testRuntimeClasspath --dependency org.jetbrains.exposed:exposed-core \
    --no-configuration-cache
  ```

  `exposed-core`, `exposed-dao`, `exposed-jdbc`, `exposed-java-time`,
  `exposed-spring-boot4-starter`, `spring7-transaction`이 모두 `1.5.0`으로 해석됐다.
- 예제 테스트: 99개 통과, 실패·오류·제외 0개(JUnit XML 집계).
- CI 의존성 그래프 계약 테스트: 17개 통과. catalog 변경은 `shared` 경로에 포함된다.
- 다운로드한 catalog의 SHA-256과 sidecar 일치:
  `622761bc3e518f052fe769c7fa057b3e1ec0cacd22ad9a871a9d1d8157120e0a`.
- README locale 값·추가 링크·settings/CI SHA 일치 검사와 한국어 용어 검사 통과.
- CI와 같은 범위의 `build -x test` 통과: 2분 21초, 674개 태스크
  (618 executed, 1 from cache, 55 up-to-date). CI와 동일하게
  `protobuf-codec-benchmark`, `serializer-benchmark`, `web-framework-benchmark`의 build를 제외했다.
  중앙 매뉴얼 루트는 `bluetape4k.github.io/docs/manual/bluetape4k-projects`를 사용했다.
- 빌드에 연결된 K3s 테스트 9개 통과. 일반 `test` 전체 실행을 대신하는 증거는 아니다.
- Gradle 10 호환성 경고와 외부 라이브러리의 JVM native/Unsafe 경고는 남아 있다.
- PR 생성·전체 CI·병합·배포: 아직 실행하지 않았다.
