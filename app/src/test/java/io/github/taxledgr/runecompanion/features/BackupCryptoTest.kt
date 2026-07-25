package io.github.taxledgr.runecompanion.features

import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCryptoTest {
    @Test
    fun currentFormatRoundTripsAndUsesRc2() {
        val payload = BackupCrypto.encrypt(SAMPLE, PASSPHRASE)
        assertTrue(payload.startsWith("RC2."))
        assertEquals(SAMPLE, BackupCrypto.decrypt(payload, PASSPHRASE))
    }

    @Test(expected = IllegalArgumentException::class)
    fun tamperedBackupIsRejected() {
        val parts = BackupCrypto.encrypt(SAMPLE, PASSPHRASE).split('.').toMutableList()
        val encrypted = Base64.getUrlDecoder().decode(parts[3])
        encrypted[0] = (encrypted[0].toInt() xor 1).toByte()
        parts[3] = Base64.getUrlEncoder().withoutPadding().encodeToString(encrypted)
        BackupCrypto.decrypt(parts.joinToString("."), PASSPHRASE)
    }

    @Test
    fun legacyRc1BackupRemainsReadable() {
        val legacy = legacyEncrypt(SAMPLE, LEGACY_PASSPHRASE)
        assertEquals(SAMPLE, BackupCrypto.decrypt(legacy, LEGACY_PASSPHRASE))
    }

    private fun legacyEncrypt(plainText: String, passphrase: String): String {
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val iv = ByteArray(12).also(SecureRandom()::nextBytes)
        val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(passphrase.toCharArray(), salt, 150_000, 256))
            .encoded
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(key, "AES"),
            GCMParameterSpec(128, iv),
        )
        val encoder = Base64.getUrlEncoder()
        return listOf(
            "RC1",
            encoder.encodeToString(salt),
            encoder.encodeToString(iv),
            encoder.encodeToString(
                cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8)),
            ),
        ).joinToString(".")
    }

    private companion object {
        const val SAMPLE = """{"rune_companion_features":{"root":"saved"}}"""
        const val PASSPHRASE = "correct horse battery staple"
        const val LEGACY_PASSPHRASE = "legacy-passphrase"
    }
}
