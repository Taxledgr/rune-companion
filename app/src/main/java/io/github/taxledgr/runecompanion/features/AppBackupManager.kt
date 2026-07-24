package io.github.taxledgr.runecompanion.features

import android.content.Context
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import org.json.JSONArray
import org.json.JSONObject

class AppBackupManager(private val context: Context) {
    fun exportEncrypted(passphrase: String): String {
        require(passphrase.length >= 8) { "Use a passphrase with at least 8 characters" }
        val root = JSONObject()
        PREFERENCE_FILES.forEach { name ->
            val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            root.put(name, JSONObject().apply {
                prefs.all.forEach { (key, value) ->
                    when (value) {
                        is String, is Boolean, is Int, is Long, is Float -> put(key, value)
                        is Set<*> -> put(key, value.filterIsInstance<String>())
                    }
                }
            })
        }
        return encrypt(root.toString(), passphrase)
    }

    fun importEncrypted(payload: String, passphrase: String) {
        require(passphrase.length >= 8) { "Enter the backup passphrase" }
        val root = JSONObject(decrypt(payload.trim(), passphrase))
        PREFERENCE_FILES.forEach { name ->
            val objectValue = root.optJSONObject(name) ?: return@forEach
            val editor = context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear()
            objectValue.keys().forEach { key ->
                when (val value = objectValue.get(key)) {
                    is String -> editor.putString(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Double -> editor.putFloat(key, value.toFloat())
                    is JSONArray -> editor.putStringSet(
                        key,
                        buildSet {
                            for (index in 0 until value.length()) {
                                add(value.optString(index))
                            }
                        },
                    )
                }
            }
            editor.apply()
        }
    }

    private fun encrypt(plainText: String, passphrase: String): String {
        val salt = ByteArray(SALT_SIZE).also(SecureRandom()::nextBytes)
        val iv = ByteArray(IV_SIZE).also(SecureRandom()::nextBytes)
        val cipher = Cipher.getInstance(CIPHER)
        cipher.init(Cipher.ENCRYPT_MODE, key(passphrase, salt), GCMParameterSpec(TAG_BITS, iv))
        val encrypted = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
        return listOf(FORMAT, encode(salt), encode(iv), encode(encrypted)).joinToString(".")
    }

    private fun decrypt(payload: String, passphrase: String): String {
        val parts = payload.split(".")
        require(parts.size == 4 && parts[0] == FORMAT) { "This is not a Rune Companion backup" }
        val salt = decode(parts[1])
        val iv = decode(parts[2])
        val cipher = Cipher.getInstance(CIPHER)
        cipher.init(Cipher.DECRYPT_MODE, key(passphrase, salt), GCMParameterSpec(TAG_BITS, iv))
        return String(cipher.doFinal(decode(parts[3])), StandardCharsets.UTF_8)
    }

    private fun key(passphrase: String, salt: ByteArray) = SecretKeySpec(
        SecretKeyFactory.getInstance(KEY_ALGORITHM)
            .generateSecret(PBEKeySpec(passphrase.toCharArray(), salt, ITERATIONS, KEY_BITS))
            .encoded,
        "AES",
    )

    private fun encode(value: ByteArray): String =
        Base64.encodeToString(value, Base64.NO_WRAP or Base64.URL_SAFE)

    private fun decode(value: String): ByteArray =
        Base64.decode(value, Base64.NO_WRAP or Base64.URL_SAFE)

    private companion object {
        const val FORMAT = "RC1"
        const val CIPHER = "AES/GCM/NoPadding"
        const val KEY_ALGORITHM = "PBKDF2WithHmacSHA256"
        const val ITERATIONS = 150_000
        const val KEY_BITS = 256
        const val TAG_BITS = 128
        const val SALT_SIZE = 16
        const val IV_SIZE = 12
        val PREFERENCE_FILES = listOf(
            "rune_companion_toolkit",
            "rune_companion_features",
            "rune_companion_overlay",
            "shooting_star_alerts",
            "shooting_star_filters",
        )
    }
}
