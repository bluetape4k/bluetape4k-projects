# Module Bluetape4k Exposed Jasypt

Exposed Crypt 모듈은 **비결정적 암호화 방식** 을 사용하여, 암호화 방식이 암호화 할 때마다 매번 값이 변경됩니다. 이 때문에 암호화된 컬럼으로 조회할 수 없고, 인덱스도 사용할 수 없습니다.

[Jasypt](https://www.jasypt.org)는 Java에서 암호화된 값을 복호화할 수 있는 라이브러리인데,
**결정적 암호화 방식**을 지원하므로, 특정 값을 암호화/복호화 시 여러번 수행해도 같은 값으로 암호화가 됩니다.

물론 이 방식은 보안상 좋지 않지만, 인덱스를 사용할 수 있고, 조건절에 사용하여 검색을 수행할 수도 있습니다.

## 문서

* [Exposed Jasypt](https://debop.notion.site/Exposed-Jasypt-1c32744526b080f08ab2f3e21149e9d7)
* [Exposed Crypt](https://debop.notion.site/Exposed-Crypt-1c32744526b0802da419d5ce74d2c5f3)
