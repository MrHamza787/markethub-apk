package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.text.NumberFormat
import java.util.Locale

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val role: String = "customer", // "customer", "shopkeeper", "admin"
    val phone: String = "",
    @get:PropertyName("isSuspended") @set:PropertyName("isSuspended")
    var isSuspended: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Market(
    @DocumentId val id: String = "",
    val name: String = "",
    val city: String = "",
    val address: String = "",
    val description: String = "",
    @get:PropertyName("isActive") @set:PropertyName("isActive")
    var isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Category(
    @DocumentId val id: String = "",
    val name: String = "",
    val slug: String = "",
    val iconName: String = "store",
    val description: String = "",
    val displayOrder: Long = 0L,
    @get:PropertyName("isActive") @set:PropertyName("isActive")
    var isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Shop(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val name: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val marketId: String = "",
    val marketName: String = "",
    val address: String = "",
    val mapLocation: String = "",
    val description: String = "",
    val businessHours: String = "10:00 AM - 10:00 PM (Mon-Sat)",
    val contactPhone: String = "",
    val whatsappNumber: String = "",
    val logoUrl: String = "",
    val coverUrl: String = "",
    val status: String = "pending", // "pending", "approved", "rejected", "suspended"
    @get:PropertyName("isVerified") @set:PropertyName("isVerified")
    var isVerified: Boolean = false,
    @get:PropertyName("isFeatured") @set:PropertyName("isFeatured")
    var isFeatured: Boolean = false,
    val viewsCount: Long = 0L,
    val inquiriesCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val shopId: String = "",
    val shopName: String = "",
    val marketName: String = "",
    val ownerId: String = "",
    val name: String = "",
    val normalizedName: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val description: String = "",
    val pricePkr: Long = 0L,
    val originalPricePkr: Long = 0L,
    val stockQuantity: Long = 0L,
    @get:PropertyName("isAvailable") @set:PropertyName("isAvailable")
    var isAvailable: Boolean = true,
    @get:PropertyName("isNewArrival") @set:PropertyName("isNewArrival")
    var isNewArrival: Boolean = true,
    @get:PropertyName("isPopular") @set:PropertyName("isPopular")
    var isPopular: Boolean = false,
    val imageUrl: String = "",
    val whatsappNumber: String = "",
    val status: String = "active", // "active", "suspended"
    val viewsCount: Long = 0L,
    val inquiriesCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Offer(
    @DocumentId val id: String = "",
    val shopId: String = "",
    val shopName: String = "",
    val marketName: String = "",
    val ownerId: String = "",
    val productId: String = "",
    val title: String = "",
    val description: String = "",
    val discountBadge: String = "",
    val promoPricePkr: Long = 0L,
    val originalPricePkr: Long = 0L,
    val whatsappNumber: String = "",
    @get:PropertyName("isActive") @set:PropertyName("isActive")
    var isActive: Boolean = true,
    @get:PropertyName("isFeatured") @set:PropertyName("isFeatured")
    var isFeatured: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Inquiry(
    @DocumentId val id: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val shopId: String = "",
    val shopName: String = "",
    val shopOwnerId: String = "",
    val productId: String = "",
    val productName: String = "",
    val displayedPricePkr: Long = 0L,
    val message: String = "",
    val channel: String = "whatsapp", // "whatsapp", "in_app"
    val status: String = "new", // "new", "replied", "closed"
    val shopkeeperReply: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Report(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val reporterName: String = "",
    val targetType: String = "product", // "product", "shop"
    val targetId: String = "",
    val targetName: String = "",
    val shopId: String = "",
    val reason: String = "",
    val status: String = "open", // "open", "resolved", "dismissed"
    val adminNotes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class PriceComparisonGroup(
    val groupKey: String,
    val displayTitle: String,
    val categoryName: String,
    val lowestPricePkr: Long,
    val highestPricePkr: Long,
    val listings: List<Product>
)

object MarketHubDefaults {
    const val BOOTSTRAPPED_ADMIN_EMAIL = "awan11118@gmail.com"

    val defaultCategories: List<Category> = listOf(
        Category(
            id = "cat_clothing",
            name = "Clothing and garments",
            slug = "clothing-and-garments",
            iconName = "checkroom",
            description = "Unstitched lawn, ready-to-wear kurtas, formal wear & men's suiting",
            displayOrder = 1L,
            isActive = true
        ),
        Category(
            id = "cat_shoes",
            name = "Shoes and footwear",
            slug = "shoes-and-footwear",
            iconName = "hiking",
            description = "Peshawari chappals, khussas, sneakers, formal leather shoes & sandals",
            displayOrder = 2L,
            isActive = true
        ),
        Category(
            id = "cat_mobile",
            name = "Mobile phones and accessories",
            slug = "mobile-phones-and-accessories",
            iconName = "smartphone",
            description = "PTA-approved smartphones, earbuds, power banks, cases & fast chargers",
            displayOrder = 3L,
            isActive = true
        ),
        Category(
            id = "cat_computers",
            name = "Computers and electronics",
            slug = "computers-and-electronics",
            iconName = "laptop",
            description = "Laptops, gaming PCs, monitors, networking gear & accessories",
            displayOrder = 4L,
            isActive = true
        ),
        Category(
            id = "cat_cosmetics",
            name = "Cosmetics and beauty",
            slug = "cosmetics-and-beauty",
            iconName = "face",
            description = "Skincare, makeup, perfumes, attar, haircare & beauty tools",
            displayOrder = 5L,
            isActive = true
        ),
        Category(
            id = "cat_grocery",
            name = "Grocery and general stores",
            slug = "grocery-and-general-stores",
            iconName = "shopping_basket",
            description = "Basmati rice, spices, dry fruits, cooking oil, tea & daily essentials",
            displayOrder = 6L,
            isActive = true
        ),
        Category(
            id = "cat_jewellery",
            name = "Jewellery and watches",
            slug = "jewellery-and-watches",
            iconName = "watch",
            description = "Bridal sets, artificial & sterling jewellery, smartwatches & luxury timepieces",
            displayOrder = 7L,
            isActive = true
        ),
        Category(
            id = "cat_home",
            name = "Home appliances and furniture",
            slug = "home-appliances-and-furniture",
            iconName = "weekend",
            description = "Inverter ACs, refrigerators, kitchen appliances, sofa sets & decor",
            displayOrder = 8L,
            isActive = true
        ),
        Category(
            id = "cat_gifts",
            name = "Gifts and toys",
            slug = "gifts-and-toys",
            iconName = "card_giftcard",
            description = "Educational toys, gift hampers, board games, stationery & souvenirs",
            displayOrder = 9L,
            isActive = true
        ),
        Category(
            id = "cat_other",
            name = "Other shops",
            slug = "other-shops",
            iconName = "storefront",
            description = "Books, sports gear, opticals, tailoring & specialty market stores",
            displayOrder = 10L,
            isActive = true
        )
    )

    val defaultMarkets: List<Market> = listOf(
        Market(
            id = "mkt_liberty_lhr",
            name = "Liberty & Gulberg Commercial Market",
            city = "Lahore",
            address = "Main Boulevard Gulberg III, Lahore",
            description = "Premier commercial shopping hub for clothing, footwear, jewellery, and electronics.",
            isActive = true
        ),
        Market(
            id = "mkt_hafeez_lhr",
            name = "Hafeez Center & Hall Road Plaza",
            city = "Lahore",
            address = "Gulberg III / Mall Road, Lahore",
            description = "Pakistan's largest mobile phones, laptops, and electronics wholesale & retail market.",
            isActive = true
        ),
        Market(
            id = "mkt_anarkali_lhr",
            name = "Anarkali & Shah Alam Bazaar",
            city = "Lahore",
            address = "Mall Road & Circular Road, Lahore",
            description = "Historic commercial bazaar featuring garments, footwear, cosmetics, and general stores.",
            isActive = true
        ),
        Market(
            id = "mkt_tariq_khi",
            name = "Tariq Road & Saddar Commercial Market",
            city = "Karachi",
            address = "PECHS & Saddar Town, Karachi",
            description = "Vibrant multi-category market for fashion, mobile plazas, appliances, and watches.",
            isActive = true
        ),
        Market(
            id = "mkt_jinnah_isb",
            name = "Jinnah Super & Blue Area Market",
            city = "Islamabad",
            address = "F-7 Markaz & Blue Area, Islamabad",
            description = "Modern capital commercial center with verified boutiques, electronics, and gift shops.",
            isActive = true
        ),
        Market(
            id = "mkt_raja_rwp",
            name = "Raja Bazaar & Commercial Market",
            city = "Rawalpindi",
            address = "Satellite Town & Raja Bazaar, Rawalpindi",
            description = "Bustling wholesale and retail marketplace for household appliances, grocery, and garments.",
            isActive = true
        )
    )
}

fun formatPkr(amount: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    return "Rs. ${formatter.format(amount)}"
}

fun normalizeProductNameForComparison(name: String): String {
    return name.lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9\\s]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(120)
        .let { if (it.length < 2) "item" else it }
}
