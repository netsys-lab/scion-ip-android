package de.ovgu.netsys.scion_ip_translator

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object StatusManager {
    private val _status = MutableStateFlow(Status())
    val statusFlow: Flow<Status> = _status.asStateFlow()

    var status: Status
        get() = _status.value
        set(value) {
            _status.value = value
        }

    fun reset() {
        status = Status()
    }

    @JvmStatic
    fun setTranslationStatus(translationStatus: TranslationStatus) {
        status = status.copy(translationStatus = translationStatus)
    }

    @JvmStatic
    fun updateUploadedBytes(bytes: Long) {
        status = status.copy(uploadedBytes = status.uploadedBytes + bytes)
    }

    @JvmStatic
    fun updateDownloadedBytes(bytes: Long) {
        status = status.copy(downloadedBytes = status.downloadedBytes + bytes)
    }

    @JvmStatic
    fun updateConnectionTime(time: Long) {
        status = status.copy(connectionTime = time)
    }

    @JvmStatic
    fun setIpAddress(ipAddress: String) {
        status = status.copy(ipAddress = ipAddress)
    }

}
