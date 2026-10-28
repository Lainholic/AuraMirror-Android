package com.synapse.auramirror

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.coroutines.cancellation.CancellationException

class HealthConnectManager(private val context: Context) {

    val isAvailable: Boolean
        get() = try {
            HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
        } catch (e: Exception) {
            false
        }

    val permissions = setOf(
        HealthPermission.getReadPermission(SleepSessionRecord::class)
    )

    private fun getClient(): HealthConnectClient? {
        return if (isAvailable) {
            try {
                HealthConnectClient.getOrCreate(context)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    suspend fun hasPermissions(): Boolean {
        val client = getClient() ?: return false
        return try {
            val granted = client.permissionController.getGrantedPermissions()
            granted.containsAll(permissions)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }

    suspend fun readLastNightSleep(): SleepReadResult {
        if (!isAvailable) {
            return SleepReadResult.Unavailable
        }

        val client = getClient() ?: return SleepReadResult.Unavailable

        if (!hasPermissions()) {
            return SleepReadResult.PermissionRequired
        }

        val now = Instant.now()
        val yesterday = now.minus(24, ChronoUnit.HOURS)

        return try {
            val allSessions = mutableListOf<SleepSessionRecord>()
            var pageToken: String? = null
            var pagesRead = 0
            do {
                val response = client.readRecords(
                    ReadRecordsRequest(
                        recordType = SleepSessionRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(yesterday, now),
                        ascendingOrder = false,
                        pageToken = pageToken
                    )
                )
                allSessions.addAll(response.records)
                pageToken = response.pageToken
                pagesRead++
            } while (!pageToken.isNullOrEmpty() && pagesRead < 10)

            if (allSessions.isNotEmpty()) {
                val lastSession = allSessions.maxByOrNull { it.endTime } ?: allSessions.first()
                val durationSec = Duration.between(lastSession.startTime, lastSession.endTime).seconds
                val rawHours = durationSec / 3600.0
                val rounded = Math.round(rawHours * 10.0) / 10.0
                SleepReadResult.Success(
                    SleepData(
                        durationHours = rounded,
                        startTimeIso = lastSession.startTime.toString(),
                        endTimeIso = lastSession.endTime.toString(),
                        isDeficit = rounded < 6.5
                    )
                )
            } else {
                SleepReadResult.NoData
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            SleepReadResult.Error(e.message ?: "Failed to read sleep session")
        }
    }
}
