package ca.skopek.calculator.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Exchange rates quoted against USD: 1 USD = rate units of each currency. */
data class CurrencyRates(
    val rates: Map<String, BigDecimal>,
    /** When the provider last updated the rates (epoch millis). */
    val updatedAt: Long,
    /** When this app fetched them (epoch millis). */
    val fetchedAt: Long,
    val providerName: String,
    val providerUrl: String,
) {
    fun fetchedToday(zone: ZoneId = ZoneId.systemDefault()): Boolean =
        Instant.ofEpochMilli(fetchedAt).atZone(zone).toLocalDate() == LocalDate.now(zone)
}

/**
 * Fetches daily exchange rates and caches them on disk. Rates are refreshed at most once per
 * calendar day; the cached copy is always available offline.
 */
class CurrencyRepository(context: Context) {
    private val file = File(context.filesDir, "currency_rates.json")
    private val mutex = Mutex()

    private val _rates = MutableStateFlow(loadCache())
    val rates: StateFlow<CurrencyRates?> = _rates.asStateFlow()

    private val _updating = MutableStateFlow(false)
    val updating: StateFlow<Boolean> = _updating.asStateFlow()

    /** Set when the most recent refresh failed; cleared on success. */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    suspend fun refreshIfStale() {
        if (_rates.value?.fetchedToday() == true) return
        refresh()
    }

    suspend fun refresh() = mutex.withLock {
        _updating.value = true
        try {
            val fetched = withContext(Dispatchers.IO) { fetch() }
            withContext(Dispatchers.IO) { saveCache(fetched) }
            _rates.value = fetched
            _error.value = null
        } catch (e: Exception) {
            _error.value = e.message ?: e.javaClass.simpleName
        } finally {
            _updating.value = false
        }
    }

    private fun fetch(): CurrencyRates {
        var lastError: Exception? = null
        for (source in sources) {
            try {
                return source.fetch()
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("No rate sources")
    }

    private interface RateSource {
        fun fetch(): CurrencyRates
    }

    /** Primary: ExchangeRate-API's open endpoint (~160 currencies, no key, daily). */
    private object ExchangeRateApi : RateSource {
        override fun fetch(): CurrencyRates {
            val json = JSONObject(get("https://open.er-api.com/v6/latest/USD"))
            if (json.optString("result") != "success") error("ExchangeRate-API: ${json.optString("error-type", "unexpected response")}")
            val rates = parseRates(json.getJSONObject("rates"))
            val updatedAt = json.optLong("time_last_update_unix", 0L).takeIf { it > 0 }?.times(1000) ?: System.currentTimeMillis()
            return CurrencyRates(rates, updatedAt, System.currentTimeMillis(), "ExchangeRate-API", "https://www.exchangerate-api.com")
        }
    }

    /** Fallback: Frankfurter (European Central Bank reference rates, ~30 currencies). */
    private object Frankfurter : RateSource {
        override fun fetch(): CurrencyRates {
            val json = JSONObject(get("https://api.frankfurter.dev/v1/latest?base=USD"))
            val rates = parseRates(json.getJSONObject("rates")).toMutableMap()
            rates["USD"] = BigDecimal.ONE
            val updatedAt = json.optString("date").takeIf { it.isNotEmpty() }
                ?.let { LocalDate.parse(it).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli() }
                ?: System.currentTimeMillis()
            return CurrencyRates(rates, updatedAt, System.currentTimeMillis(), "Frankfurter (ECB)", "https://frankfurter.dev")
        }
    }

    private companion object {
        val sources: List<RateSource> = listOf(ExchangeRateApi, Frankfurter)

        fun get(url: String): String {
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 10_000
                connection.readTimeout = 15_000
                connection.setRequestProperty("Accept", "application/json")
                val code = connection.responseCode
                if (code != HttpURLConnection.HTTP_OK) error("HTTP $code from ${URL(url).host}")
                return connection.inputStream.bufferedReader().use { it.readText() }
            } finally {
                connection.disconnect()
            }
        }

        fun parseRates(obj: JSONObject): Map<String, BigDecimal> {
            val result = LinkedHashMap<String, BigDecimal>()
            for (key in obj.keys()) {
                val value = obj.optString(key).toBigDecimalOrNull() ?: continue
                if (value.signum() > 0) result[key.uppercase()] = value
            }
            if (result.isEmpty()) error("No rates in response")
            return result
        }
    }

    private fun loadCache(): CurrencyRates? {
        if (!file.exists()) return null
        return try {
            val json = JSONObject(file.readText())
            CurrencyRates(
                rates = parseRates(json.getJSONObject("rates")),
                updatedAt = json.getLong("updatedAt"),
                fetchedAt = json.getLong("fetchedAt"),
                providerName = json.optString("providerName", "ExchangeRate-API"),
                providerUrl = json.optString("providerUrl", "https://www.exchangerate-api.com"),
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun saveCache(rates: CurrencyRates) {
        val json = JSONObject()
            .put("updatedAt", rates.updatedAt)
            .put("fetchedAt", rates.fetchedAt)
            .put("providerName", rates.providerName)
            .put("providerUrl", rates.providerUrl)
            .put("rates", JSONObject().also { obj -> rates.rates.forEach { (k, v) -> obj.put(k, v.toPlainString()) } })
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeText(json.toString())
        if (!temp.renameTo(file)) {
            file.writeText(json.toString())
            temp.delete()
        }
    }
}
