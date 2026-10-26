package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Category
import com.example.data.model.Inquiry
import com.example.data.model.Market
import com.example.data.model.MarketHubDefaults
import com.example.data.model.Offer
import com.example.data.model.Order
import com.example.data.model.PaymentSettings
import com.example.data.model.Product
import com.example.data.model.Report
import com.example.data.model.Shop
import com.example.data.model.UserProfile
import com.example.data.model.normalizeProductNameForComparison
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class MarketHubRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun isCurrentUserBootstrappedAdmin(): Boolean {
        val user = auth.currentUser ?: return false
        return user.email.equals(MarketHubDefaults.BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true)
    }

    suspend fun checkIsAdmin(uid: String): Boolean {
        if (isCurrentUserBootstrappedAdmin()) return true
        val path = "admins/$uid"
        return try {
            val doc = db.collection("admins").document(uid).get().await()
            doc.exists()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }

    // --- USER PROFILE ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                throw error
            }
    }

    suspend fun ensureUserProfile(
        displayName: String,
        email: String,
        preferredRole: String = "customer"
    ): Result<UserProfile> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                val profile = existing.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: UserProfile(id = uid, userId = uid, displayName = displayName, email = email)
                Result.success(profile)
            } else {
                val isAdminUser = email.equals(MarketHubDefaults.BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true)
                val safeRole = when {
                    isAdminUser -> "admin"
                    preferredRole == "shopkeeper" -> "shopkeeper"
                    else -> "customer"
                }
                val safeName = displayName.trim().ifBlank { email.substringBefore("@").ifBlank { "MarketHub User" } }.take(100)
                val safeEmail = email.trim().ifBlank { "user@markethub.pk" }.take(150)
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to safeEmail,
                    "role" to safeRole,
                    "phone" to "",
                    "isSuspended" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                val created = docRef.get().await().toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: UserProfile(id = uid, userId = uid, displayName = safeName, email = safeEmail, role = safeRole)
                Result.success(created)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(
        userId: String,
        displayName: String,
        phone: String,
        role: String
    ): Result<Unit> {
        val docRef = db.collection("users").document(userId)
        return try {
            val updates = mapOf(
                "displayName" to displayName.trim().take(100),
                "phone" to phone.trim().take(30),
                "role" to role,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- MARKETS & CATEGORIES ---

    fun observeActiveMarkets(): Flow<List<Market>> {
        val path = "markets"
        return db.collection(path)
            .whereEqualTo("isActive", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Market::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeActiveCategories(): Flow<List<Category>> {
        val path = "categories"
        return db.collection(path)
            .whereEqualTo("isActive", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Category::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.displayOrder }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun saveCategoryAsAdmin(
        categoryId: String?,
        name: String,
        iconName: String,
        description: String,
        displayOrder: Long,
        isActive: Boolean = true
    ): Result<String> {
        requireUserId()
        val id = categoryId?.takeIf { it.isNotBlank() } ?: "cat_${UUID.randomUUID().toString().take(8)}"
        val docRef = db.collection("categories").document(id)
        val slug = name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "category" }
        return try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                docRef.update(
                    mapOf(
                        "name" to name.trim().take(80),
                        "slug" to slug.take(80),
                        "iconName" to iconName.trim().ifBlank { "store" }.take(50),
                        "description" to description.trim().ifBlank { name }.take(250),
                        "displayOrder" to displayOrder,
                        "isActive" to isActive,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "name" to name.trim().take(80),
                        "slug" to slug.take(80),
                        "iconName" to iconName.trim().ifBlank { "store" }.take(50),
                        "description" to description.trim().ifBlank { name }.take(250),
                        "displayOrder" to displayOrder,
                        "isActive" to isActive,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCategoryAsAdmin(categoryId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("categories").document(categoryId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun saveMarketAsAdmin(
        marketId: String?,
        name: String,
        city: String,
        address: String,
        description: String,
        isActive: Boolean = true
    ): Result<String> {
        requireUserId()
        val id = marketId?.takeIf { it.isNotBlank() } ?: "mkt_${UUID.randomUUID().toString().take(8)}"
        val docRef = db.collection("markets").document(id)
        return try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                docRef.update(
                    mapOf(
                        "name" to name.trim().take(100),
                        "city" to city.trim().take(60),
                        "address" to address.trim().take(250),
                        "description" to description.trim().take(500),
                        "isActive" to isActive,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "name" to name.trim().take(100),
                        "city" to city.trim().take(60),
                        "address" to address.trim().take(250),
                        "description" to description.trim().take(500),
                        "isActive" to isActive,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun seedDefaultCategoriesAndMarketsAsAdmin(): Result<Unit> {
        requireUserId()
        return try {
            for (cat in MarketHubDefaults.defaultCategories) {
                val ref = db.collection("categories").document(cat.id)
                if (!ref.get().await().exists()) {
                    ref.set(
                        mapOf(
                            "name" to cat.name,
                            "slug" to cat.slug,
                            "iconName" to cat.iconName,
                            "description" to cat.description,
                            "displayOrder" to cat.displayOrder,
                            "isActive" to true,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            for (mkt in MarketHubDefaults.defaultMarkets) {
                val ref = db.collection("markets").document(mkt.id)
                if (!ref.get().await().exists()) {
                    ref.set(
                        mapOf(
                            "name" to mkt.name,
                            "city" to mkt.city,
                            "address" to mkt.address,
                            "description" to mkt.description,
                            "isActive" to true,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "categories/markets")
            Result.failure(e)
        }
    }

    // --- SHOPS ---

    fun observeApprovedShops(): Flow<List<Shop>> {
        val path = "shops"
        return db.collection(path)
            .whereEqualTo("status", "approved")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Shop::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeMyShops(ownerId: String): Flow<List<Shop>> {
        val path = "shops"
        return db.collection(path)
            .whereEqualTo("ownerId", ownerId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Shop::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeAllShopsForAdmin(): Flow<List<Shop>> {
        val path = "shops"
        return db.collection(path)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Shop::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun getShopById(shopId: String): Result<Shop> {
        val docRef = db.collection("shops").document(shopId)
        return try {
            val snapshot = docRef.get().await()
            val shop = snapshot.toObject(Shop::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: throw IllegalStateException("Shop not found")
            Result.success(shop)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun submitShopRegistration(
        ownerName: String,
        name: String,
        categoryId: String,
        categoryName: String,
        marketId: String,
        marketName: String,
        address: String,
        mapLocation: String,
        description: String,
        businessHours: String,
        contactPhone: String,
        whatsappNumber: String,
        logoUrl: String = "",
        coverUrl: String = ""
    ): Result<String> {
        val uid = requireUserId()
        val shopId = "shop_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val docRef = db.collection("shops").document(shopId)
        val payload = mapOf(
            "ownerId" to uid,
            "ownerName" to ownerName.trim().take(100),
            "name" to name.trim().take(100),
            "categoryId" to categoryId.trim(),
            "categoryName" to categoryName.trim().take(100),
            "marketId" to marketId.trim(),
            "marketName" to marketName.trim().take(100),
            "address" to address.trim().take(250),
            "mapLocation" to mapLocation.trim().take(250),
            "description" to description.trim().take(1000),
            "businessHours" to businessHours.trim().take(120),
            "contactPhone" to contactPhone.trim().take(30),
            "whatsappNumber" to whatsappNumber.trim().take(30),
            "logoUrl" to logoUrl.trim().take(800),
            "coverUrl" to coverUrl.trim().take(800),
            "status" to "pending",
            "isVerified" to false,
            "isFeatured" to false,
            "viewsCount" to 0L,
            "inquiriesCount" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(shopId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateShopDetailsByOwner(
        shopId: String,
        name: String,
        categoryId: String,
        categoryName: String,
        marketId: String,
        marketName: String,
        address: String,
        mapLocation: String,
        description: String,
        businessHours: String,
        contactPhone: String,
        whatsappNumber: String,
        logoUrl: String,
        coverUrl: String
    ): Result<Unit> {
        requireUserId()
        val docRef = db.collection("shops").document(shopId)
        val updates = mapOf(
            "name" to name.trim().take(100),
            "categoryId" to categoryId.trim(),
            "categoryName" to categoryName.trim().take(100),
            "marketId" to marketId.trim(),
            "marketName" to marketName.trim().take(100),
            "address" to address.trim().take(250),
            "mapLocation" to mapLocation.trim().take(250),
            "description" to description.trim().take(1000),
            "businessHours" to businessHours.trim().take(120),
            "contactPhone" to contactPhone.trim().take(30),
            "whatsappNumber" to whatsappNumber.trim().take(30),
            "logoUrl" to logoUrl.trim().take(800),
            "coverUrl" to coverUrl.trim().take(800),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateShopModerationByAdmin(
        shopId: String,
        status: String,
        isVerified: Boolean,
        isFeatured: Boolean
    ): Result<Unit> {
        requireUserId()
        val docRef = db.collection("shops").document(shopId)
        return try {
            docRef.update(
                mapOf(
                    "status" to status,
                    "isVerified" to isVerified,
                    "isFeatured" to isFeatured,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun incrementShopViewCount(shop: Shop) {
        if (shop.id.isBlank() || shop.status != "approved") return
        val docRef = db.collection("shops").document(shop.id)
        try {
            docRef.update(
                mapOf(
                    "viewsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            // Non-blocking telemetry
        }
    }

    // --- PRODUCTS ---

    fun observeActiveProducts(): Flow<List<Product>> {
        val path = "products"
        return db.collection(path)
            .whereEqualTo("status", "active")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeMyProducts(ownerId: String): Flow<List<Product>> {
        val path = "products"
        return db.collection(path)
            .whereEqualTo("ownerId", ownerId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeAllProductsForAdmin(): Flow<List<Product>> {
        val path = "products"
        return db.collection(path)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun createProduct(
        shop: Shop,
        name: String,
        categoryId: String,
        categoryName: String,
        description: String,
        pricePkr: Long,
        originalPricePkr: Long = 0L,
        stockQuantity: Long = 1L,
        isAvailable: Boolean = true,
        isNewArrival: Boolean = true,
        isPopular: Boolean = false,
        imageUrl: String = ""
    ): Result<String> {
        val uid = requireUserId()
        val productId = "prod_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val docRef = db.collection("products").document(productId)
        val trimmedName = name.trim().take(120)
        val payload = mapOf(
            "shopId" to shop.id,
            "shopName" to shop.name.take(100),
            "marketName" to shop.marketName.take(100),
            "ownerId" to uid,
            "name" to trimmedName,
            "normalizedName" to normalizeProductNameForComparison(trimmedName),
            "categoryId" to categoryId.trim(),
            "categoryName" to categoryName.trim().take(100),
            "description" to description.trim().take(1000),
            "pricePkr" to pricePkr,
            "originalPricePkr" to originalPricePkr.coerceAtLeast(0L),
            "stockQuantity" to stockQuantity.coerceAtLeast(0L),
            "isAvailable" to isAvailable,
            "isNewArrival" to isNewArrival,
            "isPopular" to isPopular,
            "imageUrl" to imageUrl.trim().take(800),
            "whatsappNumber" to shop.whatsappNumber.take(30),
            "status" to "active",
            "viewsCount" to 0L,
            "inquiriesCount" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(productId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateProductByOwner(
        productId: String,
        name: String,
        categoryId: String,
        categoryName: String,
        description: String,
        pricePkr: Long,
        originalPricePkr: Long,
        stockQuantity: Long,
        isAvailable: Boolean,
        isNewArrival: Boolean,
        isPopular: Boolean,
        imageUrl: String
    ): Result<Unit> {
        requireUserId()
        val docRef = db.collection("products").document(productId)
        val trimmedName = name.trim().take(120)
        val updates = mapOf(
            "name" to trimmedName,
            "normalizedName" to normalizeProductNameForComparison(trimmedName),
            "categoryId" to categoryId.trim(),
            "categoryName" to categoryName.trim().take(100),
            "description" to description.trim().take(1000),
            "pricePkr" to pricePkr,
            "originalPricePkr" to originalPricePkr.coerceAtLeast(0L),
            "stockQuantity" to stockQuantity.coerceAtLeast(0L),
            "isAvailable" to isAvailable,
            "isNewArrival" to isNewArrival,
            "isPopular" to isPopular,
            "imageUrl" to imageUrl.trim().take(800),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateProductStatusByAdmin(
        productId: String,
        status: String,
        isPopular: Boolean
    ): Result<Unit> {
        requireUserId()
        val docRef = db.collection("products").document(productId)
        return try {
            docRef.update(
                mapOf(
                    "status" to status,
                    "isPopular" to isPopular,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("products").document(productId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun incrementProductViewCount(product: Product) {
        if (product.id.isBlank() || product.status != "active") return
        val docRef = db.collection("products").document(product.id)
        try {
            docRef.update(
                mapOf(
                    "viewsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            // Non-blocking
        }
    }

    // --- OFFERS ---

    fun observeActiveOffers(): Flow<List<Offer>> {
        val path = "offers"
        return db.collection(path)
            .whereEqualTo("isActive", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Offer::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeMyOffers(ownerId: String): Flow<List<Offer>> {
        val path = "offers"
        return db.collection(path)
            .whereEqualTo("ownerId", ownerId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Offer::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun createOffer(
        shop: Shop,
        title: String,
        description: String,
        discountBadge: String,
        promoPricePkr: Long,
        originalPricePkr: Long,
        productId: String = ""
    ): Result<String> {
        val uid = requireUserId()
        val offerId = "offer_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val docRef = db.collection("offers").document(offerId)
        val payload = mapOf(
            "shopId" to shop.id,
            "shopName" to shop.name.take(100),
            "marketName" to shop.marketName.take(100),
            "ownerId" to uid,
            "productId" to productId.trim().take(128),
            "title" to title.trim().take(120),
            "description" to description.trim().take(500),
            "discountBadge" to discountBadge.trim().take(40),
            "promoPricePkr" to promoPricePkr,
            "originalPricePkr" to originalPricePkr,
            "whatsappNumber" to shop.whatsappNumber.take(30),
            "isActive" to true,
            "isFeatured" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(offerId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleOfferFeaturedByAdmin(offerId: String, isFeatured: Boolean, isActive: Boolean): Result<Unit> {
        requireUserId()
        val docRef = db.collection("offers").document(offerId)
        return try {
            docRef.update(
                mapOf(
                    "isFeatured" to isFeatured,
                    "isActive" to isActive,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteOffer(offerId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("offers").document(offerId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- INQUIRIES ---

    fun observeCustomerInquiries(customerId: String): Flow<List<Inquiry>> {
        val path = "inquiries"
        return db.collection(path)
            .whereEqualTo("customerId", customerId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Inquiry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeShopkeeperInquiries(shopOwnerId: String): Flow<List<Inquiry>> {
        val path = "inquiries"
        return db.collection(path)
            .whereEqualTo("shopOwnerId", shopOwnerId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Inquiry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun submitCustomerInquiry(
        customerName: String,
        customerPhone: String,
        shopId: String,
        shopName: String,
        shopOwnerId: String,
        productId: String,
        productName: String,
        displayedPricePkr: Long,
        message: String,
        channel: String
    ): Result<String> {
        val uid = requireUserId()
        val inquiryId = "inq_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val docRef = db.collection("inquiries").document(inquiryId)
        val payload = mapOf(
            "customerId" to uid,
            "customerName" to customerName.trim().ifBlank { "Customer" }.take(100),
            "customerPhone" to customerPhone.trim().take(30),
            "shopId" to shopId,
            "shopName" to shopName.trim().take(100),
            "shopOwnerId" to shopOwnerId,
            "productId" to productId.trim().take(128),
            "productName" to productName.trim().ifBlank { "General Shop Inquiry" }.take(120),
            "displayedPricePkr" to displayedPricePkr.coerceAtLeast(0L),
            "message" to message.trim().take(1000),
            "channel" to if (channel == "in_app") "in_app" else "whatsapp",
            "status" to "new",
            "shopkeeperReply" to "",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            // Also increment shop and product inquiry counters (best-effort)
            try {
                db.collection("shops").document(shopId).update(
                    mapOf(
                        "inquiriesCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                if (productId.isNotBlank()) {
                    db.collection("products").document(productId).update(
                        mapOf(
                            "inquiriesCount" to FieldValue.increment(1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            } catch (_: Exception) {
            }
            Result.success(inquiryId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun replyToInquiry(
        inquiryId: String,
        replyText: String,
        closeInquiry: Boolean = false
    ): Result<Unit> {
        requireUserId()
        val docRef = db.collection("inquiries").document(inquiryId)
        val newStatus = if (closeInquiry) "closed" else "replied"
        return try {
            docRef.update(
                mapOf(
                    "status" to newStatus,
                    "shopkeeperReply" to replyText.trim().take(1000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- REPORTS ---

    fun observeAllReportsForAdmin(): Flow<List<Report>> {
        val path = "reports"
        return db.collection(path)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Report::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun submitReport(
        reporterName: String,
        targetType: String,
        targetId: String,
        targetName: String,
        shopId: String,
        reason: String
    ): Result<String> {
        val uid = requireUserId()
        val reportId = "rep_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val docRef = db.collection("reports").document(reportId)
        val payload = mapOf(
            "reporterId" to uid,
            "reporterName" to reporterName.trim().ifBlank { "Customer" }.take(100),
            "targetType" to if (targetType == "shop") "shop" else "product",
            "targetId" to targetId,
            "targetName" to targetName.trim().take(120),
            "shopId" to shopId,
            "reason" to reason.trim().take(500),
            "status" to "open",
            "adminNotes" to "",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(reportId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun resolveReportByAdmin(
        reportId: String,
        status: String,
        adminNotes: String
    ): Result<Unit> {
        requireUserId()
        val docRef = db.collection("reports").document(reportId)
        return try {
            docRef.update(
                mapOf(
                    "status" to status,
                    "adminNotes" to adminNotes.trim().take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- PAYMENT SETTINGS (ADMIN CONFIGURED) ---

    fun observePaymentSettings(): Flow<PaymentSettings> {
        val path = "payment_settings/global"
        return db.collection("payment_settings").document("global")
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(PaymentSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: PaymentSettings()
                } else {
                    PaymentSettings()
                }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                emit(PaymentSettings())
            }
    }

    suspend fun savePaymentSettingsAsAdmin(
        bankTransferEnabled: Boolean,
        bankName: String,
        bankAccountTitle: String,
        bankAccountNumberOrIban: String,
        bankInstructions: String,
        easypaisaEnabled: Boolean,
        easypaisaAccountHolderName: String,
        easypaisaMobileNumber: String,
        easypaisaInstructions: String,
        binancePayEnabled: Boolean,
        binanceMerchantName: String,
        binancePayIdOrInstructions: String,
        codEnabledByDefault: Boolean,
        allowShopkeeperPaymentVerification: Boolean
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("payment_settings").document("global")
        val payload = mapOf(
            "bankTransferEnabled" to bankTransferEnabled,
            "bankName" to bankName.trim().take(120),
            "bankAccountTitle" to bankAccountTitle.trim().take(120),
            "bankAccountNumberOrIban" to bankAccountNumberOrIban.trim().take(80),
            "bankInstructions" to bankInstructions.trim().take(600),
            "easypaisaEnabled" to easypaisaEnabled,
            "easypaisaAccountHolderName" to easypaisaAccountHolderName.trim().take(120),
            "easypaisaMobileNumber" to easypaisaMobileNumber.trim().take(40),
            "easypaisaInstructions" to easypaisaInstructions.trim().take(600),
            "binancePayEnabled" to binancePayEnabled,
            "binanceMerchantName" to binanceMerchantName.trim().take(120),
            "binancePayIdOrInstructions" to binancePayIdOrInstructions.trim().take(600),
            "codEnabledByDefault" to codEnabledByDefault,
            "allowShopkeeperPaymentVerification" to allowShopkeeperPaymentVerification,
            "updatedByUid" to uid,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    // --- WHATSAPP DIRECT PURCHASE ORDERS ---

    fun observeCustomerOrders(customerId: String): Flow<List<Order>> {
        val path = "orders"
        return db.collection(path)
            .whereEqualTo("customerId", customerId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Order::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeShopkeeperOrders(shopOwnerId: String): Flow<List<Order>> {
        val path = "orders"
        return db.collection(path)
            .whereEqualTo("shopOwnerId", shopOwnerId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Order::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeAllOrdersForAdmin(): Flow<List<Order>> {
        val path = "orders"
        return db.collection(path)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Order::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    /**
     * Creates a persistent pending order in Firestore before redirecting to WhatsApp.
     * - Re-reads the authoritative Product document from Firestore to validate availability, stock, and pricePkr.
     * - Uses a deterministic document ID derived from idempotencyKey so repeated taps do not create duplicate orders.
     */
    suspend fun createDirectPurchaseOrder(
        idempotencyKey: String,
        productId: String,
        quantity: Long,
        customerName: String,
        customerPhone: String,
        deliveryType: String,
        deliveryAddress: String,
        paymentMethod: String
    ): Result<Order> {
        val uid = requireUserId()
        val safeIdempotency = idempotencyKey.replace(Regex("[^a-zA-Z0-9_\\-]"), "").take(80)
            .ifBlank { "idem_${uid.take(8)}_${productId.take(12)}" }
        val orderDocId = "ord_$safeIdempotency".take(120)
        val orderRef = db.collection("orders").document(orderDocId)

        return try {
            // Idempotency check: if order already exists for this checkout session key, return it immediately
            val existingSnap = orderRef.get().await()
            if (existingSnap.exists()) {
                val existingOrder = existingSnap.toObject(Order::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (existingOrder != null) {
                    return Result.success(existingOrder)
                }
            }

            // Fetch authoritative product snapshot from Firestore
            val productSnap = db.collection("products").document(productId).get().await()
            if (!productSnap.exists()) {
                return Result.failure(IllegalStateException("Product no longer exists in the marketplace."))
            }
            val authoritativeProduct = productSnap.toObject(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: return Result.failure(IllegalStateException("Unable to read product data."))

            if (authoritativeProduct.status != "active" || !authoritativeProduct.isAvailable) {
                return Result.failure(IllegalStateException("This product is currently marked unavailable by the shopkeeper."))
            }
            val validQty = quantity.coerceIn(1L, 1000L)
            if (authoritativeProduct.stockQuantity < validQty) {
                return Result.failure(
                    IllegalStateException("Requested quantity ($validQty) exceeds available stock (${authoritativeProduct.stockQuantity}).")
                )
            }

            val unitPrice = authoritativeProduct.pricePkr
            val subtotal = validQty * unitPrice
            val shortCode = safeIdempotency.takeLast(6).uppercase()
            val orderNumber = "MH-$shortCode"

            val safeCustomerName = customerName.trim().ifBlank { "MarketHub Customer" }.take(100)
            val safeCustomerPhone = customerPhone.trim().ifBlank { "+923000000000" }.take(30)
            val safeDeliveryType = if (deliveryType == "pickup") "pickup" else "delivery"
            val safeAddress = deliveryAddress.trim().ifBlank {
                if (safeDeliveryType == "pickup") "${authoritativeProduct.shopName}, ${authoritativeProduct.marketName}"
                else "Address to be confirmed on WhatsApp"
            }.take(350)

            val payload = mapOf(
                "orderNumber" to orderNumber,
                "idempotencyKey" to safeIdempotency,
                "customerId" to uid,
                "customerName" to safeCustomerName,
                "customerPhone" to safeCustomerPhone,
                "deliveryType" to safeDeliveryType,
                "deliveryAddress" to safeAddress,
                "productId" to authoritativeProduct.id,
                "productName" to authoritativeProduct.name.take(120),
                "shopId" to authoritativeProduct.shopId,
                "shopName" to authoritativeProduct.shopName.take(100),
                "shopOwnerId" to authoritativeProduct.ownerId,
                "shopWhatsappNumber" to authoritativeProduct.whatsappNumber.take(30),
                "quantity" to validQty,
                "unitPricePkr" to unitPrice,
                "subtotalPkr" to subtotal,
                "paymentMethod" to paymentMethod,
                "paymentStatus" to "Awaiting Payment",
                "orderStatus" to "pending",
                "transactionReference" to "",
                "proofScreenshotUri" to "",
                "verifierNotes" to "",
                "verifiedByUid" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            orderRef.set(payload).await()
            val savedSnap = orderRef.get().await()
            val savedOrder = savedSnap.toObject(Order::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: Order(
                    id = orderDocId,
                    orderNumber = orderNumber,
                    idempotencyKey = safeIdempotency,
                    customerId = uid,
                    customerName = safeCustomerName,
                    customerPhone = safeCustomerPhone,
                    deliveryType = safeDeliveryType,
                    deliveryAddress = safeAddress,
                    productId = authoritativeProduct.id,
                    productName = authoritativeProduct.name,
                    shopId = authoritativeProduct.shopId,
                    shopName = authoritativeProduct.shopName,
                    shopOwnerId = authoritativeProduct.ownerId,
                    shopWhatsappNumber = authoritativeProduct.whatsappNumber,
                    quantity = validQty,
                    unitPricePkr = unitPrice,
                    subtotalPkr = subtotal,
                    paymentMethod = paymentMethod,
                    paymentStatus = "Awaiting Payment",
                    orderStatus = "pending"
                )
            Result.success(savedOrder)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, orderRef.path)
            Result.failure(e)
        }
    }

    /**
     * Allows the customer to submit a transaction reference and optional payment screenshot URI.
     * Sets paymentStatus to "Proof Submitted" (never "Payment Verified").
     */
    suspend fun submitOrderPaymentProof(
        orderId: String,
        transactionReference: String,
        proofScreenshotUri: String
    ): Result<Unit> {
        requireUserId()
        val docRef = db.collection("orders").document(orderId)
        val trimmedRef = transactionReference.trim().take(150)
        if (trimmedRef.length < 2) {
            return Result.failure(IllegalArgumentException("Please enter a valid transaction reference or TID."))
        }
        return try {
            docRef.update(
                mapOf(
                    "transactionReference" to trimmedRef,
                    "proofScreenshotUri" to proofScreenshotUri.trim().take(500000),
                    "paymentStatus" to "Proof Submitted",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Allows an authorized Administrator (or authorized Shopkeeper) to update payment verification
     * status ("Under Review", "Payment Verified", "Payment Rejected") and order fulfilment status.
     */
    suspend fun updateOrderAndPaymentStatusByVerifier(
        orderId: String,
        paymentStatus: String,
        orderStatus: String,
        verifierNotes: String
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("orders").document(orderId)
        val verifiedBy = if (paymentStatus == "Payment Verified") uid else ""
        return try {
            docRef.update(
                mapOf(
                    "paymentStatus" to paymentStatus,
                    "orderStatus" to orderStatus,
                    "verifierNotes" to verifierNotes.trim().take(500),
                    "verifiedByUid" to verifiedBy,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }
}
