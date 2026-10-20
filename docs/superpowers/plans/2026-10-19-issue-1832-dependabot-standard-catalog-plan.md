# Issue #1832 표준 카탈로그 구현 계획

> **실행 방식:** 이전에 승인된 inline 실행을 사용한다. 저장소 밖 subagent 리뷰 attestation은 주장하지 않는다.

**목표:** Projects의 표준 Gradle catalog에서 Issue #1832 대상 열 개 GAV를 Dependabot이 읽게 하고, 기존 중앙 버전 관리와 실제 dependency graph의 선택을 보존한다.

**설계:** bluetape4k-dependencies가 Projects 원본 카탈로그와 고정된 중앙 catalog를 받아 표준 gradle/libs.versions.toml을 생성하고, Projects CI가 원본·allowlist·생성물을 검증한다. repository map v2는 생성 catalog와 Projects 원본 authority catalog를 분리해 기존 v1 소비자와 호환성을 유지한다.

**기술 스택:** Python 3.11+ (tomllib), Python 3.13 CI, Gradle Wrapper 9.8.1, Kotlin Gradle Plugin 2.4.20, Kotlin language/API 2.4, Temurin JDK 21/25, GitHub Actions, pinned dependabot-core Gradle parser.

---

## 현재 실행 상태 (2026-10-20)

| 구간 | 상태 | 현재 근거와 다음 확인 |
|---|---|---|
| Tasks 1–3 구현·테스트 | 완료 | map v1/v2 및 consumer 회귀 검사를 포함한 Central 전체 suite가 최신 후보에서 515 PASS, 2 SKIP이다. |
| Task 4 pinned parser | Central hosted 및 Projects 로컬 검증 완료 | Central PR 최초 run `39285492069`에서 `toml-rb` LoadError를 확인했다. `023569162d070771b67b13f102baa4447ddf4f1a`에서 `bundle exec` 실행을 고쳤고 이후 parser contract가 `39288320776`, `39290083993`, `39293581072`, 최신 exact-head run `39295843422`에서 통과했다. `39288320776`은 더 높은 우선순위의 merge-ref 실행으로 취소됐다. 기본 호스트 Ruby 2.6.10/Bundler 1.17.2 실행은 실패했지만, 아래 기록처럼 Ruby 4.0.5/Bundler 4.0.14 컨테이너에서 Projects 후보 catalog의 parser contract를 통과했다. |
| Tasks 5–6 구현·로컬 검증 | 로컬 수정 완료; hosted 재검증 대기 | Projects workflow contract 18/18, verifier, actionlint, Python compile, diff check가 통과했다. PR run `39371790611`의 generator pin 비교 및 gitleaks false positive, `39408775102`의 synthetic merge SHA checkout을 순차 수정했다. 새 후보는 PR head SHA checkout/assertion, catalog signed tag object SHA pin, MongoDB Testcontainers runner 제약을 포함한다. |
| Task 7 baseline/candidate 증거 | 로컬 완료; Projects PR exact-head CI 보완 중 | 10개 GAV 선택, POM/GMM 26개 byte-identical, graph 비교를 완료했다. 실제 Dokka resolver 98개와 `dependencyInsight` 196건은 중앙 jsoup `1.23.2`/FreeMarker `2.3.35`를 선택했고 잘못된 중앙 catalog 입력 9개는 모두 거부됐다. Projects 후보 `ae7e876b59c52365b2e36f33e9e405314883cea9`에서 Dependabot Core `de71a59f11cc8373d39bbf5b7af5e8b73f45ca0a` parser가 10개 의존성을 읽었고 Central `develop` 및 signed tag의 peeled SHA `dcc7fa7557d3c0548b9e3fc28c48877827ea2f74`를 고정한 strict generator `--check`가 receipt/lock을 발행했다. Central suite는 515 PASS, 2 SKIP이다. `1.9.2-SNAPSHOT` artifact 확인은 HTTP 403으로 건너뛰었다. PR #1838은 열려 있으며 현재 코드 보완을 반영한 새 exact-head CI 전용 map/receipt/lock을 별도 evidence directory에 발행해 이전 후보 산출물과 구분한다. |
| Task 8 review·lesson·Central PR | Central PR #269 병합과 signed tag 완료; Projects PR #1838 open | 원격 head는 `3ae9be4d36bdb6eb9d61289cd62d5ffac9d7461c`이다. run `39408775102`는 기본 PR checkout이 synthetic merge SHA여서 exact-head 증거가 아니고, Spring Boot batch-nosql의 MongoDB 8.0.28 테스트 1건은 GitHub runner kernel 비호환으로 실패했다. 이를 반영한 현재 후보는 로컬 contract 18/18 및 targeted checks를 통과했고, Mongo runner 이전 후속 issue #1839가 milestone 2.1.0에 등록됐다. 새 exact-head CI와 독립 최종 리뷰가 대기 중이며 merge는 fresh approval 전까지 보류한다. Issue #1832와 alert #235는 계속 OPEN이다. |
| Task 9 실제 Dependabot 결과 | 대기 | 2026-10-20 live 조회에서 alert #235가 CRITICAL/OPEN이고 Issue #1832가 OPEN이다. signed central tag, Projects pin/PR/merge, merge SHA Dependency Submission이 선행돼야 hosted 재실행을 한다. |

### 이번 inline 구현 리뷰에서 발견·수정한 CI 공백

