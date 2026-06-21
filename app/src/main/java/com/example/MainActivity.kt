package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.MeasureUnit
import com.example.data.UnitCategory
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.ui.theme.MyApplicationTheme

import com.example.data.ConversionHistory
import com.example.data.ConversionPreset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.launch

fun getCategoryIcon(category: UnitCategory): ImageVector {
    return when(category) {
        UnitCategory.LENGTH -> Icons.Rounded.Straighten
        UnitCategory.AREA -> Icons.Rounded.Dashboard
        UnitCategory.VOLUME -> Icons.Rounded.WaterDrop
        UnitCategory.SPEED -> Icons.Rounded.Speed
        UnitCategory.DATA -> Icons.Rounded.Storage
        UnitCategory.MASS -> Icons.Rounded.Scale
        UnitCategory.PRESSURE -> Icons.Rounded.Compress
        UnitCategory.VOLTAGE -> Icons.Rounded.Bolt
        UnitCategory.CURRENT -> Icons.Rounded.Timeline
        UnitCategory.RESISTANCE -> Icons.Rounded.LinearScale
        UnitCategory.POWER -> Icons.Rounded.Power
        UnitCategory.TEMPERATURE -> Icons.Rounded.Thermostat
        UnitCategory.ENERGY -> Icons.Rounded.Lightbulb
        UnitCategory.CURRENCY -> Icons.Rounded.Payments
    }
}

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(
            (application as ConverterApplication).repository,
            getSharedPreferences("unit_converter_prefs", android.content.Context.MODE_PRIVATE)
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.state.collectAsState()
            val history by viewModel.history.collectAsState()
            val presetsByCategory by viewModel.presetsByCategory.collectAsState()
            
            var showHistorySheet by remember { mutableStateOf(false) }

            MyApplicationTheme(darkTheme = state.isDarkTheme) {
                val context = LocalContext.current

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        TopAppBar(
                            title = { Text("UnitConverter", fontWeight = FontWeight.Medium) },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                            ),
                            actions = {
                                IconButton(onClick = { viewModel.toggleTheme() }) {
                                    Icon(
                                        if (state.isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                        contentDescription = "Toggle Theme",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = { showHistorySheet = true },
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Icon(Icons.Rounded.History, contentDescription = "History")
                        }
                    }
                ) { innerPadding ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        item {
                            // Search bar for categories
                            OutlinedTextField(
                                value = state.categorySearchQuery,
                                onValueChange = viewModel::updateCategorySearchQuery,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 8.dp),
                                placeholder = { Text("Search groups of units...") },
                                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true,
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                        
                        item {
                            // Categories List
                            CategoryRow(
                                categories = state.allCategories,
                                selectedCategory = state.selectedCategory,
                                onSelect = { viewModel.selectCategory(it) },
                                onReorder = { from, to -> viewModel.reorderCategory(from, to) }
                            )
                        }

                        item {
                            // Converter Card
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                            ) {
                                // Source
                                ConversionInputCard(
                                    title = "FROM",
                                    unit = state.sourceUnit,
                                    value = state.inputValue,
                                    onValueChange = { viewModel.updateInput(it) },
                                    readOnly = false,
                                    onClickSelectUnit = { viewModel.setSourceDropdownExpanded(true) }
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                // Swap Button
                                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .clickable { viewModel.swapUnits() },
                                        shadowElevation = 8.dp
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Rounded.SwapVert, contentDescription = "Swap", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(32.dp))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))

                                // Target
                                ConversionInputCard(
                                    title = "TO",
                                    unit = state.targetUnit,
                                    value = state.resultValue,
                                    onValueChange = { },
                                    readOnly = true,
                                    onClickSelectUnit = { viewModel.setTargetDropdownExpanded(true) }
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Copy Result Button
                                    Button(
                                        onClick = {
                                            viewModel.copyResult()
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clipText = "${state.resultValue} ${state.targetUnit?.symbol ?: ""}"
                                            val clip = ClipData.newPlainText("Conversion", clipText)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Copied: $clipText", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f).height(56.dp),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Icon(Icons.Rounded.ContentCopy, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Copy Result", style = MaterialTheme.typography.labelLarge)
                                    }

                                    // Save Preset Button
                                    IconButton(
                                        onClick = { viewModel.saveCurrentConversionAsPreset() }
                                    ) {
                                        Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }

                        if (presetsByCategory.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(32.dp))
                                Text(
                                    "Saved Conversions",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                                )
                            }
                            
                            presetsByCategory.forEach { (category, categoryPresets) ->
                                item {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                                    ) {
                                        Icon(getCategoryIcon(category), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(category.title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                items(categoryPresets.chunked(2), key = { rowPresets -> rowPresets.map { it.id }.joinToString("_") }) { rowPresets ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowPresets.forEach { preset ->
                                            HistoryPresetItem(
                                                modifier = Modifier.weight(1f),
                                                sourceId = preset.sourceUnitId,
                                                targetId = preset.targetUnitId,
                                                value = null,
                                                result = null,
                                                timestamp = null,
                                                onClick = { viewModel.loadPresetOrHistory(preset.sourceUnitId, preset.targetUnitId) },
                                                onDelete = { viewModel.removePreset(preset.id) }
                                            )
                                        }
                                        if (rowPresets.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Modals
                if (showHistorySheet) {
                    @OptIn(ExperimentalMaterial3Api::class)
                    androidx.compose.material3.ModalBottomSheet(
                        onDismissRequest = { showHistorySheet = false }
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "History",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                if (history.isNotEmpty()) {
                                    TextButton(onClick = { viewModel.clearHistory() }) {
                                        Text("Clear")
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            if (history.isEmpty()) {
                                Text(
                                    "No history yet.",
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(bottom = 16.dp)
                                ) {
                                    items(history.chunked(2), key = { rowHistory -> rowHistory.map { it.id }.joinToString("_") }) { rowHistory ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            rowHistory.forEach { hist ->
                                                HistoryPresetItem(
                                                    modifier = Modifier.weight(1f),
                                                    sourceId = hist.sourceUnitId,
                                                    targetId = hist.targetUnitId,
                                                    value = hist.sourceValue.toString(),
                                                    result = hist.targetValue.toString(),
                                                    timestamp = hist.timestamp,
                                                    onClick = {
                                                        showHistorySheet = false
                                                    },
                                                    onDelete = { viewModel.deleteHistory(hist.id) }
                                                )
                                            }
                                            if (rowHistory.size == 1) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (state.isSourceDropdownExpanded) {
                    UnitSelectionModal(
                        units = state.unitsForCategory,
                        onDismiss = { viewModel.setSourceDropdownExpanded(false) },
                        onSelect = { viewModel.setSourceUnit(it) }
                    )
                }
                if (state.isTargetDropdownExpanded) {
                    UnitSelectionModal(
                        units = state.unitsForCategory,
                        onDismiss = { viewModel.setTargetDropdownExpanded(false) },
                        onSelect = { viewModel.setTargetUnit(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryRow(
    categories: List<UnitCategory>,
    selectedCategory: UnitCategory,
    onSelect: (UnitCategory) -> Unit,
    onReorder: (Int, Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 24.dp),
        modifier = Modifier.padding(bottom = 24.dp)
    ) {
        itemsIndexed(categories, key = { _, cat -> cat.name }) { index, cat ->
            val isSelected = selectedCategory == cat
            var dragXOffset by remember { mutableStateOf(0f) }
            val currentIndex by rememberUpdatedState(index)
            val spacing = with(androidx.compose.ui.platform.LocalDensity.current) { 8.dp.toPx() }
            
            Surface(
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .then(if (dragXOffset == 0f) Modifier.animateItem() else Modifier)
                    .offset { androidx.compose.ui.unit.IntOffset(dragXOffset.toInt(), 0) }
                    .zIndex(if (dragXOffset != 0f) 1f else 0f)
                    .clip(RoundedCornerShape(24.dp))
                    .pointerInput(cat) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragXOffset += dragAmount.x

                                val itemWidth = size.width.toFloat() + spacing
                                while (dragXOffset > itemWidth * 0.6f && currentIndex < categories.lastIndex) {
                                    onReorder(currentIndex, currentIndex + 1)
                                    dragXOffset -= itemWidth
                                    scope.launch { listState.scrollBy(itemWidth) }
                                }
                                while (dragXOffset < -itemWidth * 0.6f && currentIndex > 0) {
                                    onReorder(currentIndex, currentIndex - 1)
                                    dragXOffset += itemWidth
                                    scope.launch { listState.scrollBy(-itemWidth) }
                                }
                            },
                            onDragEnd = { dragXOffset = 0f },
                            onDragCancel = { dragXOffset = 0f }
                        )
                    }
                    .clickable { onSelect(cat) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Icon(
                        getCategoryIcon(cat),
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = cat.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HistoryPresetItem(
    modifier: Modifier = Modifier,
    sourceId: String,
    targetId: String,
    value: String?,
    result: String?,
    timestamp: Long?,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val isHistory = value != null

    val bgColor = MaterialTheme.colorScheme.primary
    val fgColor = MaterialTheme.colorScheme.onPrimary
    
    val sourceSymbol = remember(sourceId) { com.example.data.StaticUnits.find { it.id == sourceId }?.symbol ?: sourceId }
    val targetSymbol = remember(targetId) { com.example.data.StaticUnits.find { it.id == targetId }?.symbol ?: targetId }

    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val haptic = LocalHapticFeedback.current

    val shape = RoundedCornerShape(if (isHistory) 16.dp else 24.dp)

    val itemContent = @Composable { mod: Modifier ->
        Surface(
            modifier = mod
                .clickable {
                    if (isHistory) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val formattedRes = result?.toDoubleOrNull()?.let { String.format("%.4f", it).trimEnd('0').trimEnd { c -> c == '.' || c == ',' } } ?: result ?: ""
                        val text = "$formattedRes $targetSymbol"
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(text))
                        android.widget.Toast.makeText(context, "Copied result: $text", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        onClick()
                    }
                },
            color = bgColor,
            shape = shape
        ) {
            Column(
                modifier = Modifier.padding(if (isHistory) 16.dp else 20.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "$sourceSymbol → $targetSymbol",
                        style = if (isHistory) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = fgColor
                    )
                }
                
                if (isHistory && result != null && value != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val formattedVal = value.toDoubleOrNull()?.let { if (it % 1 == 0.0) it.toInt().toString() else value } ?: value
                    val formattedRes = result.toDoubleOrNull()?.let { String.format("%.4f", it).trimEnd('0').trimEnd { c -> c == '.' || c == ',' } } ?: result
                    Text(
                        text = "$formattedVal = $formattedRes",
                        style = MaterialTheme.typography.bodyLarge,
                        color = fgColor.copy(alpha = 0.8f)
                    )
                }
                
                if (timestamp != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val sdf = remember { java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()) }
                    val dateStr = sdf.format(java.util.Date(timestamp))
                    Text(text = dateStr, style = MaterialTheme.typography.labelMedium, color = fgColor.copy(alpha = 0.6f))
                }
            }
        }
    }

    if (onDelete != null) {
        val dismissState = androidx.compose.material3.rememberSwipeToDismissBoxState(
            confirmValueChange = {
                if (it == androidx.compose.material3.SwipeToDismissBoxValue.EndToStart || it == androidx.compose.material3.SwipeToDismissBoxValue.StartToEnd) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDelete()
                    true
                } else false
            }
        )
        androidx.compose.material3.SwipeToDismissBox(
            modifier = modifier,
            state = dismissState,
            backgroundContent = {
                val color = MaterialTheme.colorScheme.errorContainer
                val iconColor = MaterialTheme.colorScheme.onErrorContainer
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = shape,
                    color = color
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), contentAlignment = Alignment.CenterEnd) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = iconColor)
                    }
                }
            },
            content = { itemContent(Modifier) }
        )
    } else {
        itemContent(modifier)
    }
}

@Composable
fun ConversionInputCard(
    title: String,
    unit: MeasureUnit?,
    value: String,
    onValueChange: (String) -> Unit,
    readOnly: Boolean,
    onClickSelectUnit: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        shadowElevation = if (readOnly) 0.dp else 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClickSelectUnit)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = unit?.name ?: "Select Unit",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                BasicTextField(
                    value = value,
                    onValueChange = { newStr ->
                        if (!readOnly) {
                            val filtered = newStr.filter { it.isDigit() || it == '.' }
                            if (filtered.count { it == '.' } <= 1) {
                                onValueChange(filtered)
                            }
                        }
                    },
                    readOnly = readOnly,
                    textStyle = MaterialTheme.typography.displayLarge.copy(
                        color = if (readOnly) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f).background(Color.Transparent),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (value.isEmpty()) {
                                Text(
                                    text = "0",
                                    style = MaterialTheme.typography.displayLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = unit?.symbol ?: "",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitSelectionModal(
    units: List<MeasureUnit>,
    onDismiss: () -> Unit,
    onSelect: (MeasureUnit) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredUnits = remember(searchQuery, units) {
        if (searchQuery.isBlank()) units else units.filter { 
            it.name.contains(searchQuery, ignoreCase = true) || it.symbol.contains(searchQuery, ignoreCase = true) 
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f)
        ) {
            Text(
                "Select Unit",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search units...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(filteredUnits) { u ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(u); onDismiss() }
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = u.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = u.symbol,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
