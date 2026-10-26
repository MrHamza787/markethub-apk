package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.Category
import com.example.data.model.Market
import com.example.data.model.Offer
import com.example.data.model.Product
import com.example.data.model.Shop
import com.example.ui.components.OfferCard
import com.example.ui.components.ProductCard
import com.example.ui.components.ShopCard
import com.example.ui.components.VerifiedShopBadge
import com.example.ui.components.launchMapIntent
import com.example.ui.components.launchPhoneDialer
import com.example.ui.theme.WhatsAppGreen

@Composable
fun ShopsDirectoryScreen(
    shops: List<Shop>,
    markets: List<Market>,
    categories: List<Category>,
    onShopClick: (Shop) -> Unit,
    onWhatsAppShop: (Shop) -> Unit,
    onRegisterShopClick: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedMarketId by remember { mutableStateOf<String?>(null) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }

    val filteredShops = remember(shops, query, selectedMarketId, selectedCategoryId) {
        val q = query.trim().lowercase()
        shops.filter { s ->
            val matchesQuery = q.isEmpty() ||
                s.name.lowercase().contains(q) ||
                s.marketName.lowercase().contains(q) ||
                s.categoryName.lowercase().contains(q) ||
                s.address.lowercase().contains(q)
            val matchesMarket = selectedMarketId == null || s.marketId == selectedMarketId
            val matchesCat = selectedCategoryId == null || s.categoryId == selectedCategoryId
            matchesQuery && matchesMarket && matchesCat
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("shops_directory_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Commercial Markets & Shops",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Explore approved independent shops across commercial markets",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search shop name, market, or lane...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedMarketId == null,
                        onClick = { selectedMarketId = null },
                        label = { Text("All Markets") }
                    )
                }
                items(markets, key = { it.id }) { mkt ->
                    FilterChip(
                        selected = selectedMarketId == mkt.id,
                        onClick = {
                            selectedMarketId = if (selectedMarketId == mkt.id) null else mkt.id
                        },
                        label = { Text("${mkt.name} (${mkt.city})") }
                    )
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedCategoryId == null,
                        onClick = { selectedCategoryId = null },
                        label = { Text("All Categories") }
                    )
                }
                items(categories, key = { it.id }) { cat ->
                    FilterChip(
                        selected = selectedCategoryId == cat.id,
                        onClick = {
                            selectedCategoryId = if (selectedCategoryId == cat.id) null else cat.id
                        },
                        label = { Text(cat.name) }
                    )
                }
            }
        }

        if (filteredShops.isEmpty()) {
            item {
                EmptyMarketplaceCard(
                    title = "No matching shops found",
                    subtitle = "Own a shop in a commercial market? Submit your shop registration to list your catalogue on MarketHub.",
                    actionText = "Register Your Shop",
                    onAction = onRegisterShopClick
                )
            }
        } else {
            items(filteredShops, key = { it.id }) { shop ->
                ShopCard(
                    shop = shop,
                    onClick = { onShopClick(shop) },
                    onWhatsAppClick = { onWhatsAppShop(shop) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun PublicShopPageScreen(
    shop: Shop,
    shopProducts: List<Product>,
    shopOffers: List<Offer>,
    onBack: () -> Unit,
    onProductClick: (Product) -> Unit,
    onCompareProduct: (Product) -> Unit,
    onWhatsAppShop: (Shop) -> Unit,
    onWhatsAppProduct: (Product) -> Unit,
    onWhatsAppOffer: (Offer) -> Unit,
    onReportShop: (Shop) -> Unit,
    onBuyNowProduct: (Product) -> Unit = {}
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var searchInShop by remember { mutableStateOf("") }

    val displayedProducts = remember(shopProducts, searchInShop) {
        val q = searchInShop.trim().lowercase()
        if (q.isEmpty()) shopProducts
        else shopProducts.filter {
            it.name.lowercase().contains(q) || it.description.lowercase().contains(q)
        }
    }

    val newArrivals = remember(shopProducts) {
        shopProducts.filter { it.isNewArrival }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("public_shop_page"),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Shop Cover Photo + Logo + Header
        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF052E24),
                                    Color(0xFF0A5C46),
                                    Color(0xFF0F766E)
                                )
                            )
                        )
                ) {
                    if (shop.coverUrl.isNotBlank()) {
                        AsyncImage(
                            model = shop.coverUrl,
                            contentDescription = "${shop.name} Cover",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Top Bar Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.45f),
                        shape = CircleShape
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    }

                    Surface(
                        color = Color.Black.copy(alpha = 0.45f),
                        shape = CircleShape
                    ) {
                        IconButton(onClick = { onReportShop(shop) }) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = "Report Shop",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Overlapping Shop Profile Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 135.dp, start = 16.dp, end = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                if (shop.logoUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = shop.logoUrl,
                                        contentDescription = shop.name,
                                        modifier = Modifier.size(64.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Storefront,
                                        contentDescription = shop.name,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = shop.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    VerifiedShopBadge(isVerified = shop.isVerified)
                                }
                                Text(
                                    text = shop.categoryName,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Status: ${shop.status.uppercase()} • ${shop.viewsCount} views",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = shop.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Market, Address & Business Hours
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Address",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = shop.marketName,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = shop.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = "Business Hours",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Hours: ${shop.businessHours}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Contact Action Buttons: Call, Map, WhatsApp
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { launchPhoneDialer(context, shop.contactPhone) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Call",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Call Shop")
                            }

                            if (shop.mapLocation.isNotBlank() || shop.address.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        launchMapIntent(
                                            context,
                                            shop.mapLocation.ifBlank { "${shop.address}, ${shop.marketName}" }
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Map,
                                        contentDescription = "Map",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text("View Map")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { onWhatsAppShop(shop) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("shop_whatsapp_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WhatsAppGreen,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "WhatsApp",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Contact ${shop.name} on WhatsApp (${shop.whatsappNumber})",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }

        // 2. Shop Special Offers
        if (shopOffers.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "Current Shop Offers (${shopOffers.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(shopOffers, key = { it.id }) { offer ->
                            OfferCard(
                                offer = offer,
                                onShopClick = {},
                                onWhatsAppClick = onWhatsAppOffer
                            )
                        }
                    }
                }
            }
        }

        // 3. New Arrivals in this Shop
        if (newArrivals.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "New Arrivals at ${shop.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(newArrivals, key = { "shop_new_${it.id}" }) { product ->
                            ProductCard(
                                product = product,
                                onProductClick = onProductClick,
                                onShopClick = {},
                                onCompareClick = onCompareProduct,
                                onWhatsAppClick = onWhatsAppProduct,
                                onBuyNowClick = onBuyNowProduct,
                                modifier = Modifier.width(260.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Full Shop Product Catalogue
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Product Catalogue (${displayedProducts.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchInShop,
                    onValueChange = { searchInShop = it },
                    placeholder = { Text("Search inside ${shop.name}...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (displayedProducts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "No products listed in this shop's catalogue yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(displayedProducts, key = { it.id }) { product ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ProductCard(
                        product = product,
                        onProductClick = onProductClick,
                        onShopClick = {},
                        onCompareClick = onCompareProduct,
                        onWhatsAppClick = onWhatsAppProduct,
                        onBuyNowClick = onBuyNowProduct
                    )
                }
            }
        }
    }
}
