package io.bluetape4k.io.okio.coroutines

import okio.Buffer
import okio.ByteString
import okio.Options

/**
 * A source that keeps a buffer internally so that callers can do small reads without a performance
 * penalty. It also allows clients to read ahead, buffering as much as necessary before consuming
 * input.
 */
interface BufferedSuspendSource: SuspendSource {

    /**
     * 이 Source의 내부 버퍼.
     */
    val buffer: Buffer

    /**
     * 이 Source에 더 이상 바이트가 없으면 true를 반환합니다. 이것은 읽을 바이트가 있거나 소스가 확실히 고갈 될 때까지 차단됩니다.
     */
    suspend fun exhausted(): Boolean

    /**
     * 버퍼에 적어도 [byteCount] 바이트가 포함되어 있을 때 true를 반환합니다.
     * 필요한 바이트를 읽기 전에 소스가 고갈되면 [okio.EOFException]을 throw합니다.
     */
    suspend fun require(byteCount: Long)

    /**
     * 버퍼에 적어도 `byteCount` 바이트가 포함되어 있을 때 true를 반환하고
     * 필요한 바이트를 읽기 전에 소스가 고갈되면 false를 반환합니다.
     */
    suspend fun request(byteCount: Long): Boolean

    /**
     * 이 `Source`의 내부 버퍼에서 한 Byte를 제거해서 반환한다
     */
    suspend fun readByte(): Byte

    /**
     * 이 `Source`의 내부 버퍼에서 두 Byte를 제거해서 big-endian short를 반환한다
     *
     * ```
     * Buffer buffer = new Buffer()
     *     .writeByte(0x7f)
     *     .writeByte(0xff)
     *     .writeByte(0x00)
     *     .writeByte(0x0f);
     * assertEquals(4, buffer.size());
     *
     * assertEquals(32767, buffer.readShort());
     * assertEquals(2, buffer.size());
     *
     * assertEquals(15, buffer.readShort());
     * assertEquals(0, buffer.size());
     * ```
     */
    suspend fun readShort(): Short

    /**
     * 이 `Source`의 내부 버퍼에서 두 Byte를 제거해서 little-endian short를 반환한다
     *
     * ```
     * Buffer buffer = new Buffer()
     *     .writeByte(0xff)
     *     .writeByte(0x7f)
     *     .writeByte(0x0f)
     *     .writeByte(0x00);
     * assertEquals(4, buffer.size());
     *
     * assertEquals(32767, buffer.readShortLe());
     * assertEquals(2, buffer.size());
     *
     * assertEquals(15, buffer.readShortLe());
     * assertEquals(0, buffer.size());
     * ```
     */
    suspend fun readShortLe(): Short

    /**
     * 이 `Source`의 내부 버퍼에서 네 Byte를 제거해서 big-endian int를 반환한다
     *
     * ```
     * Buffer buffer = new Buffer()
     *     .writeByte(0x7f)
     *     .writeByte(0xff)
     *     .writeByte(0xff)
     *     .writeByte(0xff)
     *     .writeByte(0x00)
     *     .writeByte(0x00)
     *     .writeByte(0x00)
     *     .writeByte(0x0f);
     * assertEquals(8, buffer.size());
     *
     * assertEquals(2147483647, buffer.readInt());
     * assertEquals(4, buffer.size());
     *
     * assertEquals(15, buffer.readInt());
     * assertEquals(0, buffer.size());
     * ```
     */
    suspend fun readInt(): Int

    /**
     * 이 `Source`의 내부 버퍼에서 네 Byte를 제거해서 little-endian int를 반환한다
     *
     * ```
     * Buffer buffer = new Buffer()
     *     .writeByte(0xff)
     *     .writeByte(0xff)
     *     .writeByte(0xff)
     *     .writeByte(0x7f)
     *     .writeByte(0x0f)
     *     .writeByte(0x00)
     *     .writeByte(0x00)
     *     .writeByte(0x00);
     * assertEquals(8, buffer.size());
     *
     * assertEquals(2147483647, buffer.readIntLe());
     * assertEquals(4, buffer.size());
     *
     * assertEquals(15, buffer.readIntLe());
     * assertEquals(0, buffer.size());
     * ```
     */
    suspend fun readIntLe(): Int

