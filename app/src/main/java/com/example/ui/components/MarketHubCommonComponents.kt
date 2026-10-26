package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.Category
import com.example.data.model.Offer
import com.example.data.model.Product
import com.example.data.model.Shop
import com.example.data.model.formatPkr
import com.example.ui.theme.DangerRed
import com.example.ui.theme.VerifiedBlue
import com.example.ui.theme.WhatsAppGreen
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

fun categoryIconFor(iconName: String): ImageVector {
    return when (iconName.lowercase()) {
        "checkroom", "clothing" -> Icons.Default.Checkroom
        "hiking", "shoes", "footwear" -> Icons.Default.Hiking
        "smartphone", "mobile" -> Icons.Default.Smartphone
        "laptop", "computers", "electronics" -> Icons.Default.Laptop
        "face", "cosmetics", "beauty" -> Icons.Default.Face
        "shopping_basket", "grocery" -> Icons.Default.ShoppingBasket
        "watch", "jewellery" -> Icons.Default.Watch
        "weekend", "home", "furniture" -> Icons.Default.Weekend
        "card_giftcard", "gifts", "toys" -> Icons.Default.CardGiftcard
        else -> Icons.Default.Storefront
    }
}

fun formatTimestampShort(timestamp: Timestamp?): String {
    if (timestamp == null) return "Just now"
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.US)
    return sdf.format(timestamp.toDate())
}

fun buildWhatsAppPreFilledMessage(
    shopName: String,
    productName: String?,
    pricePkr: Long?,
    shopId: String,
    productId: String?
): String {
    return if (!productName.isNullOrBlank() && pricePkr != null && pricePkr > 0) {
        val link = "https://markethub.pk/shops/$shopId/products/${productId ?: "item"}"
        "Assalam-o-Alaikum $shopName! I found your listing on MarketHub:\n" +
            "• Product: $productName\n" +
            "• Displayed Price: ${formatPkr(pricePkr)}\n" +
            "• Listing Link: $link\n\n" +
            "Please confirm current availability and details."
    } else {
        val link = "https://markethub.pk/shops/$shopId"
        "Assalam-o-Alaikum $shopName! I visited your shop page on MarketHub ($link) and would like to inquire about your products."
    }
}

fun launchWhatsAppIntent(
    context: Context,
    rawWhatsAppNumber: String,
    message: String
) {
    val cleanDigits = rawWhatsAppNumber.filter { it.isDigit() }.let { digits ->
        when {
            digits.startsWith("03") && digits.length == 11 -> "92" + digits.substring(1)
            digits.isEmpty() -> "923000000000"
            else -> digits
        }
    }
    val encodedMessage = Uri.encode(message)
    val uri = Uri.parse("https://wa.me/$cleanDigits?text=$encodedMessage")
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}

