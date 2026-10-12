# assertions 대칭성과 Duration·유한수 검증 설계 통합 리뷰

## 검토 범위와 결론

- **대상 스펙:** `docs/superpowers/specs/2026-10-08-assertion-symmetry-duration-finite-design.md`
- **저장소/브랜치:** `bluetape4k-projects`, `feat/assertion-complements-duration-finite`
- **기준 커밋:** `5fc36ff930d624674d852a364f7411c26522a56a`
- **분류/단계:** Type A Full Feature, Step 2-R
- **핵심 요구:** assertions의 모든 공개 조건 함수는 overload까지 긍정·부정 짝을 제공한다. core는 자주 쓰는 helper만 선별하며, 선택한 각 family는 `require`/`check` 모두에서 긍정·부정 짝을 제공한다.
- **최종 판정:** `PASS` — 여섯 검토 관점과 통합 검토에서 P0=0, P1=0, P2=0, P3=0.
- **다음 단계:** 사용자가 이 스펙을 승인하면 구현 계획을 작성한다. 승인 전 구현과 테스트는 시작하지 않는다.

이 문서는 설계 검토 결과다. production source는 수정하지 않았으며 Gradle 테스트, Detekt, Kotlin/JVM ABI 검증이나 CI를 실행하지 않았다. 해당 검증은 승인 후 구현 단계의 DoD에 포함된다.

## 독립 관점 결과

| 관점 | 판정 | P0 | P1 | P2 | P3 | 확인한 경계 |
|---|---|---:|---:|---:|---:|---|
| 성능 | PASS | 0 | 0 | 0 | 0 | `Collection.size`, 일반 Iterable의 전체 1회 순회, predicate 평가 횟수, 배열 원소 빈도 계산과 추가 공간 |
| 안정성 | PASS | 0 | 0 | 0 | 0 | nullable 성공 반환형, 배열 overload 선택, Sequence/Iterable 소비, Map 값 및 lazy-message 경계 |
| 보안·실패 격리 | PASS | 0 | 0 | 0 | 0 | malformed regex 평가 순서, tolerance 검증, nullable expected 참조 equality, Map null/key 경계 |
| 운영·Ops | PASS | 0 | 0 | 0 | 0 | CI path filter, 정확한 PR head 직접 테스트, ABI 증거 보존, 문서, 복구 경계 |
| 개발자·API | PASS | 0 | 0 | 0 | 0 | 전체 pair 범위, core 선별 기준, overload/반환형/ABI, matrix 증거 위치 |
| 호출자·문서 | PASS | 0 | 0 | 0 | 0 | Java/Kotlin Duration, Float/Double finite API, nullable 배열 호출, 예시와 용어 |
| **통합 결과** | **PASS** | **0** | **0** | **0** | **0** | **사용자 범위, 설계, 검증 요구 및 승인 게이트가 서로 일치함** |

## 통합한 계약

1. `bluetape4k-assertions`에서는 공개 조건 assertion 전체를 receiver와 인자 overload까지 대조해 긍정·부정 짝을 완성한다. 이름이 달라도 정확한 반대 조건인 기존 API는 유지하고 중복 alias를 만들지 않는다. Flow/Turbine의 별도 `assert*`와 assertion 보조 DSL은 범위 밖이다.
2. `bluetape4k-core`는 assertions 전체를 복제하지 않는다. 사용 빈도와 일반 입력 검증 용도를 근거로 선택한 helper만 추가하고, 각 선택 family에는 `require`/`check` 긍정·부정 쌍을 모두 제공한다.
3. Duration assertion과 core helper는 `java.time.Duration`과 `kotlin.time.Duration`을 모두 지원한다. 양수 및 0 이상 조건과 각각의 반대 부호 조건을 함께 제공하며, Kotlin Duration의 무한대는 부호 검사와 별도 `isFinite()` guard로 구분한다.
4. Float/Double의 `shouldBeFinite`/`shouldNotBeFinite`는 각각 exact complement다. finite-sign API는 두 쌍의 네 함수이며, 유한수 입력 domain 안에서 서로 반대이고 NaN/±∞는 양쪽 모두 거부한다. 일반 부호 API에는 finite 조건을 넣지 않아 기존 Number helper 동작을 유지한다.
5. 기존 core `requireContains`/`checkContains`는 `containsKey(key)`와 값 일치를 함께 검사하도록 바로잡는다. 새 `requireNotContains`/`checkNotContains`는 이 positive 조건의 정확한 부정이다. missing key + null value와 실제 null-valued key를 구분하는 호환성 변경을 CHANGELOG와 회귀 검증에 기록한다.
6. null에서 성공할 수 있는 신규 assertion은 nullable 반환형을 사용하고 실제 null을 반환한다. 특히 `shouldNotMatch(String)`과 `shouldNotBeEqualToIgnoringCase`는 `T?`로 반환한다. non-null 배열 expected의 negative equality는 content semantics를 사용하고 nullable expected의 기존 generic overload는 참조 equality를 유지한다.
7. assertions 전체 overload matrix와 core의 선택 family matrix는 `docs/superpowers/reviews/2026-10-08-assertion-symmetry-duration-finite-api-matrix.md`에 별도 표로 남긴다. 각 행의 compile/runtime 근거와 기준 PR head SHA도 같은 기록에 포함한다.

