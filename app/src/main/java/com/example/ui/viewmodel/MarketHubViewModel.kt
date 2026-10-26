package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Category
import com.example.data.model.Inquiry
import com.example.data.model.Market
import com.example.data.model.MarketHubDefaults
import com.example.data.model.Offer
import com.example.data.model.PriceComparisonGroup
import com.example.data.model.Product
import com.example.data.model.Report
import com.example.data.model.Shop
import com.example.data.model.UserProfile
import com.example.data.repository.MarketHubRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class BannerNotification(
    val message: String,
    val isError: Boolean = false
)

enum class ProductSortOption(val label: String) {
    POPULAR("Popularity"),
    NEWEST("Newest Arrivals"),
    PRICE_LOW_HIGH("Price: Low to High"),
    PRICE_HIGH_LOW("Price: High to Low")
}

@OptIn(ExperimentalCoroutinesApi::class)
class MarketHubViewModel(
    private val repository: MarketHubRepository,
    val currentUserId: String,
    val currentUserName: String,
    val currentUserEmail: String
) : ViewModel() {

    private val _isAdmin = MutableStateFlow(repository.isCurrentUserBootstrappedAdmin())
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _banner = MutableStateFlow<BannerNotification?>(null)
    val banner: StateFlow<BannerNotification?> = _banner.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    // Search & filter state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    private val _selectedMarketId = MutableStateFlow<String?>(null)
    val selectedMarketId: StateFlow<String?> = _selectedMarketId.asStateFlow()

    private val _selectedShopIdFilter = MutableStateFlow<String?>(null)
    val selectedShopIdFilter: StateFlow<String?> = _selectedShopIdFilter.asStateFlow()

    private val _minPricePkr = MutableStateFlow<Long?>(null)
    val minPricePkr: StateFlow<Long?> = _minPricePkr.asStateFlow()

    private val _maxPricePkr = MutableStateFlow<Long?>(null)
    val maxPricePkr: StateFlow<Long?> = _maxPricePkr.asStateFlow()

    private val _onlyInStock = MutableStateFlow(false)
    val onlyInStock: StateFlow<Boolean> = _onlyInStock.asStateFlow()

    private val _sortOption = MutableStateFlow(ProductSortOption.POPULAR)
    val sortOption: StateFlow<ProductSortOption> = _sortOption.asStateFlow()

    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .catch { e ->
            Log.w(TAG, "Error observing user profile", e)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val categories: StateFlow<List<Category>> = repository.observeActiveCategories()
        .map { remoteList ->
            if (remoteList.isEmpty()) {
                MarketHubDefaults.defaultCategories
            } else {
                val remoteIds = remoteList.map { it.id }.toSet()
                val merged = remoteList + MarketHubDefaults.defaultCategories.filter { it.id !in remoteIds }
                merged.sortedBy { it.displayOrder }
            }
        }
        .catch { emit(MarketHubDefaults.defaultCategories) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), MarketHubDefaults.defaultCategories)

    val markets: StateFlow<List<Market>> = repository.observeActiveMarkets()
        .map { remoteList ->
            if (remoteList.isEmpty()) {
                MarketHubDefaults.defaultMarkets
            } else {
                val remoteIds = remoteList.map { it.id }.toSet()
                remoteList + MarketHubDefaults.defaultMarkets.filter { it.id !in remoteIds }
            }
        }
        .catch { emit(MarketHubDefaults.defaultMarkets) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), MarketHubDefaults.defaultMarkets)

    val approvedShopsState: StateFlow<UiState<List<Shop>>> = repository.observeApprovedShops()
        .map<List<Shop>, UiState<List<Shop>>> { list ->
            UiState.Success(list.sortedByDescending { it.isFeatured })
        }
        .catch { error ->
            Log.w(TAG, "Error observing approved shops", error)
            emit(UiState.Error(error.localizedMessage ?: "Failed to load approved shops"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val activeProductsState: StateFlow<UiState<List<Product>>> = repository.observeActiveProducts()
        .map<List<Product>, UiState<List<Product>>> { list ->
            UiState.Success(list)
        }
        .catch { error ->
            Log.w(TAG, "Error observing active products", error)
            emit(UiState.Error(error.localizedMessage ?: "Failed to load products"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val activeOffersState: StateFlow<UiState<List<Offer>>> = repository.observeActiveOffers()
        .map<List<Offer>, UiState<List<Offer>>> { list ->
            UiState.Success(list.sortedByDescending { it.isFeatured })
        }
        .catch { error ->
            Log.w(TAG, "Error observing offers", error)
            emit(UiState.Error(error.localizedMessage ?: "Failed to load daily offers"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val myShops: StateFlow<List<Shop>> = repository.observeMyShops(currentUserId)
        .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val myProducts: StateFlow<List<Product>> = repository.observeMyProducts(currentUserId)
        .map { list -> list.sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) } }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val myOffers: StateFlow<List<Offer>> = repository.observeMyOffers(currentUserId)
        .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val customerInquiries: StateFlow<List<Inquiry>> = repository.observeCustomerInquiries(currentUserId)
        .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val shopkeeperInquiries: StateFlow<List<Inquiry>> = repository.observeShopkeeperInquiries(currentUserId)
        .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Admin streams - strictly gated by _isAdmin
    val allShopsAdmin: StateFlow<List<Shop>> = _isAdmin
        .flatMapLatest { admin ->
            if (admin) repository.observeAllShopsForAdmin() else flowOf(emptyList())
        }
        .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allProductsAdmin: StateFlow<List<Product>> = _isAdmin
        .flatMapLatest { admin ->
            if (admin) repository.observeAllProductsForAdmin() else flowOf(emptyList())
        }
        .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allReportsAdmin: StateFlow<List<Report>> = _isAdmin
        .flatMapLatest { admin ->
            if (admin) repository.observeAllReportsForAdmin() else flowOf(emptyList())
        }
        .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Filtered & Sorted Products for Customer Discovery
    val filteredProducts: StateFlow<List<Product>> = combine(
        activeProductsState,
        _searchQuery,
        _selectedCategoryId,
        _selectedShopIdFilter,
        combine(_minPricePkr, _maxPricePkr, _onlyInStock, _sortOption) { minP, maxP, inStock, sort ->
            FilterParams(minP, maxP, inStock, sort)
        }
    ) { prodState, query, catId, shopId, params ->
        val products = (prodState as? UiState.Success)?.data ?: emptyList()
        val q = query.trim().lowercase()
        val filtered = products.filter { p ->
            val matchesQuery = q.isEmpty() ||
                p.name.lowercase().contains(q) ||
                p.description.lowercase().contains(q) ||
                p.shopName.lowercase().contains(q) ||
                p.categoryName.lowercase().contains(q) ||
                p.marketName.lowercase().contains(q)
            val matchesCat = catId == null || p.categoryId == catId
            val matchesShop = shopId == null || p.shopId == shopId
            val matchesMin = params.minPrice == null || p.pricePkr >= params.minPrice
            val matchesMax = params.maxPrice == null || p.pricePkr <= params.maxPrice
            val matchesStock = !params.onlyInStock || p.isAvailable
            matchesQuery && matchesCat && matchesShop && matchesMin && matchesMax && matchesStock
        }
        when (params.sortOption) {
            ProductSortOption.POPULAR -> filtered.sortedWith(
                compareByDescending<Product> { it.isPopular }
                    .thenByDescending { it.viewsCount + it.inquiriesCount * 2 }
            )
            ProductSortOption.NEWEST -> filtered.sortedByDescending {
                it.createdAt ?: Timestamp(0, 0)
            }
            ProductSortOption.PRICE_LOW_HIGH -> filtered.sortedBy { it.pricePkr }
            ProductSortOption.PRICE_HIGH_LOW -> filtered.sortedByDescending { it.pricePkr }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Price Comparison Groups across shops
    val priceComparisonGroups: StateFlow<List<PriceComparisonGroup>> = combine(
        activeProductsState,
        _searchQuery,
        _selectedCategoryId
    ) { prodState, query, catId ->
        val products = (prodState as? UiState.Success)?.data ?: emptyList()
        val q = query.trim().lowercase()
        val filtered = products.filter { p ->
            val matchesQuery = q.isEmpty() ||
                p.name.lowercase().contains(q) ||
                p.categoryName.lowercase().contains(q) ||
                p.shopName.lowercase().contains(q)
            val matchesCat = catId == null || p.categoryId == catId
            matchesQuery && matchesCat
        }
        filtered
            .groupBy { it.normalizedName.ifBlank { it.name.lowercase().trim() } }
            .map { (key, list) ->
                val sortedByPrice = list.sortedBy { it.pricePkr }
                PriceComparisonGroup(
                    groupKey = key,
                    displayTitle = sortedByPrice.first().name,
                    categoryName = sortedByPrice.first().categoryName,
                    lowestPricePkr = sortedByPrice.first().pricePkr,
                    highestPricePkr = sortedByPrice.last().pricePkr,
                    listings = sortedByPrice
                )
            }
            .sortedWith(
                compareByDescending<PriceComparisonGroup> { it.listings.size }
                    .thenBy { it.lowestPricePkr }
            )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(
                displayName = currentUserName,
                email = currentUserEmail
            )
            val adminVerified = repository.checkIsAdmin(currentUserId)
            _isAdmin.value = adminVerified
            if (adminVerified) {
                repository.seedDefaultCategoriesAndMarketsAsAdmin()
            }
        }
    }

    fun dismissBanner() {
        _banner.value = null
    }

    private fun showBanner(message: String, isError: Boolean = false) {
        _banner.value = BannerNotification(message, isError)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategoryFilter(categoryId: String?) {
        _selectedCategoryId.value = if (_selectedCategoryId.value == categoryId) null else categoryId
    }

    fun selectMarketFilter(marketId: String?) {
        _selectedMarketId.value = if (_selectedMarketId.value == marketId) null else marketId
    }

    fun selectShopFilter(shopId: String?) {
        _selectedShopIdFilter.value = if (_selectedShopIdFilter.value == shopId) null else shopId
    }

    fun setPriceRangeFilter(minPrice: Long?, maxPrice: Long?) {
        _minPricePkr.value = minPrice
        _maxPricePkr.value = maxPrice
    }

    fun toggleOnlyInStock(enabled: Boolean) {
        _onlyInStock.value = enabled
    }

    fun setSortOption(option: ProductSortOption) {
        _sortOption.value = option
    }

    fun clearAllFilters() {
        _searchQuery.value = ""
        _selectedCategoryId.value = null
        _selectedMarketId.value = null
        _selectedShopIdFilter.value = null
        _minPricePkr.value = null
        _maxPricePkr.value = null
        _onlyInStock.value = false
        _sortOption.value = ProductSortOption.POPULAR
    }

    fun recordShopView(shop: Shop) {
        viewModelScope.launch {
            repository.incrementShopViewCount(shop)
        }
    }

    fun recordProductView(product: Product) {
        viewModelScope.launch {
            repository.incrementProductViewCount(product)
        }
    }

    fun updateProfile(displayName: String, phone: String, role: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updateUserProfile(currentUserId, displayName, phone, role)
            _isBusy.value = false
            res.onSuccess {
                showBanner("Profile updated successfully.")
            }.onFailure {
                showBanner(it.localizedMessage ?: "Could not update profile.", isError = true)
            }
        }
    }

    // --- SHOPKEEPER ACTIONS ---

    fun submitShopRegistration(
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
        coverUrl: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val ownerName = userProfile.value?.displayName?.ifBlank { currentUserName } ?: currentUserName
            val res = repository.submitShopRegistration(
                ownerName = ownerName,
                name = name,
                categoryId = categoryId,
                categoryName = categoryName,
                marketId = marketId,
                marketName = marketName,
                address = address,
                mapLocation = mapLocation,
                description = description,
                businessHours = businessHours,
                contactPhone = contactPhone,
                whatsappNumber = whatsappNumber,
                logoUrl = logoUrl,
                coverUrl = coverUrl
            )
            _isBusy.value = false
            res.onSuccess {
                showBanner("Shop submitted! Pending administrator approval.")
                onSuccess()
            }.onFailure {
                showBanner(it.localizedMessage ?: "Failed to submit shop.", isError = true)
            }
        }
    }

    fun updateShopDetails(
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
        coverUrl: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updateShopDetailsByOwner(
                shopId = shopId,
                name = name,
                categoryId = categoryId,
                categoryName = categoryName,
                marketId = marketId,
                marketName = marketName,
                address = address,
                mapLocation = mapLocation,
                description = description,
                businessHours = businessHours,
                contactPhone = contactPhone,
                whatsappNumber = whatsappNumber,
                logoUrl = logoUrl,
                coverUrl = coverUrl
            )
            _isBusy.value = false
            res.onSuccess {
                showBanner("Shop details saved.")
                onSuccess()
            }.onFailure {
                showBanner(it.localizedMessage ?: "Failed to update shop details.", isError = true)
            }
        }
    }

    fun saveProduct(
        existingProductId: String?,
        shop: Shop,
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
        imageUrl: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = if (existingProductId.isNullOrBlank()) {
                repository.createProduct(
                    shop = shop,
                    name = name,
                    categoryId = categoryId,
                    categoryName = categoryName,
                    description = description,
                    pricePkr = pricePkr,
                    originalPricePkr = originalPricePkr,
                    stockQuantity = stockQuantity,
                    isAvailable = isAvailable,
                    isNewArrival = isNewArrival,
                    isPopular = isPopular,
                    imageUrl = imageUrl
                ).map { Unit }
            } else {
                repository.updateProductByOwner(
                    productId = existingProductId,
                    name = name,
                    categoryId = categoryId,
                    categoryName = categoryName,
                    description = description,
                    pricePkr = pricePkr,
                    originalPricePkr = originalPricePkr,
                    stockQuantity = stockQuantity,
                    isAvailable = isAvailable,
                    isNewArrival = isNewArrival,
                    isPopular = isPopular,
                    imageUrl = imageUrl
                )
            }
            _isBusy.value = false
            res.onSuccess {
                showBanner(if (existingProductId.isNullOrBlank()) "Product published to ${shop.name}!" else "Product updated!")
                onSuccess()
            }.onFailure {
                showBanner(it.localizedMessage ?: "Failed to save product.", isError = true)
            }
        }
    }

    fun toggleProductAvailability(product: Product) {
        viewModelScope.launch {
            val res = repository.updateProductByOwner(
                productId = product.id,
                name = product.name,
                categoryId = product.categoryId,
                categoryName = product.categoryName,
                description = product.description,
                pricePkr = product.pricePkr,
                originalPricePkr = product.originalPricePkr,
                stockQuantity = product.stockQuantity,
                isAvailable = !product.isAvailable,
                isNewArrival = product.isNewArrival,
                isPopular = product.isPopular,
                imageUrl = product.imageUrl
            )
            res.onSuccess {
                val statusLabel = if (!product.isAvailable) "Available in Stock" else "Unavailable"
                showBanner("${product.name} marked as $statusLabel.")
            }.onFailure {
                showBanner(it.localizedMessage ?: "Failed to update availability.", isError = true)
            }
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            val res = repository.deleteProduct(productId)
            res.onSuccess {
                showBanner("Product deleted.")
            }.onFailure {
                showBanner(it.localizedMessage ?: "Failed to delete product.", isError = true)
            }
        }
    }

    fun publishOffer(
        shop: Shop,
        title: String,
        description: String,
        discountBadge: String,
        promoPricePkr: Long,
        originalPricePkr: Long,
        productId: String = "",
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.createOffer(
                shop = shop,
                title = title,
                description = description,
                discountBadge = discountBadge,
                promoPricePkr = promoPricePkr,
                originalPricePkr = originalPricePkr,
                productId = productId
            )
            _isBusy.value = false
            res.onSuccess {
                showBanner("Special offer published!")
                onSuccess()
            }.onFailure {
                showBanner(it.localizedMessage ?: "Failed to publish offer.", isError = true)
            }
        }
    }

    fun deleteOffer(offerId: String) {
        viewModelScope.launch {
            repository.deleteOffer(offerId).onSuccess {
                showBanner("Offer removed.")
            }.onFailure {
                showBanner(it.localizedMessage ?: "Failed to remove offer.", isError = true)
            }
        }
    }

    // --- CUSTOMER INQUIRIES & REPORTS ---

    fun recordAndSendInquiry(
        customerPhone: String,
        shopId: String,
        shopName: String,
        shopOwnerId: String,
        productId: String,
        productName: String,
        displayedPricePkr: Long,
        message: String,
        channel: String,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val senderName = userProfile.value?.displayName?.ifBlank { currentUserName } ?: currentUserName
            val res = repository.submitCustomerInquiry(
                customerName = senderName,
                customerPhone = customerPhone,
                shopId = shopId,
                shopName = shopName,
                shopOwnerId = shopOwnerId,
                productId = productId,
                productName = productName,
                displayedPricePkr = displayedPricePkr,
                message = message,
                channel = channel
            )
            res.onSuccess {
                if (channel == "in_app") {
                    showBanner("Inquiry sent to $shopName! Track replies in Account.")
                }
                onComplete()
            }.onFailure {
                showBanner(it.localizedMessage ?: "Failed to log inquiry.", isError = true)
                onComplete()
            }
        }
    }

    fun replyToCustomerInquiry(inquiryId: String, replyText: String, closeInquiry: Boolean) {
        viewModelScope.launch {
            val res = repository.replyToInquiry(inquiryId, replyText, closeInquiry)
            res.onSuccess {
                showBanner(if (closeInquiry) "Inquiry replied and closed." else "Reply sent to customer.")
            }.onFailure {
                showBanner(it.localizedMessage ?: "Failed to send reply.", isError = true)
            }
        }
    }

    fun submitListingReport(
        targetType: String,
        targetId: String,
        targetName: String,
        shopId: String,
        reason: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val reporterName = userProfile.value?.displayName?.ifBlank { currentUserName } ?: currentUserName
            val res = repository.submitReport(
                reporterName = reporterName,
                targetType = targetType,
                targetId = targetId,
                targetName = targetName,
                shopId = shopId,
                reason = reason
            )
            res.onSuccess {
                showBanner("Report submitted to MarketHub administrators.")
                onSuccess()
            }.onFailure {
                showBanner(it.localizedMessage ?: "Failed to submit report.", isError = true)
            }
        }
    }

    // --- ADMINISTRATOR ACTIONS ---

    fun moderateShopAsAdmin(
        shopId: String,
        status: String,
        isVerified: Boolean,
        isFeatured: Boolean
    ) {
        viewModelScope.launch {
            val res = repository.updateShopModerationByAdmin(shopId, status, isVerified, isFeatured)
            res.onSuccess {
                showBanner("Shop updated: status=$status, verified=$isVerified, featured=$isFeatured")
            }.onFailure {
                showBanner(it.localizedMessage ?: "Admin action failed.", isError = true)
            }
        }
    }

    fun moderateProductAsAdmin(productId: String, status: String, isPopular: Boolean) {
        viewModelScope.launch {
            val res = repository.updateProductStatusByAdmin(productId, status, isPopular)
            res.onSuccess {
                showBanner("Product moderation updated ($status).")
            }.onFailure {
                showBanner(it.localizedMessage ?: "Failed to moderate product.", isError = true)
            }
        }
    }

    fun saveCategoryAsAdmin(
        categoryId: String?,
        name: String,
        iconName: String,
        description: String,
        displayOrder: Long
    ) {
        viewModelScope.launch {
            val res = repository.saveCategoryAsAdmin(categoryId, name, iconName, description, displayOrder)
            res.onSuccess {
                showBanner("Category '$name' saved.")
            }.onFailure {
                showBanner(it.localizedMessage ?: "Failed to save category.", isError = true)
            }
        }
    }

    fun deleteCategoryAsAdmin(categoryId: String) {
        viewModelScope.launch {
            val res = repository.deleteCategoryAsAdmin(categoryId)
            res.onSuccess {
                showBanner("Category deleted.")
            }.onFailure {
                showBanner(it.localizedMessage ?: "Failed to delete category.", isError = true)
            }
        }
    }

    fun saveMarketAsAdmin(
        marketId: String?,
        name: String,
        city: String,
        address: String,
        description: String
    ) {
        viewModelScope.launch {
            val res = repository.saveMarketAsAdmin(marketId, name, city, address, description)
            res.onSuccess {
                showBanner("Market '$name' saved.")
            }.onFailure {
                showBanner(it.localizedMessage ?: "Failed to save market.", isError = true)
            }
        }
    }

    fun resolveReportAsAdmin(reportId: String, status: String, adminNotes: String) {
        viewModelScope.launch {
            val res = repository.resolveReportByAdmin(reportId, status, adminNotes)
            res.onSuccess {
                showBanner("Report marked as $status.")
            }.onFailure {
                showBanner(it.localizedMessage ?: "Failed to resolve report.", isError = true)
            }
        }
    }

    private data class FilterParams(
        val minPrice: Long?,
        val maxPrice: Long?,
        val onlyInStock: Boolean,
        val sortOption: ProductSortOption
    )

    private companion object {
        const val TAG = "MarketHubVM"
    }
}
