# Module bluetape4k-jackson-text

[jackson-dataformats-text](https://github.com/FasterXML/jackson-dataformats-text) 에서 제공하는

* CSV
* Properties
* Yaml

형식의 정보를 생성, 파싱하는 라이브러리 활용 예제

## NOTE

Jackson 2.15.2 에서는 Yaml 은 제대로 동작하는데, Properties 는 값이 설정되지 않은 Int? 값에 기본 값으로 null 이 아닌 0을 설정하는 버그가 있다.

Jackson 2.12.5 에서는 정상 동작한다.
