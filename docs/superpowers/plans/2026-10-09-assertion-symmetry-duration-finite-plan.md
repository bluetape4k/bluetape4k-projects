# Assertions/Core 검증 API 대칭·Duration·유한수 구현 계획

> **실행 게이트:** 최신 계획 SHA에 대한 6관점 검토가 P0=0/P1=0이고 설계·계획·통합 리뷰가 bluetape-writer SPW-01..05를 통과하기 전에는 production/test 코드를 수정하지 않는다.

> **For agentic workers:** 구현은 승인된 계획을 단계별로 수행하고, 각 변경은 테스트 우선으로 검증한다. 외부 부작용과 PR/merge는 이 계획의 권한에 포함되지 않는다.

**목표:** Assertions의 공개 조건 API를 모든 overload에서 긍정·부정 쌍으로 맞추고, 선택된 Core 검증 계열과 Duration·유한수 helper를 같은 원칙으로 보완한다.

**구성:** 기존 파일과 모듈 경계를 유지하면서 Assertions는 공개 조건 API 전체를 감사하고, Core는 자주 쓰는 공통 검증 계열만 선택한다. 각 선택 계열은 정확한 논리 부정 쌍을 제공하며, compile fixture·회귀 테스트·ABI 대조표로 source 및 JVM 계약을 확인한다.

**기술 환경:** Kotlin 2.4, Java 25, Gradle 9.7.0, JUnit 5, 기존 bluetape4k assertions/core utilities.

---

## 목표와 경계

**목표:** bluetape4k-assertions의 공개 조건 assertion을 모든 receiver/argument overload까지 긍정·부정 쌍으로 완성한다. bluetape4k-core는 자주 사용하는 검증만 선별하고, 선택한 각 검증을 requireXxxx/checkXxxx 양쪽에서 긍정·부정 쌍으로 제공한다.

**기준:** 승인 설계 docs/superpowers/specs/2026-10-08-assertion-symmetry-duration-finite-design.md, baseline 5fc36ff930d624674d852a364f7411c26522a56a, Type A Full Feature. 세부 API·null·NaN·infinity·Duration 계약은 설계 표를 기준으로 한다.

**범위:** Assertions 전체 공개 조건 API 대칭 감사, Core에서 승인 설계가 고른 공통 검증 함수, Java/Kotlin Duration, Float/Double 유한수 부호 검증, 테스트, Kotlin 컴파일 fixture, ABI/API 대조표, 한국어 KDoc/README/CHANGELOG 및 고정 명령용 task-local bounded runner를 포함한다. 재사용 가능한 계획·사용자 교정 교훈은 `docs/lessons/2026-10-10-assertion-symmetry-duration-finite.md`에 남긴다. 새 dependency, CI workflow 변경, PR/push/merge/release는 포함하지 않는다. 최종 증거는 로컬 구현 HEAD에 연결한다. 추후 PR이 별도 승인되면 PR의 정확한 head 확인은 새 gate다.

## 이슈와 기존 계획 심사 반영

