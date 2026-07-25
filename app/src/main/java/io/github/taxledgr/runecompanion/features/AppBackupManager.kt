package io.github.taxledgr.runecompanion.features

import android.content.Context
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import org.json.JSONArray
import org.json.JSONObject
import io.github.taxledgr.runecompanion.personalization.ActivityProfilePreferences

class AppBackupManager(private val context: Context) {
    fun exportEncrypted(passphrase: String): String {
        BackupCrypto.requireNewPassphrase(passphrase)
        val root = JSONObject()
        PREFERENCE_FILES.forEach { name ->
            val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            root.put(name, JSONObject().apply {
                prefs.all.forEach { (key, value) ->
                    when (value) {
                        is String, is Boolean, is Int, is Long, is Float -> put(key, value)
                        is Set<*> -> put(key, JSONArray(value.filterIsInstance<String>()))
                    }
                }
            })
        }
        return BackupCrypto.encrypt(root.toString(), passphrase)
    }

    fun importEncrypted(payload: String, passphrase: String) {
        BackupCrypto.requireImportPassphrase(passphrase)
        val root = JSONObject(BackupCrypto.decrypt(payload, passphrase))
        val updates = linkedMapOf<String, Map<String, Any>>()
        PREFERENCE_FILES.forEach { name ->
            if (!root.has(name)) {
                if (name == ActivityProfilePreferences.PREFERENCES_NAME) {
                    updates[name] = emptyMap()
                }
                return@forEach
            }
            val objectValue = root.get(name)
            require(objectValue is JSONObject) { "Backup contains invalid preference data" }
            updates[name] = parsePreferenceValues(objectValue)
        }
        require(updates.isNotEmpty()) { "Backup does not contain Rune Companion data" }

        val snapshots = updates.keys.associateWith { name ->
            context.getSharedPreferences(name, Context.MODE_PRIVATE).all
                .mapValues { (_, value) -> requireNotNull(value) }
        }
        try {
            updates.forEach { (name, values) ->
                check(replacePreferences(name, values)) {
                    "Android could not save the restored data"
                }
            }
            if (PREFERENCES_FEATURES in updates) {
                FeaturePreferences(context).refreshDatabaseFromPreferenceMirror()
            }
        } catch (error: Throwable) {
            snapshots.forEach { (name, values) -> replacePreferences(name, values) }
            if (PREFERENCES_FEATURES in snapshots) {
                runCatching {
                    FeaturePreferences(context).refreshDatabaseFromPreferenceMirror()
                }
            }
            throw error
        }
    }

    private fun parsePreferenceValues(source: JSONObject): Map<String, Any> {
        require(source.length() <= MAX_KEYS_PER_FILE) { "Backup contains too many settings" }
        return buildMap {
            val keys = source.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                require(key.length <= MAX_KEY_LENGTH) { "Backup contains an invalid setting name" }
                val parsed = when (val value = source.get(key)) {
                    is String -> value.also {
                        require(it.length <= MAX_VALUE_LENGTH) {
                            "Backup setting exceeds the safety limit"
                        }
                    }
                    is Boolean, is Int, is Long -> value
                    is Double -> {
                        require(value.isFinite()) { "Backup contains an invalid number" }
                        value.toFloat()
                    }
                    is JSONArray -> buildSet {
                        require(value.length() <= MAX_SET_VALUES) {
                            "Backup contains too many list values"
                        }
                        for (index in 0 until value.length()) {
                            val item = value.get(index)
                            require(item is String && item.length <= MAX_SET_VALUE_LENGTH) {
                                "Backup contains an invalid list value"
                            }
                            add(item)
                        }
                    }
                    else -> error("Backup contains an unsupported setting type")
                }
                put(key, parsed)
            }
        }
    }

    private fun replacePreferences(name: String, values: Map<String, Any>): Boolean {
        val editor = context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear()
        values.forEach { (key, value) ->
            when (value) {
                is String -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
                else -> error("Unsupported stored preference type")
            }
        }
        return editor.commit()
    }

    private companion object {
        const val MAX_KEYS_PER_FILE = 2_000
        const val MAX_KEY_LENGTH = 256
        const val MAX_VALUE_LENGTH = 12 * 1_024 * 1_024
        const val MAX_SET_VALUES = 10_000
        const val MAX_SET_VALUE_LENGTH = 4_096
        const val PREFERENCES_FEATURES = "rune_companion_features"
        val PREFERENCE_FILES = listOf(
            "rune_companion_toolkit",
            PREFERENCES_FEATURES,
            "rune_companion_overlay",
            "rune_companion_personalization",
            ActivityProfilePreferences.PREFERENCES_NAME,
            "shooting_star_alerts",
            "shooting_star_filters",
        )
    }
}

internal object BackupCrypto {
    fun requireNewPassphrase(passphrase: String) {
        require(passphrase.length in MIN_NEW_PASSPHRASE_LENGTH..MAX_PASSPHRASE_LENGTH) {
            "Use a passphrase between $MIN_NEW_PASSPHRASE_LENGTH and " +
                "$MAX_PASSPHRASE_LENGTH characters"
        }
    }

