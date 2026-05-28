package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.api.AttributeResult
import com.example.data.model.ClothingItem
import com.example.ui.components.GarmentVisualizer
import com.example.ui.components.getColorFromName
import com.example.ui.viewmodel.AddClothingState
import com.example.ui.viewmodel.ClosetViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClosetCatalogScreen(
    viewModel: ClosetViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val items by viewModel.filteredItems.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedSeason by viewModel.selectedSeason.collectAsStateWithLifecycle()
    val isApiKeyAvailable by viewModel.isApiKeyAvailable.collectAsStateWithLifecycle()
    val addState by viewModel.addClothingState.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "智能衣橱",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                        Text(
                            text = "共收录了 ${items.size} 件时尚好物",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                },
                actions = {
                    // Quick guide warning about Qwen Key
                    IconButton(onClick = {
                        Toast.makeText(
                            context,
                            if (isApiKeyAvailable) "千问 AI 已成功连接 ⚡" else "请在本地 .env 中设置 QWEN_API_KEY",
                            Toast.LENGTH_LONG
                        ).show()
                    }) {
                        Icon(
                            imageVector = if (isApiKeyAvailable) Icons.Default.CloudQueue else Icons.Default.CloudOff,
                            contentDescription = "API Status",
                            tint = if (isApiKeyAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = "拍照录入") },
                text = { Text("录入单品", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // 1. Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                placeholder = { Text("搜索名字、标签或备注...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.Transparent
                )
            )

            // 2. Categories Filter Row
            Text(
                text = "品类分类",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            val categories = listOf("All" to "全部") + ClothingItem.ALL_CATEGORIES.map { it to translateCategory(it) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { (catValue, catLabel) ->
                    val isSelected = selectedCategory == catValue
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectedCategory.value = catValue },
                        label = { Text(catLabel, fontSize = 12.sp) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        border = null
                    )
                }
            }

            // 3. Seasons Filter Row
            Text(
                text = "适用季节",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
            val seasons = ClothingItem.ALL_SEASONS.map { it to translateSeason(it) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                seasons.forEach { (seasonValue, seasonLabel) ->
                    val isSelected = selectedSeason == seasonValue
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectedSeason.value = seasonValue },
                        label = { Text(seasonLabel, fontSize = 12.sp) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        border = null
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Closet Grid
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DryCleaning,
                            contentDescription = "Empty Closet",
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "没有找到匹配的衣物哦",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            fontSize = 16.sp
                        )
                        Text(
                            text = "你可以尝试清空筛选或者点击下方按钮录入一件新衣服！",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        ClothingItemCard(
                            item = item,
                            onDelete = { viewModel.deleteClothing(item) }
                        )
                    }
                }
            }
        }
    }

    // 5. Add Clothing Dialog
    if (showAddDialog) {
        AddClothingDialog(
            viewModel = viewModel,
            isApiKeyAvailable = isApiKeyAvailable,
            onDismiss = {
                viewModel.resetAddClothingState()
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ClothingItemCard(
    item: ClothingItem,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Visualizer on top
            GarmentVisualizer(
                category = item.category,
                colorName = item.color,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )

            // Info bottom
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    // Category Tag Label Caps
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = translateCategory(item.category),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Season label
                    Text(
                        text = translateSeason(item.season),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                // Styled Tags Flow
                if (item.tags.isNotEmpty()) {
                    val tagList = item.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        tagList.take(3).forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = tag,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (item.notes.isNotEmpty()) {
                    Text(
                        text = item.notes,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}

/**
 * Visual Form & Camera Simulator Dialog for registering garment
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddClothingDialog(
    viewModel: ClosetViewModel,
    isApiKeyAvailable: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val addState by viewModel.addClothingState.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf(ClothingItem.CATEGORY_TOPS) }
    var selectedColor by remember { mutableStateOf("White") }
    var selectedSeason by remember { mutableStateOf("All") }
    var tagsInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }

    // Simulation presets to click on
    val presetClothes = listOf(
        Triple("复古墨黄厚毛针织衫", "Yellow", "亮金色针织衫开衫"),
        Triple("浅粉色优雅压褶半裙", "Pink", "仙气百褶裙 A字裙"),
        Triple("高级感亮黑皮质单肩托特包", "Black", "经典皮革大容量配饰"),
        Triple("马卡龙蓝色透气网眼跑步鞋", "Blue", "极度舒适减震气垫鞋"),
        Triple("复古双排扣落肩卡其风衣", "Khaki", "中长款宽松挡风大衣外套")
    )

    if (addState is AddClothingState.Success) {
        LaunchedEffect(Unit) {
            onDismiss()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "新增衣橱单品",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 12.dp)
                ) {
                    // Simulation Section
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = "Simulate",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "选择模拟拍照款式",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Text(
                                text = "点击下方模板模拟用手持相机拍下一件衣服，将调用真实千问 API 智能分类和自动生成穿搭分析！",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))

                            // Flow row of simulations
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                presetClothes.forEach { (title, colorName, desc) ->
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                MaterialTheme.colorScheme.surface,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .border(
                                                1.dp,
                                                MaterialTheme.colorScheme.outlineVariant,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                name = title
                                                notesInput = desc
                                                
                                                // Generate mock picture on Canvas, then trigger Qwen Vision/Text analyzer
                                                val mockBitmap = generateMockBitmap(title, colorName)
                                                
                                                viewModel.runSmartClothingAnalysis(
                                                    title,
                                                    desc,
                                                    mockBitmap
                                                ) { aiResult ->
                                                    if (aiResult.error == null) {
                                                        selectedCat = aiResult.category
                                                        selectedColor = aiResult.color
                                                        selectedSeason = aiResult.season
                                                        tagsInput = aiResult.tags.joinToString(", ")
                                                        if (aiResult.notesText.isNotEmpty()) {
                                                            notesInput = aiResult.notesText
                                                        }
                                                    }
                                                }
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "📸 $title",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("衣服名称 *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Category Selection
                    Text(
                        text = "类别",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ClothingItem.ALL_CATEGORIES.forEach { cat ->
                            val isSel = selectedCat == cat
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedCat = cat }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = translateCategory(cat),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Color Selector
                    Text(
                        text = "主要色系",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ClothingItem.ALL_COLORS.forEach { colName ->
                            val isSel = selectedColor == colName
                            val colorValue = getColorFromName(colName)
                            Row(
                                modifier = Modifier
                                    .background(
                                        if (isSel) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        if (isSel) 1.5.dp else 1.dp,
                                        if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedColor = colName }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(colorValue)
                                        .border(0.5.dp, Color.Gray, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = translateColor(colName),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Season Selector
                    Text(
                        text = "合适季节",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ClothingItem.ALL_SEASONS.forEach { season ->
                            val isSel = selectedSeason == season
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSel) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedSeason = season }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = translateSeason(season),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tags and Notes
                    OutlinedTextField(
                        value = tagsInput,
                        onValueChange = { tagsInput = it },
                        label = { Text("流行标签 (多个请用逗号隔开)") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("例如：韩系, 纯棉, 宽松") },
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("面料或穿搭备注") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // AI suggestions or general state footer banner
                AnimatedVisibility(
                    visible = addState is AddClothingState.AiAnalyzing || addState is AddClothingState.Saving,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .background(
                                MaterialTheme.colorScheme.tertiaryContainer,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (addState is AddClothingState.AiAnalyzing) "千问正在分析照片提取配色/面料..." else "正在拼命为您存档中...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                if (addState is AddClothingState.Error) {
                    Text(
                        text = (addState as AddClothingState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // Confirm Actions row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("取消")
                    }

                    Button(
                        onClick = {
                            if (name.trim().isEmpty()) {
                                Toast.makeText(context, "请先输入衣服名称", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.insertClothing(
                                name = name,
                                category = selectedCat,
                                color = selectedColor,
                                season = selectedSeason,
                                tags = tagsInput,
                                notes = notesInput
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = addState !is AddClothingState.AiAnalyzing && addState !is AddClothingState.Saving
                    ) {
                        Text("录入衣橱", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Helper to dynamically draw a dummy mock photo on canvas for Qwen Vision REST requests!
 */
fun generateMockBitmap(name: String, colorName: String): Bitmap {
    val bitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint()

    // 1. Draw chic background gradient
    paint.shader = android.graphics.LinearGradient(
        0f, 0f, 500f, 500f,
        android.graphics.Color.DKGRAY,
        android.graphics.Color.BLACK,
        android.graphics.Shader.TileMode.CLAMP
    )
    canvas.drawRect(0f, 0f, 500f, 500f, paint)
    paint.shader = null

    // 2. Clear color shape circle representation
    val c = getColorFromName(colorName)
    paint.color = android.graphics.Color.argb(
        (c.alpha * 255).toInt(),
        (c.red * 255).toInt(),
        (c.green * 255).toInt(),
        (c.blue * 255).toInt()
    )
    canvas.drawCircle(250f, 220f, 120f, paint)

    // 3. Draw text water print labels
    paint.color = android.graphics.Color.WHITE
    paint.textSize = 26f
    paint.isAntiAlias = true
    paint.textAlign = Paint.Align.CENTER
    canvas.drawText("AI CLOSET PHOTO SCAN", 250f, 400f, paint)

    paint.color = android.graphics.Color.LTGRAY
    paint.textSize = 20f
    canvas.drawText(name, 250f, 440f, paint)

    return bitmap
}

fun translateCategory(cat: String): String {
    return when (cat) {
        ClothingItem.CATEGORY_TOPS -> "上衣"
        ClothingItem.CATEGORY_BOTTOMS -> "下装"
        ClothingItem.CATEGORY_SHOES -> "鞋履"
        ClothingItem.CATEGORY_OUTERBODY -> "外套"
        ClothingItem.CATEGORY_ACCESSORIES -> "配饰"
        else -> cat
    }
}

fun translateSeason(season: String): String {
    return when (season) {
        "All" -> "四季"
        "Spring" -> "春"
        "Summer" -> "夏"
        "Autumn" -> "秋"
        "Winter" -> "冬"
        else -> season
    }
}

fun translateColor(color: String): String {
    return when (color.lowercase()) {
        "white" -> "白色"
        "black" -> "黑色"
        "gray" -> "灰色"
        "navy" -> "藏青"
        "blue" -> "蓝色"
        "red" -> "红色"
        "green" -> "绿色"
        "yellow" -> "黄色"
        "orange" -> "橙色"
        "pink" -> "粉色"
        "purple" -> "紫色"
        "beige" -> "米色"
        "khaki" -> "卡其"
        "brown" -> "棕色"
        else -> color
    }
}
