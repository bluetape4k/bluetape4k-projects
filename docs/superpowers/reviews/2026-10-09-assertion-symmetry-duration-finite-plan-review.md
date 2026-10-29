# assertion 대칭·Duration·유한수 구현 계획 통합 리뷰

## 검토 범위와 현재 상태

- 계획: docs/superpowers/plans/2026-10-09-assertion-symmetry-duration-finite-plan.md
- 검토 대상 계획 SHA-256: `543efcb44cb0945aa3a4e0f3868c0ccfcc8e1ec046bfe2b97a1b9865823da816`.
- 기준 구현 HEAD: 5fc36ff930d624674d852a364f7411c26522a56a
- 범위: Type A plan Step 3-R. Assertions 전체 조건 API 대칭, Core에서 선택한 helper pair, Duration/finite API, tests, ABI, 문서와 무PR 경계.
- 이전 계획 SHA: 0e467bd702d91b78d5d2f9ac5c18eb0f2a6374594e8e845c1ad5dab3541e8040.
- 이전 계획에 대한 6관점 집계: P0=0, P1=17, P2=16, P3=1. 아래 처분 표는 이전 finding을 새 계획에서 어떻게 닫았는지 기록한다.
- 최신 계획 SHA `543efcb44cb0945aa3a4e0f3868c0ccfcc8e1ec046bfe2b97a1b9865823da816`는 `241fe34b…`에서 계획 자체 DoD의 `plan-ready` 항목만 체크한 버전이다. 독립 reviewer `plan_final_receipt_recheck2_1011`가 두 정확한 SHA를 대조해 기술 내용 불변과 체크의 정당성을 확인했다. 전체 6관점 판정은 `e2f880ca…`에 대한 독립 검토이며, 최신 SHA는 상태 전용 재검토다.
- workflow receipt는 구현 전 계획·6관점 검토 및 writer evidence를 현재 계획 SHA `543efcb4…`와 최신 리뷰 문서에 연결해야 한다. 이 정확한 receipt 기록과 component 증거가 완료되기 전에는 구현하지 않는다.
- Assertions/Core source와 tests는 아직 수정하지 않았다.
- 기존 2026-10-08 spec review는 역사적 기록으로 보존한다.

## 이전 계획 finding 처분

| 관점 | 이전 결과 | finding | 새 계획의 처분 |
|---|---|---|---|
| 성능 | P0=0, P1=1, P2=1 | nullable array identity를 거부하던 source guard | custom parser/guard를 제거하고 고정 source inventory와 API matrix를 사용한다. |
| 성능 | 동일 | 임의 call count나 wall-clock으로 O(n) 주장을 할 위험 | shouldContainSame 실제 9개 overload를 source review하며 시간 기반 complexity claim을 하지 않는다. |
| 보안 | P0=0, P1=7, P2=2 | self-referential baseline/additions manifest, mutable path, symlink 및 arbitrary path 입력 | 자체 XML/manifest/parser 흐름을 제거한다. 승인된 모듈의 고정 경로와 실제 명령 output만 사용한다. |
| 보안 | 동일 | Python assert, arbitrary command manifest, hardcoded PASS | Python verifier와 command manifest를 제거한다. 실제 argv, 시간, exit code, log/output SHA를 기록한다. |
| 보안 | 동일 | 미검증 git revert/rollback | 자동 rollback을 제거하고, 필요 시 exact SHA·ancestry·경로·상태 확인 및 별도 권한을 요구한다. |
| 보안/API | 동일 | Kotlin regex로 multiline/annotation public API를 누락할 위험 | regex parser를 단일 source inventory로 쓰지 않고 고정 source 파일을 직접 대조하며 javap facades로 cross-check한다. |
| Ops | P0=0, P1=3, P2=4 | receipt lifecycle 및 checklist 적용표 누락 | workflow section에 WF, CL, CG, A-01..A-12 적용·N/A·PENDING을 명시하고 reviewer/component evidence 순서를 계획한다. |
| Ops | 동일 | 사용자 작업 상태와 정리 경계 부족 | 변경 전 NUL 구분 `git status --porcelain=v1 -z`와 `git diff`, 브랜치와 기준 HEAD를 보존하고 task-owned path만 다룬다. 미통합 worktree 정리는 PENDING으로 보존한다. |
| Ops/Stability | 동일 | timeout, interruption, stale-output 검증 미정 | bounded process observation, partial log 보존, receipt diagnosis 후 retry, clean compile/javap, mtime 이외 직접 task 결과 확인을 명시한다. |
| 안정성 | P0=0, P1=0, P2=5 | random seed와 toolchain/locale/timezone 미기록 | 새 테스트의 fixed input, final seed property, baseline/final Java·Gradle·locale·timezone 증거를 계획한다. |
| 사용자/API | P0=0, P1=3, P2=2, P3=1 | Assertions/Core fixture 혼합 | AssertionsApiCompileFixture와 CoreValidationApiCompileFixture를 모듈별로 분리하고 각 module compileTestKotlin을 실행한다. |
| 사용자/API | 동일 | shouldContainSame overload 개수 오기 | 정확히 generic array + 7 primitive arrays + LongArray × Iterable<Long>의 9개로 표기하고 BooleanArray overload를 제외한다. |
| 사용자/API | 동일 | 무승인 PR head DoD | 검증 대상은 local final implementation HEAD이며 향후 PR authority가 별도로 승인될 때만 exact PR head를 반복 검증한다. |
| API | P0=0, P1=3, P2=2 | ReflectionKt 누락 | Assertions javap facade owner list에 ReflectionKt를 포함한다. |
| API | 동일 | 존재하지 않는 Map 2-argument shouldContain 예시 | 실제 Pair signature에 맞춰 shouldContain("absent" to null)을 사용한다. |
| API/사용자 | 동일 | Core Map negative의 empty/null/callback semantics 부족 | null Map은 거부, non-null empty Map은 negative success 및 callback 0회, 실패 메시지 callback 1회로 명시한다. |