    fun requireImportPassphrase(passphrase: String) {
        require(passphrase.length in MIN_LEGACY_PASSPHRASE_LENGTH..MAX_PASSPHRASE_LENGTH) {
            "Enter the backup passphrase"
        }
    }

    fun encrypt(plainText: String, passphrase: String): String {
        requireNewPassphrase(passphrase)
        val plainBytes = plainText.toByteArray(StandardCharsets.UTF_8)
        require(plainBytes.size <= MAX_PLAIN_BYTES) { "Backup is too large to export safely" }
        val salt = ByteArray(CURRENT_SALT_SIZE).also(SecureRandom()::nextBytes)
        val iv = ByteArray(IV_SIZE).also(SecureRandom()::nextBytes)
        return try {
            val cipher = Cipher.getInstance(CIPHER)
            cipher.init(
                Cipher.ENCRYPT_MODE,
                key(passphrase, salt, CURRENT_ITERATIONS),
                GCMParameterSpec(TAG_BITS, iv),
            )
            val encrypted = cipher.doFinal(plainBytes)
            listOf(
                CURRENT_FORMAT,
                encode(salt),
                encode(iv),
                encode(encrypted),
            ).joinToString(".")
        } finally {
            plainBytes.fill(0)
        }
    }

    fun decrypt(payload: String, passphrase: String): String {
        requireImportPassphrase(passphrase)
        require(payload.length <= MAX_PAYLOAD_CHARS) { "Backup is too large to import safely" }
        val parts = payload.trim().split('.', limit = 5)
        require(parts.size == 4) { "This is not a Rune Companion backup" }
        val format = formats[parts[0]]
            ?: throw IllegalArgumentException("This is not a supported Rune Companion backup")
        return try {
            val salt = decode(parts[1])
            val iv = decode(parts[2])
            val encrypted = decode(parts[3])
            require(salt.size == format.saltSize && iv.size == IV_SIZE) {
                "Backup header is invalid"
            }
            require(encrypted.size in (TAG_BITS / 8)..(MAX_PLAIN_BYTES + TAG_BITS / 8)) {
                "Backup payload is invalid"
            }
            val cipher = Cipher.getInstance(CIPHER)
            cipher.init(
                Cipher.DECRYPT_MODE,
                key(passphrase, salt, format.iterations),
                GCMParameterSpec(TAG_BITS, iv),
            )
            val plainBytes = cipher.doFinal(encrypted)
            try {
                require(plainBytes.size <= MAX_PLAIN_BYTES) {
                    "Backup is too large to import safely"
                }
                String(plainBytes, StandardCharsets.UTF_8)
            } finally {
                plainBytes.fill(0)
            }
        } catch (error: GeneralSecurityException) {
            throw IllegalArgumentException(
                "Backup is damaged or the passphrase is incorrect",
                error,
            )
        } catch (error: IllegalArgumentException) {
            if (error.message?.startsWith("Backup ") == true) throw error
            throw IllegalArgumentException("Backup data is invalid", error)
        }
    }

    private fun key(
        passphrase: String,
        salt: ByteArray,
        iterations: Int,
    ): SecretKeySpec {
        val password = passphrase.toCharArray()
        val spec = PBEKeySpec(password, salt, iterations, KEY_BITS)
        return try {
            val encoded = SecretKeyFactory.getInstance(KEY_ALGORITHM)
                .generateSecret(spec)
                .encoded
            try {
                SecretKeySpec(encoded, "AES")
            } finally {
                encoded.fill(0)
            }
        } finally {
            spec.clearPassword()
            password.fill('\u0000')
        }
    }

    private fun encode(value: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value)

    private fun decode(value: String): ByteArray =
        Base64.getUrlDecoder().decode(value)

    private data class Format(
        val iterations: Int,
        val saltSize: Int,
    )

    private const val CURRENT_FORMAT = "RC2"
    private const val CIPHER = "AES/GCM/NoPadding"
    private const val KEY_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val CURRENT_ITERATIONS = 310_000
    private const val LEGACY_ITERATIONS = 150_000
    private const val KEY_BITS = 256
    private const val TAG_BITS = 128
    private const val CURRENT_SALT_SIZE = 32
    private const val LEGACY_SALT_SIZE = 16
    private const val IV_SIZE = 12
    private const val MIN_NEW_PASSPHRASE_LENGTH = 12
    private const val MIN_LEGACY_PASSPHRASE_LENGTH = 8
    private const val MAX_PASSPHRASE_LENGTH = 128
    private const val MAX_PLAIN_BYTES = 12 * 1_024 * 1_024
    private const val MAX_PAYLOAD_CHARS = 20 * 1_024 * 1_024
    private val formats = mapOf(
        CURRENT_FORMAT to Format(CURRENT_ITERATIONS, CURRENT_SALT_SIZE),
        "RC1" to Format(LEGACY_ITERATIONS, LEGACY_SALT_SIZE),
    )
}
