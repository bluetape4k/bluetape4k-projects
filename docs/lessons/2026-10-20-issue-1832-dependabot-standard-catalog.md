# Issue #1832: catalog 선언과 실행 경계별 증거를 함께 검증하기

## 맥락

Projects의 중앙 버전 소유권 catalog는 Gradle 내부 alias의 버전 정보를 중앙 관리하지만 GitHub Dependabot이 읽는 표준 경로와 같지 않았다. Issue #1832는 Gradle 보안 업데이트가 제출된 dependency graph에서 catalog 관리 좌표를 찾지 못해 종료되는 현상을 다룬다. 현재 GitHub issue는 OPEN이다. 2026-10-20 조회에서 FreeMarker alert #235도 CRITICAL 상태로 OPEN이었다.

FreeMarker 보안 공지는 2.3.34 이하를 영향 범위, 2.3.35를 수정 버전으로 지정한다. Projects build 설정은 Dokka 구성에서 중앙 catalog 버전을 선택하고, 최소 2.3.35 아래로 내려가면 빌드를 실패시킨다. [GitHub advisory](https://github.com/advisories/GHSA-27j2-h3m2-8237)와 [Apache 2.3.35 릴리스 노트](https://freemarker.apache.org/docs/versions_2_3_35.html)를 참조한다.

## 결정

Projects의 기존 catalog를 versionless 원본으로 분리하고, 중앙 mapping에서 고른 열 개 GAV만 표준 gradle/libs.versions.toml에 version.ref로 투영한다. generator는 원본 alias와 순서를 유지하고 allowlist, repository map, parser receipt, candidate lock을 서로 해시로 결속한다. CI는 Dependabot Gradle parser가 실제 표준 catalog에서 열 개 좌표를 읽는지 고정 upstream commit으로 확인한다.

각 workflow job은 자체 파일시스템 경계를 가진다. 다른 job의 parser receipt를 사용할 수 없으므로 Projects catalog-governance도 parser fixture를 실행하고 자기 receipt를 만든다. 같은 이유로 Central의 push-only source audit는 Projects 원본 catalog를 직접 읽어야 한다.

## 결과와 검증

- commit `85d0312cf9b7e9c97dc592645582a341a4164ab3` 기준 Projects local workflow contract 테스트 11개와 actionlint, verifier, Python compile, diff check가 통과했다.
- Central 전체 Python suite 514개 통과, 2개 skip. 두 skip은 관리 sibling 저장소가 격리 worktree에서 보이지 않아 real-workspace inventory를 실행하지 않은 결과다.
- 외부 1.9.2-SNAPSHOT artifact 검사 한 건은 HTTP 403으로 건너뛰었다. 제품 테스트 실패로 계산하지 않는다.
- 98개 Dokka configuration에서 FreeMarker 2.3.35와 jsoup 1.23.2를 확인했다. 13개 게시 모듈의 POM과 Gradle Module Metadata는 26개 모두 baseline과 byte-identical했다.
- 대상 모듈 classes와 mock-web-server/mock-webflux-server Jib 빌드가 앞선 검증에서 통과했다.
- 이 결과는 parser PR CI나 hosted Dependabot 성공을 뜻하지 않는다. 현재 FreeMarker alert #235는 계속 OPEN이며 Issue #1832도 해결 처리하지 않는다.

## 놓친 점과 재발 방지

첫 구현에서는 Projects CI가 다른 workflow의 parser receipt를 사용할 수 있다고 가정했다. 새 runner의 파일시스템에는 그 receipt가 없어서 generator check가 실패할 수 있었다. workflow 계약 테스트를 먼저 실패시키고, parser contract를 같은 job 안에서 실행해 receipt를 생성하도록 수정했다.

두 번째 구현 공백은 Projects catalog를 나눈 뒤 Central push workflow가 여전히 기본 generated catalog를 authority로 읽는 경로였다. Projects 원본 파일이 존재할 때 source catalog를 지정하도록 수정하고, 이전 구조의 저장소를 위한 legacy fallback을 보존했다.

새 검증 자료를 추가하면 실행 경계마다 파일이 공유되는지 확인한다. catalog authority를 나누면 정규 map loader뿐 아니라 모든 CI audit와 간접 consumer가 source/generated 역할을 구분하는지 확인한다. 증거 생성 전후로 exact HEAD와 파일 해시를 재검증하며 map, receipt, lock을 closeout까지 보존한다.

## PR exact-head CI에서 찾은 실행 경계 결함

PR #1838의 첫 exact-head run `39370896190`은 Projects HEAD `ac3d40b70367eec66091da02a35d078bdc6bf9b1`에서 실패했다. `JVM Release Contract`는 설정 단계에서 signed tag `catalog/2026-10-20-01`을 commit SHA가 아니라고 거부했다. `Central Catalog Governance`는 Dependabot Core용 `Gemfile` 밖에서 parser fixture를 일반 `ruby`로 실행해 `toml-rb`를 불러오지 못했다. governance 실패가 downstream gate를 닫아 15개 동적 build/test job은 모두 skip되었다. 제품 테스트가 실패한 것은 아니며 해당 run은 제품 테스트 증거도 제공하지 않는다.

수정 commit `9dc756c7be448fbb62b7d2f4cde10c4904b8ac67`은 설정에서 허용된 catalog tag와 peeled exact SHA를 함께 요구하고, 실제 파일 다운로드와 cache key는 SHA로 고정한다. parser fixture는 pinned Dependabot Core `Gemfile`을 `BUNDLE_GEMFILE`로 지정하고 `bundle exec ruby`로 실행한다. 두 실행 경계 회귀 테스트는 수정 전 RED, 수정 후 GREEN을 확인했다. 이후 Projects contract 테스트는 11개로 늘어 모두 통과했다. Dependabot Core bundle의 실제 hosted 실행은 수정된 PR exact-head CI에서 다시 확인해야 한다.

수정 HEAD `9dc756c7be448fbb62b7d2f4cde10c4904b8ac67`의 retry `39371790611`은 parser와 JVM settings 단계를 통과했지만, Central generator가 `BLUETAPE4K_DEPENDENCIES_CATALOG_REF`의 tag 문자열을 central commit SHA와 직접 비교해 실패했다. 앞선 단계에서 signed tag의 peeled SHA를 확인했으므로 generator 호출 범위에서만 `REF`에 그 SHA를 전달해 호환 검사를 만족시킨다. 같은 run의 gitleaks는 cache SHA 변수 이름 `CacheKey`와 이를 고정하는 workflow test 문자열에서 generic API key 패턴 두 건을 보고했다. 실제 값은 인증정보가 아니라 commit SHA 식별자이므로 변수명을 `CacheSha`로 바꿨다. clean source archive에서 재실행한 gitleaks는 누출을 찾지 못했고 Central pin guard, 11개 workflow test, verifier, actionlint, Python compile, Gradle help가 통과했다. 이 수정은 commit `85d0312cf9b7e9c97dc592645582a341a4164ab3`에 들어갔지만 아직 PR 원격에 push하지 않았으며 hosted exact-head 검증이 남아 있다.

PR #1838의 다음 exact-head run `39373076530`은 `Central Catalog Governance`의 adoption 검사에서 실패했다. Projects workflow에는 tag 문자열을 `BLUETAPE4K_DEPENDENCIES_CATALOG_REF`로 기록했지만 Central `sync-shared-versions.py`는 이 값을 정규식으로 full commit SHA만 읽는다. 따라서 catalog `settings.gradle.kts`의 SHA pin은 `local=dcc7fa7557d3c0548b9e3fc28c48877827ea2f74`, CI workflow 값은 `central=<missing>`으로 판정됐다. 이 검사는 실패했으며 Build와 14개 Test matrix job, 총 15개 제품 job이 skip됐다. 제품 테스트 실패로 분류하지 않는다.

회귀 검사 두 개는 수정 전에 기대한 이유로 실패했다. 첫 검사는 workflow의 `REF`가 tag라 SHA 계약을 만족하지 않음을 잡았고, 둘째 검사는 verifier가 tag를 canonical `REF`로 받아들이는 것을 잡았다. `REF`는 full SHA로, immutable tag 이름은 별도 `TAG`로 두고 workflow는 `TAG`를 체크아웃과 peeled-commit 확인에 쓰도록 수정했다. `REF`와 `SHA`는 같은 commit으로 유지한다. 수정 후보는 로컬 workflow contract 12개, verifier, actionlint, Python compile, diff check, Central adoption 비교(`[]`), Gradle help를 통과했다. 원격에는 아직 반영되지 않았으므로 새 exact-head CI가 최종 증거다.

## 현재 상태

PR #1838은 열려 있다. 원격 head `93285abb7eef770aa155cb330d2c5ad5bb6f818e`의 CI run `39373076530`은 adoption 검사 실패로 끝났고, 제품 Build/Test job 15개는 실행되지 않았다. 별도 `TAG`와 full-SHA `REF`를 쓰는 수정 후보는 현재 로컬에만 있으며 12개 계약 테스트와 targeted checks가 통과했다. 이 후보의 push 및 새 exact-head CI, 독립 최종 코드 리뷰, merge-ready 확인이 남아 있다. merge 후 exact-SHA Dependency Submission과 실제 hosted Dependabot 재실행으로 alert #235를 확인하기 전까지 Issue #1832를 열어 두고 보안 문제가 해결됐다고 보고하지 않는다.

## Writer DoD

| 검사 | 결과 |
|---|---|
| SPW-01 독자·목적·근거·미확인 사항 | PASS |
| SPW-02 lesson의 맥락·결정·결과·검증·miss·future guard | PASS |
| SPW-03 한국어 기술 문체 및 용어 일관성 | PASS |
| SPW-04 이슈·CI 로그·회귀 테스트·Gradle·advisory 근거 대조 | PASS |
| SPW-05 최종 read-back 및 workflow DoD 기록 | PASS |
| KO-01..KO-06 사실·문체·용어·표·링크 점검 | PASS |
| KO-07 contextual terminology audit | PASS, lesson·계획·testlog findings `[]` |
