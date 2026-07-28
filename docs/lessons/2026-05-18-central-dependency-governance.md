# 중앙 Dependency Governance 동기화

## 배경

Downstream Dependabot PR들이 shared dependency version을 repository별로 하나씩 업데이트하면서
bluetape4k organization 전반에 version drift가 생기고 있었다.

## 결정

Shared dependency version은 먼저 `bluetape4k-dependencies`에서 변경하고, 그 뒤
`sync-shared-versions.py`로 이 repository에 materialize해야 한다. 이 repository의 Dependabot은
중앙에서 관리되는 dependency name도 ignore하여 future PR이 중앙 source of truth를 거치도록 한다.

## 결과

Local version catalog와 `.github/dependabot.yml`이 중앙 dependency-governance policy를 따른다.

## 검증

- 이 repository에 대해 `sync-shared-versions.py --write --check --summary`
- 이 repository에 대해 `sync-dependabot-ignores.py --write --check --summary`
- `git diff --check`

## 향후 가드

중앙에서 관리되는 dependency에 대한 repo-local Dependabot PR을 merge하지 않는다.
`bluetape4k-dependencies`를 업데이트한 뒤 이 repository를 sync한다.