## 통합 확인 항목

- 긍정·부정 쌍 원칙은 Assertions 전체에 적용하고 Core에는 승인 설계에서 자주 쓰는 선택 family만 적용한다.
- Java/Kotlin Duration을 모두 다루며 Kotlin infinity 부호와 finite timeout 검사를 구별한다.
- finite-sign 쌍은 유한값 domain에서만 exact complement이고 NaN/±infinity는 양쪽 모두 거부한다. 기존 일반 Number sign helper에 finite 조건을 넣지 않는다.
- Core public KDoc owner, 4개 README, root CHANGELOG, source/JVM overload matrix가 계획 대상에 포함된다.
- PR/push/merge는 승인되지 않았다. CG-11..16 및 merge gate CG-17은 권한 경계로 N/A다. Lesson commit이 canonical checkout에 도달하지 않은 상태의 CG-17A/B와 CG-18은 PENDING으로 보존한다. 미통합 task worktree도 삭제하지 않는다.
- source/test 파일은 현재 수정하지 않았다. 구현 진입은 fresh plan review P0=0/P1=0 및 SPW-01..05 완료에 종속된다.

## 재검토 DoD

- [x] 최신 계획 SHA에 대한 독립 여섯 관점 review와 통합 결과를 기록했다. `e2f880ca…`의 6관점 검토와 `cf307e…`의 정확한 SHA 상태 재검토를 구분해 기록했다.
- [x] 현재 계획 본문에 대한 6관점 결과 `P0=0/P1=0` 및 상태 변경 후 P0/P1 유지 여부를 fresh evidence로 확인했다.
- [x] 계획·설계·통합 리뷰의 SPW-01..05와 한국어 자연스러움 checklist를 확인했다. 용어 감사 스크립트의 비관련 규칙 한계는 통과 근거에서 제외했다.
- [x] 계획 finding 수정 후 변경된 계획 SHA를 다시 검토했고, 영향을 받은 writer 기록을 갱신했다.
- [x] 위 계획 gate 전에 source/test 구현을 시작하지 않았다.

## 2026-10-10 최신 계획 재검토

- **검토 대상:** `docs/superpowers/plans/2026-10-09-assertion-symmetry-duration-finite-plan.md`
- **대상 계획 SHA-256:** `c05dea8b76d277e5d48d2752599e431f8f094c29afa3f189e01ae9e62aee112a`
- **기준 HEAD:** `5fc36ff930d624674d852a364f7411c26522a56a`
- **검토 방식:** workflow가 허용한 주 세션 대체 검토. 아래 여섯 관점과 통합 검토를 이 세션에서 직접 수행했다. 독립 검토자 검토로 기록하지 않는다.
- **독립 reviewer 상태:** `plan_api_1010`의 이전 결과는 계획 SHA `3dbd52ef4e325ef7c9f5dd644995258b79a2f337e7fb4ac9a92be1ecd074473e` 대상이므로 최신 승인 근거가 아니다. `plan_integrator_recovery_1010`과 `plan_user_1010`은 사용량 한도 오류로 종료됐다. 이를 검토 통과로 처리하지 않았다.
- **이전 미해결 지적 처리:** `requireNullOrBlank`의 공백 입력·nullable 반환 fixture와 API 대조표 추적, Assertions의 `Pair<K,V>`와 Core의 `(key, value, ...)` 호출 구분을 계획에 반영했다.
- **이번 검토 중 보완한 계획:** 기준선보다 앞선 실행기 및 자체 테스트 순서, overload별 공개 조건 assertion의 선행 대조표, 기준 환경 별칭과 고정 명령 계약을 추가했다. 실행기의 심볼릭 링크 검사가 동시 악성 경로 변경까지 막는 격리 환경이 아니라는 점과 자식 프로세스 출력물에 민감한 값이 들어갈 수 있는 점도 명시했다.
- **해시 갱신 후 확인:** 마지막 계획 변경은 제한 사항과 명령 표의 한국어 표현만 다듬었다. API 범위, 실행 순서, 검증 명령과 승인 조건은 바꾸지 않았다. 최신 계획을 다시 읽고 아래 기술 검토의 판단 범위와 일치하는지 확인했다.

