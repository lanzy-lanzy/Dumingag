package com.example.ui

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import android.net.Uri
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.auth.GoogleAuthHelper
import com.example.data.repository.AttractionRepository
import com.example.data.repository.DumingagCloudRepository
import com.example.data.repository.DumingagOfflineRepository
import com.example.data.repository.WeatherRepository
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.model.AppThemeMode
import com.example.notifications.TravelAlertManager
import com.example.ui.components.AiConciergeSheet
import com.example.ui.components.EditProfileDialog
import com.example.ui.components.LiquidGlassAmbientBackdrop
import com.example.ui.components.LiquidGlassFloatingNavBar
import com.example.ui.components.LiquidGlassNavTab
import com.example.ui.components.LiquidGlassSignOutOverlay
import com.example.ui.components.UserAvatar
import com.example.ui.screens.AdminManagementScreen
import com.example.ui.screens.AttractionDetailScreen
import com.example.ui.screens.AttractionsListScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CommunityReviewsScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.InteractiveMapScreen
import com.example.ui.screens.ItineraryVaultScreen
import com.example.ui.screens.LiquidGlassSplashScreen
import com.example.ui.screens.PreliminarySplashScreen
import com.example.ui.screens.TransitSafetyScreen
import com.example.ui.theme.GlassDarkBorderMid
import com.example.ui.theme.GlassLightHighlight
import com.example.ui.theme.LocalIsDarkTheme
import com.example.viewmodel.AttractionDetailViewModel
import com.example.viewmodel.AttractionsViewModel
import com.example.viewmodel.DumingagViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

object DumingagRoutes {
    const val EXPLORE = "explore"
    const val ATTRACTIONS = "attractions"
    const val ATTRACTION_DETAIL = "attraction_detail/{attractionId}"
    const val MAP = "map"
    const val TRANSIT = "transit"
    const val PLAN = "plan"
    const val REVIEWS = "reviews"
    const val ADMIN = "admin/{initialTab}"

    fun attractionDetail(attractionId: String): String =
        "attraction_detail/${Uri.encode(attractionId)}"

    fun admin(initialTab: Int = 0): String = "admin/$initialTab"
}

private data class NavDestinationItem(
    val route: String,
    val icon: ImageVector,
    val label: (AppLanguage) -> String
)

private val TopDestinations = listOf(
    NavDestinationItem(DumingagRoutes.EXPLORE, Icons.Default.Explore, AppStrings::navExplore),
    NavDestinationItem(DumingagRoutes.MAP, Icons.Default.Map, AppStrings::navMap),
    NavDestinationItem(DumingagRoutes.TRANSIT, Icons.Default.DirectionsBus, AppStrings::navTransit),
    NavDestinationItem(DumingagRoutes.PLAN, Icons.Default.BookmarkAdded, AppStrings::navPlan),
    NavDestinationItem(DumingagRoutes.REVIEWS, Icons.Default.Forum, AppStrings::navReviews)
)

private data class SignedInUser(
    val uid: String, val email: String?, val displayName: String?, val isEmailVerified: Boolean
)

private fun FirebaseUser.toSessionUser() = SignedInUser(uid, email, displayName, isEmailVerified)

private fun FirebaseAuth.authStateFlow(): Flow<SignedInUser?> = callbackFlow {
    val listener = FirebaseAuth.IdTokenListener { auth ->
        trySend(auth.currentUser?.toSessionUser())
    }
    addIdTokenListener(listener)
    awaitClose { removeIdTokenListener(listener) }
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val app = remember(context) { com.google.firebase.FirebaseApp.initializeApp(context) }
    if (app == null) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("Sign-in is unavailable because this build is missing its Firebase configuration.")
        }
        return
    }
    AppNavigation(Firebase.auth)
}

private enum class AppLaunchPhase {
    PRELIMINARY_SPLASH,
    INTERACTIVE_SPLASH,
    MAIN_APP
}

