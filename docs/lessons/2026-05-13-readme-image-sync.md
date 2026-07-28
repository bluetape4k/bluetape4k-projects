# README image sync

## 배경

Project README image는 bluetape4k organization profile image와 맞아야 했다.

## 결정

Profile image를 repository로 복사하고 README image path를 안정적으로 유지한다.

2026-05-14에 대체됨: obsolete top-level `doc/` directory는 제거됐다.
현재 README image는 `docs/assets/projects-workbench.png` 아래에 있다.

## 결과

English/Korean README file은 모두 복사된 workbench image를 locale에 맞는 alt text로 설명한다.

## 검증

두 README file이 같은 local image asset을 참조하는지 확인했다.

## 향후 지침

Top-level project image를 갱신할 때는 cross-repository relative path에 GitHub rendering을 의존시키지 말고
`docs/assets/` 아래의 local README asset을 교체한다.
