package io.bluetape4k.tokenizer.korean.utils


fun nameDictionaryContains(key: String, charseq: CharSequence): Boolean =
    KoreanDictionaryProvider.nameDictionary[key]?.contains(charseq) ?: false

fun nameDictionaryContains(key: String, str: String): Boolean =
    KoreanDictionaryProvider.nameDictionary[key]?.contains(str) ?: false

fun koreanContains(pos: KoreanPos, cs: CharSequence): Boolean =
    KoreanDictionaryProvider.koreanDictionary[pos]?.contains(cs) ?: false
