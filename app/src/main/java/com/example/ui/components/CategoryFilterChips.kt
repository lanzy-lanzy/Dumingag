package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.model.AttractionCategory

@Composable
fun CategoryFilterChips(
    selectedCategory: AttractionCategory,
    onCategorySelected: (AttractionCategory) -> Unit,
    language: AppLanguage,
    categories: List<AttractionCategory> = AttractionCategory.entries,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .testTag("category_filter_chips_row"),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories, key = { it.name }) { category ->
            val isSelected = category == selectedCategory
            val label = AppStrings.categoryLabel(category, language)
            val icon = categoryIcon(category)

            FilterChip(
                selected = isSelected,
                onClick = {
                    val nextCategory = if (isSelected && category != AttractionCategory.ALL) {
                        AttractionCategory.ALL
                    } else {
                        category
                    }
                    onCategorySelected(nextCategory)
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (isSelected && category != AttractionCategory.ALL) {
                            Icons.Default.Check
                        } else {
                            icon
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                shape = RoundedCornerShape(50),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }
                ),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    iconColor = MaterialTheme.colorScheme.primary,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("directory_chip_${category.name.lowercase()}")
            )
        }
    }
}

private fun categoryIcon(category: AttractionCategory): ImageVector = when (category) {
    AttractionCategory.ALL -> Icons.Default.Apps
    AttractionCategory.HISTORICAL -> Icons.Default.AccountBalance
    AttractionCategory.NATURE -> Icons.Default.Forest
    AttractionCategory.CULTURAL -> Icons.Default.Palette
    AttractionCategory.LANDMARK -> Icons.Default.LocationCity
    AttractionCategory.ORGANIC_FARM -> Icons.Default.Eco
}
