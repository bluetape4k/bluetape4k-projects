# Spring reified 확장 PR 전달 계획 보충

## 목적과 기준 문서

이 문서는 [승인된 구현 계획](2026-10-09-spring-reified-extensions-plan.md)의 당시 범위를 보존하면서, 2026-10-11에 사용자가 지시한 PR 및 merge 전달 단계를 별도로 구체화한다. 구현 범위와 승인 API 목록은 기존 계획과 설계를 따른다. 이 보충 계획은 원래 계획을 수정하거나 과거 승인 시점을 소급해 바꾸지 않는다.

## 전달 권한과 경계

- 저장소: `bluetape4k/bluetape4k-projects`
- 이슈: [#1823](https://github.com/bluetape4k/bluetape4k-projects/issues/1823)
- base: `develop`
- head: `feat/spring-reified-extensions`
- 사용자는 현재 작업에 대해 PR 생성과 merge까지 진행하라고 지시했다.
- merge는 공통 gate `CG-16`을 별도로 통과해야 한다. PR과 정확한 head SHA를 기준으로 merge-ready 보고서를 제시한 뒤 새 명시 승인을 받는다.
- force push, auto-merge, tag, release, workflow dispatch, 원격 branch 삭제는 이 계획에 포함되지 않는다. 로컬 worktree와 branch 삭제도 요청되지 않았다.
- CI 설정 변경은 범위 밖이다. PR의 exact-head CI 확인은 필수다.

## PR 전달 재개 시점 기준선

- 최신 base rebase와 Detekt 대응 커밋 뒤 구현 branch의 확인된 code HEAD: `079e6666805d9cf21d287b66cad16623a9456080`.
- 원격 `develop`: `d9e175f240b16b4f6d6964ff35867ec9d09cd739`.
- branch는 최신 원격 `develop`에서 14 commits 앞, 0 commits 뒤다. feature branch를 `origin/develop`에 rebase했다. upstream의 `ClassSupport.kt`, 테스트, testlog 변경은 feature 기능 파일과 겹치지 않았고, `docs/testlogs/2026-10.md` 충돌은 upstream #1821 결과와 feature #1823 결과를 모두 보존해 해결했다. 이후 테스트 fixture lint 수정은 별도 commit `079e6666805d9cf21d287b66cad16623a9456080`에 담았다.
- 이슈 #1823은 live GitHub 조회에서 `OPEN`, label `enhancement`, milestone `2.1.0`, assignee `debop`이다.

## 2026-10-12 워크플로 복구 기록

- 누락: 이 전달 작업의 첫 문서 수정 전에 Type A leaf `bluetape-full-feature`를 읽지 않아 WF-04 실행 순서를 놓쳤다. 누락을 발견한 뒤 downstream 작업을 멈췄다.
- 복구: 전체 `bluetape-full-feature`, `bluetape-workflow` checklist/common-gates, `bluetape-kotlin-patterns`와 Kotlin checklist/testing/Spring/JVM compatibility references, `bluetape-writer`와 Korean naturalness/terminology references, PR/final-report templates, Step 6-R review reference, liveness/model-routing/topology contracts를 읽고 현재 task scope와 남은 gate를 다시 대조했다.
- 영향 판단: 구현 API와 승인된 `bluetape4k/bluetape4k-projects`/`develop`/`feat/spring-reified-extensions` 대상은 바뀌지 않았다. 기존 구현 계획 승인과 현재 PR 지시는 분리되어 있으며, merge는 CG-16 fresh approval 전까지 금지 상태다. 이전 테스트 결과는 그 실행 당시 HEAD에만 귀속한다.
- 이전 A-08 실행: run `20261010T060453Z-1b1ba453`는 receipt sequence 201의 `run_blocked`에서 끝났다. receipt head 201에 대한 2026-10-12 read-only `completion-check`는 `complete=false`, 취소된 두 문서 reviewer lane이 `missing_lanes`, `missing_main_verification=true`, `missing_components=[]`, `failed_checks=[]`라고 보고했다. sequence 201은 `completion-check` 이벤트가 아니다.
- 이전 PR 전달 실행: run `20261011T110221Z-51ef7347`는 sequence 17에서 문서 reviewer lane을 시작했으나 native spawn이 관찰되지 않았다. sequence 18 stall, 19 probe, 20 probe ACK 뒤 lane을 sequence 21에서 `cancelled`로 기록했다. receipt head 22의 read-only `completion-check`는 `complete=false`, 해당 lane의 `missing_lanes`, `missing_components=[spring-reified-extensions]`, `missing_main_verification=true`, 두 required check 미통과를 보고했다. 취소 lane은 `lane-resolve` 대상이 아니므로 이전 run은 sequence 22에서 `run_blocked`로 보존했다.
- 새 PR 전달 실행: 동일한 승인 범위의 run `20261011T181753Z-8e390735`를 sequence 1에서 초기화하고, 사용자 계획 승인 근거를 연결해 sequence 2에서 승인, sequence 3에서 시작했다. sequence 4–6에서 code/docs reviewer lane과 topology를 등록했다. code lane `spring_reified_pr_code_review_r1`은 sequence 7에서 시작되고 sequence 8에서 ACK를 받았으나, 시작 후 30초를 넘겨 기록했다. sequence 9 완료 시 reviewer는 HEAD `2346515603f72135743db92a4c55869780d1e264`에 APPROVE/0 findings를 반환했으며, sequence 10에서 해당 check를 기록했다. 당시 forced Detekt가 pending이었고, 후속 forced run은 새 테스트 fixture에서 여섯 finding을 발견해 commit `079e6666805d9cf21d287b66cad16623a9456080`에서 수정했다. 이 초회 verdict는 최종 code review를 대신하지 않는다.
- 최신 code commit `079e6666805d9cf21d287b66cad16623a9456080`에서 `build --rerun-tasks`는 68 tasks를 실행해 성공했고 337 tests, failures/errors/skipped 0, Kover 검증을 확인했다. forced Detekt는 `BUILD SUCCESSFUL`, 변경 Kotlin 파일 findings 0이며 나머지 28 finding은 변경하지 않은 파일에서 나왔다. Kotlin LSP `263.6379.0 --stdio`는 20/20 변경 Kotlin 파일에서 진단 0건이다.
- 새 run의 첫 문서 검토 lane `spring_reified_pr_docs_review_r1`은 시작되지 않은 만료 placeholder여서 sequence 11에서 취소했다. `spring_reified_pr_docs_review_r2`는 sequence 12에서 생성, 13에서 시작, 14에서 ACK, 15에서 stall, 16에서 probe, 17에서 probe ACK를 기록했다. 결과가 오지 않아 sequence 18에서 lane을 `failed`로 닫았으며, 이 lane의 미관찰 결과는 검토 근거로 사용하지 않는다.
- 대체 문서 reviewer `spring_reified_pr_docs_review_r3`는 실패한 r2를 parent로 sequence 19에서 생성, 20에서 시작, 21에서 ACK, 22에서 완료했다. 검토 결과는 REQUEST CHANGES, P0=0, P1=0, P2=1, P3=0이었다. 유일한 P2는 이 addendum과 최종 리뷰에 r3의 sequence 19–21 기록이 누락된 점이다. sequence 23에서 `final-docs-review` check를 실패로 기록했다. 보완한 문서는 `spring_reified_pr_docs_review_r4`가 sequence 24에서 생성, 25에서 시작, 26에서 ACK, 27에서 APPROVE로 완료해 다시 검토했다. P0–P3는 모두 0이고 `changed_paths=[]`였다. sequence 28에서 `final-docs-review` check를 PASS로 기록했고 sequence 29에서 r2의 terminal evidence와 r4의 terminal evidence digest를 연결해 r2를 해소했다.
- 이전 전달 run의 종료: read-only `completion-check`는 sequence 29에서 `complete=false`, `missing_lanes=[spring_reified_pr_docs_review_r1]`, `missing_components=[spring-reified-extensions]`, `missing_main_verification=true`, `failed_checks=[]`, `unresolved_failed_lanes=[]`를 반환했다. sequence 11에서 취소된 필수 r1은 완료나 실패 lane 해소로 바꿀 수 없다. 따라서 sequence 30에서 run을 `BLOCKED`로 종결하고 원본 receipt를 보존했다.
- 새 실행: 같은 승인 범위로 Phase 2 run `20261011T193452Z-45b1f5b2`를 초기화하고 sequence 2에서 사용자 승인 근거를 연결해 승인, sequence 3에서 시작했다. 문서 writer r5는 sequence 4–7에서 APPROVE, P0–P3=0, `changed_paths=[]`로 완료했다. ACK는 deadline `19:41:49Z`보다 55초 늦은 `19:42:44Z`였고, lane 완료는 command deadline `19:46:19Z`보다 3분 2초 늦은 `19:49:21Z`였다. 후속 writer r6는 sequence 8–14에서 APPROVE, P0–P3=0, `changed_paths=[]`를 보고했다. canonical startup ACK deadline `19:54:13Z`에 비해 ACK `19:54:22Z`는 9초 늦었다. sequence 10의 evidence 요약은 `19:53:41Z`를 잘못 적었지만 sequence 14의 downstream evidence에서 바로잡았다. r7은 sequence 15–18에서 당시 문서본을 APPROVE했다(P0–P3=0, `changed_paths=[]`, 링크 15/15, 용어 감사 5개 파일 findings 0건). 이후 lesson/review/addendum/testlog를 수정했으므로 r7 결과는 현재본에 적용되지 않는다. r8은 sequence 19–24에서 stall·probe 뒤 interrupt됐고 verdict가 없다. replacement writer r9는 sequence 25–31에서 current document set을 REQUEST CHANGES(P0=0, P1=1, P2=1, P3=0)로 판정했다. P1은 과거 r7 승인을 현재 SPW-04/05 근거로 재사용한 점이고 P2는 final review의 코드 변경 범위 오류다. r9는 링크 15/15, 용어 감사 5개 파일 findings 0건, `changed_paths=[]`를 확인했으며 파일 수정이나 테스트는 수행하지 않았다. sequence 32에서 r8/r9 replacement lineage를 닫았다.
- 문서 및 코드 리뷰 진행 상태: r10의 P2 용어 지적을 수정하고 다섯 문서 task-local 감사에서 `findings=[]`를 확인했다. r11은 sequence 78에서 생성, 79에서 시작, 80에서 ACK됐으나 ACK는 기록된 deadline `02:40:42Z`보다 늦은 `02:43:29Z`였다. main session은 결과를 `02:54:29Z`에 확인해 command deadline `02:45:12Z`를 넘긴 사실도 보존했다. r11은 REQUEST CHANGES(P0=0, P1=0, P2=1, P3=0), `changed_paths=[]`를 반환했다. P2는 final review의 build/Detekt 명령에 `--rerun-tasks`와 `--console=plain`이 빠져 testlog의 실행 명령과 불일치한다는 점이다. 링크 17/17은 유효했고 용어 추가 지적은 없었으며 reviewer는 편집·테스트를 하지 않았다. sequence 81에서 r11을 완료 처리하고 82에서 `final-docs-review`를 실패로 기록했다. 이 문서 revision은 Gradle 명령 provenance를 testlog에 맞추고 다섯 문서를 재독·감사했다. delivery 순서상 수정 문서 묶음의 독립 final-docs review 결과를 기록한 뒤 required checks와 component evidence, main verification, `completion-check`를 끝내고 commit/push/PR 및 exact-head GitHub 검증을 진행한다. 여섯 Step6R lane과 topology는 sequence 36–42에 등록했다. 사용량 제한으로 끝난 성능·안정성·보안 native lane의 실패는 sequence 51–53에 보존했다. main-session inline fallback은 sequence 71/73/75에 완료됐고 원 lane 해소는 72/74/76에 기록했다. 이들은 독립 검토가 아니다. 세 reviewer의 지연 native retry는 같은 exact HEAD에서 PASS/APPROVE였지만 보조 결과로만 남겼다. Ops lane은 sequence 63/64에서 늦게 시작·ACK됐고, liveness-check `suspect_stall` 뒤 잘못된 `stall-record --decision` 입력은 helper가 거부해 receipt에 기록되지 않았다. Ops exact-head PASS는 sequence 67, Developer/API APPROVE는 68, caller PASS는 77이다. 여섯 코드 관점에서 P0/P1/P2는 0이며 stability inline fallback의 P3 주석 불일치는 비차단으로 deferred다. Delivery status는 `PENDING`이다.

## 실행 순서와 완료 기준

1. 저장소·worktree·기준 문서·이슈 metadata·사용자 승인 범위를 새로 읽고 예상치 못한 변경을 보존한다. 현재 branch/base/SHA와 dirty 문서 경로를 확인했다. PR 생성 전에는 issue metadata와 PR 대상 상태를 live GitHub에서 다시 확인한다.
2. 최신 `origin/develop`을 fetch하고 feature branch를 rebase했다. rebase HEAD `2346515603f72135743db92a4c55869780d1e264`, 이후 lint 수정 code HEAD `079e6666805d9cf21d287b66cad16623a9456080`, base `d9e175f240b16b4f6d6964ff35867ec9d09cd739`, 현재 ahead 14/behind 0이다. testlog 충돌에서 양쪽 기록을 보존했다.
3. 코드·문서 검토 결과와 workflow receipt 오류를 이 문서·교훈·최종 리뷰에 기록한다. 각 Superpowers artifact에 SPW-01..05를 독립 적용하고 한국어 terminology audit와 독립 문서 리뷰를 수행한다. r3의 P2 chronology finding을 수정한 문서는 r4가 APPROVE했으며, r2는 sequence 29에서 해소됐다. 취소된 r1 때문에 이전 run은 sequence 30에서 BLOCKED로 보존하고, 새 run `20261011T193452Z-45b1f5b2`에서 최종 검토를 진행한다. r11은 sequence 81에서 늦게 완료되어 P2 command-provenance mismatch를 보고했고, sequence 82에서 `final-docs-review`를 실패로 기록했다. final-review 명령을 testlog와 일치시키고, 최신 문서본을 다시 읽고 독립 검토한다. 이전 모듈 테스트/build/Detekt 결과는 코드 HEAD `78bbbe9fd88e084b8b7d1127f5835a7b4f625fa2`의 2026-10-10 결과로만 인용하며, 최신 검증은 2026-10-12 exact code HEAD `079e6666805d9cf21d287b66cad16623a9456080`에 연결한다.
4. exact code HEAD `079e6666805d9cf21d287b66cad16623a9456080`에서 build/test, forced Detekt, LSP를 완료했다. Build는 68 tasks와 337 tests를 성공했고 Kover 검증을 포함한다. Detekt는 `BUILD SUCCESSFUL`, 변경 Kotlin 파일 finding 0이며 28 finding은 변경하지 않은 파일에 남았다. LSP `263.6379.0 --stdio`는 변경 Kotlin 파일 20/20 진단 0건이다.
5. run `20261011T181753Z-8e390735`에는 code-review와 Korean docs-review lane/topology를 등록했다. 초기 code-review lane은 rebase HEAD에서 APPROVE였지만 forced Detekt gap이 발견돼 최종 exact-head review를 대신하지 않는다. r4가 문서를 승인하고 r2를 sequence 29에서 해소한 뒤에도, 취소된 r1 때문에 이 run은 sequence 30에서 BLOCKED가 됐다. 새 run `20261011T193452Z-45b1f5b2`는 sequence 1–3에서 초기화·승인·시작했다. r5는 sequence 4–7, r6는 sequence 8–14에서 APPROVE였다. r7은 sequence 15–18에서 당시 문서본을 승인했지만 이후 문서가 수정됐다. r8은 sequence 19–24에서 interrupt됐고, r9는 sequence 25–31에서 REQUEST CHANGES(P1=1, P2=1)를 반환했다. r10은 sequence 33–35에서 시작·ACK 후 P2 용어 불일치를 보고했고, sequence 45에서 REQUEST CHANGES로 끝나 sequence 46에서 `final-docs-review`가 실패했다. 용어 지적을 고친 뒤 다섯 문서의 감사 결과는 `findings=[]`였다. r11은 sequence 78–81에서 P2 command-provenance 불일치로 REQUEST CHANGES를 반환했다. reviewer의 ACK와 결과 확인은 각각 deadline 뒤였고 이를 늦은 것으로 기록했다. sequence 82에서 `final-docs-review`를 실패로 남겼다. 이 revision에서 final-review 명령을 testlog에 맞췄고 다섯 문서의 writer audit·출처 대조·read-back을 마쳤다. 수정본에 대한 independent final-docs review가 그 결과를 receipt에 기록한 다음, required check·component evidence·main verification·`completion-check`를 수행한다. lane ACK와 completion deadline은 각각 실제 시각으로 기록하고, P0/P1이 있으면 영향받는 검증과 리뷰를 다시 실행한다.
6. `completion-check`, check/component/main verification, `git diff --check`, NUL-safe changed-path 검토, commit 및 최종 local HEAD를 확인한다. 최신 `develop`에 이미 rebase했으므로 push 직전에 base/head를 다시 읽고 필요하면 충돌 처리 후 리뷰와 검증을 갱신한다. `feat/spring-reified-extensions`는 force 없이 push하고 remote SHA가 local HEAD와 같을 때만 PR을 만든다.
7. PR template을 사용해 한국어 PR을 만들고 #1823을 연결한다. 담당자 `debop`, label `enhancement`, milestone `2.1.0`을 반영하고 body의 마지막 `##` 섹션을 `## DoD Status`로 둔다. PR 생성 직후 live metadata와 exact head를 다시 읽는다.
8. exact PR head의 필수 CI 완료를 기다리고, 완료 뒤 리뷰와 review thread를 다시 읽는다. 적용 가능한 review artifact와 P0/P1 상태를 확인한다. 단독 관리 저장소의 human-review subgate만 근거와 함께 `N/A (single-developer lane)`로 기록한다.
9. CG-15에서 PR URL, 정확한 head SHA, CI, 리뷰·thread, lesson, 체크 수를 근거로 merge-ready 상태를 사용자에게 보고한다. 그 후 CG-16에서 해당 PR과 SHA에 대한 새 명시 승인을 기다린다.
10. 승인 뒤 최신 base/head·CI·review·thread·mergeability를 다시 읽고 `gh pr merge <번호> --rebase --match-head-commit <SHA>`를 실행한다. GitHub의 merged 상태와 merge SHA를 확인한다.
11. merge 후 canonical `develop` checkout을 보존 가능한 상태로 동기화하고 SHA·작업 트리 상태를 확인한다. task-owned cleanup은 요청이 없으므로 삭제하지 않고 근거를 남겨 N/A로 기록한다. lesson이 canonical checkout에 포함된 다음에만 CG-18 순서로 GNO를 갱신·embed·검색한다.

## 중단 및 재검증 조건

- 승인된 파일 범위 밖 dirty/untracked 경로는 보존하고 stage하거나 덮어쓰지 않는다.
- rebase 충돌, remote head 불일치, 실패한 exact-head CI, unresolved review thread, P0/P1, 또는 검증할 수 없는 API 계약은 해당 하위 gate를 PENDING/FAIL로 두고 수리 전 PR 또는 merge 단계로 진행하지 않는다.
- PR merge는 사용자 지시만으로 즉시 실행하지 않는다. CG-15 보고 뒤 정확한 PR/head에 대한 새 승인과 merge 직전 live read-back이 필요하다.

## Superpowers 작성자 게이트

- **SPW-01 — PASS:** 독자는 저장소 유지보수자와 이 전달을 이어받는 실행자다. 목적은 승인된 구현의 PR·merge 단계를 추적하는 것이다. 근거는 기존 승인 계획, 현재 `AGENTS.md`/공통 gate/PR template, issue #1823 metadata, rebase된 Git refs, delivery receipt, 사용자 지시다. repo, issue, branch, SHA, gate ID를 보존했다. 남은 조건은 current docs rereview, workflow DoD, PR exact-head CI·review·thread, CG-16 및 post-merge closeout이다.
- **SPW-02 — PASS:** 본문에 권한 경계, 정확한 repo/base/head, rebase 결과, 이전 receipt 세 건과 새 run의 reviewer chronology 및 lane 복구 근거, 의존 순서, 파일·검증 근거, merge 전 승인, 중단·재검증 조건, post-merge sync/cleanup/GNO 순서를 기록했다.
- **SPW-03 — PASS:** 한국어 자연성 checklist를 적용하고 최신 다섯 문서의 contextual terminology audit를 다시 실행했다. 결과는 `files=5`, `findings=[]`다. 명령, 경로, API 및 gate 식별자는 원문대로 보존했다.
- **SPW-04 — PASS:** final review의 명령을 `docs/testlogs/2026-10.md`의 exact build/Detekt 명령과 맞췄다. build 결과, LSP·Detekt 집계, exact code HEAD, 여섯 code-review disposition과 r11 sequence 81–82를 원 증거와 대조했다.
- **SPW-05 — PASS:** 수정 후 다섯 Markdown 문서를 다시 읽고 작성자 DoD를 기록했다. 독립 `final-docs-review`는 별도 workflow check이며 corrected document set의 reviewer evidence로 판정한다. PR exact-head CI/review/thread, CG-16 및 post-merge closeout도 별도 후속 gate다.
