package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DumingagSeedData
import com.example.data.SmartTravelEngines
import com.example.data.WeatherSuitabilityTone
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.model.Attraction
import com.example.model.CategoryCatalog
import com.example.model.CommunityReview
import com.example.model.ManagedCategory
import com.example.model.TrailConditionReport
import com.example.model.UiState
import com.example.model.WeatherSnapshot
import com.example.ui.components.AttractionCoverImage
import com.example.ui.components.AttractionReviewsSection
import com.example.ui.components.ComposeReviewDialog
import com.example.ui.components.FullScreenPhotoZoomDialog
import com.example.ui.components.TravelPhotoImage
import com.example.ui.components.liquidGlassBorder
import com.example.ui.components.liquidGlassCardColors
import com.example.viewmodel.AttractionDetailViewModel
import java.util.Calendar
import java.util.Locale

@Composable
fun AttractionDetailScreen(
    viewModel: AttractionDetailViewModel,
    language: AppLanguage,
    managedCategories: List<ManagedCategory> = CategoryCatalog.defaultCategories,
    isFavorite: Boolean = false,
    reviews: List<CommunityReview> = emptyList(),
    currentUserId: String = "",
    isAdmin: Boolean = false,
    weatherSnapshot: WeatherSnapshot? = null,
    galleryPhotosOverride: List<String> = emptyList(),
    onToggleFavorite: () -> Unit = {},
    onSubmitReview: (Attraction, Int, String, String, String?) -> Unit = { _, _, _, _, _ -> },
    onUpdateReview: (String, Int, String, String, String?, Boolean) -> Unit = { _, _, _, _, _, _ -> },
    onDeleteReview: (String) -> Unit = {},
    onToggleReviewHelpful: (CommunityReview) -> Unit = {},
    onAdminReplyReview: (String, String) -> Unit = { _, _ -> },
    onBackClick: () -> Unit,
    onNavigateOffline: (Attraction) -> Unit,
    onAddToItinerary: (Attraction, Long, String, Boolean) -> Unit,
    onShowMessage: (String) -> Unit,
    isCheckedIn: Boolean = false,
    userLatLon: Pair<Double, Double> = DumingagSeedData.CENTER_LAT to DumingagSeedData.CENTER_LON,
    trailReports: List<TrailConditionReport> = emptyList(),
    onCheckIn: (Attraction) -> Unit = {},
    onPostTrailReport: (attractionId: String, conditionTag: String, crowdLevel: String, note: String) -> Unit = { _, _, _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSavedOffline by viewModel.isSavedOffline.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is UiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.testTag("detail_loading_indicator"))
            }
        }

        is UiState.Error -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onBackClick,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Back", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Button(
                        onClick = viewModel::ensureSeeded,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Retry", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        is UiState.Success -> {
            val mergedAttraction = remember(state.data, galleryPhotosOverride) {
                if (state.data.galleryPhotos.isEmpty() && galleryPhotosOverride.isNotEmpty()) {
                    state.data.copy(galleryPhotos = galleryPhotosOverride)
                } else {
                    state.data
                }
            }
            val attractionReviews = remember(reviews, mergedAttraction.id) {
                reviews.filter { it.attractionId == mergedAttraction.id }
            }
            AttractionDetailContent(
                attraction = mergedAttraction,
                language = language,
                managedCategories = managedCategories,
                isSavedOffline = isSavedOffline,
                isFavorite = isFavorite,
                reviews = attractionReviews,
                currentUserId = currentUserId,
                isAdmin = isAdmin,
                weatherSnapshot = weatherSnapshot,
                onBackClick = onBackClick,
                onToggleSaveOffline = viewModel::toggleSavedOffline,
                onToggleFavorite = onToggleFavorite,
                onNavigateOffline = { onNavigateOffline(mergedAttraction) },
                onAddToItinerary = { millis, notes, reminder ->
                    onAddToItinerary(mergedAttraction, millis, notes, reminder)
                },
                onSubmitReview = { rating, comment, tag, photoData ->
                    onSubmitReview(mergedAttraction, rating, comment, tag, photoData)
                },
                onUpdateReview = onUpdateReview,
                onDeleteReview = onDeleteReview,
                onToggleReviewHelpful = onToggleReviewHelpful,
                onAdminReplyReview = onAdminReplyReview,
                onShowMessage = onShowMessage,
                isCheckedIn = isCheckedIn,
                userLatLon = userLatLon,
                trailReports = trailReports,
                onCheckIn = { onCheckIn(mergedAttraction) },
                onPostTrailReport = { tag, crowd, note ->
                    onPostTrailReport(mergedAttraction.id, tag, crowd, note)
                }
            )
        }
    }
}

