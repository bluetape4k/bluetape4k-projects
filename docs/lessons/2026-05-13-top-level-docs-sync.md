# Top-level docs sync

## 배경

Root README는 아직 오래된 monorepo-era module description과 현재 split-repository/module layout을 섞고 있었다.

## 결정

Top-level README, agent guidance, WIP update의 source of truth는 `settings.gradle.kts`, module build file,
module README file로 둔다. CHANGELOG entry의 source of truth는 planned work가 아니라 merged PR history다.

## 결과

Root README pair는 compression을 `bluetape4k-io`로 연결하고, 현재 infra/testing/utility/example module을 나열하며,
removed-module list에서 `nats`를 제거하고, agent-facing module guidance는 영어로 유지한다.
CHANGELOG는 WIP-completed idgenerator example과 workflow fix를 포함해 #347 이후 merged PR을 다룬다.

## 검증

Module reference를 현재 module directory와 대조하고, 오래된 README token을 검색했으며,
CHANGELOG PR reference를 #347 이후 merged PR과 비교했다.

## 향후 지침

Module이 추가, 이동, 제거, 분리될 때는 `README.md`, `README.ko.md`, `CLAUDE.md`, `AGENTS.md`,
`WIP.md`, `CHANGELOG.md`를 같은 documentation pass에서 갱신한다.
