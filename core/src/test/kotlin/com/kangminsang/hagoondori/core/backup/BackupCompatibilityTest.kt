package com.kangminsang.hagoondori.core.backup

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BackupCompatibilityTest {

    private val allTables = BackupCompatibility.REQUIRED_TABLES.toList() + listOf("sync_change_log", "room_master_table")

    @Test
    fun `same version with all tables is ok`() {
        assertEquals(BackupCompatibility.Verdict.Ok, BackupCompatibility.judge(7, 7, allTables))
    }

    @Test
    fun `older version is ok so migrations can lift it`() {
        assertEquals(BackupCompatibility.Verdict.Ok, BackupCompatibility.judge(3, 7, BackupCompatibility.REQUIRED_TABLES))
    }

    @Test
    fun `newer version than the app is too new`() {
        assertEquals(BackupCompatibility.Verdict.TooNew(8, 7), BackupCompatibility.judge(8, 7, allTables))
    }

    @Test
    fun `version below 1 is too old`() {
        assertEquals(BackupCompatibility.Verdict.TooOld(0), BackupCompatibility.judge(0, 7, allTables))
    }

    @Test
    fun `missing required table is not our database`() {
        val verdict = BackupCompatibility.judge(7, 7, allTables - "holiday")
        assertTrue(verdict is BackupCompatibility.Verdict.NotOurDatabase)
        assertEquals(setOf("holiday"), (verdict as BackupCompatibility.Verdict.NotOurDatabase).missingTables)
    }
}
