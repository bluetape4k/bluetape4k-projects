# GitHub PR에서 이슈를 닫는 조건을 확인한다

## 배경

2026-10 Exposed #885 작업에서 사용자가 `Refs #xxx`는 이슈를 자동 종료하지 않으며, 종료할 PR에는 `Closes #xxx`를 써야 한다고 바로잡았다. #885를 해결한 Exposed [PR #900](https://github.com/bluetape4k/bluetape4k-exposed/pull/900)은 본문에 `Closes #885`를 넣어 병합했고, live 이슈 상태가 `CLOSED`로 바뀌었다.

Projects [PR #1819](https://github.com/bluetape4k/bluetape4k-projects/pull/1819)은 publication POM 검증을 보완하는 후속 PR이다. 이 작업 자체로 #885의 완료 조건을 다시 증명하지 않으므로 종료 키워드를 넣지 않는다.

## 결정

- 현재 PR이 연결된 이슈의 완료 조건을 모두 충족할 때만 `Closes #<번호>`를 사용한다.
- 범위가 일부이거나 후속 검증을 추가하는 PR에는 `Part of #<번호>` 또는 일반 이슈 링크를 사용한다.
- 병합 후 이슈의 live 상태와 완료 증거를 각각 확인한다. 자동 종료로 바뀐 상태만으로 제품·운영 완료를 입증하지 않는다.

## 검증과 재발 방지

- Exposed PR #900 본문에서 `Closes #885`, 병합 SHA `befa5af2f6b8f3c8cba6644b5a0a363e6da24063`, issue #885의 `CLOSED` 상태를 확인했다.
- Projects PR #1819는 `develop` 대상의 보조 POM 검증 PR이며, 본문에 종료 키워드가 없다.
- 다음 PR의 본문을 작성하기 전에 해당 PR diff와 연결 이슈의 모든 완료 조건을 대조한다. 부분 완료라면 종료 키워드를 넣지 않고, 병합 후 issue 상태와 별도 완료 증거를 다시 읽는다.
