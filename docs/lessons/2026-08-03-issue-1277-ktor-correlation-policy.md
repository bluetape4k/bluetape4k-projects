# 교훈: 이슈 #1277 Ktor correlation 정책 상속과 trace override

## 배경

통합 Ktor observability installer는 top-level `CorrelationIdSettings`를
CallId와 CallLogging에 전달했지만, tracing 설정에는 별도의 기본 정책을
생성했습니다. tracing이 `call.callId`를 먼저 읽기 때문에 trace 전용 header와
`maxLength`를 명시해도 애플리케이션 correlation ID가 span attribute를 덮어쓸 수
있었습니다.

## 결정

- `KtorOpenTelemetryTracingConfig.correlationId == null`을 통합 installer의
  top-level 정책을 상속한다는 의미로 사용합니다.
- combined installer는 상속 정책을 tracing helper에 전달합니다.
- 명시적인 trace 정책은 해당 request header만 읽어 top-level CallId 정책과
  독립적으로 정제합니다.
- standalone tracing에서 정책을 생략하면 기존처럼 CallId 값을 우선하고, 없으면
  기본 request header를 확인합니다.

## 결과

CallId, CallLogging, 응답 전파, tracing이 기본 correlation 정책을 공유하면서도,
trace 전용 header와 길이 제한을 지정한 사용자는 명시한 override를 유지합니다.
KDoc과 EN/KO README에 상속 및 override 규칙을 함께 기록했습니다.

## 향후 방지책

관측성 plugin을 조합할 때는 정책의 상속·override 여부를 nullable 설정 또는
동등한 명시적 상태로 표현하고, `call.callId` fallback이 명시 override를
덮어쓰지 않는지 확인합니다. custom header와 non-default `maxLength`를 사용하는
combined installer regression test를 유지합니다.

## 검증

- RED: explicit trace override 테스트가 top-level `applicat`를 반환해 trace 전용
  기대값과 불일치함을 확인했습니다.
- GREEN: inherited/explicit correlation 정책 회귀 테스트 2개 통과.
- `repo-test-summary -- ./gradlew :bluetape4k-ktor-observability:cleanTest :bluetape4k-ktor-observability:test --rerun-tasks --no-build-cache --no-configuration-cache`: 22 passing.
- `./gradlew :bluetape4k-ktor-observability:detekt --no-build-cache --no-configuration-cache`: `BUILD SUCCESSFUL`; 기존 파일의 경고성 규칙 위반만 보고됨.
- `git diff --check`: 통과.