- Projects catalog-governance runner에는 parser receipt가 없는데 generator --check가 이를 필수 입력으로 읽었다. 계약 테스트를 먼저 추가해 RED를 확인하고, pinned Dependabot Core checkout과 parser fixture를 같은 job에서 generator check보다 먼저 실행하도록 고쳤다. 별도 workflow의 receipt는 job 사이에서 공유되지 않는다. 심각도 P1, 미해결 P1 아님.
- Projects 원본/생성 catalog 분리 이후 Central push-only downstream audit가 기본 catalog만 읽을 수 있었다. 새 authority catalog가 있으면 Projects만 libs.project.versions.toml로 검사하고 나머지 저장소는 기존 catalog를 읽도록 조건을 추가했다. 회귀 테스트의 RED를 확인하고 기존 legacy 경로도 유지했다. 심각도 P2, 미해결 P2 아님.
- Projects `develop` commit `34c84a6` (#1837)은 중앙 catalog `e9946b1`을 고정하지만 Central 소비자 정책은 `198147f` 기본값만 사용했다. CI의 exact-ref 검증이 이 drift를 찾아 `Verify catalog scripts`가 실패했고 실제 Gradle `Build BOM` 단계는 skip됐다. 저장소별 exact SHA override를 추가했고 manifest 테스트 RED→21 PASS, Central suite 515 PASS/2 SKIP 및 실제 workspace policy verifier가 통과했다. 심각도 P2, 미해결 P2 아님.
- Projects PR `39408775102`의 governance job은 PR head `3ae9be4…` 대신 synthetic merge SHA `41a685c7…`에서 map/receipt를 생성했다. PR에서는 `github.event.pull_request.head.sha`를 expected SHA로 선택하고 checkout/assert/generator branch에 일관되게 쓰도록 수정했다. merge-ref 회귀 테스트 RED→GREEN. 심각도 P1, 수정 후보에 반영; hosted exact-head 확인 대기.
- 중앙 tag checkout은 peeled commit SHA를 확인했지만 annotated tag object 자체는 고정하지 않았다. 현재 GitHub API에서 검증된 signed tag object `4919a6f511e57b4349ca6856816500b3a85ace95`도 pin하고, workflow에서 `^{tag}` object SHA와 commit SHA를 각각 검사한다. unsigned/lightweight replacement은 새 contract test로 거부한다. 심각도 P2, 수정 후보에 반영.
- `39408775102`의 MongoDB `8.0.28` Testcontainers 실패는 GitHub runner kernel `6.19+` incompatibility 로그로 확인했다. `Test / Data`와 Spring Boot `batch-nosql`을 kernel `6.8`인 `ubuntu-22.04`로 임시 고정했으며, 해당 runner의 2027-04-17 종료 전에 이전할 후속 issue #1839를 2.1.0 milestone에 등록했다. 심각도 P2, 새 hosted validation 대기.

- PR #1838 run `39371790611`은 `Verify generated catalog and parser contract`에서 `Projects effective central catalog pin does not match central HEAD`로 실패했다. 앞선 immutable checkout step은 tag peel을 검증했으므로 generator 호출에서 SHA-valued `REF`를 전달하도록 수정했다. gitleaks는 `CacheKey` 이름이 붙은 SHA cache 식별자와 test contract를 generic API-key로 오인해 두 건을 보고했다. 변수명을 `CacheSha`로 바꿔 clean source archive 검사에서 누출 0건을 확인했다. run은 downstream gate 때문에 Build/Test 15개를 skip했고 제품 테스트를 실행하지 않았다.
**현재 로컬 검증:** Central unittest 515 PASS/2 SKIP, 182.553초. 두 skip은 격리 worktree에서 관리 sibling 저장소가 없어 real-workspace inventory 두 건을 실행하지 않은 결과다. `1.9.2-SNAPSHOT` artifact 조회는 HTTP 403으로 건너뛰었다. Central policy verifier는 실제 workspace에서 publisher 8개, 중앙 catalog consumer 9개, 개발 버전 catalog 예제 5개를 통과했다. Central actionlint/py_compile/diff check 통과. 기존 테스트 파일의 Ruff I001은 HEAD와 후보 모두에서 재현돼 새 진단은 없다. Projects workflow contract 테스트는 11/11 PASS, `verify-catalog-workflows.py`와 `actionlint .github/workflows/ci.yml`도 통과했다. `./gradlew --no-daemon --no-configuration-cache --console=plain help`는 성공했다.

**Projects pinned parser 및 strict receipt 검증 (2026-10-20):** clean 후보 `ae7e876b59c52365b2e36f33e9e405314883cea9`, Central `develop`/tag SHA `dcc7fa7557d3c0548b9e3fc28c48877827ea2f74`, Dependabot Core `de71a59f11cc8373d39bbf5b7af5e8b73f45ca0a`를 확인했다. Ruby 4.0.5/Bundler 4.0.14 컨테이너에서 parser contract가 후보 catalog의 10개 version/ref를 모두 읽었다. 로컬 ARM Linux 실행을 위해 무시되는 Dependabot Core clone에만 `aarch64-linux` lock 플랫폼을 임시 추가하고 테스트 뒤 `Gemfile.lock`을 pinned commit 상태로 복구했다. canonical host 경로를 사용한 parser receipt의 catalog SHA-256은 `d9ef46a3413740567d06a0b4af63037202465383ccd43289c8ef68db8cdbfb2f`다. Central `project-dependabot-catalog.py --check`가 repository map, parser receipt, generator receipt, candidate lock을 strict 검증했다. 계획 문서 갱신 commit 뒤 map과 generator receipt/lock을 새 HEAD에 맞춰 다시 발행한다. Projects PR exact-head CI는 아직 실행되지 않았다.

**계획 문서 갱신 writer DoD (2026-10-20 13:17 KST):** SPW-01–05는 모두 통과했다. 대상은 한국어 구현 계획이며, 근거는 이 파일, Projects workflow/catalog, Central mapping/generator, pinned parser receipt와 strict generator outputs, live Issue #1832·alert #235·branch PR 조회다. PR exact-head CI, fresh merge approval, hosted Dependabot 결과는 아직 미확인으로 남겼다. SPW-02는 구현·receipt·PR·merge·Dependabot 순서를 보존하고, 테스트·재실행 지점과 승인 경계를 기록했다. SPW-03 자연스러움 검사 KO-01–07은 모두 통과했고 contextual terminology audit는 `findings=[]`였다. SPW-04 SHA, 버전, 10개 dependency count, HTTP 403 범위와 체크되지 않은 PR/CI 단계를 현재 증거에 대조했다. SPW-05 final read-back 및 `git diff --check`가 통과했다.
외부 보안 근거는 bluetape4k-wiki의 research/2026-10-20-issue-1832-freemarker-path-traversal.md에 한국어로 요약했다. 통합 checkout에서 gno update가 1개를 추가했고, embedding 1 chunk와 FreeMarker path traversal 검색 hit를 확인했다. wiki 원본에는 이전 untracked 파일이 있어 그대로 보존했으며, 새 노트는 아직 commit/push하지 않았다.

**계획 갱신 writer DoD (2026-10-20 02:51 KST):** SPW-01–05 및 KO-01–07을 유지하며 최신 exact-head 결과로 상태를 다시 대조했다. Central PR head `80ae0d6bf2525c1d434fabe87993905c0fc10197`, CI run `39295843422`의 9/9 성공, Dependency Submission `39295845219`, 4개 skipped 테스트와 조건부 workflow 단계, HTTP 403 artifact 조회, PR body/metadata 및 review/thread 재조회 결과를 반영했다. Issue #1832와 alert #235는 여전히 OPEN이며 merge/tag/Projects PR/hosted Dependabot 경계는 대기 상태로 남겼다. 기존 실패·취소 run은 현재 성공 증거와 분리해 기록했다.

**병합·동기화 read-back (2026-10-20 02:34 UTC):** PR #269 head `80ae0d6bf2525c1d434fabe87993905c0fc10197`에 대한 fresh approval 뒤 `gh pr merge --rebase --match-head-commit`을 실행했다. GitHub read-back에서 PR은 `MERGED`, merge SHA는 `dcc7fa7557d3c0548b9e3fc28c48877827ea2f74`이며, Central canonical `develop`과 `origin/develop`이 그 SHA로 일치하고 작업 트리가 깨끗하다. merge-SHA CI `39359607639`는 8 success/0 failure로 완료됐고 push 조건부 publication contract 1개가 skip됐다. 이 contract는 PR exact-head에서 통과했으며 merge SHA Dependency Submission `39359608560`도 성공했다. Issue #1832와 CRITICAL alert #235는 계속 OPEN이다. 더티한 Projects 작업 트리와 untracked lesson을 보존해 cleanup 및 GNO 단계는 미완료로 둔다. 이 상태 갱신은 현재 GitHub 및 로컬 상태를 기준으로 기존 acceptance/DoD 순서를 유지한다. SPW-01–05와 KO-01–07을 확인했다. `audit-korean-terms.mjs --series clinic-appointment --json`은 findings=[]를 반환했고 `git diff --check`도 통과했다. 최종 Markdown read-back에서 merge SHA, post-merge CI 상태, 보존한 변경 및 남은 gate가 근거와 일치한다.

**Task 7 Dokka 보안 선택 검증 (2026-10-20 03:08 UTC):** 실제 Dokka 내부 runtime resolver 98개 각각에 대해 jsoup/FreeMarker `dependencyInsight` 196건을 생성했다. 모든 결과가 pinned Central catalog와 일치해 jsoup `1.23.2`, FreeMarker `2.3.35`를 선택했다. 별도로 ignored catalog cache의 버전 입력을 한 건씩 바꾸고 매 실행 후 원본 bytes와 checksum을 복구하는 matrix를 수행했다. jsoup `1.16.1`, `1.23.0`, dynamic/range/rich-range/qualifier/malformed, FreeMarker `2.3.34`, empty jsoup 입력 총 9건이 모두 거부됐으며 `CASES=9 passed=9`, `CACHE_RESTORED=True`, 최종 Gradle `help`도 성공했다. 이 검증은 개별 dependency request가 아니라 중앙 catalog version input guard를 대상으로 한다.

**Projects 후보 표적 검증 (2026-10-20 04:17 UTC):** 후보 `ae7e876b59c52365b2e36f33e9e405314883cea9`에서 workflow contract 9/9, `scripts/verify-catalog-workflows.py`, `actionlint .github/workflows/ci.yml`, Python compile, `git diff --check`, 계획 writer terminology audit(findings=[])가 통과했다. `./gradlew --no-daemon --no-configuration-cache --console=plain help`는 Gradle 9.8.1 `BUILD SUCCESSFUL`이다. 중앙 merge SHA `dcc7fa7557d3c0548b9e3fc28c48877827ea2f74`의 CI run `39359607639`와 Dependency Submission `39359608560`도 live read-back에서 success다. workflow receipt `verify`와 `resume-check`는 PASS/unlocked/running, `completion-check`는 component 증거 3개와 main verification이 없는 상태로 incomplete이며 이를 미완료로 유지한다.

## 범위와 실행 기준

- Projects 저장소: bluetape4k/bluetape4k-projects, base develop, 작업 branch fix/issue-1832-dependabot-standard-catalog, 승인 기준 HEAD 34c84a6115c80e3b3dfcde84be54106084259cfa.
- 중앙 저장소: bluetape4k/bluetape4k-dependencies, base develop, 최초 통합 계획의 기준 HEAD `e9946b18bdb0022f52cd5dc8994d859d470e7bab`; 최종 PR #269 merge SHA와 signed catalog authority는 `dcc7fa7557d3c0548b9e3fc28c48877827ea2f74`다.
- 실행 worktree는 이미 두 저장소에 각각 존재한다. 통합 branch와 기존 worktree의 변경은 건드리지 않는다.
- 로컬 경로는 Projects `/Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/fix/issue-1832-dependabot-standard-catalog`, 병합 후 canonical Central `/Users/debop/work/bluetape4k/bluetape4k-dependencies` (merge SHA `dcc7fa7557d3c0548b9e3fc28c48877827ea2f74`), workspace `/Users/debop/work/bluetape4k`로 고정한다. 이전 Central feature worktree `/Users/debop/work/bluetape4k/bluetape4k-dependencies/.worktrees/feat/dependabot-standard-catalog`는 보존한다. map, receipt, lock은 Git이 무시하는 Projects `$EVIDENCE_DIR` 아래에 두고 승인된 closeout까지 보존한다.
- 구현 명령에서 재사용할 변수는 다음과 같이 한 번 설정한다.

```bash
export PROJECTS_WT=/Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/fix/issue-1832-dependabot-standard-catalog
export DEPENDENCIES_WT=/Users/debop/work/bluetape4k/bluetape4k-dependencies
export WORKSPACE=/Users/debop/work/bluetape4k
export EVIDENCE_DIR="$PROJECTS_WT/.gradle/issue-1832-evidence"
export REPOSITORY_MAP="$EVIDENCE_DIR/repository-map.json"
mkdir -p "$EVIDENCE_DIR"
```
- Issue #1832는 OPEN, milestone 2.1.0, labels security/ci, assignee debop이다. 이슈는 새로 만들지 않고 기존 이슈에 연결한다.
- 같은 중앙 커밋에서 Projects PR을 검증하려면 immutable signed catalog tag가 필요하다. 중앙 PR 병합 후 `catalog/2026-10-20-01`을 명시적 승인에 따라 발행했고, GitHub signature verification과 peeled SHA를 확인했다. Projects pin과 로컬 검증은 후보 `ae7e876b59c52365b2e36f33e9e405314883cea9`에서 완료했다. 계획 문서 commit 뒤 최종 map/receipt/lock 갱신, Projects PR exact-head CI, merge 전 fresh approval이 남았다.
- Dependabot 보안 업데이트의 hosted Try again 재실행은 이전 요청에서 승인됐지만, Projects 변경이 기본 branch에 반영되고 exact-SHA dependency submission이 확인된 뒤에만 실행한다. 한 번의 실행 결과로 모든 alert가 해결됐다고 추정하지 않는다.
- 현재 Projects workflow receipt는 `verify` PASS, `completion-check` incomplete 상태다. `dependabot-execution`, `dependencies-catalog-promotion`, `projects-standard-catalog-view`와 main verification이 남아 있다. 구현 중 helper가 허용한 evidence만 기록하고, tag/Projects merge/Try again 전에는 완료로 처리하지 않는다. `.bluetape` 파일은 직접 수정하지 않는다.

## Catalog tag 및 Projects consumer closeout checklist (2026-10-20 03:26 UTC)

이 범위는 Maven stable release가 아니라 중앙 catalog authority tag와 Projects consumer 통합이다. 별도 Maven version/artifact publication은 수행하지 않는다. 이전 13개 게시 모듈의 POM/GMM 26개가 baseline과 byte-identical했고, Central PR exact-head publication contract도 통과했다.

| 고정 항목 | 현재 read-back |
|---|---|
| 후보 immutable tag | `catalog/2026-10-20-01`; GitHub signature verification `valid`, tag object `4919a6f511e57b4349ca6856816500b3a85ace95`, peeled SHA `dcc7fa7557d3c0548b9e3fc28c48877827ea2f74` (verified 2026-10-20 03:25 UTC) |
| 최신 관측 catalog tag | `catalog/2026-10-20-01`, GitHub signature verification `valid`, peeled SHA `dcc7fa7557d3c0548b9e3fc28c48877827ea2f74`; 직전 tag `catalog/2026-10-18-01`의 peeled SHA는 `e9946b18bdb0022f52cd5dc8994d859d470e7bab` |
| tag authority | `bluetape4k/bluetape4k-dependencies:develop` merge SHA `dcc7fa7557d3c0548b9e3fc28c48877827ea2f74`; canonical `develop` 및 `origin/develop` 일치 |
| consumer scope | `bluetape4k/bluetape4k-projects` 한 곳, base `develop`, branch `fix/issue-1832-dependabot-standard-catalog`; 다른 소비자는 변경하지 않는다 |
| Maven artifact matrix | artifact publication 없음. 기존 13 publisher의 POM/GMM 26개 byte-identical 증거와 Central PR exact-head POM contract PASS를 보존한다 |
| milestone/issue | Issue #1832 OPEN, milestone `2.1.0`; FreeMarker alert #235 CRITICAL/OPEN, GHSA-27j2-h3m2-8237, first patched `2.3.35` |
| 현재 실행 hold | Central merge-SHA CI run `39359607639` 및 Dependency Submission `39359608560` PASS. Projects pin candidate `ae7e876b59c52365b2e36f33e9e405314883cea9`에서 workflow contract 9/9, verifier, actionlint, Gradle 9.8.1 `help`, pinned parser 10개 의존성, strict generator receipt/lock PASS. 계획 문서 commit 뒤 최종 HEAD로 receipt를 갱신한다. Projects PR exact-head CI와 hosted Try again은 미실행 |

- [x] **CAT-01 — Pin target authority and consumer scope**
  - **Action:** 카탈로그 tag 후보, 정확한 Central commit, consumer 및 Maven publication 범위를 고정한다.
  - **Evidence:** 위 표의 tag/authority/consumer/artifact matrix; 기존 최신 tag의 서명과 peeled SHA를 live-read했다.
  - **Failure:** tag identity, authority 또는 consumer 범위가 바뀌면 downstream pin을 보류한다.
- [x] **CAT-02 — Validate Central source state**
  - **Action:** 정확한 merge commit, CI, Dependency Submission, local signing 설정 현황 및 후보 tag 부재를 확인한다.
  - **Evidence:** Merge SHA `dcc7fa7557d3c0548b9e3fc28c48877827ea2f74`; CI run `39359607639`의 8 success/0 failure, push 조건부 publication contract 1개 skip (PR exact-head contract 통과); Dependency Submission `39359608560` success; tag-signing 설정 값 존재 (실제 signing identity 가용성은 아직 검증하지 않음); 03:19 UTC 기준 후보 tag 없음.
  - **Failure:** 다른 commit이나 미검증 commit에는 tag하지 않는다. Central 변경 뒤 checklist를 갱신한다.
- [x] **CAT-03 — Validate and commit Projects candidate**
  - **Action:** 계획한 branch의 Projects 구현, 원본/생성 catalog, workflow와 보안 하한을 검토한다.
  - **Evidence:** Initial implementation commit `ec040eb40f57424659913e4125d56b14541ca146`와 후속 pin commit `ae7e876b59c52365b2e36f33e9e405314883cea9`; workflow contract 9/9; verifier/actionlint/Python compile/diff check PASS; Gradle `help` BUILD SUCCESSFUL; pinned parser 10 dependencies; Dokka resolver 98개/196 reports 및 catalog-input guard 9/9. 최종 receipt는 실행 계획 commit 뒤 새 HEAD로 재발행한다.
  - **Failure:** 서명 catalog ref 생성 전 수정 또는 재검토한다.
- [x] **CAT-04 — Create and push the signed catalog tag**
  - **Action:** fresh 명시적 권한과 직전 hold 재확인 후 Central merge SHA `dcc7fa7557d3c0548b9e3fc28c48877827ea2f74`에 `catalog/2026-10-20-01`을 서명해 push한다.
  - **Evidence:** 2026-10-20 03:24 UTC fresh hold에서 issue/PR/run/tag/HEAD를 확인한 뒤 승인에 따라 push했다. GitHub tag object `4919a6f511e57b4349ca6856816500b3a85ace95`의 verification은 `valid`; 원격 peeled SHA는 선언된 Central merge SHA `dcc7fa7557d3c0548b9e3fc28c48877827ea2f74`와 같다. 로컬 `git verify-tag`는 `gpg.ssh.allowedSignersFile` 미설정으로 검증하지 못했으며, GitHub의 독립 서명 검증이 통과했다.
  - **Failure:** ref 변경, signing identity 누락, 서명 실패 또는 기존 tag 존재 시 중단한다.
- [ ] **CAT-05 — Retarget and verify the Projects consumer**
  - **Action:** 후보에 새 signed tag와 peeled SHA를 pin하고 map, parser receipt, generator receipt, candidate lock을 재생성한 다음 Projects 전체 검증을 실행한다.
  - **Evidence:** Projects settings/workflow ref+SHA parity; strict receipt verification; pinned Dependabot Core commit의 parser contract; exact candidate HEAD의 필수 compile/test/CI.
  - **Failure:** 오래됐거나 불완전한 증거로 Projects PR을 열지 않는다.
- [ ] **CAT-06 — Open and validate the Projects PR**
  - **Action:** exact-head 검증 후 계획된 `bluetape4k/bluetape4k-projects:develop <- fix/issue-1832-dependabot-standard-catalog` PR을 연다.
  - **Evidence:** remote PR head SHA, 전체 CI, review/thread read-back, 한국어 본문, milestone `2.1.0`, labels `security`/`ci`, assignee `debop`.
  - **Failure:** 필수 contract 실패/skip 또는 미해결 P0/P1이 있으면 merge를 보류한다.
- [ ] **CAT-07 — Merge and verify dependency submission**
  - **Action:** fresh merge approval 뒤 검토한 exact PR head를 병합하고 merge-SHA Dependency Submission을 확인한다.
  - **Evidence:** PR merge SHA와 canonical `develop` 일치; Dependency Submission run이 같은 SHA를 checkout하고 기록함.
  - **Failure:** merge-SHA graph 증거가 통과하기 전 Dependabot을 재실행하지 않는다.
- [ ] **CAT-08 — Run hosted Dependabot and classify the alert delta**
  - **Action:** CAT-07 이후에만 이전 승인된 Issue #1832 Try again을 실행하고, 실행 직전/직후 alert baseline을 수집한다.
  - **Evidence:** run ID/log, dependency graph SHA, alert별 ID/GAV/outcome, 제공된 경우 update PR diff와 resolved version, 최종 live alert 상태.
  - **Failure:** open alert, updater error, 누락 alert 또는 `dependency_not_found`가 남으면 Issue #1832를 PENDING으로 유지하며, action 실행 성공만으로 해결을 추정하지 않는다.

## 변경 파일 지도

### 중앙 bluetape4k-dependencies

- 생성: scripts/project-dependabot-catalog.py, config/projects-dependabot-catalog.toml.
- 수정: scripts/catalog_candidate.py, scripts/sync-shared-versions.py, scripts/sync-dependabot-ignores.py, scripts/run-catalog-validation.py, scripts/sync-managed-catalog.py, scripts/sync-publishing-signing-support.py, scripts/migrate-catalog-authority.py, scripts/run-issues-242-243-validation.py, scripts/verify-issues-242-243-receipt.py, scripts/verify-publication-poms.py, .github/workflows/ci.yml.
- 수정/추가 테스트: tests/test_project_dependabot_catalog.py, tests/test_catalog_candidate.py, tests/test_ci_catalog_governance.py, tests/test_sync_shared_versions.py, tests/test_sync_dependabot_ignores.py, tests/test_run_catalog_validation.py, tests/test_sync_managed_catalog.py, tests/test_sync_publishing_signing_support.py, tests/test_migrate_catalog_authority.py, tests/test_run_issues_242_243_validation.py, tests/test_verify_issues_242_243_receipt.py, tests/test_verify_publication_poms.py; parser fixture는 tests/fixtures/project-dependabot-catalog/ 아래 둔다.

### Projects

- 이동: 기존 gradle/libs.versions.toml 원본을 gradle/libs.project.versions.toml로 옮긴다.
- 생성: 새 기본 gradle/libs.versions.toml, gradle/libs.project.versions.allowlist.json.
- 수정: .github/workflows/ci.yml의 catalog-governance와 ci-status, .github/workflows/dependency-submission.yml, build.gradle.kts, AGENTS.md, CLAUDE.md. 현재 경로 설명은 이 범위에서만 갱신한다. 이전 경로를 당시 상태로 기록한 `docs/review/2026-07-09-issue-1000-kafka4-compat-review.md`, `docs/review/2026-09-08-measured-infra-implementation-review.md`와 다른 모듈의 README는 역사적·비대상 자료로 둔다.
- .github/dependabot.yml의 weekly schedule, open-pull-requests-limit: 0, security-update opt-in은 그대로 둔다.
- 모듈을 추가/이동하지 않으며 CHANGELOG.md, 공개 Kotlin API, published source set은 변경하지 않는다. 검증 중 차이가 발견되면 계획을 다시 열고 별도 계약 판단을 한다.

## 구현 작업

### 구현 시작 전 필수 검토

- [x] 수정된 design과 이 plan을 main session에서 승인된 inline 경로로 performance, stability, security, operator/Ops, developer/API, user/caller, integration 관점에서 다시 검토했다. 독립 lane attestation은 주장하지 않는다.
- [x] Projects `docs/review/2026-10-19-issue-1832-catalog-design-review.md`에 설계 리뷰 표와 P0/P1/P2/P3 결과를 한국어로 남겼다. 리뷰 문서의 writer SPW-01..05는 모두 PASS다.
- [x] design, plan, review 파일의 `SPW-01`..`SPW-05` PASS와 acceptance mapping을 확인하고 세 파일을 Lore commit 형식으로 커밋한다. 이 commit 전에는 Task 1 구현을 시작하지 않는다.

**문서 게이트 증거 (2026-10-19):** 각 자료에 SPW-01..05를 적용한다. Korean terminology audit 명령은 `node /Users/debop/.codex/skills/bluetape-writer/scripts/audit-korean-terms.mjs --series clinic-appointment --json docs/superpowers/specs/2026-10-19-issue-1832-dependabot-standard-catalog-design.md docs/superpowers/plans/2026-10-19-issue-1832-dependabot-standard-catalog-plan.md docs/review/2026-10-19-issue-1832-catalog-design-review.md`이며 결과는 `files=3`, `findings=[]`다. 전체 readback 및 최종 staged diff 검사는 Lore commit 직전에 다시 수행한다.

| 자료 | SPW-01: 목적·근거·미확인 사항 | SPW-02: 구조 | SPW-03: 한국어 | SPW-04: 추적성 | SPW-05: 최종 readback |
|---|---|---|---|---|---|
| 설계 명세 | 목표·근거·미확인 사항을 기록 | 범위·결정·수용 기준·복구 순서를 구분 | 3개 자료 terminology audit 통과 | Dependabot/Python source와 16개 기준 기록 | PASS |
| 실행 계획 | 저장소·기준 HEAD·승인 경계 기록 | 순서형 Task 1–9와 위험 표 | 3개 자료 terminology audit 통과 | 명세 기준 1–16을 Task 1–9에 연결 | PASS |
| 설계 리뷰 | source ledger와 남은 검증 기록 | 7-Tier·초기 P1·게이트 상태를 분리 | 3개 자료 terminology audit 통과 | spec/plan SHA와 local call sites 연결 | PASS |

### Task 1: 생성기 계약 테스트를 먼저 작성한다

**대상 파일:**
- 생성: 중앙 tests/test_project_dependabot_catalog.py
- 생성: 중앙 tests/fixtures/project-dependabot-catalog/
- Test: 중앙 tests/test_project_dependabot_catalog.py

- [x] test fixture에 Projects 원본 TOML, 303개 alias allowlist 샘플, 중앙 TOML, 열 개 좌표 매핑 샘플, repository map v1/v2 샘플을 둔다.
- [x] 열 개 GAV가 설계 표의 중앙 원천과 정확히 대응하는지 검사한다. Jackson 2/3은 version key, Netty는 netty version key, RabbitMQ/Bouncy Castle/FreeMarker/jsoup은 중앙 library alias에서 버전을 해석한다.
- [x] 원본의 non-target 선언 내용과 상대 순서, alias 집합을 유지하는지와 생성 결과의 반복 실행 바이트가 같은지 검사한다.
- [x] 원본 library alias의 version/version.ref, plugin/bundle, 변조 GAV, 동적/rich version, 미해결 ref, 중복 좌표, alias 정규화 충돌을 각각 거부하는 테스트를 쓴다.
- [x] 주입한 쓰기·atomic replace·read-back 실패 후 기존 출력 SHA-256이 유지되는지 검사한다.
- [x] receipt/lock이 모든 경로, input/output SHA-256, Projects/central full HEAD, central peeled commit, Python 버전, map bytes를 결속하고 재사용·변조 receipt를 거부하는 테스트를 쓴다.
- [x] `test_projection_preserves_unmapped_alias_bytes_and_order`와 `test_rejects_versioned_source_alias`부터 작성한다. 첫 실행은 생성기 모듈이 아직 없어 실패해야 하며, 실패 원인이 assertion 또는 import 경로임을 확인한다.
- [x] 실행: `python3.13 -m unittest tests.test_project_dependabot_catalog`; 예상 결과: 구현 전 실패, 구현 후 전체 PASS.

### Task 2: Projects 표준 catalog 투영 생성기를 구현한다

**대상 파일:**
- 생성: 중앙 scripts/project-dependabot-catalog.py
- 생성: 중앙 config/projects-dependabot-catalog.toml
- 수정: Task 1의 fixture/test

- [x] tomllib import 전에 sys.version_info >= (3, 11)을 확인하고 지원 하한보다 낮으면 입력 읽기와 쓰기 전에 명확히 실패한다.
- [x] mapping을 명시적으로 정의한다. 별칭/GAV 추측을 금지하고 central alias의 module과 resolved fixed version을 확인한다.
- [x] 프로젝트 원본의 library alias는 전부 versionless여야 하며, generated catalog에서 version이 있는 library alias는 매핑 열 개만 허용한다.
- [x] TOML 전체를 다시 직렬화하지 않고 non-target 원본 구간을 보존한다. 생성은 고정 경로에서만 허용하고 같은 입력에 대해 결정적 바이트를 낸다.
- [x] generator receipt와 candidate lock을 canonical JSON으로 쓰고, 입력 HEAD·해시 재확인, 임시 파일 fsync, atomic replace, directory fsync, read-back, 복구 동작을 구현한다.
- [x] 작업은 fixture 기반 단위 테스트가 먼저 실패한 뒤 최소 구현으로 통과하도록 한다.
- [x] 실행: python3.13 -m unittest tests.test_project_dependabot_catalog; 예상 결과: 전체 PASS, 음성 사례는 각기 실패 처리.

### Task 3: repository map v2와 모든 consumer를 함께 갱신한다

**대상 파일:**
- 수정: 중앙 scripts/catalog_candidate.py (repository-map v2와 project 전용 map builder `build-project-map`)
- 수정: 중앙 scripts/sync-shared-versions.py, scripts/sync-dependabot-ignores.py, scripts/run-catalog-validation.py, scripts/sync-managed-catalog.py, scripts/sync-publishing-signing-support.py, scripts/migrate-catalog-authority.py, scripts/run-issues-242-243-validation.py, scripts/verify-issues-242-243-receipt.py, scripts/verify-publication-poms.py, .github/workflows/ci.yml
- 수정: 대응하는 tests/test_catalog_candidate.py, tests/test_ci_catalog_governance.py, tests/test_run_catalog_validation.py 및 변경 consumer별 단위 테스트

- [x] repository-map v2 정규화 API에 catalog와 authority_catalog을 추가한다. v1은 authority_catalog = catalog 기본값으로 기존 의미를 유지한다.
- [x] Projects v2 entry는 gradle/libs.versions.toml 생성 catalog와 gradle/libs.project.versions.toml authority catalog를 각각 잠근다. 다른 저장소의 경로는 기존 경로를 유지한다.
- [x] 기존 CI inline map builder는 full repository map v2를 만들도록 갱신한다. 별도 `build-project-map` 명령은 로컬의 두 승인 worktree만 관측해 generator 전용 canonical map을 쓰며, 다른 저장소 worktree의 상태에 의존하지 않는다.
- [x] 직접/간접 consumer 열 곳 모두 같은 loader/API를 사용하도록 수정한다. `run-catalog-validation.py`도 manifest의 repository map을 다음 consumer에 전달하므로 v2 정규화 API를 사용해야 한다. map을 직접 읽지 않는 소비 경로는 기존 tests와 call graph로 확인하고 근거를 남긴다.
- [x] v1/v2, 파일 경로 escaping, 심볼릭 링크, repository-root mismatch, stale HEAD, lock/receipt/map/catalog SHA 불일치를 거부하는 회귀 테스트를 추가한다.
- [x] sync-shared-versions.py에 --catalog-filename libs.project.versions.toml을 추가하고 이 선택자는 --repo bluetape4k-projects 단독 지정에서만 허용한다. 공유 버전, compatibility, adoption-gap, authority inventory가 같은 source catalog를 읽게 한다.
- [x] 실행: `python3.13 -m unittest tests.test_catalog_candidate tests.test_ci_catalog_governance tests.test_sync_shared_versions tests.test_sync_dependabot_ignores tests.test_run_catalog_validation tests.test_sync_managed_catalog tests.test_sync_publishing_signing_support tests.test_migrate_catalog_authority tests.test_run_issues_242_243_validation tests.test_verify_issues_242_243_receipt tests.test_verify_publication_poms`.

### Task 4: pinned Dependabot parser evidence와 중앙 CI를 연결한다

**대상 파일:**
- 수정: 중앙 config/projects-dependabot-catalog.toml, scripts/project-dependabot-catalog.py
- Create/Modify: 중앙 tests/fixtures/project-dependabot-catalog/ parser fixture 및 검증 코드
- 수정: 중앙 .github/workflows/ci.yml

- [x] dependabot-core full commit을 설정에서 고정하고 Gradle parser fixture가 generated 기본 catalog에서 열 개 GAV와 version.ref를 모두 읽는지 검사한다.
- [x] parser receipt에 upstream commit, fixture 경로/SHA-256, generated catalog 경로/SHA-256, GAV별 alias/version을 기록하고 candidate lock과 교차 검증한다.
- [x] 중앙에 새 generator 전용 test job을 두고 `actions/setup-python@v7`의 Python `3.11`/`3.13` matrix에서 unit suite를 실행한다.
- [x] Python을 실행하는 central `build`, `publication-pom-contract`, `supply-chain-report-only` jobs 각각에서 첫 Python invocation 전에 `actions/setup-python@v7`로 Python 3.13을 고정하고 `python3 --version`을 기록한다. generator 전용 test job은 위의 3.11/3.13 matrix를 쓴다.
- [x] central CI는 고정된 Projects fixture로 generator/parser/candidate lock을 검증하고 기존 v1 repository map을 계속 검증한다.
- [x] 실행: `python3.13 -m py_compile scripts/project-dependabot-catalog.py scripts/catalog_candidate.py tests/test_project_dependabot_catalog.py`; 이어 해당 consumer 8개를 대상으로 한 기존 unittest 모음과 `python3.13 -m unittest discover -s tests -p 'test_*.py'`.

> Python 3.11은 표준 `tomllib` 사용을 위한 호환성 하한이며, 중앙 CI의 일반 Python jobs는 3.13을 쓴다. generator 전용 CI matrix는 3.11/3.13에서 하한 호환성을 확인한다.

### Task 5: Projects source, allowlist, generated catalog, Gradle 제약을 연결한다

**대상 파일:**
- 이동: Projects gradle/libs.versions.toml -> gradle/libs.project.versions.toml
- 생성: Projects gradle/libs.project.versions.allowlist.json
- 생성: Projects generated gradle/libs.versions.toml
- 수정: Projects build.gradle.kts, .github/workflows/ci.yml, AGENTS.md, CLAUDE.md

- [x] 원본의 303개 alias/GAV에서 정렬된 allowlist를 만든다. 원본 alias는 versionless이고 plugins/bundles는 비어 있어야 한다.
- [x] `python3.13 scripts/project-dependabot-catalog.py --projects-root "$PROJECTS_WT" --central-root "$DEPENDENCIES_WT" --write`로 투영 파일을 생성한다. 이 준비 모드는 결정적 출력을 쓰되 provenance receipt를 발행하지 않으며, receipt와 candidate lock은 Task 7의 clean committed HEAD 검증에서만 발행한다.
- [x] 결과의 10개 versioned alias, 기존 alias 전체, 결정적 바이트, allowlist 일치를 확인한다.
- [x] root catalog accessor baseline과 candidate를 비교한다. 기존 accessor를 모두 보존하고 예상 추가 libs.amqp.client, libs.bouncycastle.bcprov, libs.bouncycastle.bcprov.lts8on, libs.freemarker, libs.jsoup만 허용한다.
- [x] 모든 Dokka 구성의 jsoup 및 FreeMarker 선택 버전을 중앙 카탈로그에 결속하고, 숫자 dotted-release 비교와 최소 보안 버전(jsoup 1.23.1, FreeMarker 2.3.35)을 검사한다.
- [x] catalog path를 직접 읽거나 원본을 수정하는 안내를 새 gradle/libs.project.versions.toml / generated gradle/libs.versions.toml 역할에 맞게 고친다.

### Task 6: Projects CI와 Dependency Submission을 exact SHA에 결속한다

**대상 파일:**
- 수정: Projects .github/workflows/ci.yml
- 수정: Projects .github/workflows/dependency-submission.yml
- 생성: Projects `tests/test_catalog_workflows.py` (Python stdlib unittest, workflow structural contract checks; YAML semantics are also validated by actionlint)
- 생성: Projects `scripts/verify-catalog-workflows.py`

- [x] catalog-governance가 exact immutable central ref와 peeled commit을 확인하고 generator --check, repository map v2, allowlist, parser receipt, stale generated output, authority-source sync를 검증하게 한다.
- [x] `catalog-governance` job에서 Python 3.13을 `actions/setup-python@v7`로 고정해 generator와 contract tests를 실행한다.
- [x] 같은 job에서 mapping의 pinned `dependabot-core` commit을 checkout하고 parser fixture를 실행해 `.gradle/issue-1832-evidence/parser-receipt.json`을 generator `--check`보다 먼저 생성한다. 별도 workflow의 receipt 파일은 공유되지 않으므로 대체 입력으로 쓰지 않는다. Ruby dependency 설치를 포함해 job timeout은 30분으로 둔다.
- [x] ci-status가 catalog-governance가 success일 때만 통과하도록 고치고 skipped를 성공으로 취급하지 않는다.
- [x] Dependency Submission은 github.sha를 checkout하고 checkout HEAD와 github.sha를 비교한다. run head_sha, 제출 graph의 commit SHA를 artifact/log에 남긴다.
- [x] concurrency key는 SHA를 포함하고 cancel-in-progress: false로 설정해 다른 SHA 실행을 취소하지 않는다.
- [x] workflow contract 테스트는 잘못된 ref, HEAD 불일치, skipped governance, 다른 SHA의 동시 실행 취소를 각각 거부한다.
- [x] 실행: 변경한 workflow contract 테스트, actionlint .github/workflows/ci.yml .github/workflows/dependency-submission.yml, git diff --check.

**Projects parser-receipt 공백을 발견·수정한 날짜 (2026-10-20):** 새 Projects runner에는 parser receipt가 없는데 `--check`는 이를 필수 입력으로 읽었다. 계약 테스트를 먼저 추가해 실패를 확인한 뒤, catalog-governance 안에서 full SHA pinned Dependabot Core를 checkout하고 parser fixture를 실행하도록 고쳤다. 계약 테스트는 parser 실행이 generator `--check`보다 앞서는지 검사한다. 중앙 PR의 별도 parser job은 파일을 공유하지 않으므로 이를 Projects 검증 증거로 계산하지 않는다.

### Task 7: baseline/candidate 그래프, API/ABI, POM/GMM, toolchain을 비교한다

**대상 파일:**
- 이 비교 단계에서는 Task 3에서 갱신한 중앙 `scripts/run-catalog-validation.py`와 map 소비자 테스트를 추가 수정 없이 사용한다. `scripts/verify-publication-poms.py`의 결과와 함께 Gradle `dependencies`/`dependencyInsight` 결과를 비교한다. 필요한 차이를 입증하지 못하면 계획을 다시 열고 대상 파일과 테스트를 구체적으로 추가한다.
- 증거: 두 저장소의 baseline/candidate 명령 결과 및 receipt

- [x] Tasks 1–6의 구현과 생성 결과를 Lore 형식으로 커밋했다. clean Projects/Central worktree에서 canonical repository map을 만들고 pinned parser receipt와 함께 `project-dependabot-catalog.py --check`를 실행해 generator receipt와 candidate lock을 발행·검증했다. 증거는 ignored `.gradle/issue-1832-evidence` 아래에 보존한다. 계획 문서 commit으로 Projects HEAD가 바뀐 뒤 PR 전 map/receipt/lock을 새 HEAD로 다시 발행한다.
- [x] Projects worktree에서 `./gradlew --no-daemon help` 및 실제 `bt4kVersion`/`libs` alias caller에서 확인한 영향 모듈의 compile/test를 실행한다. 대상 모듈과 명령은 이 Task의 baseline inventory에서 구체화하고 테스트 전에 임의로 생략하지 않는다.
- [x] baseline Projects HEAD에서 영향 모듈의 Gradle resolution과 dependencyInsight를 순차로 수집한 뒤 candidate에서 반복한다. 열 개 GAV별 configuration, requested/selected version, path를 비교한다.
- [x] resolved graph, API/runtime classpath, published POM 및 Gradle Module Metadata를 비교한다. dependency edge/version/scope/variant/capability/constraint 차이가 있으면 실패 처리하고 별도 public-contract 승인 전에는 PR을 진행하지 않는다.
- [x] 공개 Kotlin/Java source 또는 ABI surface가 변경되는 경우에만 consumer fixture/API dump/`javap -public -s`를 baseline과 candidate에서 비교한다. 코드 파일 변경이 없고 dependency path·POM/GMM이 같다면 API/ABI는 N/A로 기록하고 그 범위 증거를 명시한다.
- [x] Gradle Wrapper 9.8.1/checksum, Kotlin Gradle Plugin 2.4.20, language/API 2.4, Temurin JDK 21/25를 기록한다. Java 21 island 및 최소 dependency closure를 Java 25 기본 target과 분리해 확인한다.
- [x] 모든 Dokka configuration에서 org.jsoup:jsoup 및 org.freemarker:freemarker의 최종 선택 버전이 중앙 catalog와 같음을 dependencyInsight로 검사한다. 실제 내부 runtime resolver 98개, 보고서 196건이 중앙 catalog 선택과 일치했다. 중앙 catalog version input guard의 jsoup 1.16.1/1.23.0, FreeMarker 2.3.34, dynamic/range/rich-range/qualifier/malformed/empty 9개 음성 사례를 확인했다. 이들은 개별 dependency request 사례가 아니다.
- [x] Testcontainers와 real DB 테스트가 필요하면 module/worktree 사이에서 순차 실행한다. 모듈 추가/이동이 없음을 diff와 settings auto-registration 검사로 확인한다.

### Task 8: pre-PR 리뷰, lesson, commit, PR 및 CI를 처리한다

**대상 파일:**
- 생성: Projects `docs/lessons/2026-10-20-issue-1832-dependabot-standard-catalog.md`
- 생성: Projects `docs/review/2026-10-20-issue-1832-catalog-implementation-review.md`
- 생성: central `docs/review/2026-10-20-issue-1832-catalog-implementation-review.md`
- 수정: 두 저장소의 승인된 구현 파일만

- [x] 중앙과 Projects의 diff를 각 저장소의 Kotlin/Python/workflow 패턴에 따라 7-Tier로 inline review한다. 보안, 정확성/lifecycle, 구조/reuse, Kotlin API/ABI, 테스트/callers, 성능, docs/CI/release를 분리해 P0/P1=0을 증명한다.
- [x] 승인된 inline review로 7-Tier 결과를 기록하고 독립 reviewer attestation은 포함하지 않는다. GitHub human-reviewer subgate는 단일 개발자 lane 근거로 N/A 처리한다고 기록했다. exact-head CI와 review/thread read-back은 아직 대기 중이며, 현재 model registry/runtime에서 gpt-6-luna max provenance가 확인되지 않으면 이를 독립 증명으로 표현하지 않는다.
- [x] 표준 catalog와 원본 catalog 분리, versionless alias 보존, immutable tag 승인 순서에서 재사용할 교훈을 한국어 lesson에 기록하고 writer SPW-01..05를 통과시킨다.
- [x] Central parser/consumer review 수정은 PR #269로 병합했다. Projects tag-ref/SHA pin은 Lore 형식의 `ae7e876b` commit으로 반영했고 workflow contract 9/9, verifier, actionlint, Gradle help, pinned parser 10 dependencies, strict generator receipt/lock 검증을 통과했다. `Tested:` 범위는 이 로컬 후보까지이며 Projects PR exact-head CI와 merge는 별도 단계다.
- [x] 승인된 대상 `bluetape4k/bluetape4k-dependencies:develop <- feat/dependabot-standard-catalog`에 중앙 PR #269을 만들고, 정확한 head 및 CI를 받은 뒤 한국어 본문을 공통 PR template에 맞춰 갱신했다. 본문 마지막 heading은 `## DoD Status`다. PR labels `security`/`ci`, milestone `2.1.0`, assignee `debop`가 Issue #1832와 일치한다.
- [x] 중앙 PR의 최신 exact remote head `80ae0d6bf2525c1d434fabe87993905c0fc10197`에서 CI와 review/thread read-back을 완료했다. merge 전 read-back에서 PR은 OPEN/CLEAN/MERGEABLE였으며 branch-protection required checks는 GitHub에서 보고되지 않았다. exact-head run `39295843422`의 9개 job 전부 성공했다. `Build BOM`은 10m06s, publication POM contract는 10m12s에 성공했고, catalog script suite 103개 및 Central suite 515개 실행(4 skipped)이 OK였다. `Verify downstream repository sync`, 소비자 입력 목록 생성/업로드는 PR event 조건으로 실행되지 않았고, `1.9.2-SNAPSHOT` artifact 조회는 HTTP 403으로 건너뛰었다. 같은 SHA Dependency Submission `39295845219`도 성공했다. 최신 CI 후 reviews, issue comments, inline comments, GraphQL threads는 모두 비어 있다. P0=0/P1=0이며 inline fallback review는 독립 reviewer attestation이 아니다. human-reviewer subgate는 single-developer lane 근거로 N/A다. `39285492069`의 parser/ref 오류는 수정됐고 `39288320776` 취소 이후 여러 exact-head run이 통과했다. merge는 fresh approval을 받아 완료했다. signed tag에는 별도 fresh approval이 필요하다.
- [x] 중앙 PR #269은 fresh approval을 받은 뒤 `--rebase`로 병합하고 exact merge SHA `dcc7fa7557d3c0548b9e3fc28c48877827ea2f74`를 read-back했다. canonical Central `develop`을 `origin/develop`까지 fast-forward 동기화했다. merge-SHA CI run `39359607639`는 8 success/0 failure로 완료됐고 push 조건부 publication contract 1개가 skip됐다. 이 contract는 PR exact-head에서 통과했으며 merge-SHA Dependency Submission `39359608560`도 성공했다.
- [x] signed `catalog/2026-10-20-01` tag를 별도 승인 뒤 발행했다. GitHub signature verification과 peeled SHA `dcc7fa7557d3c0548b9e3fc28c48877827ea2f74`가 Central canonical `develop` HEAD와 일치한다.
- [ ] Projects pin/mapping/lock을 signed tag의 peeled SHA로 갱신하고 PR #1838을 열었다. 최초 run `39370896190`의 tag/parser 결함은 `9dc756c7`에서 고쳤다. retry `39371790611`은 Central SHA-only pin 검사와 gitleaks false positive를 찾았고, 수정 commit `85d0312cf9b7e9c97dc592645582a341a4164ab3`을 push했다. 그 뒤 exact-head run `39373076530`은 CI adoption 검사에서 tag REF를 SHA로 찾지 못해 실패했고 제품 Build/Test 15개 job이 skip됐다. 두 REF/TAG 회귀 검사는 수정 전 의도대로 RED였으며, REF를 peeled SHA로, checkout ref를 별도 TAG로 분리한 로컬 후보에서 workflow contract 12개와 verifier, actionlint, Python compile, diff check, Central adoption 비교 `[]`, Gradle help가 통과했다. 후보를 commit/push한 뒤 새 exact-head CI와 독립 최종 리뷰를 완료한다.
- [ ] PR head `3ae9be4d36bdb6eb9d61289cd62d5ffac9d7461c`의 CI run `39408775102`는 Central adoption과 다른 초기 checks를 통과했지만, `pull_request`의 기본 checkout이 synthetic merge SHA `41a685c7…`를 사용해 generator map/receipt가 PR head에 결속되지 않았다. `PROJECTS_EXPECTED_SHA`를 PR head 우선으로 선택하고 catalog-governance checkout, HEAD assertion, generator branch를 일치시키는 회귀 수정을 로컬에 반영했다. 수정 전 merge-SHA regression test는 RED, 수정 후 workflow contract 14개와 verifier/actionlint/Python compile/diff check는 PASS다. 이 후보를 commit/push하고 새 exact-head CI 및 독립 최종 리뷰를 통과시킨 뒤 Task 8을 완료한다.
- [ ] 각 PR merge는 merge-ready DoD 이후 fresh approval을 받아 수행한다. auto-merge는 설정하지 않는다.

### Task 9: 실제 hosted Dependabot 결과와 Issue #1832를 판정한다

**선행 조건:** 승인된 Projects merge, merge SHA, exact SHA dependency-submission 성공, 서명된 중앙 tag pin 확인.

- [ ] implementation 직전, Try again 직전, 재실행 후에 Dependabot open alert 목록을 다시 읽는다. alert ID/GAV/firstPatchedVersion을 세 시점으로 비교한다.
- [ ] 각 현재 대상 alert에서 승인된 hosted Try again 경로로 실행하고 alert마다 updated, already-patched, no-update-available, not-detected, updater-error, unknown 중 하나의 증거를 보존한다.
- [ ] updated는 실제 PR diff와 resolved patched version, already-patched는 패치 graph와 fixed, no-update-available은 계속 열린 alert와 PENDING을 확인한다.
- [ ] alert 누락, open alert, updater error, dependency_not_found, 또는 상태 불명 항목이 하나라도 있으면 Issue #1832를 open으로 유지한다. 모든 대상 alert가 종료되고 final graph가 패치 버전을 선택할 때만 해결 처리한다.
- [ ] Issue를 해결한 뒤 merge, canonical sync, 보존 대상 worktree, 정리, GNO indexing을 각 워크플로우 gate에 따라 확인한다.

## 수용 기준 추적

| 명세 기준 | 계획 작업 |
|---|---|
| 1–4: 열 개 중앙 매핑, 보존·결정성, allowlist, 실패 입력 | 1–2, 5 |
| 5: repository-map v1/v2와 전체 consumer | 3 |
| 6: 서명된 중앙 태그와 모든 pin/checkout 일치 | 3–6, 8의 승인 대기 |
| 7: provenance receipt, atomic write, lock/replay 거부 | 1–2 |
| 8: central/Projects CI와 exact SHA dependency submission | 4–6 |
| 9: pinned parser, accessor baseline, 10개 alias만 versioned | 1, 4–6 |
| 10: dependencyInsight 및 version-selected path | 7 |
| 11: graph/API/ABI/POM/GMM와 baseline consumer | 7 |
| 12: Gradle/Kotlin/JDK/Java 21 closure/Dokka 보안 의존성 | 5, 7 |
| 13: 두 PR exact-head CI/review | 8; tag 승인 전 Projects 작업은 PENDING |
| 14: alert baseline/실행 결과별 판정 | 9 |
| 15: retry/hold/revert recovery | 8–9 |
| 16: Python 3.11 minimum, Python 3.13 CI | 2, 4 |

## 위험 예측과 복구

| 위험 | 징후 | 완화 / 복구 |
|---|---|---|
| 중앙 카탈로그 투영으로 Gradle 해석 또는 게시 결과가 바뀜 | `dependencyInsight`, API/runtime classpath, POM/GMM, API dump 또는 ABI fixture가 다름 | PR 진행을 멈추고 좌표와 artifact 차이를 기록한다. 작업 브랜치에서 기준 pin과 생성 파일을 복구한다. 게시 동작을 바꾸려면 별도 공개 계약 결정을 연다. |
| repository-map v2가 기존 소비자를 깨뜨림 | v1 fixture/consumer 실패 또는 v2 경로·lock SHA 불일치 | v1 호환 검사를 먼저 유지하고 소비자 하나씩 복구한다. 전체 consumer suite가 통과하기 전에는 Projects 브랜치를 갱신하지 않는다. |
| receipt 재사용 또는 경로 대체 | exact field/hash/head/peeled/map 검증 실패 또는 심볼릭 링크·비정규 경로 허용 | 출력 교체 전에 거부하고 기존 생성 바이트를 보존한다. 해당 경로를 조사한다. |
| 새 기본 catalog가 의도하지 않은 Dependabot 후보를 노출함 | 매핑되지 않은 원본 alias에 version/version.ref가 있거나 생성된 버전 지정 alias 집합이 매핑 열 개와 다름 | 생성을 거부한다. 원본 303개 alias는 모두 versionless로 유지하고 버전은 중앙 매핑에서만 바꾼다. |
| signed tag 발행 전에 Projects가 중앙 pin을 검증할 수 없음 | CI가 후보 SHA의 승인된 immutable tag를 가져오거나 검증하지 못함 | Projects PR 생성과 merge 의존 실행을 보류한다. 별도 명시 승인이 오기 전에 태그를 만들거나 push하지 않는다. |
| 구현 중 hosted alert 목록이 바뀜 | alert ID/GAV가 시작 기준과 다르거나 새 alert가 나타남 | 새 ID/GAV를 작업 ledger에 추가하고 분류한 뒤 세 시점 목록을 대조한다. Issue #1832를 해결 처리하기 전에 결과를 조정한다. |
| Python 실행 버전이 지원 범위와 다름 | 버전이 3.11 미만이거나 `tomllib`을 사용할 수 없음 | 입력을 읽거나 쓰기 전에 실패시킨다. 중앙 검증은 Python 3.13을 쓰고 CI도 3.13으로 고정한다. |

## 계획 인라인 리뷰

검토 방식: 승인된 inline 경로. 독립 reviewer attestation은 사용하지 않는다. 첫 재검토에서 확인한 P1 세 건(consumer 목록 누락, map 삭제와 receipt 보존 계약 충돌, PR 리뷰 방식의 승인 범위 불일치)을 수정하고 관련 관점을 다시 확인했다.

| 관점 | 검토 결과 | 처리 |
|---|---|---|
| Performance | generator는 303개 작은 alias 목록을 선형 검증하며 런타임 Kotlin artifact에는 코드를 추가하지 않는다. | P2 없음; 결정성·불필요 직렬화 금지 유지 |
| Stability | v1 호환, input 재해시, clean HEAD map, atomic replace/read-back, versionless 원본 검증을 단계별로 둔다. | P0=0, P1=0 |
| Security | canonical non-symlink path, 고정 GAV mapping, immutable signed tag, alert 결과별 판정이 명시돼 있다. | P0=0, P1=0 |
| Operations | exact SHA dependency submission, cancel-in-progress 비활성화, helper-only receipt, map을 포함한 evidence 보존, merge/tag/retry 승인 경계를 분리했다. | P0=0, P1=0 |
| Developer/API | 기본 `libs` alias는 유지하고 예상 신규 accessor 다섯 개만 허용한다. 게시 API/소스 변경은 별도 계약 검토 전 차단한다. | P0=0, P1=0 |
| User/caller | 중앙 map consumer 열 곳 모두 Task 3에 포함하고 v1/v2 검증, Gradle graph/accessor/POM/GMM 비교를 수용 기준에 연결했다. | P0=0, P1=0 |
| Integration | dirty worktree에서 provenance를 만들 수 없는 기존 helper 제약을 반영해 출력 생성과 clean committed HEAD receipt 검증을 순서화했다. tag 승인 전에는 Projects PR/hosted retry를 차단한다. | P0=0, P1=0 |

계획 재검토 결과: 모호했던 실행 경로와 문서 범위, CI governance 테스트 누락, 조기 map 생성 순서를 앞선 수정에 반영했다. 이번 검토에서는 map 소비자와 map 증거 보존을 구현 파일 및 closeout 순서에 명시하고, PR 검토 범위를 승인된 inline 방식으로 고쳤다. 수용 기준 1–16을 Task 1–9에 대조했다. 요청 모델은 `gpt-6-luna max`이며 실제 session provenance는 검증되지 않았으므로 독립 attestation으로 주장하지 않는다. 중앙 tag, PR merge, Projects PR/merge, hosted retry, sync, cleanup 경계는 서로 분리되어 있다.