## 초기 지적의 처분

| 초기 지적 | 스펙에 반영한 처리 | 최신 상태 |
|---|---|---|
| nullable 성공 부정 assertion의 반환형이 기존 non-null 반환 선언과 충돌할 수 있음 | null에서 성공하는 `shouldNotMatch(String)` 및 대소문자 무시 equality의 결과를 `T?`로 정하고 compile fixture 대입을 요구 | 해소, 안정성 재검토 PASS |
| nullable expected 배열 호출의 generic overload 선택에 대한 런타임 회귀 근거가 부족함 | non-null/nullable expected별 overload 분기, 같은 참조·같은 내용의 별도 인스턴스·null receiver 테스트를 구분 | 해소, 보안·안정성 재검토 PASS |
| core Map positive 변경 범위가 불명확함 | `requireContains`/`checkContains`까지 변경 대상으로 지정하고 missing-key/null-value와 기존 null-valued key의 반대 결과를 테스트 및 CHANGELOG에 연결 | 해소, Developer/API 재검토 PASS |
| plain Float/Double finite assertion이 공개 API·ABI·fixture matrix에서 빠짐 | `shouldBeFinite`/`shouldNotBeFinite`의 Float/Double descriptor, compile fixture, DoD를 명시하고 finite-sign을 두 쌍의 네 함수로 표기 | 해소, 호출자·Developer/API 재검토 PASS |
| 완성한 overload matrix의 위치가 지정되지 않음 | assertions 전체 표와 core 선별 helper 표를 별도 검증 문서에 두고 정확한 PR head를 기록 | 해소, Developer/API 재검토 PASS |

## 근거와 검증 경계

- assertions와 core의 현재 선언 및 helper 사용 빈도를 기준으로 API 범위와 선택 근거를 확인했다. 스펙의 `현재 근거`, `누락된 공개 overload 목록`, `core에서 제공할 공통 쌍` 표가 해당 조사 결과를 담는다.
- Ops 검토에서 `.github/workflows/ci.yml:214-274`를 확인했다. 현재 PR 경로는 `testing/assertions/**` 변경만으로 assertions 테스트를 보장하지 않으므로, 정확한 PR head에서 `:bluetape4k-assertions:test :bluetape4k-core:test`를 직접 실행해야 한다.
- Writer 용어 검사에서 스펙 파일은 `findings: []`였다. untracked 스펙에 대한 `git diff --no-index --check`와 trailing-whitespace 검색에서도 진단이 없었다.
- 리뷰 시점에 구현·테스트·`javap`·정확한 PR head 증거는 없다. 이는 현재 spec approval gate의 범위이며 구현 완료를 뜻하지 않는다.
- PR, push, merge, release는 이번 실행 권한에 포함되지 않는다.

## 작성 및 통합 검토 DoD

- [x] **SPW-01 — 독자·목적·근거:** 스펙과 독립 리뷰라는 문서 종류, 개발자 및 API 사용자 독자, 저장소·브랜치·기준 커밋과 검토 범위를 명시했다.
- [x] **SPW-02 — 리뷰 계약:** 여섯 관점의 판정, severity, 초기 지적의 처리, 근거, 검증 공백과 다음 gate를 기록했다.
- [x] **SPW-03 — 한국어 기술 문체:** 한국어 자연스러움 checklist를 적용했다. 용어 도구는 `clinic-appointment` 규칙으로 실행되어 네 문서에서 finding이 없었지만, 이 규칙 집합은 Projects 기술 문서에 맞지 않는다. 저장소·API 용어는 문맥에 맞게 직접 대조했으며, 이 도구 결과를 Projects 전용 감사 통과로 해석하지 않는다. API명, 경로, 명령 및 식별자는 보존했다.
- [x] **SPW-04 — 기술 근거와 추적성:** 저장소 선언, CI path filter, 스펙 DoD와 각 reviewer 판단을 대조했다. 실제 실행 검증이 아닌 설계 검토라는 경계를 명시했다.
- [x] **SPW-05 — 최종 읽기:** 새 리뷰 문서와 갱신한 스펙을 다시 읽고 Markdown 구조, code fence, 표, 공백 진단을 확인했다.
- [x] 여섯 관점 및 통합 결과가 P0=0, P1=0으로 수렴했다.
- [ ] 사용자의 구체적인 스펙 승인 — 다음 단계 진입 조건이다.
- [ ] 구현·테스트·ABI·정확한 PR head 검증 — 승인 후 DoD에 따라 수행한다.
