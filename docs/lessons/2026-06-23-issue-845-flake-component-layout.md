# 이슈 #845 Flake component byte layout

issue #845는 `Flake.asComponentString()`이 generated Flake ID를 `Flake.nextId()`가 쓰는
byte order와 다르게 parse한다는 점을 찾았다.

## 결정

written layout 그대로 component string을 parse한다. 순서는 timestamp `Long`, node byte
6개, sequence `Short`다. 또한 Flake ID length validation을 `asBase62String()`과
일관되게 맞춘다.

## 교훈

- component helper는 log만이 아니라 정확한 binary layout에 대해 test해야 한다. fixed
  clock과 fixed node id를 쓰면 expected string이 안정된다.
- fixed-clock `Flake`의 첫 generated ID는 현재 sequence `1`을 가진다. generator가 같은
  clock value에서 `lastTime`을 초기화한 뒤 같은 millisecond 안에서 증가시키기 때문이다.
- byte layout test는 shifted read가 그럴듯한 output을 우연히 만들지 못하도록 distinct
  timestamp와 node value를 사용해야 한다.

## 검증

- RED: `./gradlew :bluetape4k-idgenerators:test --tests "io.bluetape4k.idgenerators.flake.FlakeTest.component string uses timestamp node and sequence byte layout" --no-build-cache`가 shifted component `7528612310232073478-25939941-1`로 실패했다.
- GREEN targeted: 같은 Flake component layout test가 통과했다.
- module: `./gradlew :bluetape4k-idgenerators:test --no-build-cache`가 1149 tests로 통과했다.
- build: `./gradlew :bluetape4k-idgenerators:build --no-build-cache`가 통과했다.
- hygiene: `git diff --check`가 통과했다.
- static analysis: `./gradlew detekt`가 `:detekt NO-SOURCE`와 함께 통과했다.
