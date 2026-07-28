# 이슈 491 Java Properties Empty Numeric Null

## 배경

`JavaPropsMapper`는 Java Properties value를 untyped string으로 읽는다. `maxWaitMillis=`와
`idleTimeout=` 같은 empty property가 nullable numeric Kotlin constructor parameter로 deserialize될 때
`0`이 되어, generated properties가 원래 `null` model과 round-trip되지 않았다.

## 결정

Fix를 Jackson 2와 Jackson 3의 `JacksonText.Props.defaultMapper`에만 국소화한다. Numeric logical type에
Jackson coercion metadata를 적용하고, non-empty value는 정상 parsing하되 empty string은 boxed numeric
type에서 `null`로 mapping하는 Properties-only numeric module을 추가한다.

## 결과

Disabled 상태였던 named datasource properties round-trip test를 Jackson 2와 Jackson 3 모두에서
활성화했다. Empty nullable numeric property는 이제 `null`로 deserialize된다. 다른 text format과 CSV
mapper default는 바뀌지 않는다.

## 검증

`./gradlew :bluetape4k-jackson2:test --tests 'io.bluetape4k.jackson.text.datasources.ParseNamedDataSourcePropertiesTest' :bluetape4k-jackson3:test --tests 'io.bluetape4k.jackson3.text.datasources.ParseNamedDataSourcePropertiesTest' --console=plain --no-configuration-cache`가 module당 4 tests로 통과.

`./gradlew :bluetape4k-jackson2:test :bluetape4k-jackson3:test --console=plain --no-configuration-cache`가 Jackson 2 430 tests, Jackson 3 432 tests로 통과.

## 향후 가드

같은 bug를 증명하는 별도 test가 없다면 Java Properties coercion fix를 CSV/TOML/YAML mapper로 넓히지
않는다. Java Properties scalar handling은 format-specific이므로 future coercion module은 affected
mapper에만 붙인다.
