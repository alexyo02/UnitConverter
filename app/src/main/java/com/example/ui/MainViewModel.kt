package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ConversionRepository
import com.example.data.ConversionPreset
import com.example.data.ConversionHistory
import com.example.data.MeasureUnit
import com.example.data.UnitCategory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ManualConversionState(
    val selectedCategory: UnitCategory = UnitCategory.LENGTH,
    val unitsForCategory: List<MeasureUnit> = emptyList(),
    val sourceUnit: MeasureUnit? = null,
    val targetUnit: MeasureUnit? = null,
    val inputValue: String = "1",
    val resultValue: String = "",
    val isSourceDropdownExpanded: Boolean = false,
    val isTargetDropdownExpanded: Boolean = false,
    val categorySearchQuery: String = "",
    val allCategories: List<UnitCategory> = UnitCategory.entries.toList(),
    val isDarkTheme: Boolean = true,
    val showHistory: Boolean = false
)

class MainViewModel(
    private val repository: ConversionRepository,
    private val sharedPrefs: android.content.SharedPreferences
) : ViewModel() {

    private val initialCategories: List<UnitCategory> = run {
        val saved = sharedPrefs.getString("category_order", null)
        if (saved != null) {
            val names = saved.split(",")
            val sortedList = mutableListOf<UnitCategory>()
            names.forEach { name ->
                try {
                    sortedList.add(UnitCategory.valueOf(name))
                } catch (e: Exception) { }
            }
            UnitCategory.entries.filter { it !in sortedList }.forEach { sortedList.add(it) }
            sortedList
        } else {
            UnitCategory.entries.toList()
        }
    }

    private val _state = MutableStateFlow(ManualConversionState(allCategories = initialCategories))
    val state: StateFlow<ManualConversionState> = _state
    
    fun updateCategorySearchQuery(query: String) {
        _state.update {
            val filtered = UnitCategory.entries.filter { cat ->
                cat.title.contains(query, ignoreCase = true)
            }
            it.copy(
                categorySearchQuery = query,
                allCategories = filtered.ifEmpty { UnitCategory.entries.toList() } // Fallback to all if empty or no match
            )
        }
    }
    
    fun reorderCategory(fromIndex: Int, toIndex: Int) {
        _state.update {
            val list = it.allCategories.toMutableList()
            if (fromIndex in list.indices && toIndex in list.indices) {
                val item = list.removeAt(fromIndex)
                list.add(toIndex, item)
                sharedPrefs.edit().putString("category_order", list.joinToString(",") { cat -> cat.name }).apply()
                it.copy(allCategories = list)
            } else it
        }
    }
    
    val history: StateFlow<List<ConversionHistory>> = repository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
        
    val presetsByCategory: StateFlow<Map<UnitCategory, List<ConversionPreset>>> = combine(repository.presets, repository.allUnits) { presets, units ->
        val distinctPresets = presets.distinctBy { setOf(it.sourceUnitId, it.targetUnitId) }
        val unitMap = units.associateBy { it.id }
        distinctPresets.groupBy { p ->
            unitMap[p.sourceUnitId]?.category ?: UnitCategory.LENGTH
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val presets: StateFlow<List<ConversionPreset>> = repository.presets
        .map { list -> list.distinctBy { setOf(it.sourceUnitId, it.targetUnitId) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.fetchLatestCurrencyRates()
        }
        
        viewModelScope.launch {
            combine(repository.allUnits, repository.unitUsages) { units, usages ->
                Pair(units, usages)
            }.collect { (units, usages) ->
                val currentCategory = _state.value.selectedCategory
                val categoryUnits = units.filter { it.category == currentCategory }
                    .sortedWith(compareByDescending<MeasureUnit> { usages[it.id] ?: 0 }.thenBy { it.name })
                _state.update { 
                    it.copy(
                        unitsForCategory = categoryUnits,
                        sourceUnit = it.sourceUnit ?: categoryUnits.firstOrNull(),
                        targetUnit = it.targetUnit ?: categoryUnits.getOrNull(1) ?: categoryUnits.firstOrNull()
                    )
                }
                recalculate()
            }
        }
    }

    fun selectCategory(category: UnitCategory) {
        viewModelScope.launch {
            val usages = repository.unitUsages.first()
            val categoryUnits = repository.allUnits.value.filter { it.category == category }
                .sortedWith(compareByDescending<MeasureUnit> { usages[it.id] ?: 0 }.thenBy { it.name })
            _state.update {
                it.copy(
                    selectedCategory = category,
                    unitsForCategory = categoryUnits,
                    sourceUnit = categoryUnits.firstOrNull(),
                    targetUnit = categoryUnits.getOrNull(1) ?: categoryUnits.firstOrNull()
                )
            }
            recalculate()
        }
    }

    fun setSourceUnit(unit: MeasureUnit) {
        _state.update { it.copy(sourceUnit = unit, isSourceDropdownExpanded = false) }
        viewModelScope.launch { repository.incrementUnitUsage(unit.id) }
        recalculate()
    }

    fun setTargetUnit(unit: MeasureUnit) {
        _state.update { it.copy(targetUnit = unit, isTargetDropdownExpanded = false) }
        viewModelScope.launch { repository.incrementUnitUsage(unit.id) }
        recalculate()
    }
    
    fun swapUnits() {
        _state.update { 
            it.copy(
                sourceUnit = it.targetUnit,
                targetUnit = it.sourceUnit
            )
        }
        recalculate()
    }
    
    fun updateInput(value: String) {
        _state.update { it.copy(inputValue = value) }
        recalculate()
    }
    
    fun setSourceDropdownExpanded(expanded: Boolean) {
        _state.update { it.copy(isSourceDropdownExpanded = expanded) }
    }

    fun setTargetDropdownExpanded(expanded: Boolean) {
        _state.update { it.copy(isTargetDropdownExpanded = expanded) }
    }

    fun toggleTheme() {
        _state.update { it.copy(isDarkTheme = !it.isDarkTheme) }
    }
    
    fun toggleHistory() {
        _state.update { it.copy(showHistory = !it.showHistory) }
    }

    fun saveCurrentConversionAsPreset() {
        val s = _state.value
        val source = s.sourceUnit
        val target = s.targetUnit
        if (source != null && target != null) {
            val exists = presets.value.any { it.sourceUnitId == source.id && it.targetUnitId == target.id }
            if (!exists) {
                viewModelScope.launch {
                    repository.addPreset(source.id, target.id)
                }
            }
        }
    }
    
    fun copyResult() {
        val s = _state.value
        val source = s.sourceUnit
        val target = s.targetUnit
        val input = s.inputValue.toDoubleOrNull() ?: 0.0
        val res = s.resultValue.replace(',', '.').toDoubleOrNull() ?: 0.0
        if (source != null && target != null && input > 0) {
            viewModelScope.launch {
                repository.addHistory(source.id, target.id, input, res)
            }
        }
    }

    fun removePreset(id: Int) {
        viewModelScope.launch {
            repository.removePreset(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun deleteHistory(id: Int) {
        viewModelScope.launch {
            repository.deleteHistory(id)
        }
    }

    fun loadPresetOrHistory(sourceId: String, targetId: String, inputValue: String? = null) {
        val sourceUnit = repository.getUnitById(sourceId)
        val targetUnit = repository.getUnitById(targetId)
        
        if (sourceUnit != null && targetUnit != null) {
            val categoryUnits = repository.allUnits.value.filter { it.category == sourceUnit.category }
            _state.update {
                it.copy(
                    selectedCategory = sourceUnit.category,
                    unitsForCategory = categoryUnits,
                    sourceUnit = sourceUnit,
                    targetUnit = targetUnit,
                    inputValue = inputValue ?: it.inputValue
                )
            }
            recalculate()
            if (inputValue == null) {
                viewModelScope.launch {
                    repository.incrementUsage(sourceId, targetId)
                }
            }
        }
    }

    private fun recalculate() {
        val s = _state.value
        val source = s.sourceUnit
        val target = s.targetUnit
        val input = s.inputValue.toDoubleOrNull() ?: 0.0
        
        if (source != null && target != null) {
            val res = repository.convert(input, source, target)
            val formatted = if (res == 0.0) "0" else String.format("%.6f", res).trimEnd('0').trimEnd { it == '.' || it == ',' }
            _state.update { it.copy(resultValue = formatted) }
        } else {
            _state.update { it.copy(resultValue = "") }
        }
    }
}
