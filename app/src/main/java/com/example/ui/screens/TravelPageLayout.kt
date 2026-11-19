package com.example.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
internal fun TravelPageGrid(tag: String, content: LazyGridScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val margin = if (maxWidth >= 600.dp) 24.dp else 12.dp
        LazyVerticalGrid(
            columns = GridCells.Adaptive(320.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)),
            modifier = Modifier.widthIn(max = 1260.dp).fillMaxSize().testTag(tag),
            contentPadding = PaddingValues(margin),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
    }
}

internal fun LazyGridScope.pageSection(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { Column(Modifier.fillMaxWidth()) { content() } }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TravelPageHeader(
    title: String,
    summary: String?,
    @DrawableRes photo: Int,
    actions: @Composable () -> Unit
) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))) {
        Image(painterResource(photo), null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(
            listOf(Color(0xF00C3029), Color(0xAF15382D), Color(0x85112D28))
        )))
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.headlineLarge, color = Color.White)
            summary?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Color.White) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                actions()
            }
        }
    }
}

@Composable
internal fun TravelHeaderAction(label: String, icon: ImageVector, tag: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF133E32)),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = Modifier.heightIn(min = 48.dp).testTag(tag)
    ) {
        Icon(icon, null, Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label)
    }
}