    /**
     * 이 `Source`의 내부 버퍼에서 여덟 Byte를 제거해서 big-endian long을 반환한다
     *
     * ```
     * Buffer buffer = new Buffer()
     *     .writeByte(0x7f)
     *     .writeByte(0xff)
     *     .writeByte(0xff)
     *     .writeByte(0xff)
     *     .writeByte(0xff)
     *     .writeByte(0xff)
     *     .writeByte(0xff)
     *     .writeByte(0xff)
     *     .writeByte(0x00)
     *     .writeByte(0x00)
     *     .writeByte(0x00)
     *     .writeByte(0x00)
     *     .writeByte(0x00)
     *     .writeByte(0x00)
     *     .writeByte(0x00)
     *     .writeByte(0x0f);
     * assertEquals(16, buffer.size());
     *
     * assertEquals(9223372036854775807L, buffer.readLong());
     * assertEquals(8, buffer.size());
     *
     * assertEquals(15, buffer.readLong());
     * assertEquals(0, buffer.size());
     * ```
     */
    suspend fun readLong(): Long

    /**
     * 이 `Source`의 내부 버퍼에서 여덟 Byte를 제거해서 little-endian long을 반환한다
     * ```
     * Buffer buffer = new Buffer()
     *     .writeByte(0xff)
     *     .writeByte(0xff)
     *     .writeByte(0xff)
     *     .writeByte(0xff)
     *     .writeByte(0xff)
     *     .writeByte(0xff)
     *     .writeByte(0xff)
     *     .writeByte(0x7f)
     *     .writeByte(0x0f)
     *     .writeByte(0x00)
     *     .writeByte(0x00)
     *     .writeByte(0x00)
     *     .writeByte(0x00)
     *     .writeByte(0x00)
     *     .writeByte(0x00)
     *     .writeByte(0x00);
     * assertEquals(16, buffer.size());
     *
     * assertEquals(9223372036854775807L, buffer.readLongLe());
     * assertEquals(8, buffer.size());
     *
     * assertEquals(15, buffer.readLongLe());
     * assertEquals(0, buffer.size());
     * ```
     */
    suspend fun readLongLe(): Long

    /**
     * 이 소스에서 long을 읽어서 부호 있는 10진수 형식으로 반환합니다 (즉, 선택적으로 선행 '-'가 있는 10진수 문자열로).
     * 이것은 숫자가 아닌 문자가 발견될 때까지 반복됩니다.
     *
     *
     * ```
     * Buffer buffer = new Buffer()
     *     .writeUtf8("8675309 -123 00001");
     *
     * assertEquals(8675309L, buffer.readDecimalLong());
     * assertEquals(' ', buffer.readByte());
     * assertEquals(-123L, buffer.readDecimalLong());
     * assertEquals(' ', buffer.readByte());
     * assertEquals(1L, buffer.readDecimalLong());
     * ```
     *
     * @throws NumberFormatException 찾은 숫자가 `long`에 맞지 않거나 소수점 숫자가 없는 경우
     */
    suspend fun readDecimalLong(): Long

    /**
     * 이 소스에서 long을 16진수 형식으로 읽어서 반환합니다 (즉, 16진수 문자열로).
     * 이것은 16진수가 아닌 문자가 발견될 때까지 반복됩니다.
     * ```
     * Buffer buffer = new Buffer()
     *     .writeUtf8("ffff CAFEBABE 10");
     *
     * assertEquals(65535L, buffer.readHexadecimalUnsignedLong());
     * assertEquals(' ', buffer.readByte());
     * assertEquals(0xcafebabeL, buffer.readHexadecimalUnsignedLong());
     * assertEquals(' ', buffer.readByte());
     * assertEquals(0x10L, buffer.readHexadecimalUnsignedLong());
     * ```
     *
     * @throws NumberFormatException 찾은 16진수가 `long`에 맞지 않거나 16진수가 발견되지 않은 경우
     */
    suspend fun readHexadecimalUnsignedLong(): Long

    /**
     * 이 source에서 `byteCount` 만큼 읽어서 버린다.
     * 주어진 바이트를 건너 뛰기 전에 소스가 고갈되면 [okio.EOFException]을 throw합니다.
     */
    suspend fun skip(byteCount: Long)

    /**
     * 이 소스의 모든 바이트를 읽어서 [ByteString] 형태로 반환합니다. 내부 버퍼는 삭제됩니다.
     */
    suspend fun readByteString(): ByteString

    /**
     * 이 소스의 `byteCount` 바이트를 읽어서 [ByteString] 형태로 반환합니다. 내부 버퍼는 삭제됩니다.
     *
     * @return 읽은 byte array를 담은 [ByteString]
     */
    suspend fun readByteString(byteCount: Long): ByteString

