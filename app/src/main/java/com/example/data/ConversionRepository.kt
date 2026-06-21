package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import android.util.Log

class ConversionRepository(private val dao: ConversionDao) {
    val presets: Flow<List<ConversionPreset>> = dao.getAllPresets()
    val history: Flow<List<ConversionHistory>> = dao.getHistory()
    
    private val _allUnits = MutableStateFlow<List<MeasureUnit>>(StaticUnits)
    val allUnits: StateFlow<List<MeasureUnit>> = _allUnits

    // Currency exchange rates relative to USD
    // Fallback static values in case network fails
    private val exchangeRates = mutableMapOf<String, Double>(
        "USD" to 1.0, "EUR" to 0.92, "GBP" to 0.79, "JPY" to 150.8,
        "CAD" to 1.35, "AUD" to 1.52, "INR" to 83.3, "CNY" to 7.19,
        "CHF" to 0.88, "SEK" to 10.4, "NZD" to 1.63
    )

    private val currencyNames = mapOf(
        "USD" to "🇺🇸 US Dollar", "EUR" to "🇪🇺 Euro", "GBP" to "🇬🇧 British Pound",
        "JPY" to "🇯🇵 Japanese Yen", "CAD" to "🇨🇦 Canadian Dollar", "AUD" to "🇦🇺 Australian Dollar",
        "INR" to "🇮🇳 Indian Rupee", "CNY" to "🇨🇳 Chinese Yuan", "CHF" to "🇨🇭 Swiss Franc",
        "SEK" to "🇸🇪 Swedish Krona", "NZD" to "🇳🇿 New Zealand Dollar", "MXN" to "🇲🇽 Mexican Peso",
        "SGD" to "🇸🇬 Singapore Dollar", "HKD" to "🇭🇰 Hong Kong Dollar", "NOK" to "🇳🇴 Norwegian Krone",
        "KRW" to "🇰🇷 South Korean Won", "TRY" to "🇹🇷 Turkish Lira", "RUB" to "🇷🇺 Russian Ruble",
        "BRL" to "🇧🇷 Brazilian Real", "ZAR" to "🇿🇦 South African Rand", "THB" to "🇹🇭 Thai Baht",
        "IDR" to "🇮🇩 Indonesian Rupiah", "MYR" to "🇲🇾 Malaysian Ringgit", "PHP" to "🇵🇭 Philippine Peso",
        "VND" to "🇻🇳 Vietnamese Dong", "TWD" to "🇹🇼 New Taiwan Dollar", "ARS" to "🇦🇷 Argentine Peso"
    )
    
    val unitUsages: Flow<Map<String, Int>> = dao.getAllUnitUsages()
        .map { list -> list.associate { it.unitId to it.usageCount } }

    suspend fun incrementUnitUsage(unitId: String) {
        if (dao.insertUnitUsage(UnitUsage(unitId, 1)) == -1L) {
            dao.incrementUnitUsage(unitId)
        }
    }
    
    init {
        updateCurrencyUnits()
    }

    suspend fun populateDefaultsIfNeeded() {
        if (dao.getCount() == 0) {
            val defaults = listOf(
                ConversionPreset(sourceUnitId = "mm", targetUnitId = "in", usageCount = 10),
                ConversionPreset(sourceUnitId = "c", targetUnitId = "f", usageCount = 9),
                ConversionPreset(sourceUnitId = "kg", targetUnitId = "lb", usageCount = 8),
                ConversionPreset(sourceUnitId = "km", targetUnitId = "mi", usageCount = 7),
                ConversionPreset(sourceUnitId = "V", targetUnitId = "mV", usageCount = 6),
                ConversionPreset(sourceUnitId = "USD", targetUnitId = "EUR", usageCount = 5)
            )
            defaults.forEach { dao.insertPreset(it) }
        }
    }
    
    suspend fun fetchLatestCurrencyRates() {
        withContext(Dispatchers.IO) {
            try {
                // Using an open API that requires no key
                val jsonStr = URL("https://open.er-api.com/v6/latest/USD").readText()
                val json = JSONObject(jsonStr)
                if (json.getString("result") == "success") {
                    val rates = json.getJSONObject("rates")
                    rates.keys().forEach { cur ->
                        exchangeRates[cur] = rates.getDouble(cur)
                    }
                    Log.d("Currency", "Rates updated successfully: ${exchangeRates.size} currencies")
                    updateCurrencyUnits()
                }
            } catch (e: Exception) {
                Log.e("Currency", "Failed to fetch currency rates, using defaults", e)
            }
        }
    }
    
    private fun updateCurrencyUnits() {
        val currencyUnits = currencyNames.keys.filter { exchangeRates.containsKey(it) }.sorted().map { cur ->
            MeasureUnit(
                id = cur,
                category = UnitCategory.CURRENCY,
                symbol = cur,
                name = currencyNames[cur] ?: cur, // Use full name if available
                toBase = { it },
                fromBase = { it }
            )
        }
        _allUnits.value = StaticUnits + currencyUnits
    }

    suspend fun addPreset(sourceId: String, targetId: String) {
        dao.insertPreset(ConversionPreset(sourceUnitId = sourceId, targetUnitId = targetId, usageCount = 0))
    }

    suspend fun incrementUsage(sourceId: String, targetId: String) {
        dao.incrementUsage(sourceId, targetId)
    }
    
    suspend fun removePreset(id: Int) {
        dao.deletePreset(id)
    }

    suspend fun addHistory(sourceId: String, targetId: String, sourceValue: Double, targetValue: Double) {
        dao.insertHistory(
            ConversionHistory(
                sourceUnitId = sourceId,
                targetUnitId = targetId,
                sourceValue = sourceValue,
                targetValue = targetValue,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearHistory() {
        dao.clearHistory()
    }

    suspend fun deleteHistory(id: Int) {
        dao.deleteHistory(id)
    }

    fun getUnitById(id: String): MeasureUnit? {
        return _allUnits.value.find { it.id == id }
    }

    fun convert(value: Double, source: MeasureUnit, target: MeasureUnit): Double {
        if (source.category != target.category) return 0.0
        
        if (source.category == UnitCategory.CURRENCY) {
            val sourceRate = exchangeRates[source.id] ?: 1.0
            val targetRate = exchangeRates[target.id] ?: 1.0
            // convert source to USD, then USD to target
            val inUsd = value / sourceRate
            return inUsd * targetRate
        }
        
        // Mechanical, Electrical, Thermal
        val inBase = source.toBase(value)
        return target.fromBase(inBase)
    }
}