| 관점 | 판정 | 근거 및 확인 결과 |
|---|---|---|
| 성능 | PASS · P0=0/P1=0 | §3은 유한 Iterable/Sequence 입력, 한 번 순회, `shouldContainSame`의 O(n) 보존 비용을 명시하고 성공 경로의 메모리 비용과 진단 출력 규모도 밝힌다. benchmark나 측정하지 않은 속도 개선을 주장하지 않는다. |
| 안정성 | PASS · P0=0/P1=0 | §0은 실행기 자체 테스트를 기준선보다 앞에 두고, `baseline-environment`와 별칭별 최대 실행 시간·산출물 검증을 고정한다. §선행 검증은 유휴 경고만으로 작업을 종료하지 않고 hard deadline에서 프로세스 그룹을 정리하며 부분 로그/XML을 보존한다. §6은 `near`의 경계값과 바로 바깥 값을 긍정·부정 쌍으로 검증한다. |
| 보안 | PASS · P0=0/P1=0 | §0의 별칭 표는 허용 `phase`, 고정 `argv`, 셸 미사용, 출력 경로와 보고서 생성 여부 확인을 정한다. §워크플로·상태 보존 및 위험 표는 run ID 형식, 기존 심볼릭 링크와 저장소 밖 경로 거부, 동시 변경 한계, 자식 출력물을 receipt나 commit에 복사하지 않는 경계를 적는다. |
| 운영 | PASS · P0=0/P1=0 | §워크플로 체크리스트, §선행 검증, §12는 NUL 구분 상태, 기준선/최종 구분, receipt 증거와 정확한 로컬 HEAD를 연결한다. PR 제외 범위와 CG-17A/B/18의 미완료 상태도 유지한다. 현재 helper `completion-check`가 리뷰·계획 증거를 요구하므로 workflow receipt는 이 문서만으로 완료 처리하지 않는다. |
| 개발자/API | PASS · P0=0/P1=0 | §0 및 §9는 Assertions 소스의 모든 공개 조건 선언을 overload별로 기록하고 기존 쌍과 누락 쌍을 구분한다. 빈 값/공백, 포함·멤버십, 시간, 숫자, Exceptions, Reflection 범위를 명시하며 설계에서 제외한 Flow/Turbine `assert*`와 보조 DSL은 이유와 함께 범위 밖으로 둔다. Core의 공백 계약 회귀와 Map 호출 signature도 별도 fixture로 확인한다. |
| 사용자/호출자 | PASS · P0=0/P1=0 | §1–10은 nullable 성공 반환, predicate/callback 평가와 예외 전파, Java/Kotlin Duration 무한대, 일반 부호 검사와 finite-sign의 NaN 차이, tolerance 경계를 테스트·KDoc·README 예제로 추적한다. 선택하지 않은 Assertions API를 Core에 복제하지 않는다. |
| 통합 | PASS · P0=0/P1=0 | 계획은 승인 설계의 Assertions 전체 쌍과 선택 Core 쌍, issue #1824 범위에 맞는다. 문서·compile fixture·runtime tests·JVM ABI·receipt 순서는 서로 충돌하지 않으며 PR/push/merge 권한을 추가하지 않는다. |

**당시 통합 결과:** `P0=0, P1=0, P2=0, P3=0`. 이는 SHA `c05dea8b…`에 대한 주 세션 대체 검토의 최초 판정이며 독립 검토 완료를 뜻하지 않는다. 후속 사용자 관점 재독에서 finite-sign 표의 무한대 행이 상충하는 P1을 찾아 아래와 같이 정정했다. 따라서 이 최초 판정은 계획 승인 근거로 사용하지 않는다.

### 재검토 DoD

