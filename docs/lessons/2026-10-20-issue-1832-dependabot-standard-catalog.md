# Issue #1832: catalog 선언과 실행 경계별 증거를 함께 검증하기

## 맥락

Projects의 중앙 버전 소유권 catalog는 Gradle 내부 alias의 버전 정보를 중앙 관리하지만 GitHub Dependabot이 읽는 표준 경로와 같지 않았다. Issue #1832는 Gradle 보안 업데이트가 제출된 dependency graph에서 catalog 관리 좌표를 찾지 못해 종료되는 현상을 다룬다. 현재 GitHub issue는 OPEN이다. 2026-10-20 조회에서 FreeMarker alert #235도 CRITICAL 상태로 OPEN이었다.

FreeMarker 보안 공지는 2.3.34 이하를 영향 범위, 2.3.35를 수정 버전으로 지정한다. Projects build 설정은 Dokka 구성에서 중앙 catalog 버전을 선택하고, 최소 2.3.35 아래로 내려가면 빌드를 실패시킨다. [GitHub advisory](https://github.com/advisories/GHSA-27j2-h3m2-8237)와 [Apache 2.3.35 릴리스 노트](https://freemarker.apache.org/docs/versions_2_3_35.html)를 참조한다.

## 결정

Projects의 기존 catalog를 versionless 원본으로 분리하고, 중앙 mapping에서 고른 열 개 GAV만 표준 gradle/libs.versions.toml에 version.ref로 투영한다. generator는 원본 alias와 순서를 유지하고 allowlist, repository map, parser receipt, candidate lock을 서로 해시로 결속한다. CI는 Dependabot Gradle parser가 실제 표준 catalog에서 열 개 좌표를 읽는지 고정 upstream commit으로 확인한다.

각 workflow job은 자체 파일시스템 경계를 가진다. 다른 job의 parser receipt를 사용할 수 없으므로 Projects catalog-governance도 parser fixture를 실행하고 자기 receipt를 만든다. 같은 이유로 Central의 push-only source audit는 Projects 원본 catalog를 직접 읽어야 한다.

## 결과와 검증

- Projects local workflow contract 8개와 actionlint, verifier, Python compile, Ruff, diff check가 통과했다.
- Central 전체 Python suite 514개 통과, 2개 skip. 두 skip은 관리 sibling 저장소가 격리 worktree에서 보이지 않아 real-workspace inventory를 실행하지 않은 결과다.
- 외부 1.9.2-SNAPSHOT artifact 검사 한 건은 HTTP 403으로 건너뛰었다. 제품 테스트 실패로 계산하지 않는다.
- 98개 Dokka configuration에서 FreeMarker 2.3.35와 jsoup 1.23.2를 확인했다. 13개 게시 모듈의 POM과 Gradle Module Metadata는 26개 모두 baseline과 byte-identical했다.
- 대상 모듈 classes와 mock-web-server/mock-webflux-server Jib 빌드가 앞선 검증에서 통과했다.
- 이 결과는 parser PR CI나 hosted Dependabot 성공을 뜻하지 않는다. 현재 FreeMarker alert #235는 계속 OPEN이며 Issue #1832도 해결 처리하지 않는다.

## 놓친 점과 재발 방지

첫 구현에서는 Projects CI가 다른 workflow의 parser receipt를 사용할 수 있다고 가정했다. 새 runner의 파일시스템에는 그 receipt가 없어서 generator check가 실패할 수 있었다. workflow 계약 테스트를 먼저 실패시키고, parser contract를 같은 job 안에서 실행해 receipt를 생성하도록 수정했다.

두 번째 구현 공백은 Projects catalog를 나눈 뒤 Central push workflow가 여전히 기본 generated catalog를 authority로 읽는 경로였다. Projects 원본 파일이 존재할 때 source catalog를 지정하도록 수정하고, 이전 구조의 저장소를 위한 legacy fallback을 보존했다.

새 검증 자료를 추가하면 실행 경계마다 파일이 공유되는지 확인한다. catalog authority를 나누면 정규 map loader뿐 아니라 모든 CI audit와 간접 consumer가 source/generated 역할을 구분하는지 확인한다. 증거 생성 전후로 exact HEAD와 파일 해시를 재검증하며 map, receipt, lock을 closeout까지 보존한다.

## 현재 상태

로컬 구현과 inline 7-Tier review는 완료 단계다. Central PR exact-head CI, 중앙 merge 및 signed tag, Projects tag pin/PR/merge, merge SHA Dependency Submission, hosted Dependabot 재시도는 아직 수행하지 않았다. 이 단계가 끝나기 전에는 보안 alert나 Issue #1832가 해결됐다고 보고하지 않는다.

## Writer DoD

| 검사 | 결과 |
|---|---|
| SPW-01 독자·목적·근거·미확인 사항 | PASS |
| SPW-02 lesson의 맥락·결정·결과·검증·miss·future guard | PASS |
| SPW-03 한국어 기술 문체 및 용어 일관성 | PASS |
| SPW-04 이슈·테스트·Gradle·advisory 근거 대조 | PASS |
| SPW-05 최종 read-back 및 workflow DoD 기록 | PASS |
| KO-01..KO-06 사실·문체·용어·표·링크 점검 | PASS |
| KO-07 contextual terminology audit | PASS, 최종 audit 결과를 계획에 기록 |
