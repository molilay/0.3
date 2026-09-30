package com.ultimatetool.app

import com.ultimatetool.app.core.util.AesGcm
import com.ultimatetool.app.core.util.ColorConvert
import com.ultimatetool.app.core.util.DiffEngine
import com.ultimatetool.app.core.util.Generators
import com.ultimatetool.app.core.util.Hashing
import com.ultimatetool.app.core.util.TarExtractor
import com.ultimatetool.app.core.util.Totp
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Юнит-тесты новой функциональности (чистая JVM, без Android). */
class FeatureLogicTest {

    // ---------- TOTP (векторы RFC 6238) ----------

    @Test
    fun `totp rfc6238 vector at 59 seconds`() {
        // Секрет "12345678901234567890" (ASCII) в base32.
        val base32 = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"
        assertEquals("287082", Totp.currentCode(base32, digits = 6, timeMillis = 59_000L))
        assertEquals("94287082", Totp.currentCode(base32, digits = 8, timeMillis = 59_000L))
    }

    @Test
    fun `totp rfc6238 vector at 1111111109`() {
        val base32 = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"
        assertEquals("07081804", Totp.currentCode(base32, digits = 8, timeMillis = 1_111_111_109_000L))
    }

    @Test
    fun `totp invalid secret returns null`() {
        assertNull(Totp.currentCode("!!!", digits = 6))
    }

    @Test
    fun `base32 decodes rfc secret`() {
        val decoded = Totp.decodeBase32("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ")
        assertEquals("12345678901234567890", String(decoded, Charsets.US_ASCII))
    }

    // ---------- AES-GCM ----------

    @Test
    fun `aes round trip`() {
        val secret = "Секретное сообщение 123"
        val encrypted = AesGcm.encrypt(secret, "пароль")
        assertEquals(secret, AesGcm.decrypt(encrypted, "пароль"))
    }

    @Test
    fun `aes wrong passphrase fails`() {
        val encrypted = AesGcm.encrypt("data", "right")
        try {
            AesGcm.decrypt(encrypted, "wrong")
            fail("Ожидалось падение при неверном пароле")
        } catch (_: Exception) {
            // GCM-тег не сошёлся — ожидаемо.
        }
    }

    // ---------- Генераторы ----------

    @Test
    fun `password contains all selected classes`() {
        val password = Generators.password(
            Generators.PasswordOptions(length = 16, lower = true, upper = true, digits = true, symbols = true)
        )
        assertEquals(16, password.length)
        assertTrue(password.any { it.isLowerCase() })
        assertTrue(password.any { it.isUpperCase() })
        assertTrue(password.any { it.isDigit() })
    }

    @Test
    fun `uuid has canonical format`() {
        assertTrue(
            Generators.uuid().matches(
                Regex("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")
            )
        )
    }

    // ---------- Diff ----------

    @Test
    fun `diff detects add remove and same`() {
        val result = DiffEngine.diff("a\nb\nc", "a\nx\nc")
        val kinds = result.map { it.kind to it.text }
        assertTrue(kinds.contains(DiffEngine.Kind.SAME to "a"))
        assertTrue(kinds.contains(DiffEngine.Kind.REMOVED to "b"))
        assertTrue(kinds.contains(DiffEngine.Kind.ADDED to "x"))
        assertTrue(kinds.contains(DiffEngine.Kind.SAME to "c"))
    }

    // ---------- Цвета ----------

    @Test
    fun `color parse full hex`() {
        val info = ColorConvert.parse("#0B57D0")
        assertNotNull(info)
        assertEquals(0x0B, info!!.r)
        assertEquals(0x57, info.g)
        assertEquals(0xD0, info.b)
    }

    @Test
    fun `color parse short hex`() {
        val info = ColorConvert.parse("F00")
        assertNotNull(info)
        assertEquals(255, info!!.r)
        assertEquals(0, info.g)
    }

    @Test
    fun `color parse garbage returns null`() {
        assertNull(ColorConvert.parse("zzz"))
    }

    // ---------- Хэши: новые алгоритмы ----------

    @Test
    fun `sha384 known vector`() {
        assertEquals(
            "cb00753f45a35e8bb5a03d699ac65007272c32ab0eded1631a8b605a43ff5bed" +
                "8086072ba1e7cc2358baeca134c825a7",
            Hashing.hash("SHA-384", "abc")
        )
    }

    @Test
    fun `crc32 known vector`() {
        assertEquals("352441C2", Hashing.hash("CRC32", "abc"))
    }

    // ---------- Tar-распаковка ----------

    private fun tarHeader(name: String, size: Int, type: Char): ByteArray {
        val header = ByteArray(512)
        fun put(offset: Int, text: String) {
            val bytes = text.toByteArray(Charsets.UTF_8)
            System.arraycopy(bytes, 0, header, offset, minOf(bytes.size, 512 - offset))
        }
        put(0, name)
        put(100, "0000644")
        put(124, "%011o".format(size))
        put(136, "%011o".format(0))
        header[156] = type.code.toByte()
        return header
    }

    @Test
    fun `tar extractor unpacks file and directory`() {
        val archive = File.createTempFile("test", ".tar")
        val target = createTempDir("tar-out")
        try {
            val content = "привет из tar".toByteArray(Charsets.UTF_8)
            archive.outputStream().use { out ->
                // У каталога данных нет — только заголовок (иначе парсер
                // примет нулевой блок за конец архива).
                out.write(tarHeader("dir/", 0, '5'))
                out.write(tarHeader("dir/hello.txt", content.size, '0'))
                out.write(content)
                val pad = 512 - content.size % 512
                if (pad != 512) out.write(ByteArray(pad))
                out.write(ByteArray(1024)) // два пустых блока — конец архива
            }

            TarExtractor.extract(archive, target)

            val extracted = File(target, "dir/hello.txt")
            assertTrue(extracted.exists())
            assertEquals("привет из tar", extracted.readText(Charsets.UTF_8))
        } finally {
            archive.delete()
            target.deleteRecursively()
        }
    }
}
