# 이슈 844 - geo README example

## 배경

`bluetape4k-geo`는 기존 geocode, geohash, geoip2 module을 통합했지만 영어와 한국어
README는 여전히 오래된 `io.bluetape4k.geo.*` package hierarchy와 존재하지 않는 helper
type을 문서화했다.

installation example도 consumer가 자기 build에서 사용할 수 없는 `Libs.feign_core` 같은
project-internal Gradle catalog symbol을 노출했다.

## 결정

README example을 현재 public API에 맞춰 다시 작성한다.

- `io.bluetape4k.geohash` factory와 extension function
- `io.bluetape4k.geocode.google.GoogleAddressFinder`
- `io.bluetape4k.geoip2.Geoip` 및 `DatabaseReader` extension function

installation block은 internal `Libs.*` alias 대신 Maven coordinate와 repository version
catalog의 concrete version을 사용한다.

## 후속 가드

geocode, geohash, geoip2 public entry point가 이동하면 `README.md`와 `README.ko.md`를
source-equivalent하게 유지한다. `GeoReadmeContractTest`는 이 issue를 만든 stale
package/API name을 막는다.
