package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.model.CommunityReview
import com.example.model.ItineraryItem
import com.example.model.TourismEvent
import com.example.model.TourismEventSeverity
import com.example.model.TourismEventType
import com.example.model.TransitMethod
import com.example.model.TransitModeCategory
import com.example.model.TransitServiceStatus
import com.example.model.ManagedUser
import com.google.firebase.firestore.MetadataChanges
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Locale

class DumingagCloudRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
    }

    suspend fun ensureUserProfile(
        userId: String,
        displayName: String,
        email: String = ""
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val cleanEmail = email.trim().ifBlank { auth.currentUser?.email.orEmpty().trim() }
                .lowercase(Locale.ROOT)
                .take(160)
            val docRef = db.collection("users").document(uid)
            db.runTransaction { transaction ->
                val existing = transaction.get(docRef)
                if (!existing.exists()) {
                    val safeName = displayName.trim().ifBlank { "Traveler" }.take(80)
                    val payload = buildMap<String, Any> {
                        put("userId", uid)
                        if (cleanEmail.isNotEmpty()) {
                            put("email", cleanEmail)
                        }
                        put("role", "regular")
                        put("displayName", safeName)
                        put("favoriteAttractionIds", emptyList<String>())
                        put("preferredLanguage", "en")
                        put("offlinePackEnabled", false)
                        put("createdAt", FieldValue.serverTimestamp())
                        put("updatedAt", FieldValue.serverTimestamp())
                    }
                    transaction.set(docRef, payload)
                } else {
                    val existingEmail = existing.getString("email").orEmpty().trim().lowercase(Locale.ROOT)
                    if (cleanEmail.isNotEmpty() && existingEmail != cleanEmail) {
                        transaction.update(
                            docRef,
                            mapOf(
                                "email" to cleanEmail,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }
                Unit
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val docRef = db.collection("users").document(userId)
        docRef.snapshots()
            .map { snapshot ->
                snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                }
                throw error
            }
            .collect { emit(it) }
    }

    suspend fun updateUserPreferences(
        userId: String,
        languageCode: String,
        offlinePackEnabled: Boolean
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val validLang = if (languageCode in listOf("en", "ceb", "tl", "es")) languageCode else "en"
            db.collection("users").document(uid).update(
                mapOf(
                    "preferredLanguage" to validLang,
                    "offlinePackEnabled" to offlinePackEnabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    fun observeItineraries(userId: String): Flow<List<ItineraryItem>> = flow {
        val path = "users/$userId/itineraries"
        db.collection("users").document(userId).collection("itineraries")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ItineraryItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.plannedDateMillis }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
            .collect { emit(it) }
    }

    suspend fun addItineraryItem(
        userId: String,
        attractionId: String,
        attractionName: String,
        plannedDateMillis: Long,
        notes: String,
        reminderEnabled: Boolean
    ): Result<String> {
        val colRef = db.collection("users").document(userId).collection("itineraries")
        val docRef = colRef.document()
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val safeAttractionId = attractionId.trim().take(80)
            val safeAttractionName = attractionName.trim().take(120)
            require(safeAttractionId.isNotBlank() && safeAttractionName.isNotBlank()) {
                "Attraction details cannot be blank"
            }
            val safeNotes = notes.trim().take(500)
            val payload = buildMap<String, Any> {
                put("userId", uid)
                put("attractionId", safeAttractionId)
                put("attractionName", safeAttractionName)
                put("plannedDateMillis", plannedDateMillis.coerceAtLeast(0L))
                if (safeNotes.isNotEmpty()) put("notes", safeNotes)
                put("reminderEnabled", reminderEnabled)
                put("calendarSynced", false)
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateItineraryStatus(
        userId: String,
        itineraryId: String,
        reminderEnabled: Boolean,
        calendarSynced: Boolean
    ): Result<Unit> {
        val docPath = "users/$userId/itineraries/$itineraryId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            require(itineraryId.isNotBlank()) { "Itinerary ID cannot be blank" }
            db.collection("users").document(uid).collection("itineraries").document(itineraryId)
                .update(
                    mapOf(
                        "reminderEnabled" to reminderEnabled,
                        "calendarSynced" to calendarSynced,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteItineraryItem(userId: String, itineraryId: String): Result<Unit> {
        val docPath = "users/$userId/itineraries/$itineraryId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            db.collection("users").document(uid).collection("itineraries").document(itineraryId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // Query strictly includes .whereEqualTo("visibility", "public") matching firestore.rules allow list
    fun observePublicReviews(): Flow<List<CommunityReview>> = flow {
        val path = "reviews"
        db.collection(path)
            .whereEqualTo("visibility", "public")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(CommunityReview::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser != null) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
            .collect { emit(it) }
    }

    suspend fun addReview(
        userId: String,
        authorName: String,
        attractionId: String,
        attractionName: String,
        rating: Int,
        comment: String,
        visitTag: String,
        photoData: String? = null,
        verifiedVisit: Boolean = false
    ): Result<Unit> {
        val docRef = db.collection("reviews").document()
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val trimmedComment = comment.trim().take(800)
            require(trimmedComment.length >= 3) { "Review must be at least 3 characters." }
            val clampedRating = rating.coerceIn(1, 5).toLong()
            val safeAuthor = authorName.trim().ifBlank { "Traveler" }.take(80)
            val safeTag = visitTag.trim().ifBlank { "Eco-Tour" }.take(40)
            val cleanPhoto = photoData?.trim()
            if (cleanPhoto != null) {
                require(cleanPhoto.length <= 65536) { "Review photo exceeds maximum size limit." }
            }

            val payload = buildMap<String, Any> {
                put("userId", uid)
                put("authorName", safeAuthor)
                put("attractionId", attractionId.trim().take(80))
                put("attractionName", attractionName.trim().take(120))
                put("rating", clampedRating)
                put("comment", trimmedComment)
                put("visitTag", safeTag)
                if (!cleanPhoto.isNullOrEmpty()) {
                    put("photoData", cleanPhoto)
                }
                put("verifiedVisit", verifiedVisit)
                put("helpfulUserIds", emptyList<String>())
                put("visibility", "public")
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateReview(
        userId: String,
        reviewId: String,
        rating: Int,
        comment: String,
        visitTag: String,
        photoData: String? = null,
        clearPhoto: Boolean = false
    ): Result<Unit> {
        val docPath = "reviews/$reviewId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val trimmedComment = comment.trim().take(800)
            require(trimmedComment.length >= 3) { "Review must be at least 3 characters." }
            val cleanPhoto = photoData?.trim()
            if (cleanPhoto != null) {
                require(cleanPhoto.length <= 65536) { "Review photo exceeds maximum size limit." }
            }
            val updates = buildMap<String, Any> {
                put("rating", rating.coerceIn(1, 5).toLong())
                put("comment", trimmedComment)
                put("visitTag", visitTag.trim().ifBlank { "Eco-Tour" }.take(40))
                if (clearPhoto) {
                    put("photoData", FieldValue.delete())
                } else if (!cleanPhoto.isNullOrEmpty()) {
                    put("photoData", cleanPhoto)
                }
                put("updatedAt", FieldValue.serverTimestamp())
            }
            db.collection("reviews").document(reviewId)
                .update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleReviewHelpful(
        userId: String,
        reviewId: String,
        currentlyHelpful: Boolean
    ): Result<Boolean> {
        val docPath = "reviews/$reviewId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val op = if (currentlyHelpful) FieldValue.arrayRemove(uid) else FieldValue.arrayUnion(uid)
            db.collection("reviews").document(reviewId)
                .update(
                    mapOf(
                        "helpfulUserIds" to op,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(!currentlyHelpful)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun postAdminReviewReply(
        reviewId: String,
        replyText: String,
        adminLabel: String = "Official Dumingag Tourism"
    ): Result<Unit> {
        val docPath = "reviews/$reviewId"
        return try {
            requireUserId()
            val cleanReply = replyText.trim().take(500)
            val cleanLabel = adminLabel.trim().ifBlank { "Official Dumingag Tourism" }.take(80)
            val updates = if (cleanReply.isEmpty()) {
                mapOf(
                    "adminReply" to FieldValue.delete(),
                    "adminReplyBy" to FieldValue.delete(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            } else {
                mapOf(
                    "adminReply" to cleanReply,
                    "adminReplyBy" to cleanLabel,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            }
            db.collection("reviews").document(reviewId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteReview(userId: String, reviewId: String): Result<Unit> {
        val docPath = "reviews/$reviewId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            db.collection("reviews").document(reviewId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- Dumingag Transit Methods (Firestore-backed real-time stream + CRUD) ---
    // Query strictly includes .whereEqualTo("visibility", "public") matching firestore.rules allow list
    fun observePublicTransitMethods(): Flow<List<TransitMethod>> = flow {
        val path = "transit_methods"
        db.collection(path)
            .whereEqualTo("visibility", "public")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(TransitMethod::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(compareBy({ modeSortOrder(it.modeCategory) }, { it.name }))
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser != null) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
            .collect { emit(it) }
    }

    suspend fun refreshTransitMethodsFromServer(): Result<List<TransitMethod>> {
        val path = "transit_methods"
        return try {
            requireUserId()
            val snapshot = db.collection(path)
                .whereEqualTo("visibility", "public")
                .get(Source.SERVER)
                .await()
            val items = snapshot.toObjects(
                TransitMethod::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedWith(compareBy({ modeSortOrder(it.modeCategory) }, { it.name }))
            Result.success(items)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun seedDefaultTransitMethodsIfEmpty(
        userId: String,
        defaults: List<TransitMethod>
    ): Result<Boolean> {
        val path = "transit_methods"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val existingQuery = db.collection(path)
                .whereEqualTo("visibility", "public")
                .limit(1)
                .get()
                .await()
            if (!existingQuery.isEmpty) {
                return Result.success(false)
            }
            for (item in defaults) {
                val docId = item.id.trim().take(128).ifBlank { continue }
                val docRef = db.collection(path).document(docId)
                if (!docRef.get().await().exists()) {
                    val payload = buildTransitPayload(
                        uid = uid,
                        name = item.name,
                        modeCategory = item.modeCategory,
                        operatorName = item.operatorName,
                        routeSummary = item.routeSummary,
                        scheduleText = item.scheduleText,
                        fareRangePhp = item.fareRangePhp,
                        durationText = item.durationText,
                        terminalLocation = item.terminalLocation,
                        travelTip = item.travelTip,
                        serviceStatus = item.serviceStatus,
                        includeCreationFields = true
                    )
                    docRef.set(payload).await()
                }
            }
            Result.success(true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun addTransitMethod(
        userId: String,
        name: String,
        modeCategory: String,
        operatorName: String,
        routeSummary: String,
        scheduleText: String,
        fareRangePhp: String,
        durationText: String,
        terminalLocation: String,
        travelTip: String,
        serviceStatus: String
    ): Result<Unit> {
        val docRef = db.collection("transit_methods").document()
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val payload = buildTransitPayload(
                uid = uid,
                name = name,
                modeCategory = modeCategory,
                operatorName = operatorName,
                routeSummary = routeSummary,
                scheduleText = scheduleText,
                fareRangePhp = fareRangePhp,
                durationText = durationText,
                terminalLocation = terminalLocation,
                travelTip = travelTip,
                serviceStatus = serviceStatus,
                includeCreationFields = true
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateTransitMethod(
        userId: String,
        transitId: String,
        name: String,
        modeCategory: String,
        operatorName: String,
        routeSummary: String,
        scheduleText: String,
        fareRangePhp: String,
        durationText: String,
        terminalLocation: String,
        travelTip: String,
        serviceStatus: String
    ): Result<Unit> {
        val docPath = "transit_methods/$transitId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            require(transitId.isNotBlank()) { "Transit ID cannot be blank" }
            val updates = buildTransitPayload(
                uid = uid,
                name = name,
                modeCategory = modeCategory,
                operatorName = operatorName,
                routeSummary = routeSummary,
                scheduleText = scheduleText,
                fareRangePhp = fareRangePhp,
                durationText = durationText,
                terminalLocation = terminalLocation,
                travelTip = travelTip,
                serviceStatus = serviceStatus,
                includeCreationFields = false
            )
            db.collection("transit_methods").document(transitId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteTransitMethod(userId: String, transitId: String): Result<Unit> {
        val docPath = "transit_methods/$transitId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            require(transitId.isNotBlank()) { "Transit ID cannot be blank" }
            db.collection("transit_methods").document(transitId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    private fun buildTransitPayload(
        uid: String,
        name: String,
        modeCategory: String,
        operatorName: String,
        routeSummary: String,
        scheduleText: String,
        fareRangePhp: String,
        durationText: String,
        terminalLocation: String,
        travelTip: String,
        serviceStatus: String,
        includeCreationFields: Boolean
    ): Map<String, Any> {
        val cleanName = name.trim().take(120)
        val cleanOperator = operatorName.trim().take(100)
        val cleanRoute = routeSummary.trim().take(240)
        val cleanSchedule = scheduleText.trim().take(160)
        val cleanFare = fareRangePhp.trim().take(60)
        val cleanDuration = durationText.trim().take(60)
        val cleanTerminal = terminalLocation.trim().take(160)
        val cleanTip = travelTip.trim().take(400)

        require(cleanName.length >= 2) { "Transport name must be at least 2 characters." }
        require(cleanOperator.length >= 2) { "Operator name must be at least 2 characters." }
        require(cleanRoute.length >= 3) { "Route summary must be at least 3 characters." }
        require(cleanSchedule.length >= 3) { "Schedule must be at least 3 characters." }
        require(cleanFare.isNotEmpty()) { "Fare range cannot be empty." }
        require(cleanDuration.isNotEmpty()) { "Duration cannot be empty." }
        require(cleanTerminal.length >= 2) { "Terminal location must be at least 2 characters." }

        val validMode = TransitModeCategory.fromCode(modeCategory).code
        val validStatus = TransitServiceStatus.fromCode(serviceStatus).code

        return buildMap {
            if (includeCreationFields) {
                put("userId", uid)
                put("visibility", "public")
                put("createdAt", FieldValue.serverTimestamp())
            }
            put("name", cleanName)
            put("modeCategory", validMode)
            put("operatorName", cleanOperator)
            put("routeSummary", cleanRoute)
            put("scheduleText", cleanSchedule)
            put("fareRangePhp", cleanFare)
            put("durationText", cleanDuration)
            put("terminalLocation", cleanTerminal)
            if (cleanTip.isNotEmpty()) {
                put("travelTip", cleanTip)
            }
            put("serviceStatus", validStatus)
            put("updatedAt", FieldValue.serverTimestamp())
        }
    }

    private fun modeSortOrder(modeCategory: String): Int = when (modeCategory) {
        TransitModeCategory.TRICYCLE.code -> 0
        TransitModeCategory.HABAL_HABAL.code -> 1
        TransitModeCategory.JEEPNEY_MULTICAB.code -> 2
        TransitModeCategory.BUS_VAN.code -> 3
        else -> 4
    }

    // --- Profile Editing (Display Name & DP) + Admin Role Verification ---

    suspend fun loadManagedUsers(): List<ManagedUser> {
        val profiles = db.collection("users").get(Source.SERVER).await()
        return profiles.documents.map { doc ->
            val rawRole = doc.getString("role")?.trim()?.lowercase(Locale.ROOT)
            val normalizedRole = if (rawRole == "admin") "admin" else "regular"
            ManagedUser(
                uid = doc.id,
                displayName = doc.getString("displayName").orEmpty(),
                email = doc.getString("email").orEmpty(),
                role = normalizedRole
            )
        }.sortedWith(
            compareByDescending<ManagedUser> { it.isAdminRole }
                .thenBy { it.displayName.lowercase(Locale.ROOT) }
                .thenBy { it.uid }
        )
    }

    fun observeAdminAccess(userId: String, email: String, verified: Boolean): Flow<Boolean> {
        val address = email.trim().lowercase(Locale.ROOT)
        if (!verified || address.isBlank() || '/' in address || auth.currentUser?.uid != userId) {
            return flowOf(false)
        }
        val userDocFlow = db.collection("users").document(userId)
            .snapshots(MetadataChanges.INCLUDE)
            .map { snapshot ->
                val role = snapshot.getString("role")?.trim()?.lowercase(Locale.ROOT)
                val docEmail = snapshot.getString("email").orEmpty().trim().lowercase(Locale.ROOT)
                val emailMatches = docEmail.isBlank() || docEmail == address
                !snapshot.metadata.isFromCache &&
                    !snapshot.metadata.hasPendingWrites() &&
                    snapshot.exists() &&
                    role == "admin" &&
                    emailMatches &&
                    auth.currentUser?.uid == userId
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                emit(false)
            }

        val adminEmailDocFlow = db.collection("admin_emails").document(address)
            .snapshots(MetadataChanges.INCLUDE)
            .map { snapshot ->
                val role = snapshot.getString("role")?.trim()?.lowercase(Locale.ROOT)
                val enabled = snapshot.get("enabled") == true
                !snapshot.metadata.isFromCache &&
                    !snapshot.metadata.hasPendingWrites() &&
                    snapshot.exists() &&
                    (enabled || role == "admin") &&
                    auth.currentUser?.uid == userId
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                emit(false)
            }

        return combine(userDocFlow, adminEmailDocFlow) { fromUserDoc, fromEmailDoc ->
            (fromUserDoc || fromEmailDoc) && auth.currentUser?.uid == userId
        }
    }

    suspend fun updateUserProfile(
        userId: String,
        displayName: String,
        photoData: String?,
        clearPhoto: Boolean = false
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val safeName = displayName.trim().take(80)
            require(safeName.isNotEmpty()) { "Display name cannot be empty." }
            val cleanPhoto = photoData?.trim()
            if (cleanPhoto != null) {
                require(cleanPhoto.length <= 65536) { "Profile photo exceeds maximum size limit." }
            }

            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val currentEmail = auth.currentUser?.email.orEmpty().trim().lowercase(Locale.ROOT).take(160)
                val payload = buildMap<String, Any> {
                    put("userId", uid)
                    if (currentEmail.isNotEmpty()) {
                        put("email", currentEmail)
                    }
                    put("role", "regular")
                    put("displayName", safeName)
                    if (!clearPhoto && !cleanPhoto.isNullOrEmpty()) {
                        put("photoData", cleanPhoto)
                    }
                    put("favoriteAttractionIds", emptyList<String>())
                    put("preferredLanguage", "en")
                    put("offlinePackEnabled", false)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                docRef.set(payload).await()
            } else {
                val updates = buildMap<String, Any> {
                    put("displayName", safeName)
                    if (clearPhoto) {
                        put("photoData", FieldValue.delete())
                    } else if (!cleanPhoto.isNullOrEmpty()) {
                        put("photoData", cleanPhoto)
                    }
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                docRef.update(updates).await()
            }

            try {
                val profileUpdate = com.google.firebase.auth.userProfileChangeRequest {
                    this.displayName = safeName
                }
                auth.currentUser?.updateProfile(profileUpdate)?.await()
            } catch (_: Exception) {
                // Auth profile displayName sync is best-effort; Firestore UserProfile is authoritative
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleFavoriteAttraction(
        userId: String,
        attractionId: String,
        displayNameFallback: String = "Traveler"
    ): Result<Boolean> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val safeAttractionId = attractionId.trim().take(80)
            require(safeAttractionId.isNotEmpty()) { "Attraction ID cannot be blank." }
            val safeName = displayNameFallback.trim().ifBlank { "Traveler" }.take(80)
            val docRef = db.collection("users").document(uid)

            val added = db.runTransaction { transaction ->
                val existing = transaction.get(docRef)
                if (!existing.exists()) {
                    val currentEmail = auth.currentUser?.email.orEmpty().trim().lowercase(Locale.ROOT).take(160)
                    val payload = buildMap<String, Any> {
                        put("userId", uid)
                        if (currentEmail.isNotEmpty()) {
                            put("email", currentEmail)
                        }
                        put("role", "regular")
                        put("displayName", safeName)
                        put("favoriteAttractionIds", listOf(safeAttractionId))
                        put("preferredLanguage", "en")
                        put("offlinePackEnabled", false)
                        put("createdAt", FieldValue.serverTimestamp())
                        put("updatedAt", FieldValue.serverTimestamp())
                    }
                    transaction.set(docRef, payload)
                    true
                } else {
                    val current = (existing.get("favoriteAttractionIds") as? List<*>)
                        ?.filterIsInstance<String>()
                        .orEmpty()
                    val existsInFavorites = current.contains(safeAttractionId)
                    val updated = if (existsInFavorites) {
                        current.filterNot { it == safeAttractionId }
                    } else {
                        require(current.size < 100) { "You can save up to 100 favorite attractions." }
                        current + safeAttractionId
                    }
                    transaction.update(
                        docRef,
                        mapOf(
                            "favoriteAttractionIds" to updated,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    !existsInFavorites
                }
            }.await()
            Result.success(added)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    // --- Admin-Managed Local Attractions (Synced live to Room) ---

    fun observePublicAttractions(): Flow<List<com.example.model.Attraction>> = flow {
        val path = "attractions"
        db.collection(path)
            .whereEqualTo("visibility", "public")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    com.example.model.CloudAttraction::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).map { it.toDomain() }
                    .sortedWith(compareByDescending<com.example.model.Attraction> { it.baseRating }.thenBy { it.name.en })
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser != null) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
            .collect { emit(it) }
    }

    suspend fun seedDefaultAttractionsIfEmpty(
        userId: String,
        defaults: List<com.example.model.Attraction>
    ): Result<Boolean> {
        val path = "attractions"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val existingQuery = db.collection(path)
                .whereEqualTo("visibility", "public")
                .limit(1)
                .get()
                .await()
            if (!existingQuery.isEmpty) {
                return Result.success(false)
            }
            for (item in defaults) {
                val docId = item.id.trim().take(128).ifBlank { continue }
                val docRef = db.collection(path).document(docId)
                if (!docRef.get().await().exists()) {
                    val payload = buildAttractionPayload(
                        uid = uid,
                        mapNodeId = item.mapNodeId,
                        nameEn = item.name.en,
                        nameCeb = item.name.ceb,
                        nameTl = item.name.tl,
                        nameEs = item.name.es,
                        subtitleEn = item.subtitle.en,
                        descriptionEn = item.description.en,
                        historyNoteEn = item.historyNote.en,
                        barangay = item.barangay,
                        category = item.categoryCode.ifBlank { item.category.name },
                        latitude = item.latitude,
                        longitude = item.longitude,
                        openHours = item.openHours,
                        entryFeeEn = item.entryFee.en,
                        bestTimeEn = item.bestTime.en,
                        transitHintEn = item.transitHint.en,
                        baseRating = item.baseRating,
                        imageKey = item.imageKey,
                        photoData = item.photoData.ifBlank { null },
                        galleryPhotos = item.galleryPhotos,
                        clearPhoto = false,
                        includeCreationFields = true
                    )
                    docRef.set(payload).await()
                }
            }
            Result.success(true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun upsertCloudAttraction(
        userId: String,
        attractionId: String?,
        mapNodeId: String,
        nameEn: String,
        subtitleEn: String,
        descriptionEn: String,
        historyNoteEn: String,
        barangay: String,
        category: String,
        latitude: Double,
        longitude: Double,
        openHours: String,
        entryFeeEn: String,
        bestTimeEn: String,
        transitHintEn: String,
        baseRating: Double,
        imageKey: String,
        photoData: String?,
        galleryPhotos: List<String> = emptyList(),
        clearPhoto: Boolean = false
    ): Result<String> {
        val colRef = db.collection("attractions")
        val docRef = if (attractionId.isNullOrBlank()) colRef.document() else colRef.document(attractionId.trim().take(128))
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val exists = !attractionId.isNullOrBlank() && docRef.get().await().exists()
            val payload = buildAttractionPayload(
                uid = uid,
                mapNodeId = mapNodeId,
                nameEn = nameEn,
                nameCeb = nameEn,
                nameTl = nameEn,
                nameEs = nameEn,
                subtitleEn = subtitleEn,
                descriptionEn = descriptionEn,
                historyNoteEn = historyNoteEn,
                barangay = barangay,
                category = category,
                latitude = latitude,
                longitude = longitude,
                openHours = openHours,
                entryFeeEn = entryFeeEn,
                bestTimeEn = bestTimeEn,
                transitHintEn = transitHintEn,
                baseRating = baseRating,
                imageKey = imageKey,
                photoData = photoData,
                galleryPhotos = galleryPhotos,
                clearPhoto = clearPhoto && exists,
                includeCreationFields = !exists
            )
            if (exists) {
                docRef.update(payload).await()
            } else {
                docRef.set(payload).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (attractionId.isNullOrBlank()) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCloudAttraction(userId: String, attractionId: String): Result<Unit> {
        val docPath = "attractions/$attractionId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            require(attractionId.isNotBlank()) { "Attraction ID cannot be blank" }
            db.collection("attractions").document(attractionId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    private fun buildAttractionPayload(
        uid: String,
        mapNodeId: String,
        nameEn: String,
        nameCeb: String,
        nameTl: String,
        nameEs: String,
        subtitleEn: String,
        descriptionEn: String,
        historyNoteEn: String,
        barangay: String,
        category: String,
        latitude: Double,
        longitude: Double,
        openHours: String,
        entryFeeEn: String,
        bestTimeEn: String,
        transitHintEn: String,
        baseRating: Double,
        imageKey: String,
        photoData: String?,
        galleryPhotos: List<String>,
        clearPhoto: Boolean,
        includeCreationFields: Boolean
    ): Map<String, Any> {
        val cleanName = nameEn.trim().take(120)
        val cleanSubtitle = subtitleEn.trim().take(160)
        val cleanDesc = descriptionEn.trim().take(1200)
        val cleanHistory = historyNoteEn.trim().take(1000)
        val cleanBarangay = barangay.trim().take(80)
        val cleanOpen = openHours.trim().take(100)
        val cleanFee = entryFeeEn.trim().take(120)
        val cleanBest = bestTimeEn.trim().take(120)
        val cleanTransit = transitHintEn.trim().take(240)
        val cleanNode = mapNodeId.trim().ifBlank { "node_poblacion" }.take(80)
        val cleanImageKey = imageKey.trim().ifBlank { "waterfall" }.take(40)
        val cleanPhoto = photoData?.trim()
        val cleanGallery = galleryPhotos.map { it.trim() }.filter { it.isNotEmpty() && it.length <= 65536 }.take(4)
        val normalizedCat = com.example.model.CategoryCatalog.normalizeCode(category)
        val validCategory = if (com.example.model.CategoryCatalog.isValidCode(normalizedCat)) {
            normalizedCat
        } else {
            "NATURE"
        }
        val validLat = latitude.coerceIn(-90.0, 90.0)
        val validLon = longitude.coerceIn(-180.0, 180.0)
        val validRating = baseRating.coerceIn(1.0, 5.0)

        require(cleanName.length >= 2) { "Attraction name must be at least 2 characters." }
        require(cleanSubtitle.length >= 2) { "Subtitle must be at least 2 characters." }
        require(cleanDesc.length >= 5) { "Description must be at least 5 characters." }
        require(cleanBarangay.length >= 2) { "Barangay must be at least 2 characters." }
        require(cleanOpen.length >= 2) { "Open hours must be at least 2 characters." }
        require(cleanFee.isNotEmpty()) { "Entry fee cannot be empty." }
        require(cleanBest.isNotEmpty()) { "Best time to visit cannot be empty." }
        require(cleanTransit.isNotEmpty()) { "Transit hint cannot be empty." }
        if (cleanPhoto != null) {
            require(cleanPhoto.length <= 65536) { "Cover image exceeds size limit." }
        }

        return buildMap {
            if (includeCreationFields) {
                put("userId", uid)
                put("visibility", "public")
                put("createdAt", FieldValue.serverTimestamp())
            }
            put("mapNodeId", cleanNode)
            put("nameEn", cleanName)
            if (nameCeb.isNotBlank()) put("nameCeb", nameCeb.trim().take(120))
            if (nameTl.isNotBlank()) put("nameTl", nameTl.trim().take(120))
            if (nameEs.isNotBlank()) put("nameEs", nameEs.trim().take(120))
            put("subtitleEn", cleanSubtitle)
            put("descriptionEn", cleanDesc)
            if (cleanHistory.isNotEmpty()) put("historyNoteEn", cleanHistory)
            put("barangay", cleanBarangay)
            put("category", validCategory)
            put("latitude", validLat)
            put("longitude", validLon)
            put("openHours", cleanOpen)
            put("entryFeeEn", cleanFee)
            put("bestTimeEn", cleanBest)
            put("transitHintEn", cleanTransit)
            put("baseRating", validRating)
            put("imageKey", cleanImageKey)
            if (clearPhoto) {
                put("photoData", FieldValue.delete())
            } else if (!cleanPhoto.isNullOrEmpty()) {
                put("photoData", cleanPhoto)
            }
            put("galleryPhotos", cleanGallery)
            put("updatedAt", FieldValue.serverTimestamp())
        }
    }

    // --- Admin-Managed Attraction Categories (Synced live across all travelers) ---

    fun observePublicCategories(): Flow<List<com.example.model.ManagedCategory>> = flow {
        val path = "attraction_categories"
        db.collection(path)
            .whereEqualTo("visibility", "public")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    com.example.model.ManagedCategory::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedWith(compareBy<com.example.model.ManagedCategory> { it.sortOrder }.thenBy { it.name.lowercase(Locale.ROOT) })
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser != null) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
            .collect { emit(it) }
    }

    suspend fun seedDefaultCategoriesIfEmpty(
        userId: String,
        defaults: List<com.example.model.ManagedCategory>
    ): Result<Boolean> {
        val path = "attraction_categories"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val existingQuery = db.collection(path)
                .whereEqualTo("visibility", "public")
                .limit(1)
                .get()
                .await()
            if (!existingQuery.isEmpty) {
                return Result.success(false)
            }
            for (item in defaults) {
                val docId = item.effectiveCode.takeIf { com.example.model.CategoryCatalog.isValidCode(it) } ?: continue
                val docRef = db.collection(path).document(docId)
                if (!docRef.get().await().exists()) {
                    val payload = buildCategoryPayload(
                        uid = uid,
                        code = docId,
                        name = item.name,
                        iconKey = item.iconKey,
                        sortOrder = item.sortOrder,
                        includeCreationFields = true
                    )
                    docRef.set(payload).await()
                }
            }
            Result.success(true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun upsertCloudCategory(
        userId: String,
        categoryId: String?,
        code: String,
        name: String,
        iconKey: String,
        sortOrder: Long
    ): Result<String> {
        val cleanCode = com.example.model.CategoryCatalog.normalizeCode(code.ifBlank { name })
        val resolvedDocId = categoryId?.trim()?.take(128)?.takeIf { it.isNotBlank() } ?: cleanCode
        val colRef = db.collection("attraction_categories")
        val docRef = colRef.document(resolvedDocId.ifBlank { "CUSTOM_SPOT" })
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            require(com.example.model.CategoryCatalog.isValidCode(cleanCode)) {
                "Category name must produce a valid code (at least 2 letters or numbers)."
            }
            val exists = docRef.get().await().exists()
            val payload = buildCategoryPayload(
                uid = uid,
                code = cleanCode,
                name = name,
                iconKey = iconKey,
                sortOrder = sortOrder,
                includeCreationFields = !exists
            )
            if (exists) {
                docRef.update(payload).await()
            } else {
                docRef.set(payload).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (categoryId.isNullOrBlank()) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCloudCategory(userId: String, categoryId: String): Result<Unit> {
        val docPath = "attraction_categories/$categoryId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            require(categoryId.isNotBlank()) { "Category ID cannot be blank" }
            db.collection("attraction_categories").document(categoryId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    private fun buildCategoryPayload(
        uid: String,
        code: String,
        name: String,
        iconKey: String,
        sortOrder: Long,
        includeCreationFields: Boolean
    ): Map<String, Any> {
        val cleanCode = com.example.model.CategoryCatalog.normalizeCode(code)
        val cleanName = name.trim().take(50)
        val cleanIcon = iconKey.trim().lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9_]+"), "")
            .take(32)
            .ifBlank { "forest" }
        val safeOrder = sortOrder.coerceIn(0L, 999L)

        require(com.example.model.CategoryCatalog.isValidCode(cleanCode)) {
            "Category code must be 2–40 uppercase letters, digits, or underscores."
        }
        require(cleanName.length >= 2) { "Category name must be at least 2 characters." }

        return buildMap {
            if (includeCreationFields) {
                put("userId", uid)
                put("code", cleanCode)
                put("visibility", "public")
                put("createdAt", FieldValue.serverTimestamp())
            }
            put("name", cleanName)
            put("iconKey", cleanIcon)
            put("sortOrder", safeOrder)
            put("updatedAt", FieldValue.serverTimestamp())
        }
    }

    // --- Admin-Managed Emergency Contacts ---

    fun observePublicEmergencyContacts(): Flow<List<com.example.model.EmergencyContact>> = flow {
        val path = "emergency_contacts"
        db.collection(path)
            .whereEqualTo("visibility", "public")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    com.example.model.CloudEmergencyContact::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).map { it.toDomain() }
                    .sortedWith(compareBy({ emergencyPriorityOrder(it.priorityLevel) }, { it.title.en }))
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser != null) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
            .collect { emit(it) }
    }

    suspend fun seedDefaultEmergencyContactsIfEmpty(
        userId: String,
        defaults: List<com.example.model.EmergencyContact>
    ): Result<Boolean> {
        val path = "emergency_contacts"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val existingQuery = db.collection(path)
                .whereEqualTo("visibility", "public")
                .limit(1)
                .get()
                .await()
            if (!existingQuery.isEmpty) {
                return Result.success(false)
            }
            for (item in defaults) {
                val docId = item.id.trim().take(128).ifBlank { continue }
                val docRef = db.collection(path).document(docId)
                if (!docRef.get().await().exists()) {
                    val payload = buildEmergencyPayload(
                        uid = uid,
                        titleEn = item.title.en,
                        department = item.department,
                        phoneNumber = item.phoneNumber,
                        availability = item.availability,
                        priorityLevel = item.priorityLevel,
                        email = item.email,
                        websiteUrl = item.websiteUrl,
                        includeCreationFields = true,
                        clearOptionalOnUpdate = false
                    )
                    docRef.set(payload).await()
                }
            }
            Result.success(true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun upsertCloudEmergencyContact(
        userId: String,
        contactId: String?,
        titleEn: String,
        department: String,
        phoneNumber: String,
        availability: String,
        priorityLevel: String,
        email: String?,
        websiteUrl: String?
    ): Result<String> {
        val colRef = db.collection("emergency_contacts")
        val docRef = if (contactId.isNullOrBlank()) colRef.document() else colRef.document(contactId.trim().take(128))
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val exists = !contactId.isNullOrBlank() && docRef.get().await().exists()
            val payload = buildEmergencyPayload(
                uid = uid,
                titleEn = titleEn,
                department = department,
                phoneNumber = phoneNumber,
                availability = availability,
                priorityLevel = priorityLevel,
                email = email,
                websiteUrl = websiteUrl,
                includeCreationFields = !exists,
                clearOptionalOnUpdate = exists
            )
            if (exists) {
                docRef.update(payload).await()
            } else {
                docRef.set(payload).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (contactId.isNullOrBlank()) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCloudEmergencyContact(userId: String, contactId: String): Result<Unit> {
        val docPath = "emergency_contacts/$contactId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            require(contactId.isNotBlank()) { "Emergency contact ID cannot be blank" }
            db.collection("emergency_contacts").document(contactId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    private fun buildEmergencyPayload(
        uid: String,
        titleEn: String,
        department: String,
        phoneNumber: String,
        availability: String,
        priorityLevel: String,
        email: String?,
        websiteUrl: String?,
        includeCreationFields: Boolean,
        clearOptionalOnUpdate: Boolean
    ): Map<String, Any> {
        val cleanTitle = titleEn.trim().take(140)
        val cleanDept = department.trim().take(160)
        val cleanPhone = phoneNumber.trim().take(40)
        val cleanAvail = availability.trim().take(100)
        val allowedPriorities = setOf("CRITICAL", "HIGH", "MEDICAL", "STANDARD")
        val normalizedPriority = if (priorityLevel in allowedPriorities) priorityLevel else "STANDARD"
        val cleanEmail = email?.trim()?.take(120).orEmpty()
        val cleanUrl = websiteUrl?.trim()?.take(240).orEmpty()

        require(cleanTitle.length >= 2) { "Contact title must be at least 2 characters." }
        require(cleanDept.length >= 2) { "Department must be at least 2 characters." }
        require(cleanPhone.length >= 2) { "Phone number must be at least 2 characters." }
        require(cleanAvail.length >= 2) { "Availability must be at least 2 characters." }

        return buildMap {
            if (includeCreationFields) {
                put("userId", uid)
                put("visibility", "public")
                put("createdAt", FieldValue.serverTimestamp())
            }
            put("titleEn", cleanTitle)
            put("department", cleanDept)
            put("phoneNumber", cleanPhone)
            put("availability", cleanAvail)
            put("priorityLevel", normalizedPriority)
            if (cleanEmail.isNotEmpty()) {
                put("email", cleanEmail)
            } else if (clearOptionalOnUpdate) {
                put("email", FieldValue.delete())
            }
            if (cleanUrl.isNotEmpty()) {
                put("websiteUrl", cleanUrl)
            } else if (clearOptionalOnUpdate) {
                put("websiteUrl", FieldValue.delete())
            }
            put("updatedAt", FieldValue.serverTimestamp())
        }
    }

    private fun emergencyPriorityOrder(priority: String): Int = when (priority) {
        "CRITICAL" -> 0
        "HIGH" -> 1
        "MEDICAL" -> 2
        else -> 3
    }

    // --- Admin-Managed Dumingag Tourism Events & Trail Advisories ---

    fun observePublicTourismEvents(): Flow<List<TourismEvent>> = flow {
        val path = "tourism_events"
        db.collection(path)
            .whereEqualTo("visibility", "public")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(TourismEvent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(
                        compareByDescending<TourismEvent> { it.active }
                            .thenBy { eventSeverityOrder(it.severity) }
                            .thenByDescending { it.createdAt?.seconds ?: 0L }
                    )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser != null) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
            .collect { emit(it) }
    }

    suspend fun seedDefaultTourismEventsIfEmpty(
        userId: String,
        defaults: List<TourismEvent>
    ): Result<Boolean> {
        val path = "tourism_events"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val existing = db.collection(path).whereEqualTo("visibility", "public").limit(1).get().await()
            if (!existing.isEmpty) return Result.success(false)
            for (event in defaults) {
                val docId = event.id.trim().take(128).ifBlank { continue }
                val docRef = db.collection(path).document(docId)
                if (!docRef.get().await().exists()) {
                    docRef.set(
                        buildTourismEventPayload(
                            uid = uid,
                            title = event.title,
                            summary = event.summary,
                            eventType = event.eventType,
                            severity = event.severity,
                            dateLabel = event.dateLabel,
                            locationName = event.locationName,
                            attractionId = event.attractionId,
                            active = event.active,
                            includeCreationFields = true
                        )
                    ).await()
                }
            }
            Result.success(true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun upsertTourismEvent(
        userId: String,
        eventId: String?,
        title: String,
        summary: String,
        eventType: String,
        severity: String,
        dateLabel: String,
        locationName: String,
        attractionId: String,
        active: Boolean
    ): Result<String> {
        val colRef = db.collection("tourism_events")
        val docRef = if (eventId.isNullOrBlank()) colRef.document() else colRef.document(eventId.trim().take(128))
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val exists = !eventId.isNullOrBlank() && docRef.get().await().exists()
            val payload = buildTourismEventPayload(
                uid = uid,
                title = title,
                summary = summary,
                eventType = eventType,
                severity = severity,
                dateLabel = dateLabel,
                locationName = locationName,
                attractionId = attractionId,
                active = active,
                includeCreationFields = !exists
            )
            if (exists) {
                docRef.update(payload).await()
            } else {
                docRef.set(payload).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (eventId.isNullOrBlank()) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteTourismEvent(userId: String, eventId: String): Result<Unit> {
        val docPath = "tourism_events/$eventId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            require(eventId.isNotBlank()) { "Event ID cannot be blank" }
            db.collection("tourism_events").document(eventId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    private fun buildTourismEventPayload(
        uid: String,
        title: String,
        summary: String,
        eventType: String,
        severity: String,
        dateLabel: String,
        locationName: String,
        attractionId: String,
        active: Boolean,
        includeCreationFields: Boolean
    ): Map<String, Any> {
        val cleanTitle = title.trim().take(120)
        val cleanSummary = summary.trim().take(500)
        val cleanDate = dateLabel.trim().take(80)
        val cleanLoc = locationName.trim().take(120)
        val cleanSpotId = attractionId.trim().take(80)
        require(cleanTitle.length >= 2) { "Event title must be at least 2 characters." }
        require(cleanSummary.length >= 3) { "Summary must be at least 3 characters." }
        require(cleanDate.length >= 2) { "Schedule/date label must be at least 2 characters." }
        require(cleanLoc.length >= 2) { "Location must be at least 2 characters." }
        val validType = TourismEventType.fromCode(eventType).code
        val validSeverity = TourismEventSeverity.fromCode(severity).code

        return buildMap {
            if (includeCreationFields) {
                put("userId", uid)
                put("visibility", "public")
                put("createdAt", FieldValue.serverTimestamp())
            }
            put("title", cleanTitle)
            put("summary", cleanSummary)
            put("eventType", validType)
            put("severity", validSeverity)
            put("dateLabel", cleanDate)
            put("locationName", cleanLoc)
            put("attractionId", cleanSpotId)
            put("active", active)
            put("updatedAt", FieldValue.serverTimestamp())
        }
    }

    private fun eventSeverityOrder(severity: String): Int = when (severity) {
        TourismEventSeverity.ALERT.code -> 0
        TourismEventSeverity.ADVISORY.code -> 1
        else -> 2
    }
}


