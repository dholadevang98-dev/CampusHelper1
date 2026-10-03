package com.devang.campushelper

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object NoticeDownloadUtil {

    private const val TAG = "NoticeDownloadUtil"

    val CIRCULAR_FILES = listOf(
        "Sem5 - Mid Sem Result.pdf",
        "GTU_National_Seminar.pdf",
        "20260917191103-92cb7308c6.pdf"
    )

    data class NoticeModel(
        var fileName: String,
        var title: String,
        var tag: String,
        var tagColorRes: Int,
        var dateText: String,
        var description: String,
        var customFilePath: String? = null
    )

    val NOTICES_LIST = mutableListOf(
        NoticeModel(
            fileName = "Sem5 - Mid Sem Result.pdf",
            title = "Semester 5 Mid-Sem Exam Marks / Results",
            tag = "MID-SEM RESULT",
            tagColorRes = R.color.accent_rose,
            dateText = "September 2026",
            description = "Government Polytechnic Rajkot IT Department Semester 5-A, 5-B, and 5-C official marksheet."
        ),
        NoticeModel(
            fileName = "GTU_National_Seminar.pdf",
            title = "GTU National Seminar: AI & Quantum Frontiers",
            tag = "SEMINAR & RESEARCH",
            tagColorRes = R.color.accent_cyan,
            dateText = "30 Sep - 1 Oct 2026",
            description = "National seminar at GTU Chandkheda campus on AI, Quantum Computing, and Research Innovations with KCG."
        ),
        NoticeModel(
            fileName = "20260917191103-92cb7308c6.pdf",
            title = "GTU Remedial Exam Forms Circular (Winter 2026)",
            tag = "GTU CIRCULAR",
            tagColorRes = R.color.accent_amber,
            dateText = "17 Sep 2026",
            description = "Official GTU instructions & schedule for filling Diploma Engineering Sem 1 & 2 exam forms."
        )
    )

    fun addNotice(notice: NoticeModel) {
        NOTICES_LIST.add(0, notice)
    }

    fun removeNotice(index: Int): Boolean {
        if (index in 0 until NOTICES_LIST.size) {
            NOTICES_LIST.removeAt(index)
            return true
        }
        return false
    }

    /**
     * Opens an individual PDF notice using the system PDF viewer via FileProvider.
     */
    fun openCircularPdf(context: Context, fileName: String, displayName: String = fileName, customFilePath: String? = null) {
        try {
            val destFile: File
            if (customFilePath != null && File(customFilePath).exists() && File(customFilePath).length() > 0L) {
                destFile = File(customFilePath)
            } else {
                val directCheck = File(context.filesDir, fileName)
                if (directCheck.exists() && directCheck.length() > 0L) {
                    destFile = directCheck
                } else {
                    val uploadedCheck = File(context.filesDir, "notices_uploaded/$fileName")
                    if (uploadedCheck.exists() && uploadedCheck.length() > 0L) {
                        destFile = uploadedCheck
                    } else {
                        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
                        destFile = File(downloadDir, fileName)

                        // Ensure destination file is refreshed from assets with the authentic PDF bytes
                        val assetPath = "notices/$fileName"
                        try {
                            context.assets.open(assetPath).use { input ->
                                val assetSize = input.available().toLong()
                                if (!destFile.exists() || destFile.length() != assetSize || destFile.length() == 0L) {
                                    FileOutputStream(destFile).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Asset notice not found: $fileName", e)
                        }
                    }
                }
            }

            if (!destFile.exists() || destFile.length() == 0L) {
                Toast.makeText(context, "PDF file not available: $displayName", Toast.LENGTH_SHORT).show()
                return
            }

            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                destFile
            )

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(fileUri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val resInfoList = context.packageManager.queryIntentActivities(viewIntent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                context.grantUriPermission(packageName, fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(viewIntent, "Open $displayName")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            Toast.makeText(context, "Opening $displayName 📄", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error opening circular PDF: $fileName", e)
            Toast.makeText(context, "Could not open $displayName: ${e.localizedMessage ?: "No PDF viewer found"}", Toast.LENGTH_SHORT).show()
        }
    }

    sealed class DownloadResult {
        object AllAlreadyDownloaded : DownloadResult()
        data class Success(val downloadedCount: Int, val savedFiles: List<String>) : DownloadResult()
        data class PartialFailure(val successCount: Int, val failedFiles: List<String>) : DownloadResult()
        data class AllFailed(val failedFiles: List<String>) : DownloadResult()
    }

    /**
     * Checks if all 3 circular PDFs have already been downloaded to the app downloads directory.
     */
    fun areAllCircularsAlreadyDownloaded(context: Context): Boolean {
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return false
        return CIRCULAR_FILES.all { fileName ->
            val file = File(downloadDir, fileName)
            val assetSize = try {
                context.assets.open("notices/$fileName").use { it.available().toLong() }
            } catch (e: Exception) {
                0L
            }
            file.exists() && file.length() == assetSize && file.length() > 0
        }
    }

    /**
     * Downloads and saves all 3 official PDF circulars from assets to accessible storage.
     */
    fun downloadAllCirculars(context: Context): DownloadResult {
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (downloadDir != null && areAllCircularsAlreadyDownloaded(context)) {
            return DownloadResult.AllAlreadyDownloaded
        }

        val failedFiles = mutableListOf<String>()
        val successfulFiles = mutableListOf<String>()

        for (fileName in CIRCULAR_FILES) {
            val assetPath = "notices/$fileName"
            try {
                // 1. Verify asset exists
                val assetInputStream = try {
                    context.assets.open(assetPath)
                } catch (e: IOException) {
                    Log.e(TAG, "Asset not found: $assetPath", e)
                    failedFiles.add(fileName)
                    continue
                }

                // 2. Save to App External Storage (Always accessible & scoped)
                if (downloadDir != null) {
                    val destFile = File(downloadDir, fileName)
                    FileOutputStream(destFile).use { output ->
                        assetInputStream.use { input ->
                            input.copyTo(output)
                        }
                    }
                }

                // 3. Save to Public Downloads via MediaStore on Android 10+ (API 29+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/CampusHelper_Notices")
                            put(MediaStore.MediaColumns.IS_PENDING, 1)
                        }
                        val resolver = context.contentResolver
                        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                        if (uri != null) {
                            context.assets.open(assetPath).use { input ->
                                resolver.openOutputStream(uri)?.use { output ->
                                    input.copyTo(output)
                                }
                            }
                            contentValues.clear()
                            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                            resolver.update(uri, contentValues, null, null)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "MediaStore save skipped/fallback for $fileName: ${e.message}")
                    }
                }

                successfulFiles.add(fileName)
            } catch (e: Exception) {
                Log.e(TAG, "Error copying circular PDF: $fileName", e)
                failedFiles.add(fileName)
            }
        }

        return when {
            failedFiles.isEmpty() && successfulFiles.size == CIRCULAR_FILES.size -> {
                DownloadResult.Success(successfulFiles.size, successfulFiles)
            }
            successfulFiles.isNotEmpty() -> {
                DownloadResult.PartialFailure(successfulFiles.size, failedFiles)
            }
            else -> {
                DownloadResult.AllFailed(failedFiles)
            }
        }
    }
}
