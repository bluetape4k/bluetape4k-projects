# outbound 오류 정제 공용 API 구현 계획

> **For agentic workers:** 이 계획은 Type-A gate를 통과한 뒤 task-by-task로 실행한다. 각 단계는 체크박스로 추적하고 Kotlin pattern/TDD 규칙을 따른다.

**Goal:** `bluetape4k-http`에 credential-safe outbound 오류 문자열 sanitizer를 추가한다.

**Architecture:** `io.bluetape4k.http.sanitizeOutboundError`는 status prefix와 한 줄 메시지를 조합하는 순수 함수다. credential-like key를 case-insensitive regex로 redaction하고, caller의 retry/status/transaction/cancellation을 전혀 소유하지 않는다.

**Tech Stack:** Kotlin 2.x, JUnit 5, bluetape4k assertions, Gradle module `:bluetape4k-http`, Spring/Ktor downstream compile/test.

---

## 변경 파일 지도

- Create: `io/http/src/main/kotlin/io/bluetape4k/http/OutboundErrorSanitizer.kt` — public pure API and KDoc.
- Create: `io/http/src/test/kotlin/io/bluetape4k/http/OutboundErrorSanitizerTest.kt` — exact contract and no-secret tests.
- Modify: `io/http/README.md` — English API contract/example.
- Modify: `io/http/README.ko.md` — Korean equivalent contract/example.
- No provider dependency, catalog, Spring/Ktor source, or workflow changes in this provider PR.

### Task 1: RED 테스트로 redaction contract를 고정한다

**Files:**
- Create: `io/http/src/test/kotlin/io/bluetape4k/http/OutboundErrorSanitizerTest.kt`