    /**
     * 이 버퍼의 접두사인 `options` 중 첫 번째 문자열을 찾아 이 버퍼에서 소비하고 해당 인덱스를 반환합니다.
     * `options`의 바이트 문자열이 이 버퍼의 접두사가 아닌 경우 -1을 반환하고 바이트가 소비되지 않습니다.
     * 이것은 예상되는 값 집합이 미리 알려진 경우, [readByteString] 또는 [readUtf8] 대체로 사용할 수 있습니다.
     *
     * ```
     * Options FIELDS = Options.of(
     *     ByteString.encodeUtf8("depth="),
     *     ByteString.encodeUtf8("height="),
     *     ByteString.encodeUtf8("width="));
     *
     * Buffer buffer = new Buffer()
     *     .writeUtf8("width=640\n")
     *     .writeUtf8("height=480\n");
     *
     * assertEquals(2, buffer.select(FIELDS));
     * assertEquals(640, buffer.readDecimalLong());
     * assertEquals('\n', buffer.readByte());
     * assertEquals(1, buffer.select(FIELDS));
     * assertEquals(480, buffer.readDecimalLong());
     * assertEquals('\n', buffer.readByte());
     * ```
     */
    suspend fun select(options: Options): Int

    /**
     * 이 소스의 모든 바이트를 읽어서 byte array로 반환합니다. 내부 버퍼는 삭제됩니다.
     */
    suspend fun readByteArray(): ByteArray

    /**
     * 이 소스의 `byteCount` 바이트를 읽어서 byte array로 반환합니다. 내부 버퍼는 삭제됩니다.
     *
     * @return 읽은 byte array
     */
    suspend fun readByteArray(byteCount: Long): ByteArray

    /**
     * 이 소스로부터 `sink.length` 만큼의 바이트를 복사해서 `sink`에 쓴다. 소스로부터는 제거된다.
     *
     * @param sink 읽은 byte array를 저장할 대상
     * @return 읽은 바이트 수, 원하는 바이트 수만큼 읽지 못했을 때에는 -1 을 반환 한다.
     */
    suspend fun read(sink: ByteArray): Int

    /**
     * 이 소스로부터 `sink.length` 만큼의 바이트를 복사해서 `sink`에 쓴다. 소스로부터는 제거된다.
     *
     * 원하는 바이트 수만큼 읽지 못했을 때에는 [okio.EOFException]을 throw한다.
     *
     * @param sink 읽은 byte array를 저장할 대상
     * @return 읽은 바이트 수, 원하는 바이트 수만큼 읽지 못했을 때에는 [okio.EOFException]을 throw한다.
     */
    suspend fun readFully(sink: ByteArray)

    /**
     * 이 소스에서 `byteCount` 바이트를 제거하고 `sink`의 `offset`에 복사한다.
     *
     * @param sink 읽은 byte array를 저장할 대상
     * @param offset `sink`에 복사할 시작 위치
     * @param byteCount 읽을 바이트 수
     * @return 읽은 바이트 수, 원하는 바이트 수만큼 읽지 못했을 때에는 -1 을 반환 한다.
     */
    suspend fun read(sink: ByteArray, offset: Int, byteCount: Int): Int

    /**
     * 이 소스로부터 `sink.length` 만큼의 바이트를 복사해서 `sink`에 쓴다. 소스로부터는 제거된다.
     * 원하는 바이트 수만큼 읽지 못했을 때에는 [okio.EOFException]을 throw한다.
     *
     * @param sink 읽은 byte array를 저장할 대상
     * @param byteCount 읽을 바이트 수
     */
    suspend fun readFully(sink: Buffer, byteCount: Long)

    /**
     * 이 소스의 모든 bytes를 읽어서 `sink`에 쓴다. 소스로부터는 제거된다.
     * Removes all bytes from this and appends them to `sink`.
     *
     * @param sink 읽은 byte array를 저장할 대상
     * @return `sink`에 쓴 총 바이트 수를 반환한다. 이 source가 exhausted 되면 0을 반환한다
     */
    suspend fun readAll(sink: SuspendSink): Long

    /**
     * 이 소스의 모든 bytes를 읽어서 UTF-8로 디코딩하여 문자열로 반환한다.
     * 소스의 내용이 비어 있다면 빈 문자열을 반환한다.
     *
     * ```
     * Buffer buffer = new Buffer()
     *     .writeUtf8("Uh uh uh!")
     *     .writeByte(' ')
     *     .writeUtf8("You didn't say the magic word!");
     *
     * assertEquals("Uh uh uh! You didn't say the magic word!", buffer.readUtf8());
     * assertEquals(0, buffer.size());
     *
     * assertEquals("", buffer.readUtf8());
     * assertEquals(0, buffer.size());
     * ```
     */
    suspend fun readUtf8(): String

