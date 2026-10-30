package com.kangminsang.hagoondori.core.backup

/**
 * 가져오려는 DB 파일이 이 앱 버전에서 쓸 수 있는 파일인지 판정한다(데이터 이전). 순수 계산이라
 * 파일을 여는 쪽(SQLite)과 분리해 단위 테스트한다.
 */
object BackupCompatibility {

    /** 버전 1부터 존재하는 테이블들(app/schemas/1.json과 같다). 이 중 하나라도 없으면 이 앱의 DB가 아니다. */
    val REQUIRED_TABLES: Set<String> = setOf(
        "user_profile",
        "leave_type",
        "leave_grant",
        "leave_usage",
        "combat_rest_grant",
        "combat_rest_usage",
        "overnight_record",
        "overnight_forfeiture",
        "pass_record",
        "event",
        "duty_assignment",
        "holiday",
        "sync_state",
    )

    sealed interface Verdict {
        /** 같은 버전이거나 옛 버전이라 마이그레이션으로 올릴 수 있다. */
        data object Ok : Verdict

        /** 파일이 이 앱보다 새 버전에서 만들어졌다. */
        data class TooNew(val fileVersion: Int, val appVersion: Int) : Verdict

        /** 버전 번호가 1 미만이라 이 앱이 만든 파일로 볼 수 없다. */
        data class TooOld(val fileVersion: Int) : Verdict

        /** 필수 테이블이 빠져 있다. */
        data class NotOurDatabase(val missingTables: Set<String>) : Verdict
    }

    /**
     * @param fileVersion 파일의 `PRAGMA user_version`
     * @param appVersion 이 앱의 DB 버전
     * @param tableNames 파일의 `sqlite_master`에 있는 테이블 이름들
     */
    fun judge(fileVersion: Int, appVersion: Int, tableNames: Collection<String>): Verdict {
        if (fileVersion < 1) return Verdict.TooOld(fileVersion)
        if (fileVersion > appVersion) return Verdict.TooNew(fileVersion, appVersion)
        val missing = REQUIRED_TABLES - tableNames.toSet()
        if (missing.isNotEmpty()) return Verdict.NotOurDatabase(missing)
        return Verdict.Ok
    }
}