- 작업 이슈: [#1824](https://github.com/bluetape4k/bluetape4k-projects/issues/1824), 제목 `[testing/assertions][core] 검증 API의 긍정·부정 쌍과 Duration·유한수 helper 보완`. 2026-10-10 실시간 조회에서 `OPEN`, milestone `2.1.0` (#18), `enhancement`와 `test` 라벨, `debop` 할당으로 확인했다.
- 중복 조사에서 현재 열린 이슈 중 Assertions API 전체 대칭, Duration sign pair, finite-sign helpers를 다루는 항목은 없었다. GNO의 broad 검색과 wiki 검색에는 직접 관련 결과가 없었다. 좁은 `shouldNotContainAny` 검색은 이미 종료된 #341과 병합 PR #342를 찾았으며, live GitHub에서 #1817과 #341이 닫힌 상태이고 각각 좁은 matcher·호환 alias 범위임을 확인했다. 이 선행 작업은 현재의 전체 대칭 범위와 중복되지 않는다. 이슈 상태·마일스톤·라벨·중복 여부는 live GitHub 조회를 기준으로 한다.
- 최신 계획 전에 받은 Performance/Security 심사는 이전 계획 SHA `cb2ebc273ac82d222746af8ece34fa446cc0e8dc18cd2abbee1ed4d20d43f1f2`를 대상으로 했으므로 현 계획의 최종 승인 근거가 아니다. 성능 수치 주장 제거, 정확한 기준선 ABI 비교, Iterable/Sequence 입력·진단 비용, Map 호출 수, near tolerance/operand 표, Core callback 예외 전파, 빈 range 의미, Kotlin Duration 무한대, Regex 비용 경계를 아래 작업과 새 계획 심사에 반영한다.
- 현재 작업 worktree의 실행 run은 `20261010T041935Z-a103acef`다. helper `verify`는 sequence 1, 사용자 승인 연결은 sequence 2, 실행 전환은 sequence 3에서 통과했다. 이전 run `20261009T102208Z-788b7ef5`는 필수 API review lane이 terminal 상태지만 completed가 아니어서 sequence 146에서 blocked 처리해 증거를 보존했다. `receipt-diagnose`는 receipt 무결성을 확인했으므로 손상 receipt 복구 경로는 적용하지 않았다. 이전 parent-checkout run `20261007T172235Z-8398ebf8`는 scope mismatch로 차단되어 근거로 사용하지 않는다. 수정된 계획 SHA의 심사 결과는 새 worktree run에 새 lane으로 연결한다. receipt checksum은 Git SHA가 아니며 각 후속 receipt mutation 전에 helper가 반환한 직전 checksum을 사용한다.

## 파일 범위

### Assertions
- 수정: testing/assertions/src/main/kotlin/io/bluetape4k/assertions/Basic.kt, CharSequences.kt, Arrays.kt, Collections.kt, Maps.kt, DateTimesInstantZoned.kt, DateTimesLegacy.kt, DateTimesLocal.kt, Numerical.kt.
- 읽기 전용 감사: testing/assertions/src/main/kotlin/io/bluetape4k/assertions/Exceptions.kt와 Reflection.kt, 대응 ReflectionTest.kt의 기존 동기/suspend `shouldThrow`/`shouldNotThrow`, reified/KClass `shouldBeInstanceOf`/`shouldNotBeInstanceOf` 쌍은 구현 변경 없이 API 대조표와 compile fixture에 기록한다. Reflection receiver null 및 반환 계약도 확인한다. 예외 메시지 보조 DSL 등은 승인 설계의 조건 assertion 범위 밖에 둔다.
- 생성: 같은 package의 DurationAssertions.kt.
- 테스트: 위 source 대응 test 파일 및 신규 testing/assertions/src/test/kotlin/io/bluetape4k/assertions/DurationAssertionsTest.kt.
- 생성: `scripts/assertion_symmetry_runner.py`와 표준 라이브러리만 사용하는 `scripts/test_assertion_symmetry_runner.py`. runner는 고정 단계만 실행하고 Gradle/`javap` 출력과 timeout/process-group 종료 증거를 보존한다.

### Core
- 수정: bluetape4k/core/src/main/kotlin/io/bluetape4k/support/RequireSupport.kt, CheckSupport.kt, KotlinDurationSupport.kt, bluetape4k/core/src/main/kotlin/io/bluetape4k/javatimes/DurationSupport.kt.
- 테스트: bluetape4k/core/src/test/kotlin/io/bluetape4k/support/RequireSupportTest.kt, CheckSupportTest.kt, KotlinDurationSupportTest.kt, bluetape4k/core/src/test/kotlin/io/bluetape4k/javatimes/DurationSupportTest.kt.
- 생성: testing/assertions/src/test/kotlin/io/bluetape4k/assertions/api/AssertionsApiCompileFixture.kt (assertions API 전용), bluetape4k/core/src/test/kotlin/io/bluetape4k/api/CoreValidationApiCompileFixture.kt (core API 전용). 각 fixture는 소유 모듈의 최종 `test` task가 test source set을 컴파일할 때 검증하고, 모듈 간 dependency를 추가하지 않는다.

### 문서/증거
- 수정: testing/assertions/README.md, testing/assertions/README.ko.md, bluetape4k/core/README.md, bluetape4k/core/README.ko.md, root CHANGELOG.md, 변경한 모든 public API의 한국어 KDoc.
- 생성: docs/superpowers/reviews/2026-10-08-assertion-symmetry-duration-finite-api-matrix.md와 docs/superpowers/reviews/2026-10-09-assertion-symmetry-duration-finite-plan-review.md.
- 생성: `docs/lessons/2026-10-10-assertion-symmetry-duration-finite.md`. 구현에서 재사용 가능한 설계·검증 교훈과 사용자의 API 대칭 correction을 기록한다.
- Matrix는 overload별 source API와 해당 compile/runtime 근거를 기록한다.

## 워크플로와 상태 보존

1. `$bluetape-workflow`가 시작한 run/session/owner handle을 `bluetape-flow.py` helper로만 확인·갱신한다. Owner-file 본문/fencing value를 열거나 출력하지 않는다. Receipt checksum과 Git HEAD를 혼동하지 않는다.
2. 변경 전 git status --porcelain=v1 -z, staged/unstaged diff, branch, baseline HEAD를 보존한다. NUL-safe 상태를 사용한다. Task-owned path만 stage하고 git add -A, reset/clean을 하지 않는다.
3. 모든 mutation 전에 mutation-check에 session ID와 정확한 대상 path를 전달한다. Receipt 수정은 직전 receipt checksum을 --expected-head로 사용한다. .bluetape는 helper 외 방식으로 쓰지 않는다.
4. 여섯 reviewer lane의 새 SHA 검토 완료 후 helper 순서로 lane-complete, check-result, component-evidence를 기록한다. 마지막에 completion-check 후에만 complete한다.
5. `scripts/assertion_symmetry_runner.py`는 `baseline-assertions`, `baseline-core`, `baseline-detekt-assertions`, `baseline-detekt-core`, `compile-assertions`, `compile-core`, `test-assertions`, `test-core`, `detekt-assertions`, `detekt-core`, `javap-assertions`, `javap-core`처럼 소스에 고정한 단계만 받는다. 임의 argv, shell command, 외부 manifest는 받지 않으며 각 alias가 내부 고정 argv와 제한시간을 선택한다. 각 호출에는 `--run-id`와 `--phase`를 전달한다. run ID는 `^[0-9]{8}T[0-9]{6}Z-[a-f0-9]{8}$` 형식, phase는 `baseline` 또는 `final`만 허용하며 alias별 허용 조합도 고정한다: `baseline-*`는 `baseline`, `compile-*`/`test-*`/`detekt-*`는 `final`, `javap-*`는 두 값만 허용한다. 한 검증 실행에서 생성한 ID를 모든 단계에서 재사용한다. 출력 기준 경로 `build/workflow/assertion-symmetry/`는 저장소 root에 고정한다. runner는 `<run-id>/<phase>/<step>/attempt-<n>/` 디렉터리를 만든 뒤 각 경로 구성요소가 심볼릭 링크가 아닌지, 해석된 최종 경로가 저장소 root 아래인지 쓰기 전에 확인하고, 벗어나면 거부한다. runner는 `Popen(start_new_session=True)`에 stdout/stderr 파일 descriptor를 직접 연결해 실행 중 로그 파일이 자라게 하고, hard deadline을 monotonic clock으로 감시하고 로그 정체는 idle warning으로 기록한다. idle warning만으로 실행 중 프로세스를 종료하지 않는다. Timeout/interrupt에서는 로그와 해당 단계가 생성한 부분 XML을 보존한 뒤 해당 process group에 SIGINT, TERM, KILL을 순서대로 보내고 그룹 종료를 확인한다. 각 시도는 `build/workflow/assertion-symmetry/<run-id>/<phase>/<step>/attempt-<n>/` 아래 stdout, stderr, 생성된 XML report(해당 단계가 만들 때), argv/PID/PGID, UTC·monotonic 시작·종료, exit 또는 signal, 종료 사유, SHA-256을 함께 보존한다. 부모 디렉터리를 자동 생성한다. 자동 재시도는 하지 않는다. 외부 OS/runner 중단으로 결과 생성 전 끊겼음이 확인된 경우에만 최초 산출물과 실패 상태를 보존한 채 최대 한 번 별도 시도로 재개하고 각 시도를 receipt에 연결한다. 테스트·컴파일·Detekt의 실제 실패나 timeout은 재시도하지 않는다. 성공 문자열이나 설명을 실행 증거로 대체하지 않는다.
   Baseline/test alias는 모듈 `clean` 후 `test`를 한 Gradle invocation에서 실행하고, compile alias는 `clean`, `compileKotlin`, `compileTestKotlin`을 한 invocation에서 실행한다. Detekt alias는 지정 모듈 task에 `--rerun-tasks`를 적용한다.
   실행기의 심볼릭 링크 및 경로 포함 여부 검사는 고정된 로컬 출력 경로 안에서 작업하도록 제한한다. 같은 사용자 권한을 가진 프로세스가 동시에 경로를 바꾸는 상황까지 막는 격리 환경은 아니다. 실행기는 프로세스 환경 변수나 인증 정보를 메타데이터로 저장하지 않는다. 다만 자식 프로세스의 표준 출력·오류와 JUnit XML에는 실행 도구가 출력한 값이 포함될 수 있다. 이 파일은 로컬 빌드 산출물로만 보존하고 내용을 receipt에 복사하거나 commit하지 않는다.
   **신뢰 경계:** 이 runner는 maintainer가 신뢰한 현재 저장소의 기준 HEAD와 이 작업에서 직접 변경한 task branch만 검증한다. 임의 PR head, fork, 출처를 확인하지 않은 checkout 또는 외부 코드를 실행하지 않는다. `./gradlew`, Gradle build logic와 테스트는 현재 사용자 권한으로 실행되며 상속한 환경 변수, 사용자가 읽을 수 있는 파일, 네트워크에 접근할 수 있다. 따라서 runner는 sandbox나 자격 증명 격리 도구가 아니다. 고정 alias·argv와 출력 경로 검사는 명령 범위 및 산출물 경로를 제한할 뿐 실행 코드의 권한을 낮추지 않는다. 주 세션은 각 실행 전에 기준 HEAD와 작업 diff가 이 신뢰 범위에 있는지 확인한다. 신뢰할 수 없는 소스를 검증해야 하는 경우 이 runner를 사용하지 않고, 자격 증명·사용자 파일을 노출하지 않는 별도 격리 환경을 먼저 마련한다.
6. Push/PR/merge/release는 실행하지 않는다. workflow가 요구하면 로컬 commit은 Lore protocol과 task-owned path로만 생성한다.

## 워크플로 체크리스트 적용 여부

| Checklist | 적용·상태 | 증거 또는 경계 |
|---|---|---|
| WF-00 | 완료 | User-scope, workspace, repository guidance를 순서대로 확인했다. |
| WF-01 | 완료 | Type A Full Feature; 두 모듈 공개 API와 문서·ABI·tests가 포함된다. |
| WF-02 | 완료 | ordered plan을 대화에 제시했고, 승인된 목표와 범위를 기준으로 실행한다. |
| WF-03 | 완료 | 현재 thread에서 사용자 승인 확인. |
| WF-04 | 완료 | bluetape-full-feature, workflow common gates, Kotlin/writer/TDD 관련 계약을 읽었다. |
| WF-04A | 진행 | 새 worktree run `20261010T041935Z-a103acef`를 초기화하고 verify(sequence 1), 사용자 승인 연결(sequence 2), 실행 전환(sequence 3)을 확인했다. 이전 run `20261009T102208Z-788b7ef5`는 필수 `plan_api_current_92f1` lane이 terminal이지만 completed가 아니어서 sequence 146에서 blocked 처리했다. receipt는 무결해 손상 복구 경로를 적용하지 않았다. 현재 계획 SHA의 fresh 6관점 리뷰를 새 run에 연결한다. 이전 parent-checkout run은 scope mismatch로 차단되어 근거로 사용하지 않는다. |
| WF-05 | 적용 | common-gates와 Type A checklist를 의존 순서대로 처리한다. |
| WF-06 | 적용 | 각 수정 뒤 영향 받은 review, tests, compile, matrix 증거를 갱신한다. |
| CL-01/02 | 완료 | router/common/leaf checklist와 적용 여부를 계획에 기록한다. |
| CL-03/04/05/06/08 | 적용 | gate 순서, 즉시 증거 기록, fail-closed, 수정 후 downstream 재검증, 최종 X/Y·N/A·Blocked 집계를 따른다. |
| CL-07 | 조건부 N/A | 현재 scope에는 외부·비가역 side effect가 없다. 추후 PR/push/merge/release가 별도 승인되면 직전에 CL-01부터 hold를 다시 읽는다. |
| CG-01..07 | 적용 | 권한/이력/사용자 상태/언어·API 경계/패턴/문서/TDD 검증을 수행한다. |
| CG-08 | N/A | 변경 대상은 순차 로컬 Gradle tests이며 DB/Testcontainers/native/shared-state 검증을 요구하지 않는다. 대상 확인 후 final checklist에 근거를 적는다. |
| CG-09 | 적용 | 사용자의 correction과 계획 심사에서 확인한 재발 방지 교훈을 정확한 `docs/lessons/2026-10-10-assertion-symmetry-duration-finite.md` 경로에 한국어로 작성·검토·로컬 commit한다. 이 단계에서 GNO를 갱신하거나 인덱싱하지 않는다. canonical integration 이후 CG-18에서만 처리한다. |
| CG-10 | 적용 | final diff/review와 local validation을 수렴시키고 task branch에 commit한다. |
| CG-11..16 | N/A (no-PR branch) | 현재 요청은 이슈와 로컬 구현 작업이며 PR 생성·push·merge가 범위에 없다. 각 PR 전용 gate는 해당 권한 부재를 구체적으로 적어 N/A 처리한다. |
| CG-17 | N/A (no-PR branch) | merge 권한과 PR delivery가 범위에 없다. |
| CG-17A | PENDING | lesson commit과 구현 branch가 canonical integration checkout에 동기화되지 않은 동안 대기한다. |
| CG-17B | PENDING | 미통합 task worktree/branch를 보존하며 삭제하지 않는다. |
| CG-18 | PENDING | canonical sync 및 CG-17B 완료 또는 유효한 N/A 이후 canonical checkout에서 GNO update/embed/search를 수행한다. feature worktree에서 인덱싱하지 않는다. |
| A-01..04 | 완료/진행 | 요구·근거·승인 spec·계획과 writer/review gate를 연결한다. A-04는 revised plan P0/P1 및 SPW gate 뒤 pass 처리한다. |
| A-05 | 적용 | overload/null/ABI/Duration/finite/Map 소비 경계를 risk table과 구현 단계에 연결한다. |
| A-06..08 | 적용 | Kotlin pattern/TDD 구현, acceptance·hazard verification, 6-lens final review와 writer gate를 거친다. |
| A-09 | 적용 | 사용자 correction이 재사용 가능한 API 대칭 원칙을 바로잡았으므로 한국어 lesson을 작성·검토·commit한다. |
| A-10 | N/A | PR delivery 미승인 경로다. CG-11..18의 N/A는 구체적인 scope evidence와 함께 기록한다. |
| A-11 | 적용 | no-PR branch의 DoD에 로컬 검증 상태와 미완료 post-integration gate를 함께 기록한다. |
| A-12 | N/A (no-PR delivery) | PR delivery 자체는 범위 밖이다. 이 N/A는 CG-17A/B/18을 완료시키지 않으며 task 결과는 통합 후 closeout 전까지 PENDING이다. |

이 작업은 PR delivery가 범위 밖이므로 Type A no-PR branch를 따른다. 로컬 구현·검증, 독립 최종 리뷰, lesson commit까지 끝내도 CG-17A/B/18은 별도로 남는다. canonical integration, 승인된 cleanup, GNO indexing 전에는 task를 DONE으로 보고하지 않고 PENDING으로 유지한다. 이후 PR이 별도 범위로 승인되면 CG-14에서 이 저장소가 단일 유지보수자 lane임을 근거로 live human-review 하위 항목만 `N/A (single-developer lane)`로 기록한다. 이 예외는 exact-head CI, 리뷰와 thread 재조회, 독립 final code review, P0/P1 처분, 해당 산출물, CG-16의 새로운 merge approval을 면제하지 않는다.

### 구현 전 문서 커밋 gate

최신 계획 SHA에 대한 여섯 관점 검토에서 P0=0/P1=0을 확인하고, 승인 설계·계획·통합 리뷰가 모두 SPW-01..05 및 한국어 자연스러움 checklist를 통과한 뒤, baseline/source 변경 전에 승인 설계, 계획, 기존 설계 리뷰, 최신 통합 계획 리뷰를 task branch에 먼저 commit한다. Commit은 Lore protocol을 따르고 task-owned 문서 경로만 포함한다. 이 문서 커밋은 로컬 검토 산출물을 보존하며 PR·push·merge 권한을 뜻하지 않는다. 이후 lesson과 구현 산출물은 각각 최종 diff 검토를 거쳐 별도 로컬 commit으로 기록한다.

## 선행 검증

baseline부터 최종 단계까지 재사용할 식별자는 저장소 밖 입력을 받지 않고 한 번 생성한다: `ASSERTION_RUN_ID="$(python3 -c 'from datetime import datetime, timezone; import secrets; print(datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ") + "-" + secrets.token_hex(4))')"`. 모든 runner 호출에는 `--run-id "$ASSERTION_RUN_ID"`를 전달하고 runner는 `^[0-9]{8}T[0-9]{6}Z-[a-f0-9]{8}$` 형식을 다시 검사한다.

아래의 모든 Gradle 실행은 `--no-daemon`, `--no-configuration-cache`, `--no-build-cache`, `-Pkotlin.compiler.execution.strategy=in-process`로 실행한다. Kotlin 공식 문서는 `in-process`를 컴파일러를 Gradle 프로세스 안에서 실행하는 방법으로 정의한다([Kotlin compiler execution strategy](https://kotlinlang.org/docs/compiler-execution-strategy.html)). baseline 전에 `python3 -m unittest scripts.test_assertion_symmetry_runner`를 실행해 task-local 표준 라이브러리 runner 자체를 검증한다. runner는 고정 alias에서만 argv를 구성하고 `Popen(start_new_session=True, stdout=<raw fd>, stderr=<raw fd>)`로 실행 중 로그를 파일에 직접 기록한다. Gradle compile/clean의 hard deadline은 30분, 각 module test는 45분, Detekt는 20분, `javap`/고정 검증은 5분이다. Gradle 로그가 15분, lightweight 명령 로그가 2분간 늘지 않으면 idle warning을 기록한다. 로그 정체만으로 프로세스를 종료하지 않으며 hard deadline에서만 종료한다. timeout/interrupt 시 부분 로그와 XML을 보존하고 process group에 SIGINT, 30초 대기, TERM, 30초 대기, KILL 순으로 보내 종료를 확인한다. `--no-daemon` 명령에서 시작한 그룹만 종료하며 공유 daemon/다른 worktree 프로세스는 건드리지 않는다. 자동 재시도는 없다. 외부 OS/runner 중단으로 결과 생성 전 끊긴 것이 확인된 경우에만 최초 산출물과 실패 상태를 보존해 최대 한 번 별도 시도로 재개한다. 실제 테스트·컴파일·Detekt 실패와 timeout은 재시도하지 않는다. 실패 복구를 검토할 때 `receipt-diagnose`로 상태와 원인을 확인하고 최초 로그·XML을 덮어쓰지 않는다.

### Runner contract test

고정 명령 runner는 해당 runner 자체의 unit test를 통과한 후에만 baseline을 실행한다. 테스트는 임시 폴더와 표준 라이브러리 가짜 child로 다음을 검증한다: child가 실행 중일 때 stdout/stderr raw file 크기가 증가함, hard timeout 종료, idle warning 뒤에도 child가 계속 실행되는지, SIGINT→TERM→KILL process group cleanup, 종료 후 같은 group의 child가 남지 않음, interrupt 결과의 signal/reason 기록, 자동 재시도 없음, 부모 output 디렉터리 자동 생성, 형식이 잘못된 `--run-id` 및 허용되지 않은 phase/alias 조합 거부, symlink를 통한 저장소 밖 output 경로 거부. 제한 시간은 테스트에서 짧게 주입하고 생산 alias가 정한 timeout은 변경하지 않는다. runner CLI는 검증된 alias 외 argv/shell/임의 경로를 받지 않는다.

### 기준선
기준 HEAD가 승인 SHA와 일치하는지 확인하고, Java/Gradle toolchain, locale/timezone 및 전체 NUL-safe Git 상태를 기록한다. `baseline-environment`는 §0의 고정 argv로 `java -version`, Gradle 버전/toolchain, locale, timezone을 수집하고 각 출력과 실행 메타데이터를 보존한다. 테스트 요약 도구는 Gradle 종료까지 stdout을 임시 파일에 버퍼링하므로 timeout 감시에 사용하지 않는다. `baseline-assertions`와 `baseline-core`는 각 모듈에서 `clean` 후 `test`를 한 Gradle invocation으로 실행하고, runner 메타데이터의 실제 exit code 및 task log와 고정 JUnit XML verifier 결과를 함께 확인한다. 이후 Step 9의 baseline owner 목록으로 기준선 `javap -public -s` 출력을 수집한다. ABI 비교는 양쪽 단계에 공통인 owner의 descriptor를 비교하고, baseline에는 없는 `JavaDurationsKt`는 final 결과에서만 API matrix와 대조한다.

~~~bash
python3 scripts/assertion_symmetry_runner.py baseline-environment --run-id "$ASSERTION_RUN_ID" --phase baseline
python3 scripts/assertion_symmetry_runner.py baseline-assertions --run-id "$ASSERTION_RUN_ID" --phase baseline
python3 scripts/assertion_symmetry_runner.py baseline-core --run-id "$ASSERTION_RUN_ID" --phase baseline
python3 scripts/assertion_symmetry_runner.py baseline-detekt-assertions --run-id "$ASSERTION_RUN_ID" --phase baseline
python3 scripts/assertion_symmetry_runner.py baseline-detekt-core --run-id "$ASSERTION_RUN_ID" --phase baseline
python3 scripts/assertion_symmetry_runner.py javap-assertions --run-id "$ASSERTION_RUN_ID" --phase baseline
python3 scripts/assertion_symmetry_runner.py javap-core --run-id "$ASSERTION_RUN_ID" --phase baseline
~~~

Baseline 실패 시 로그를 분류하고 해결하기 전 구현하지 않는다. `baseline-detekt-*` XML이 없으면 runner가 지정 module Detekt task를 먼저 실행해 기준 report를 생성하고, 이를 “이번 baseline 실행에서 생성”으로 표시한다. XML 부재를 zero finding으로 기록하지 않는다.

고정 경로 JUnit verifier는 Python 표준 라이브러리 `xml.etree.ElementTree`로 두 모듈의 report 디렉터리만 읽는다. `clean`가 같은 invocation에서 먼저 실행되고 `test` task log가 실제 실행을 보인 뒤 생성된 XML이 1개 이상, testcase가 1개 이상이어야 한다. API matrix가 runtime 검증 대상으로 표시한 변경 test class마다 testcase가 최소 1개 있어야 한다. 모든 report에서 failure/error/skipped/aborted가 0이고 runner metadata의 실제 Gradle exit code가 0이어야 통과한다. Compile-only API fixture는 runtime testcase로 세지 않고 해당 모듈의 `test` task가 fixture를 compile했다는 task log/compile evidence로 확인한다. XML 경로와 module은 두 모듈로 고정하며, 임의 parser, 외부 manifest, Python `assert`, 하드코딩된 PASS 입력은 허용하지 않는다. runner unit tests는 임시 Python child를 사용해 실행 중 log growth, hard timeout 종료와 idle warning만으로 child를 종료하지 않는 동작, process-group 종료, interrupt 결과 기록, 자동 재시도 없음 및 출력 디렉터리 자동 생성 경계를 확인한다.

### 재현성
이번에 추가하는 테스트는 고정 입력을 쓴다. 기존 randomized test의 `EnhancedRandomSupport`, `Fakers`, `UUID.randomUUID()` 등 난수 경로는 이 범위에서 수정하지 않는다. 저장소 기존 테스트 전체가 결정적 seed를 사용한다는 주장은 하지 않는다. 난수 테스트가 실패하면 최초 결과와 로그에서 확인할 수 있는 생성값을 보존하고 재현 한계를 기록한다. 재실행 성공만으로 최초 실패를 지우지 않는다.

## 구현 순서

각 기능은 test/compile fixture부터 추가하고 해당 검증이 실패하는지 확인한 뒤 구현한다. Targeted 검증 통과 후 다음 단계로 이동한다.

### 0. 기준 API 대조표와 고정 명령 runner
모듈 소스를 변경하기 전에 공개 조건 assertion을 파일·함수·receiver·인자 overload별로 목록화한다. 각 선언을 기존 긍정·부정 쌍, 이번에 추가할 쌍, 조건 assertion 범위 밖 항목으로 분류하고 승인 설계의 누락 목록과 대조한다. 특히 `shouldBeEmpty`/`shouldBeBlank`, 포함·멤버십, starts/ends, null/Boolean/identity/equality, 크기·내용·순서 비교, 시간·숫자, Exceptions의 sync/suspend, Reflection의 reified/KClass 함수군을 빠뜨리지 않는다. Flow/Turbine의 별도 `assert*`, `TestAssertionSupport.shouldFail*`, assertion context/보조 DSL은 설계에서 정한 범위 밖 항목으로 이유를 적는다. 초기 대조표를 `docs/superpowers/reviews/2026-10-08-assertion-symmetry-duration-finite-api-matrix.md`에 기록하고 구현 중 추가·변경 사항을 같은 표에 추적한다.

고정 명령 실행기와 자체 단위 테스트를 `baseline` 전에 TDD로 구현한다. `scripts/test_assertion_symmetry_runner.py`에 검증 동작을 먼저 작성하고 `scripts/assertion_symmetry_runner.py`를 구현한다. 실행기 자체 단위 테스트가 통과한 뒤에만 이 문서의 `baseline` 별칭을 실행한다. 이 단계에서는 워크플로 검증 도구만 추가하고 Assertions/Core 모듈 소스와 기존 모듈 테스트는 수정하지 않는다. 따라서 `baseline` 테스트 결과는 대상 모듈 코드의 기준선으로 유지된다.

실행기를 구현하기 전에 허용 별칭과 내부 인자 배열 표를 확정한다. 모든 Gradle 별칭은 저장소 root를 작업 디렉터리로 사용하고 `--no-daemon --no-configuration-cache --no-build-cache -Pkotlin.compiler.execution.strategy=in-process`를 공통 적용한다. `baseline-assertions`와 `test-assertions`는 `:bluetape4k-assertions:clean :bluetape4k-assertions:test`, Core 대응 별칭은 `:bluetape4k-core:clean :bluetape4k-core:test`를 실행한다. `compile-*`는 해당 모듈의 `clean compileKotlin compileTestKotlin`, `baseline-detekt-*`와 `detekt-*`는 해당 모듈의 `detekt --rerun-tasks`를 실행한다. `javap-assertions`와 `javap-core`는 승인된 JVM owner 목록만 대상으로 `javap -public -s`를 실행한다. Core 별칭은 `RequireSupportKt`와 `CheckSupportKt`의 `javap -v -public` 메타데이터도 저장한다. 별칭별 허용 `phase`, 최대 실행 시간, 산출물, XML이 현재 실행에서 새로 생성됐는지 확인하는 방법을 표에 고정한다.

허용 목록에는 환경 기준선을 수집하는 `baseline-environment`도 포함한다. 이 별칭은 `baseline` phase에서 `java -version`, `./gradlew --no-daemon --no-configuration-cache --no-build-cache -Pkotlin.compiler.execution.strategy=in-process --version`, `./gradlew --no-daemon --no-configuration-cache --no-build-cache -Pkotlin.compiler.execution.strategy=in-process -q javaToolchains`, `locale`, `date '+%Z %z'`를 셸 없이 각각 argv 배열로 실행하고 stdout/stderr 및 실행 메타데이터를 보존한다. Gradle 별칭에는 위 공통 제한시간 정책을 적용하고, 각 환경 명령의 최대 실행 시간은 5분으로 둔다.

고정 별칭의 실행 계약은 다음과 같다. `<G>`는 `--no-daemon --no-configuration-cache --no-build-cache -Pkotlin.compiler.execution.strategy=in-process`를 뜻한다. `javap` 대상은 아래에 고정한 phase별 클래스에 한정하며 Assertions facade에는 `io.bluetape4k.assertions.` package prefix를 적용한다. Baseline은 구현 전 존재하는 owner만 사용하고, final은 그 owner와 새 `JavaDurationsKt`를 사용한다. Core owner는 baseline과 final에서 같다.

| 별칭 | 허용 `phase` | 내부 `argv` / 최대 실행 시간 | 산출물·생성 확인 |
|---|---|---|---|
| `baseline-environment` | `baseline` | 앞서 열거한 다섯 명령을 셸 없이 각각 실행; 각 5분 | 명령별 stdout/stderr 및 실행 메타데이터 |
| `baseline-assertions` | `baseline` | `./gradlew <G> :bluetape4k-assertions:clean :bluetape4k-assertions:test`; 45분 | `clean` 이후 생성된 모듈 JUnit XML, runner stdout/stderr |
| `test-assertions` | `final` | `./gradlew <G> :bluetape4k-assertions:clean :bluetape4k-assertions:test`; 45분 | `clean` 이후 생성된 모듈 JUnit XML, runner stdout/stderr |
| `baseline-core` | `baseline` | `./gradlew <G> :bluetape4k-core:clean :bluetape4k-core:test`; 45분 | `clean` 이후 생성된 모듈 JUnit XML, runner stdout/stderr |
| `test-core` | `final` | `./gradlew <G> :bluetape4k-core:clean :bluetape4k-core:test`; 45분 | `clean` 이후 생성된 모듈 JUnit XML, runner stdout/stderr |
| `baseline-detekt-assertions` | `baseline` | `./gradlew <G> :bluetape4k-assertions:detekt --rerun-tasks`; 20분 | 기존 모듈 Detekt 보고서를 지운 뒤 생성된 XML 보고서와 SHA-256 |
| `detekt-assertions` | `final` | `./gradlew <G> :bluetape4k-assertions:detekt --rerun-tasks`; 20분 | 기존 모듈 Detekt 보고서를 지운 뒤 생성된 XML 보고서와 SHA-256 |
| `baseline-detekt-core` | `baseline` | `./gradlew <G> :bluetape4k-core:detekt --rerun-tasks`; 20분 | 기존 모듈 Detekt 보고서를 지운 뒤 생성된 XML 보고서와 SHA-256 |
| `detekt-core` | `final` | `./gradlew <G> :bluetape4k-core:detekt --rerun-tasks`; 20분 | 기존 모듈 Detekt 보고서를 지운 뒤 생성된 XML 보고서와 SHA-256 |
| `compile-assertions` | `final` | `./gradlew <G> :bluetape4k-assertions:clean :bluetape4k-assertions:compileKotlin :bluetape4k-assertions:compileTestKotlin`; 30분 | runner stdout/stderr와 현재 clean 이후 생성된 class files |
| `compile-core` | `final` | `./gradlew <G> :bluetape4k-core:clean :bluetape4k-core:compileKotlin :bluetape4k-core:compileTestKotlin`; 30분 | runner stdout/stderr와 현재 clean 이후 생성된 class files |
| `javap-assertions`, `javap-core` | `baseline`, `final` | `javap -public -s -classpath <fixed module main output> <fixed owners>`; Core는 `RequireSupportKt`와 `CheckSupportKt`에 `javap -v -public`도 실행하며, 각 실행 제한은 5분 | owner별 출력, SHA-256, 공개 멤버 수 |

실행기는 argv 배열로 위 명령만 실행하며 셸 명령 문자열, 임의 task, 외부 명령 목록 파일을 입력받지 않는다. `test` 별칭이 만든 XML과 Detekt 별칭이 만든 보고서의 경로·hash를 고정 verifier에 전달한다.

### 1. Basic 및 CharSequence assertions
Basic.kt, CharSequences.kt, 대응 tests.
- `should(message, predicate)`와 `shouldNot(message, predicate)` pair를 추가한다. 각 함수는 predicate를 정확히 한 번 평가한다. 부정형은 predicate의 논리 부정을 적용하며 긍정형 호출 뒤 예외를 catch하지 않는다. predicate가 던진 sentinel 예외는 예외 타입과 객체 identity를 보존해 호출자에게 전파한다.
- CharSequence의 shouldContainAny(vararg)와 shouldContainNone(vararg), shouldContainAll(vararg/Iterable)와 각 shouldNotContainAll overload를 맞춘다.
- String/Regex shouldMatch pair와 shouldBeEqualToIgnoringCase pair를 구현한다.
- Null receiver에서 성공하는 negative는 nullable 반환형을 사용한다. shouldMatch(String)와 negative도 receiver null 전에 Regex pattern을 구성하므로 invalid pattern은 PatternSyntaxException을 먼저 낸다.
- Predicate/receiver를 중복 평가하거나 오류 메시지를 위해 재순회하지 않는다.
- 검증: CharSequencesTest; null+잘못된 regex, 대소문자, null/empty receiver 및 candidate 경계. shouldContainAny()는 후보가 없을 때 실패하고 shouldContainNone()은 성공한다. shouldContainAll()은 빈 expected 후보에서 성공하고 shouldNotContainAll()은 실패한다. 각 결과를 null receiver와 non-null empty receiver에서 모두 고정하고 null 성공 시 nullable 반환형을 확인한다.
- Regex 입력과 pattern의 실행 비용은 호출자 입력에 따라 달라지므로 이 helper를 비신뢰 입력에 대한 ReDoS 방지 경계로 문서화하지 않는다. 정규식 동작이나 입력 제한 정책은 추가하지 않고, KDoc에 비신뢰 pattern/input의 비용을 호출자가 통제해야 함을 적는다.

### 2. Array assertions
Arrays.kt, ArraysTest.kt.
- generic/primitive overload별 shouldHaveSize, shouldContentEqual, shouldContainSame pair.
- generic 및 8개 primitive 배열의 non-null expected용 shouldNotBeEqualTo 추가. Nullable expected는 generic reference-equality overload로 남긴다.
- shouldContainSame는 generic array+7 primitive array+LongArray × Iterable<Long> 총 9 declarations다. BooleanArray overload를 새로 추가하지 않는다.
- Null receiver의 기존 equality legacy, same-reference/content/different-content, NaN/signed zero 경계를 유지한다.
- 9개 overload 알고리즘은 각각 source review로 확인한다. 승인 설계의 one-pass 입력 계약은 counting/single-use Iterable로 검증하고, 배열 알고리즘은 source review로 순회를 확인한다. 목록 변환을 없애는 구조는 source diff로 확인하되 실제 allocation 감소나 속도 향상을 측정했다고 주장하지 않는다. 별도 benchmark는 추가하지 않는다.
- 검증: 배열별 runtime pair 및 nullable/non-null expected compile resolution.

### 3. Iterable/Sequence assertions
Collections.kt, CollectionsTest.kt.
- shouldContainAll, predicate, size, content, same pair를 기존 overload별로 보완한다.
- 기존 `Iterable.shouldContainAny`/`shouldContainNone`의 의미와 `shouldNotContainAny` 두 호환 alias의 source/ABI를 유지한다. 중복인 `shouldNotContainNone`은 추가하지 않으며 이 계약을 API matrix, compile fixture, README에 기록한다.
- 설계의 null/empty semantics와 nullable 성공 반환을 유지한다. Predicate는 방문 원소별 최대 1회. Sequence identity/one-shot/mismatch 결과를 보존하고 재소비하지 않는다.
- 계약: 이 순회 API의 입력은 종료하는 유한 Iterable/Sequence여야 한다. 무한 입력의 종료를 보장하지 않으며 사용자 정의 iterator·`equals`·`hashCode`·`toString`의 비용도 제한하지 않는다. `shouldContainSame(Iterable)`은 한 번 순회하며 진단에 필요한 actual/expected 원소 목록과 빈도 map을 유지한다. 이 보존 비용은 성공 경로에도 발생하며 입력 크기에 따른 고정 메모리 상한은 두지 않으므로 호출자는 크기가 관리 가능한 입력을 넘겨야 한다. 기대 O(n) 시간과 O(n) 추가 공간은 일반 hash 분포를 전제로 한 source-level 분석이며 benchmark 결과나 성능 개선 주장이 아니다. 해시 충돌이나 사용자 구현이 비정상적인 경우 최악 시간 복잡도는 보장하지 않는다. 실패 진단의 전체 출력 비용도 입력 크기에 비례할 수 있다. 오류 메시지를 위해 입력을 다시 소비하지 않는다.
- 검증: null/empty/non-empty, counting/single-use Iterable·Sequence, callback count. shouldMatchAllWith/shouldMatchAtLeastOneOf와 새 predicate positive/negative pair는 원소별 1회 평가 및 sentinel 예외의 동일 객체 전파를 확인한다. should(message, predicate) 역시 positive/negative 방향 모두에서 같은 예외 인스턴스를 다시 던진다.

### 4. Map assertion 및 Core Map
Assertions Maps.kt/MapsTest.kt, Core RequireSupport.kt/CheckSupport.kt와 tests.
- Map size/same/key-value 포함 pair. Assertion shouldContain은 Pair<K,V>를 받고 key 존재와 값 equality를 함께 검사한다. 예제는 shouldContain("absent" to null) 형식이다.
- Core Contains/NotContains, HasKey/NotHasKey, HasValue/NotHasValue의 require/check pair를 맞춘다. 기존 긍정 함수의 parameter-name/null 사전 검증을 보존한다.
- 추가할 부정 함수 이름을 정확히 고정한다: `requireNotContains`/`checkNotContains`, `requireNotHasKey`/`checkNotHasKey`, `requireNotHasValue`/`checkNotHasValue`. Matrix와 fixture에는 긍정 counterpart인 `requireContains`/`checkContains`, `requireHasKey`/`checkHasKey`, `requireHasValue`/`checkHasValue`도 포함한다.
- 새 부정 Core Map helper는 null Map을 기존 필수 입력 계약대로 거부한다. non-null empty Map은 부정 조건을 만족하며 lazy callback 0회; 조건 실패 메시지 callback은 1회.
- Missing key + null value와 existing null-valued key 구분. 이를 위해 positive/negative Map predicate는 먼저 `containsKey`로 존재 여부를 확인하고 값이 있으면 `get`/entry 값을 비교한다. callback이 던진 예외를 broad catch로 삼키지 않으며, sentinel `IllegalArgumentException`, `IllegalStateException`, `AssertionError`의 종류와 동일 인스턴스가 보존되는지 검증한다.
- Core `CountingMap`은 predicate 단계의 성공/실패 입력에서 `containsKey`와 `get` 호출 수만 확인한다. missing key는 `containsKey` 1회/get 0회, present key는 `containsKey` 1회/get 1회다. lazy message callback은 조건 실패에서만 1회 실행되고, callback이 던진 예외를 변환하지 않는다. Assertions의 Map 실패 진단은 별도 fixture로 검증한다. `Messages.stringify`가 Map/Collection 내용을 재귀 순회하고 사용자 값의 `toString()`을 호출할 수 있으므로 진단을 격리·escape·redact하거나 출력 크기/비용을 제한한다고 보장하지 않는다. 진단 중 사용자 `toString()`이 던진 예외를 삼키거나 대체하지 않고 그대로 전파한다. 테스트는 이 예외 인스턴스가 보존되는지 확인하고 진단 호출을 Core predicate 호출 횟수에 포함하지 않는다.
- 검증: Assertions는 실제 `Pair<K,V>` overload, Core는 `(key, value, ...)` 인자 overload를 각각 compile fixture에서 호출한다. 예외 및 callback 횟수도 확인한다.

### 5. 날짜·시간 assertions
DateTimesInstantZoned.kt, DateTimesLegacy.kt, DateTimesLocal.kt, DateTimesTest.kt.
- Date, LocalDate, LocalTime, LocalDateTime, Instant, ZonedDateTime, OffsetDateTime 각각 after/before/on-or-after/on-or-before의 negative overload. 총 28 declarations와 matrix 비교.
- 경계 equality, 직전/직후, 반환형 검증.

### 6. 숫자 및 Duration assertions
Numerical.kt, 신규 DurationAssertions.kt, NumericalTest.kt, 신규 DurationAssertionsTest.kt.
- Byte/Short/Int/Long/Float/Double의 양수·음수·0 조건과 각 부정 함수는 정확한 논리 부정 관계다.
- Float/Double near pair는 같은 predicate와 tolerance validation을 사용한다. 거리와 tolerance가 같으면 positive는 성공하고 negative는 실패하며, 이 경계를 기존 positive의 inclusive 의미로 유지한다. BigDecimal pair도 tolerance 경계에서 같은 결과를 낸다. `tolerance < 0`, NaN, `+∞`, `-∞`는 두 API 모두 operand 판정 전에 `IllegalArgumentException`으로 거부한다. 유효 tolerance에서 operand 중 하나라도 NaN 또는 ±∞이면 `near`는 false이므로 positive는 실패하고 negative는 성공한다. 여기에는 같은 부호 무한대 차이가 NaN인 경우와, 다른 부호 또는 유한수와 무한대의 차이가 무한대인 경우가 모두 포함된다. 기존 부정 함수의 `delta` 이름과 단일 인자 source overload를 보존하고 default tolerance를 양쪽에서 일치시킨다. BigDecimal near pair도 동일 tolerance를 사용한다.
- Float/Double `shouldBeFinite`와 `shouldNotBeFinite`; finite-sign 두 쌍은 양수와 0 이하, 0 이상과 음수 조건의 긍정·부정 쌍이다. 일반 부호 assertion에 유한 조건을 넣지 않고 finite-sign 쌍은 NaN/±∞를 양쪽 모두 거부.
- Java/Kotlin Duration의 `shouldBePositive`/`shouldNotBePositive`, `shouldBeZeroOrPositive`/`shouldBeNegative` 쌍. Java Duration에는 무한대 표현이 없으므로 Kotlin `INFINITE`/`-Duration.INFINITE`의 부호와 timeout `isFinite()`만 구분해 검증한다.
- 검증: Float/Double 일반 부호 pair 각각의 NaN, `+∞`, `-∞`, `+0.0`, `-0.0`, 양수, 음수 결과표와 별도 finite-sign 결과표를 테스트한다. `shouldBeFinite`/`shouldNotBeFinite`는 NaN/±∞ 대 ±0에서 반대 결과를 낸다. 유효/무효 tolerance와 모든 operand 경계에 더해, 거리와 tolerance가 같은 값 및 바로 바깥 값을 positive·negative pair로 테스트해 inclusive 경계를 고정한다. Java Duration zero/sign, Kotlin Duration zero/sign/`INFINITE`/`-Duration.INFINITE`도 검증한다.

Assertions의 일반 부호 pair truth table은 아래와 같이 고정한다.

| 값 | `shouldBePositive` | `shouldNotBePositive` | `shouldBeNegative` | `shouldNotBeNegative` | `shouldBeZero` | `shouldNotBeZero` |
|---|---:|---:|---:|---:|---:|---:|
| NaN | 실패 | 성공 | 실패 | 성공 | 실패 | 성공 |
| `+∞` | 성공 | 실패 | 실패 | 성공 | 실패 | 성공 |
| `-∞` | 실패 | 성공 | 성공 | 실패 | 실패 | 성공 |
| `+0.0` / `-0.0` | 실패 | 성공 | 실패 | 성공 | 성공 | 실패 |
| 유한 양수 | 성공 | 실패 | 실패 | 성공 | 실패 | 성공 |
| 유한 음수 | 실패 | 성공 | 성공 | 실패 | 실패 | 성공 |

Finite-sign truth table은 다음 네 함수가 Float/Double에서 같은 결과를 내는지 별도로 고정한다.

| 값 | `shouldBeFinitePositive` | `shouldBeFiniteZeroOrNegative` | `shouldBeFiniteZeroOrPositive` | `shouldBeFiniteNegative` |
|---|---:|---:|---:|---:|
| NaN | 실패 | 실패 | 실패 | 실패 |
| `+∞` | 실패 | 실패 | 실패 | 실패 |
| `-∞` | 실패 | 실패 | 실패 | 실패 |
| `+0.0` / `-0.0` | 실패 | 성공 | 성공 | 실패 |
| 유한 양수 | 성공 | 실패 | 성공 | 실패 |
| 유한 음수 | 실패 | 성공 | 실패 | 성공 |

### 7. 선택 Core common helpers
RequireSupport.kt, CheckSupport.kt와 tests. 승인 설계의 common pair 표만 추가한다.
- Equality: `requireEquals`/`requireNotEquals`, `checkEquals`/`checkNotEquals` pair.
- CharSequence·Array·Collection·Map `requireNullOrEmpty`/`requireNotEmpty`, `checkNullOrEmpty`/`checkNotEmpty`; nullable input 전역에서 대칭한다.
- 기존 CharSequence `requireNotBlank`/`requireNullOrBlank`, `checkNotBlank`/`checkNullOrBlank`도 의미상 정확한 쌍이다. 새 API나 별칭은 추가하지 않고 source matrix와 fixture에 포함해 null·blank·nonblank 경계를 확인한다. 기존 `requireNullOrEmpty`와 CharSequence 전용 `requireNullOrBlank`의 잘못된 null-only Kotlin contract를 제거하되 runtime predicate와 nullable 반환형은 유지한다.
- CharSequence: `requireContains`/`requireNotContains`, `checkContains`/`checkNotContains`; `requireStartsWith`/`requireNotStartsWith`, `checkStartsWith`/`checkNotStartsWith`; `requireEndsWith`/`requireNotEndsWith`, `checkEndsWith`/`checkNotEndsWith`. `EndsWith`의 lazy-message overload shape도 맞춘다.
- Map: `requireContains`/`requireNotContains`, `checkContains`/`checkNotContains`; `requireHasKey`/`requireNotHasKey`, `checkHasKey`/`checkNotHasKey`; `requireHasValue`/`requireNotHasValue`, `checkHasValue`/`checkNotHasValue`.
- Closed range: `requireInRange`/`requireNotInRange`, `checkInRange`/`checkNotInRange`. Half-open range: `requireInOpenRange`/`requireNotInOpenRange`, `checkInOpenRange`/`checkNotInOpenRange`. Length/size: `requireLengthInRange`/`requireNotLengthInRange`, `checkLengthInRange`/`checkNotLengthInRange`, `requireSizeInRange`/`requireNotSizeInRange`, `checkSizeInRange`/`checkNotSizeInRange` pair와 기존 overload call shape.
- Float/Double finite: `requireFinite`/`requireNotFinite`, `checkFinite`/`checkNotFinite`.
- 기존 general sign helper pair도 matrix와 truth table에 포함한다: `requirePositiveNumber`/`requireZeroOrNegativeNumber`, `requireZeroOrPositiveNumber`/`requireNegativeNumber`와 각각의 `check` 대응형. 이 sign-only helper에 finite 조건을 추가하지 않는다.
- 모든 선택 family는 `require`/`check`에서 긍정·부정 쌍을 제공한다. Assertion 전용 API를 1:1 복제하지 않는다.
- 모든 negative helper는 positive helper의 실패 예외를 catch해 성공으로 바꾸지 않는다. 부정 predicate를 직접 계산하거나 catch 없는 공통 predicate로 위임한다. callback 및 사전 검증에서 발생한 sentinel `IllegalArgumentException`, `IllegalStateException`, `AssertionError`는 원래 타입과 동일 인스턴스로 호출자에게 전파한다.
- 닫힌 범위 `[start, endInclusive]`와 반열린 범위 `[start, endExclusive)`의 경계를 각각 확인한다. `start > endInclusive`인 닫힌 범위와 `start >= endExclusive`인 반열린 범위는 Kotlin range와 같은 빈 범위 의미를 유지하고 새 경계 예외를 만들지 않는다. 따라서 positive는 일반 실패하고 `requireNotInRange`/`checkNotInRange` 또는 반열린 부정형은 성공해야 한다. length/size pair도 음수·빈 범위를 포함해 끝 경계에서 보완 관계를 확인한다.
- 검증: boundary, null/empty, 예외 종류, overload, 성공 callback 0회/조건 실패 1회 및 기존 precheck 동작. Blank pair는 null·blank에서 `NullOrBlank`만 성공하고 nonblank에서 `NotBlank`만 성공하는 결과를 `require`와 `check`에 각각 고정한다. 위에 이름을 적은 선택 family의 모든 positive/negative `require`와 `check` 선언, nullable 반환형, parameter-name/lazy-message overload를 Core fixture에서 컴파일한다. 선택되지 않은 assertion 전용 API는 Core에 복제하지 않는다.

Core의 일반 Float/Double 부호 전용 pair는 기존 비교 연산 결과를 다음처럼 별도 검증한다. `require`와 `check`는 같은 판정을 하되 실패 예외는 각 API 계약을 따른다.

Assertions와 Core의 일반 부호 표는 서로 다른 API 계약을 나타낸다. Assertions의 `shouldNotBePositive`, `shouldNotBeNegative`, `shouldNotBeZero`는 각 strict predicate의 논리 부정이므로 NaN에서 성공한다. Core의 기존 `ZeroOrNegativeNumber`/`ZeroOrPositiveNumber`는 `<= 0`/`>= 0` 비교 조건을 뜻한다. 따라서 NaN에서는 `require`와 `check`의 `PositiveNumber`, `ZeroOrNegativeNumber`, `ZeroOrPositiveNumber`, `NegativeNumber` 조건이 모두 실패한다. 이 표의 동작은 기존 sign-only Core helper 계약을 유지하며 유한수 검사로 바꾸지 않는다. NaN과 ±∞를 모두 거부하는 API는 별도 finite-sign family다.

| 값 | Positive | ZeroOrNegative | ZeroOrPositive | Negative |
|---|---:|---:|---:|---:|
| NaN | 실패 | 실패 | 실패 | 실패 |
| `+∞` | 성공 | 실패 | 성공 | 실패 |
| `-∞` | 실패 | 성공 | 실패 | 성공 |
| `+0.0` / `-0.0` | 실패 | 성공 | 성공 | 실패 |
| 유한 양수 | 성공 | 실패 | 성공 | 실패 |
| 유한 음수 | 실패 | 성공 | 실패 | 성공 |

Core finite-sign pair는 이 표와 별도로 Float/Double primitive overload 각각을 검증한다. NaN과 양·음의 무한대는 네 finite-sign helper 모두 거부하며, 유한 값에서는 두 쌍의 positive/negative 조건이 정확히 보완된다.

### 8. Core finite-sign 및 Java/Kotlin Duration
RequireSupport.kt, CheckSupport.kt, DurationSupport.kt, KotlinDurationSupport.kt 및 tests.
- Float/Double primitive overload로 `requireFinitePositiveNumber` ↔ `requireFiniteZeroOrNegativeNumber`, `requireFiniteZeroOrPositiveNumber` ↔ `requireFiniteNegativeNumber` 두 쌍과 각 함수의 `check` 대응형을 제공한다. Generic Number overload는 금지하고 기존 general sign helper는 유지.
- Java/Kotlin 각각 `requirePositiveDuration` ↔ `requireZeroOrNegativeDuration`, `requireZeroOrPositiveDuration` ↔ `requireNegativeDuration` 두 쌍과 각 함수의 `check` 대응형을 제공한다. Java package는 `io.bluetape4k.javatimes`, Kotlin package는 `io.bluetape4k.support`다.
- Kotlin `Duration.INFINITE`는 positive 및 zero-or-positive에 해당하고 `-Duration.INFINITE`는 negative 및 zero-or-negative에 해당한다. 이 sign helper는 finite timeout 검증을 뜻하지 않는다. 시간 제한이 유한해야 하는 호출자는 별도로 `isFinite()`를 확인해야 한다. 해당 구분을 함수 KDoc과 README 예제에 명시한다.
- 검증: Float/Double finite-sign pair의 NaN, ±∞, ±0, finite positive/negative 결과를 테스트하고, Core general `PositiveNumber`/`ZeroOrNegativeNumber` 및 `ZeroOrPositiveNumber`/`NegativeNumber` sign-only pair의 동일 값 표를 별도 기록한다. `requireFinite`/`checkFinite`와 `requireNotFinite`/`checkNotFinite`의 NaN/±∞ 대 ±0 결과도 고정한다. Java Duration zero/sign, Kotlin Duration zero/sign/`INFINITE`/`-Duration.INFINITE`와 성공 시 원 receiver 반환을 확인한다.

#### Duration sign truth tables

Assertions의 두 반대 조건 pair는 Java/Kotlin Duration 모두 다음 결과를 낸다. Java Duration에는 유한 음수·ZERO·유한 양수만 있다. Kotlin Duration에는 음의 무한대(-Duration.INFINITE)와 양의 무한대(Duration.INFINITE)도 포함하며 sign-only로 판정한다.

| 입력 | shouldBePositive | shouldNotBePositive | shouldBeZeroOrPositive | shouldBeNegative |
|---|---:|---:|---:|---:|
| Java/Kotlin 유한 음수 | 실패 | 성공 | 실패 | 성공 |
| Kotlin `-Duration.INFINITE` | 실패 | 성공 | 실패 | 성공 |
| Java/Kotlin ZERO | 실패 | 성공 | 성공 | 실패 |
| Java/Kotlin 유한 양수 | 성공 | 실패 | 성공 | 실패 |
| Kotlin `Duration.INFINITE` | 성공 | 실패 | 성공 | 실패 |

Core의 선택된 두 pair는 같은 값에서 다음 결과를 낸다.

| 입력 | PositiveDuration | ZeroOrNegativeDuration | ZeroOrPositiveDuration | NegativeDuration |
|---|---:|---:|---:|---:|
| Java/Kotlin 유한 음수 | 실패 | 성공 | 실패 | 성공 |
| Kotlin `-Duration.INFINITE` | 실패 | 성공 | 실패 | 성공 |
| Java/Kotlin ZERO | 실패 | 성공 | 성공 | 실패 |
| Java/Kotlin 유한 양수 | 성공 | 실패 | 성공 | 실패 |
| Kotlin `Duration.INFINITE` | 성공 | 실패 | 성공 | 실패 |

Java Duration에는 무한대 값이 없으며 Kotlin Duration.INFINITE 및 -Duration.INFINITE 부호 검증은 timeout 유한성 검사가 아니다. 유한 timeout이 필요한 호출자는 별도로 isFinite()를 확인한다.

### 9. Compile fixtures, API matrix, JVM ABI
- 기준 API 대조표는 `testing/assertions/src/main/kotlin/io/bluetape4k/assertions/` 아래 모든 공개 조건 assertion 선언을 overload별로 포함한다. 기존 pair도 empty/blank, element containment/membership, starts/ends, equality/null/Boolean/identity/comparison/range, 크기·내용·순서 비교, date/time, number, Exceptions, Reflection family까지 전부 기록하고, 각 항목에 pair, overload 수, null/return 계약, 검증 위치를 연결한다. Flow/Turbine의 `assert*`, `TestAssertionSupport.shouldFail*`, assertion context/보조 DSL은 설계에서 정한 범위 밖 항목으로 이유를 남긴다. 대조표를 source 변경 전에 만들고 구현 중 갱신한다.
- Assertions fixture는 assertions API만 호출한다. 신규 declaration 및 overload resolution, nullability, 반환형, parameter-name 계약이 달라진 declaration은 fixture에서 직접 호출한다. 기존 unchanged overload 전체의 완전성은 고정 source matrix에서 declaration별로 대조하고 baseline/final ABI 차이에서 기존 공개 signature가 사라지지 않았는지 확인한다. fixture가 변경되지 않은 모든 legacy pair를 중복 호출한다고 주장하지 않는다. Matrix 범위에는 Basic predicate/message 및 equality/null/Boolean/identity/comparison/range pair; CharSequence와 Iterable containment, 빈 candidate 의미와 기존 alias; Regex/String match 및 ignoring-case; generic/primitive array의 overload resolution 위험군; Iterable/Sequence/Map; 7개 temporal receiver; 숫자 sign/near tolerance와 delta; BigDecimal; Reflection reified/KClass; Exceptions sync/suspend; Float/Double finite/finite-sign; Java/Kotlin Duration을 포함한다. shouldNotContainNone은 추가하지 않는다.
- Core fixture는 선택한 모든 family의 positive/negative require와 check 선언, nullable 반환 typed assignment, parameter-name/lazy-message overload를 직접 호출한다. 선택 범위는 Equals/NotEquals; CharSequence·Array·Collection·Map NullOrEmpty/NotEmpty; 기존 CharSequence NotBlank/NullOrBlank pair; CharSequence Contains/NotContains, StartsWith/NotStartsWith, EndsWith/NotEndsWith; Map HasKey/NotHasKey, HasValue/NotHasValue; InRange/NotInRange, InOpenRange/NotInOpenRange, LengthInRange/NotLengthInRange, SizeInRange/NotSizeInRange; Finite/NotFinite; Float/Double 일반 PositiveNumber/ZeroOrNegativeNumber, ZeroOrPositiveNumber/NegativeNumber; Float/Double FinitePositiveNumber/FiniteZeroOrNegativeNumber, FiniteZeroOrPositiveNumber/FiniteNegativeNumber; Java/Kotlin PositiveDuration/ZeroOrNegativeDuration, ZeroOrPositiveDuration/NegativeDuration이다. 계약 제거 회귀를 위해 non-null empty CharSequence의 `requireNullOrEmpty`와 non-null blank CharSequence의 `requireNullOrBlank`를 각각 호출하고 nullable 반환형에 대입한다. Core Map API는 실제 signature대로 `requireContains(key, value, ...)` / `requireNotContains(key, value, ...)` 및 `check` 대응형을 호출한다. `Pair<K,V>` 인자는 Assertions의 `shouldContain` fixture에만 둔다.
- Source declarations를 고정된 관련 Kotlin 파일에서 직접 대조해 overload matrix를 완성한다. `Exceptions.kt`의 동기/suspend pair와 `Reflection.kt`/`ReflectionTest.kt`의 reified/KClass pair, overload, null/return 계약을 source audit에 포함한다. Regex parser를 단독 근거로 쓰지 않는다. 미분류 declaration을 통과 처리하지 않는다.
- Assertions baseline `javap -public -s` owner: `BasicKt`, `ArraysKt`, `CharSequencesKt`, `CollectionsKt`, `JavaDateTimesInstantZonedKt`, `JavaDateTimesLegacyKt`, `JavaDateTimesLocalKt`, `MapsKt`, `NumericalKt`, `ReflectionKt`, `InvokingBlock`, `CoInvokingBlock`. Assertions final owner는 이 목록에 새 `JavaDurationsKt`를 더한다. Core baseline/final owner는 `io.bluetape4k.javatimes.DurationSupportKt`, `io.bluetape4k.support.KotlinDurationSupportKt`, `io.bluetape4k.support.RequireSupportKt`, `io.bluetape4k.support.CheckSupportKt`로 동일하다.
- Source overload와 $default, $annotations, bridge 등 synthetic method를 구분한다. Java Duration descriptor, Kotlin Duration mangling, Float/Double primitive descriptor, near negative arity를 확인한다.
- 두 compile fixture는 소유 모듈의 최종 `test` task가 `compileTestKotlin`을 실행할 때 함께 검증한다. 같은 최종 소스의 `compileTestKotlin`을 별도 반복해 전체 test compile을 중복하지 않는다. 별도 compile alias는 모듈 `clean`부터 시작해 `compileKotlin`과 `compileTestKotlin`을 실행한다. Compile 실패 시 즉시 중단하고 `javap`를 실행하지 않는다. `javap` 전에 예상 JVM owner class 파일이 현재 clean build output에 존재하며 비어 있지 않은지 확인해 stale class를 ABI 근거로 사용하지 않는다. ABI 출력은 두 모듈 main classes만 대상으로 한다.
- 기준선과 최종 ABI 출력은 같은 JDK와 `javap -public -s` 옵션을 사용한다. phase별 owner 목록에서 양쪽에 모두 있는 owner만 비교해 기존 공개 declaration과 descriptor가 사라지거나 바뀌지 않았는지 `comm`으로 확인하며, 누락 또는 변경은 blocker다. Final-only `JavaDurationsKt`는 final에서 성공·non-empty 출력을 확인하고 모든 신규 descriptor를 승인 API matrix와 대조한다. 기준선에는 이 owner가 없어야 한다. Contract가 정의된 공통 Core owner는 baseline/final `javap -v -public` output도 저장해 Kotlin metadata/annotation 차이를 parameter name, nullability, contract와 대조한다. 의도된 `requireNullOrEmpty`/`requireNullOrBlank` contract 제거는 별도 표기하고, 그 외 기존 Kotlin source contract의 변경은 허용하지 않는다. 공개 method 수와 classfile byte 수는 기준선/최종 보고서에 기록하되 측정하지 않은 성능 결론으로 사용하지 않는다.

~~~bash
python3 scripts/assertion_symmetry_runner.py compile-assertions --run-id "$ASSERTION_RUN_ID" --phase final
python3 scripts/assertion_symmetry_runner.py compile-core --run-id "$ASSERTION_RUN_ID" --phase final
~~~

Compile 산출물을 기준으로 다음 classpath와 owner를 사용해 각 facade/class의 공개 API `javap -public -s` 출력을 저장한다. `testing/assertions/build/classes/kotlin/main`을 Assertions classpath로, `bluetape4k/core/build/classes/kotlin/main`을 Core classpath로 사용하고 owner별 명령 결과와 SHA-256을 matrix에 기록한다.

~~~bash
python3 scripts/assertion_symmetry_runner.py javap-assertions --run-id "$ASSERTION_RUN_ID" --phase final
python3 scripts/assertion_symmetry_runner.py javap-core --run-id "$ASSERTION_RUN_ID" --phase final
~~~

기준선과 최종 단계는 동일한 검증 run ID를 runner의 `--run-id`에 전달해 `.../<run-id>/baseline/`과 `.../<run-id>/final/` 산출물로 구분한다. 각 phase에 지정된 모든 owner의 `javap` 성공과 non-empty output 확인 뒤 SHA, 공개 member 수, classfile byte 수를 보존한다. 비교용 `.api` 파일은 공통 owner만 포함해 기준선/최종 `javap` 결과의 `^  public `와 `^    descriptor:` 행을 `LC_ALL=C sort`로 정렬하고 `comm -23 baseline.api final.api`가 비어 있는지 확인한다. Final-only owner의 출력은 별도 `.api` 파일로 정렬하고 모든 descriptor를 승인 API matrix의 신규 선언과 대조한다. `comm -13`으로 공통 owner에서 추가된 선언도 추출해 matrix와 연결한다. Contract가 정의된 `RequireSupportKt`와 `CheckSupportKt`는 baseline/final `javap -v -public` output도 저장해 Kotlin metadata를 source fixture 및 의도된 contract 변경과 대조한다. 재시도 시 `ATTEMPT`를 올려 첫 산출물과 SHA 기록을 보존한다.

### 10. 한국어 API 문서
Assertions의 README.md/README.ko.md, Core의 README.md/README.ko.md, root CHANGELOG, RequireSupport.kt, CheckSupport.kt, DurationSupport.kt, KotlinDurationSupport.kt 및 변경한 Assertions API의 KDoc.
- Pair의 의미, nullability/반환형, 예외, lazy callback/predicate 소비를 기술.
- `java.time.Duration`과 `kotlin.time.Duration` 각각에 alias import를 보이고, `shouldBePositive`/`shouldNotBePositive`, `shouldBeZeroOrPositive`/`shouldBeNegative`, `requirePositiveDuration`/`requireZeroOrNegativeDuration`, `requireZeroOrPositiveDuration`/`requireNegativeDuration`, `checkPositiveDuration`/`checkZeroOrNegativeDuration`, `checkZeroOrPositiveDuration`/`checkNegativeDuration`을 모두 컴파일 가능한 예로 든다. Java Duration 예시는 zero/sign만 다루고, Kotlin infinite timeout은 `isFinite()`와 함께 표시.
- Float/Double의 `shouldBeFinite`/`shouldNotBeFinite`, `requireFinite`/`requireNotFinite`, `checkFinite`/`checkNotFinite`, 유한수 부호 검증, `requireFinitePositiveNumber`/`requireFiniteZeroOrNegativeNumber`, `requireFiniteZeroOrPositiveNumber`/`requireFiniteNegativeNumber`와 각 `check` 대응형을 각각 Float·Double의 컴파일 가능한 예로 들고 일반 부호 검사와 분리한다. Assertions와 Core general sign 표에서 NaN 결과가 다른 이유, finite-sign pair에서 NaN/±∞를 모두 거부하는 계약, ±0 및 유한 값 결과를 함께 적는다.
- Core README와 KDoc은 `requireNullOrEmpty`가 non-null empty 입력을 허용하고 nullable 값을 반환함을 설명한다. `requireNotBlank`/`requireNullOrBlank`와 `checkNotBlank`/`checkNullOrBlank`의 기존 보완 관계 및 null·blank·nonblank 결과를 문서화하고, `requireNullOrBlank`의 non-null blank 성공과 nullable 반환형, 제거한 null-only Kotlin contract도 설명한다.
- Core README에는 선택한 common family 전부의 정확한 positive/negative `require`와 `check` 이름, parameter-name 및 lazy-message 대표 호출을 컴파일 가능한 예로 둔다. 기존 `requireNotBlank`/`requireNullOrBlank`와 `checkNotBlank`/`checkNullOrBlank` 쌍도 포함한다. `requireNotContains`/`checkNotContains`, `requireNotHasKey`/`checkNotHasKey`, `requireNotHasValue`/`checkNotHasValue`, `requireNotInRange`/`checkNotInRange`, `requireNotInOpenRange`/`checkNotInOpenRange`, `requireNotLengthInRange`/`checkNotLengthInRange`, `requireNotSizeInRange`/`checkNotSizeInRange`를 각각 positive pair와 함께 보인다.
- Nullable 배열 expected의 reference equality와 non-null expected의 content equality, null receiver legacy는 null 검증 용도가 아니라는 경고.
- `Iterable.shouldContainAny`/`shouldContainNone`의 현재 의미, `shouldNotContainAny` 두 호환 alias 유지, `shouldNotContainNone` 미추가를 두 Assertions README에 기록한다.
- Map missing-key/null-value, nullable NullOrEmpty, range 경계 기재.
- 두 언어 README의 코드 의미를 동일하게 유지한다.

### 11. 최종 검증
Assertions/Core tests와 Detekt를 bounded runner alias로 순차 실행한다. `test-assertions`와 `test-core`는 각 모듈의 `clean` 후 전체 `test`를 한 Gradle invocation으로 실행한다. 테스트 요약 wrapper는 Gradle 종료 전까지 로그를 버퍼링하므로 timeout 관찰에 사용하지 않는다. runner는 stdout/stderr raw file descriptor를 직접 연결해 실행 중 로그 증가를 확인한다. 전체 `test`가 module-owned compile fixture를 포함하므로 동일 HEAD에서 `compileTestKotlin`을 다시 실행하지 않는다. hard deadline과 idle warning, process-group recovery를 적용하고 중간 단계 실패 시 다음 모듈로 넘어가지 않는다. Detekt alias는 `ignoreFailures=true` 설정을 보완하도록 `--rerun-tasks`로 지정 module task를 하나씩 실행하고, module report를 먼저 비운 뒤 새 XML을 고정 verifier로 검사해 baseline finding 및 SHA와 비교한다. Root `detektReportMerge`는 전체 target project task를 의존하므로 실행하지 않는다.

~~~bash
python3 scripts/assertion_symmetry_runner.py test-assertions --run-id "$ASSERTION_RUN_ID" --phase final
python3 scripts/assertion_symmetry_runner.py test-core --run-id "$ASSERTION_RUN_ID" --phase final
python3 scripts/assertion_symmetry_runner.py detekt-assertions --run-id "$ASSERTION_RUN_ID" --phase final
python3 scripts/assertion_symmetry_runner.py detekt-core --run-id "$ASSERTION_RUN_ID" --phase final
~~~

전체 test task에서 fixture compile을 증명한 다음 변경된 owner의 최종 `javap`를 저장해 기준선 descriptor와 비교하고, `git diff --check`, untracked 문서별 `git diff --no-index --check /dev/null <path>`, 전체 변경 review를 수행한다. JUnit XML verifier로 실행·성공·실패·error·skip·abort 수, 변경 test class별 testcase 수와 각 모듈의 경과 시간을 기록한다. 실제 exit code, argv, start/end, 고정 테스트 입력, Java/Gradle/locale/timezone, output SHA를 남긴다. Freshness는 runner metadata의 실제 argv/exit, task log, 모듈 `clean` 이후 생성된 JUnit/Detekt report set으로 확인한다.

### 12. Receipt와 종료
Component별 실제 명령, 시간, exit code, log/output SHA, API matrix 경로, 검증한 local HEAD를 receipt에 연결한다. 최종 DoD 직전에 live issue #1824의 state, title, milestone, labels, assignee와 issue에 연결한 계획·commit·검증 근거를 다시 조회한다. 이슈를 닫지 않는다. Baseline 이후 변경 path와 NUL-safe Git 상태를 확인하고 사용자 파일을 보존한다. PR/GitHub CI/review/merge 증거를 완료로 표시하지 않는다. completion-check의 미완료 gate를 임의로 통과시키지 않는다.

## 위험 및 승인 조건 추적

| 위험 | 대응 |
|---|---|
| nullable overload/반환형 변화 | typed compile fixture + null 경계 test |
| 배열 expected overload 전환 | non-null/nullable compile 및 runtime 비교 |
| NaN/infinity/signed-zero/tolerance | 별도 truth-table test |
| missing key를 null value로 판단 | containsKey + value equality test |
| callback 과다 실행 | 성공 0회, 실패 메시지 1회, 기존 precheck 확인 |
| Sequence 재소비 | single-use test와 제한된 traversal contract |
| stale class ABI | clean 후 compile/javap |
| 실행기 출력 경로의 심볼릭 링크 및 동시 변경 | 기존 심볼릭 링크와 저장소 밖 경로는 거부한다. 이 검사는 작업 디렉터리 보호이며 같은 사용자 프로세스의 동시 경로 변경까지 막는 격리 환경은 아니다. 자식 프로세스의 표준 출력·오류와 JUnit XML에는 실행 도구가 출력한 값이 포함될 수 있으므로, 내용을 receipt에 복사하거나 commit하지 않고 로컬 빌드 산출물로 보존한다. |
| 실패한 변경 복구 | 먼저 forward-fix하고 branch 상태를 보존한다. reset/clean/branch 삭제나 자동 git revert는 하지 않는다. Backout이 꼭 필요하면 exact commit SHA, baseline ancestry, changed-path allowlist, clean task state를 확인하고 별도 명시 권한을 얻는다. |
| PR CI scope | 이번에는 로컬 증거만; 추후 PR 승인 시 exact head 확인 |
| assertion 진단 출력 | assertion failure 메시지는 호출자가 전달한 값에 좌우되므로 테스트 문맥에서만 사용한다. 민감 정보 제거, 출력 이스케이프, 출력 크기·비용 제한, 순환 참조 감지는 보장하지 않는다. 사용자 toString() 예외는 삼키거나 바꾸지 않고 그대로 전파한다. 공용 formatter hardening은 이번 범위 밖이다. |

| 승인 조건 | 단계 | 증거 |
|---|---:|---|
| Assertions 전체 pair | 1–6, 9 | source inventory, tests, matrix |
| 예외 assertion 기존 pair 유지 | 9 | `Exceptions.kt`, `InvokingBlock`, `CoInvokingBlock` matrix |
| 선택 Core family만 pair | 4, 7–8 | source/tests, selection matrix |
| Java/Kotlin Duration | 6, 8–9 | tests, alias fixture, javap |
| Float/Double finite/sign | 6–9 | boundary tests, primitive descriptors |
| nullability/반환 | 1–4, 9 | runtime + separate compile fixture |
| 한국어 문서 | 10 | 4 README, KDoc, CHANGELOG |
| local validation/receipt | 11–12 | 실제 실행 증거와 local HEAD |

## 계획 자체 DoD

- [x] 승인 설계의 기능 범위 및 Assertions/Core 대칭 원칙과 일치한다.
- [x] 현재 계획의 six-lens review가 P0=0/P1=0이다.
- [x] 설계·계획·리뷰가 SPW-01..05와 한국어 자연스러움 checklist를 통과한다.
- [x] Workflow receipt에 최종 계획 SHA의 `plan-ready` 결과와 근거가 기록된다.
- [x] 계획 gate 통과 전 source/test는 수정하지 않았다.
