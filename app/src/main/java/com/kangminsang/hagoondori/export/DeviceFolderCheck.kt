package com.kangminsang.hagoondori.export

/**
 * 사용자가 고른 SAF 폴더가 장치(외부 USB 저장소)의 루트인지 판정한다. 장치는 루트에
 * `calendar.txt`만 두는 계약이므로(펌웨어 스펙 8절), 내장 저장소나 하위 폴더를 고르면
 * 쓰기는 성공해도 장치는 파일을 읽지 못한다.
 *
 * [treeDocumentId]는 `DocumentsContract.getTreeDocumentId(uri)` 값이다. 외부 볼륨 루트는
 * `1A2B-3C4D:`, 내장 저장소는 `primary:...`, 하위 폴더는 `:` 뒤에 경로가 붙는다.
 */
object DeviceFolderCheck {
    sealed class Result {
        data object Ok : Result()
        data class Rejected(val message: String) : Result()
    }

    fun evaluate(treeDocumentId: String): Result {
        val volume = treeDocumentId.substringBefore(':')
        val path = treeDocumentId.substringAfter(':', missingDelimiterValue = "")
        return when {
            volume == "primary" || volume == "home" ->
                Result.Rejected("휴대폰 내장 저장소입니다. 장치(USB 드라이브)의 최상위 폴더를 선택해 주세요.")
            path.isNotEmpty() ->
                Result.Rejected("장치의 하위 폴더입니다. 장치의 최상위(루트) 폴더를 선택해 주세요.")
            else -> Result.Ok
        }
    }
}

/** 설정에 저장된 장치 폴더의 상태. */
enum class DeviceFolderState {
    /** 한 번도 고르지 않았다. */
    NotSelected,

    /** 고른 적은 있으나, USB를 뽑았다 꽂는 등으로 시스템이 접근 권한을 회수했다. */
    PermissionLost,

    /** 권한이 살아 있다. */
    Ready,
}
