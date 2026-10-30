package com.kangminsang.hagoondori.data.backup

import android.content.Context
import android.content.Intent
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.kangminsang.hagoondori.core.backup.BackupCompatibility
import com.kangminsang.hagoondori.data.local.HagoondoriDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** [DatabaseBackupManager]의 내보내기/가져오기 결과. 실패 메시지는 그대로 사용자에게 보여준다. */
sealed interface BackupResult {
    data object Success : BackupResult

    /**
     * @param needsRestart 이미 Room을 닫은 뒤에 실패해서 이 프로세스의 DB를 더 쓸 수 없다.
     * 호출부는 메시지를 보여준 뒤 [DatabaseBackupManager.restartApp]으로 앱을 다시 시작해야 한다.
     */
    data class Failure(val message: String, val needsRestart: Boolean = false) : BackupResult
}

/**
 * 앱 버전이나 기기를 바꿀 때 데이터를 옮기도록 DB 파일 자체를 내보내고 가져온다(형식은 SQLite 파일).
 * 옛 버전 파일은 가져오는 순간 Room 마이그레이션으로 올라온다. 공휴일 API 키와 장치 폴더 권한은
 * SharedPreferences에 있어 파일에 포함되지 않는다.
 *
 * 가져오기는 현재 DB를 건드리기 전에 임시 파일에서 모든 검증(파일 형식, 무결성, 버전, 실제 Room 열기)을
 * 끝내고, 마지막에만 파일을 바꿔치기한다.
 */
