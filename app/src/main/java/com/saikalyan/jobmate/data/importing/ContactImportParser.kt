package com.saikalyan.jobmate.data.importing

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Xml
import com.saikalyan.jobmate.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile
import javax.inject.Inject

private const val MAX_IMPORT_BYTES = 10L * 1024L * 1024L
private const val MAX_CONTACT_ROWS = 2_000
private const val MAX_COLUMNS = 100
private const val MAX_XML_ENTRY_BYTES = 8L * 1024L * 1024L

data class RawContactRow(val rowNumber: Int, val cells: List<String>)

data class ContactFilePreview(
    val fileName: String,
    val headers: List<String>,
    val rows: List<RawContactRow>,
    val format: ContactFileFormat,
)

enum class ContactFileFormat { CSV, XLSX }

class ContactImportException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Reads a SAF document into a bounded local preview. Source files are never uploaded or retained. */
class ContactImportParser @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun preview(uri: Uri): ContactFilePreview = withContext(ioDispatcher) {
        val fileName = displayName(uri)
        val format = detectFormat(fileName, context.contentResolver.getType(uri))
        val temporary = File.createTempFile("contact-import-", ".tmp", context.cacheDir)
        try {
            val source = context.contentResolver.openInputStream(uri)
                ?: throw ContactImportException("The selected document could not be opened.")
            source.use { input -> temporary.outputStream().use { output -> copyBounded(input, output) } }
            val rows = when (format) {
                ContactFileFormat.CSV -> parseCsv(temporary.inputStream().bufferedReader(Charsets.UTF_8).use { it.readText() })
                ContactFileFormat.XLSX -> parseXlsx(temporary)
            }
            if (rows.isEmpty()) throw ContactImportException("The selected file has no header row.")
            val dataRows = rows.drop(1)
            if (dataRows.size > MAX_CONTACT_ROWS) {
                throw ContactImportException("The file exceeds the $MAX_CONTACT_ROWS row import limit.")
            }
            ContactFilePreview(
                fileName = fileName,
                headers = rows.first().take(MAX_COLUMNS),
                rows = dataRows.mapIndexed { index, cells -> RawContactRow(index + 2, cells.take(MAX_COLUMNS)) },
                format = format,
            )
        } catch (failure: ContactImportException) {
            throw failure
        } catch (failure: Exception) {
            throw ContactImportException("Could not read this ${format.name} file.", failure)
        } finally {
            temporary.delete()
        }
    }

    private fun copyBounded(input: InputStream, output: java.io.OutputStream) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            if (total > MAX_IMPORT_BYTES) throw ContactImportException("The selected file exceeds the 10 MB limit.")
            output.write(buffer, 0, count)
        }
        if (total == 0L) throw ContactImportException("The selected file is empty.")
    }

    private fun displayName(uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) cursor.getString(index)?.let { return it }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "contacts"
    }

    private fun detectFormat(fileName: String, mime: String?): ContactFileFormat {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        if (extension == "xls" || mime == "application/vnd.ms-excel") {
            throw ContactImportException("Legacy XLS files are not supported. Save the workbook as XLSX or CSV.")
        }
        return when {
            extension == "csv" || mime == "text/csv" -> ContactFileFormat.CSV
            extension == "xlsx" || mime == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> ContactFileFormat.XLSX
            else -> throw ContactImportException("Choose a CSV or XLSX contacts file.")
        }
    }

    private fun parseCsv(text: String): List<List<String>> {
        val input = text.removePrefix("\uFEFF")
        val rows = mutableListOf<List<String>>()
        val row = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var index = 0
        while (index < input.length) {
            val char = input[index]
            when {
                quoted && char == '"' && input.getOrNull(index + 1) == '"' -> { cell.append('"'); index++ }
                char == '"' && !quoted -> quoted = true
                char == '"' && quoted -> quoted = false
                (char == ',' || char == '\n' || char == '\r') && !quoted -> {
                    if (char == ',') {
                        row += cell.toString()
                        cell.setLength(0)
                    } else {
                        row += cell.toString()
                        cell.setLength(0)
                        if (row.any(String::isNotBlank)) rows += row.toList()
                        row.clear()
                        if (char == '\r' && input.getOrNull(index + 1) == '\n') index++
                    }
                }
                else -> cell.append(char)
            }
            index++
        }
        if (quoted) throw ContactImportException("CSV contains an unterminated quoted field.")
        if (cell.isNotEmpty() || row.isNotEmpty()) {
            row += cell.toString()
            if (row.any(String::isNotBlank)) rows += row.toList()
        }
        if (rows.size > MAX_CONTACT_ROWS + 1) throw ContactImportException("The file exceeds the $MAX_CONTACT_ROWS row import limit.")
        if (rows.any { it.size > MAX_COLUMNS }) throw ContactImportException("The file has more than $MAX_COLUMNS columns.")
        return rows
    }

    private fun parseXlsx(file: File): List<List<String>> = ZipFile(file).use { zip ->
        val sheet = zip.getEntry("xl/worksheets/sheet1.xml")
            ?: throw ContactImportException("The XLSX file has no first worksheet.")
        if (sheet.size > MAX_XML_ENTRY_BYTES) throw ContactImportException("The first worksheet expands beyond the supported size.")
        val sharedStrings = zip.getEntry("xl/sharedStrings.xml")?.let { entry ->
            if (entry.size > MAX_XML_ENTRY_BYTES) throw ContactImportException("The shared-string table is too large.")
            zip.getInputStream(entry).use { readSharedStrings(it) }
        }.orEmpty()
        zip.getInputStream(sheet).use { readWorksheet(it, sharedStrings) }
    }

    private fun readSharedStrings(input: InputStream): List<String> {
        val parser = safeParser(input)
        val strings = mutableListOf<String>()
        var inItem = false
        val value = StringBuilder()
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "si" -> { inItem = true; value.setLength(0) }
                    "t" -> if (inItem) value.append(parser.nextText())
                }
                XmlPullParser.END_TAG -> if (parser.name == "si") {
                    strings += value.toString()
                    inItem = false
                }
            }
            parser.next()
        }
        return strings
    }

    private fun readWorksheet(input: InputStream, sharedStrings: List<String>): List<List<String>> {
        val parser = safeParser(input)
        val rows = mutableListOf<List<String>>()
        var rowNumber = 0
        var cellIndex = 0
        var cellType = ""
        var cellValue = ""
        var currentCells = mutableMapOf<Int, String>()
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "row" -> {
                        rowNumber = parser.getAttributeValue(null, "r")?.toIntOrNull() ?: (rowNumber + 1)
                        currentCells = mutableMapOf()
                    }
                    "c" -> {
                        cellIndex = columnIndex(parser.getAttributeValue(null, "r").orEmpty())
                        cellType = parser.getAttributeValue(null, "t").orEmpty()
                        cellValue = ""
                    }
                    "v", "t" -> cellValue = parser.nextText()
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "c" -> {
                        val resolved = if (cellType == "s") sharedStrings.getOrNull(cellValue.toIntOrNull() ?: -1).orEmpty() else cellValue
                        if (cellIndex in 0 until MAX_COLUMNS) currentCells[cellIndex] = resolved
                    }
                    "row" -> {
                        if (currentCells.isNotEmpty()) {
                            val last = currentCells.keys.maxOrNull() ?: -1
                            rows += (0..last).map { currentCells[it].orEmpty() }
                            if (rows.size > MAX_CONTACT_ROWS + 1) throw ContactImportException("The file exceeds the $MAX_CONTACT_ROWS row import limit.")
                        }
                    }
                }
            }
            parser.next()
        }
        if (rows.any { it.size > MAX_COLUMNS }) throw ContactImportException("The file has more than $MAX_COLUMNS columns.")
        return rows
    }

    private fun safeParser(input: InputStream): XmlPullParser = Xml.newPullParser().apply {
        setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false)
        setInput(input, Charsets.UTF_8.name())
    }

    private fun columnIndex(reference: String): Int {
        var result = 0
        for (char in reference.takeWhile(Char::isLetter).uppercase()) result = result * 26 + (char - 'A' + 1)
        return result - 1
    }
}

