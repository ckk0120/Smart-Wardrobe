package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ClothingItem
import com.example.ui.components.getColorFromName
import com.example.ui.viewmodel.ClosetViewModel

data class ColorSegment(
    val colorName: String,
    val count: Int,
    val rawColor: Color,
    val percentage: Float,
    val label: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: ClosetViewModel,
    modifier: Modifier = Modifier
) {
    val allItems by viewModel.allItems.collectAsStateWithLifecycle(emptyList())
    val scrollState = rememberScrollState()

    // 1. Calculations
    val totalItems = allItems.size

    // Color grouping calculations
    val colorSegments = remember(allItems) {
        if (allItems.isEmpty()) emptyList()
        else {
            val grouped = allItems.groupBy { it.color }
            val sorted = grouped.map { (colorName, items) ->
                val count = items.size
                val pct = count.toFloat() / totalItems
                val swatchColor = getColorFromName(colorName)
                ColorSegment(
                    colorName = colorName,
                    count = count,
                    rawColor = swatchColor,
                    percentage = pct,
                    label = translateColor(colorName)
                )
            }.sortedByDescending { it.count }
            sorted
        }
    }

    // Category grouping calculations (Tops, Bottoms, Outerwear, Shoes, Accessories)
    val categoryStats = remember(allItems) {
        val total = totalItems.coerceAtLeast(1)
        val categories = ClothingItem.ALL_CATEGORIES
        categories.map { category ->
            val count = allItems.count { it.category == category }
            val pct = count.toFloat() / total
            category to Pair(count, pct)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("衣橱美学多维分析", fontWeight = FontWeight.Bold, fontSize = 21.sp)
                        Text(
                            "用量化的科学色彩比例，解构您的日常穿搭基因",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            if (totalItems == 0) {
                // Beautiful empty state centered
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            RoundedCornerShape(24.dp)
                        )
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.InsertChartOutlined,
                            contentDescription = "Empty Charts",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "暂无统计数据哦！",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "请切换至“我的衣橱”分页添加几件美衣，或者点击衣橱首页顶部的模拟拍照快速填充预设物品后，AI统计看板将在此为您分析色彩配比与消费建议！",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            } else {
                // 1. Top Metrics Cards Grid
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Card A: Total Items
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                               ) {
                                    Icon(
                                        imageVector = Icons.Default.AllInbox,
                                        contentDescription = "Total",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "收录总量",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$totalItems",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("件单品", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
                            }
                        }
                    }

                    // Card B: Primary Category
                    val primeCategory = remember(categoryStats) {
                        categoryStats.maxByOrNull { it.second.first }?.let {
                            if (it.second.first > 0) translateCategory(it.first) else "--"
                        } ?: "--"
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(
                                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Checkroom,
                                        contentDescription = "Prime Category",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "主力品类",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = primeCategory,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Card C: Dominant Color
                    val dominantColorName = colorSegments.firstOrNull()?.label ?: "--"
                    val dominantColorVal = colorSegments.firstOrNull()?.rawColor ?: Color.Transparent
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(
                                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = "Dominant Color",
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "最爱色系",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (dominantColorVal != Color.Transparent) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(dominantColorVal)
                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = dominantColorName,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // 2. Section: Donut Pie Color Chart
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Color Distribution",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "🎨 衣橱色彩分布",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "了解偏好，拯救永远在买同一种颜色衣服的自己",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 28.dp, bottom = 20.dp)
                        )

                        // Layout: Left: Circular Donut Chart. Right: Swatches List
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Part: Circle Donut Drawing
                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    var currentAngle = -90f
                                    if (colorSegments.isEmpty()) {
                                        drawArc(
                                            color = Color.LightGray,
                                            startAngle = 0f,
                                            sweepAngle = 360f,
                                            useCenter = false,
                                            style = Stroke(width = 38.dp.toPx())
                                        )
                                    } else {
                                        colorSegments.forEach { segment ->
                                            val sweep = segment.percentage * 360f
                                            drawArc(
                                                color = segment.rawColor,
                                                startAngle = currentAngle,
                                                sweepAngle = sweep,
                                                useCenter = false,
                                                style = Stroke(width = 38.dp.toPx())
                                            )
                                            currentAngle += sweep
                                        }
                                    }
                                }

                                // Center Label
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${colorSegments.size}",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = "色系构成",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Right Part: Swatches Scroll Column
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "色彩占比排行榜",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )

                                // Show top 4 color groups, plus "other" if more exist
                                val displaySegments = colorSegments.take(4)
                                displaySegments.forEach { segment ->
                                    val percentLabel = String.format("%.1f%%", segment.percentage * 100)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clip(CircleShape)
                                                    .background(segment.rawColor)
                                                    .border(
                                                        1.dp,
                                                        MaterialTheme.colorScheme.outlineVariant,
                                                        CircleShape
                                                    )
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = segment.label,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        Text(
                                            text = "$percentLabel (${segment.count}件)",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (colorSegments.size > 4) {
                                    val otherCount = colorSegments.drop(4).sumOf { it.count }
                                    val otherPct = otherCount.toFloat() / totalItems
                                    val otherPercentLabel = String.format("%.1f%%", otherPct * 100)
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFE2DDD5))
                                                    .border(
                                                        1.dp,
                                                        MaterialTheme.colorScheme.outlineVariant,
                                                        CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("•", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "其他色彩",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Text(
                                            text = "$otherPercentLabel (${otherCount}件)",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Section: Category Proportion progress bars
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = "Category Proportion",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "👕 品类配比比重",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "直观对照上衣、长裤、外套的配比，帮您理性规划采购计划",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 28.dp, bottom = 20.dp)
                        )

                        // List of categories with styled bars
                        Column(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            categoryStats.forEach { (category, stats) ->
                                val (count, pct) = stats
                                val pctLabel = String.format("%.1f%%", pct * 100)
                                val chiLabel = translateCategory(category)

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = chiLabel,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Text(
                                            text = "$count 件 / $pctLabel",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Custom visual progress bar: Alternate green and purple for premium Gucci visual vibe!
                                    val isPurpleBar = category == ClothingItem.CATEGORY_BOTTOMS || category == ClothingItem.CATEGORY_ACCESSORIES
                                    val barColor = if (isPurpleBar) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                    val trackColor = if (isPurpleBar) Color(0xFFECE5F5) else Color(0xFFE4EDE5)

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(10.dp)
                                            .clip(CircleShape)
                                            .background(trackColor)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(pct.coerceIn(0.01f, 1f))
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(
                                                            barColor.copy(alpha = 0.7f),
                                                            barColor
                                                        )
                                                    )
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Smart Wardrobe Insight & Shopping Plan advice box
                val insightText = remember(allItems) {
                    val countsMap = allItems.groupBy { it.category }.mapValues { it.value.size }
                    val tops = countsMap[ClothingItem.CATEGORY_TOPS] ?: 0
                    val bottoms = countsMap[ClothingItem.CATEGORY_BOTTOMS] ?: 0
                    val outerwear = countsMap[ClothingItem.CATEGORY_OUTERBODY] ?: 0
                    val shoes = countsMap[ClothingItem.CATEGORY_SHOES] ?: 0
                    val accessories = countsMap[ClothingItem.CATEGORY_ACCESSORIES] ?: 0

                    when {
                        totalItems < 5 -> {
                            "您目前的衣橱刚起步，AI统计雷达还在收集时尚因子。建议再录入 3 件以上单品，解锁专业的个性化穿衣穿搭配比分析评估建议！"
                        }
                        tops > bottoms * 3 && bottoms > 0 -> {
                            "💡 【衣橱预警】上衣多、下装少！目前您的上衣储备几乎是裤装下装的 ${String.format("%.1f", tops.toFloat()/bottoms)} 倍。您可能经常陷入「衣服很多却选不出一件得体搭配」的状态，因为缺少百搭的下装组合。强烈建议您下次购物时，将预算重点向大地色、黑色等高品质长裤或裙装倾斜，平衡衣架配比！"
                        }
                        bottoms > tops * 2 && tops > 0 -> {
                            "💡 【穿搭平衡建议】您的下摆库容相当雄厚，但上衣样式偏少。可以考虑多添置一些具有设计感、色彩纯净的针织打底、利落衬衫或舒适T恤，能极大提升您已有下装的利用概率和搭配自由度。"
                        }
                        outerwear == 0 -> {
                            "💡 【采购建议】您的衣橱还没有录入「外套」分类的服装哦。外套是一个高阶穿搭的灵魂，适合制造穿衣的层次感。计划下一次购物清单时，不妨将一卷有质感的风衣、休闲夹克或羊毛大衣列入必选星标吧。"
                        }
                        shoes == 0 -> {
                            "💡 【鞋履贴士】您已经录入了很多好看的衣服，但还没有录入鞋子。一套出色的搭配必然需要鞋子画龙点睛，记得把您的运动鞋、马丁靴或德训鞋也拍照录入衣橱，好让 Gemini 穿搭师向您交出更加完整的造型提案！"
                        }
                        else -> {
                            "✨ 【穿搭黄金比例】您的衣架品类呈现非常健康的生态！上衣、裤装、外套、鞋履的配比均匀饱满，处于 1.5 - 2 倍上装率的经典黄金区间。近期您可以专注于色彩融合研究，无需盲目添置日常衣物，理性省钱的同时穿得更美学精致！"
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 36.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Planner Advice",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "🧠 智能衣橱购物采购指南",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = insightText,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 19.sp
                        )
                    }
                }
            }
        }
    }
}
