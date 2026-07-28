# 배운 점 — Ktor Client addition to io/http (2026-05-15)

**관련 PR**: #454
**영향 모듈**: `:bluetape4k-http`

## L1: Ktor suspend-native 특성으로 executeSuspending 브리지 불필요

### 문제
기존 HC5/OkHttp3/Vert.x backend는 blocking/async API를 suspend로 연결하는 bridge(`executeSuspending`)가 필요했다.

### 교훈
Ktor Client는 이미 suspend-native이므로 별도 bridge 없이 `client.get()` 등을 직접 coroutine context에서 호출할 수 있다.
새로운 suspend-native library를 추가할 때 기존 pattern(bridging)을 그대로 적용하지 않는다.

---

## L2: Ktor Client CIO는 HTTP/2 미지원 — README에 명시 필수

### 문제
CIO engine이 HTTP/1.x만 지원한다는 사실을 문서화하지 않으면 HTTP/2가 필요한 팀이 잘못 채택할 수 있다.

### 교훈
Backend 추가 시 지원하지 않는 protocol/function을 README에 "intentionally not supported" 형태로 명시한다.
"지원하지 않는 기능은 명시해야 한다" 원칙을 적용한다.

---

## L3: compileOnly 백엔드 추가 시 버전 카탈로그 선 확인

### 문제
Ktor client 추가 전에 `ktor = "3.4.3"` version이 이미 catalog에 존재했다.
중복 version 항목을 만들 뻔했다.

### 교훈
새 dependency를 추가하기 전에 `grep -r "libraryname" gradle/libs.versions.toml`로 기존 version 항목을 반드시 확인한다.
Version 항목은 하나만 유지하고 library 항목만 추가한다.

---

## L4: HttpClientConfig<*> 대신 CIOEngineConfig 명시 필요

### 문제
Issue 제안 code의 `HttpClientConfig<*>.() -> Unit`은 Ktor generic `<T : HttpClientEngineConfig>` signature와 type mismatch가 있어 compile error가 발생했다.

### 교훈
Ktor Client factory는 generic이다. CIO 전용 factory function은 `HttpClientConfig<CIOEngineConfig>.() -> Unit`으로 명시해야 compile된다.
Issue 제안 code는 pseudocode일 수 있으므로 실제 API signature를 확인한 뒤 구현한다.

---

## L5: Codex CLI oh-my-codex 인프라 오류 대응

### 문제
`omc ask codex` 실행 시 `patch-oh-my-codex: failed to remove co-author requirement` error로 2회 모두 실패했다.

### 교훈
Codex CLI를 사용할 수 없는 경우 Tier 4 Claude review로 대체하고 DoD/PR comment에 사유를 명시한다.
`omc ask codex` 실패 시 artifact file(`.omc/artifacts/ask/`)을 확인해 exit code와 error 내용을 파악한다.
