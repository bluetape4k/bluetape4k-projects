# 이슈 661: Benchmark report template

## 배경

benchmark evidence가 `docs/benchmarks`, module-local `Benchmark.md` file, issue
comment, README chart에 나뉘어 있었다.

## 결정

`docs/benchmarks/README.md`를 durable benchmark report index와 template으로 사용한다.
report에는 scope, command, environment, raw artifact, summary table, chart artifact,
interpretation, follow-up link를 명시해야 한다.

## 결과

- benchmark report index와 standard report shape를 추가했다.
- 기존 benchmark report에 raw/chart artifact availability를 표시했다.
- module-local benchmark document는 가볍게 유지한다. durable issue evidence는
  `docs/benchmarks`에 둔다.

## 검증

- markdown-only change를 `git diff --check`로 검토했다.
- `docs/benchmarks/README.md`의 기존 artifact link가 file presence를 갖는지 확인했다.

## 향후 지침

새 performance PR의 결과가 public guidance, default, README chart를 바꾼다면 issue
comment를 `docs/benchmarks` report에 연결해야 한다.