- [ ] **Step 1: exact output cases 작성**

  `null/blank는 HTTP prefix만 반환`, `첫 줄과 양 끝 공백을 trim`, `Authorization/Cookie/Token/Secret/API-Key`의 대소문자·`:`·`=`·Bearer·space/hyphen/underscore 변형을 `[redacted]`로 바꿈`, `여러 credential을 모두 치환`, `multiline은 첫 줄만 남김`을 exact string으로 검증한다.

- [ ] **Step 2: boundary/security cases 작성**

  `결과가 정확히 240 UTF-16 Char 이내이며 surrogate pair를 자르지 않음`, `Int status prefix가 항상 보존됨`, `Unicode 문자를 포함해 경계가 안전함`, `malformed marker/empty Bearer는 status-only fail-closed`, `입력 secret이 결과에 절대 포함되지 않음`을 검증한다.

- [ ] **Step 3: RED 실행**

  Run: `./gradlew :bluetape4k-http:test --tests "io.bluetape4k.http.OutboundErrorSanitizerTest"`

  Expected: sanitizer 함수가 없어 compile failure.

### Task 2: 최소 public sanitizer 구현

**Files:**
- Create: `io/http/src/main/kotlin/io/bluetape4k/http/OutboundErrorSanitizer.kt`

- [ ] **Step 1: constants와 credential regex 작성**

  `MAX_LENGTH = 240`, `credentialPattern = Regex("(?i)\\b(authorization|cookie|token|secret|api[-_ ]?key)\\b\\s*[:=]\\s*(?:Bearer\\s+)?[^\\s]+")`를 private immutable constant로 둔다. 별도의 marker regex로 key-value가 malformed/empty인지 검사해 해당 first line을 status-only로 버린다. replacement는 captured key의 표기를 유지하는 `"\$1:[redacted]"`를 사용한다.

- [ ] **Step 2: 순수 함수 구현**

  `prefix = "HTTP $statusCode"`를 만들고 `rawMessage?.lineSequence()?.firstOrNull()?.trim()`을 redaction한 뒤 malformed marker이면 버린다. `MAX_LENGTH - prefix.length`를 `coerceAtLeast(0)`으로 계산하고, surrogate-safe helper로 body를 자른다. blank/empty는 prefix only, 모든 반환 경로는 prefix를 보존한다. 함수는 로그·예외·외부 상태를 만들지 않는다.

- [ ] **Step 3: GREEN 실행**

  Run: `./gradlew :bluetape4k-http:test --tests "io.bluetape4k.http.OutboundErrorSanitizerTest"`

  Expected: sanitizer unit tests PASS.

### Task 3: provider 경계와 publication surface 확인

- [ ] **Step 1: compile/static checks**

  Run: `./gradlew :bluetape4k-http:compileKotlin :bluetape4k-http:compileTestKotlin :bluetape4k-http:detekt`

  Expected: Kotlin compile/detekt PASS; sanitizer source imports only Kotlin/JDK APIs.

- [ ] **Step 2: JAR/POM/module metadata 생성**

  Run: `./gradlew :bluetape4k-http:jar :bluetape4k-http:generateMetadataFileForMavenJavaPublication :bluetape4k-http:generatePomFileForMavenJavaPublication`

  Expected: public function appears in JAR and no Spring/Ktor dependency is introduced by the new file or provider POM.

- [ ] **Step 3: security scan evidence**

  Run: `git grep -n -E 'secret-token|Authorization.*\[redacted\]' -- io/http/src`; run `gitleaks detect --source . --redact --no-git --config .gitleaks.toml` when the gitleaks binary is available; run `./gradlew dependencyCheckAnalyze --no-daemon` and record its advisory result separately because the provider workflow marks Dependency Check `continue-on-error: true`.

  Expected: no hard-coded credential in production source; advisory scans are recorded as evidence and not silently treated as blocking pass when CI marks them `continue-on-error`.

### Task 4: locale documentation

**Files:**
- Modify: `io/http/README.md`
- Modify: `io/http/README.ko.md`

- [ ] **Step 1: identical API section 추가**

  두 README에 root-package import, status-only/null, first-line, supported credential patterns, `[redacted]`, 240-character limit, caller ownership example을 같은 순서로 추가한다.

- [ ] **Step 2: docs validation**

  Run: `git diff --check` and inspect code fences/English-Korean structure side by side.

### Task 5: downstream compatibility proof (publication hold)

- [ ] **Step 1: verify current consumer call sites**

  Before provider publication, only read/record Spring and Ktor call sites. Do not edit them in this branch. Confirm status/retry/transaction/cancellation remain outside the sanitizer and record that caller tests must log/persist the sanitized result rather than the original Throwable.

- [ ] **Step 2: record dependency hold**

  The consumer migration waits for provider exact-head publication and central BOM/catalog update. This is an explicit PENDING gate, not a reason to add a direct arbitrary version to workshop.

### Task 6: verifier and rollback

- [ ] **Step 1: full provider validation**

  Run: `./gradlew :bluetape4k-http:test :bluetape4k-http:koverVerify`

  Expected: targeted and module regression tests PASS.

- [ ] **Step 2: scope/diff validation**

  Run: `git diff --check`, `git status --short`, `git diff --name-only origin/develop...HEAD`

  Expected: only provider source/test/README files in the change map; no workshop/catalog mutation.

- [ ] **Step 3: rollback point**

  If API behavior or publication metadata fails, revert the latest provider implementation/documentation commits and keep workshop #234 blocked until the spec/plan are re-reviewed.

## 수용 기준 추적

| Spec 기준 | 검증 task |
| --- | --- |
| prefix/first-line/null/blank/240 | Task 1, 2 |
| all credential variants and malformed input | Task 1, 2 |
| no raw secret and no exception leakage | Task 1, 2, 3 |
| no header/OTel/API dependency conflict | Task 3 |
| caller behavior unchanged | Task 5 |
| README locale parity | Task 4 |

## 위험 예측

- Regex가 `API-Key`, `API_Key`, `API Key`를 모두 잡지 못하면 raw secret이 남으므로 변형별 exact table test를 유지한다.
- `statusCode`가 비정상적으로 긴 문자열을 만들 수 있어 prefix 길이 계산 전에 `coerceAtLeast(0)`을 적용한다.
- `String.take(240)`은 Kotlin Char 기준이므로 Unicode surrogate/경계 test를 포함하고 DB schema와의 계약을 문서화한다.
- Spring consumer는 현재 HTTP artifact를 사용하지 않으므로 provider publication 후 `implementation(libs.bluetape4k.http)`와 resolved graph/POM을 별도 downstream PR에서 검증한다.
