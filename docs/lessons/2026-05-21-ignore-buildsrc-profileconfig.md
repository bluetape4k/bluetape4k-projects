# buildSrc profile config ignore

## 배경

README diagram 작업 이후 `buildSrc/.profileconfig.json`이 untracked local profile artifact로 나타났다.

## 결정

모든 nested `.profileconfig.json`으로 pattern을 넓히지 않고, 정확한 `buildSrc` path만 ignore한다.

## 결과

Local profile file은 일반 repository status에 더 이상 나타나지 않고, root `/.profileconfig.json`은 이전처럼 ignore된다.

## 검증

- `git check-ignore -v buildSrc/.profileconfig.json`
- `git status -sb`

## 향후 노트

동일한 generated file이 여러 module directory에서 확인되기 전까지 local tool profile ignore는 path-specific하게 유지한다.
