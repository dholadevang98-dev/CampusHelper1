package com.devang.campushelper

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object PdfGeneratorUtil {

    private const val PAGE_WIDTH = 595 // A4 standard width in points (72 dpi)
    private const val PAGE_HEIGHT = 842 // A4 standard height in points (72 dpi)
    private const val MARGIN = 40f

    fun getSyllabusAssetFileName(subjectCode: String): String? {
        return when (subjectCode) {
            "DI05016011" -> "GTU_Syllabus_DI05016011_Artificial_Intelligence_with_Prompt_Engineering.pdf"
            "DI05016021" -> "GTU_Syllabus_DI05016021_AI_Product_Design.pdf"
            "DI05016031" -> "GTU_Syllabus_DI05016031_Cloud_and_Data_Center_Technology.pdf"
            "DI05016061" -> "GTU_Syllabus_DI05016061_Structured_Programming_with_C.pdf"
            "DI05016081" -> "GTU_Syllabus_DI05016081_Emotional_Intelligence_Digital_Wellbeing.pdf"
            else -> null
        }
    }

    /**
     * Retrieves or extracts the authentic GTU Syllabus PDF for the given subject
     */
    fun getSyllabusPdfFile(context: Context, subject: CampusSearchManager.SyllabusSubject): File {
        val assetFileName = getSyllabusAssetFileName(subject.code)
        val targetFileName = assetFileName ?: "GTU_Syllabus_${subject.code}_${subject.name.replace(" ", "_")}.pdf"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), targetFileName)

        var extractedFromAssets = false
        if (assetFileName != null) {
            try {
                context.assets.open(assetFileName).use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                extractedFromAssets = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (!extractedFromAssets || !file.exists() || file.length() == 0L) {
            generateFallbackSyllabusPdf(subject, file)
        }
        return file
    }

    /**
     * Opens or downloads the authentic GTU Syllabus PDF for the given subject
     */
    fun openOrDownloadSyllabusPdf(context: Context, subject: CampusSearchManager.SyllabusSubject) {
        try {
            val file = getSyllabusPdfFile(context, subject)

            val fileUri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(fileUri, "application/pdf")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            val chooser = Intent.createChooser(viewIntent, "Open ${subject.name} Syllabus PDF")
            chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(chooser)
            Toast.makeText(context, "Official GTU Syllabus PDF opened: ${file.name} 📥", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Could not open Syllabus PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Shares the authentic GTU Syllabus PDF for the given subject
     */
    fun shareSyllabusPdf(context: Context, subject: CampusSearchManager.SyllabusSubject) {
        try {
            val file = getSyllabusPdfFile(context, subject)
            val fileUri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "GTU Syllabus: ${subject.name} (${subject.code})")
                putExtra(Intent.EXTRA_TEXT, "Official GTU Diploma IT Sem 5 Syllabus for ${subject.name} (${subject.code}) w.e.f. 2026-27.")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            val chooser = Intent.createChooser(shareIntent, "Share Syllabus PDF")
            chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Could not share Syllabus PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateFallbackSyllabusPdf(subject: CampusSearchManager.SyllabusSubject, destinationFile: File) {
        try {
            val pdfDocument = PdfDocument()
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            var currentPageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas
            var yPos = 50f

            fun drawHeader() {
                // GTU Header Bar
                paint.color = Color.rgb(18, 60, 105) // Navy GTU Header
                paint.textSize = 14f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText("GUJARAT TECHNOLOGICAL UNIVERSITY", PAGE_WIDTH / 2f, yPos, paint)
                yPos += 18f

                paint.color = Color.rgb(100, 116, 139)
                paint.textSize = 10f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText("Diploma Engineering • Branch: Information Technology (Sem 5)", PAGE_WIDTH / 2f, yPos, paint)
                yPos += 16f

                paint.color = Color.rgb(220, 38, 38)
                paint.textSize = 12f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("Subject: ${subject.name} (${subject.code})", PAGE_WIDTH / 2f, yPos, paint)
                yPos += 18f

                // Horizontal Rule
                paint.color = Color.rgb(203, 213, 225)
                paint.strokeWidth = 1f
                canvas.drawLine(MARGIN, yPos, PAGE_WIDTH - MARGIN, yPos, paint)
                yPos += 20f

                paint.textAlign = Paint.Align.LEFT
            }

            fun checkNewPage(neededHeight: Float) {
                if (yPos + neededHeight > PAGE_HEIGHT - 60f) {
                    // Page Footer
                    paint.color = Color.rgb(148, 163, 184)
                    paint.textSize = 9f
                    paint.textAlign = Paint.Align.RIGHT
                    canvas.drawText("Page $currentPageNumber • GTU Syllabus w.e.f. 2026-27", PAGE_WIDTH - MARGIN, PAGE_HEIGHT - 30f, paint)
                    paint.textAlign = Paint.Align.LEFT

                    pdfDocument.finishPage(page)
                    currentPageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    yPos = 50f
                    drawHeader()
                }
            }

            drawHeader()

            // Metadata Box
            paint.color = Color.rgb(241, 245, 249)
            canvas.drawRoundRect(MARGIN, yPos, PAGE_WIDTH - MARGIN, yPos + 64f, 8f, 8f, paint)

            paint.color = Color.rgb(15, 23, 42)
            paint.textSize = 9.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Category: ${subject.category} | Credits: ${subject.credits} (L:${subject.lectureHours}, T:${subject.tutorialHours}, PR:${subject.practicalHours})", MARGIN + 14f, yPos + 20f, paint)
            canvas.drawText("Total Marks: ${subject.totalMarks} (Theory ESE: ${subject.theoryEseMarks}, PA: ${subject.theoryPaMarks} | Practical PA: ${subject.practicalPaMarks}, ESE: ${subject.practicalEseMarks})", MARGIN + 14f, yPos + 38f, paint)
            canvas.drawText("Academic Year: ${subject.academicYear} | Effective Term: 2026-27", MARGIN + 14f, yPos + 54f, paint)
            yPos += 80f

            // Course Outcomes Section
            checkNewPage(40f)
            paint.color = Color.rgb(30, 41, 59)
            paint.textSize = 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("1. COURSE OUTCOMES (COs)", MARGIN, yPos, paint)
            yPos += 16f

            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            for (co in subject.courseOutcomes) {
                checkNewPage(18f)
                paint.color = Color.rgb(51, 65, 85)
                canvas.drawText("• [${co.id}] ${co.description} (${co.rbtLevel})", MARGIN + 8f, yPos, paint)
                yPos += 15f
            }
            yPos += 12f

            // Course Content / Units
            checkNewPage(40f)
            paint.color = Color.rgb(30, 41, 59)
            paint.textSize = 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("2. COURSE CONTENT & UNITS", MARGIN, yPos, paint)
            yPos += 16f

            for (unit in subject.units) {
                checkNewPage(30f)
                paint.color = Color.rgb(15, 23, 42)
                paint.textSize = 9.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("Unit ${unit.unitNo}: ${unit.title} (${unit.hours} Hours, ${unit.weightagePercent}% Weightage)", MARGIN + 6f, yPos, paint)
                yPos += 14f

                paint.textSize = 8.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = Color.rgb(71, 85, 105)
                for (topic in unit.topics) {
                    checkNewPage(16f)
                    canvas.drawText("   $topic", MARGIN + 12f, yPos, paint)
                    yPos += 13f
                }
                yPos += 6f
            }

            // Practical List
            if (subject.suggestedPracticals.isNotEmpty()) {
                checkNewPage(40f)
                paint.color = Color.rgb(30, 41, 59)
                paint.textSize = 11f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("3. SUGGESTED PRACTICAL LIST (PrOs)", MARGIN, yPos, paint)
                yPos += 16f

                paint.textSize = 8.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = Color.rgb(51, 65, 85)
                for ((idx, pr) in subject.suggestedPracticals.withIndex()) {
                    checkNewPage(16f)
                    canvas.drawText("${idx + 1}. $pr", MARGIN + 8f, yPos, paint)
                    yPos += 14f
                }
                yPos += 10f
            }

            // References & Books
            if (subject.books.isNotEmpty()) {
                checkNewPage(40f)
                paint.color = Color.rgb(30, 41, 59)
                paint.textSize = 11f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("4. REFERENCES & LEARNING RESOURCES", MARGIN, yPos, paint)
                yPos += 16f

                paint.textSize = 8.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = Color.rgb(51, 65, 85)
                for (b in subject.books) {
                    checkNewPage(16f)
                    canvas.drawText("${b.srNo}. ${b.title} by ${b.author} (${b.publication})", MARGIN + 8f, yPos, paint)
                    yPos += 14f
                }
                yPos += 10f
            }

            // Sample Projects
            if (subject.sampleProjects.isNotEmpty()) {
                checkNewPage(40f)
                paint.color = Color.rgb(30, 41, 59)
                paint.textSize = 11f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("5. SAMPLE MICRO-PROJECTS", MARGIN, yPos, paint)
                yPos += 16f

                paint.textSize = 8.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = Color.rgb(51, 65, 85)
                for ((idx, sp) in subject.sampleProjects.withIndex()) {
                    checkNewPage(16f)
                    canvas.drawText("${idx + 1}. $sp", MARGIN + 8f, yPos, paint)
                    yPos += 14f
                }
            }

            // Last page footer
            paint.color = Color.rgb(148, 163, 184)
            paint.textSize = 9f
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Page $currentPageNumber • GTU Syllabus w.e.f. 2026-27", PAGE_WIDTH - MARGIN, PAGE_HEIGHT - 30f, paint)

            pdfDocument.finishPage(page)

            val outputStream = FileOutputStream(destinationFile)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    const val TIMETABLE_FILE_NAME = "GP_Rajkot_IT_Master_TimeTable_2026-27.pdf"

    /**
     * Retrieves or extracts the authentic GP Rajkot IT Department Master Time Table PDF
     */
    fun getTimeTablePdfFile(context: Context): File {
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), TIMETABLE_FILE_NAME)
        var extractedFromAssets = false
        try {
            context.assets.open(TIMETABLE_FILE_NAME).use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            extractedFromAssets = true
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (!extractedFromAssets || !file.exists() || file.length() == 0L) {
            generateFallbackTimeTablePdf(file)
        }
        return file
    }

    /**
     * Opens or downloads the official GP Rajkot IT Department Master Time Table PDF (Term Odd 2026-27)
     */
    fun openOrDownloadTimeTablePdf(context: Context) {
        try {
            val file = getTimeTablePdfFile(context)

            // Launch Intent via FileProvider
            val fileUri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(fileUri, "application/pdf")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            val chooser = Intent.createChooser(viewIntent, "Open GP Rajkot Master Timetable PDF")
            chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(chooser)
            Toast.makeText(context, "Master Timetable PDF ready & opened: ${file.name} 📥", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Could not open Time Table PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Shares the official GP Rajkot IT Department Master Time Table PDF
     */
    fun shareTimeTablePdf(context: Context) {
        try {
            val file = getTimeTablePdfFile(context)
            val fileUri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "GP Rajkot IT Department Master Time Table (Term Odd 2026-27)")
                putExtra(Intent.EXTRA_TEXT, "Official IT Department Master Time Table (Term Odd 2026-27) • Government Polytechnic Rajkot.")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            val chooser = Intent.createChooser(shareIntent, "Share Master Timetable PDF")
            chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Could not share PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateFallbackTimeTablePdf(destinationFile: File) {
        try {
            val pdfDocument = PdfDocument()
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            paint.color = Color.WHITE
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), paint)

            var yPos = 35f

            paint.color = Color.rgb(15, 23, 42)
            paint.textSize = 13f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("GOVERNMENT POLYTECHNIC, RAJKOT", PAGE_WIDTH / 2f, yPos, paint)
            yPos += 16f

            paint.color = Color.rgb(71, 85, 105)
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Information Technology Department • Master Time Table (Term Odd 2026-27)", PAGE_WIDTH / 2f, yPos, paint)
            yPos += 14f

            paint.textSize = 8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("TERM: SEM 5: 15/06/2026 To 30/10/2026 | SEM 3: 13/07/2026 To 03/12/2026 | WEF: 07/08/2026", PAGE_WIDTH / 2f, yPos, paint)
            yPos += 18f

            paint.color = Color.rgb(30, 58, 138)
            canvas.drawRect(MARGIN, yPos, PAGE_WIDTH - MARGIN, yPos + 22f, paint)

            paint.color = Color.WHITE
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("DAY", MARGIN + 6f, yPos + 14f, paint)
            canvas.drawText("SEM 5-A (Room 101)", MARGIN + 70f, yPos + 14f, paint)
            canvas.drawText("SEM 5-B (Room 102)", MARGIN + 220f, yPos + 14f, paint)
            canvas.drawText("SEM 5-C (Room 103)", MARGIN + 370f, yPos + 14f, paint)
            yPos += 24f

            val days = listOf(
                "MONDAY" to listOf(
                    "11-1: AIPD (DMT) | 1:45-3:45: CDCT (SRT)\n4-6: AIWPE Lab APL-2",
                    "11-1: CDCT (SVN) / AIWPE (HKV)\n1:45-3:45: SPC (MTV/HKV) | Min Pro",
                    "11-1: AIWPE (HKV) / CDCT (SVN)\n1:45-3:45: AIPD Lab APL-2 | EIDW"
                ),
                "TUESDAY" to listOf(
                    "11-1: SPC Lab APL-1\n1:45-3:45: SPC (MTV/SBP) | Min Pro",
                    "11-1: AIPD Lab APL-2\n1:45-3:45: CDCT Lab APL-2 | EIDW",
                    "11-1: AIWPE (SJS) | 1:45-3:45: SPC (SBP) / AIPD (GJB)"
                ),
                "WEDNESDAY" to listOf(
                    "11-1: AIPD Lab APL-1 | 1:45-3:45: AIWPE (SJS)\n4-6: Min Pro 107",
                    "11-1: CDCT (SRT) | 1:45-3:45: SPC Lab APL-2\n4-6: EIDW Lab BPL-1",
                    "11-1: AIWPE Lab BPL-1\n1:45-3:45: AIPD (DMT) | 4-6: EIDW Lab"
                ),
                "THURSDAY" to listOf(
                    "11-1: SPC (HKV) / AIWPE (HKV)\n1:45-3:45: CDCT (SVN) / AIPD (GJB)",
                    "11-1: AIPD (GJB) / SPC (SBP)\n1:45-3:45: AIWPE (SJS) | Min Pro",
                    "11-1: CDCT Lab APL-2\n1:45-3:45: SPC Lab APL-1 | Min Pro"
                ),
                "FRIDAY" to listOf(
                    "11-1: EIDW Lab APL-1\n1:45-3:45: CDCT Lab APL-2",
                    "11-1: AIPD (DMT) | 1:45-3:45: AIWPE Lab BPL-1\n4-6: EIDW (T)",
                    "11-1: CDCT (SRT) | 1:45-3:45: SPC (MTV/HKV)\n4-6: Min Pro BPL-1"
                ),
                "SATURDAY" to listOf(
                    "11-1 & 1:45-3:45: Min Project (APL-2)",
                    "11-1 & 1:45-3:45: Min Project (APL-1)",
                    "11-1 & 1:45-3:45: Min Project (BPL-1)"
                )
            )

            paint.textSize = 7.5f
            for ((dayName, divSchedules) in days) {
                paint.color = if (dayName == "SATURDAY") Color.rgb(254, 243, 199) else Color.rgb(248, 250, 252)
                canvas.drawRect(MARGIN, yPos, PAGE_WIDTH - MARGIN, yPos + 62f, paint)

                paint.color = Color.rgb(30, 41, 59)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(dayName, MARGIN + 6f, yPos + 18f, paint)

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = Color.rgb(51, 65, 85)

                val linesA = divSchedules[0].split("\n")
                var lineY = yPos + 16f
                for (l in linesA) {
                    canvas.drawText(l, MARGIN + 70f, lineY, paint)
                    lineY += 13f
                }

                val linesB = divSchedules[1].split("\n")
                lineY = yPos + 16f
                for (l in linesB) {
                    canvas.drawText(l, MARGIN + 220f, lineY, paint)
                    lineY += 13f
                }

                val linesC = divSchedules[2].split("\n")
                lineY = yPos + 16f
                for (l in linesC) {
                    canvas.drawText(l, MARGIN + 370f, lineY, paint)
                    lineY += 13f
                }

                paint.color = Color.rgb(226, 232, 240)
                canvas.drawLine(MARGIN, yPos + 62f, PAGE_WIDTH - MARGIN, yPos + 62f, paint)
                yPos += 66f
            }

            yPos += 10f
            paint.color = Color.rgb(241, 245, 249)
            canvas.drawRoundRect(MARGIN, yPos, PAGE_WIDTH - MARGIN, yPos + 80f, 6f, 6f, paint)

            paint.color = Color.rgb(15, 23, 42)
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("FACULTY INITIALS & COURSE MAPPING:", MARGIN + 10f, yPos + 16f, paint)

            paint.textSize = 7.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = Color.rgb(71, 85, 105)
            canvas.drawText("• DMT: Prof. D. M. Tank (AIPD)  |  SVN: Prof. S. V. Nimavat (CDCT)  |  HKV: Prof. H. K. Vora (AIWPE/SPC)", MARGIN + 10f, yPos + 32f, paint)
            canvas.drawText("• SRT: Prof. S. R. Tank (CDCT)  |  MTV: Prof. M. T. Vaghasia (SPC)    |  SBP: Prof. S. B. Parmar (SPC)", MARGIN + 10f, yPos + 46f, paint)
            canvas.drawText("• GJB: Prof. G. J. Bhensdadia (AIPD)  |  SJS: Prof. S. J. Sangani (AIWPE)  |  AOB: Prof. A. O. Bhatt (EIDW)", MARGIN + 10f, yPos + 60f, paint)
            canvas.drawText("• Labs: APL-1, APL-2 (Advanced Programming Lab) | BPL-1 (Basic Programming Lab) | Rooms: 101, 102, 103", MARGIN + 10f, yPos + 74f, paint)
            yPos += 96f

            paint.color = Color.rgb(15, 23, 42)
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("HoD (IT Dept)", MARGIN + 30f, yPos + 18f, paint)
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("Time Table Co-ordinator", PAGE_WIDTH / 2f, yPos + 18f, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Principal (GP Rajkot)", PAGE_WIDTH - MARGIN - 30f, yPos + 18f, paint)

            pdfDocument.finishPage(page)

            val outputStream = FileOutputStream(destinationFile)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