- [x] SHA `c05dea8b…`에 대한 여섯 관점과 통합 검토의 최초 기록을 남겼다. 후속 재독에서 P1이 발견되어 이 결과를 최종 승인 근거로 쓰지 않는다.
- [ ] 최신 수정 계획 SHA에 대한 재검토에서 P0=0/P1=0을 확인한다. 독립 reviewer 미수행 사실과 과거 결과의 SHA를 구분해 기록한다.
- [x] 계획과 통합 리뷰의 SPW-01..05를 확인했다. 독자·목적·근거, 계획/리뷰 필수 항목, 한국어 기술 문체, 설계·계획 추적성, 최종 Markdown 읽기를 다시 대조했다.
- [x] KO-01..07을 적용했다. 의미 있는 사실과 SHA를 보존했고, 근거 없는 표현을 추가하지 않았으며, 번역투·용어·독자 대상 문구를 확인했다. 용어 감사는 네 문서 모두 finding 0건이었고 설정 series가 소프트웨어 전용이 아니라는 한계를 기록했다.
- [ ] 주 세션 대체 검토와 실패한 reviewer lane을 helper receipt에 구분해 기록한다.
- [ ] 최신 P1 처분을 반영한 계획 SHA를 다시 확인하고, 영향받은 관점의 재검토를 기록한다.
- [x] 이 기록 전에는 production/test source를 수정하지 않았다.

## 2026-10-11 사용자 관점 재독 및 계획 수정

- **발견 위치:** 계획 §6의 Assertions finite-sign 결과표.
- **심각도:** P1 — `NaN, +∞, -∞`를 묶은 행은 네 API 모두 실패로 적었으나, 뒤의 개별 `+∞`/`-∞` 행은 일부 finite-sign 함수의 성공으로 적혀 승인된 유한수 입력 계약과 충돌했다.
- **조치:** `+∞`와 `-∞`를 각각 네 함수 모두 실패로 고쳤다. NaN, 무한대, signed zero, 유한 양수·음수 행을 별도로 두어 표가 계약을 직접 나타내게 했다.
- **수정된 계획 SHA-256:** `1035a7e27b28fc068b95d3c668f160c8f9841c655877181e28c74abe89ecf16a`.
- **현재 판정:** 이 P1의 문서 수정은 반영됐지만, 수정된 SHA에 대한 전체·영향 관점 재검토와 문서 최종 읽기는 아직 끝나지 않았다. 따라서 계획 gate와 구현은 대기 상태다.
- **영수증 경계:** SHA `c05dea8b…`의 최초 대체 통합 검토와 `plan_integrator_recovery_1010` 해소 기록은 그대로 보존한다. 그 기록은 이 P1을 발견하지 못했으므로 최신 계획 통과 증거로 재사용하지 않는다. 사용자 관점 lane의 최초 재독 결과도 발견 근거로 남기고, 수정 후 별도 rereview evidence를 추가한다.

## 2026-10-11 수정 계획 재검토

- **검토 대상 계획 SHA-256:** `1035a7e27b28fc068b95d3c668f160c8f9841c655877181e28c74abe89ecf16a`.
- **Finding 처분:** finite-sign 결과표의 `NaN`, `+∞`, `-∞`를 각각 네 함수 모두 실패로 수정했다. `+0.0`/`-0.0`와 유한 양수·음수의 결과는 승인 설계와 유지 조건을 대조했다.
- **영향 범위 재확인:** Assertions의 일반 부호 표는 NaN을 논리 부정에서 통과시키고, 별도 finite-sign 표는 비유한 입력을 모두 거부한다. Core의 기존 general sign 표는 비교 기반 결과를 유지한다. Java/Kotlin Duration 표는 sign-only 결과를 유지하고 Kotlin infinity와 timeout `isFinite()`를 별도 구분한다. 세 계약 사이에 추가 모순을 찾지 않았다.
- **잠정 관점 결과:** 성능·안정성·보안·운영·개발자/API·사용자/호출자 및 통합에서 이 표 수정으로 남은 P0/P1은 없다. 이는 수정된 SHA에 대한 주 세션 rereview이며 독립 검토자 attestation이 아니다. 앞선 SHA의 최초 대체 검토가 P1을 놓친 사실은 보존한다.
- **문서/영수증 상태:** 계획과 이 리뷰 문서를 다시 읽고 SPW/KO 감사 및 helper receipt 최신 증거를 갱신하기 전에는 구현 gate를 통과 처리하지 않는다. Assertions/Core source와 tests는 여전히 변경하지 않았다.

## 2026-10-11 최신 수정 계획 6관점 재검토

