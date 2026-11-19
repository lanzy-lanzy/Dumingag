package com.example.ui

import android.app.Application
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
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Forum
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.example.notifications.TravelAlertManager
import com.example.ui.components.EditProfileDialog
import com.example.ui.components.UserAvatar
import com.example.ui.screens.AdminManagementScreen
import com.example.ui.screens.AttractionDetailScreen
import com.example.ui.screens.AttractionsListScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CommunityReviewsScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.InteractiveMapScreen
import com.example.ui.screens.ItineraryVaultScreen
import com.example.ui.screens.TransitSafetyScreen
import com.example.viewmodel.AttractionDetailViewModel
import com.example.viewmodel.AttractionsViewModel
import com.example.viewmodel.DumingagViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

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

@Composable
fun AppNavigation(auth: FirebaseAuth) {
    val currentUser by remember(auth) { auth.authStateFlow() }.collectAsStateWithLifecycle(initialValue = auth.currentUser?.toSessionUser())
    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        val displayName = user.displayName?.takeIf { it.isNotBlank() } ?: user.email ?: "Dumingag Traveler"
        key(user.uid, user.email, user.isEmailVerified) {
            DumingagMainScaffold(
                currentUserId = user.uid,
                userDisplayName = displayName,
                userEmail = user.email.orEmpty(),
                isEmailVerified = user.isEmailVerified
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DumingagMainScaffold(
    currentUserId: String,
    userDisplayName: String,
    userEmail: String = "",
    isEmailVerified: Boolean = false
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
    val attractions by viewModel.filteredAttractions.collectAsStateWithLifecycle()
    val allAttractions by viewModel.allAttractions.collectAsStateWithLifecycle()
    val emergencyContacts by viewModel.emergencyContacts.collectAsStateWithLifecycle()
    val selectedAttraction by viewModel.selectedAttraction.collectAsStateWithLifecycle()
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
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

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

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (!isDetailRoute && !isAdminRoute && !showFullscreenChromeHidden) {
                    TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Dumingag",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = if (isEffectiveOffline) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.clickable { navigateToTab(DumingagRoutes.PLAN) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isEffectiveOffline) Icons.Default.CloudOff else Icons.Default.CloudDone,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = if (isEffectiveOffline) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isEffectiveOffline) AppStrings.offlineLabel(language) else AppStrings.onlineLabel(language),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    },
                    actions = {
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
                            onClick = { showLanguageDialog = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("language_picker_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = "Select language"
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
                                modifier = Modifier.clip(CircleShape)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                    )
                }
            },
            bottomBar = {
                if (!useNavRail && !showFullscreenChromeHidden && !isAdminRoute) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        TopDestinations.forEach { dest ->
                            NavigationBarItem(
                                selected = currentRoute == dest.route,
                                onClick = { navigateToTab(dest.route) },
                                icon = { Icon(dest.icon, contentDescription = dest.label(language)) },
                                label = {
                                    Text(
                                        text = dest.label(language),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
                        }
                    }
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
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surface
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
                            ExploreScreen(
                                language = language,
                                searchQuery = searchQuery,
                                selectedCategory = selectedCategory,
                                attractions = attractions,
                                savedIds = savedIds,
                                favoriteIds = favoriteIds,
                                showFavoritesOnly = showFavoritesOnly,
                                weatherState = weatherState,
                                isEffectiveOffline = isEffectiveOffline,
                                onSearchChange = viewModel::setSearchQuery,
                                onCategorySelected = viewModel::setCategory,
                                onToggleFavoritesOnly = viewModel::toggleFavoritesOnly,
                                onToggleFavorite = viewModel::toggleFavoriteAttraction,
                                onRefreshWeather = viewModel::refreshWeather,
                                onAttractionClick = { attraction ->
                                    navController.navigate(DumingagRoutes.attractionDetail(attraction.id))
                                },
                                onNavigateToAttraction = { attraction ->
                                    viewModel.startOfflineNavigation(attraction)
                                    navigateToTab(DumingagRoutes.MAP)
                                },
                                onToggleSaveOffline = viewModel::toggleAttractionSavedOffline,
                                onOpenMapTab = { navigateToTab(DumingagRoutes.MAP) },
                                onOpenDirectory = { navController.navigate(DumingagRoutes.ATTRACTIONS) }
                            )
                        }

                        composable(DumingagRoutes.ATTRACTIONS) {
                            val attractionsVm: AttractionsViewModel = viewModel(
                                key = "attractions_$currentUserId",
                                factory = viewModelFactory {
                                    initializer {
                                        val app = checkNotNull(this[APPLICATION_KEY]) as Application
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
                                        val app = checkNotNull(this[APPLICATION_KEY]) as Application
                                        AttractionDetailViewModel(
                                            attractionId = attractionId,
                                            attractionRepository = AttractionRepository(app),
                                            offlineRepository = DumingagOfflineRepository(app)
                                        )
                                    }
                                }
                            )
                            AttractionDetailScreen(
                                viewModel = detailVm,
                                language = language,
                                isFavorite = attractionId in favoriteIds,
                                reviews = publicReviews,
                                currentUserId = currentUserId,
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
                                onShowMessage = viewModel::postStatusMessage
                            )
                        }

                        composable(DumingagRoutes.MAP) {
                            InteractiveMapScreen(
                                language = language,
                                attractions = attractions,
                                selectedCategory = selectedCategory,
                                selectedAttraction = selectedAttraction,
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
                                onSelectAttraction = viewModel::selectAttraction,
                                onStartNavigation = { viewModel.startOfflineNavigation(it) },
                                onClearNavigation = viewModel::clearActiveNavigation,
                                onStepNavigation = viewModel::stepNavigation,
                                onTravelModeChange = viewModel::setTravelMode,
                                onUpdateLocation = viewModel::updateUserLocation,
                                onOpenDetail = { attraction ->
                                    isMapFullscreen = false
                                    navController.navigate(DumingagRoutes.attractionDetail(attraction.id))
                                }
                            )
                        }

                        composable(DumingagRoutes.TRANSIT) {
                            TransitSafetyScreen(
                                language = language,
                                transitState = transitState,
                                emergencyContacts = emergencyContacts,
                                isAdmin = isAdmin,
                                isEffectiveOffline = isEffectiveOffline,
                                onRefreshTransit = viewModel::refreshTransit,
                                onManageEmergencyContacts = {
                                    navController.navigate(DumingagRoutes.admin(1))
                                },
                                onAddTransitMethod = viewModel::addTransitMethod,
                                onUpdateTransitMethod = viewModel::updateTransitMethod,
                                onDeleteTransitMethod = viewModel::deleteTransitMethod,
                                onShowMessage = viewModel::postStatusMessage
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
                                onShowMessage = viewModel::postStatusMessage
                            )
                        }

                        composable(DumingagRoutes.REVIEWS) {
                            CommunityReviewsScreen(
                                language = language,
                                currentUserId = currentUserId,
                                userDisplayName = effectiveDisplayName,
                                userEmail = userEmail,
                                userPhotoData = userProfile?.photoData.orEmpty(),
                                isAdmin = isAdmin,
                                attractions = allAttractions,
                                favoriteAttractions = favoriteAttractions,
                                reviewsState = reviewsState,
                                onOpenLanguagePicker = { showLanguageDialog = true },
                                onEditProfile = { showEditProfileDialog = true },
                                onOpenAdminConsole = { navController.navigate(DumingagRoutes.admin(0)) },
                                onSignOut = {
                                    GoogleAuthHelper.signOut(credentialManager, scope)
                                },
                                onOpenAttraction = { attraction ->
                                    navController.navigate(DumingagRoutes.attractionDetail(attraction.id))
                                },
                                onToggleFavorite = viewModel::toggleFavoriteAttraction,
                                onSubmitReview = viewModel::submitReview,
                                onUpdateReview = viewModel::updateReview,
                                onVerifyEmail = if (isEmailVerified || userEmail.isBlank()) null else ({
                                    scope.launch {
                                        try {
                                            val user = Firebase.auth.currentUser
                                            if (user?.uid == currentUserId) {
                                                user.sendEmailVerification().await()
                                                viewModel.postStatusMessage("Check your email, then sign in again after verifying.")
                                            }
                                        } catch (e: CancellationException) {
                                            throw e
                                        } catch (e: Exception) {
                                            viewModel.postStatusMessage(e.localizedMessage ?: "Unable to send verification email.")
                                        }
                                    }
                                }),
                                onDeleteReview = viewModel::deleteReview
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
                                emergencyContacts = emergencyContacts,
                                initialTab = initialTab,
                                onBackClick = { navController.popBackStack() },
                                onSaveAttraction = viewModel::saveAdminAttraction,
                                onDeleteAttraction = viewModel::deleteAdminAttraction,
                                onSaveEmergencyContact = viewModel::saveAdminEmergencyContact,
                                onDeleteEmergencyContact = viewModel::deleteAdminEmergencyContact
                            )
                        }
                    }
                }
            }
        }
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

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("App & Navigation Language") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AppLanguage.entries.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setLanguage(lang)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = lang == language,
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
