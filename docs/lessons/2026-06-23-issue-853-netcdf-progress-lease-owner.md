# 이슈 #853 NetCDF progress lease owner

issue #853은 `NetCdfImportProgressRepository`가 shared progress row id를 유일한 write
authority로 사용한다는 점을 찾았다. `IN_PROGRESS` lease가 만료되고 두 번째 importer가
같은 `(fileId, variableName)` row를 다시 획득하면, stale importer도 같은 `progressId`를
가지고 새 owner의 row를 renew, complete, fail할 수 있었다.

## 결정

`acquireLease`가 반환한 현재 `lease_expires_at` 값을 lease owner token으로 사용한다.
`renewLease`, `markCompleted`, `markFailed`는 `WHERE` clause에서 expected token을
요구한다. token이 맞지 않으면 `NetCdfException.ImportLeaseLost`를 만들고 current owner
row는 변경하지 않는다.

기존 lease expiry timestamp가 acquisition과 renewal마다 이미 업데이트되므로 schema
migration은 필요 없다.

## 교훈

- reusable progress row id는 lease ownership proof가 아니다. expired row를 다시 획득할
  때는 primary key가 같아도 새 write token을 만들어야 한다.
- heartbeat renewal은 다음 token을 반환해야 한다. completion/failure path는 original
  acquisition이 아니라 latest lease에 대해 ownership을 증명해야 한다.
- stale-owner test는 heartbeat renewal뿐 아니라 모든 terminal writer를 cover해야 한다.
  그렇지 않으면 stale importer가 completed 또는 failed로 row를 corrupt할 수 있다.

## 검증

- RED: `./gradlew :bluetape4k-science:test --tests "io.bluetape4k.science.exposed.service.NetCdfCatalogServiceTest.23a - stale lease owner cannot renew after expired lease is reacquired" --no-build-cache`가 `Expected <99> to be <null>`로 실패했다.
- GREEN targeted: `./gradlew :bluetape4k-science:test --tests "io.bluetape4k.science.exposed.service.NetCdfCatalogServiceTest.23a - stale lease owner cannot renew after expired lease is reacquired" --tests "io.bluetape4k.science.exposed.service.NetCdfCatalogServiceTest.23b - stale lease owner cannot complete after expired lease is reacquired" --tests "io.bluetape4k.science.exposed.service.NetCdfCatalogServiceTest.23c - stale lease owner cannot fail after expired lease is reacquired" --no-build-cache`
- regression: `./gradlew :bluetape4k-science:test --tests "io.bluetape4k.science.exposed.service.NetCdfCatalogServiceTest" --no-build-cache`
- module: `./gradlew :bluetape4k-science:test --no-build-cache`가 214 tests로 통과했다.