    /**
     * 이 소스의 `byteCount` 수 만큼의 bytes를 읽어서 UTF-8로 디코딩하여 문자열로 반환한다.
     * 소스의 내용이 비어 있다면 빈 문자열을 반환한다.
     *
     * ```
     * Buffer buffer = new Buffer()
     *     .writeUtf8("Uh uh uh!")
     *     .writeByte(' ')
     *     .writeUtf8("You didn't say the magic word!");
     * assertEquals(40, buffer.size());
     *
     * assertEquals("Uh uh uh! You ", buffer.readUtf8(14));
     * assertEquals(26, buffer.size());
     *
     * assertEquals("didn't say the", buffer.readUtf8(14));
     * assertEquals(12, buffer.size());
     *
     * assertEquals(" magic word!", buffer.readUtf8(12));
     * assertEquals(0, buffer.size());
     * ```
     */
    suspend fun readUtf8(byteCount: Long): String

    /**
     * 이 소스의 개행문자까지의 한 라인의 문자열을 반환한다. 단 개행문자는 결과 라인에서 제거된다.
     * ```
     * Buffer buffer = new Buffer()
     *     .writeUtf8("I'm a hacker!\n")
     *     .writeUtf8("That's what I said: you're a nerd.\n")
     *     .writeUtf8("I prefer to be called a hacker!\n");
     * assertEquals(81, buffer.size());
     *
     * assertEquals("I'm a hacker!", buffer.readUtf8Line());
     * assertEquals(67, buffer.size());
     *
     * assertEquals("That's what I said: you're a nerd.", buffer.readUtf8Line());
     * assertEquals(32, buffer.size());
     *
     * assertEquals("I prefer to be called a hacker!", buffer.readUtf8Line());
     * assertEquals(0, buffer.size());
     *
     * assertEquals(null, buffer.readUtf8Line());
     * assertEquals(0, buffer.size());
     * ```
     *
     * [java.io.BufferedReader]처럼, **스트림의 끝에 도달하면 null을 반환합니다.**
     * 만약 개행문자 없이 소스가 끝나면, 암시적인 개행문자가 있다고 가정합니다.
     * 소스가 exhausted 되면 null을 반환합니다. 이것은 사람이 작성한 데이터에 사용하며, 라인 끝에 개행문자가 없어도 된다.
     */
    suspend fun readUtf8Line(): String?

    /**
     * 이 소스의 개행문자까지의 한 라인의 문자열을 반환한다. 단 개행문자는 결과 라인에서 제거된다.
     *
     * **스트림의 끝에 도달하면 예외를 일으킵니다**
     * 모든 호출은 '\r\n' 또는 '\n'을 소비해야 합니다. 이 문자가 스트림에 없으면 [okio.EOFException]이 throw됩니다.
     * 이것은 개행문자가 생략된 것은 truncated 된 입력이라고 간주하는 기계가 생성한 데이터에 사용합니다.
     */
    suspend fun readUtf8LineStrict(): String

    /**
     * [readUtf8LineStrict]와 같은 기능인데, 특정 최대 길이를 지정한다.
     * 이러한 보호조치는 `"\n"` 또는 `"\r\n"`을 포함하지 않을 수 있는 스트림에 대해 사용합니다.
     *
     * 반환된 문자열은 최대 `limit` UTF-8 바이트를 가지며, 스캔된 최대 바이트 수는 `limit + 2`입니다.
     * 만약 `limit == 0`이면, 이것은 항상 `EOFException`을 throw합니다. 왜냐하면 바이트가 스캔되지 않기 때문입니다.
     *
     * 이 메서드는 안전합니다. 일치하지 않으면 바이트가 삭제되지 않으며, 호출자는 다른 일치를 시도할 수 있습니다:
     * ```
     * Buffer buffer = new Buffer();
     * buffer.writeUtf8("12345\r\n");
     *
     * // This will throw! There must be \r\n or \n at the limit or before it.
     * buffer.readUtf8LineStrict(4);
     *
     * // No bytes have been consumed so the caller can retry.
     * assertEquals("12345", buffer.readUtf8LineStrict(5));
     * ```
     */
    suspend fun readUtf8LineStrict(limit: Long): String

    /**
     * source를 읽어 한개의 UTF-8 코드 포인트를 제거하고 반환한다. 필요한 경우 1바이트에서 4바이트까지 읽는다.
     *
     * 만약 완전한 코드 포인트를 읽기 전에 이 소스가 고갈되면, [okio.EOFException]을 throw하고 입력을 소비하지 않는다.
     *
     * 이 소스가 올바르게 인코딩된 UTF-8 코드 포인트로 시작하지 않으면,
     * 이 메서드는 1바이트 이상의 UTF-8이 아닌 바이트를 제거하고 대체 문자 (`U+FFFD`)를 반환한다.
     *
     * 이것은 인코딩 문제 (입력이 올바르게 인코딩된 UTF-8이 아님), Unicode의 0x10ffff 한계를 넘는 문자,
     * UTF-8 서로게이트 (U+d800..U+dfff) 및 오버롱 인코딩 (수정된 UTF-8에서 NUL 문자에 대한 `0xc080`과 같은)에 대한 대안이다.
     */
    suspend fun readUtf8CodePoint(): Int