- **검토 대상 계획 SHA-256:** `9b76bfe48335f01d83ed5e29138d9786d300469f8bb380e6a9048f1f7a21ed18`.
- **검토 방식:** 주 세션 inline fallback review. 현재 SHA에 대한 독립 reviewer attestation은 없다. 앞서 받은 독립 결과는 더 오래된 SHA를 대상으로 하므로 최신 verdict로 재사용하지 않는다.
- **검토 범위:** 승인 설계와 계획의 요구 추적, 여섯 관점(성능·안정성·보안·운영·개발자/API·사용자/호출자), 계획 통합, 두 P1 수정, 실행 순서 및 무PR 경계.
- **수정 확인:** 보안 P1은 계획의 신뢰 경계에 반영됐다. runner는 신뢰된 기준 HEAD와 task branch만 실행하며, 현재 사용자 권한·환경 변수·읽을 수 있는 파일·네트워크 접근을 그대로 상속하고 sandbox가 아님을 명시한다. ABI P1은 baseline의 기존 owner 목록과 final의 `JavaDurationsKt`를 분리하고, 제거 비교를 공통 owner에 한정하며 final-only descriptor를 API matrix와 대조하도록 수정됐다.
- **최종 집계:** `P0=0, P1=0, P2=6, P3=0`. P2 여섯 항목은 실행 바이너리 identity 고정, raw 로그/XML의 권한·보존 기간, 동일 사용자에 의한 출력 경로 TOCTOU, run ID 재사용·receipt binding, CLI의 비정상 인자 거부 테스트, JUnit/Detekt 입력의 no-follow·regular-file·크기 제한이다. 신뢰 범위를 maintainer checkout으로 제한하고 산출물을 receipt/commit에서 제외하는 현재 계획을 기준으로 이들은 후속 hardening 항목으로 남긴다. 이번 계획 gate의 P0/P1 통과를 막지 않으며, 실제 실행 시 raw 산출물에 민감 정보가 들어갈 수 있다는 한계는 유지한다.

| 관점 | 판정 | 최신 계획에서 확인한 내용 |
|---|---|---|
| 성능 | PASS · P0=0/P1=0 | 성공 경로 순회·보존 비용, 유한 입력 전제, 해시 비용 가정, 진단 비용과 미보장 최악 복잡도를 드러냈고 측정하지 않은 개선 주장을 하지 않는다. |
| 안정성 | PASS · P0=0/P1=0 | 고정 실행 별칭, hard deadline, 유휴 로그 관찰, process-group 종료, 부분 로그/XML 보존, 테스트·컴파일·Detekt baseline/final 구분이 실행 단계에 연결돼 있다. |
| 보안 | PASS · P0=0/P1=0 | 신뢰 경계를 명확히 하고 runner를 sandbox로 표현하지 않는다. 비신뢰 checkout을 제외하고, raw output의 민감 가능성·TOCTOU 및 남은 hardening P2를 숨기지 않는다. |
| 운영 | PASS · P0=0/P1=0 | receipt helper 경계, NUL-safe 사용자 상태 보존, task-owned path, 로컬 commit과 no-PR scope, merge 후에만 가능한 CG-17A/B/18을 보존한다. |
| 개발자/API | PASS · P0=0/P1=0 | Assertions 전체 overload audit과 Core 선택 family, 양쪽 require/check 대칭, 신규 Duration facade, source/runtime/compile/ABI matrix가 서로 추적된다. |
| 사용자/호출자 | PASS · P0=0/P1=0 | predicate/callback 예외와 호출 횟수, null 반환, 배열 overload, NaN/무한대/signed-zero, tolerance, Java/Kotlin Duration 및 timeout 유한성 계약을 구분한다. |
| 통합 | PASS · P0=0/P1=0 | baseline에는 새 `JavaDurationsKt`를 요구하지 않고 final에서만 검증한다. 공통 owner 제거 비교와 신규 owner의 descriptor 대조가 계획 순서에 맞으며 전체 `test`가 fixture compile을 포함한다. |

**판정:** 현재 계획 SHA의 6관점 내용 검토는 P0/P1 차단 finding 없이 통과했다. 앞서 독립 검토자가 찾은 두 P1의 조치와 현재 inline verdict를 구분해 기록했다. workflow receipt의 최신 계획 SHA·writer evidence 연결과 문서 선행 commit 전에는 구현을 시작하지 않는다. source/test 변경, build, test, ABI 실행은 아직 없다.

### 최신 계획 리뷰 문서 DoD

