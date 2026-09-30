package com.ultimatetool.app

import com.ultimatetool.app.core.util.Hashing
import com.ultimatetool.app.core.util.JwtDecoder
import com.ultimatetool.app.plugin_engine.LuaPluginEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Юнит-тесты чистой бизнес-логики (без Android-фреймворка). */
class CoreLogicTest {

    // ---------- Hashing ----------

    @Test
    fun `sha256 of abc matches known vector`() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            Hashing.hash("SHA-256", "abc")
        )
    }

    @Test
    fun `md5 of abc matches known vector`() {
        assertEquals("900150983cd24fb0d6963f7d28e17f72", Hashing.hash("MD5", "abc"))
    }

    // ---------- JWT ----------

    private val sampleJwt =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
            "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIn0." +
            "SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"

    @Test
    fun `jwt decode returns header and payload`() {
        val decoded = JwtDecoder.decode(sampleJwt)
        assertNotNull(decoded)
        assertTrue(decoded!!.headerJson!!.contains("HS256"))
        assertTrue(decoded.payloadJson!!.contains("John Doe"))
    }

    /** Генерируем гарантированно валидный HS256-токен прямо в тесте. */
    private fun signedToken(secret: String): String {
        val enc = java.util.Base64.getUrlEncoder().withoutPadding()
        val header = enc.encodeToString("""{"alg":"HS256","typ":"JWT"}""".toByteArray())
        val payload = enc.encodeToString("""{"sub":"1234567890","name":"John Doe"}""".toByteArray())
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        mac.init(javax.crypto.spec.SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
        val signature = enc.encodeToString(mac.doFinal("$header.$payload".toByteArray()))
        return "$header.$payload.$signature"
    }

    @Test
    fun `jwt verify hs256 with correct secret`() {
        val token = signedToken("secret")
        assertEquals(true, JwtDecoder.verifyHs256(token, "secret"))
    }

    @Test
    fun `jwt verify hs256 with wrong secret`() {
        val token = signedToken("secret")
        assertEquals(false, JwtDecoder.verifyHs256(token, "wrong-secret"))
    }

    @Test
    fun `not a jwt returns null`() {
        assertNull(JwtDecoder.decode("hello world"))
    }

    // ---------- Lua-песочница ----------

    @Test
    fun `plugin logs via tool api`() {
        val engine = LuaPluginEngine(null)
        val result = engine.run("tool.log('ok')")
        assertNull(result.error)
        assertTrue(result.output.contains("ok"))
    }

    @Test
    fun `on_run entry point is executed`() {
        val engine = LuaPluginEngine(null)
        val result = engine.run("function on_run() tool.log('ran') end")
        assertNull(result.error)
        assertTrue(result.output.contains("ran"))
    }

    @Test
    fun `io module is removed from sandbox`() {
        val engine = LuaPluginEngine(null)
        val result = engine.run("io.open('/etc/passwd')")
        assertNotNull("Ожидается ошибка: модуль io вырезан из песочницы", result.error)
    }

    @Test
    fun `os module is removed from sandbox`() {
        val engine = LuaPluginEngine(null)
        val result = engine.run("os.execute('ls')")
        assertNotNull("Ожидается ошибка: модуль os вырезан из песочницы", result.error)
    }

    @Test(timeout = 10_000)
    fun `infinite loop hits timeout`() {
        val engine = LuaPluginEngine(null)
        val result = engine.run("while true do end", timeoutMs = 300)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("Таймаут"))
    }
}
