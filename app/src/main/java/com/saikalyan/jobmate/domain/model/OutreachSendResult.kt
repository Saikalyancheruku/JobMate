package com.saikalyan.jobmate.domain.model

sealed interface OutreachSendResult {
    data object Sent : OutreachSendResult
    data class Failed(val reasonCode: String) : OutreachSendResult
    data class Unknown(val reasonCode: String) : OutreachSendResult
    data object UnknownRetryNeedsConfirmation : OutreachSendResult
}
