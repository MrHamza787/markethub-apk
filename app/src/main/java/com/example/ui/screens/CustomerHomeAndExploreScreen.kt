package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Category
import com.example.data.model.Market
import com.example.data.model.Offer
import com.example.data.model.Product
import com.example.data.model.Shop
import com.example.data.model.formatPkr
import com.example.ui.components.CategoryCardItem
import com.example.ui.components.OfferCard
import com.example.ui.components.ProductCard
import com.example.ui.components.ShopCard
import com.example.ui.viewmodel.ProductSortOption
import com.example.ui.viewmodel.UiState

@Composable
fun CustomerHomeAndExploreScreen(
    categories: List<Category>,
    markets: List<Market>,
    approvedShopsState: UiState<List<Shop>>,
    activeOffersState: UiState<List<Offer>>,
    filteredProducts: List<Product>,
    activeProductsState: UiState<List<Product>>,
    searchQuery: String,
    selectedCategoryId: String?,
    selectedMarketId: String?,
    selectedShopIdFilter: String?,
    minPricePkr: Long?,
    maxPricePkr: Long?,
    onlyInStock: Boolean,
    sortOption: ProductSortOption,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onSelectMarket: (String?) -> Unit,
    onSelectShopFilter: (String?) -> Unit,
    onPriceRangeChange: (Long?, Long?) -> Unit,
    onToggleOnlyInStock: (Boolean) -> Unit,
    onSelectSortOption: (ProductSortOption) -> Unit,
    onClearFilters: () -> Unit,
    onOpenProductDetail: (Product) -> Unit,
    onOpenShopDetail: (String) -> Unit,
    onCompareProduct: (Product) -> Unit,
    onWhatsAppProduct: (Product) -> Unit,
    onWhatsAppShop: (Shop) -> Unit,
    onWhatsAppOffer: (Offer) -> Unit,
    onNavigateToCompareTab: () -> Unit,
    onNavigateToShopkeeperTab: () -> Unit,
    onBuyNowProduct: (Product) -> Unit = {}
) {
    var showFilterModal by remember { mutableStateOf(false) }
    val approvedShops = (approvedShopsState as? UiState.Success)?.data ?: emptyList()
    val activeOffers = (activeOffersState as? UiState.Success)?.data ?: emptyList()

    val shopsInSelectedMarket = remember(approvedShops, selectedMarketId) {
        if (selectedMarketId == null) approvedShops
        else approvedShops.filter { it.marketId == selectedMarketId }
    }

    val displayedProducts = remember(filteredProducts, selectedMarketId, shopsInSelectedMarket) {
        if (selectedMarketId == null) {
            filteredProducts
        } else {
            val allowedShopIds = shopsInSelectedMarket.map { it.id }.toSet()
            val selectedMarketName = markets.find { it.id == selectedMarketId }?.name
            filteredProducts.filter { p ->
                p.shopId in allowedShopIds || (selectedMarketName != null && p.marketName.equals(selectedMarketName, ignoreCase = true))
            }
        }
    }

    val newArrivals = remember(displayedProducts) {
        displayedProducts.filter { it.isNewArrival }.take(6)
    }

    val featuredShops = remember(shopsInSelectedMarket) {
        shopsInSelectedMarket.sortedByDescending { it.isFeatured || it.isVerified }.take(6)
    }

    val hasActiveFilters = searchQuery.isNotBlank() ||
        selectedCategoryId != null ||
        selectedMarketId != null ||
        selectedShopIdFilter != null ||
        minPricePkr != null ||
        maxPricePkr != null ||
        onlyInStock

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("customer_home_list"),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Search Bar & Filter Trigger
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Search products, shops, or categories...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search"
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("search_input")
                    )

                    Surface(
                        color = if (hasActiveFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { showFilterModal = true }
                            .testTag("open_filters_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Filter & Sort",
                                tint = if (hasActiveFilters) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Commercial Market Selector Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedMarketId == null,
                            onClick = { onSelectMarket(null) },
                            label = { Text("All Commercial Markets") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                    items(markets, key = { it.id }) { market ->
                        FilterChip(
                            selected = selectedMarketId == market.id,
                            onClick = { onSelectMarket(market.id) },
                            label = { Text("${market.name} (${market.city})") }
                        )
                    }
                }

                if (hasActiveFilters) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Showing ${displayedProducts.size} matching products • Sort: ${sortOption.label}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(onClick = onClearFilters) {
                            Text("Reset All Filters")
                        }
                    }
                }
            }
        }

        // 2. Hero Banner (Shown when not actively searching)
        if (searchQuery.isBlank()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(22.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_market),
                            contentDescription = "MarketHub Hero",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(185.dp),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(185.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xE604221B),
                                            Color(0x9904221B),
                                            Color(0x3304221B)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(18.dp)
                        ) {
                            Surface(
                                color = Color(0xFFF59E0B),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "COMPARE PRICES ACROSS SHOPS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF04221B),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Discover Local Shops &\nBest Prices in PKR",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Check real stock status & message shopkeepers directly on WhatsApp.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE2E8F0)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = onNavigateToCompareTab,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFF59E0B),
                                        contentColor = Color(0xFF04221B)
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CompareArrows,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text("Compare Prices", style = MaterialTheme.typography.labelLarge)
                                }
                                OutlinedButton(
                                    onClick = onNavigateToShopkeeperTab,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Storefront,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text("Open a Shop", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Shop Categories Section
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(
                    title = "Shop Categories",
                    subtitle = "Explore 10 specialized market departments",
                    actionLabel = if (selectedCategoryId != null) "Clear Category" else null,
                    onActionClick = { onSelectCategory(null) }
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(categories, key = { it.id }) { category ->
                        CategoryCardItem(
                            category = category,
                            isSelected = selectedCategoryId == category.id,
                            onClick = { onSelectCategory(category.id) }
                        )
                    }
                }
            }
        }

        // 4. Daily Offers & Discounts Section
        if (activeOffers.isNotEmpty() && searchQuery.isBlank()) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(
                        title = "Daily Offers & Discounts",
                        subtitle = "Limited-time deals published by verified shopkeepers"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(activeOffers, key = { it.id }) { offer ->
                            OfferCard(
                                offer = offer,
                                onShopClick = onOpenShopDetail,
                                onWhatsAppClick = onWhatsAppOffer
                            )
                        }
                    }
                }
            }
        }

        // 5. Featured & Verified Shops Section
        if (searchQuery.isBlank()) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(
                        title = "Featured Market Shops",
                        subtitle = if (featuredShops.isEmpty()) "Approved shops will appear here" else "${featuredShops.size} independent shops ready to serve"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    when (approvedShopsState) {
                        is UiState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        is UiState.Error -> {
                            EmptyMarketplaceCard(
                                title = "Could not load shops",
                                subtitle = approvedShopsState.message,
                                actionText = "Register Your Shop",
                                onAction = onNavigateToShopkeeperTab
                            )
                        }
                        is UiState.Success -> {
                            if (featuredShops.isEmpty()) {
                                EmptyMarketplaceCard(
                                    title = "No approved shops in this market yet",
                                    subtitle = "Are you a shop owner? Submit your shop registration in the 'My Shop' tab and an administrator can approve it.",
                                    actionText = "Register a Shop Now",
                                    onAction = onNavigateToShopkeeperTab
                                )
                            } else {
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    featuredShops.take(3).forEach { shop ->
                                        ShopCard(
                                            shop = shop,
                                            onClick = { onOpenShopDetail(shop.id) },
                                            onWhatsAppClick = { onWhatsAppShop(shop) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. New Arrivals Highlight Row
        if (newArrivals.isNotEmpty() && searchQuery.isBlank() && selectedCategoryId == null) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(
                        title = "New Arrivals",
                        subtitle = "Freshly added stock across commercial markets"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(newArrivals, key = { "new_${it.id}" }) { product ->
                            ProductCard(
                                product = product,
                                onProductClick = onOpenProductDetail,
                                onShopClick = onOpenShopDetail,
                                onCompareClick = onCompareProduct,
                                onWhatsAppClick = onWhatsAppProduct,
                                onBuyNowClick = onBuyNowProduct,
                                modifier = Modifier.width(265.dp)
                            )
                        }
                    }
                }
            }
        }

        // 7. Popular & Filtered Products Catalogue
        item {
            SectionHeader(
                title = if (hasActiveFilters) "Filtered Products (${displayedProducts.size})" else "Popular Products Across Markets",
                subtitle = "Compare PKR prices, check live stock status & inquire directly",
                actionLabel = "Filter & Sort",
                onActionClick = { showFilterModal = true }
            )
        }

        when (activeProductsState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            is UiState.Error -> {
                item {
                    EmptyMarketplaceCard(
                        title = "Unable to load products",
                        subtitle = activeProductsState.message,
                        actionText = "Reset Filters",
                        onAction = onClearFilters
                    )
                }
            }
            is UiState.Success -> {
                if (displayedProducts.isEmpty()) {
                    item {
                        EmptyMarketplaceCard(
                            title = if (hasActiveFilters) "No products match your filters" else "No active products published yet",
                            subtitle = if (hasActiveFilters) {
                                "Try clearing your price range, shop, or category filter to see more listings."
                            } else {
                                "Approved shopkeepers can add products with PKR prices and stock status in the 'My Shop' dashboard."
                            },
                            actionText = if (hasActiveFilters) "Clear Filters" else "Go to My Shop Dashboard",
                            onAction = if (hasActiveFilters) onClearFilters else onNavigateToShopkeeperTab
                        )
                    }
                } else {
                    items(displayedProducts, key = { it.id }) { product ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            ProductCard(
                                product = product,
                                onProductClick = onOpenProductDetail,
                                onShopClick = onOpenShopDetail,
                                onCompareClick = onCompareProduct,
                                onWhatsAppClick = onWhatsAppProduct,
                                onBuyNowClick = onBuyNowProduct
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFilterModal) {
        ProductFilterAndSortModal(
            categories = categories,
            approvedShops = approvedShops,
            selectedCategoryId = selectedCategoryId,
            selectedShopId = selectedShopIdFilter,
            minPricePkr = minPricePkr,
            maxPricePkr = maxPricePkr,
            onlyInStock = onlyInStock,
            sortOption = sortOption,
            onApply = { catId, shopId, minP, maxP, inStock, sort ->
                if (catId != selectedCategoryId) onSelectCategory(catId)
                if (shopId != selectedShopIdFilter) onSelectShopFilter(shopId)
                onPriceRangeChange(minP, maxP)
                onToggleOnlyInStock(inStock)
                onSelectSortOption(sort)
                showFilterModal = false
            },
            onReset = {
                onClearFilters()
                showFilterModal = false
            },
            onDismiss = { showFilterModal = false }
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (actionLabel != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(actionLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun EmptyMarketplaceCard(
    title: String,
    subtitle: String,
    actionText: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Storefront,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(actionText)
            }
        }
    }
}

@Composable
private fun ProductFilterAndSortModal(
    categories: List<Category>,
    approvedShops: List<Shop>,
    selectedCategoryId: String?,
    selectedShopId: String?,
    minPricePkr: Long?,
    maxPricePkr: Long?,
    onlyInStock: Boolean,
    sortOption: ProductSortOption,
    onApply: (
        categoryId: String?,
        shopId: String?,
        minPrice: Long?,
        maxPrice: Long?,
        onlyInStock: Boolean,
        sort: ProductSortOption
    ) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    var catId by remember { mutableStateOf(selectedCategoryId) }
    var shopId by remember { mutableStateOf(selectedShopId) }
    var minPriceText by remember { mutableStateOf(minPricePkr?.toString() ?: "") }
    var maxPriceText by remember { mutableStateOf(maxPricePkr?.toString() ?: "") }
    var inStockOnly by remember { mutableStateOf(onlyInStock) }
    var currentSort by remember { mutableStateOf(sortOption) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Filter & Sort Products",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Sort By", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ProductSortOption.entries) { option ->
                        FilterChip(
                            selected = currentSort == option,
                            onClick = { currentSort = option },
                            label = { Text(option.label) }
                        )
                    }
                }

                Text("Price Range (PKR Rs.)", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minPriceText,
                        onValueChange = { minPriceText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Min Rs.") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxPriceText,
                        onValueChange = { maxPriceText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Max Rs.") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Filter by Category", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = catId == null,
                            onClick = { catId = null },
                            label = { Text("All") }
                        )
                    }
                    items(categories, key = { it.id }) { cat ->
                        FilterChip(
                            selected = catId == cat.id,
                            onClick = { catId = cat.id },
                            label = { Text(cat.name) }
                        )
                    }
                }

                if (approvedShops.isNotEmpty()) {
                    Text("Filter by Shop", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = shopId == null,
                                onClick = { shopId = null },
                                label = { Text("All Shops") }
                            )
                        }
                        items(approvedShops, key = { it.id }) { shop ->
                            FilterChip(
                                selected = shopId == shop.id,
                                onClick = { shopId = shop.id },
                                label = { Text(shop.name) }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Only Available in Stock",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = inStockOnly,
                        onCheckedChange = { inStockOnly = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApply(
                        catId,
                        shopId,
                        minPriceText.toLongOrNull(),
                        maxPriceText.toLongOrNull(),
                        inStockOnly,
                        currentSort
                    )
                }
            ) {
                Text("Apply Filters")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onReset) {
                    Text("Reset")
                }
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}
