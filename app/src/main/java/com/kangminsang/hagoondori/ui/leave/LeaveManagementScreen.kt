package com.kangminsang.hagoondori.ui.leave

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val TAB_TITLES = listOf("휴가", "전투휴무", "외박", "외출")

/**
 * ③ 출타 관리 (스펙 5.1/5.2절, F7~F14) - 휴가/전투휴무/외박/외출 4개 탭.
 */
@Composable
fun LeaveManagementScreen(
    modifier: Modifier = Modifier,
    viewModel: LeaveManagementViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(modifier = modifier) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                TAB_TITLES.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, style = MaterialTheme.typography.labelLarge) },
                    )
                }
            }

            when (selectedTab) {
                0 -> LeaveTab(
                    uiState = uiState,
                    onAddType = viewModel::addLeaveType,
                    onAddGrant = viewModel::addLeaveGrant,
                    onAddUsage = viewModel::addLeaveUsage,
                    onDeleteUsage = viewModel::deleteLeaveUsage,
                    modifier = Modifier.fillMaxSize(),
                )
                1 -> CombatRestTab(
                    uiState = uiState,
                    onAddGrant = viewModel::addCombatRestGrant,
                    onAddUsage = viewModel::addCombatRestUsage,
                    onDeleteUsage = viewModel::deleteCombatRestUsage,
                    modifier = Modifier.fillMaxSize(),
                )
                2 -> OvernightTab(
                    uiState = uiState,
                    onAddRecord = viewModel::addOvernightRecord,
                    onAddForfeiture = viewModel::addOvernightForfeiture,
                    modifier = Modifier.fillMaxSize(),
                )
                3 -> PassTab(
                    uiState = uiState,
                    onAddRecord = viewModel::addPassRecord,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
