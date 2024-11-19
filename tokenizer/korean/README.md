# Module bluetape4k-tokenizer-korean

한글 형태소 분석 기능과 그를 활용환 Normailization, 금칙어 처리, 특수 기능 (Hashtag, Eamil 추출 등)을 제공합니다.

기본 기능은 Twitter에서 만든 [open-korean-text](https://github.com/open-korean-text/open-korean-text) 라이브러리를 참조해서 구현했습니다.

`open-korean-text` 는 Twitter에서 한국어 구어체를 분석하기에 적합하고, 다양한 추출 기능 (HashTag, Email, URL, Mention 등)을 제공하므로,
Chat 서비스에 사용하기에 적합하다고 판단됩니다.

단, `open-korean-text` 는 Scala 언어로 구현되어서, 성능 상의 문제가 있을 수 있고, 기능 개선을 직접 할 수 없다는 단점 때문에 모든 기능을 Kotlin 으로 재작성했습니다.

`bluetape4k-tokenizer-korean` 은 Kotlin 언어로 구현되었고, Coroutines 를 활용하였으므로, Async/Non-Blocking을 지원합니다. 
