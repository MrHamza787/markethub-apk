package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
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
import com.example.data.model.Category
import com.example.data.model.Market
import com.example.data.model.Offer
import com.example.data.model.Product
import com.example.data.model.Report
import com.example.data.model.Shop
import com.example.data.model.formatPkr
import com.example.ui.components.VerifiedShopBadge
import com.example.ui.theme.DangerRed

@Composable
fun AdminDashboardScreen(
    isAdmin: Boolean,
    allShops: List<Shop>,
    allProducts: List<Product>,
    allOffers: List<Offer>,
    allReports: List<Report>,
    categories: List<Category>,
    markets: List<Market>,
    onModerateShop: (shopId: String, status: String, isVerified: Boolean, isFeatured: Boolean) -> Unit,
    onModerateProduct: (productId: String, status: String, isPopular: Boolean) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onSaveCategory: (categoryId: String?, name: String, iconName: String, description: String, displayOrder: Long) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onSaveMarket: (marketId: String?, name: String, city: String, address: String, description: String) -> Unit,
    onResolveReport: (reportId: String, status: String, adminNotes: String) -> Unit
) {
    if (!isAdmin) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Restricted",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Administrator Access Required",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "This dashboard is protected by backend Firestore security rules and is restricted to authorized MarketHub administrators.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        return
    }

    var activeTab by remember { mutableIntStateOf(0) }
    var showAddCategoryModal by remember { mutableStateOf(false) }
    var showAddMarketModal by remember { mutableStateOf(false) }

    val pendingShops = remember(allShops) { allShops.filter { it.status == "pending" } }
    val approvedShopsCount = remember(allShops) { allShops.count { it.status == "approved" } }
    val openReports = remember(allReports) { allReports.filter { it.status == "open" } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Administrator Control Center",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Approve shops, moderate listings, manage categories & resolve reports",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Platform Statistics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdminStatCard(
                    title = "Pending Shops",
                    value = pendingShops.size.toString(),
                    highlight = pendingShops.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                )
                AdminStatCard(
                    title = "Approved Shops",
                    value = approvedShopsCount.toString(),
                    highlight = false,
                    modifier = Modifier.weight(1f)
                )
                AdminStatCard(
                    title = "Products",
                    value = allProducts.size.toString(),
                    highlight = false,
                    modifier = Modifier.weight(1f)
                )
                AdminStatCard(
                    title = "Open Reports",
                    value = openReports.size.toString(),
                    highlight = openReports.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            val tabs = listOf(
                "Shops (${allShops.size})",
                "Products (${allProducts.size})",
                "Categories (${categories.size})",
                "Markets (${markets.size})",
                "Reports (${allReports.size})"
            )
            ScrollableTabRow(selectedTabIndex = activeTab, edgePadding = 0.dp) {
                tabs.forEachIndexed { index, label ->
                    Tab(
                        selected = activeTab == index,
                        onClick = { activeTab = index },
                        text = { Text(label) }
                    )
                }
            }
        }

        when (activeTab) {
            0 -> {
                // SHOPS APPROVAL & MANAGEMENT
                if (allShops.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                            Text(
                                text = "No shop applications submitted yet.",
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                } else {
                    items(allShops, key = { it.id }) { shop ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
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
                                                text = shop.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            VerifiedShopBadge(isVerified = shop.isVerified)
                                        }
                                        Text(
                                            text = "Owner: ${shop.ownerName} • ${shop.categoryName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "${shop.marketName} — ${shop.address}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Phone: ${shop.contactPhone} | WhatsApp: ${shop.whatsappNumber}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = shop.status.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (shop.status != "approved") {
                                        Button(
                                            onClick = {
                                                onModerateShop(shop.id, "approved", true, shop.isFeatured)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("approve_shop_${shop.id}")
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Approve & Verify")
                                        }
                                    }

                                    if (shop.status == "pending") {
                                        OutlinedButton(
                                            onClick = {
                                                onModerateShop(shop.id, "rejected", false, false)
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Reject")
                                        }
                                    }

                                    if (shop.status == "approved") {
                                        OutlinedButton(
                                            onClick = {
                                                onModerateShop(shop.id, "approved", !shop.isVerified, shop.isFeatured)
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (shop.isVerified) "Unverify" else "Verify")
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                onModerateShop(shop.id, "approved", shop.isVerified, !shop.isFeatured)
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (shop.isFeatured) "Unfeature" else "Feature")
                                        }
                                    }

                                    if (shop.status != "suspended") {
                                        OutlinedButton(
                                            onClick = {
                                                onModerateShop(shop.id, "suspended", false, false)
                                            },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
                                        ) {
                                            Icon(Icons.Default.Block, contentDescription = "Suspend", modifier = Modifier.size(15.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // PRODUCTS MODERATION
                items(allProducts, key = { it.id }) { prod ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = prod.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${formatPkr(prod.pricePkr)} • ${prod.shopName} (${prod.marketName})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = prod.status.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        onModerateProduct(prod.id, prod.status, !prod.isPopular)
                                    }
                                ) {
                                    Text(if (prod.isPopular) "Remove Popular" else "Mark Popular")
                                }
                                OutlinedButton(
                                    onClick = {
                                        val nextStatus = if (prod.status == "active") "suspended" else "active"
                                        onModerateProduct(prod.id, nextStatus, prod.isPopular)
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = if (prod.status == "active") DangerRed else MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text(if (prod.status == "active") "Suspend Listing" else "Restore Listing")
                                }
                                IconButton(onClick = { onDeleteProduct(prod.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DangerRed)
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // CATEGORIES MANAGEMENT
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Shop Categories (${categories.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Button(onClick = { showAddCategoryModal = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Category")
                        }
                    }
                }
                items(categories, key = { it.id }) { cat ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
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
                                    text = cat.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = cat.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onDeleteCategory(cat.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove Category", tint = DangerRed)
                            }
                        }
                    }
                }
            }

            3 -> {
                // COMMERCIAL MARKETS MANAGEMENT
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Commercial Markets (${markets.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Button(onClick = { showAddMarketModal = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Market")
                        }
                    }
                }
                items(markets, key = { it.id }) { mkt ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "${mkt.name} (${mkt.city})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = mkt.address,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = mkt.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            4 -> {
                // REPORTED LISTINGS
                if (allReports.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                            Text(
                                text = "No reported listings at this time.",
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                } else {
                    items(allReports, key = { it.id }) { report ->
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
                                        text = "${report.targetType.uppercase()}: ${report.targetName}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = report.status.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (report.status == "open") DangerRed else MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Reported by ${report.reporterName}: ${report.reason}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (report.status == "open") {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                onResolveReport(report.id, "resolved", "Reviewed and actioned by Admin.")
                                            }
                                        ) {
                                            Text("Mark Resolved")
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                onResolveReport(report.id, "dismissed", "Dismissed after verification.")
                                            }
                                        ) {
                                            Text("Dismiss")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }

    if (showAddCategoryModal) {
        var name by remember { mutableStateOf("") }
        var iconName by remember { mutableStateOf("store") }
        var description by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCategoryModal = false },
            title = { Text("Add Shop Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Category Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = iconName,
                        onValueChange = { iconName = it },
                        label = { Text("Icon Key (checkroom, smartphone, laptop, watch, store)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Category Description *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.trim().length >= 2) {
                            onSaveCategory(
                                null,
                                name.trim(),
                                iconName.trim().ifBlank { "store" },
                                description.trim().ifBlank { name.trim() },
                                (categories.size + 1).toLong()
                            )
                            showAddCategoryModal = false
                        }
                    }
                ) {
                    Text("Save Category")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddMarketModal) {
        var name by remember { mutableStateOf("") }
        var city by remember { mutableStateOf("Lahore") }
        var address by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddMarketModal = false },
            title = { Text("Add Commercial Market") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Market Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address / Area *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.trim().length >= 2 && city.trim().length >= 2) {
                            onSaveMarket(
                                null,
                                name.trim(),
                                city.trim(),
                                address.trim().ifBlank { city.trim() },
                                description.trim().ifBlank { "Commercial market in ${city.trim()}" }
                            )
                            showAddMarketModal = false
                        }
                    }
                ) {
                    Text("Save Market")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMarketModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AdminStatCard(
    title: String,
    value: String,
    highlight: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = if (highlight) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (highlight) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
