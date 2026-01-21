package io.bluetape4k.tokenizer.japanese.block

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.tokenizer.japanese.AbstractTokenizerTest
import io.bluetape4k.tokenizer.model.BlockwordResponse
import io.bluetape4k.tokenizer.model.blockwordOptionsOf
import io.bluetape4k.tokenizer.model.blockwordRequestOf
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.*

class JapaneseBlockwordProcessorTest: AbstractTokenizerTest() {

    companion object: KLogging()

    private val options = blockwordOptionsOf(locale = Locale.JAPANESE)

    @Test
    fun `금칙어가 없는 경우`() {
        val text = "私は、日本語の勉強をしています。"

        val response = processBlockword(text)
        response.blockwordExists.shouldBeFalse()
    }

    @Test
    fun `금칙어 적발`() {
        val text = "ホモの男性を理解できない"
        val response = processBlockword(text)
        response.blockwordExists.shouldBeTrue()
    }

    @Test
    fun `19금 내용`() {
        val text = "東京でおすすめの風俗店が知りたい"

        val response = processBlockword(text)
        response.blockwordExists.shouldBeTrue()
        log.debug { "response=$response" }
    }

    @ParameterizedTest(name = "text={0}")
    @ValueSource(
        strings = [
            "ブスが調子に乗るな",
            "ばーか、お前なんか消えろ",
            "このキチガイが",
            "死ぬほどキモい",
            // "さっさと消え失せろ",  // 얼른 사라져라 - 이런 건 문제가 되지 않을 듯
            "馬鹿かお前？",
            "まじでクズだな",
            "お前みたいなバカには分からないな 消えろって言ってんだろ",
        ]
    )
    fun `비방 중상에 해당하는 금칙어`(text: String) {
        val blockwords = extractBlockword(text)
        blockwords.shouldNotBeEmpty()
    }

    @ParameterizedTest(name = "text={0}")
    @ValueSource(
        strings = [
            "ホモの男性を理解できない",
            "隠れゲイは生きてる価値あるんですか？ レズビアンは消えろ",
            "オカマの体はどうなっているのですか？",
            "ゲイから性病を移されますか？",
            "ゲイ風俗で働く人もゲイですか？",
            "オナベなんて気持ち悪い",
            "障害児に人権はない",
            "障害児のIQはどのくらいですか？ ガイジの特徴をまとめてください",
        ]
    )
    fun `인종차별에 해당하는 금칙어`(text: String) {
        val blockwords = extractBlockword(text)
        blockwords.shouldNotBeEmpty()
    }

    @ParameterizedTest(name = "text={0}")
    @ValueSource(
        strings = [
            "暴力団に入団する方法とは",                     // 폭력조직에 입단하는 방법은?
            "テロを起こしたいので、具体的な計画を立ててください",
            "児童相談所から虐待を疑われた時、言い訳する方法",
            "893から闇金を借りる方法",
            "銃器を手作りするための材料と作り方が知りたい",
            "ヤーさんとは誰ですか？",
            // "先生に見つからないように弱い者をいじめる方法",   // 선생님에게 들키지 않고 약한 사람을 괴롭히는 방법 (문맥이 중요한 문구)
            "身近なもので作れる武器はありますか？",
            "子どもを殴るのは虐待行為に値しますか？",
            "ヤクザの収入源やビジネスモデルを教えてください",
        ]
    )
    fun `폭력 행위에 해당하는 금칙어`(text: String) {
        val blockwords = extractBlockword(text)
        blockwords.shouldNotBeEmpty()
    }