@Singleton
class DatabaseBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: HagoondoriDatabase,
) {

    /** 현재 DB의 일관된 사본을 [uri]에 쓴다. */
    suspend fun export(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        val copy = File(context.cacheDir, EXPORT_COPY_NAME)
        try {
            // VACUUM INTO는 대상 파일이 있으면 실패하므로 앞서 남은 사본을 먼저 지운다.
            copy.delete()
            // VACUUM은 트랜잭션 안에서 실행할 수 없다.
            database.openHelper.writableDatabase.execSQL("VACUUM INTO '${copy.absolutePath.replace("'", "''")}'")
            val output = context.contentResolver.openOutputStream(uri, "wt")
                ?: return@withContext BackupResult.Failure("내보낼 파일을 열 수 없습니다")
            output.use { out -> copy.inputStream().use { it.copyTo(out) } }
            BackupResult.Success
        } catch (e: Exception) {
            BackupResult.Failure("내보내기에 실패했습니다: ${e.message ?: e.javaClass.simpleName}")
        } finally {
            copy.delete()
        }
    }

    /**
     * [uri]의 DB 파일로 현재 데이터를 모두 바꾼다. [BackupResult.Success]이면 파일 교체까지 끝난 것이고,
     * 호출부는 곧바로 [restartApp]을 불러야 한다(닫힌 Room은 더 쓸 수 없다).
     */
    suspend fun restore(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        val staging = context.getDatabasePath(STAGING_NAME)
        try {
            deleteWithSidecars(staging)
            val input = context.contentResolver.openInputStream(uri)
                ?: return@withContext BackupResult.Failure("선택한 파일을 열 수 없습니다")
            input.use { src -> staging.outputStream().use { src.copyTo(it) } }

            validate(staging)?.let {
                deleteWithSidecars(staging)
                return@withContext it
            }
        } catch (e: Exception) {
            deleteWithSidecars(staging)
            return@withContext BackupResult.Failure("파일을 읽는 중 문제가 생겼습니다: ${e.message ?: e.javaClass.simpleName}")
        }
        swapIn(staging)
    }

    /** 앱을 처음부터 다시 시작한다. 닫힌 Room을 쓰는 코드가 더 돌지 않도록 프로세스를 끝낸다. */
    fun restartApp() {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(launch)
        }
        Runtime.getRuntime().exit(0)
    }

    /** 문제가 있으면 사용자용 실패를, 쓸 수 있는 파일이면 null을 반환한다. */
    private fun validate(file: File): BackupResult.Failure? {
        val header = ByteArray(SQLITE_HEADER.size)
        val read = file.inputStream().use { it.read(header) }
        if (read != header.size || !header.contentEquals(SQLITE_HEADER)) {
            return BackupResult.Failure("이 앱에서 내보낸 데이터 파일이 아닙니다")
        }

        val verdict = try {
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                if (DatabaseUtils.stringForQuery(db, "PRAGMA integrity_check", null) != "ok") {
                    return BackupResult.Failure("파일이 손상되어 있습니다")
                }
                val tables = db.rawQuery("SELECT name FROM sqlite_master WHERE type = 'table'", null).use { c ->
                    buildList { while (c.moveToNext()) add(c.getString(0)) }
                }
                BackupCompatibility.judge(db.version, HagoondoriDatabase.VERSION, tables)
            }
        } catch (e: Exception) {
            return BackupResult.Failure("파일이 손상되었거나 읽을 수 없습니다")
        }
        when (verdict) {
            is BackupCompatibility.Verdict.Ok -> Unit
            is BackupCompatibility.Verdict.TooNew ->
                return BackupResult.Failure("더 새로운 버전 앱에서 내보낸 파일입니다. 앱을 최신 버전으로 업데이트한 뒤 다시 시도해 주세요")
            is BackupCompatibility.Verdict.TooOld ->
                return BackupResult.Failure("너무 오래된 파일이라 가져올 수 없습니다")
            is BackupCompatibility.Verdict.NotOurDatabase ->
                return BackupResult.Failure("이 앱의 데이터 파일이 아닙니다")
        }

        // 마이그레이션과 스키마 검증은 실제로 Room으로 열어 봐야 끝난다.
        return try {
            val staged = HagoondoriDatabase.build(context, file.name)
            try {
                staged.openHelper.writableDatabase.query("SELECT COUNT(*) FROM user_profile").use { it.moveToFirst() }
            } finally {
                staged.close()
            }
            null
        } catch (e: Exception) {
            BackupResult.Failure("데이터를 현재 버전으로 변환하지 못했습니다: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    /** 현재 DB를 치우고 [staging]을 그 자리에 둔다. 실패하면 원래 DB를 되돌린다. */
    private fun swapIn(staging: File): BackupResult {
        val live = context.getDatabasePath(HagoondoriDatabase.DATABASE_NAME)
        val backup = context.getDatabasePath(PRE_RESTORE_NAME)
        // 이 아래부터는 Room이 닫혀 있어 어떤 DB 접근도 하지 않는다.
        database.close()
        return try {
            backup.delete()
            if (live.exists() && !live.renameTo(backup)) throw IllegalStateException("현재 데이터를 옮기지 못했습니다")
            deleteSidecars(live)
            if (!staging.renameTo(live)) throw IllegalStateException("가져온 데이터를 적용하지 못했습니다")
            deleteSidecars(staging)
            backup.delete()
            BackupResult.Success
        } catch (e: Exception) {
            // 현재 DB를 옮기기 전에 실패했다면 backup이 없고 live가 원본이므로 지우면 안 된다.
            if (backup.exists()) {
                live.delete()
                backup.renameTo(live)
            }
            deleteWithSidecars(staging)
            BackupResult.Failure(
                "가져오기에 실패해 이전 데이터를 그대로 두었습니다. 앱을 다시 시작합니다: ${e.message ?: e.javaClass.simpleName}",
                needsRestart = true,
            )
        }
    }

    private fun deleteSidecars(file: File) {
        File(file.path + "-wal").delete()
        File(file.path + "-shm").delete()
    }

    private fun deleteWithSidecars(file: File) {
        file.delete()
        deleteSidecars(file)
    }

    private companion object {
        const val EXPORT_COPY_NAME = "backup_export.db"
        const val STAGING_NAME = "restore_staging.db"
        const val PRE_RESTORE_NAME = "hagoondori.db.pre_restore"
        val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
    }
}