@Composable
fun AppNavigation(
    auth: FirebaseAuth,
    startWithSplash: Boolean = true
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var launchPhase by rememberSaveable {
        mutableStateOf(
            if (startWithSplash) AppLaunchPhase.PRELIMINARY_SPLASH else AppLaunchPhase.MAIN_APP
        )
    }
    var isSigningOut by remember { mutableStateOf(false) }
    var authRefreshTick by remember { mutableIntStateOf(0) }
    val observedUser by remember(auth) { auth.authStateFlow() }
        .collectAsStateWithLifecycle(initialValue = auth.currentUser?.toSessionUser())
    val user = remember(observedUser, authRefreshTick) {
        auth.currentUser?.toSessionUser() ?: observedUser
    }

    val performAnimatedSignOut: () -> Unit = {
        if (!isSigningOut) {
            isSigningOut = true
            scope.launch {
                val minDisplayJob = launch { delay(780L) }
                GoogleAuthHelper.signOut(credentialManager, scope) {
                    scope.launch {
                        minDisplayJob.join()
                        authRefreshTick++
                        isSigningOut = false
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (launchPhase == AppLaunchPhase.MAIN_APP) {
            if (user == null || !user.isEmailVerified) {
                val pendingEmail = user?.email?.takeIf { !user.isEmailVerified && it.isNotBlank() }
                AuthScreen(
                    pendingVerificationEmail = pendingEmail,
                    onAuthSuccess = { authRefreshTick++ },
                    onCheckVerification = {
                        val verified = GoogleAuthHelper.refreshEmailVerificationStatus()
                        if (verified) {
                            authRefreshTick++
                        }
                        verified
                    },
                    onResendVerification = {
                        GoogleAuthHelper.resendVerificationEmail()
                    },
                    onSignOutPendingVerification = performAnimatedSignOut
                )
            } else {
                val displayName = user.displayName?.takeIf { it.isNotBlank() } ?: user.email ?: "Dumingag Traveler"
                key(user.uid, user.email, user.isEmailVerified) {
                    DumingagMainScaffold(
                        currentUserId = user.uid,
                        userDisplayName = displayName,
                        userEmail = user.email.orEmpty(),
                        isEmailVerified = user.isEmailVerified,
                        onRequestSignOut = performAnimatedSignOut
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = launchPhase == AppLaunchPhase.INTERACTIVE_SPLASH,
            enter = fadeIn(animationSpec = tween(420, easing = FastOutSlowInEasing)),
            exit = fadeOut(animationSpec = tween(420, easing = FastOutSlowInEasing)) +
                scaleOut(targetScale = 1.08f, animationSpec = tween(420, easing = FastOutSlowInEasing))
        ) {
            LiquidGlassSplashScreen(
                onEnterApp = { launchPhase = AppLaunchPhase.MAIN_APP }
            )
        }

        AnimatedVisibility(
            visible = launchPhase == AppLaunchPhase.PRELIMINARY_SPLASH,
            enter = fadeIn(animationSpec = tween(220)),
            exit = fadeOut(animationSpec = tween(480, easing = FastOutSlowInEasing)) +
                scaleOut(targetScale = 1.06f, animationSpec = tween(480, easing = FastOutSlowInEasing))
        ) {
            PreliminarySplashScreen(
                onFinished = { launchPhase = AppLaunchPhase.INTERACTIVE_SPLASH }
            )
        }

        LiquidGlassSignOutOverlay(visible = isSigningOut)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DumingagMainScaffold(
    currentUserId: String,
    userDisplayName: String,
    userEmail: String = "",
    isEmailVerified: Boolean = false,
    onRequestSignOut: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val sessionOwner = remember(currentUserId, userEmail, isEmailVerified) {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    DisposableEffect(sessionOwner) {
        onDispose { sessionOwner.viewModelStore.clear() }
    }
    val viewModel: DumingagViewModel = viewModel(
        viewModelStoreOwner = sessionOwner,
        factory = viewModelFactory {
            initializer {
                val app = context.applicationContext as Application
                DumingagViewModel(
                    cloudRepo = DumingagCloudRepository(app),
                    offlineRepo = DumingagOfflineRepository(app),
                    attractionRepo = AttractionRepository(app),
                    weatherRepo = WeatherRepository(app),
                    currentUserId = currentUserId,
                    userDisplayName = userDisplayName,
                    userEmail = userEmail,
                    isEmailVerified = isEmailVerified
                )
            }
        }
    )

    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val effectiveDisplayName by viewModel.effectiveDisplayName.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedCategoryCode by viewModel.selectedCategoryCode.collectAsStateWithLifecycle()
    val managedCategories by viewModel.managedCategories.collectAsStateWithLifecycle()
    val attractions by viewModel.filteredAttractions.collectAsStateWithLifecycle()
    val allAttractions by viewModel.allAttractions.collectAsStateWithLifecycle()
    val emergencyContacts by viewModel.emergencyContacts.collectAsStateWithLifecycle()
    val selectedAttraction by viewModel.selectedAttraction.collectAsStateWithLifecycle()
    val markedAttractions by viewModel.markedAttractions.collectAsStateWithLifecycle()
    val activeRoute by viewModel.activeRoute.collectAsStateWithLifecycle()
    val activeStepIndex by viewModel.activeStepIndex.collectAsStateWithLifecycle()
    val travelMode by viewModel.travelMode.collectAsStateWithLifecycle()
    val userLatLon by viewModel.userLatLon.collectAsStateWithLifecycle()
    val weatherState by viewModel.weatherState.collectAsStateWithLifecycle()
    val isEffectiveOffline by viewModel.isEffectiveOffline.collectAsStateWithLifecycle()
    val forceOfflineMode by viewModel.forceOfflineMode.collectAsStateWithLifecycle()
    val offlinePackReady by viewModel.offlinePackReady.collectAsStateWithLifecycle()
    val savedIds by viewModel.savedAttractionIds.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteAttractionIds.collectAsStateWithLifecycle()
    val favoriteAttractions by viewModel.favoriteAttractions.collectAsStateWithLifecycle()
    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsStateWithLifecycle()
    val savedDocs by viewModel.savedDocuments.collectAsStateWithLifecycle()
    val itinerariesState by viewModel.itinerariesState.collectAsStateWithLifecycle()
    val reviewsState by viewModel.reviewsState.collectAsStateWithLifecycle()
    val publicReviews = (reviewsState as? com.example.model.UiState.Success)?.data.orEmpty()
    val transitState by viewModel.transitState.collectAsStateWithLifecycle()
    val travelGuideCacheStatus by viewModel.travelGuideCacheStatus.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val smartPackingItems by viewModel.smartPackingItems.collectAsStateWithLifecycle()
    val tripExpenses by viewModel.tripExpenses.collectAsStateWithLifecycle()
    val checkedInAttractionIds by viewModel.checkedInAttractionIds.collectAsStateWithLifecycle()
    val trailReports by viewModel.trailReports.collectAsStateWithLifecycle()
    val ecoBadges by viewModel.ecoBadges.collectAsStateWithLifecycle()
    val tileDownloadProgress by viewModel.tileDownloadProgress.collectAsStateWithLifecycle()
    val tourismEvents by viewModel.tourismEvents.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val lastSyncedAtMillis by viewModel.lastSyncedAtMillis.collectAsStateWithLifecycle()
    val aiMessages by viewModel.aiMessages.collectAsStateWithLifecycle()
    val aiStreamingText by viewModel.aiStreamingText.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val aiError by viewModel.aiError.collectAsStateWithLifecycle()
    val aiVoiceState by viewModel.aiVoiceState.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showAiConciergeSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: DumingagRoutes.EXPLORE
    val isDetailRoute = currentRoute.startsWith("attraction_detail")
    val isAdminRoute = currentRoute.startsWith("admin")
    var isMapFullscreen by rememberSaveable { mutableStateOf(false) }
    val showFullscreenChromeHidden = currentRoute == DumingagRoutes.MAP && isMapFullscreen

    LaunchedEffect(currentRoute) {
        if (currentRoute != DumingagRoutes.MAP && isMapFullscreen) {
            isMapFullscreen = false
        }
    }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val darkMode = LocalIsDarkTheme.current
    val glassBorderColor = if (darkMode) GlassDarkBorderMid else GlassLightHighlight.copy(alpha = 0.85f)

    Box(modifier = Modifier.fillMaxSize()) {
    LiquidGlassAmbientBackdrop()
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (!isDetailRoute && !isAdminRoute && !showFullscreenChromeHidden) {
                    val pulseTransition = rememberInfiniteTransition(label = "online_status_pulse")
                    val pulseAlpha by pulseTransition.animateFloat(
                        initialValue = 0.22f,
                        targetValue = 0.62f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "online_dot_halo"
                    )
                    val statusDotColor = if (isEffectiveOffline) {
                        MaterialTheme.colorScheme.secondary
                    } else if (darkMode) {
                        Color(0xFF34D399)
                    } else {
                        Color(0xFF10B981)
                    }
                    val statusPillBg = if (isEffectiveOffline) {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (darkMode) 0.72f else 0.85f)
                    } else {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (darkMode) 0.68f else 0.84f)
                    }
                    val statusPillText = if (isEffectiveOffline) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    }

                    TopAppBar(
                    title = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "Dumingag",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 22.sp,
                                    lineHeight = 25.sp
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                color = statusPillBg,
                                contentColor = statusPillText,
                                border = BorderStroke(
                                    0.9.dp,
                                    statusDotColor.copy(alpha = if (darkMode) 0.45f else 0.35f)
                                ),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { navigateToTab(DumingagRoutes.PLAN) }
                                    .testTag("topbar_connectivity_pill")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.size(10.dp)
                                    ) {
                                        if (!isEffectiveOffline) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(statusDotColor.copy(alpha = pulseAlpha))
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(statusDotColor)
                                        )
                                    }
                                    Text(
                                        text = if (isEffectiveOffline) AppStrings.offlineLabel(language) else AppStrings.onlineLabel(language),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.5.sp,
                                            lineHeight = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = statusPillText,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showAiConciergeSheet = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("topbar_ai_concierge_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Open Dumingag AI Concierge",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (isAdmin) {
                            IconButton(
                                onClick = { navController.navigate(DumingagRoutes.admin(0)) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("topbar_admin_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = "Open admin console",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        IconButton(
                            onClick = { showThemeDialog = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("theme_picker_button")
                        ) {
                            Icon(
                                imageVector = if (darkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Switch glass theme",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = { showLanguageDialog = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("language_picker_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = "Select language",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = { showEditProfileDialog = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("topbar_profile_button")
                        ) {
                            UserAvatar(
                                displayName = effectiveDisplayName,
                                photoData = userProfile?.photoData.orEmpty(),
                                size = 32.dp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .border(1.5.dp, glassBorderColor, CircleShape)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (darkMode) 0.84f else 0.86f),
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                    )
                }
            },
            bottomBar = {
                if (!useNavRail && !showFullscreenChromeHidden && !isAdminRoute) {
                    LiquidGlassFloatingNavBar(
                        tabs = TopDestinations.map { dest ->
                            LiquidGlassNavTab(
                                route = dest.route,
                                icon = dest.icon,
                                label = dest.label(language)
                            )
                        },
                        selectedRoute = currentRoute,
                        onSelectRoute = { navigateToTab(it) }
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail && !showFullscreenChromeHidden && !isAdminRoute) {
                    NavigationRail(
                        modifier = Modifier
                            .fillMaxHeight()
                            .border(1.dp, glassBorderColor),
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (darkMode) 0.78f else 0.84f)
                    ) {
                        TopDestinations.forEach { dest ->
                            NavigationRailItem(
                                selected = currentRoute == dest.route,
                                onClick = { navigateToTab(dest.route) },
                                icon = { Icon(dest.icon, contentDescription = dest.label(language)) },
                                label = {
                                    Text(
                                        text = dest.label(language),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = DumingagRoutes.EXPLORE,
                        modifier = if (showFullscreenChromeHidden) {
                            Modifier.fillMaxSize()
                        } else {
                            Modifier
                                .widthIn(max = 840.dp)
                                .fillMaxSize()
                        }
                    ) {
                        composable(DumingagRoutes.EXPLORE) {
                            val transitList = (transitState as? com.example.model.UiState.Success)?.data.orEmpty()
                            ExploreScreen(
                                language = language,
                                searchQuery = searchQuery,
                                selectedCategory = selectedCategory,
                                selectedCategoryCode = selectedCategoryCode,
                                managedCategories = managedCategories,
                                attractions = attractions,
                                allAttractionsForSearch = allAttractions,
                                savedIds = savedIds,
                                favoriteIds = favoriteIds,
                                showFavoritesOnly = showFavoritesOnly,
                                weatherState = weatherState,
                                isEffectiveOffline = isEffectiveOffline,
                                isRefreshing = isRefreshing,
                                lastSyncedAtMillis = lastSyncedAtMillis,
                                tourismEvents = tourismEvents,
                                transitMethods = transitList,
                                emergencyContacts = emergencyContacts,
                                reviews = publicReviews,
                                onSearchChange = viewModel::setSearchQuery,
                                onCategorySelected = viewModel::setCategory,
                                onCategoryCodeSelected = viewModel::setCategoryCode,
                                onToggleFavoritesOnly = viewModel::toggleFavoritesOnly,
                                onToggleFavorite = viewModel::toggleFavoriteAttraction,
                                onRefreshWeather = viewModel::refreshWeather,
                                onRefreshAll = viewModel::refreshAllData,
                                onAttractionClick = { attraction ->
                                    navController.navigate(DumingagRoutes.attractionDetail(attraction.id))
                                },
                                onNavigateToAttraction = { attraction ->
                                    viewModel.startOfflineNavigation(attraction)
                                    navigateToTab(DumingagRoutes.MAP)
                                },
                                onToggleSaveOffline = viewModel::toggleAttractionSavedOffline,
                                onOpenMapTab = { navigateToTab(DumingagRoutes.MAP) },
                                onOpenDirectory = { navController.navigate(DumingagRoutes.ATTRACTIONS) },
                                onOpenAiConcierge = { showAiConciergeSheet = true },
                                onOpenTransitTab = { navigateToTab(DumingagRoutes.TRANSIT) },
                                onOpenReviewsTab = { navigateToTab(DumingagRoutes.REVIEWS) },
                                ecoBadges = ecoBadges,
                                checkedInCount = checkedInAttractionIds.size,
                                totalAttractionsCount = allAttractions.size
                            )
                        }

                        composable(DumingagRoutes.ATTRACTIONS) {
                            val attractionsVm: AttractionsViewModel = viewModel(
                                key = "attractions_$currentUserId",
                                factory = viewModelFactory {
                                    initializer {
                                        val app = checkNotNull(this[APPLICATION_KEY])
                                        AttractionsViewModel(
                                            attractionRepository = AttractionRepository(app),
                                            offlineRepository = DumingagOfflineRepository(app)
                                        )
                                    }
                                }
                            )
                            AttractionsListScreen(
                                viewModel = attractionsVm,
                                language = language,
                                managedCategories = managedCategories,
                                favoriteIds = favoriteIds,
                                onToggleFavorite = viewModel::toggleFavoriteAttraction,
                                onAttractionClick = { attraction ->
                                    navController.navigate(DumingagRoutes.attractionDetail(attraction.id))
                                },
                                onNavigateOffline = { attraction ->
                                    viewModel.startOfflineNavigation(attraction)
                                    navigateToTab(DumingagRoutes.MAP)
                                },
                                onBackClick = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = DumingagRoutes.ATTRACTION_DETAIL,
                            arguments = listOf(navArgument("attractionId") { type = NavType.StringType })
                        ) { entry ->
                            val rawArg = entry.arguments?.getString("attractionId").orEmpty()
                            val attractionId = Uri.decode(rawArg)
                            val detailVm: AttractionDetailViewModel = viewModel(
                                key = "attraction_detail_${currentUserId}_$attractionId",
                                factory = viewModelFactory {
                                    initializer {
                                        val app = checkNotNull(this[APPLICATION_KEY])
                                        AttractionDetailViewModel(
                                            attractionId = attractionId,
                                            attractionRepository = AttractionRepository(app),
                                            offlineRepository = DumingagOfflineRepository(app)
                                        )
                                    }
                                }
                            )
                            val currentWeather = (weatherState as? com.example.model.UiState.Success)?.data
                            val galleryPhotosForSpot = allAttractions.firstOrNull { it.id == attractionId }?.galleryPhotos.orEmpty()
                            AttractionDetailScreen(
                                viewModel = detailVm,
                                language = language,
                                managedCategories = managedCategories,
                                isFavorite = attractionId in favoriteIds,
                                reviews = publicReviews,
                                currentUserId = currentUserId,
                                isAdmin = isAdmin,
                                weatherSnapshot = currentWeather,
                                galleryPhotosOverride = galleryPhotosForSpot,
                                onToggleFavorite = {
                                    val target = allAttractions.firstOrNull { it.id == attractionId }
                                        ?: (detailVm.uiState.value as? com.example.model.UiState.Success)?.data
                                    if (target != null) {
                                        viewModel.toggleFavoriteAttraction(target)
                                    }
                                },
                                onSubmitReview = viewModel::submitReview,
                                onUpdateReview = viewModel::updateReview,
                                onDeleteReview = viewModel::deleteReview,
                                onToggleReviewHelpful = viewModel::toggleReviewHelpful,
                                onAdminReplyReview = viewModel::postAdminReviewReply,
                                onBackClick = { navController.popBackStack() },
                                onNavigateOffline = { target ->
                                    viewModel.startOfflineNavigation(target)
                                    navigateToTab(DumingagRoutes.MAP)
                                },
                                onAddToItinerary = { target, millis, notes, reminder ->
                                    viewModel.addItineraryStop(target, millis, notes, reminder) { created ->
                                        if (reminder) {
                                            TravelAlertManager.scheduleItineraryReminder(context, created)
                                        }
                                    }
                                },
                                onShowMessage = viewModel::postStatusMessage,
                                isCheckedIn = attractionId in checkedInAttractionIds,
                                userLatLon = userLatLon,
                                trailReports = trailReports,
                                onCheckIn = viewModel::checkInAttraction,
                                onPostTrailReport = viewModel::addTrailReport
                            )
                        }

                        composable(DumingagRoutes.MAP) {
                            InteractiveMapScreen(
                                language = language,
                                attractions = attractions,
                                selectedCategory = selectedCategory,
                                selectedCategoryCode = selectedCategoryCode,
                                managedCategories = managedCategories,
                                selectedAttraction = selectedAttraction,
                                markedAttractions = markedAttractions,
                                activeRoute = activeRoute,
                                activeStepIndex = activeStepIndex,
                                travelMode = travelMode,
                                userLatLon = userLatLon,
                                offlinePackReady = offlinePackReady,
                                isEffectiveOffline = isEffectiveOffline,
                                isFullscreen = isMapFullscreen,
                                favoriteIds = favoriteIds,
                                onToggleFullscreen = { isMapFullscreen = it },
                                onToggleFavorite = viewModel::toggleFavoriteAttraction,
                                onCategorySelected = viewModel::setCategory,
                                onCategoryCodeSelected = viewModel::setCategoryCode,
                                onSelectAttraction = viewModel::selectAttraction,
                                onToggleMarkAttraction = viewModel::toggleMarkedAttraction,
                                onRemoveMarkedAttraction = viewModel::removeMarkedAttraction,
                                onClearAllMarks = viewModel::clearMarkedAttractions,
                                onStartNavigation = { viewModel.startOfflineNavigation(it) },
                                onClearNavigation = viewModel::clearActiveNavigation,
                                onStepNavigation = viewModel::stepNavigation,
                                onTravelModeChange = viewModel::setTravelMode,
                                onUpdateLocation = viewModel::updateUserLocation,
                                onOpenDetail = { attraction ->
                                    isMapFullscreen = false
                                    navController.navigate(DumingagRoutes.attractionDetail(attraction.id))
                                },
                                reviews = publicReviews,
                                currentUserId = currentUserId,
                                isAdmin = isAdmin,
                                tourismEvents = tourismEvents,
                                onSubmitReview = viewModel::submitReview,
                                onUpdateReview = viewModel::updateReview,
                                onDeleteReview = viewModel::deleteReview,
                                onToggleReviewHelpful = viewModel::toggleReviewHelpful,
                                onAdminReplyReview = viewModel::postAdminReviewReply
                            )
                        }

                        composable(DumingagRoutes.TRANSIT) {
                            TransitSafetyScreen(
                                language = language,
                                transitState = transitState,
                                emergencyContacts = emergencyContacts,
                                isAdmin = isAdmin,
                                isEffectiveOffline = isEffectiveOffline,
                                isRefreshing = isRefreshing,
                                cacheStatus = travelGuideCacheStatus,
                                onRefreshTransit = viewModel::refreshTransit,
                                onRefreshAll = viewModel::refreshAllData,
                                onManageEmergencyContacts = {
                                    navController.navigate(DumingagRoutes.admin(1))
                                },
                                onAddTransitMethod = viewModel::addTransitMethod,
                                onUpdateTransitMethod = viewModel::updateTransitMethod,
                                onDeleteTransitMethod = viewModel::deleteTransitMethod,
                                onShowMessage = viewModel::postStatusMessage,
                                attractions = allAttractions,
                                userLatLon = userLatLon
                            )
                        }

                        composable(DumingagRoutes.PLAN) {
                            ItineraryVaultScreen(
                                language = language,
                                attractions = allAttractions,
                                itinerariesState = itinerariesState,
                                savedDocuments = savedDocs,
                                savedAttractionCount = savedIds.size,
                                offlinePackReady = offlinePackReady,
                                forceOfflineMode = forceOfflineMode,
                                onToggleForceOffline = viewModel::toggleForceOffline,
                                onDownloadOfflinePack = viewModel::downloadFullOfflinePack,
                                onAddItinerary = { attr, millis, notes, reminder ->
                                    viewModel.addItineraryStop(attr, millis, notes, reminder) { created ->
                                        if (reminder) {
                                            TravelAlertManager.scheduleItineraryReminder(context, created)
                                        }
                                    }
                                },
                                onSyncCalendar = viewModel::markCalendarSynced,
                                onToggleReminder = viewModel::toggleItineraryReminder,
                                onDeleteItinerary = viewModel::deleteItineraryItem,
                                onSaveCustomDocument = viewModel::saveCustomDocument,
                                onDeleteCustomDocument = viewModel::deleteCustomDocument,
                                onShowMessage = viewModel::postStatusMessage,
                                userLatLon = userLatLon,
                                packingItems = smartPackingItems,
                                tripExpenses = tripExpenses,
                                tileDownloadProgress = tileDownloadProgress,
                                onTogglePackingItem = viewModel::togglePackingItem,
                                onAddExpense = viewModel::addTripExpense,
                                onDeleteExpense = viewModel::deleteTripExpense,
                                onPreDownloadMapTiles = viewModel::preDownloadOfflineMapTiles,
                                onOpenAiConcierge = { showAiConciergeSheet = true }
                            )
                        }

                        composable(DumingagRoutes.REVIEWS) {
                            CommunityReviewsScreen(
                                language = language,
                                currentUserId = currentUserId,
                                isAdmin = isAdmin,
                                attractions = allAttractions,
                                favoriteAttractions = favoriteAttractions,
                                reviewsState = reviewsState,
                                isRefreshing = isRefreshing,
                                onRefreshAll = viewModel::refreshAllData,
                                onOpenAttraction = { attraction ->
                                    navController.navigate(DumingagRoutes.attractionDetail(attraction.id))
                                },
                                onShowOnMap = { attraction ->
                                    viewModel.selectAttraction(attraction)
                                    navigateToTab(DumingagRoutes.MAP)
                                },
                                onToggleFavorite = viewModel::toggleFavoriteAttraction,
                                onSubmitReview = viewModel::submitReview,
                                onUpdateReview = viewModel::updateReview,
                                onDeleteReview = viewModel::deleteReview,
                                onToggleReviewHelpful = viewModel::toggleReviewHelpful,
                                onAdminReplyReview = viewModel::postAdminReviewReply
                            )
                        }

                        composable(
                            route = DumingagRoutes.ADMIN,
                            arguments = listOf(
                                navArgument("initialTab") {
                                    type = NavType.IntType
                                    defaultValue = 0
                                }
                            )
                        ) { entry ->
                            val initialTab = entry.arguments?.getInt("initialTab") ?: 0
                            AdminManagementScreen(
                                language = language,
                                isAdmin = isAdmin,
                                adminEmail = userEmail,
                                currentUserId = currentUserId,
                                loadUsers = viewModel::loadManagedUsers,
                                attractions = allAttractions,
                                managedCategories = managedCategories,
                                emergencyContacts = emergencyContacts,
                                tourismEvents = tourismEvents,
                                reviews = publicReviews,
                                trailReports = trailReports,
                                userLatLon = userLatLon,
                                initialTab = initialTab,
                                onBackClick = { navController.popBackStack() },
                                onSaveAttraction = viewModel::saveAdminAttraction,
                                onDeleteAttraction = viewModel::deleteAdminAttraction,
                                onSaveCategory = viewModel::saveAdminCategory,
                                onDeleteCategory = viewModel::deleteAdminCategory,
                                onSaveEmergencyContact = viewModel::saveAdminEmergencyContact,
                                onDeleteEmergencyContact = viewModel::deleteAdminEmergencyContact,
                                onSaveEvent = viewModel::saveAdminEvent,
                                onDeleteEvent = viewModel::deleteAdminEvent,
                                onAdminReplyReview = viewModel::postAdminReviewReply,
                                onDeleteReview = viewModel::deleteReview
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAiConciergeSheet) {
        AiConciergeSheet(
            messages = aiMessages,
            streamingText = aiStreamingText,
            isLoading = isAiLoading,
            errorMessage = aiError,
            isOffline = isEffectiveOffline,
            voiceState = aiVoiceState,
            onSelectVoicePersona = viewModel::selectAiVoicePersona,
            onToggleAutoSpeak = viewModel::toggleAiAutoSpeak,
            onSpeakMessage = viewModel::speakAiMessage,
            onStopVoice = viewModel::stopAiVoice,
            onSendPrompt = viewModel::sendAiConciergeMessage,
            onClearChat = viewModel::clearAiChat,
            onDismiss = {
                viewModel.stopAiVoice()
                showAiConciergeSheet = false
            }
        )
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            initialName = effectiveDisplayName,
            initialPhotoData = userProfile?.photoData.orEmpty(),
            email = userEmail,
            isAdmin = isAdmin,
            favoriteAttractions = favoriteAttractions,
            language = language,
            onOpenAttraction = { attraction ->
                showEditProfileDialog = false
                navController.navigate(DumingagRoutes.attractionDetail(attraction.id))
            },
            onRemoveFavorite = viewModel::toggleFavoriteAttraction,
            onOpenLanguagePicker = {
                showEditProfileDialog = false
                showLanguageDialog = true
            },
            onOpenAdminConsole = {
                showEditProfileDialog = false
                navController.navigate(DumingagRoutes.admin(0))
            },
            onSignOut = {
                showEditProfileDialog = false
                onRequestSignOut()
            },
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, photoData, clearPhoto ->
                viewModel.updateProfile(
                    displayName = name,
                    photoData = photoData,
                    clearPhoto = clearPhoto
                )
                showEditProfileDialog = false
            }
        )
    }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (darkMode) 0.94f else 0.96f),
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("Liquid Glass Appearance", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppThemeMode.entries.forEach { mode ->
                        val isSelected = mode == themeMode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                    else Color.Transparent
                                )
                                .border(
                                    width = if (isSelected) 1.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else Color.Transparent,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                                .testTag("theme_option_${mode.code}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    mode.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    mode.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Close") }
            }
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (darkMode) 0.94f else 0.96f),
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("App & Navigation Language", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppLanguage.entries.forEach { lang ->
                        val isSelected = lang == language
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                    else Color.Transparent
                                )
                                .border(
                                    width = if (isSelected) 1.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else Color.Transparent,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    viewModel.setLanguage(lang)
                                    showLanguageDialog = false
                                }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    viewModel.setLanguage(lang)
                                    showLanguageDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(lang.nativeLabel, style = MaterialTheme.typography.titleSmall)
                                Text(lang.displayName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showLanguageDialog = false },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text("Close", maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        )
    }
}
