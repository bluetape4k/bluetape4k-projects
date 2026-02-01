package io.bluetape4k.idgenerators.hashids

import io.bluetape4k.collections.eclipse.fastListOf
import io.bluetape4k.collections.eclipse.primitives.toFastList
import io.bluetape4k.collections.toLongArray
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import java.io.Serializable
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.sign

/**
 * YouTube 처럼 단축 URL을 만드는데 사용하는 Hashids 알고리즘을 구현한 클래스
 *
 * ```kotlin
 * val hashids = Hashids("great korea")
 *
 * val x = Long.MAX_VALUE
 * val y = -1L
 * val encoded = hashids.encode(x, y)
 * val decoded = hashids.decode(encoded)
 * ```
 */
class Hashids(
    salt: String = DEFAULT_SALT,
    minHashLength: Int = DEFAULT_MIN_HASH_LEN,
    customAlphabet: String = DEFAULT_ALPHABET,
) {

    companion object: KLogging() {
        /** 기본 알고리즘에서 변환을 지원하는 최대 값 */
        const val MAX_NUMBER = 9007199254740992L

        private const val DEFAULT_SALT = ""
        private const val DEFAULT_MIN_HASH_LEN = 0
        private const val DEFAULT_ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"
        private const val DEFAULT_SEPARATORS = "cfhistuCFHISTU"
        private const val MIN_ALPHABET_LEN = 16
        private const val SEPARATOR_DIV = 3.5
        private const val GUARD_DIV = 12

        private const val EMPTY_STRING = ""
        private const val SPACE = " "
        internal const val NUMBER_SEPARATOR = "-"
        internal const val LARGE_NUMBER_SUFFIX = "="

        private val hexRegex: Regex = "^[0-9a-fA-F]+$".toRegex()
        private val numRegex: Regex = "[\\w\\W]{1,12}".toRegex()


        private fun String.calcSeparatorsLength(): Int {
            val separatorCount = ceil(this.length / SEPARATOR_DIV).toInt()
            return if (separatorCount == 1) 2 else separatorCount
        }

        private fun String.unique(): String = toSet().joinToString(separator = EMPTY_STRING)

        private fun Long.isValidNumber() = this in 0..MAX_NUMBER

    }

    private val salt: String = salt.ifEmpty { DEFAULT_SALT }
    private val hashLength: Int = if (minHashLength > 0) minHashLength else DEFAULT_MIN_HASH_LEN

    private val alphabetAndSeparators: AlphabetAndSeparators by lazy { calculateAlphabetAndSeparators(customAlphabet) }
    private val alphabet: String = alphabetAndSeparators.alphabet
    private val separators: String = alphabetAndSeparators.separators
    private val guards: String = alphabetAndSeparators.guards

    private val separatorsRegex: Regex = "[${separators}]".toRegex()
    private val guardsRegex: Regex = "[${guards}]".toRegex()

    /**
     * Long 수형을 인코딩해서 문자열로 만듭니다.
     *
     * @param numbers 인코드할 Long 숫자들
     * @return 인코딩된 문자열
     */
    fun encode(vararg numbers: Long): String {
        if (numbers.isEmpty()) {
            return EMPTY_STRING
        }
        return when {
            numbers.size == 1 -> encodeSingle(numbers[0])
            numbers.all { it.isValidNumber() } -> encodeArray(*numbers)
            else -> numbersToArrayList(numbers).joinToString(NUMBER_SEPARATOR) {
                if (it.first) {  // valid number 인 경우
                    encodeArray(*it.second)
                } else {
                    // valid number가 아닌 경우
                    encodeSingle(it.second.first())
                }
            }
        }
    }

    private fun encodeSingle(number: Long): String {
        if (number in 0..MAX_NUMBER) {
            return encodeArray(number)
        }
        return encodeArray(*numberToArray(number)) + LARGE_NUMBER_SUFFIX
    }

    private fun numbersToArrayList(numbers: LongArray): List<Pair<Boolean, LongArray>> {
        val result = fastListOf<Pair<Boolean, LongArray>>()
        val current = fastListOf<Long>()

        numbers.forEach {
            if (it.isValidNumber()) {
                current.add(it)
            } else {
                if (current.isNotEmpty()) {
                    result.add(true to current.toLongArray())
                    current.clear()
                }
                result.add(false to longArrayOf(it))
            }
        }
        if (current.isNotEmpty()) {
            result.add(true to current.toLongArray())
        }
        return result
    }

    private fun numberToArray(number: Long): LongArray {
        return if (number in 0..MAX_NUMBER) longArrayOf(number)
        else {
            val quotient = number / MAX_NUMBER
            val reminder = number % MAX_NUMBER

            if (number.sign > 0) longArrayOf(reminder, quotient)
            else longArrayOf(-reminder, 0, -quotient)
        }
    }

    private fun encodeArray(vararg numbers: Long): String {
        val numbersHash = numbers.indices.sumOf { (numbers[it] % (it + 100)).toInt() }
        val initialChar = alphabet[numbersHash % alphabet.length].toString()
        log.trace { "numbersHash=$numbersHash, initialChar=$initialChar" }

        val (encodedString, encodingAlphabet) = initEncode(
            numbers.asList(),
            separators,
            initialChar,
            0,
            alphabet,
            initialChar
        )
        val tempReturnString = addGuardsIfNecessary(encodedString, numbersHash)

        val halfLength = alphabet.length / 2
        return ensureMinLength(halfLength, encodingAlphabet, tempReturnString)
    }

    /**
     * 인코딩된 문자열을 디코딩해서 [LongArray]를 반환합니다.
     *
     * @param hash 인코딩된 문자열
     * @return 디코딩된 [LongArray]
     */
    fun decode(hash: String): LongArray {
        val hashes = hash.split(NUMBER_SEPARATOR)
        return hashes.flatMap { decodeSingle(it).toFastList() }.toLongArray()
    }

    private fun decodeSingle(hash: String): LongArray {
        return if (hash.endsWith(LARGE_NUMBER_SUFFIX)) {
            longArrayOf(arrayToLong(decodeInternal(hash.dropLast(1))))
        } else {
            decodeInternal(hash)
        }
    }

    private fun arrayToLong(array: LongArray): Long = when (array.size) {
        1 -> array[0]
        2 -> {
            val reminder = array[0]
            val quotient = array[1]
            quotient * MAX_NUMBER + reminder
        }

        3 -> {
            val reminder = array[0]
            val quotient = array[2]
            -(quotient * MAX_NUMBER + reminder)
        }

        else -> throw IllegalArgumentException("Wrong array size[${array.size}]")
    }

    private fun decodeInternal(hash: String): LongArray {
        if (hash.isEmpty()) {
            return longArrayOf()
        }

        val initialSplit: List<String> = hash.replace(guardsRegex, SPACE).split(SPACE)
        val (lottery, hashBreakdown) = extractLotteryCharAndHashArray(initialSplit)

        log.trace { "lottery=$lottery, hashBreakdown=${hashBreakdown.joinToString()}" }
        val returnValue = unhashSubHashes(hashBreakdown.iterator(), lottery, fastListOf(), alphabet)
        log.trace { "returnValue=${returnValue.joinToString()}" }

        val decodedValue =
            if (encode(*returnValue) != hash) longArrayOf()
            else returnValue
        log.trace { "Decode hash=[$hash], returnValue=${returnValue.joinToString()}, decodedValue=${decodedValue.joinToString()}" }
        return decodedValue
    }

    private fun guardIndex(numbersHash: Int, returnStr: String, index: Int): Int =
        (numbersHash + returnStr[index].code) % guards.length

    /**
     * Encode hexadecimal formatted string
     *
     * @param hexStr hexadecimal formatted string to encode
     * @return encoded string as hash
     */
    private fun encodeHex(hexStr: String): String {
        if (!hexStr.matches(hexRegex)) {
            return EMPTY_STRING
        }

        val toEncode =
            numRegex.findAll(hexStr)
                .flatMap { it.groupValues }
                .map { it.toLong(16) }
                .toLongArray()

        return encode(*toEncode)
    }

    /**
     * Decode hash to hexadecimal formatted string
     *
     * @param hash encoded string
     * @return hexadecimal formatted string
     */
    private fun decodeHex(hash: String): String {
        return decode(hash).map { n -> n.toString(16).substring(1) }.toString()
    }

    private fun calculateAlphabetAndSeparators(userAlphabet: String): AlphabetAndSeparators {
        val uniqueAlphabet = userAlphabet.unique()
        assert(uniqueAlphabet.length >= MIN_ALPHABET_LEN) {
            "alphabet must contain at least $MIN_ALPHABET_LEN unique characters"
        }
        require(!uniqueAlphabet.contains(SPACE)) { "alphabet cannot contains spaces" }

        val legalSeparators = DEFAULT_SEPARATORS.toSet().intersect(uniqueAlphabet.toSet())
        val alphabetWithoutSeparators = String(uniqueAlphabet.toSet().minus(legalSeparators).toCharArray())
        val shuffleSeparators = consistentShuffle(String(legalSeparators.toCharArray()), salt)
        val (adjustedAlphabet, adjustedSeparators) = adjustAlphabetAndSeparators(
            alphabetWithoutSeparators,
            shuffleSeparators
        )

        val guardCount = ceil(adjustedAlphabet.length.toDouble() / GUARD_DIV).toInt()

        return if (adjustedAlphabet.length < 3) {
            val guards = adjustedSeparators.substring(0, guardCount)
            val seps = adjustedSeparators.substring(guardCount)
            AlphabetAndSeparators(adjustedAlphabet, seps, guards)
        } else {
            val guards = adjustedAlphabet.substring(0, guardCount)
            val alphabet = adjustedAlphabet.substring(guardCount)
            AlphabetAndSeparators(alphabet, adjustedSeparators, guards)
        }
    }

    private fun adjustAlphabetAndSeparators(
        alphabetWithoutSeparators: String,
        shuffledSeparators: String,
    ): AlphabetAndSeparators {
        val moreSeparators = (alphabetWithoutSeparators.length / shuffledSeparators.length).toFloat() > SEPARATOR_DIV

        return if (shuffledSeparators.isEmpty() || moreSeparators) {
            val sepsLength = alphabetWithoutSeparators.calcSeparatorsLength()

            if (sepsLength > shuffledSeparators.length) {
                val difference = sepsLength - shuffledSeparators.length
                val seps = shuffledSeparators + alphabetWithoutSeparators.substring(0, difference)
                val alpha = alphabetWithoutSeparators.substring(difference)
                AlphabetAndSeparators(consistentShuffle(alpha, salt), seps)
            } else {
                val seps = shuffledSeparators.substring(0, sepsLength)
                AlphabetAndSeparators(consistentShuffle(alphabetWithoutSeparators, salt), seps)
            }
        } else {
            AlphabetAndSeparators(consistentShuffle(alphabetWithoutSeparators, salt), shuffledSeparators)
        }
    }

    private fun addGuardsIfNecessary(encodedStr: String, numbersHash: Int): String {
        if (encodedStr.length >= hashLength) {
            return encodedStr
        }

        val guard0 = guards[guardIndex(numbersHash, encodedStr, 0)]
        val retStr = guard0 + encodedStr

        return if (retStr.length < hashLength) {
            val guard2 = guards[guardIndex(numbersHash, retStr, 2)]
            retStr + guard2
        } else retStr
    }

    private fun extractLotteryCharAndHashArray(initialSplit: List<String>): Pair<Char, List<String>> {
        val index = if (initialSplit.size == 2 || initialSplit.size == 3) 1 else 0
        val nthElementOfSplit = initialSplit[index]

        val lotteryChar = nthElementOfSplit.first()
        val breakdown = nthElementOfSplit
            .substring(1)
            .replace(separatorsRegex, SPACE)
            .split(SPACE)

        return lotteryChar to breakdown
    }

    private tailrec fun unhashSubHashes(
        hashes: Iterator<String>,
        lottery: Char,
        currentReturn: MutableList<Long>,
        alphabet: String,
    ): LongArray {
        if (!hashes.hasNext()) {
            return currentReturn.toLongArray()
        }

        val subHash = hashes.next()
        val buffer = "$lottery$salt$alphabet"
        val newAlphabet = consistentShuffle(alphabet, buffer.substring(0, alphabet.length))
        currentReturn.add(unhash(subHash, newAlphabet))

        return unhashSubHashes(hashes, lottery, currentReturn, newAlphabet)
    }

    private fun hash(input: Long, alphabet: String): String {
        return doHash(input, alphabet, HashData(EMPTY_STRING, input)).hash
    }

    private tailrec fun doHash(number: Long, alphabet: String, data: HashData): HashData {
        return if (data.current > 0) {
            val newHashChar = alphabet[(data.current % alphabet.length).toInt()]
            val newCurrent = data.current / alphabet.length
            doHash(number, alphabet, HashData("$newHashChar${data.hash}", newCurrent))
        } else data
    }

    private fun unhash(input: String, alphabet: String): Long {
        return doUnhash(
            input.toCharArray(),
            alphabet,
            alphabet.length.toDouble(),
            0,
            0
        )
    }

    private tailrec fun doUnhash(
        input: CharArray,
        alphabet: String,
        alphabetLength: Double,
        currentNumber: Long,
        currentIndex: Int,
    ): Long {
        if (currentIndex >= input.size) {
            return currentNumber
        }

        val pos = alphabet.indexOf(input[currentIndex])
        val newNum = currentNumber + (pos * alphabetLength.pow(input.size - currentIndex - 1)).toLong()
        return doUnhash(input, alphabet, alphabetLength, newNum, currentIndex + 1)
    }

    private fun consistentShuffle(alphabet: String, salt: String): String {
        if (salt.isEmpty()) {
            return alphabet
        }
        val initial = ShuffleData(alphabet, salt, 0, 0)
        return shuffle(initial, alphabet.length - 1, 1).alphabet
    }

    private tailrec fun shuffle(data: ShuffleData, currentPos: Int, limit: Int): ShuffleData {
        if (currentPos < limit) {
            return data
        }

        val currentAlphabet = data.alphabet.toCharArray()
        val saltReminder = data.saltReminder % data.salt.length
        val asciiValue = data.salt[saltReminder].code
        val cumulativeValue = data.cumulative + asciiValue
        val posToSwap = (asciiValue + saltReminder + cumulativeValue) % currentPos

        currentAlphabet[posToSwap] = currentAlphabet[currentPos].also {
            currentAlphabet[currentPos] = currentAlphabet[posToSwap]
        }

        return shuffle(
            ShuffleData(String(currentAlphabet), data.salt, cumulativeValue, saltReminder + 1),
            currentPos - 1,
            limit
        )
    }

    private tailrec fun initEncode(
        numbers: List<Long>,
        separators: String,
        bufferSeed: String,
        currentIndex: Int,
        alphabet: String,
        currentReturnStr: String,
    ): Pair<String, String> {
        if (currentIndex !in numbers.indices) {
            return currentReturnStr to alphabet
        }

        val currentNumber = numbers[currentIndex]
        val buffer = bufferSeed + salt + alphabet
        val nextAlphabet = consistentShuffle(alphabet, buffer.substring(0, alphabet.length))
        val lastStr = hash(currentNumber, nextAlphabet)

        val newReturnStr = if (currentIndex + 1 >= numbers.size) {
            currentReturnStr + lastStr
        } else {
            if (lastStr.isNotEmpty()) {
                val nextNumber = currentNumber % (lastStr[0].code + currentIndex)
                val sepsIndex = (nextNumber % separators.length).toInt()
                currentReturnStr + lastStr + separators[sepsIndex]
            } else {
                val sepsIndex = 0
                currentReturnStr + lastStr + separators[sepsIndex]
            }
        }

        return initEncode(numbers, separators, bufferSeed, currentIndex + 1, nextAlphabet, newReturnStr)
    }

    private tailrec fun ensureMinLength(
        halfLength: Int,
        alphabet: String,
        returnStr: String,
    ): String {
        if (returnStr.length >= hashLength) {
            return returnStr
        }

        val newAlphabet = consistentShuffle(alphabet, alphabet)
        val tempReturnStr = newAlphabet.substring(halfLength) + returnStr + newAlphabet.substring(0, halfLength)
        val excess = tempReturnStr.length - hashLength
        val newReturnStr = when {
            excess > 0 -> {
                val pos = excess / 2
                tempReturnStr.substring(pos, pos + hashLength)
            }

            else -> tempReturnStr
        }

        return ensureMinLength(halfLength, newAlphabet, newReturnStr)
    }

    internal data class HashData(
        val hash: String,
        val current: Long,
    ): Serializable

    internal data class ShuffleData(
        val alphabet: String,
        val salt: String,
        val cumulative: Int,
        val saltReminder: Int,
    ): Serializable

    internal data class AlphabetAndSeparators(
        val alphabet: String,
        val separators: String,
        val guards: String = "",
    ): Serializable
}
