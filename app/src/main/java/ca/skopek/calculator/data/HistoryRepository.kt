package ca.skopek.calculator.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Persists history as a small JSON file in app-private storage. Call from a background thread. */
class HistoryRepository(context: Context) {
    private val file = File(context.filesDir, FILE_NAME)

    fun load(): List<HistoryEntry> {
        if (!file.exists()) return emptyList()
        return try {
            val array = JSONArray(file.readText())
            List(array.length()) { i ->
                val obj = array.getJSONObject(i)
                HistoryEntry(
                    id = obj.getLong("id"),
                    expression = obj.getString("expression"),
                    result = obj.getString("result"),
                    resultValue = obj.getString("resultValue"),
                    timestamp = obj.getLong("timestamp"),
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(entries: List<HistoryEntry>) {
        val array = JSONArray()
        entries.takeLast(MAX_ENTRIES).forEach { entry ->
            array.put(
                JSONObject()
                    .put("id", entry.id)
                    .put("expression", entry.expression)
                    .put("result", entry.result)
                    .put("resultValue", entry.resultValue)
                    .put("timestamp", entry.timestamp),
            )
        }
        val temp = File(file.parentFile, "$FILE_NAME.tmp")
        temp.writeText(array.toString())
        if (!temp.renameTo(file)) {
            file.writeText(array.toString())
            temp.delete()
        }
    }

    companion object {
        const val MAX_ENTRIES = 500
        private const val FILE_NAME = "history.json"
    }
}
