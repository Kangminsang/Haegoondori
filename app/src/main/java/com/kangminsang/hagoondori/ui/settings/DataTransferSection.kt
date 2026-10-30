package com.kangminsang.hagoondori.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kangminsang.hagoondori.data.backup.BackupResult
import com.kangminsang.hagoondori.ui.common.Card

/**
 * 데이터 이전 - 앱 버전이나 기기를 바꿀 때 DB 파일째로 내보내고 가져온다. 가져오기는 현재 데이터를
 * 모두 바꾸므로 파일을 고른 뒤 확인을 한 번 더 받고, 성공하면 앱을 다시 시작한다.
 */
@Composable
fun DataTransferSection(
    onExport: (Uri, (BackupResult) -> Unit) -> Unit,
    onRestore: (Uri, (BackupResult) -> Unit) -> Unit,
    onRestart: () -> Unit,
    defaultFileName: () -> String,
    showMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isBusy by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        if (uri != null) {
            isBusy = true
            onExport(uri) { result ->
                isBusy = false
                showMessage(
                    when (result) {
                        is BackupResult.Success -> "데이터를 내보냈습니다"
                        is BackupResult.Failure -> result.message
                    },
                )
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingRestoreUri = uri
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("데이터 이전", style = MaterialTheme.typography.titleMedium)
            Text(
                "앱 버전이나 기기를 바꿀 때 데이터를 파일로 옮깁니다. 파일에 복무 기록이 암호화 없이 들어 있으니 " +
                    "보관에 주의해 주세요. 공휴일 API 키와 장치 폴더는 파일에 포함되지 않아 새로 설정해야 합니다.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
            OutlinedButton(enabled = !isBusy, onClick = { exportLauncher.launch(defaultFileName()) }) {
                Text("내보내기")
            }
            OutlinedButton(
                enabled = !isBusy,
                onClick = { importLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.padding(top = 8.dp),
            ) { Text("가져오기") }
        }
    }

    pendingRestoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestoreUri = null },
            title = { Text("데이터 가져오기") },
            text = { Text("현재 데이터가 모두 이 파일 내용으로 바뀝니다. 되돌릴 수 없습니다.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingRestoreUri = null
                    isBusy = true
                    onRestore(uri) { result ->
                        when (result) {
                            is BackupResult.Success -> onRestart()
                            is BackupResult.Failure -> {
                                isBusy = false
                                showMessage(result.message)
                                if (result.needsRestart) onRestart()
                            }
                        }
                    }
                }) { Text("가져오기") }
            },
            dismissButton = { TextButton(onClick = { pendingRestoreUri = null }) { Text("취소") } },
        )
    }
}
