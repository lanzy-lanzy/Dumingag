package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.model.Attraction
import com.example.model.AttractionCategory
import com.example.model.CategoryCatalog
import com.example.model.ManagedCategory
import com.example.model.UiState
import com.example.ui.components.AttractionCoverImage
import com.example.ui.components.AttractionSearchBar
import com.example.ui.components.ManagedCategoryFilterChips
import com.example.ui.components.liquidGlassBorder
import com.example.ui.components.liquidGlassCardColors
import com.example.viewmodel.AttractionsViewModel

@Composable
fun AttractionsListScreen(
    viewModel: AttractionsViewModel,
    language: AppLanguage,
    managedCategories: List<ManagedCategory> = CategoryCatalog.defaultCategories,
    favoriteIds: Set<String> = emptySet(),
    onToggleFavorite: (Attraction) -> Unit = {},
    onAttractionClick: (Attraction) -> Unit,
    onNavigateOffline: (Attraction) -> Unit,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedCategoryCode by viewModel.selectedCategoryCode.collectAsStateWithLifecycle()
    val savedIds by viewModel.savedAttractionIds.collectAsStateWithLifecycle()
    val activeCategories = managedCategories.ifEmpty { CategoryCatalog.defaultCategories }

    Column(modifier = Modifier.fillMaxSize()) {
        // Directory Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("attractions_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Dumingag Attractions",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Offline Room Directory • Coordinates & Guides",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            if (uiState is UiState.Success) {
                val count = (uiState as UiState.Success<List<Attraction>>).data.size
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "$count spots",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // Real-time Room Search Bar & Chip Filter Components
        val resultCount = (uiState as? UiState.Success<List<Attraction>>)?.data?.size
        AttractionSearchBar(
            query = searchQuery,
            onQueryChange = viewModel::setSearchQuery,
            selectedCategory = selectedCategory,
            onCategorySelected = viewModel::setCategory,
            language = language,
            resultCount = resultCount,
            showCategoryChips = false,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        ManagedCategoryFilterChips(
            selectedCategoryCode = selectedCategoryCode,
            onCategoryCodeSelected = viewModel::setCategoryCode,
            language = language,
            managedCategories = activeCategories,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        // Content based on Room UiState
        when (val state = uiState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = viewModel::refreshAttractions,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Retry", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            is UiState.Success -> {
                val items = state.data
                if (items.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No attractions match your filter",
                            style = MaterialTheme.typography.titleMedium
                        )
                        TextButton(
                            onClick = {
                                viewModel.setSearchQuery("")
                                viewModel.setCategoryCode("ALL")
                            },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Show All Attractions", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(items, key = { it.id }) { attraction ->
                            AttractionDirectoryRowCard(
                                attraction = attraction,
                                categoryLabel = AppStrings.categoryLabelForCode(attraction.categoryCode, activeCategories, language),
                                language = language,
                                isSavedOffline = savedIds.contains(attraction.id),
                                isFavorite = favoriteIds.contains(attraction.id),
                                onClick = { onAttractionClick(attraction) },
                                onNavigateOffline = { onNavigateOffline(attraction) },
                                onToggleSaveOffline = { viewModel.toggleSavedOffline(attraction) },
                                onToggleFavorite = { onToggleFavorite(attraction) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttractionDirectoryRowCard(
    attraction: Attraction,
    categoryLabel: String,
    language: AppLanguage,
    isSavedOffline: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onNavigateOffline: () -> Unit,
    onToggleSaveOffline: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("directory_attraction_${attraction.id}"),
        shape = RoundedCornerShape(22.dp),
        colors = liquidGlassCardColors(),
        border = liquidGlassBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AttractionCoverImage(
                    attraction = attraction,
                    contentDescription = attraction.name.get(language),
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = categoryLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${attraction.baseRating}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = attraction.name.get(language),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Brgy. ${attraction.barangay} • ${"%.4f".format(attraction.latitude)}°N, ${"%.4f".format(attraction.longitude)}°E",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Text(
                text = attraction.description.get(language),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = attraction.entryFee.get(language),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("directory_favorite_${attraction.id}")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (isFavorite) Color(0xFFE11D48) else MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onToggleSaveOffline,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("directory_save_${attraction.id}")
                    ) {
                        Icon(
                            imageVector = if (isSavedOffline) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = AppStrings.saveOffline(language),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    TextButton(
                        onClick = onNavigateOffline,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("directory_nav_${attraction.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Directions,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = AppStrings.navigateOffline(language),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