fun launchPhoneDialer(context: Context, phoneNumber: String) {
    val clean = phoneNumber.filter { it.isDigit() || it == '+' }
    if (clean.isBlank()) return
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean")).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}

fun launchMapIntent(context: Context, addressOrCoords: String) {
    if (addressOrCoords.isBlank()) return
    val uri = Uri.parse("geo:0,0?q=${Uri.encode(addressOrCoords)}")
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}

@Composable
fun AvailabilityBadge(isAvailable: Boolean, stockQuantity: Long? = null) {
    val bgColor = if (isAvailable) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
    val contentColor = if (isAvailable) Color(0xFF15803D) else DangerRed
    val label = if (isAvailable) {
        if (stockQuantity != null && stockQuantity > 0) "In Stock ($stockQuantity)" else "Available in Stock"
    } else {
        "Unavailable"
    }
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isAvailable) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun VerifiedShopBadge(isVerified: Boolean) {
    if (!isVerified) return
    Surface(
        color = Color(0xFFE0F2FE),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = "Verified Shop",
                tint = VerifiedBlue,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Verified",
                style = MaterialTheme.typography.labelSmall,
                color = VerifiedBlue,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CategoryCardItem(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier
            .width(136.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("category_card_${category.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) Color.White.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIconFor(category.iconName),
                    contentDescription = category.name,
                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    onProductClick: (Product) -> Unit,
    onShopClick: (String) -> Unit,
    onCompareClick: (Product) -> Unit,
    onWhatsAppClick: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onProductClick(product) }
            .testTag("product_card_${product.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Product Image / Placeholder Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (product.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(145.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = categoryIconFor(product.categoryId),
                        contentDescription = product.categoryName,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(54.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AvailabilityBadge(isAvailable = product.isAvailable, stockQuantity = product.stockQuantity)

                    if (product.isNewArrival) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondary,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "NEW",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = product.categoryName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Price in PKR
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = formatPkr(product.pricePkr),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (product.originalPricePkr > product.pricePkr) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatPkr(product.originalPricePkr),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Shop & Market Attribution
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onShopClick(product.shopId) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = "Shop",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "${product.shopName} • ${product.marketName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "Updated: ${formatTimestampShort(product.updatedAt ?: product.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onCompareClick(product) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CompareArrows,
                            contentDescription = "Compare Prices",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Compare", style = MaterialTheme.typography.labelMedium)
                    }

                    Button(
                        onClick = { onWhatsAppClick(product) },
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("whatsapp_product_${product.id}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WhatsAppGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "WhatsApp",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("WhatsApp", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun ShopCard(
    shop: Shop,
    onClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("shop_card_${shop.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (shop.logoUrl.isNotBlank()) {
                        AsyncImage(
                            model = shop.logoUrl,
                            contentDescription = shop.name,
                            modifier = Modifier.size(52.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = shop.name,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = shop.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        VerifiedShopBadge(isVerified = shop.isVerified)
                    }
                    Text(
                        text = shop.categoryName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Market",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = shop.marketName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (shop.isFeatured) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Featured",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Featured",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = shop.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hours: ${shop.businessHours}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onWhatsAppClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WhatsAppGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = "WhatsApp Shop",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("WhatsApp", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun OfferCard(
    offer: Offer,
    onShopClick: (String) -> Unit,
    onWhatsAppClick: (Offer) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(290.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onShopClick(offer.shopId) },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalOffer,
                            contentDescription = "Discount",
                            tint = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = offer.discountBadge,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = offer.marketName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = offer.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = offer.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = formatPkr(offer.promoPricePkr),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = formatPkr(offer.originalPricePkr),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = TextDecoration.LineThrough
                    )
                }
                Button(
                    onClick = { onWhatsAppClick(offer) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WhatsAppGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Claim via WhatsApp", style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "By ${offer.shopName}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun CustomerInquiryModal(
    shopId: String,
    shopName: String,
    shopOwnerId: String,
    whatsappNumber: String,
    productId: String = "",
    productName: String = "General Shop Inquiry",
    displayedPricePkr: Long = 0L,
    onDismiss: () -> Unit,
    onSubmitInquiry: (customerPhone: String, message: String, channel: String) -> Unit
) {
    val defaultMessage = remember(shopName, productName, displayedPricePkr) {
        if (displayedPricePkr > 0L) {
            "Assalam-o-Alaikum $shopName, is '$productName' (${formatPkr(displayedPricePkr)}) currently available? I would like to confirm stock and pickup/delivery details."
        } else {
            "Assalam-o-Alaikum $shopName, I saw your shop on MarketHub and wanted to inquire about your products."
        }
    }
    var customerPhone by remember { mutableStateOf("") }
    var message by remember { mutableStateOf(defaultMessage) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Contact $shopName",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (displayedPricePkr > 0L) {
                    Text(
                        text = "Product: $productName • ${formatPkr(displayedPricePkr)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "No online payment is required. Contact the shopkeeper directly to verify availability and arrange purchase.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = { customerPhone = it },
                    label = { Text("Your Phone / WhatsApp (Optional)") },
                    placeholder = { Text("+92 300 1234567") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Your Inquiry Message") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        if (message.trim().length >= 2) {
                            onSubmitInquiry(customerPhone, message, "in_app")
                        }
                    }
                ) {
                    Text("Send In-App")
                }
                Button(
                    onClick = {
                        if (message.trim().length >= 2) {
                            onSubmitInquiry(customerPhone, message, "whatsapp")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WhatsAppGreen,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open WhatsApp")
                }
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
fun ReportListingDialog(
    targetType: String,
    targetId: String,
    targetName: String,
    shopId: String,
    onDismiss: () -> Unit,
    onSubmit: (reason: String) -> Unit
) {
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Flag,
                contentDescription = "Report",
                tint = DangerRed
            )
        },
        title = {
            Text("Report ${if (targetType == "shop") "Shop" else "Product"}")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Reporting: $targetName",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason (e.g., inaccurate price, fake item, spam)") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (reason.trim().length >= 3) {
                        onSubmit(reason.trim())
                    }
                },
                enabled = reason.trim().length >= 3,
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
            ) {
                Text("Submit Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