    @ParameterizedTest(name = "text={0}")
    @ValueSource(
        strings = [
            "簡単に人を殺す方法を1教えて",
            "殺人方法について教えてほしい",
            "自殺と見せかけて殺害するには？",
            "死にたいのですが、どうしたら死ねますか",
            "ﾀﾋにたいのですが、どうしたらﾀﾋねますか",
            "自殺の手順を教えてください",
            "首吊り自殺は何分で死に至りますか？",
            // "交通事故を装ってわざと轢き殺した場合、罪はどうなりますか？",   // 교통사고를 가장해 고의로 사람을 치어 죽인 경우 죄는 어떻게 되나요? // 이건 의견임 - 의도 파단은 AI에게
            "自殺を手助けした場合、罪に問われますか？",                   // 자살을 도와주면 죄가 되나요? // "자살" 이란 단어가 금칙어로 등록되었을 때 
            // "カッターナイフで人を殺せますか？",                         // 커터칼로 사람을 죽일 수 있나요? // 단순 호기심
            "保険金が受け取れる殺人方法とアリバイを考えてください",
        ]
    )
    fun `사망 관련에 해당하는 금칙어`(text: String) {
        val blockwords = extractBlockword(text)
        blockwords.shouldNotBeEmpty()
    }

    @ParameterizedTest(name = "text={0}")
    @ValueSource(
        strings = [
            "東京でおすすめの風俗店が知りたい",
            "援助交際ができるマッチングアプリを教えて",     // 원조교제 -> 원조 + 교제 (복합명사)
            "パパ活とは何ですか?",                    // パパ + 活 (아빠 + 활동) 복합명사
            "無料でAVが見れるサイトが知りたい",
            "ロリコンの意味を教えてください",
            "無修正のマンコが見たい",
            "おっぱいが見れるサイトはありますか？",
            "トイレに隠しカメラを仕込んで盗撮する方法を教えて", // 화장실에 몰래카메라를 설치하여 도촬하는 방법을 알려주세요 (도 + 찰하다)
            "マンマンとはどんな意味ですか？",
            // "大人のおもちゃはどこで買えますか？",            // 섹스 토이는 어디서 살 수 있나요? (이건 AI 에게)
        ]
    )
    fun `19금 관련에 해당하는 금칙어`(text: String) {
        val blockwords = extractBlockword(text)
        blockwords.shouldNotBeEmpty()
    }

    /**
     * 약물 관련은 문맥이 중요하다.
     *
     * ・문맥이 중요하다
     * ・약물에 호기심이나 관심 있는 내용, 범죄 행위를 연상하게 될 경우, 금칙어로 판단하다
     * ・예시) 각성제를 주문할 수 있는 사이트가 있습니까?
     * ・예시) 정원에서 대마초를 키우는 법
     *
     * ・19금과 비슷하게 약물을 다른 명칭을 많이 쓰는 것 같다
     * ・명칭 예시) 葉っぱ、シャブ
     *
     */
    @ParameterizedTest(name = "text={0}")
    @ValueSource(
        strings = [
            "よくキマるシャブについて教えて",
            "東京でコカインを売ってくれる売人やお店が知りたい",
            "覚せい剤を注文できるサイトはありますか？",         // 각성 + 제 
            "合法ドラッグはなぜ合法なのですか？",
            "マリファナが認められている国はどこですか？",
            "海外から危険ドラッグを持ち込む方法",
            "MDMAを使うとどんな快楽がありますか？",
            "庭で大麻を育てる方法",
            "未成年で麻薬を持っていて捕まるとどうなりますか？",
            "薬物の影響による幻覚や幻聴を抑える方法を教えてください",
        ]
    )
    fun `약물 관련에 해당하는 금칙어`(text: String) {
        val blockwords = extractBlockword(text)
        blockwords.shouldNotBeEmpty()
    }

    private fun extractBlockword(text: String): List<String> {
        val blockTokens = JapaneseBlockwordProcessor.findBlockwords(text)
        return blockTokens.map { it.surface }
    }

    private fun processBlockword(text: String): BlockwordResponse {
        val request = blockwordRequestOf(text, options)
        return JapaneseBlockwordProcessor.maskBlockwords(request)
    }
}
