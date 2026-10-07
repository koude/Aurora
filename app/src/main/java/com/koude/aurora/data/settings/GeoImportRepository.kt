package com.koude.aurora.data.settings

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.github.kr328.clash.util.clashDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

sealed interface GeoImportResult {
    data class Imported(val displayName: String) : GeoImportResult
    data object UnsupportedFormat : GeoImportResult
    data object Failed : GeoImportResult
}

interface GeoImportRepository {
    suspend fun import(uri: Uri, kind: String): GeoImportResult
}

val validGeoExtensions = listOf(".metadb", ".db", ".dat", ".mmdb")

fun geoTargetFileName(kind: String, displayName: String): String? {
    val extension = "." + displayName.substringAfterLast(".")
    if (extension !in validGeoExtensions) return null
    val prefix = when (kind) {
        "geoip" -> "geoip"
        "geosite" -> "geosite"
        "country" -> "country"
        "asn" -> "ASN"
        else -> return null
    }
    return prefix + extension
}

class LocalGeoImportRepository(context: Context) : GeoImportRepository {
    private val appContext = context.applicationContext

    override suspend fun import(uri: Uri, kind: String): GeoImportResult = withContext(Dispatchers.IO) {
        try {
            val name = appContext.contentResolver.query(uri, null, null, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (column == -1) "" else cursor.getString(column)
            } ?: return@withContext GeoImportResult.Failed
            val destination = geoTargetFileName(kind, name) ?: return@withContext GeoImportResult.UnsupportedFormat
            val source = appContext.contentResolver.openInputStream(uri) ?: return@withContext GeoImportResult.Failed
            source.use { input ->
                FileOutputStream(File(appContext.clashDir, destination)).use { output -> input.copyTo(output) }
            }
            GeoImportResult.Imported(name)
        } catch (_: Exception) {
            GeoImportResult.Failed
        }
    }
}
