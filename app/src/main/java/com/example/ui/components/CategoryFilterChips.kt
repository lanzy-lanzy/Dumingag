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
import androidx.compose.material.icons.filled.Cabin
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Water
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
import com.example.model.CategoryCatalog
import com.example.model.ManagedCategory

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

@Composable
fun ManagedCategoryFilterChips(
    selectedCategoryCode: String,
    onCategoryCodeSelected: (String) -> Unit,
    language: AppLanguage,
    managedCategories: List<ManagedCategory> = CategoryCatalog.defaultCategories,
    modifier: Modifier = Modifier
) {
    val activeCategories = managedCategories.ifEmpty { CategoryCatalog.defaultCategories }
    val normalizedSelected = selectedCategoryCode.trim().uppercase().ifBlank { "ALL" }

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .testTag("category_filter_chips_row"),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "ALL") {
            val isSelected = normalizedSelected == "ALL"
            FilterChip(
                selected = isSelected,
                onClick = { onCategoryCodeSelected("ALL") },
                label = {
                    Text(
                        text = AppStrings.categoryLabel(AttractionCategory.ALL, language),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Apps,
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
                    .testTag("directory_chip_all")
            )
        }

        items(activeCategories, key = { it.effectiveCode }) { cat ->
            val code = cat.effectiveCode
            val isSelected = normalizedSelected == code
            val label = AppStrings.categoryLabelForCode(code, activeCategories, language)
            val icon = categoryIconForKey(cat.iconKey)

            FilterChip(
                selected = isSelected,
                onClick = {
                    val nextCode = if (isSelected) "ALL" else code
                    onCategoryCodeSelected(nextCode)
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
                        imageVector = if (isSelected) Icons.Default.Check else icon,
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
                    .testTag("directory_chip_${code.lowercase()}")
            )
        }
    }
}

fun categoryIconForKey(iconKey: String): ImageVector = when (iconKey.trim().lowercase()) {
    "forest", "nature" -> Icons.Default.Forest
    "history", "historical" -> Icons.Default.AccountBalance
    "palette", "cultural" -> Icons.Default.Palette
    "landmark", "city" -> Icons.Default.LocationCity
    "eco", "organic_farm", "farm" -> Icons.Default.Eco
    "water", "waterfall", "waterfalls" -> Icons.Default.Water
    "mountain", "highland" -> Icons.Default.Terrain
    "camp", "resort" -> Icons.Default.Cabin
    "restaurant", "food", "cafe" -> Icons.Default.Restaurant
    "camera", "viewpoint" -> Icons.Default.CameraAlt
    else -> Icons.Default.Category
}

private fun categoryIcon(category: AttractionCategory): ImageVector = when (category) {
    AttractionCategory.ALL -> Icons.Default.Apps
    AttractionCategory.HISTORICAL -> Icons.Default.AccountBalance
    AttractionCategory.NATURE -> Icons.Default.Forest
    AttractionCategory.CULTURAL -> Icons.Default.Palette
    AttractionCategory.LANDMARK -> Icons.Default.LocationCity
    AttractionCategory.ORGANIC_FARM -> Icons.Default.Eco
}
