package com.rudi.audioplayer.util

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import java.io.File
import java.security.MessageDigest

data class ApkSignatureResult(
    val fileName: String,
    val packageName: String? = null,
    val versionName: String? = null,
    val sha256: String? = null,
    val error: String? = null
) {
    val isOk: Boolean get() = error == null && sha256 != null
}

/**
 * Reads the signing certificate straight out of an APK file (no installation needed) using
 * PackageManager's own archive-inspection API — the same mechanism Android itself uses to
 * decide whether an update is allowed to install over an existing app. Entirely offline/local;
 * nothing here touches the network.
 */
object ApkSignatureChecker {

    fun inspect(context: Context, apkUri: Uri, displayName: String): ApkSignatureResult {
        var tempFile: File? = null
        return try {
            tempFile = File(context.cacheDir, "sigcheck_${System.currentTimeMillis()}_${displayName.hashCode()}.apk")
            context.contentResolver.openInputStream(apkUri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return ApkSignatureResult(displayName, error = "Tidak bisa membuka file yang dipilih.")

            val pm = context.packageManager
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            }
            val info = pm.getPackageArchiveInfo(tempFile.absolutePath, flags)
                ?: return ApkSignatureResult(displayName, error = "File ini bukan APK yang valid, atau rusak.")

            val signatureBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val signingInfo = info.signingInfo
                if (signingInfo?.hasMultipleSigners() == true) {
                    // Multi-signer apps can't rotate keys — order is irrelevant, any entry
                    // represents "the" current signer set.
                    signingInfo.apkContentsSigners?.firstOrNull()?.toByteArray()
                } else {
                    // signingCertificateHistory is ordered oldest→newest (original cert at
                    // index 0, CURRENT cert at the LAST index) — see SigningInfo docs. Taking
                    // firstOrNull() here silently compared the ORIGINAL signing key instead of
                    // the current one for any app that has ever rotated its signing key, which
                    // could report MATCH/MISMATCH inconsistently with what Android's installer
                    // actually enforces.
                    signingInfo?.signingCertificateHistory?.lastOrNull()?.toByteArray()
                }
            } else {
                @Suppress("DEPRECATION")
                info.signatures?.firstOrNull()?.toByteArray()
            }

            if (signatureBytes == null) {
                return ApkSignatureResult(
                    displayName,
                    info.packageName,
                    info.versionName,
                    error = "APK tidak memiliki signature yang terbaca (tidak ditandatangani, atau rusak)."
                )
            }

            val digest = MessageDigest.getInstance("SHA-256").digest(signatureBytes)
            val hex = digest.joinToString(":") { "%02X".format(it) }

            ApkSignatureResult(
                fileName = displayName,
                packageName = info.packageName,
                versionName = info.versionName,
                sha256 = hex
            )
        } catch (e: Exception) {
            ApkSignatureResult(displayName, error = "${e.javaClass.simpleName}: ${e.message ?: "tidak diketahui"}")
        } finally {
            tempFile?.delete()
        }
    }
}
