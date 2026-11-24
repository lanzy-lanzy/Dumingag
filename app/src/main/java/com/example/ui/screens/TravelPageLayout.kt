package com.example.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SplashGlassPanelBottom
import com.example.ui.theme.SplashGlassPanelTop
import com.example.ui.theme.SplashGlassSpecularEdge
import com.example.ui.theme.SplashGlassSpecularMid
import com.example.ui.theme.SplashGlassSpecularTop
import com.example.ui.theme.SplashGlassTextPrimary
import com.example.ui.theme.SplashGlassTextSecondary
import com.example.ui.theme.SplashGlassThumbTop

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
    val headerShape = RoundedCornerShape(28.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .shadow(12.dp, headerShape, ambientColor = Color.Black.copy(alpha = 0.24f))
            .clip(headerShape)
            .border(
                width = 1.4.dp,
                brush = Brush.linearGradient(
                    colors = listOf(SplashGlassSpecularTop, SplashGlassSpecularMid, SplashGlassSpecularEdge)
                ),
                shape = headerShape
            )
    ) {
        Image(painterResource(photo), null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            SplashGlassPanelBottom.copy(alpha = 0.90f),
                            SplashGlassPanelTop.copy(alpha = 0.78f),
                            SplashGlassPanelBottom.copy(alpha = 0.62f)
                        )
                    )
                )
                .drawBehind {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(SplashGlassSpecularTop.copy(alpha = 0.20f), Color.Transparent),
                            startY = 0f,
                            endY = size.height * 0.40f
                        ),
                        cornerRadius = CornerRadius(28.dp.toPx(), 28.dp.toPx())
                    )
                }
        )
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = SplashGlassTextPrimary
            )
            summary?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SplashGlassTextSecondary
                )
            }
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
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = SplashGlassThumbTop,
            contentColor = EmeraldPrimary
        ),
        border = BorderStroke(1.1.dp, SplashGlassSpecularTop),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = Modifier.heightIn(min = 48.dp).testTag(tag)
    ) {
        Icon(icon, null, Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontWeight = FontWeight.Bold)
    }
}
