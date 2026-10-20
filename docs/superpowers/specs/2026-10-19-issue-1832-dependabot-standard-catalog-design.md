# Issue #1832 Gradle Dependabot 카탈로그 설계 명세

## 목표

Projects 저장소에서 Gradle 기본 파일 **gradle/libs.versions.toml**을 읽는 Dependabot이 Issue #1832의 실패 좌표를 의존성 그래프와 연결하게 한다. 버전 기준은 계속 **bluetape4k-dependencies** 저장소가 소유한다. 생성된 파일이 중앙 기준과 다르면 CI에서 차단한다.

## 근거

- Projects의 **settings.gradle.kts**는 고정된 중앙 커밋의 카탈로그를 **bt4k** 이름으로 등록한다. Gradle 스크립트는 이 카탈로그의 버전 키와 라이브러리를 사용한다.
- [Dependabot Gradle 파일 탐색기](https://github.com/dependabot/dependabot-core/blob/main/gradle/lib/dependabot/gradle/file_fetcher.rb#L969-L973)는 **gradle/libs.versions.toml**을 표준 카탈로그 경로로 읽는다. 중앙 저장소의 파일을 별도 카탈로그 이름으로 등록한 설정은 이 파일 탐색 경로가 아니다.
- [Dependabot Gradle 파서](https://github.com/dependabot/dependabot-core/blob/main/gradle/lib/dependabot/gradle/file_parser.rb#L2321-L2342)는 표준 파일의 각 라이브러리 별칭을 살피고 버전이 있는 항목만 의존성으로 만든다. 프로젝트 원본의 버전 없는 별칭은 여기에 나타나지 않는다.
- 파서가 catalog 선언을 수집하는 것과 Gradle에서 실제로 해석되는 dependency graph는 별도 근거다. 표준 파일의 명시 버전은 Dependabot manifest 인식을 증명하지만, 실행 빌드의 dependency가 변경됐다는 증거는 아니다. 구현은 `dependabot-core` exact commit을 고정하고 parser fixture에서 이를 사용한다.
- 중앙 **sync-shared-versions.py**는 기본적으로 저장소별 **gradle/libs.versions.toml**을 검사한다. Projects의 프로젝트 전용 원본을 별도 경로로 옮기면 검증기도 그 경로를 명시적으로 읽어야 한다.
- 기존 [카탈로그 pin 리뷰](../../reviews/2026-10-19-issue-1832-catalog-pin-review.md)와 중앙 저장소 정책은 consumer pin을 서명된 catalog train tag의 peeled commit에 결속한다.
- Projects의 로컬 별칭 **jackson-core**, **jackson-databind**, **jackson3-core**, **jackson3-databind**, **netty-handler**는 버전이 없다. **build.gradle.kts**의 제약은 각각 **jackson2**, **jackson3**, **netty** 중앙 키를 사용한다.
- 중앙의 **netty4-handler**는 **netty4** 키를 사용하며 Projects의 **netty-handler** 제약이 사용하는 **netty** 키와 버전 계열이 다르다. 생성기는 두 별칭을 같은 좌표라는 이유만으로 합치면 안 된다.
- **build.gradle.kts**는 Dokka 설정에서 **org.jsoup:jsoup**의 요청 버전 **1.16.1**을 **1.23.1**로 강제한다. 현재 중앙 **jsoup** 키는 **1.23.2**이므로 이 강제 버전은 중앙 기준과 분리되어 있다. 실제 Dokka 그래프의 **org.freemarker:freemarker**는 **2.3.32**를 선택하지만, 중앙 카탈로그에는 보안 패치 **2.3.35**가 있어 별도의 선택 규칙이 필요하다.
- 실패 좌표 열 개는 Jackson 2 두 개, RabbitMQ 한 개, Netty 한 개, Bouncy Castle 두 개, FreeMarker 한 개, jsoup 한 개, Jackson 3 두 개다.
- 사용자 지정 카탈로그 지원 요청 [#6831](https://github.com/dependabot/dependabot-core/issues/6831)은 **not planned**로 종료됐다.

## 범위

### 포함

- Projects의 프로젝트 전용 카탈로그를 생성기 입력 전용인 **gradle/libs.project.versions.toml**로 이동한다. Gradle은 이 파일을 별도 catalog로 등록하지 않는다. 생성한 **gradle/libs.versions.toml**을 계속 자동 `libs` catalog이자 체크인된 Dependabot manifest로 사용한다.
- 중앙 저장소에 Projects용 표준 카탈로그 투영 생성기와 명시적 좌표 매핑을 둔다.
- Projects 저장소가 원본 alias/GAV allowlist를 소유하고 원본 카탈로그 옆에 둔다. 중앙 저장소는 생성기와 열 개 좌표의 버전 매핑을 소유한다. allowlist는 library alias/module 쌍만 보유하며 plugin/bundle 집합은 비어 있어야 한다.
- 기본 경로 **gradle/libs.versions.toml**에는 기존 프로젝트 별칭과 Issue #1832의 열 개 실패 좌표만 포함한다. 전체 중앙 카탈로그를 복사하지 않는다.
- 생성된 실패 좌표 별칭은 중앙 소스에서 해석한 구체 버전을 기본 카탈로그의 명시적 `version.ref`로 materialize한다.
- Projects CI가 중앙 커밋을 고정 확인한 뒤 생성 결과와 저장된 기본 카탈로그의 바이트 단위 일치를 검사한다.
- 중앙 동기화 검증기가 Projects의 프로젝트 전용 원본을 검사할 수 있도록 명시적 카탈로그 파일 선택자를 추가한다.
- 중앙의 `catalog_candidate.py` repository map v2는 Projects의 생성/소비 카탈로그 `catalog`와 원본/authority 카탈로그 `authority_catalog`을 각각 잠근다. `catalog`는 모든 관리 저장소에서 기존 `gradle/libs.versions.toml`이며, Projects의 `authority_catalog`만 `gradle/libs.project.versions.toml`이다. 다른 저장소는 두 경로가 같거나 authority 경로가 생략된다. v1 맵은 기존 해석을 유지해 이전 consumer를 깨뜨리지 않는다. 모든 경로는 저장소 안의 정규 비심볼릭 파일이어야 한다.
- 중앙 CI의 full repository map v2와 별도로 `catalog_candidate.py build-project-map`은 Projects와 중앙 저장소만 포함하는 generator 전용 입력 맵을 만든다. 출력은 `schema_version=2`, 중앙 `central` 항목, `repositories.projects` 항목으로 구성하며 Projects의 `catalog`는 `gradle/libs.versions.toml`, `authority_catalog`은 `gradle/libs.project.versions.toml`로 고정한다. 중앙 항목은 기존 `gradle/libs.versions.toml`을 두 역할에 사용한다. 이 명령은 기존 `inspect_repository_for_map` 관측을 재사용해 두 저장소의 root, full HEAD, origin, branch, base SHA와 clean 상태를 확인하고 두 카탈로그 경로를 잠근다. projection generator만 저장소 목록이 두 곳인 이 맵을 허용한다. 전체 consumer는 완전한 저장소 키 집합을 요구하므로 이 입력 맵을 거부한다. CI의 full map v2 schema와 consumer 동작은 바뀌지 않는다.
- Dokka **jsoup** 및 **FreeMarker** 강제 버전을 각각 **bt4kVersion("jsoup")**, **bt4kVersion("freemarker")**에서 읽게 해 중앙 키와 실제 빌드 선택을 일치시킨다. 두 버전 모두 중앙 카탈로그에 최소 보안 수정 버전을 요구한다.
- **AGENTS.md**, **CLAUDE.md**, CI 명령, 프로젝트 카탈로그 설명을 새 원본/생성물 경계에 맞게 갱신한다.

### 제외

- Dependabot이 중앙 저장소의 공개 PR을 자동으로 만들거나 병합하는 교차 저장소 자동화.
- 중앙 카탈로그의 모든 라이브러리와 플러그인을 Projects의 표준 카탈로그로 복사하는 일.
- 생성 결과에 대한 Dependabot 변경을 중앙 기준 변경 없이 승인하는 일.
- Issue #1832와 무관한 의존성 정리, 버전 승격, 경고 억제.

## 결정

중앙 **bluetape4k-dependencies** 저장소가 Projects 전용 투영 규칙과 생성기를 관리한다. 생성기는 `scripts/project-dependabot-catalog.py`, 좌표 매핑은 `config/projects-dependabot-catalog.toml`에 둔다. Projects는 원본 alias/GAV allowlist를 `gradle/libs.project.versions.allowlist.json`에 둔다. 생성기는 Projects 원본 카탈로그와 allowlist, 중앙 카탈로그와 중앙 매핑 파일을 입력으로 받아 **gradle/libs.versions.toml**을 만든다. 외부 **bt4k** 카탈로그 별칭을 기본 **libs** 카탈로그에서 참조할 수 없으므로, 중앙 소스는 해석된 구체 버전 문자열로 투영한다. 매핑마다 원본 종류(중앙 버전 키 또는 중앙 라이브러리 별칭), 예상 GAV, 출력 별칭, 출력 버전 키를 명시한다. 중앙 라이브러리 별칭을 소스로 쓸 때는 alias의 module이 매핑 GAV와 정확히 같고 `version` 또는 해석 가능한 `version.ref`가 하나 있어야 한다. 그 값은 단일 고정 버전 문자열이어야 하며 동적 버전, 범위, rich version constraint, 누락·미해결 참조는 실패한다.

대상 라이브러리에는 중앙에서 해석한 버전 값을 새로 명시한 **[versions]** 키로 materialize하고 해당 라이브러리는 그 키를 참조한다. 출력 버전 키는 매핑 파일이 지정하며 원본 카탈로그의 기존 키와 충돌하면 실패한다. 투영 후 같은 GAV가 둘 이상의 별칭으로 나타나거나 하나의 GAV에 서로 다른 버전이 연결되면 실패한다. 외부 카탈로그 참조, GAV 추측, 별칭 자동 생성은 허용하지 않는다.

투영 생성은 TOML 전체를 다시 직렬화하지 않는다. 대상이 아닌 원본 선언의 내용과 상대 순서를 보존하고, 기존에 없는 매핑 별칭만 매핑 파일에 적힌 순서대로 추가한다. 출력 라이브러리 별칭 집합은 원본 별칭 집합과 원본에 없던 명시적 매핑 별칭 집합의 합집합이어야 한다. 같은 입력으로 생성기를 반복 실행하면 결과 바이트가 같아야 한다.

생성기는 원본 카탈로그, Projects allowlist, 중앙 카탈로그, 매핑 파일을 한 번 읽어 각 바이트의 SHA-256과 중앙 저장소 HEAD를 입력 기준 데이터로 고정한다. 쓰기 직전에 입력 해시와 중앙 HEAD를 다시 확인하고 하나라도 바뀌었으면 출력을 교체하지 않는다. 일반 실행은 provenance receipt와 후보 lock을 만든다. receipt와 lock은 `--evidence-dir` 아래 `generator-receipt.json`, `candidate-lock.json`, `parser-receipt.json`으로 저장한다. 이 디렉터리의 기본값은 Projects worktree 안의 Git-ignore 경로 `.gradle/issue-1832-evidence`이며 CI는 이를 artifact로 보존한다. receipt는 canonical JSON `schema_version=1`과 정확히 `schema_version`, `generator`, `mapping`, `projects_source`, `projects_allowlist`, `central_catalog`, `generated_catalog`, `repository_map`, `projects`, `central`, `command`, `python_version` 필드를 가진다. 파일 관측값은 `{path, sha256}`이며 저장소 관측값은 `root`, `expected_head`를 가진다. 중앙 관측값에는 `peeled_commit`도 포함한다. 각 경로는 canonical absolute path, regular non-symlink 파일이어야 하며 각 입력이 Projects 또는 중앙 저장소의 허용 경로 아래 있는지 검증한다. receipt와 후보 lock은 현재 `catalog_candidate.py`의 `canonical_json_bytes`와 atomic write/read-back 규약을 따른다. 후보 lock은 receipt 경로와 SHA-256, repository map 경로와 원시 바이트 SHA-256, Projects와 중앙의 `expected_head`, 모든 authority/generated catalog 경로와 SHA-256을 함께 고정한다. strict verifier는 receipt의 모든 파일을 다시 해시하고 현재 repository map과 후보 lock을 재로드해 Projects root/head, 중앙 root/head/peeled commit, map SHA, source·allowlist·mapping·generator·catalog/output SHA가 각각 일치할 때만 통과한다. 따라서 다른 실행의 receipt 재사용, 같은 경로의 내용 변경, map 교체, 오래된 프로젝트 HEAD를 거부한다.

`--write`는 구현 과정에서 생성 파일을 부트스트랩하는 비권위 준비 모드다. 동일한 입력 검증·결정적 투영·원자적 교체를 수행하지만 provenance receipt와 candidate lock을 발행하지 않으며, CI나 완료 증거로 인정하지 않는다. 구현 후 두 worktree를 commit하고 clean 상태에서 `--check`를 실행해 그 HEAD에 맞는 receipt와 lock을 만들고, 이 결과만 검증·CI 증거로 사용한다.

Gradle parser 증거는 generator receipt와 별도 `parser` receipt로 기록하고 Projects exact-head CI artifact 및 후보 lock에 그 경로와 SHA-256을 결속한다. parser receipt는 `schema_version`, pinned `dependabot-core` full commit, parser fixture 경로/SHA-256, 입력 generated catalog 경로/SHA-256, 열 개 GAV별 발견 별칭과 해석한 `version.ref`/버전을 기록한다. verifier는 fixture와 generated catalog를 다시 해시하고, 예상 GAV 집합 및 catalog SHA를 생성기 receipt와 대조한다. parser 결과가 없는 후보 lock은 통과하지 않는다.

생성기는 표준 라이브러리 `tomllib`을 사용하므로 Python 3.11 이상을 요구한다. [`tomllib` 문서](https://docs.python.org/3/library/tomllib.html)는 이 모듈이 Python 3.11에 추가됐다고 명시한다. 중앙 저장소 CI는 [`actions/setup-python`](https://github.com/actions/setup-python/blob/main/README.md)이 안내하는 대로 버전을 고정하고 Python 3.13을 사용한다. CI는 `python3 --version`과 receipt의 전체 `python_version` 값을 보존한다. 지원 하한보다 낮은 Python에서는 생성기와 검증기가 입력을 읽거나 출력을 바꾸기 전에 명확히 실패한다.

생성기 코드, 매핑 파일, 중앙 카탈로그는 Projects가 지정한 동일한 중앙 저장소의 exact SHA에서 읽는다. CI와 최종 `--check`는 중앙 checkout이 해당 full commit SHA와 같고 clean이어야 한다. 공유 catalog ref는 중앙 정책의 서명된 `catalog/YYYY-MM-DD-NN` 태그가 가리키는 peeled commit SHA여야 한다. 태그 서명, 태그 이름/peeled SHA, 중앙 checkout, Projects pin을 read-back해 일치시킨다. 생성기 입력은 두 저장소의 canonical Git root로 한정하고, branch/latest fallback이나 저장소 밖 입력 파일은 허용하지 않는다.

원본 카탈로그의 library alias/GAV 집합은 Projects 저장소의 `gradle/libs.project.versions.allowlist.json`에 폐쇄형 allowlist로 둔다. 파일 필드는 `schema_version`, `libraries`, `plugins`, `bundles`만 허용하며, `libraries`는 alias로 정렬된 정확한 `alias`/`module` 쌍을 보유한다. 기준 Projects 카탈로그의 기존 303개 쌍을 고정하고 `plugins`와 `bundles` 배열은 비어 있어야 한다. 중앙 생성기는 파일의 alias/GAV 집합과 원본을 정확히 비교한다. 새 alias/GAV, plugin, bundle을 추가하거나 기존 alias의 GAV를 바꾸려면 Projects 원본과 allowlist를 함께 검토·변경해야 한다. 원본 library alias는 모두 versionless여야 한다. 현재 303개 alias에도 `version` 선언은 없다. 중앙 생성기는 원본 library에 `version` 또는 `version.ref`가 있으면 실패하고, 생성 카탈로그에서 버전이 명시된 alias 집합이 열 개 중앙 매핑 alias와 정확히 같은지 확인한다. 버전 변경은 중앙 기준과 매핑에서만 한다.

투영은 기존 Projects 카탈로그를 바탕으로 다음 좌표만 중앙 버전과 연결한다.

| 좌표 | 기본 카탈로그 별칭 | 중앙 버전 근거 | 기본 카탈로그 버전 키 |
|---|---|---|
| **com.fasterxml.jackson.core:jackson-core** | **jackson-core** | 중앙 버전 키 **jackson2** | **dependabot-jackson2** |
| **com.fasterxml.jackson.core:jackson-databind** | **jackson-databind** | 중앙 버전 키 **jackson2** | **dependabot-jackson2** |
| **com.rabbitmq:amqp-client** | **amqp-client** | 중앙 라이브러리 별칭 **amqp-client** | **dependabot-amqp-client** |
| **io.netty:netty-handler** | **netty-handler** | 중앙 버전 키 **netty** | **dependabot-netty** |
| **org.bouncycastle:bcprov-jdk18on** | **bouncycastle-bcprov** | 중앙 라이브러리 별칭 **bouncycastle-bcprov** | **dependabot-bouncycastle-bcprov** |
| **org.bouncycastle:bcprov-lts8on** | **bouncycastle-bcprov-lts8on** | 중앙 라이브러리 별칭 **bouncycastle-bcprov-lts8on** | **dependabot-bouncycastle-bcprov-lts8on** |
| **org.freemarker:freemarker** | **freemarker** | 중앙 라이브러리 별칭 **freemarker** | **dependabot-freemarker** |
| **org.jsoup:jsoup** | **jsoup** | 중앙 라이브러리 별칭 **jsoup** | **dependabot-jsoup** |
| **tools.jackson.core:jackson-core** | **jackson3-core** | 중앙 버전 키 **jackson3** | **dependabot-jackson3** |
| **tools.jackson.core:jackson-databind** | **jackson3-databind** | 중앙 버전 키 **jackson3** | **dependabot-jackson3** |

매핑은 좌표, 별칭, 중앙 원천 종류와 이름, 기본 카탈로그의 출력 버전 키를 명시한다. 생성기는 별칭 또는 좌표를 추측해서 연결하지 않는다. Jackson 2의 **jackson-core**처럼 중앙 라이브러리 별칭도 존재하는 좌표는 Projects 원본 별칭에 실제 제약의 중앙 버전 키 **jackson2**를 연결한다. Netty도 **netty4-handler**가 아닌 실제 제약의 **netty** 키를 사용한다. 중앙 카탈로그의 각 library alias는 표의 정확한 GAV와 비교하고, `version.ref`를 포함한 실제 버전을 문자열로 해석한 뒤에만 투영한다.

생성기는 다음 조건에서 오류를 반환하고 파일을 쓰지 않는다.

- 필수 중앙 별칭 또는 버전 키가 없거나 선택한 버전이 비어 있다.
- 원본 별칭의 좌표가 매핑 좌표와 다르거나, versionless여야 할 기존 대상 alias에 직접 버전 또는 `version.ref`가 이미 지정되어 있다.
- 같은 별칭 또는 같은 투영 좌표가 두 번 나타난다.
- 중앙 라이브러리 별칭의 GAV가 표의 예상 GAV와 다르거나 version 참조가 해석되지 않는다.
- 출력 버전 키가 원본 키와 충돌하거나, 같은 GAV가 복수 별칭으로 출력되거나, GAV의 해석 버전이 서로 다르다.
- 생성 결과에 원본 카탈로그의 예상치 못한 구조나 다중 행 선언이 있어 안전하게 수정할 수 없다.
- 출력에서 대상 외 원본 선언의 내용 또는 상대 순서가 달라지거나, 출력 별칭 집합에 원본과 명시적 매핑에 없는 별칭이 생긴다.
- 중앙 저장소 HEAD, 설정의 유효 pin, 설정 기본 pin, CI pin, 후보 맵의 central `expected_head` 중 어느 하나라도 같은 full commit SHA가 아니다.
- **--check**에서 생성 결과와 저장된 **gradle/libs.versions.toml**이 다르다.

생성기는 출력 경로가 심볼릭 링크이거나 일반 파일이 아닌 경우 거부한다. `--projects-root`와 `--central-root`만 입력 경로로 받으며, 두 경로 모두 기대 저장소의 정규 Git root여야 하고 허용된 Bluetape workspace 안에 있어야 한다. 원본, 중앙 카탈로그, 매핑, allowlist와 출력의 파일 경로는 코드가 고정한 위치만 사용한다. 모든 구조·좌표·버전 검증을 마친 다음 동일 디렉터리의 임시 파일에 전체 결과를 쓰고 `flush`, `fsync`, 임시 파일 read-back 확인, atomic replace, 디렉터리 `fsync`, 최종 read-back 순서로 교체한다. 최종 read-back 실패 시 기존 출력이 있었다면 저장한 기존 바이트로 원자 복구를 시도하고, 복구가 검증되지 않으면 실패 원인과 복구 실패를 함께 반환한다. 주입된 쓰기/교체/read-back 실패 검사는 기존 파일의 바이트와 SHA-256이 보존되었는지 확인한다. `--check`는 파일을 쓰지 않는다.

생성기와 Gradle 실행의 경로 경계는 분리한다. 생성기는 canonical 저장소 root와 코드에 고정된 입력 경로만 받으며 대체 카탈로그 경로를 받지 않는다. 기존 Gradle의 `bluetape4kDependenciesCatalogPath`/`BLUETAPE4K_DEPENDENCIES_CATALOG_PATH` 재정의는 로컬 개발과 fixture 검증에서 유지한다. Projects CI는 중앙 저장소를 `.gradle/catalog-source/bluetape4k-dependencies`에 checkout하고, ref와 `BLUETAPE4K_DEPENDENCIES_CATALOG_SHA`가 같은 full commit으로 해석되는지 검사한 뒤 그 checkout의 `gradle/libs.versions.toml`만 Gradle에 전달한다. 다른 path 재정의, property/env 간 불일치, `bluetape4kDependenciesCatalogRef`의 Gradle property/env/기본값 불일치는 CI에서 차단한다. 로컬 Gradle 실행의 의도된 property/env override는 허용하지만 generator 입력과 consumer pin으로 승격하지 않는다.

중앙 `sync-shared-versions.py`는 `--catalog-filename libs.project.versions.toml`을 지원하되, 이 선택자는 `--repo bluetape4k-projects` 하나만 지정했을 때 허용한다. 선택한 경로는 공유 버전 검사, compatibility 검사, adoption gap 검사, authority inventory에 동일하게 전달한다. `--repository-map`을 사용하면 Projects의 `catalog`와 `authority_catalog`을 각각 읽는다. `catalog`는 생성된 `gradle/libs.versions.toml`이며 후보 의존성 그래프와 lock의 대상이다. `authority_catalog`은 `gradle/libs.project.versions.toml`이며 원본 allowlist, 공유 버전·compatibility·adoption gap·authority 검사의 입력이다. 후보 lock은 두 파일과 generator provenance receipt의 경로 및 SHA-256을 기록한다. `catalog_candidate.py`는 공통 normalized map API를 제공하고 기존 v1 reader 이름을 사용하는 호출부를 이 API로 옮긴다. v1은 `authority_catalog = catalog`로 정규화한다. v2 Projects 항목은 두 canonical 경로를 모두 요구하고, 다른 저장소는 authority 경로를 생략해 `catalog`와 같게 정규화한다. map 생성기도 이 표현 하나만 출력한다. `sync-shared-versions.py`, `sync-dependabot-ignores.py`, `run-catalog-validation.py`, `sync-managed-catalog.py`, `sync-publishing-signing-support.py`, `migrate-catalog-authority.py`, `verify-publication-poms.py`, `verify-issues-242-243-receipt.py`, `run-issues-242-243-validation.py`와 CI의 map 생성 단계는 모두 같은 normalized root/catalog/authority_catalog/expected_head를 사용한다. 각 직접 및 간접 소비자의 기존 테스트에서 v1 호환과 v2 Projects 경로를 검증한다. 맵 v1은 기존 단일 `catalog` 동작을 유지하고, v2에서 두 경로를 분리한다. 중앙 PR은 v1과 v2를 모두 받아 기존 Projects develop 검증을 계속 통과시킨다. 명령행 경로 선택자와 맵 경로가 불일치하거나, 후보 결과가 맵의 generated catalog를 기준으로 만들지 않으면 실패한다. 기본 경로 fallback이나 심볼릭 링크는 허용하지 않는다. 중앙 push CI는 Projects의 분리 원본 파일이 이미 있으면 기본 카탈로그 검사에서 Projects를 제외하고 `libs.project.versions.toml`을 별도로 검사한다. 파일이 아직 없는 migration 전에는 기존 기본 catalog 경로로 전체 저장소 검사를 유지한다.

## 중앙 승격 경로

이 전환은 중앙 PR과 Projects PR의 두 단계다. **중앙 PR**은 generator, mapping, map v2의 `catalog`/`authority_catalog`, strict provenance/parser receipt verifier, 후보 lock, 공통 normalized map API, 모든 map 소비자(`catalog_candidate.py`, `sync-shared-versions.py`, `sync-dependabot-ignores.py`, `run-catalog-validation.py`, `sync-managed-catalog.py`, `sync-publishing-signing-support.py`, `migrate-catalog-authority.py`, `verify-publication-poms.py`, `verify-issues-242-243-receipt.py`, `run-issues-242-243-validation.py`)와 CI map 생성 단계, map v1 호환을 함께 제공한다. Projects의 아직 병합되지 않은 신규 원본 경로를 강제하지 않는다. 기존 Projects develop은 v1/기존 경로로 계속 검사하고, 중앙 PR CI는 고정된 Projects fixture로 투영·두 경로 lock을 확인한다. Projects 원본 파일이 실제 존재하는 저장소에 대해서는 candidate map이 v2를 요구한다.

중앙 PR은 정확한 HEAD CI와 승인된 inline 7-Tier 리뷰에서 P0/P1이 없는 상태를 확인한 뒤 별도의 fresh merge approval을 받아 병합한다. 단일 개발자 lane이므로 추가 GitHub human-reviewer 검토는 `N/A`로 기록한다. 병합 후 승인된 별도 catalog-tag 단계에서 새 `catalog/YYYY-MM-DD-NN` 태그 이름이 원격에 없는지, 중앙 기본 브랜치와 작업 트리가 병합 SHA로 일치하는지 확인한 뒤 서명 태그를 만들고 서명 및 peeled commit을 로컬과 원격에서 검증한다. 태그는 이동·덮어쓰지 않는다. 태그 생성과 push는 별도 명시 권한이 있어야 한다. Projects PR은 이 태그를 pin하고 source/allowlist, generated standard catalog, v2 map, CI parser fixture, build 및 문서를 함께 갱신한다. Projects 설정 기본값, 유효 Gradle property, CI 환경 변수, candidate map의 central `expected_head`, 중앙 checkout HEAD는 태그 peeled commit과 같은 full SHA여야 한다. CI는 generator receipt와 후보 lock을 read-back한다. Projects PR도 정확한 HEAD CI와 승인된 inline 7-Tier 리뷰에서 P0/P1이 없는 상태를 확인한 뒤 별도의 fresh merge approval을 받아 병합한다. 추가 GitHub human-reviewer 검토는 같은 단일 개발자 근거로 `N/A` 처리한다.

Projects PR을 병합한 뒤 해당 merge SHA의 dependency-submission workflow를 실행하고 결과 graph를 read-back한다. push 실행은 `github.sha`를 checkout하고, 수동 실행도 workflow event의 full `github.sha`만 대상으로 한다. checkout `HEAD`, Actions run `head_sha`, 제출 graph의 commit SHA가 Projects merge SHA와 모두 같아야 한다. concurrency는 서로 다른 SHA의 작업을 취소하지 않으며, run·checkout·graph SHA 중 하나라도 다르면 alert 재시도 단계로 넘어가지 않는다. 실패한 제출은 같은 exact SHA의 Actions run을 재실행한다. 그 다음 현재 열린 alert에 한해 경보 페이지의 `Try again`을 실행한다. 후보 PR에서는 pinned `dependabot-core` parser fixture로 생성된 standard catalog가 해당 coordinate들을 dependency declaration으로 읽는지 병합 전에 검증하고 parser receipt를 lock/artifact에 결속한다. 중앙 PR은 map v1·v2 호환과 Projects fixture를, Projects PR은 실제 source/generated file·map v2·parser fixture를 검증한다. 중앙 PR 병합 후 tag 또는 Projects 갱신이 중단되면 확인한 immutable tag/SHA에서 재개한다. Projects PR 병합 전에 생성/CI가 실패하면 Projects last-known-good commit과 원본·allowlist·생성 catalog·pin SHA를 기록하고 전체 PR을 되돌린다. Projects 병합 후 제출이나 hosted updater가 실패하면 exact SHA에서 재시도하고 Issue를 열린 상태로 둔다. 정확한 SHA 근거가 없거나 중앙 기준·매핑에 대한 판정이 끝나지 않으면 `hold`하며, 회귀가 확인되면 별도 승인을 받은 Projects revert PR로 이전 pin과 원본/생성 카탈로그를 복구한 뒤 이전 SHA의 graph를 확인한다. 중앙의 immutable tag는 이동·삭제하지 않는다. 중앙 기준이 아직 바뀌지 않았거나 매핑이 불명확한 변경은 수동으로 값을 덮어써서 해소하지 않는다.

## 대안

1. **Gradle 기본 경로에 좁은 중앙 투영을 생성한다 — 선택.** Dependabot이 읽는 표준 경로를 제공하면서 현재 프로젝트 별칭과 문제 좌표만 노출한다. 중앙 매핑, 중앙 고정 커밋, 바이트 일치 검사로 버전 소유권을 확인한다.
2. **중앙 카탈로그 전체를 Projects에 복사한다.** Dependabot은 많은 미사용 별칭까지 의존성 후보로 읽을 수 있고, 같은 좌표가 복수 별칭으로 나타날 수 있어 업데이트 제안 범위가 커진다.
3. **Dependabot 설정에서 사용자 지정 bt4k 카탈로그 경로를 지정한다.** 공식 구현은 **gradle/libs.versions.toml** 표준 경로를 사용하므로 저장소 설정만으로 바꿀 수 없다.
4. **현재 사용자 지정 카탈로그를 유지하고 수동으로 보안 업데이트한다.** 기존 **dependency_not_found** 실패를 자동으로 해소하지 못하고 Issue #1832의 검증 목표를 충족하지 않는다.

## 호환성과 영향

- 이 변경은 catalog와 빌드 메타데이터 변경이므로 JVM 공개 API 및 게시 dependency metadata가 바뀌지 않는다고 선험적으로 주장하지 않는다. 이 이슈의 공개 계약 허용 범위는 baseline과 동일한 source/binary API 및 POM/Gradle Module Metadata 의미값이다. 공개 API, dependency edge/version/scope, variant, capability, constraint의 차이는 허용 목록으로 자동 승인하지 않고 실패 처리한다. 원인이 catalog 투영으로 확인되면 이 이슈에서는 병합하지 말고 별도 public-contract 판단과 명시 승인을 받아야 한다.
- 기본 Gradle 카탈로그 이름 **libs**, 기존 프로젝트 별칭, 중앙 **bt4k** 카탈로그 이름은 유지한다.
- 기존 `libs.*` alias accessor는 전부 유지한다. accessor baseline 비교는 기존 집합의 제거 여부와 정확히 다음 다섯 예상 추가를 별도로 확인한다: `libs.amqp.client`, `libs.bouncycastle.bcprov`, `libs.bouncycastle.bcprov.lts8on`, `libs.freemarker`, `libs.jsoup`. 별칭 정규화 충돌, 기존 accessor 제거, 예상 밖 추가는 실패한다. versionless alias에 버전을 투영하면 기존 중앙 제약과 같은 선택값이어야 한다. 표준 catalog의 열 개 선언은 Dependabot metadata projection이며, 실제 dependency graph의 버전 증거는 Gradle resolution 및 dependency submission으로 별도 확인한다.
- 모든 **dokka*** 구성에서 요청 버전과 관계없이 **bt4kVersion("jsoup")**을 선택한다. 허용 문법은 `^[0-9]+(\\.[0-9]+)*$`인 숫자 dotted-release뿐이다. 구성 시 중앙 값과 요청 값(있으면)을 각각 파싱하고, component를 정수로 비교하며 짧은 쪽은 0으로 채워 비교한다. qualifier, dynamic/range, rich version, 공백, 빈 값은 실패한다. 중앙 jsoup 값은 보안 기준 **1.23.1** 이상이어야 하고 실제 선택은 중앙 값과 같아야 한다. 모든 **dokka*** 구성에서 **org.freemarker:freemarker**도 **bt4kVersion("freemarker")**를 선택하며 중앙 값은 **2.3.35** 이상이어야 한다.
- 원본 카탈로그 경로를 직접 읽는 문서, 스크립트, CI 검사는 새 파일명으로 갱신한다. Dependabot과 Gradle은 표준 파일을 계속 사용한다.

## 검증 및 수용 기준

1. 중앙 생성기 단위 검사가 열 개 좌표의 별칭과 중앙 원천을 모두 확인한다.
2. 대상 외 원본 선언의 내용과 상대 순서가 보존되고, 출력 별칭 집합이 원본과 누락된 명시적 매핑의 합집합이며, 출력에서 version이 명시된 library alias 집합은 중앙 매핑 열 개 alias와 정확히 일치하고, 반복 생성 결과 바이트가 동일하다.
3. 기준 Projects 카탈로그와 allowlist의 alias/GAV 집합이 정확히 일치하고 plugin/bundle은 비어 있다. 미허용 alias, GAV 변경, plugin/bundle 또는 원본 library version 선언을 넣으면 실패한다.
4. 잘못된 별칭, 중앙 GAV 변조, version.ref 누락·미해결, 중복 좌표, 생성 키 충돌, 누락된 중앙 키, stale 생성 파일에서 생성기와 CI 검사가 실패한다.
5. `sync-shared-versions.py`, compatibility/adoption-gap 검사, authority inventory는 `authority_catalog` 원본을 읽고 `catalog_candidate.py` 및 Dependabot ignore 검증은 generated `catalog`를 읽는다. 나열한 모든 직접/간접 map 소비자와 CI map 생성기가 공통 normalized API를 사용한다. v1은 `authority_catalog = catalog`로 기존 default branch에서 계속 통과하고, v2 Projects entry는 `gradle/libs.versions.toml`과 `gradle/libs.project.versions.toml`을 고정한다. 각 소비자의 v1/v2 검사, 양쪽 경로·receipt·map·catalog SHA의 lock 교차 검증을 추가한다. 심볼릭 링크, 비정규 경로, 다른 저장소를 가리키는 경로, map/receipt/lock 불일치가 실패한다.
6. 중앙 설정 기본 pin, Gradle의 유효 pin, CI pin, 후보 맵의 central `expected_head`, 중앙 checkout HEAD가 signed catalog tag의 peeled commit과 같은 full SHA가 아니면 실패한다. 태그 이름은 새 `catalog/YYYY-MM-DD-NN`이어야 하고, 로컬 서명 검사와 GitHub의 tag verification이 모두 유효해야 한다. Gradle property/env/default 및 CI catalog path/checksum 불일치 조합과 branch/latest fallback 입력을 음성 테스트한다.
7. 생성기의 검증 실패와 쓰기/교체/read-back 실패 주입 테스트에서 기존 출력 바이트와 SHA-256이 보존된다. exact-field canonical provenance receipt와 strict verifier는 generator, mapping, Projects source/allowlist/head, central source/head/peeled SHA, repository-map bytes, generated output 각각의 경로·SHA-256을 확인하고 lock과 교차 검증한다. 재사용·변조·오래된 map/head·다른 저장소 경로 receipt 음성 검사가 실패한다.
8. 중앙 PR CI가 고정된 Projects catalog fixture로 projection과 candidate lock을 실행하고, Projects의 현재 v1 map 검증도 유지한다. Projects CI의 `catalog-governance`는 중앙 커밋 고정, 생성 파일 일치, 원본 중심 중앙 동기화 검사에 성공해야 한다. 해당 job은 중앙 mapping의 full `dependabot-core` commit을 checkout하고 pinned Gradle parser fixture를 직접 실행해 `.gradle/issue-1832-evidence/parser-receipt.json`을 생성한 뒤 generator `--check`에 전달한다. 별도 중앙 workflow의 parser 실행 결과는 Projects job의 로컬 파일을 대신하지 않는다. Dependency Submission workflow는 push event의 exact `github.sha`를 checkout하며 checkout `HEAD`, Actions run `head_sha`, 제출 graph commit SHA가 같음을 검증한다. 재실행도 같은 SHA를 유지하고 동시 실행이 다른 SHA 작업을 취소하지 않는다. aggregate `ci-status`는 `catalog-governance`가 `success`가 아닌 경우 실패하고 `skipped`를 성공으로 취급하지 않는다.
9. pinned `dependabot-core` exact commit의 Gradle parser fixture가 generated **gradle/libs.versions.toml**에서 열 개 GAV 선언을 전부 찾고 해당 `version.ref`를 해석한다. parser receipt는 exact commit, fixture SHA, generated input catalog SHA 및 좌표별 결과를 기록하고 후보 lock 또는 exact-head CI artifact에 결속한다. 각 선언이 build script에서 실제 사용되는지와 무관하게 manifest/parser 인식만 확인하며, 이것을 resolved graph 증거로 대체하지 않는다. 기존 accessor 전체 집합의 보존과 다섯 expected addition을 별도로 검증하고 alias normalization 충돌, 기존 accessor 제거, 예상 밖 추가, plugin/bundle 추가가 없어야 한다.
10. baseline/candidate의 모든 영향 모듈에서 target alias 소비처와 `dependencyInsight` 결과를 비교한다. Jackson 2/3, Netty 및 나머지 현재 alert GAV마다 좌표별 구성, 요청 버전, 선택 버전과 적용 경로를 기록한다. 중앙 제약이 같은 선택값을 내는지 확인하고, 이후 JDK/Gradle dependency-submission 결과도 대조한다.
11. baseline/candidate 영향 모듈의 resolved graph, API/runtime classpath, 게시 POM과 Gradle Module Metadata를 의미 단위로 비교한다. POM/GMM dependency edge, version, scope, variant, capability, constraint는 baseline과 같아야 하며 catalog 변경으로 생긴 차이는 허용하지 않는다. 공개 Kotlin/Java source fixture는 candidate에 대해 컴파일한다. 별도 ABI fixture는 먼저 baseline artifact로 consumer를 컴파일해 bytecode를 고정하고, 재컴파일 없이 candidate artifact와 runtime classpath로 실행한다. 영향받는 published module의 API dump 또는 `javap -public -s` owner/descriptor도 baseline과 candidate 사이에 비교한다. 어떤 ABI 또는 게시 metadata 차이라도 이 이슈의 검증을 실패시키고 별도 public-contract 승인 전에는 병합하지 않는다.
12. Gradle Wrapper **9.8.1**과 저장소 `gradle-wrapper.properties`의 distribution SHA-256, Kotlin Gradle plugin **2.4.20**, Kotlin language/API **2.4**, Temurin JDK **21** 및 **25**에서 영향 모듈의 설정·compile·runtime resolution matrix를 검증한다. Java 21 island과 그 `virtualthread/api`, logging, assertions, junit5 최소 의존성 폐쇄, Java 25 기본 target을 각각 다룬다. CI 결과에 실제 `java -version`, vendor/build, wrapper checksum을 기록하고 `./gradlew help`는 기본 smoke로만 사용한다. 모든 **dokka*** 구성의 `ResolutionResult` 또는 동등한 `dependencyInsight` 검사에서 `org.jsoup:jsoup` 최종 선택 버전이 `bt4kVersion("jsoup")`와 정확히 같아야 한다. `org.freemarker:freemarker`도 `bt4kVersion("freemarker")`와 같고 최소 **2.3.35** 이상이어야 한다. 요청 버전 **1.16.1**, 다른 취약 버전, 미지정 버전과 direct/transitive 요청을 다룬다. 숫자 dotted-release 1.23.1/1.23.2는 통과하고 1.23.0, dynamic, range, rich version, qualifier, malformed 및 빈 값은 실패한다.
13. 중앙 PR과 Projects PR 각각의 원격 exact HEAD에서 required CI가 통과하고 inline 7-Tier review/P0/P1 disposition (reviewer N/A for the single-developer lane)을 read-back한다.
14. Projects merge 뒤 exact SHA dependency-submission 결과를 확인하고, 경보 목록 조회를 세 시점에 남긴다: 착수 기준, `Try again` 직전, updater 실행 후 최종 상태. 세 목록을 alert ID와 GAV별로 대조해 추가·종료·재오픈을 기록한다. 새 alert ID는 ledger에 편입하고, 범위에 속하는 새 GAV의 `dependency_not_found`도 확인한다. 설계 갱신 시 live API는 28개 alert에서 9개 고유 GAV를 보고했고, 마지막 hosted run `39194768984`도 RabbitMQ를 제외한 아홉 GAV에서 `dependency_not_found`를 보고했다. 이 값은 작업 착수 때 다시 조회한다. 각 alert ID는 `updated`, `already-patched`, `no-update-available`, `not-detected`, `updater-error`, `unknown` 중 하나로 기록하고 GAV별로 집계한다. 같은 GAV라도 `firstPatchedVersion`이 다른 경보를 별도 추적한다. `already-patched`는 dependency graph가 패치 버전을 선택하고 해당 alert가 `fixed`일 때만 허용한다. `no-update-available`은 updater가 업데이트를 만들지 않았고 alert가 계속 `open`인 상태로 별도 기록하며 Issue를 `PENDING`으로 둔다. 적용 가능한 업데이트가 있으면 실제 PR diff와 resolved patched version을 확인한다. `not-detected`, `updater-error`, `unknown`, dismissed/상태 불명 경보는 해결로 세지 않는다. RabbitMQ는 현재 열린 alert가 없으면 과거 회귀 좌표로 parser fixture에서만 유지한다. 기록에는 Projects merge SHA, 중앙 tag와 peeled pin SHA, 세 시점 경보 목록, alert ID/GAV, run ID, `firstPatchedVersion`(제공 시), updater log, dependency submission graph, PR diff(있을 때), 최종 alert 상태가 들어간다. 최종 live 조회에서 새 alert가 발견되면 ID/GAV를 ledger에 추가해 처리한다. 조회 누락, 미해결 open alert 또는 `dependency_not_found`가 하나라도 남으면 Issue #1832를 해결 처리하지 않는다.
15. Projects 병합 후 실패는 `retry`, `hold`, `revert-projects`로 분류한다. `retry`는 병합 SHA의 CI와 pin/생성물 검사가 계속 통과하고 실패가 일시적일 때 같은 SHA로 재실행한다. 원인 또는 exact SHA 증거가 불명확하면 `hold`하고 Issue를 연다. 코드/빌드 회귀가 재현되면 이전 Projects SHA, pin, source/generated catalog를 기록하고 별도 승인을 받은 revert PR을 만든 뒤 revert SHA의 dependency graph를 확인한다. 이 복구에서 중앙 immutable tag를 삭제하거나 이동하지 않는다.
16. 중앙 생성기와 parser verifier가 Python 3.11 미만에서 부작용 전에 실패하고, Python 3.11 및 3.13에서 단위 검사가 통과한다. 중앙 CI는 Python 3.13을 설치·고정하고 실행 버전을 로그로 남긴다.

## 실패 조건과 복구

- 설정 기본 pin, 유효 Gradle pin, CI pin, 후보 맵의 central `expected_head`, 중앙 checkout 중 하나라도 다르면 생성 또는 Gradle 검증을 중단한다.
- 매핑 대상의 좌표·별칭·버전 원천이 중앙 변경과 맞지 않으면 생성기를 실패시킨다. 매핑을 자동으로 수정하지 않는다.
- 원본/생성 카탈로그 중 한쪽만 바뀌면 CI가 실패한다. 원본과 중앙 매핑을 검토한 뒤 생성기를 다시 실행한다.
- Gradle 해석 결과가 예상한 중앙 버전과 다르면 별칭을 추가로 만들지 않고 제약 구성과 투영 규칙을 조사한다.
- 실제 Dependabot 업데이트가 계속 실패하거나 일부 좌표의 로그 증거가 없으면 실행 로그와 alert 상태를 좌표별로 기록하고, 전체 **dependency_not_found** 오류나 Issue #1832가 해소됐다고 보고하지 않는다.
- parser가 특정 catalog declaration을 읽어도 Gradle에서 쓰이지 않는 alias라면 이를 실제 resolved dependency로 보고하지 않는다. 해당 좌표가 현재 alert에 연결된 이유와 dependency-submission graph를 추적한다.
- 중앙 PR 병합 후 tag 승인이나 Projects PR이 대기 중이면 legacy map v1 경로로 기존 develop 검증을 계속 지원한다. source catalog가 존재하는 Projects candidate에는 map v2와 양쪽 catalog lock을 요구한다.

## 미확인 사항과 후속 증거

- parser fixture에 사용할 `dependabot-core` full commit은 구현 시점에 선택하고 pin해야 한다. 현재 `main` source link는 경로와 기대 동작을 설명하는 참고 자료이며 재현 가능한 실행 증거가 아니다.
- alert ID, GAV, `firstPatchedVersion`과 updater 결과는 시점에 따라 바뀔 수 있다. 구현 시작 전, `Try again` 직전, 실행 후에 live 목록과 결과를 다시 수집한다.
- generated catalog의 parser 인식만으로 실제 Gradle dependency graph, dependency-submission graph, source/API/ABI, POM/GMM, toolchain 또는 exact-head CI를 증명하지 않는다. 이 자료는 계획 Task 7–9의 검증 전까지 미확인이다.
- 중앙 PR merge와 signed tag 발행, Projects PR merge, hosted retry는 각각 별도 승인 경계다. 설계 승인이나 문서 리뷰 결과를 해당 실행 승인으로 해석하지 않는다.

## 완료 정의

- [ ] 중앙 PR의 생성기, 명시적 좌표 매핑, repository-map v1/v2 정규화 API, 모든 직접/간접 map consumer 지원, 두 카탈로그와 strict provenance/parser receipt verifier 및 candidate lock, 동기화/authority/ignore validation fan-out이 구현되어 있다.
- [ ] Projects 원본/생성 카탈로그 경로, CI, Dokka jsoup 및 FreeMarker 보안 기준, 문서가 함께 갱신되어 있다.
- [ ] Projects-owned alias/GAV allowlist가 원본과 일치하고, 중앙 및 Projects의 타깃 검증·Gradle graph·accessor/API/publication·toolchain matrix가 통과했다.
- [ ] pinned Dependabot parser fixture가 열 개 GAV를 읽고 parser receipt가 exact commit·fixture·catalog·좌표별 결과를 결속한다. 중앙 PR과 Projects PR 각각의 승인된 inline 7-Tier 리뷰에서 P0=0, P1=0이고 정확한 원격 HEAD CI를 읽었다.
- [ ] alert baseline, 재시도 직전, 재시도 후 경보 목록 조회 결과의 alert ID/GAV 집합 차이를 반영하고 현재 대상 경보별 실제 hosted 실행 결과를 확보했다. update 가능 항목은 PR diff/패치 그래프, `already-patched`는 패치 graph와 `fixed`, `no-update-available`은 열린 경보와 `PENDING` 상태를 각각 증명했으며, RabbitMQ는 필요 시 역사적 parser 회귀 fixture로 확인했다.
- [ ] 정확한 Projects merge SHA에서 Dependency Submission workflow run `head_sha`, checkout `HEAD`, 제출 graph commit SHA가 일치한다. 대상 보안 경보가 `fixed`이고 최종 해결 그래프가 패치 버전을 선택할 때만 Issue #1832를 해결 처리했다.
- [ ] 중앙 PR 및 Projects PR은 각각 fresh merge approval 전까지 병합하지 않고, signed catalog tag 생성/push는 별도 명시 승인이 있기 전까지 보류한다.
