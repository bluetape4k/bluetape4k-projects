package io.bluetape4k.science.exposed.service.internal

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.science.exposed.NetCdfException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class NetCdfFileGuardTest {

    @Test
    fun `final symlink is rejected`(@TempDir dir: Path) {
        val target = Files.writeString(dir.resolve("target.nc"), "not-netcdf")
        val link = Files.createSymbolicLink(dir.resolve("link.nc"), target.fileName)

        assertFailsWith<NetCdfException.FileOpen> {
            NetCdfFileGuard.validateForRegister(link.toString())
        }
    }

    @Test
    fun `parent symlink is rejected`(@TempDir dir: Path) {
        val targetDir = Files.createDirectory(dir.resolve("target-dir"))
        Files.writeString(targetDir.resolve("sample.nc"), "not-netcdf")
        val linkDir = Files.createSymbolicLink(dir.resolve("link-dir"), targetDir.fileName)

        assertFailsWith<NetCdfException.FileOpen> {
            NetCdfFileGuard.validateForRegister(linkDir.resolve("sample.nc").toString())
        }
    }
}
