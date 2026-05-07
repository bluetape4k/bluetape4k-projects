package io.bluetape4k.utils

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.net.InetAddress
import kotlin.test.assertFailsWith

class NetworkxTest {

    companion object: KLogging()

    @Test
    fun `get local host name`() {
        Networkx.LocalhostName.shouldNotBeNull()
    }

    @Test
    fun `get localhost address`() {
        val address = Networkx.Localhost.hostAddress
        log.debug { "localhost address=$address" }
        address.shouldNotBeEmpty()
        address shouldContain "."
    }

    @Test
    fun `Ipv4 형식의 address 인가`() {
        Networkx.isIpv4Address("abc").shouldBeFalse()
        Networkx.isIpv4Address("a.a.a.a").shouldBeFalse()
        Networkx.isIpv4Address("0.0.0.500").shouldBeFalse()

        Networkx.isIpv4Address("0.0.0.0").shouldBeTrue()
        Networkx.isIpv4Address("255.255.255.255").shouldBeTrue()
    }

    @Test
    fun `InetAddress 가 private인지 확인`() {
        Networkx.isPrivateAddress(InetAddress.getByName("127.0.0.1")).shouldBeFalse()

        Networkx.isPrivateAddress(InetAddress.getByName("10.0.0.1")).shouldBeTrue()
        Networkx.isPrivateAddress(InetAddress.getByName("192.168.0.1")).shouldBeTrue()
    }

    @Test
    fun `Ipv4 형식의 문자열을 Int로 변환`() {
        Networkx.ipToInt("0.0.0.0") shouldBeEqualTo 0
        Networkx.ipToInt("255.255.255.255") shouldBeEqualTo -1
        Networkx.ipToInt("127.0.0.1") shouldBeEqualTo 2130706433
    }

    @Test
    fun `유효하지 않은 IPv4 문자열은 null로 변환된다`() {
        Networkx.ipToOptionInt("192.168.0.256").shouldBeNull()
        Networkx.ipToOptionInt("192.168..1").shouldBeNull()
        Networkx.ipToOptionInt("192.168.0").shouldBeNull()
    }

    @Test
    fun `cidr block membership를 확인한다`() {
        val block = Networkx.cidrToIpBlock("192.168.0.0/24")

        Networkx.isIpInBlock(Networkx.ipToInt("192.168.0.42"), block).shouldBeTrue()
        Networkx.isIpInBlock(Networkx.ipToInt("192.168.1.42"), block).shouldBeFalse()
    }

    @Test
    fun `ip block 목록 membership를 확인한다`() {
        val blocks = listOf(
            Networkx.cidrToIpBlock("10.0.0.0/8"),
            Networkx.cidrToIpBlock("192.168.0.0/16"),
        )

        Networkx.isIpInBlocks("10.23.1.5", blocks).shouldBeTrue()
        Networkx.isIpInBlocks("192.168.23.1", blocks).shouldBeTrue()
        Networkx.isIpInBlocks("172.16.1.1", blocks).shouldBeFalse()
    }

    @Test
    fun `inetAddress를 int로 변환한다`() {
        val inet = InetAddress.getByName("127.0.0.1")
        Networkx.inetAddressToInt(inet) shouldBeEqualTo Networkx.INT_VALUE_127_0_0_1
    }

    @Test
    fun `ipToIpBlock은 prefix 범위를 검증한다`() {
        assertFailsWith<IllegalArgumentException> {
            Networkx.ipToIpBlock("10.0.0.0", -1)
        }
        assertFailsWith<IllegalArgumentException> {
            Networkx.ipToIpBlock("10.0.0.0", 33)
        }
    }

    @Test
    fun `ipToIpBlock은 4-octet IP에서 prefixLen null이면 32를 기본값으로 사용한다`() {
        // C1 수정 검증: arr.size==4 && prefixLen==null 이면 NPE 대신 32를 기본값으로 사용
        val (netIp, mask) = Networkx.ipToIpBlock("192.168.0.1", null)
        netIp shouldBeEqualTo Networkx.ipToInt("192.168.0.1")
        mask shouldBeEqualTo -1  // /32 mask = 0xFFFFFFFF = -1 (Int)
    }

    @Test
    fun `ipToIpBlock은 부분 IP에서 prefixLen null이면 octet수 기반 prefix를 사용한다`() {
        // 2-octet IP: arr.size==2, prefixLen==null → pLen = 16
        val (_, mask) = Networkx.ipToIpBlock("10.0", null)
        mask shouldBeEqualTo (-1 shl 16)  // /16 mask
    }
}