- [x] **SPW-01 — 독자·목적·근거:** 계획 심사 문서의 범위, 대상 독자, 기준 HEAD 및 검토 대상 SHA를 기록했다.
- [x] **SPW-02 — 리뷰 계약:** 여섯 관점과 통합 관점의 판정, severity 집계, 수정된 P1, 남은 P2, 검증 경계를 적었다.
- [x] **SPW-03 — 한국어 기술 문체:** 자연스러운 한국어 서술을 사용하고 API명, 명령, 경로, SHA를 보존했다.
- [x] **SPW-04 — 근거와 추적성:** 현재 계획의 신뢰 경계와 phase별 ABI owner 규칙을 승인 요구와 대조했다. reviewer provenance와 미실행 검증을 구분했다.
- [x] **SPW-05 — 최종 읽기:** 이 절을 포함한 전체 문서를 다시 읽고 Markdown 구조·표·공백 진단을 확인했다.
- [x] **KO-01..07:** 사실·SHA·용어를 보존하고 번역투와 근거 없는 주장을 점검했다. 실행 결과나 독립 검토를 과장하지 않았다.
- [x] 구현 전 source/test 미변경 상태를 보존했다.
## 2026-10-11 독립 계획 검토: SHA `9b76bfe4…` (수정 필요)

- **검토자/방식:** 독립 `code-reviewer` lane `plan_current_6lens_1011`; 여섯 관점과 통합 검토. 이전 주 세션 검토와 별도 결과다.
- **대상:** `docs/superpowers/plans/2026-10-09-assertion-symmetry-duration-finite-plan.md`, SHA-256 `9b76bfe48335f01d83ed5e29138d9786d300469f8bb380e6a9048f1f7a21ed18`.
- **판정:** `P0=0, P1=1, P2=7, P3=0`; 이 SHA는 계획 승인에 사용할 수 없다.
- **P1 — ABI baseline owner 설명 불일치:** §기준선은 baseline과 final의 JDK·`javap` 인자·owner 목록이 같다고 읽힌다. 그러나 §0은 baseline에서 구현 전 존재하는 owner만 사용하고 final에 새 `JavaDurationsKt`를 추가한다고 정한다. 구현자가 기준선 실행에 아직 존재하지 않는 facade를 요구할 수 있다.
- **수정:** 계획 §기준선은 공통 owner descriptor만 비교하고 `JavaDurationsKt`는 final에서만 API matrix와 대조한다고 명시했다. 기준선 명령 목록에도 `baseline-environment`를 추가하고 §0의 고정 argv를 참조하도록 맞췄다.
- **P2 일곱 번째 — 환경 기준선 명령 불일치:** 설명의 Gradle 환경 명령이 별칭 표의 고정 argv와 다르고 baseline 실행 목록에서 호출되지 않았다. 위 수정으로 설명·별칭·실행 목록을 맞췄다.
- **남은 P2 여섯 건:** 실행 바이너리 identity 고정, raw log/XML 권한·보존·삭제 정책, 동일 사용자 출력 경로의 TOCTOU, run ID 재사용과 receipt-session binding, 비정상 CLI 인자 테스트 범위, 보고서 입력의 no-follow/regular-file/크기 제한. 현재 계획은 신뢰된 maintainer checkout을 전제로 하며 이 hardening 항목은 남은 위험으로 유지한다.
- **새 계획 SHA:** `e2f880ca2269dc5c67793c9e0c643dbbdb6b633551769a4b41ef2c607fa01bef`. 이 SHA에 대한 fresh 독립 재검토가 진행 중이다. 이전 inline `P0=0/P1=0`은 위 독립 finding으로 대체됐으며 새 재검토 전까지 통과 근거로 쓰지 않는다.
- **구현 경계:** 현재까지 Assertions/Core source와 tests는 수정하지 않았고, 수정 SHA의 계획 검토와 workflow receipt 갱신 전에는 구현하지 않는다.

### 이 독립 finding 기록의 Writer DoD

- [x] **SPW-01:** 독자는 계획 구현자와 workflow reviewer다. 목적은 정확한 대상 SHA의 판정·finding·수정·남은 위험을 기록하는 것이다. 근거는 위 계획과 독립 reviewer 결과이며, 미확정 사항은 수정 SHA 재검토다.
- [x] **SPW-02:** 대상, 심각도, 위치, 구체적 충돌, 조치, 잔여 위험, 재검토 상태를 기록했다.
- [x] **SPW-03:** 한국어 기술 문체를 사용하고 API명, 명령, 경로, 수치, SHA를 보존했다. KO-01..07 관점에서 사실과 용어를 대조했으며 의미를 바꾸는 윤문은 하지 않았다.
- [x] **SPW-04:** finding을 대상 계획의 §기준선·§0 owner/argv 규칙과 대조했다. 이전 inline verdict와 독립 verdict의 provenance를 분리하고 새 SHA 상태를 미완료로 표시했다.
- [x] **SPW-05:** 위 내용을 포함해 문서를 다시 읽었다. 제목·표·목록·코드 토큰의 구조가 유지되고 whitespace 검사가 통과했다.