@Composable
private fun AttractionDetailContent(
    attraction: Attraction,
    language: AppLanguage,
    managedCategories: List<ManagedCategory> = CategoryCatalog.defaultCategories,
    isSavedOffline: Boolean,
    isFavorite: Boolean,
    reviews: List<CommunityReview>,
    currentUserId: String,
    isAdmin: Boolean = false,
    weatherSnapshot: WeatherSnapshot? = null,
    onBackClick: () -> Unit,
    onToggleSaveOffline: () -> Unit,
    onToggleFavorite: () -> Unit,
    onNavigateOffline: () -> Unit,
    onAddToItinerary: (Long, String, Boolean) -> Unit,
    onSubmitReview: (Int, String, String, String?) -> Unit,
    onUpdateReview: (String, Int, String, String, String?, Boolean) -> Unit,
    onDeleteReview: (String) -> Unit,
    onToggleReviewHelpful: (CommunityReview) -> Unit = {},
    onAdminReplyReview: (String, String) -> Unit = { _, _ -> },
    onShowMessage: (String) -> Unit,
    isCheckedIn: Boolean = false,
    userLatLon: Pair<Double, Double> = DumingagSeedData.CENTER_LAT to DumingagSeedData.CENTER_LON,
    trailReports: List<TrailConditionReport> = emptyList(),
    onCheckIn: () -> Unit = {},
    onPostTrailReport: (String, String, String) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    var showItineraryDialog by remember { mutableStateOf(false) }
    var showReviewDialog by remember { mutableStateOf(false) }
    var editingReview by remember { mutableStateOf<CommunityReview?>(null) }
    var zoomedEncodedPhoto by remember { mutableStateOf<String?>(null) }

    // Offline Text-to-Speech Audio Guide
    var isSpeaking by remember { mutableStateOf(false) }
    var ttsReady by remember { mutableStateOf(false) }
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(context) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) { isSpeaking = true }
                    override fun onDone(utteranceId: String?) { isSpeaking = false }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) { isSpeaking = false }
                })
            }
        }
        ttsEngine = engine
        onDispose {
            engine.stop()
            engine.shutdown()
            ttsEngine = null
            isSpeaking = false
        }
    }

    val toggleAudioGuide: () -> Unit = {
        val engine = ttsEngine
        if (engine == null || !ttsReady) {
            onShowMessage("Offline voice narration is initializing. Try again in a moment.")
        } else if (isSpeaking) {
            engine.stop()
            isSpeaking = false
        } else {
            val targetLocale = when (language) {
                AppLanguage.ES -> Locale("es", "ES")
                AppLanguage.TL, AppLanguage.CEB -> Locale("fil", "PH")
                AppLanguage.EN -> Locale.US
            }
            val res = engine.setLanguage(targetLocale)
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                engine.setLanguage(Locale.US)
            }
            val script = buildString {
                append(attraction.name.get(language))
                append(". ")
                append(attraction.subtitle.get(language))
                append(". ")
                append(attraction.description.get(language))
                append(" Heritage story: ")
                append(attraction.historyNote.get(language))
                append(" Local transit tip: ")
                append(attraction.transitHint.get(language))
            }
            engine.speak(script, TextToSpeech.QUEUE_FLUSH, null, "dumingag_audio_${attraction.id}")
            isSpeaking = true
        }
    }

    // Build swipeable gallery: page 0 is always the cover (encoded or drawable), followed by extra galleryPhotos
    val extraGalleryPhotos = remember(attraction.photoData, attraction.galleryPhotos) {
        attraction.galleryPhotos.filter { it.isNotBlank() && it != attraction.photoData }
    }
    val totalHeroPages = 1 + extraGalleryPhotos.size
    val heroPagerState = rememberPagerState(pageCount = { totalHeroPages })

    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val weatherBadge = remember(attraction, weatherSnapshot, currentHour) {
        SmartTravelEngines.evaluateWeatherSuitability(attraction, weatherSnapshot, currentHour)
    }

    val formattedCoords = "${"%.5f".format(attraction.latitude)}° N, ${"%.5f".format(attraction.longitude)}° E"
    val categoryLabel = AppStrings.categoryLabelForCode(attraction.categoryCode, managedCategories, language)
    val ratingText = if (reviews.isNotEmpty()) {
        val avg = reviews.map { it.rating }.average()
        "%.1f (%d)".format(avg, reviews.size)
    } else {
        "${attraction.baseRating} / 5.0"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Immersive Multi-Photo Swipeable Hero Carousel with Gradient Scrim & Category/Rating Pills
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(308.dp)
        ) {
            HorizontalPager(
                state = heroPagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("detail_photo_pager")
            ) { page ->
                if (page == 0) {
                    AttractionCoverImage(
                        attraction = attraction,
                        contentDescription = attraction.name.get(language),
                        modifier = Modifier
                            .fillMaxSize()
                            .let { mod ->
                                if (attraction.photoData.isNotBlank()) {
                                    mod.clickable { zoomedEncodedPhoto = attraction.photoData }
                                } else {
                                    mod
                                }
                            },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val photoItem = extraGalleryPhotos[page - 1]
                    TravelPhotoImage(
                        photoData = photoItem,
                        contentDescription = "${attraction.name.get(language)} photo ${page + 1}",
                        contentScale = ContentScale.Crop,
                        fallbackDrawableRes = attraction.imageRes,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { zoomedEncodedPhoto = photoItem }
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.42f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.58f)
                            )
                        )
                    )
            )
            if (totalHeroPages > 1) {
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 86.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        repeat(totalHeroPages) { index ->
                            Box(
                                modifier = Modifier
                                    .size(if (heroPagerState.currentPage == index) 7.dp else 5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (heroPagerState.currentPage == index) Color.White else Color.White.copy(alpha = 0.45f)
                                    )
                            )
                        }
                        Text(
                            text = "${heroPagerState.currentPage + 1}/$totalHeroPages",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }

            // Top Navigation & Bookmark Overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                    shape = CircleShape,
                    border = liquidGlassBorder(1.dp),
                    tonalElevation = 4.dp
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to attractions",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                        shape = CircleShape,
                        border = liquidGlassBorder(1.dp),
                        tonalElevation = 4.dp
                    ) {
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("detail_favorite_button")
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                                tint = if (isFavorite) Color(0xFFE11D48) else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                        shape = CircleShape,
                        border = liquidGlassBorder(1.dp),
                        tonalElevation = 4.dp
                    ) {
                        IconButton(
                            onClick = onToggleSaveOffline,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("detail_top_bookmark_button")
                        ) {
                            Icon(
                                imageVector = if (isSavedOffline) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = if (isSavedOffline) AppStrings.savedOffline(language) else AppStrings.saveOffline(language),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Bottom Overlay Chips positioned comfortably above the rounded sheet lip
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.testTag("detail_category_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = categoryLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = ratingText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Seamless Full-Width Overlapping Content Sheet (uniform surface, zero seam or side edges)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 276.dp),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Subtle centered iOS sheet grabber handle
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(38.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.24f))
                    )
                }

                // Title, Subtitle, Weather-Smart Badge & Offline Audio Guide
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = attraction.name.get(language),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("detail_title")
                    )
                    Text(
                        text = attraction.subtitle.get(language),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    val badgeContainer = when (weatherBadge.tone) {
                        WeatherSuitabilityTone.GREAT -> MaterialTheme.colorScheme.primaryContainer
                        WeatherSuitabilityTone.RAIN_FRIENDLY -> MaterialTheme.colorScheme.secondaryContainer
                        WeatherSuitabilityTone.MORNING_PICK -> MaterialTheme.colorScheme.tertiaryContainer
                        WeatherSuitabilityTone.TRAIL_CAUTION -> MaterialTheme.colorScheme.errorContainer
                    }
                    val badgeContent = when (weatherBadge.tone) {
                        WeatherSuitabilityTone.GREAT -> MaterialTheme.colorScheme.onPrimaryContainer
                        WeatherSuitabilityTone.RAIN_FRIENDLY -> MaterialTheme.colorScheme.onSecondaryContainer
                        WeatherSuitabilityTone.MORNING_PICK -> MaterialTheme.colorScheme.onTertiaryContainer
                        WeatherSuitabilityTone.TRAIL_CAUTION -> MaterialTheme.colorScheme.onErrorContainer
                    }
                    Surface(
                        color = badgeContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("detail_weather_suitability_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = null,
                                tint = badgeContent,
                                modifier = Modifier.size(16.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = weatherBadge.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeContent
                                )
                                Text(
                                    text = weatherBadge.reason,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = badgeContent.copy(alpha = 0.88f)
                                )
                            }
                        }
                    }

                    FilledTonalButton(
                        onClick = toggleAudioGuide,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("detail_audio_guide_button")
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.StopCircle else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isSpeaking) "Stop audio guide" else "Listen to offline audio guide",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSpeaking) "Stop Audio Guide Narration" else "Listen to Offline Audio Guide",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Coordinates & Location Card (from Room)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("detail_coordinates_card"),
                    shape = RoundedCornerShape(22.dp),
                    border = liquidGlassBorder(),
                    colors = liquidGlassCardColors(
                        baseColor = MaterialTheme.colorScheme.primaryContainer,
                        lightAlpha = 0.70f,
                        darkAlpha = 0.66f
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Brgy. ${attraction.barangay}, Dumingag",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            IconButton(
                                onClick = {
                                    val rawCoords = "${attraction.latitude}, ${attraction.longitude}"
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Coordinates", rawCoords))
                                    onShowMessage("Copied coordinates: $rawCoords")
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("detail_copy_coords_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy GPS coordinates",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "GPS COORDINATES",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formattedCoords,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("detail_coordinates_text")
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "CATEGORY",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = categoryLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.testTag("detail_category_text")
                                )
                            }
                        }
                    }
                }

                // Full Description Section
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Overview & Guide",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = attraction.description.get(language),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("detail_description_text")
                    )
                }

                // Visitor & Heritage Details Card
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(22.dp),
                    border = liquidGlassBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DetailScreenInfoRow(
                            icon = Icons.Default.HistoryEdu,
                            label = "Heritage & Story",
                            value = attraction.historyNote.get(language)
                        )
                        DetailScreenInfoRow(
                            icon = Icons.Default.AccessTime,
                            label = "Open Hours & Best Time",
                            value = "${attraction.openHours} • ${attraction.bestTime.get(language)}"
                        )
                        DetailScreenInfoRow(
                            icon = Icons.Default.Paid,
                            label = "Entry Fee",
                            value = attraction.entryFee.get(language)
                        )
                        DetailScreenInfoRow(
                            icon = Icons.Default.DirectionsBus,
                            label = "Local Transit Tip",
                            value = attraction.transitHint.get(language)
                        )
                    }
                }

                // Responsive Action Buttons
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val stackButtons = maxWidth < 340.dp
                    if (stackButtons) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onNavigateOffline,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("detail_screen_navigate_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Directions,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = AppStrings.navigateOffline(language),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            FilledTonalButton(
                                onClick = { showItineraryDialog = true },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("detail_screen_itinerary_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = AppStrings.addToItinerary(language),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onNavigateOffline,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("detail_screen_navigate_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Directions,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = AppStrings.navigateOffline(language),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            FilledTonalButton(
                                onClick = { showItineraryDialog = true },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("detail_screen_itinerary_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = AppStrings.addToItinerary(language),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = onToggleSaveOffline,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(50))
                        .testTag("detail_screen_save_button")
                ) {
                    Icon(
                        imageVector = if (isSavedOffline) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSavedOffline) AppStrings.savedOffline(language) else AppStrings.saveOffline(language),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                AttractionEcoCheckInAndPostcardRow(
                    attraction = attraction,
                    language = language,
                    isCheckedIn = isCheckedIn,
                    userLatLon = userLatLon,
                    onCheckIn = onCheckIn,
                    onShowMessage = onShowMessage
                )

                TrailConditionReportsCard(
                    attractionId = attraction.id,
                    attractionName = attraction.name.get(language),
                    reports = trailReports,
                    onSubmitReport = onPostTrailReport
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                AttractionReviewsSection(
                    attractionName = attraction.name.get(language),
                    reviews = reviews,
                    currentUserId = currentUserId,
                    isAdmin = isAdmin,
                    onWriteReview = { showReviewDialog = true },
                    onEditReview = { editingReview = it },
                    onDeleteReview = onDeleteReview,
                    onToggleHelpful = onToggleReviewHelpful,
                    onAdminReply = onAdminReplyReview
                )

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (showItineraryDialog) {
        AddToItineraryDialog(
            attractionName = attraction.name.get(language),
            onDismiss = { showItineraryDialog = false },
            onConfirm = { daysFromNow, notes, reminder ->
                val plannedMillis = System.currentTimeMillis() + daysFromNow * 24L * 60L * 60L * 1000L
                onAddToItinerary(plannedMillis, notes, reminder)
                showItineraryDialog = false
            }
        )
    }

    if (showReviewDialog) {
        ComposeReviewDialog(
            language = language,
            attractions = listOf(attraction),
            initialReview = null,
            initialAttraction = attraction,
            onDismiss = { showReviewDialog = false },
            onSave = { _, rating, comment, tag, photoData ->
                onSubmitReview(rating, comment, tag, photoData)
                showReviewDialog = false
            }
        )
    }

    editingReview?.let { rev ->
        ComposeReviewDialog(
            language = language,
            attractions = listOf(attraction),
            initialReview = rev,
            initialAttraction = attraction,
            onDismiss = { editingReview = null },
            onSave = { _, rating, comment, tag, photoData ->
                val clearPhoto = photoData == null && rev.photoData.isNotBlank()
                onUpdateReview(rev.id, rating, comment, tag, photoData, clearPhoto)
                editingReview = null
            }
        )
    }

    zoomedEncodedPhoto?.let { raw ->
        FullScreenPhotoZoomDialog(
            photoData = raw,
            caption = attraction.name.get(language),
            onDismiss = { zoomedEncodedPhoto = null }
        )
    }
}

@Composable
private fun DetailScreenInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
