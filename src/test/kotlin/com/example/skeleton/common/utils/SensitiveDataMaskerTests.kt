package com.example.skeleton.common.utils

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SensitiveDataMaskerTests {
    @Test
    fun `masks every character when value is shorter than three characters`() {
        assertEquals("", SensitiveDataMasker.mask(""))
        assertEquals("*", SensitiveDataMasker.mask("a"))
        assertEquals("**", SensitiveDataMasker.mask("ab"))
    }

    @Test
    fun `keeps only first and last character and preserves length`() {
        assertEquals("a*c", SensitiveDataMasker.mask("abc"))
        assertEquals("p******d", SensitiveDataMasker.mask("p@ssw0rd"))
        assertEquals("0" + "*".repeat(30) + "9", SensitiveDataMasker.mask("0" + "x".repeat(30) + "9"))
    }

    @Test
    fun `treats null and empty value as empty string`() {
        assertEquals("", SensitiveDataMasker.mask(null))
    }

    @Test
    fun `detects sensitive keys regardless of separator and casing`() {
        assertTrue(SensitiveDataMasker.isSensitiveKey("spring.datasource.write.hikari.password"))
        assertTrue(SensitiveDataMasker.isSensitiveKey("app.client.api-key"))
        assertTrue(SensitiveDataMasker.isSensitiveKey("APP_CLIENT_API_KEY"))
        assertTrue(SensitiveDataMasker.isSensitiveKey("app.client.apiKey"))
        assertTrue(SensitiveDataMasker.isSensitiveKey("app.tls.private_key"))

        assertFalse(SensitiveDataMasker.isSensitiveKey("server.port"))
        assertFalse(SensitiveDataMasker.isSensitiveKey("spring.application.name"))
    }

    @Test
    fun `masks value only for sensitive keys`() {
        assertEquals("s" + "*".repeat(8) + "n", SensitiveDataMasker.maskIfSensitive("app.auth.token", "secretoken"))
        assertEquals("18080", SensitiveDataMasker.maskIfSensitive("server.port", "18080"))
        assertEquals("null", SensitiveDataMasker.maskIfSensitive("spring.datasource.password", null))
    }
}
