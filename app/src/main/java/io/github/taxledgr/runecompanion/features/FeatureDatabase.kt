package io.github.taxledgr.runecompanion.features

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal data class FeatureDatabaseSnapshot(
    val current: String?,
    val recovery: String?,
    val lastSavedAtEpochMillis: Long?,
    val sizeBytes: Long,
)

/**
 * Transactional storage for the large structured companion state.
 *
 * SharedPreferences remains a compatibility mirror for encrypted exports and
 * Android device transfer. The database is the primary runtime store.
 */
internal class FeatureDatabase(
    private val context: Context,
) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    init {
        setWriteAheadLoggingEnabled(true)
    }

    override fun onCreate(database: SQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE feature_state (
                slot TEXT NOT NULL PRIMARY KEY,
                payload TEXT NOT NULL,
                saved_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }

    override fun onUpgrade(
        database: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int,
    ) = Unit

    fun snapshot(): FeatureDatabaseSnapshot {
        var current: String? = null
        var recovery: String? = null
        var savedAt: Long? = null
        readableDatabase.query(
            TABLE,
            arrayOf(COLUMN_SLOT, COLUMN_PAYLOAD, COLUMN_SAVED_AT),
            null,
            null,
            null,
            null,
            null,
        ).use { cursor ->
            val slotIndex = cursor.getColumnIndexOrThrow(COLUMN_SLOT)
            val payloadIndex = cursor.getColumnIndexOrThrow(COLUMN_PAYLOAD)
            val savedIndex = cursor.getColumnIndexOrThrow(COLUMN_SAVED_AT)
            while (cursor.moveToNext()) {
                when (cursor.getString(slotIndex)) {
                    SLOT_CURRENT -> {
                        current = cursor.getString(payloadIndex)
                        savedAt = cursor.getLong(savedIndex)
                    }
                    SLOT_RECOVERY -> recovery = cursor.getString(payloadIndex)
                }
            }
        }
        return FeatureDatabaseSnapshot(
            current = current,
            recovery = recovery,
            lastSavedAtEpochMillis = savedAt,
            sizeBytes = databaseFiles().sumOf { file ->
                file.takeIf { it.exists() }?.length() ?: 0L
            },
        )
    }

    fun save(payload: String, savedAtEpochMillis: Long = System.currentTimeMillis()) {
        require(payload.length <= MAX_PAYLOAD_CHARS) { "Companion data is too large" }
        writableDatabase.inTransaction {
            queryPayload(SLOT_CURRENT)?.takeIf { it != payload }?.let { previous ->
                put(SLOT_RECOVERY, previous, savedAtEpochMillis)
            }
            put(SLOT_CURRENT, payload, savedAtEpochMillis)
        }
    }

    fun replace(
        current: String,
        recovery: String?,
        savedAtEpochMillis: Long = System.currentTimeMillis(),
    ) {
        require(current.length <= MAX_PAYLOAD_CHARS) { "Companion data is too large" }
        writableDatabase.inTransaction {
            delete(TABLE, null, null)
            put(SLOT_CURRENT, current, savedAtEpochMillis)
            recovery
                ?.takeIf { it != current && it.length <= MAX_PAYLOAD_CHARS }
                ?.let { put(SLOT_RECOVERY, it, savedAtEpochMillis) }
        }
    }

    private fun SQLiteDatabase.queryPayload(slot: String): String? =
        query(
            TABLE,
            arrayOf(COLUMN_PAYLOAD),
            "$COLUMN_SLOT = ?",
            arrayOf(slot),
            null,
            null,
            null,
            "1",
        ).use { cursor ->
            cursor.takeIf { it.moveToFirst() }?.getString(0)
        }

    private fun SQLiteDatabase.put(
        slot: String,
        payload: String,
        savedAtEpochMillis: Long,
    ) {
        val values = ContentValues().apply {
            put(COLUMN_SLOT, slot)
            put(COLUMN_PAYLOAD, payload)
            put(COLUMN_SAVED_AT, savedAtEpochMillis)
        }
        check(
            insertWithOnConflict(
                TABLE,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE,
            ) != -1L,
        ) { "Android could not save companion data" }
    }

    private inline fun SQLiteDatabase.inTransaction(block: SQLiteDatabase.() -> Unit) {
        beginTransaction()
        try {
            block()
            setTransactionSuccessful()
        } finally {
            endTransaction()
        }
    }

    private fun databaseFiles() = listOf(
        context.getDatabasePath(DATABASE_NAME),
        context.getDatabasePath("$DATABASE_NAME-wal"),
        context.getDatabasePath("$DATABASE_NAME-shm"),
    )

    private companion object {
        const val DATABASE_NAME = "rune_companion.db"
        const val DATABASE_VERSION = 1
        const val TABLE = "feature_state"
        const val COLUMN_SLOT = "slot"
        const val COLUMN_PAYLOAD = "payload"
        const val COLUMN_SAVED_AT = "saved_at"
        const val SLOT_CURRENT = "current"
        const val SLOT_RECOVERY = "recovery"
        const val MAX_PAYLOAD_CHARS = 12 * 1_024 * 1_024
    }
}
