package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketHubRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun shopRegistration_createsPendingShop_andOwnerCanObserveIt() = runBlocking {
        val ownerUid = signInTestUser("shopkeeper_a@markethub.pk")
        val repository = MarketHubRepository(firestore)

        val profileResult = withTimeout(TIMEOUT_MS) {
            repository.ensureUserProfile("Tariq Electronics", "shopkeeper_a@markethub.pk", "shopkeeper")
        }
        assertTrue(profileResult.isSuccess)

        val shopResult = withTimeout(TIMEOUT_MS) {
            repository.submitShopRegistration(
                ownerName = "Tariq Mahmood",
                name = "Tariq Mobile Zone",
                categoryId = "cat_mobile",
                categoryName = "Mobile phones and accessories",
                marketId = "mkt_hafeez_lhr",
                marketName = "Hafeez Center & Hall Road Plaza",
                address = "Shop 24, First Floor, Gulberg III, Lahore",
                mapLocation = "31.5162, 74.3435",
                description = "Official smartphone and accessories retailer in Hafeez Center.",
                businessHours = "11:00 AM - 10:00 PM",
                contactPhone = "+923214455667",
                whatsappNumber = "923214455667"
            )
        }
        assertTrue("Expected shop submission to succeed: ${shopResult.exceptionOrNull()}", shopResult.isSuccess)
        val shopId = shopResult.getOrThrow()

        val myShops = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeMyShops(ownerUid).first { list -> list.any { it.id == shopId } }
        }
        val createdShop = myShops.first { it.id == shopId }
        assertEquals("pending", createdShop.status)
        assertEquals("Tariq Mobile Zone", createdShop.name)
    }

    @Test
    fun unapprovedShopkeeper_cannotCreateProduct_failsWithPermissionDenied() = runBlocking {
        val ownerUid = signInTestUser("shopkeeper_pending@markethub.pk")
        val repository = MarketHubRepository(firestore)

        val shopId = withTimeout(TIMEOUT_MS) {
            repository.submitShopRegistration(
                ownerName = "Bilal",
                name = "Bilal Footwear",
                categoryId = "cat_shoes",
                categoryName = "Shoes and footwear",
                marketId = "mkt_anarkali_lhr",
                marketName = "Anarkali Bazaar",
                address = "Shop 8, Anarkali, Lahore",
                mapLocation = "",
                description = "Handcrafted leather chappals and formal shoes.",
                businessHours = "10:00 AM - 9:00 PM",
                contactPhone = "+923009988776",
                whatsappNumber = "923009988776"
            ).getOrThrow()
        }

        val pendingShop = withTimeout(TIMEOUT_MS) {
            repository.getShopById(shopId).getOrThrow()
        }

        val productResult = withTimeout(TIMEOUT_MS) {
            repository.createProduct(
                shop = pendingShop,
                name = "Peshawari Chappal Classic",
                categoryId = "cat_shoes",
                categoryName = "Shoes and footwear",
                description = "Pure leather hand-stitched Peshawari chappal",
                pricePkr = 4500L
            )
        }
        assertTrue(productResult.isFailure)
        val firestoreEx = productResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun crossUserProfileRead_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser("alice_customer@markethub.pk")
        val aliceRepo = MarketHubRepository(firestore)
        withTimeout(TIMEOUT_MS) {
            aliceRepo.ensureUserProfile("Alice", "alice_customer@markethub.pk", "customer").getOrThrow()
        }

        signInTestUser("bob_customer@markethub.pk")
        val bobRepo = MarketHubRepository(firestore)

        var caughtException: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeUserProfile(aliceUid).first()
            }
        } catch (e: Throwable) {
            caughtException = e
        }
        assertNotNull(caughtException)
        val firestoreEx = caughtException?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException, got $caughtException", firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun unauthenticatedCaller_cannotObserveApprovedShops_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = MarketHubRepository(firestore)

        var caughtException: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeApprovedShops().first()
            }
        } catch (e: Throwable) {
            caughtException = e
        }
        assertNotNull(caughtException)
        val firestoreEx = caughtException?.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException, got $caughtException", firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun nonAdminUser_cannotSavePaymentSettings_failsWithPermissionDenied_andWhatsAppOrderMessageFormatsCorrectly() = runBlocking {
        signInTestUser("regular_customer@markethub.pk")
        val repository = MarketHubRepository(firestore)

        val saveResult = withTimeout(TIMEOUT_MS) {
            repository.savePaymentSettingsAsAdmin(
                bankTransferEnabled = true,
                bankName = "Meezan Bank",
                bankAccountTitle = "MarketHub Merchant",
                bankAccountNumberOrIban = "PK00MEZN00010203040506",
                bankInstructions = "Include Order Ref",
                easypaisaEnabled = true,
                easypaisaAccountHolderName = "MarketHub",
                easypaisaMobileNumber = "03001234567",
                easypaisaInstructions = "",
                binancePayEnabled = false,
                binanceMerchantName = "",
                binancePayIdOrInstructions = "",
                codEnabledByDefault = true,
                allowShopkeeperPaymentVerification = false
            )
        }
        assertTrue(saveResult.isFailure)
        val firestoreEx = saveResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)

        val sampleOrder = com.example.data.model.Order(
            id = "ord_test_1",
            orderNumber = "MH-908172",
            productId = "prod_101",
            productName = "Samsung Galaxy A55 5G",
            shopId = "shop_1",
            shopName = "Tariq Mobile Zone",
            shopWhatsappNumber = "923214455667",
            quantity = 2L,
            unitPricePkr = 135000L,
            subtotalPkr = 270000L,
            customerName = "Ali Raza",
            customerPhone = "03001122334",
            deliveryType = "delivery",
            deliveryAddress = "House 12, Model Town, Lahore",
            paymentMethod = "BANK_TRANSFER"
        )
        val waMessage = com.example.data.model.buildWhatsAppDirectPurchaseOrderMessage(sampleOrder)
        assertTrue(waMessage.contains("Hello! I want to purchase this product from MarketHub."))
        assertTrue(waMessage.contains("- Product Name: Samsung Galaxy A55 5G"))
        assertTrue(waMessage.contains("- Product ID: prod_101"))
        assertTrue(waMessage.contains("- Shop Name: Tariq Mobile Zone"))
        assertTrue(waMessage.contains("- Quantity: 2"))
        assertTrue(waMessage.contains("- Unit Price: Rs. 135,000"))
        assertTrue(waMessage.contains("- Subtotal: Rs. 270,000"))
        assertTrue(waMessage.contains("- Product Link: https://markethub.pk/shops/shop_1/products/prod_101"))
        assertTrue(waMessage.contains("- Customer Name: Ali Raza"))
        assertTrue(waMessage.contains("- Contact Number: 03001122334"))
        assertTrue(waMessage.contains("- Delivery Address: House 12, Model Town, Lahore"))
        assertTrue(waMessage.contains("Selected Payment Method: Bank Transfer"))
        assertTrue(waMessage.contains("Please confirm product availability, delivery charges and the final payable amount."))
    }

    private companion object {
        const val TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
