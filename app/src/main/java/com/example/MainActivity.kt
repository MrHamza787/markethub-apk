package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.Offer
import com.example.data.model.Product
import com.example.data.model.Shop
import com.example.data.repository.MarketHubRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOut
import com.example.ui.components.CustomerInquiryModal
import com.example.ui.components.ReportListingDialog
import com.example.ui.components.buildWhatsAppPreFilledMessage
import com.example.ui.components.launchWhatsAppIntent
import com.example.ui.screens.AccountAndInquiriesScreen
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.CustomerHomeAndExploreScreen
import com.example.ui.screens.PriceComparisonScreen
import com.example.ui.screens.ProductDetailScreen
import com.example.ui.screens.PublicShopPageScreen
import com.example.ui.screens.ShopkeeperDashboardScreen
import com.example.ui.screens.ShopsDirectoryScreen
import com.example.ui.theme.MarketHubTheme
import com.example.ui.viewmodel.MarketHubViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MarketHubTheme {
                AppNavigation()
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(
            onAuthSuccess = {}
        )
    } else {
        val viewModel: MarketHubViewModel = viewModel(
            key = user.uid,
            factory = viewModelFactory {
                initializer {
                    val app = checkNotNull(this[APPLICATION_KEY]) {
                        "APPLICATION_KEY missing from CreationExtras"
                    }
                    val databaseId = app.getString(R.string.firestore_database_id)
                    val db = FirebaseFirestore.getInstance(databaseId)
                    MarketHubViewModel(
                        repository = MarketHubRepository(db),
                        currentUserId = user.uid,
                        currentUserName = user.displayName ?: "MarketHub User",
                        currentUserEmail = user.email ?: ""
                    )
                }
            }
        )
        MarketHubAuthenticatedApp(viewModel = viewModel)
    }
}

enum class MarketHubSection(val label: String, val icon: ImageVector, val testTag: String) {
    HOME("Explore", Icons.Default.Home, "nav_explore"),
    SHOPS("Shops", Icons.Default.Store, "nav_shops"),
    COMPARE("Compare", Icons.Default.CompareArrows, "nav_compare"),
    SHOPKEEPER("My Shop", Icons.Default.Storefront, "nav_my_shop"),
    ADMIN("Admin", Icons.Default.AdminPanelSettings, "nav_admin"),
    ACCOUNT("Account", Icons.Default.Person, "nav_account")
}

private sealed interface SubDestination {
    data class ShopPage(val shopId: String) : SubDestination
    data class ProductPage(val product: Product) : SubDestination
}

private data class InquiryTarget(
    val shopId: String,
    val shopName: String,
    val shopOwnerId: String,
    val whatsappNumber: String,
    val productId: String = "",
    val productName: String = "General Shop Inquiry",
    val displayedPricePkr: Long = 0L
)

