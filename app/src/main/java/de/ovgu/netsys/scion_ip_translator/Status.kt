package de.ovgu.netsys.scion_ip_translator


enum class TranslationStatus {
    CONNECTING,
    CONNECTED,
    DISCONNECTED
}

data class Status(
    var translationStatus: TranslationStatus = TranslationStatus.DISCONNECTED,
    var uploadedBytes: Long = 0,
    var downloadedBytes: Long = 0,
    var connectionTime: Long = 0,
    var ipAddress: String = ""
)
