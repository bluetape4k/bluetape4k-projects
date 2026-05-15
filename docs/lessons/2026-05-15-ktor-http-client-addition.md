# Lessons Learned — Ktor Client addition to io/http (2026-05-15)

**관련 PR**: #454
**영향 모듈**: `:bluetape4k-http`

## L1: Ktor suspend-native 특성으로 executeSuspending 브리지 불필요

### 문제
기존 HC5/OkHttp3/Vert.x 백엔드는 blocking/async API를 suspend로 연결하는 브리지(`executeSuspending`)가 필요했다.

### 교훈
Ktor Client는 이미 suspend-native이므로 별도 브리지 없이 `client.get()` 등을 직접 coroutine 컨텍스트에서 호출 가능하다.
새로운 suspend-native 라이브러리를 추가할 때 기존 패턴(bridging)을 그대로 적용하지 않도록 주의.

---

## L2: Ktor Client CIO는 HTTP/2 미지원 — README에 명시 필수

### 문제
CIO 엔진이 HTTP/1.x만 지원한다는 사실을 문서화하지 않으면 HTTP/2 필요 팀이 잘못 채택할 수 있다.

### 교훈
백엔드 추가 시 지원하지 않는 프로토콜/기능을 README에 "intentionally not supported" 형태로 명시.
"Unsupported must be explicit" 원칙 적용.

---

## L3: compileOnly 백엔드 추가 시 버전 카탈로그 선 확인

### 문제
Ktor client 추가 전에 `ktor = "3.4.3"` 버전이 이미 카탈로그에 존재했다.
중복 버전 항목을 만들 뻔했다.

### 교훈
새 의존성 추가 전에 `grep -r "libraryname" gradle/libs.versions.toml` 로 기존 버전 항목 확인 필수.
버전 항목은 하나만 유지, 라이브러리 항목만 추가.

---

## L4: HttpClientConfig<*> 대신 CIOEngineConfig 명시 필요

### 문제
issue 제안 코드의 `HttpClientConfig<*>.() -> Unit`은 Ktor generic `<T : HttpClientEngineConfig>` 서명과 타입 불일치로 컴파일 오류 발생.

### 교훈
Ktor Client factory는 제네릭이다. CIO 전용 factory 함수는 `HttpClientConfig<CIOEngineConfig>.() -> Unit`으로 명시해야 컴파일.
Issue 제안 코드는 pseudocode일 수 있으므로 실제 API 시그니처 확인 후 구현.

---

## L5: Codex CLI oh-my-codex 인프라 오류 대응

### 문제
`omc ask codex` 실행 시 `patch-oh-my-codex: failed to remove co-author requirement` 오류로 2회 모두 실패.

### 교훈
Codex CLI가 사용 불가한 경우 Tier 4 Claude 리뷰로 대체 수행하고 DoD/PR 코멘트에 사유 명시.
`omc ask codex` 실패 시 artifact 파일(`.omc/artifacts/ask/`) 확인으로 exit code 및 오류 내용 파악 가능.