## 2026-10-11 수정 계획 독립 재검토: 통과

- **검토자/방식:** 새 독립 `code-reviewer` lane `plan_recheck_6lens_1011`; 여섯 관점과 통합 검토를 수행했다. 테스트·빌드·파일 수정은 없었다.
- **검토 대상:** `docs/superpowers/plans/2026-10-09-assertion-symmetry-duration-finite-plan.md`, SHA-256 `e2f880ca2269dc5c67793c9e0c643dbbdb6b633551769a4b41ef2c607fa01bef`.
- **판정:** `P0=0, P1=0, P2=6, P3=0`. 최신 계획의 P0/P1 gate를 통과한다. 이전 SHA의 결과를 재사용하지 않았다.
- **이전 finding 재확인:** §기준선은 공통 owner의 descriptor만 비교하고 `JavaDurationsKt`는 final에서만 API matrix와 대조한다. `baseline-environment`는 baseline 명령 목록에 포함됐고, §0의 고정 argv와 일치한다.
- **남은 P2 hardening:**

| 항목 | 위치 | 검토자 조치 제안 |
|---|---|---|
| Java/JDK, `javap`, Gradle wrapper 실행 파일의 identity와 baseline/final 동일성 미강제 | §선행 검증, §기준선, §0, §12 | 실행 파일 realpath와 SHA-256을 기록하고 final에서 baseline과 일치하는지 fail-closed로 검사 |
| raw 로그/XML의 mode와 보존 기간 미정 | §위험·승인 조건, §선행 검증, §12 | 출력 디렉터리 `0700`, 파일 `0600`, 로컬 증거 보존·정리 정책 명시 |
| 같은 사용자 프로세스의 동시 출력 경로 교체 방어 부족 | §위험·승인 조건, §12 | exclusive directory 생성, no-follow open 또는 directory FD 기반 쓰기와 lock 절차 정의 |
| 기존 run ID 거부와 workflow receipt binding 없음 | §선행 검증, §12 | 출력 경로 재사용을 거부하고 run metadata에 workflow run ID, session, receipt checksum, local HEAD 저장 |
| unknown option, 추가 positional arg, 중복 옵션, 누락 인자 테스트 미기재 | runner test 및 실행기 계약 | 모든 비정상 인자 조합이 실행 전 non-zero로 끝나는 단위 테스트 추가 |
| JUnit/Detekt 입력의 no-follow, 일반 파일, 크기 제한 미기재 | JUnit verifier, §12 | `lstat`/`O_NOFOLLOW`, regular-file, 최대 byte 검사와 제한된 XML 파싱 명시 |

- **범위 판정:** 계획의 신뢰 경계는 신뢰된 maintainer checkout이며 runner를 sandbox로 간주하지 않는다. 여섯 항목은 보강할 P2로 남고 현재 plan gate를 차단하지 않는다. 구현 중 runner/verifier를 만들 때 누락이 실제 보안 경계 변경으로 이어지지 않는지 이 항목을 다시 대조한다.
- **구현 경계:** Assertions/Core source와 tests는 아직 변경하지 않았다. 계획 자체 DoD와 workflow receipt 기록을 마친 뒤 구현 단계로 넘어간다.

### 수정 계획 재검토 Writer DoD

- [x] **SPW-01:** 독자는 계획 구현자와 workflow reviewer다. 목적은 수정된 정확한 SHA의 독립 verdict와 조치 가능한 잔여 위험을 기록하는 것이다. 근거는 계획과 `plan_recheck_6lens_1011`의 결과이며, 이 여섯 P2는 구현 시 재대조한다.
- [x] **SPW-02:** 대상 SHA, 검토 범위, severity 집계, 이전 finding 처분, 여섯 P2의 위치·수정 제안, 구현 경계를 기록했다.
- [x] **SPW-03:** 한국어 기술 문체로 정리하고 API, option, 경로, 숫자, 코드 토큰과 의미를 보존했다. KO-01..07에 따라 주장과 용어를 점검했으며 번역투나 근거 없는 통과 범위는 추가하지 않았다.
- [x] **SPW-04:** 여섯 P2는 reviewer가 지목한 계획 위치와 재대조했다. 독립 검토, 이전 failed SHA, P2 비차단 판정을 서로 구분했다.
- [x] **SPW-05:** 이 절을 추가한 뒤 Markdown 구조와 표를 다시 읽고 whitespace 진단을 실행했다. 테스트·빌드 검증이 아니라 문서 검토임을 유지한다.

## 2026-10-11 계획 DoD 상태 변경 후 정확한 SHA 재검토: 통과