    /**
     * `b`가 `fromIndex`부터 `toIndex`까지의 범위에서 발견되면 그 인덱스를 반환한다.
     * `b`가 발견되지 않거나 `fromIndex == toIndex`이면 -1을 반환한다.
     *
     * scan은 `toIndex` 또는 버퍼의 끝 중 먼저 도달하는 곳에서 종료된다. 스캔된 최대 바이트 수는 `toIndex-fromIndex`이다.
     */
    suspend fun indexOf(b: Byte, fromIndex: Long = 0L, toIndex: Long = Long.MAX_VALUE): Long

    /**
     * 버퍼 안에 `fromIndex` 이후에 `bytes`와 매칭되는 첫번째 인덱스를 반환합니다.
     * 이 메서드는 필요한 바이트를 읽기 전에 소스가 고갈될 때까지 버퍼를 확장합니다.
     * ```
     * ByteString MOVE = ByteString.encodeUtf8("move");
     *
     * Buffer buffer = new Buffer();
     * buffer.writeUtf8("Don't move! He can't see us if we don't move.");
     *
     * assertEquals(6,  buffer.indexOf(MOVE));
     * assertEquals(40, buffer.indexOf(MOVE, 12));
     * ```
     */
    suspend fun indexOf(bytes: ByteString, fromIndex: Long): Long

    /**
     * `targetBytes`가 `fromIndex`부터 `toIndex`까지의 범위에서 발견되면 그 첫번째 인덱스를 반환한다.
     * target bytes가 발견되지 않으면 소스가 고갈될 때까지 버퍼를 확장한다.
     *
     * `targetBytes`가 발견되지 않거나 `fromIndex == toIndex`이면 -1을 반환한다.
     * This reads an unbounded number of bytes into the buffer.
     *
     * 만약 스트림이 요청 바이트를 찾가 전에 소진되면 -1을 반환한다.
     * ```
     * ByteString ANY_VOWEL = ByteString.encodeUtf8("AEOIUaeoiu");
     *
     * Buffer buffer = new Buffer();
     * buffer.writeUtf8("Dr. Alan Grant");
     *
     * assertEquals(4,  buffer.indexOfElement(ANY_VOWEL));    // 'A' in 'Alan'.
     * assertEquals(11, buffer.indexOfElement(ANY_VOWEL, 9)); // 'a' in 'Grant'.
     * ```
     */
    suspend fun indexOfElement(targetBytes: ByteString, fromIndex: Long = 0L, toIndex: Long = Long.MAX_VALUE): Long

    /**
     * 이 소스의 `offset`에 있는 데이터와 `bytes`의 `bytesOffset`에 있는 데이터가 `byteCount` 만큼 같으면 true를 반환한다.
     * 바이트가 일치하지 않거나 모든 바이트가 일치하거나 충분한 바이트가 일치하는지 확인하기 전에 스트림이 소진될 때까지 필요에 따라 버퍼가 확장됩니다.
     */
    suspend fun rangeEquals(offset: Long, bytes: ByteString, bytesOffset: Int = 0, byteCount: Int = bytes.size): Boolean

    /**
     * 이 [BufferedSuspendSource]의 소비없이 데이터를 읽을 수 있는 새로운 [BufferedSuspendSource]를 반환한다.
     *
     * 반환된 소스는 이 소스의 데이터를 읽을 수 있지만, 이 소스가 다음에 읽히거나 닫히면 더 이상 유효하지 않다.
     *
     * 예를 들어, `peek()`를 사용하여 데이터를 여러 번 읽을 수 있다.
     *
     * ```
     * val buffer = Buffer()
     * buffer.writeUtf8("abcdefghi")
     *
     * buffer.readUtf8(3) // returns "abc", buffer contains "defghi"
     *
     * val peek = buffer.peek()
     * peek.readUtf8(3) // returns "def", buffer contains "defghi"
     * peek.readUtf8(3) // returns "ghi", buffer contains "defghi"
     *
     * buffer.readUtf8(3) // returns "def", buffer contains "ghi"
     * ```
     */
    fun peek(): BufferedSuspendSource
}
