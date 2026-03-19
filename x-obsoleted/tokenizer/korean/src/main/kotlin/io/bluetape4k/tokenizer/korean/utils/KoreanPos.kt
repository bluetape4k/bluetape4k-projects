package io.bluetape4k.tokenizer.korean.utils

import java.io.Serializable

/**
 * 토크나이저 전 과정에서 사용하는 한국어 품사 열거형입니다.
 *
 * ## 동작/계약
 * - 어절 파싱(`KoreanTokenizer`), 청킹(`KoreanChunker`), phrase 추출에서 공통 품사로 사용된다.
 * - `Noun`/`Verb` 등 어절 내부 품사와 `Korean`/`URL` 등 청크 품사를 함께 포함한다.
 * - `Unknown`은 사전 미등록/규칙 미매칭 토큰의 fallback 품사다.
 *
 * ```kotlin
 * val pos = KoreanPos.Noun
 * // pos.name == "Noun"
 * ```
 */
enum class KoreanPos: Serializable {
    /** 일반 명사/의존 명사/대명사 등 체언 계열을 나타낸다. */
    Noun,
    /** 동작이나 상태를 서술하는 동사를 나타낸다. */
    Verb,
    /** 성질·상태를 나타내는 형용사를 나타낸다. */
    Adjective,
    /** 용언이나 문장 전체를 수식하는 부사를 나타낸다. */
    Adverb,
    /** 체언을 수식하는 관형사를 나타낸다. */
    Determiner,
    /** 감탄·호출을 표현하는 감탄사를 나타낸다. */
    Exclamation,
    /** 체언 뒤에 붙어 문법 관계를 표시하는 조사를 나타낸다. */
    Josa,
    /** 용언 어간 뒤에 붙는 종결/연결 어미를 나타낸다. */
    Eomi,
    /** 본 어미 앞에 오는 선어말어미를 나타낸다. */
    PreEomi,
    /** 단어/절/문장을 연결하는 접속사를 나타낸다. */
    Conjunction,
    /** 접두사성/관형사성 수식 성분을 나타낸다. */
    Modifier,
    /** 용언 앞에 붙어 의미를 보강하는 동사 접두어를 나타낸다. */
    VerbPrefix,
    /** 어근 뒤에 붙어 파생어를 만드는 접미사를 나타낸다. */
    Suffix,
    /** 사전/규칙으로 판정하지 못한 미확정 품사를 나타낸다. */
    Unknown,

    /** 한글 음절 중심 청크를 나타낸다. */
    Korean,
    /** 한글이 아닌 외국어 문자 중심 청크를 나타낸다. */
    Foreign,
    /** 숫자(정수/실수/기호 포함 수치) 청크를 나타낸다. */
    Number,
    /** 한글 자모/음절 조각 등 파티클성 청크를 나타낸다. */
    KoreanParticle,
    /** 영문 알파벳 중심 청크를 나타낸다. */
    Alpha,
    /** 문장부호/기호 청크를 나타낸다. */
    Punctuation,
    /** `#태그` 형태의 해시태그 청크를 나타낸다. */
    Hashtag,
    /** `@user` 형태의 스크린네임 청크를 나타낸다. */
    ScreenName,
    /** 이메일 주소 형식 청크를 나타낸다. */
    Email,
    /** URL 형식 청크를 나타낸다. */
    URL,
    /** `$tag` 형태의 캐시태그 청크를 나타낸다. */
    CashTag,

    /** 공백 문자 청크를 나타낸다. */
    Space,
    /** 위 분류에 속하지 않는 기타 청크를 나타낸다. */
    Others,

    /** 고유명사(인명/지명/기관명 등)로 판정된 명사를 나타낸다. */
    ProperNoun
}