- **검토자/방식:** 독립 `code-reviewer` lane `plan_status_recheck_1011`; 이전 technical review와 구분되는 정확한 SHA 상태 재검토다.
- **검토 대상 계획 SHA-256:** `cf307e36575bc1af2fcc6d805ea76d1141fd7028c199f38044744c2a87d4cc12`.
- **확인 결과:** 직전 독립 검토 대상 `e2f880ca…`의 기술 본문 379줄이 바뀌지 않았다. 변경은 계획 자체 DoD 체크 상태와 사소한 문구에 한정된다. 체크된 상태는 리뷰 기록과 source/test 미변경 상태로 뒷받침된다.
- **판정:** 최신 정확한 SHA에서 P0=0/P1=0을 유지한다. 기존 technical review의 P2 여섯 건도 변경되지 않아 통합 집계는 `P0=0, P1=0, P2=6, P3=0`이다. 이 lane은 여섯 관점 전체를 다시 수행했다고 주장하지 않는다.
- **경계:** 계획의 `Workflow receipt가 구현 시작 gate를 허용한다` 항목은 영수증에 최종 계획 SHA와 문서 증거를 연결한 뒤에만 체크한다. Assertions/Core source와 tests는 수정하지 않았다.

### 정확한 SHA 상태 재검토 DoD

- [x] **SPW-01:** 구현자와 workflow reviewer를 대상으로 정확한 계획 SHA, 재검토 목적과 근거를 기록했다.
- [x] **SPW-02:** 기술 본문 불변 확인, 상태 변경 범위, severity 결과와 미완료 영수증 경계를 기록했다.
- [x] **SPW-03:** 한국어 기술 문체를 사용하고 식별자·SHA·severity를 보존했다.
- [x] **SPW-04:** `e2f880ca…`의 독립 6관점 검토와 현재 상태 재검토의 범위를 구분했다.
- [x] **SPW-05:** 이 절을 추가한 뒤 문서를 다시 읽고 Markdown 구조와 공백을 확인했다.
- [x] 구현 전 source/test 미변경 상태를 유지했다.

## 2026-10-11 계획 receipt DoD 후 정확한 SHA 재검토: 통과

- **검토자/방식:** 독립 `code-reviewer` lane `plan_final_receipt_recheck2_1011`; 읽기 전용 상태 변경 검토다.
- **검토 대상:** `docs/superpowers/plans/2026-10-09-assertion-symmetry-duration-finite-plan.md`, SHA-256 `543efcb44cb0945aa3a4e0f3868c0ccfcc8e1ec046bfe2b97a1b9865823da816`.
- **비교 대상:** 직전 검토 SHA `241fe34b98df366b43d07e9e94c30e6c3b2ce9f932c6e471a9b76ee1d18b5c61`.
- **변경 확인:** 계획의 기술 내용은 동일하며 `계획 자체 DoD`에서 workflow receipt의 `plan-ready` 증거 항목만 `[ ]`에서 `[x]`로 바뀌었다.
- **근거 판정:** `241fe34b…`에 대해 receipt에 기록된 `plan-ready` 결과와 문서 evidence가 해당 체크를 뒷받침한다. 최신 SHA를 receipt에 정확히 다시 묶는 작업은 이 재검토와 구분되는 후속 workflow 기록이다.
- **심각도:** 새 P0/P1 finding 없음. 기술 계획은 기존 독립 6관점 결과를 유지하며 `P0=0, P1=0, P2=6, P3=0`이다. 이 좁은 상태 검토를 새 6관점 검토로 과장하지 않는다.
- **검증 경계:** 테스트·빌드·파일 변경은 없었다. Assertions/Core source와 tests는 계획 gate까지 수정하지 않았다.

### 정확한 SHA 상태 재검토 Writer DoD

- [x] **SPW-01:** 구현자와 workflow reviewer를 대상으로 계획 경로, 정확한 SHA, 비교 대상과 검토 목적을 기록했다.
- [x] **SPW-02:** 유일한 checkbox 변경, receipt 근거, severity, 독립 6관점 검토와 상태 재검토의 경계를 분리했다.
- [x] **SPW-03:** 한국어 기술 문체를 사용하고 API명, lane ID, SHA와 severity를 보존했다.
- [x] **SPW-04:** reviewer의 정확한 SHA와 PASS 판정을 workflow receipt에 연결하고, 이전 근거를 최신 독립 검토처럼 재사용하지 않는다.
- [x] **SPW-05:** 추가한 내용을 다시 읽고 Markdown 구조 및 whitespace를 확인했다.
- [x] 검토 완료 전 source/test 변경이나 실행 검증을 주장하지 않는다.