private data class ReportTarget(
    val targetType: String,
    val targetId: String,
    val targetName: String,
    val shopId: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketHubAuthenticatedApp(viewModel: MarketHubViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val banner by viewModel.banner.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val markets by viewModel.markets.collectAsStateWithLifecycle()
    val approvedShopsState by viewModel.approvedShopsState.collectAsStateWithLifecycle()
    val activeProductsState by viewModel.activeProductsState.collectAsStateWithLifecycle()
    val activeOffersState by viewModel.activeOffersState.collectAsStateWithLifecycle()
    val filteredProducts by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val comparisonGroups by viewModel.priceComparisonGroups.collectAsStateWithLifecycle()

    val myShops by viewModel.myShops.collectAsStateWithLifecycle()
    val myProducts by viewModel.myProducts.collectAsStateWithLifecycle()
    val myOffers by viewModel.myOffers.collectAsStateWithLifecycle()
    val customerInquiries by viewModel.customerInquiries.collectAsStateWithLifecycle()
    val shopkeeperInquiries by viewModel.shopkeeperInquiries.collectAsStateWithLifecycle()

    val allShopsAdmin by viewModel.allShopsAdmin.collectAsStateWithLifecycle()
    val allProductsAdmin by viewModel.allProductsAdmin.collectAsStateWithLifecycle()
    val allReportsAdmin by viewModel.allReportsAdmin.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val selectedMarketId by viewModel.selectedMarketId.collectAsStateWithLifecycle()
    val selectedShopIdFilter by viewModel.selectedShopIdFilter.collectAsStateWithLifecycle()
    val minPricePkr by viewModel.minPricePkr.collectAsStateWithLifecycle()
    val maxPricePkr by viewModel.maxPricePkr.collectAsStateWithLifecycle()
    val onlyInStock by viewModel.onlyInStock.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()

    var currentSection by remember { mutableStateOf(MarketHubSection.HOME) }
    var subDestination by remember { mutableStateOf<SubDestination?>(null) }
    var activeInquiryTarget by remember { mutableStateOf<InquiryTarget?>(null) }
    var activeReportTarget by remember { mutableStateOf<ReportTarget?>(null) }

    val visibleSections = remember(isAdmin) {
        if (isAdmin) MarketHubSection.entries
        else MarketHubSection.entries.filter { it != MarketHubSection.ADMIN }
    }

    val approvedShops = (approvedShopsState as? UiState.Success)?.data ?: emptyList()
    val activeProducts = (activeProductsState as? UiState.Success)?.data ?: emptyList()
    val activeOffers = (activeOffersState as? UiState.Success)?.data ?: emptyList()

    fun openWhatsAppForProduct(product: Product) {
        activeInquiryTarget = InquiryTarget(
            shopId = product.shopId,
            shopName = product.shopName,
            shopOwnerId = product.ownerId,
            whatsappNumber = product.whatsappNumber,
            productId = product.id,
            productName = product.name,
            displayedPricePkr = product.pricePkr
        )
    }

    fun openWhatsAppForShop(shop: Shop) {
        activeInquiryTarget = InquiryTarget(
            shopId = shop.id,
            shopName = shop.name,
            shopOwnerId = shop.ownerId,
            whatsappNumber = shop.whatsappNumber
        )
    }

    fun openWhatsAppForOffer(offer: Offer) {
        activeInquiryTarget = InquiryTarget(
            shopId = offer.shopId,
            shopName = offer.shopName,
            shopOwnerId = offer.ownerId,
            whatsappNumber = offer.whatsappNumber,
            productId = offer.productId,
            productName = "${offer.title} (${offer.discountBadge})",
            displayedPricePkr = offer.promoPricePkr
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 720.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = "MarketHub Logo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "MarketHub",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Commercial Market & Price Comparison",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                if (!isExpandedScreen) {
                    NavigationBar {
                        visibleSections.forEach { section ->
                            NavigationBarItem(
                                selected = currentSection == section && subDestination == null,
                                onClick = {
                                    subDestination = null
                                    currentSection = section
                                },
                                icon = {
                                    Icon(
                                        imageVector = section.icon,
                                        contentDescription = section.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = section.label,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.testTag(section.testTag)
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
                if (isExpandedScreen) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        visibleSections.forEach { section ->
                            NavigationRailItem(
                                selected = currentSection == section && subDestination == null,
                                onClick = {
                                    subDestination = null
                                    currentSection = section
                                },
                                icon = {
                                    Icon(
                                        imageVector = section.icon,
                                        contentDescription = section.label
                                    )
                                },
                                label = { Text(section.label) },
                                modifier = Modifier.testTag(section.testTag)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    // Notification Banner
                    AnimatedVisibility(visible = banner != null) {
                        banner?.let { b ->
                            Surface(
                                color = if (b.isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = b.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = if (b.isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { viewModel.dismissBanner() },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        val activeSub = subDestination
                        if (activeSub != null) {
                            when (activeSub) {
                                is SubDestination.ShopPage -> {
                                    val targetShop = approvedShops.find { it.id == activeSub.shopId }
                                        ?: myShops.find { it.id == activeSub.shopId }
                                        ?: allShopsAdmin.find { it.id == activeSub.shopId }
                                    if (targetShop != null) {
                                        val shopProducts = activeProducts.filter { it.shopId == targetShop.id }
                                        val shopOffers = activeOffers.filter { it.shopId == targetShop.id }
                                        PublicShopPageScreen(
                                            shop = targetShop,
                                            shopProducts = shopProducts,
                                            shopOffers = shopOffers,
                                            onBack = { subDestination = null },
                                            onProductClick = { prod ->
                                                viewModel.recordProductView(prod)
                                                subDestination = SubDestination.ProductPage(prod)
                                            },
                                            onCompareProduct = { prod ->
                                                subDestination = null
                                                viewModel.updateSearchQuery(prod.name)
                                                currentSection = MarketHubSection.COMPARE
                                            },
                                            onWhatsAppShop = { openWhatsAppForShop(it) },
                                            onWhatsAppProduct = { openWhatsAppForProduct(it) },
                                            onWhatsAppOffer = { openWhatsAppForOffer(it) },
                                            onReportShop = { s ->
                                                activeReportTarget = ReportTarget(
                                                    targetType = "shop",
                                                    targetId = s.id,
                                                    targetName = s.name,
                                                    shopId = s.id
                                                )
                                            }
                                        )
                                    } else {
                                        subDestination = null
                                    }
                                }

                                is SubDestination.ProductPage -> {
                                    val product = activeProducts.find { it.id == activeSub.product.id }
                                        ?: activeSub.product
                                    val otherListings = activeProducts.filter {
                                        it.id != product.id &&
                                            (it.normalizedName.equals(product.normalizedName, ignoreCase = true) ||
                                                it.name.equals(product.name, ignoreCase = true))
                                    }.sortedBy { it.pricePkr }

                                    ProductDetailScreen(
                                        product = product,
                                        otherShopListings = otherListings,
                                        onBack = { subDestination = null },
                                        onOpenShop = { shopId ->
                                            val shopObj = approvedShops.find { it.id == shopId }
                                            if (shopObj != null) viewModel.recordShopView(shopObj)
                                            subDestination = SubDestination.ShopPage(shopId)
                                        },
                                        onOpenProduct = { other ->
                                            viewModel.recordProductView(other)
                                            subDestination = SubDestination.ProductPage(other)
                                        },
                                        onWhatsAppInquiry = { openWhatsAppForProduct(it) },
                                        onInAppInquiry = { openWhatsAppForProduct(it) },
                                        onReportProduct = { p ->
                                            activeReportTarget = ReportTarget(
                                                targetType = "product",
                                                targetId = p.id,
                                                targetName = p.name,
                                                shopId = p.shopId
                                            )
                                        }
                                    )
                                }
                            }
                        } else {
                            when (currentSection) {
                                MarketHubSection.HOME -> {
                                    CustomerHomeAndExploreScreen(
                                        categories = categories,
                                        markets = markets,
                                        approvedShopsState = approvedShopsState,
                                        activeOffersState = activeOffersState,
                                        filteredProducts = filteredProducts,
                                        activeProductsState = activeProductsState,
                                        searchQuery = searchQuery,
                                        selectedCategoryId = selectedCategoryId,
                                        selectedMarketId = selectedMarketId,
                                        selectedShopIdFilter = selectedShopIdFilter,
                                        minPricePkr = minPricePkr,
                                        maxPricePkr = maxPricePkr,
                                        onlyInStock = onlyInStock,
                                        sortOption = sortOption,
                                        onSearchQueryChange = viewModel::updateSearchQuery,
                                        onSelectCategory = viewModel::selectCategoryFilter,
                                        onSelectMarket = viewModel::selectMarketFilter,
                                        onSelectShopFilter = viewModel::selectShopFilter,
                                        onPriceRangeChange = viewModel::setPriceRangeFilter,
                                        onToggleOnlyInStock = viewModel::toggleOnlyInStock,
                                        onSelectSortOption = viewModel::setSortOption,
                                        onClearFilters = viewModel::clearAllFilters,
                                        onOpenProductDetail = { prod ->
                                            viewModel.recordProductView(prod)
                                            subDestination = SubDestination.ProductPage(prod)
                                        },
                                        onOpenShopDetail = { shopId ->
                                            val shopObj = approvedShops.find { it.id == shopId }
                                            if (shopObj != null) viewModel.recordShopView(shopObj)
                                            subDestination = SubDestination.ShopPage(shopId)
                                        },
                                        onCompareProduct = { prod ->
                                            viewModel.updateSearchQuery(prod.name)
                                            currentSection = MarketHubSection.COMPARE
                                        },
                                        onWhatsAppProduct = { openWhatsAppForProduct(it) },
                                        onWhatsAppShop = { openWhatsAppForShop(it) },
                                        onWhatsAppOffer = { openWhatsAppForOffer(it) },
                                        onNavigateToCompareTab = { currentSection = MarketHubSection.COMPARE },
                                        onNavigateToShopkeeperTab = { currentSection = MarketHubSection.SHOPKEEPER }
                                    )
                                }

                                MarketHubSection.SHOPS -> {
                                    ShopsDirectoryScreen(
                                        shops = approvedShops,
                                        markets = markets,
                                        categories = categories,
                                        onShopClick = { shop ->
                                            viewModel.recordShopView(shop)
                                            subDestination = SubDestination.ShopPage(shop.id)
                                        },
                                        onWhatsAppShop = { openWhatsAppForShop(it) },
                                        onRegisterShopClick = { currentSection = MarketHubSection.SHOPKEEPER }
                                    )
                                }

                                MarketHubSection.COMPARE -> {
                                    PriceComparisonScreen(
                                        comparisonGroups = comparisonGroups,
                                        categories = categories,
                                        searchQuery = searchQuery,
                                        selectedCategoryId = selectedCategoryId,
                                        onSearchQueryChange = viewModel::updateSearchQuery,
                                        onSelectCategory = viewModel::selectCategoryFilter,
                                        onOpenProductDetail = { prod ->
                                            viewModel.recordProductView(prod)
                                            subDestination = SubDestination.ProductPage(prod)
                                        },
                                        onOpenShopDetail = { shopId ->
                                            subDestination = SubDestination.ShopPage(shopId)
                                        },
                                        onWhatsAppProduct = { openWhatsAppForProduct(it) }
                                    )
                                }

                                MarketHubSection.SHOPKEEPER -> {
                                    ShopkeeperDashboardScreen(
                                        myShops = myShops,
                                        myProducts = myProducts,
                                        myOffers = myOffers,
                                        shopkeeperInquiries = shopkeeperInquiries,
                                        categories = categories,
                                        markets = markets,
                                        isBusy = isBusy,
                                        onSubmitShopRegistration = { name, catId, catName, mktId, mktName, addr, mapLoc, desc, hours, phone, wa, logo, cover ->
                                            viewModel.submitShopRegistration(
                                                name, catId, catName, mktId, mktName, addr, mapLoc, desc, hours, phone, wa, logo, cover
                                            )
                                        },
                                        onUpdateShopDetails = { shopId, name, catId, catName, mktId, mktName, addr, mapLoc, desc, hours, phone, wa, logo, cover ->
                                            viewModel.updateShopDetails(
                                                shopId, name, catId, catName, mktId, mktName, addr, mapLoc, desc, hours, phone, wa, logo, cover
                                            )
                                        },
                                        onSaveProduct = { existingId, shop, name, catId, catName, desc, price, origPrice, stock, avail, newArr, pop, imgUrl ->
                                            viewModel.saveProduct(
                                                existingId, shop, name, catId, catName, desc, price, origPrice, stock, avail, newArr, pop, imgUrl
                                            )
                                        },
                                        onToggleProductAvailability = viewModel::toggleProductAvailability,
                                        onDeleteProduct = viewModel::deleteProduct,
                                        onPublishOffer = { shop, title, desc, badge, promoP, origP ->
                                            viewModel.publishOffer(shop, title, desc, badge, promoP, origP)
                                        },
                                        onDeleteOffer = viewModel::deleteOffer,
                                        onReplyToInquiry = viewModel::replyToCustomerInquiry
                                    )
                                }

                                MarketHubSection.ADMIN -> {
                                    AdminDashboardScreen(
                                        isAdmin = isAdmin,
                                        allShops = allShopsAdmin,
                                        allProducts = allProductsAdmin,
                                        allOffers = activeOffers,
                                        allReports = allReportsAdmin,
                                        categories = categories,
                                        markets = markets,
                                        onModerateShop = viewModel::moderateShopAsAdmin,
                                        onModerateProduct = viewModel::moderateProductAsAdmin,
                                        onDeleteProduct = viewModel::deleteProduct,
                                        onSaveCategory = viewModel::saveCategoryAsAdmin,
                                        onDeleteCategory = viewModel::deleteCategoryAsAdmin,
                                        onSaveMarket = viewModel::saveMarketAsAdmin,
                                        onResolveReport = viewModel::resolveReportAsAdmin
                                    )
                                }

                                MarketHubSection.ACCOUNT -> {
                                    AccountAndInquiriesScreen(
                                        userProfile = userProfile,
                                        fallbackName = viewModel.currentUserName,
                                        fallbackEmail = viewModel.currentUserEmail,
                                        isAdmin = isAdmin,
                                        customerInquiries = customerInquiries,
                                        onUpdateProfile = viewModel::updateProfile,
                                        onSignOut = {
                                            signOut(
                                                context = context,
                                                credentialManager = credentialManager,
                                                onSignOutComplete = {},
                                                scope = scope
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Customer Inquiry + WhatsApp Launch Modal
    activeInquiryTarget?.let { target ->
        CustomerInquiryModal(
            shopId = target.shopId,
            shopName = target.shopName,
            shopOwnerId = target.shopOwnerId,
            whatsappNumber = target.whatsappNumber,
            productId = target.productId,
            productName = target.productName,
            displayedPricePkr = target.displayedPricePkr,
            onDismiss = { activeInquiryTarget = null },
            onSubmitInquiry = { customerPhone, message, channel ->
                val targetCopy = target
                activeInquiryTarget = null
                viewModel.recordAndSendInquiry(
                    customerPhone = customerPhone,
                    shopId = targetCopy.shopId,
                    shopName = targetCopy.shopName,
                    shopOwnerId = targetCopy.shopOwnerId,
                    productId = targetCopy.productId,
                    productName = targetCopy.productName,
                    displayedPricePkr = targetCopy.displayedPricePkr,
                    message = message,
                    channel = channel
                )
                if (channel == "whatsapp") {
                    val fullWaText = buildWhatsAppPreFilledMessage(
                        shopName = targetCopy.shopName,
                        productName = targetCopy.productName.takeIf { targetCopy.displayedPricePkr > 0L },
                        pricePkr = targetCopy.displayedPricePkr.takeIf { it > 0L },
                        shopId = targetCopy.shopId,
                        productId = targetCopy.productId.takeIf { it.isNotBlank() }
                    ) + "\n\nCustomer Note: $message"
                    launchWhatsAppIntent(
                        context = context,
                        rawWhatsAppNumber = targetCopy.whatsappNumber,
                        message = fullWaText
                    )
                }
            }
        )
    }

    // Report Listing Modal
    activeReportTarget?.let { rep ->
        ReportListingDialog(
            targetType = rep.targetType,
            targetId = rep.targetId,
            targetName = rep.targetName,
            shopId = rep.shopId,
            onDismiss = { activeReportTarget = null },
            onSubmit = { reason ->
                val copy = rep
                activeReportTarget = null
                viewModel.submitListingReport(
                    targetType = copy.targetType,
                    targetId = copy.targetId,
                    targetName = copy.targetName,
                    shopId = copy.shopId,
                    reason = reason
                )
            }
        )
    }
}
