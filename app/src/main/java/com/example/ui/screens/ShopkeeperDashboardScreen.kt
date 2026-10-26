package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.Category
import com.example.data.model.Inquiry
import com.example.data.model.Market
import com.example.data.model.Offer
import com.example.data.model.Order
import com.example.data.model.PaymentSettings
import com.example.data.model.Product
import com.example.data.model.Shop
import com.example.data.model.formatPkr
import com.example.ui.components.AvailabilityBadge
import com.example.ui.components.PaymentStatusBadge
import com.example.ui.components.VerifiedShopBadge
import com.example.ui.components.formatTimestampShort
import com.example.ui.theme.DangerRed

@Composable
fun ShopkeeperDashboardScreen(
    myShops: List<Shop>,
    myProducts: List<Product>,
    myOffers: List<Offer>,
    shopkeeperInquiries: List<Inquiry>,
    shopkeeperOrders: List<Order> = emptyList(),
    paymentSettings: PaymentSettings = PaymentSettings(),
    categories: List<Category>,
    markets: List<Market>,
    isBusy: Boolean,
    onSubmitShopRegistration: (
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
    ) -> Unit,
    onUpdateShopDetails: (
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
    ) -> Unit,
    onSaveProduct: (
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
        imageUrl: String
    ) -> Unit,
    onToggleProductAvailability: (Product) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onPublishOffer: (
        shop: Shop,
        title: String,
        description: String,
        discountBadge: String,
        promoPricePkr: Long,
        originalPricePkr: Long
    ) -> Unit,
    onDeleteOffer: (String) -> Unit,
    onReplyToInquiry: (inquiryId: String, replyText: String, closeInquiry: Boolean) -> Unit,
    onUpdateOrderStatus: (orderId: String, paymentStatus: String, orderStatus: String, verifierNotes: String) -> Unit = { _, _, _, _ -> }
) {
    var selectedShopIndex by remember { mutableIntStateOf(0) }
    var activeTab by remember { mutableIntStateOf(0) }
    var showNewShopDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var showProductModal by remember { mutableStateOf(false) }
    var showOfferModal by remember { mutableStateOf(false) }
    var replyingInquiry by remember { mutableStateOf<Inquiry?>(null) }

    val activeShop = myShops.getOrNull(selectedShopIndex.coerceIn(0, (myShops.size - 1).coerceAtLeast(0)))

    if (myShops.isEmpty()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("shopkeeper_registration_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Register Your Shop on MarketHub",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "List your commercial market shop, publish your product catalogue with PKR prices, and receive customer inquiries directly on WhatsApp.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                ShopRegistrationOrEditForm(
                    existingShop = null,
                    categories = categories,
                    markets = markets,
                    isBusy = isBusy,
                    onSubmit = { name, catId, catName, mktId, mktName, addr, mapLoc, desc, hours, phone, wa, logo, cover ->
                        onSubmitShopRegistration(
                            name, catId, catName, mktId, mktName, addr, mapLoc, desc, hours, phone, wa, logo, cover
                        )
                    }
                )
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
        return
    }

    val shopProducts = remember(myProducts, activeShop) {
        if (activeShop == null) emptyList()
        else myProducts.filter { it.shopId == activeShop.id }
    }
    val shopOffers = remember(myOffers, activeShop) {
        if (activeShop == null) emptyList()
        else myOffers.filter { it.shopId == activeShop.id }
    }
    val shopInquiries = remember(shopkeeperInquiries, activeShop) {
        if (activeShop == null) emptyList()
        else shopkeeperInquiries.filter { it.shopId == activeShop.id }
    }
    val shopOrders = remember(shopkeeperOrders, activeShop) {
        if (activeShop == null) emptyList()
        else shopkeeperOrders.filter { it.shopId == activeShop.id }
    }
    val totalProductViews = remember(shopProducts) {
        shopProducts.sumOf { it.viewsCount }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("shopkeeper_dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Shop Selector & Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Shopkeeper Dashboard",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Manage your shop, products, stock, offers & inquiries",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = { showNewShopDialog = true },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Shop")
                }
            }

            if (myShops.size > 1) {
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(myShops.size) { index ->
                        val s = myShops[index]
                        FilterChip(
                            selected = selectedShopIndex == index,
                            onClick = { selectedShopIndex = index },
                            label = { Text("${s.name} (${s.status.uppercase()})") }
                        )
                    }
                }
            }
        }

        // 2. Active Shop Status Card & Stats
        if (activeShop != null) {
            item {
                val statusColor = when (activeShop.status) {
                    "approved" -> Color(0xFF15803D)
                    "pending" -> Color(0xFFD97706)
                    else -> DangerRed
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = activeShop.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    VerifiedShopBadge(isVerified = activeShop.isVerified)
                                }
                                Text(
                                    text = "${activeShop.marketName} • ${activeShop.categoryName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                color = statusColor.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = activeShop.status.uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = statusColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        if (activeShop.status == "pending") {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Pending Administrator Approval: Your shop registration is awaiting review. Once approved by a MarketHub administrator, you can publish products and promotional offers to customers.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF92400E),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 4 Statistics Cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatMiniCard(
                                label = "Shop Views",
                                value = activeShop.viewsCount.toString(),
                                modifier = Modifier.weight(1f)
                            )
                            StatMiniCard(
                                label = "Product Views",
                                value = totalProductViews.toString(),
                                modifier = Modifier.weight(1f)
                            )
                            StatMiniCard(
                                label = "Products",
                                value = shopProducts.size.toString(),
                                modifier = Modifier.weight(1f)
                            )
                            StatMiniCard(
                                label = "Inquiries",
                                value = shopInquiries.size.toString(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 3. Dashboard Sub-Tabs
            item {
                val tabs = listOf(
                    "Products (${shopProducts.size})",
                    "Orders (${shopOrders.size})",
                    "Offers (${shopOffers.size})",
                    "Inquiries (${shopInquiries.size})",
                    "Shop Profile"
                )
                ScrollableTabRow(
                    selectedTabIndex = activeTab,
                    edgePadding = 0.dp
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = activeTab == index,
                            onClick = { activeTab = index },
                            text = { Text(title) }
                        )
                    }
                }
            }

            when (activeTab) {
                0 -> {
                    // PRODUCTS TAB
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "My Product Catalogue",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = {
                                    editingProduct = null
                                    showProductModal = true
                                },
                                enabled = activeShop.status == "approved",
                                modifier = Modifier.testTag("add_product_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Product")
                            }
                        }
                    }

                    if (shopProducts.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = if (activeShop.status == "approved") {
                                        "No products added yet. Tap 'Add Product' to list your items with PKR prices and stock status."
                                    } else {
                                        "Product uploads will unlock automatically as soon as an administrator approves '${activeShop.name}'."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                        }
                    } else {
                        items(shopProducts, key = { it.id }) { product ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = product.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${product.categoryName} • ${formatPkr(product.pricePkr)}",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "Views: ${product.viewsCount} • Inquiries: ${product.inquiriesCount}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        AvailabilityBadge(
                                            isAvailable = product.isAvailable,
                                            stockQuantity = product.stockQuantity
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "In Stock",
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Switch(
                                                checked = product.isAvailable,
                                                onCheckedChange = { onToggleProductAvailability(product) }
                                            )
                                        }

                                        Row {
                                            IconButton(
                                                onClick = {
                                                    editingProduct = product
                                                    showProductModal = true
                                                }
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit Product")
                                            }
                                            IconButton(
                                                onClick = { onDeleteProduct(product.id) }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Product",
                                                    tint = DangerRed
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // ORDERS TAB
                    item {
                        Text(
                            text = "WhatsApp Direct Purchase Orders (${shopOrders.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (paymentSettings.allowShopkeeperPaymentVerification) {
                                "Manage incoming customer orders and verify submitted payment proofs."
                            } else {
                                "Manage incoming customer orders. Manual payment proof verification is handled by MarketHub Administrators."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (shopOrders.isEmpty()) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                Text(
                                    text = "No direct purchase orders received for ${activeShop.name} yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                        }
                    } else {
                        items(shopOrders, key = { it.id }) { order ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${order.orderNumber} • ${order.productName}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Qty: ${order.quantity} × ${formatPkr(order.unitPricePkr)} = ${formatPkr(order.subtotalPkr)}",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        PaymentStatusBadge(paymentStatus = order.paymentStatus)
                                    }

                                    Text(
                                        text = "Customer: ${order.customerName.ifBlank { "Customer" }} | Phone: ${order.customerPhone.ifBlank { "N/A" }}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "Delivery: ${order.deliveryType.uppercase()} — ${order.deliveryAddress.ifBlank { "Local Shop Pickup" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Payment Method: ${com.example.data.model.PaymentMethodOption.fromKey(order.paymentMethod).displayLabel} • Order Status: ${order.orderStatus.uppercase()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )

                                    if (order.transactionReference.isNotBlank() || order.proofScreenshotUri.isNotBlank()) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text(
                                                    text = "Submitted Payment Proof",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (order.transactionReference.isNotBlank()) {
                                                    Text(
                                                        text = "TID / Reference: ${order.transactionReference}",
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                                if (order.proofScreenshotUri.isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    AsyncImage(
                                                        model = order.proofScreenshotUri,
                                                        contentDescription = "Payment Proof Screenshot",
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(140.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    HorizontalDivider()

                                    Text(
                                        text = "Update Order Fulfillment Status:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                onUpdateOrderStatus(order.id, order.paymentStatus, "confirmed", order.verifierNotes)
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Confirm Order", style = MaterialTheme.typography.labelSmall)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                onUpdateOrderStatus(order.id, order.paymentStatus, "completed", order.verifierNotes)
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Complete", style = MaterialTheme.typography.labelSmall)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                onUpdateOrderStatus(order.id, order.paymentStatus, "cancelled", order.verifierNotes)
                                            },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Cancel", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }

                                    if (paymentSettings.allowShopkeeperPaymentVerification &&
                                        order.paymentMethod != com.example.data.model.PaymentMethodOption.CASH_ON_DELIVERY.key
                                    ) {
                                        Text(
                                            text = "Authorized Shopkeeper Payment Verification:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    onUpdateOrderStatus(
                                                        order.id,
                                                        "Payment Verified",
                                                        if (order.orderStatus == "pending") "confirmed" else order.orderStatus,
                                                        "Payment verified by shopkeeper"
                                                    )
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Verify Payment", style = MaterialTheme.typography.labelSmall)
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    onUpdateOrderStatus(
                                                        order.id,
                                                        "Payment Rejected",
                                                        order.orderStatus,
                                                        "Payment proof rejected by shopkeeper"
                                                    )
                                                },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Reject Proof", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // OFFERS TAB
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Promotional Discounts & Offers",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = { showOfferModal = true },
                                enabled = activeShop.status == "approved"
                            ) {
                                Icon(Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Publish Offer")
                            }
                        }
                    }

                    if (shopOffers.isEmpty()) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                Text(
                                    text = "No promotional offers published yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                        }
                    } else {
                        items(shopOffers, key = { it.id }) { offer ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${offer.discountBadge} — ${offer.title}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Offer Price: ${formatPkr(offer.promoPricePkr)} (Was ${formatPkr(offer.originalPricePkr)})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = offer.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { onDeleteOffer(offer.id) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Offer",
                                            tint = DangerRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // INQUIRIES TAB
                    item {
                        Text(
                            text = "Customer Inquiries (${shopInquiries.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (shopInquiries.isEmpty()) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                Text(
                                    text = "No customer inquiries received for ${activeShop.name} yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                        }
                    } else {
                        items(shopInquiries, key = { it.id }) { inq ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${inq.customerName} (${inq.channel.uppercase()})",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = inq.status.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (inq.customerPhone.isNotBlank()) {
                                        Text(
                                            text = "Customer Phone: ${inq.customerPhone}",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    Text(
                                        text = "Item: ${inq.productName} (${formatPkr(inq.displayedPricePkr)}) • ${formatTimestampShort(inq.createdAt)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = inq.message,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (inq.shopkeeperReply.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Your Reply: ${inq.shopkeeperReply}",
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                    }
                                    if (inq.status != "closed") {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = { replyingInquiry = inq }
                                        ) {
                                            Icon(Icons.Default.Reply, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Reply / Close Inquiry")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                4 -> {
                    // SHOP PROFILE EDIT TAB
                    item {
                        ShopRegistrationOrEditForm(
                            existingShop = activeShop,
                            categories = categories,
                            markets = markets,
                            isBusy = isBusy,
                            onSubmit = { name, catId, catName, mktId, mktName, addr, mapLoc, desc, hours, phone, wa, logo, cover ->
                                onUpdateShopDetails(
                                    activeShop.id, name, catId, catName, mktId, mktName, addr, mapLoc, desc, hours, phone, wa, logo, cover
                                )
                            }
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }

    if (showNewShopDialog) {
        AlertDialog(
            onDismissRequest = { showNewShopDialog = false },
            title = { Text("Submit Another Shop") },
            text = {
                LazyColumn(modifier = Modifier.height(420.dp)) {
                    item {
                        ShopRegistrationOrEditForm(
                            existingShop = null,
                            categories = categories,
                            markets = markets,
                            isBusy = isBusy,
                            onSubmit = { name, catId, catName, mktId, mktName, addr, mapLoc, desc, hours, phone, wa, logo, cover ->
                                onSubmitShopRegistration(
                                    name, catId, catName, mktId, mktName, addr, mapLoc, desc, hours, phone, wa, logo, cover
                                )
                                showNewShopDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showNewShopDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showProductModal && activeShop != null) {
        ProductEditorModal(
            existingProduct = editingProduct,
            shop = activeShop,
            categories = categories,
            onDismiss = { showProductModal = false },
            onSave = { name, catId, catName, desc, price, origPrice, stock, avail, newArr, pop, imgUrl ->
                onSaveProduct(
                    editingProduct?.id,
                    activeShop,
                    name,
                    catId,
                    catName,
                    desc,
                    price,
                    origPrice,
                    stock,
                    avail,
                    newArr,
                    pop,
                    imgUrl
                )
                showProductModal = false
            }
        )
    }

    if (showOfferModal && activeShop != null) {
        OfferEditorModal(
            shop = activeShop,
            onDismiss = { showOfferModal = false },
            onPublish = { title, desc, badge, promoP, origP ->
                onPublishOffer(activeShop, title, desc, badge, promoP, origP)
                showOfferModal = false
            }
        )
    }

    if (replyingInquiry != null) {
        val target = replyingInquiry!!
        var replyText by remember(target.id) { mutableStateOf(target.shopkeeperReply) }
        AlertDialog(
            onDismissRequest = { replyingInquiry = null },
            title = { Text("Reply to ${target.customerName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Inquiry: ${target.message}", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        label = { Text("Your Reply") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            if (replyText.isNotBlank()) {
                                onReplyToInquiry(target.id, replyText, false)
                                replyingInquiry = null
                            }
                        }
                    ) {
                        Text("Send Reply")
                    }
                    Button(
                        onClick = {
                            onReplyToInquiry(target.id, replyText.ifBlank { "Resolved via WhatsApp" }, true)
                            replyingInquiry = null
                        }
                    ) {
                        Text("Reply & Close")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { replyingInquiry = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatMiniCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ShopRegistrationOrEditForm(
    existingShop: Shop?,
    categories: List<Category>,
    markets: List<Market>,
    isBusy: Boolean,
    onSubmit: (
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
    ) -> Unit
) {
    var name by remember(existingShop?.id) { mutableStateOf(existingShop?.name ?: "") }
    var selectedCatId by remember(existingShop?.id) {
        mutableStateOf(existingShop?.categoryId?.takeIf { it.isNotBlank() } ?: categories.firstOrNull()?.id ?: "cat_mobile")
    }
    var selectedMarketId by remember(existingShop?.id) {
        mutableStateOf(existingShop?.marketId?.takeIf { it.isNotBlank() } ?: markets.firstOrNull()?.id ?: "mkt_hafeez_lhr")
    }
    var address by remember(existingShop?.id) { mutableStateOf(existingShop?.address ?: "") }
    var mapLocation by remember(existingShop?.id) { mutableStateOf(existingShop?.mapLocation ?: "") }
    var description by remember(existingShop?.id) { mutableStateOf(existingShop?.description ?: "") }
    var businessHours by remember(existingShop?.id) {
        mutableStateOf(existingShop?.businessHours ?: "10:00 AM - 10:00 PM (Mon-Sat)")
    }
    var contactPhone by remember(existingShop?.id) { mutableStateOf(existingShop?.contactPhone ?: "+92 300 ") }
    var whatsappNumber by remember(existingShop?.id) { mutableStateOf(existingShop?.whatsappNumber ?: "92300") }
    var logoUrl by remember(existingShop?.id) { mutableStateOf(existingShop?.logoUrl ?: "") }
    var coverUrl by remember(existingShop?.id) { mutableStateOf(existingShop?.coverUrl ?: "") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) logoUrl = uri.toString().take(800)
    }

    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) coverUrl = uri.toString().take(800)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (existingShop == null) "Shop Registration Details" else "Edit Shop Profile",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Shop Name *") },
                placeholder = { Text("e.g. Al-Madina Electronics & Mobile Center") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("shop_name_input")
            )

            Text("Select Shop Category *", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories, key = { it.id }) { cat ->
                    FilterChip(
                        selected = selectedCatId == cat.id,
                        onClick = { selectedCatId = cat.id },
                        label = { Text(cat.name) }
                    )
                }
            }

            Text("Select Commercial Market *", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(markets, key = { it.id }) { mkt ->
                    FilterChip(
                        selected = selectedMarketId == mkt.id,
                        onClick = { selectedMarketId = mkt.id },
                        label = { Text("${mkt.name} (${mkt.city})") }
                    )
                }
            }

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Full Shop Address (Plaza, Floor, Shop #) *") },
                placeholder = { Text("Shop #14, Ground Floor, Main Plaza") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("shop_address_input")
            )

            OutlinedTextField(
                value = mapLocation,
                onValueChange = { mapLocation = it },
                label = { Text("Map Location / Coordinates (Optional)") },
                placeholder = { Text("31.5162, 74.3435 or Landmark") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Shop Description & Specialties *") },
                placeholder = { Text("Describe the brands, warranty, and products your shop offers...") },
                minLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("shop_description_input")
            )

            OutlinedTextField(
                value = businessHours,
                onValueChange = { businessHours = it },
                label = { Text("Business Hours *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = contactPhone,
                    onValueChange = { contactPhone = it },
                    label = { Text("Contact Phone *") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("shop_phone_input")
                )
                OutlinedTextField(
                    value = whatsappNumber,
                    onValueChange = { whatsappNumber = it },
                    label = { Text("WhatsApp Number *") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("shop_whatsapp_input")
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = logoUrl,
                    onValueChange = { logoUrl = it },
                    label = { Text("Logo Photo URL / URI (Optional)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = {
                        logoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pick")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = coverUrl,
                    onValueChange = { coverUrl = it },
                    label = { Text("Cover Banner URL / URI (Optional)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = {
                        coverPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pick")
                }
            }

            if (validationError != null) {
                Text(
                    text = validationError ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = DangerRed
                )
            }

            Button(
                onClick = {
                    val catName = categories.find { it.id == selectedCatId }?.name ?: "Other shops"
                    val mkt = markets.find { it.id == selectedMarketId }
                    val mktName = if (mkt != null) "${mkt.name}, ${mkt.city}" else "Commercial Market"

                    when {
                        name.trim().length < 2 -> validationError = "Please enter a valid shop name (at least 2 characters)."
                        address.trim().length < 3 -> validationError = "Please enter your shop address."
                        description.trim().length < 3 -> validationError = "Please enter a brief shop description."
                        contactPhone.trim().length < 5 -> validationError = "Please enter a valid contact phone number."
                        whatsappNumber.trim().length < 5 -> validationError = "Please enter a valid WhatsApp number."
                        else -> {
                            validationError = null
                            onSubmit(
                                name.trim(),
                                selectedCatId,
                                catName,
                                selectedMarketId,
                                mktName,
                                address.trim(),
                                mapLocation.trim(),
                                description.trim(),
                                businessHours.trim().ifBlank { "10:00 AM - 10:00 PM" },
                                contactPhone.trim(),
                                whatsappNumber.trim(),
                                logoUrl.trim(),
                                coverUrl.trim()
                            )
                        }
                    }
                },
                enabled = !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_shop_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (existingShop == null) "Submit Shop for Admin Approval" else "Save Shop Profile",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ProductEditorModal(
    existingProduct: Product?,
    shop: Shop,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (
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
    ) -> Unit
) {
    var name by remember { mutableStateOf(existingProduct?.name ?: "") }
    var catId by remember {
        mutableStateOf(existingProduct?.categoryId?.takeIf { it.isNotBlank() } ?: shop.categoryId)
    }
    var description by remember { mutableStateOf(existingProduct?.description ?: "") }
    var priceText by remember { mutableStateOf(existingProduct?.pricePkr?.toString() ?: "") }
    var origPriceText by remember {
        mutableStateOf(existingProduct?.originalPricePkr?.takeIf { it > 0 }?.toString() ?: "")
    }
    var stockText by remember { mutableStateOf((existingProduct?.stockQuantity ?: 5L).toString()) }
    var isAvailable by remember { mutableStateOf(existingProduct?.isAvailable ?: true) }
    var isNewArrival by remember { mutableStateOf(existingProduct?.isNewArrival ?: true) }
    var isPopular by remember { mutableStateOf(existingProduct?.isPopular ?: false) }
    var imageUrl by remember { mutableStateOf(existingProduct?.imageUrl ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) imageUrl = uri.toString().take(800)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingProduct == null) "Add Product to ${shop.name}" else "Edit Product",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.height(420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Product Name *") },
                        placeholder = { Text("e.g. Samsung Galaxy A55 5G 256GB") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_name_input")
                    )
                }
                item {
                    Text("Category", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories, key = { it.id }) { cat ->
                            FilterChip(
                                selected = catId == cat.id,
                                onClick = { catId = cat.id },
                                label = { Text(cat.name) }
                            )
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { priceText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Price (PKR Rs.) *") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("product_price_input")
                        )
                        OutlinedTextField(
                            value = origPriceText,
                            onValueChange = { origPriceText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Old Price (Optional)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = stockText,
                        onValueChange = { stockText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Available Stock Quantity *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description & Specifications *") },
                        minLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_description_input")
                    )
                }
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = imageUrl,
                            onValueChange = { imageUrl = it },
                            label = { Text("Product Photo URL / URI") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = {
                                imagePicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        ) {
                            Text("Pick")
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Available in Stock")
                        Switch(checked = isAvailable, onCheckedChange = { isAvailable = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mark as New Arrival")
                        Switch(checked = isNewArrival, onCheckedChange = { isNewArrival = it })
                    }
                }
                if (errorMsg != null) {
                    item {
                        Text(errorMsg ?: "", color = DangerRed, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceText.toLongOrNull() ?: 0L
                    val origPrice = origPriceText.toLongOrNull() ?: 0L
                    val stock = stockText.toLongOrNull() ?: 0L
                    val catName = categories.find { it.id == catId }?.name ?: shop.categoryName
                    when {
                        name.trim().length < 2 -> errorMsg = "Enter a valid product name."
                        price <= 0L -> errorMsg = "Enter a valid price in PKR (> 0)."
                        description.trim().length < 2 -> errorMsg = "Enter a product description."
                        else -> {
                            onSave(
                                name.trim(),
                                catId,
                                catName,
                                description.trim(),
                                price,
                                origPrice,
                                stock,
                                isAvailable,
                                isNewArrival,
                                isPopular,
                                imageUrl.trim()
                            )
                        }
                    }
                },
                modifier = Modifier.testTag("save_product_confirm_button")
            ) {
                Text("Save Product")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun OfferEditorModal(
    shop: Shop,
    onDismiss: () -> Unit,
    onPublish: (
        title: String,
        description: String,
        discountBadge: String,
        promoPricePkr: Long,
        originalPricePkr: Long
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var discountBadge by remember { mutableStateOf("20% OFF") }
    var promoPriceText by remember { mutableStateOf("") }
    var originalPriceText by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Publish Special Offer for ${shop.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Offer Headline *") },
                    placeholder = { Text("Weekend Mega Sale on Unstitched Lawn") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = discountBadge,
                    onValueChange = { discountBadge = it },
                    label = { Text("Discount Badge *") },
                    placeholder = { Text("e.g. 25% OFF or Rs. 2,000 OFF") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = promoPriceText,
                        onValueChange = { promoPriceText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Offer Price (Rs.) *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = originalPriceText,
                        onValueChange = { originalPriceText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Regular Price (Rs.) *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Offer Details *") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMsg != null) {
                    Text(errorMsg ?: "", color = DangerRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val promo = promoPriceText.toLongOrNull() ?: 0L
                    val orig = originalPriceText.toLongOrNull() ?: 0L
                    when {
                        title.trim().length < 3 -> errorMsg = "Enter a valid offer title."
                        description.trim().length < 3 -> errorMsg = "Enter offer details."
                        promo <= 0L || orig <= 0L -> errorMsg = "Enter valid PKR prices."
                        else -> onPublish(
                            title.trim(),
                            description.trim(),
                            discountBadge.trim().ifBlank { "SPECIAL OFFER" },
                            promo,
                            orig
                        )
                    }
                }
            ) {
                Text("Publish Offer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
