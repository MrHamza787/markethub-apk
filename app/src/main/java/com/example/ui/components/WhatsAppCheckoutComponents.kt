package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.Order
import com.example.data.model.PaymentMethodOption
import com.example.data.model.PaymentSettings
import com.example.data.model.Product
import com.example.data.model.buildWhatsAppDirectPurchaseOrderMessage
import com.example.data.model.formatPkr
import com.example.ui.theme.DangerRed
import com.example.ui.theme.WhatsAppGreen
import java.io.ByteArrayOutputStream
import java.util.UUID

private fun encodePickedImageUriToCompactDataUri(context: Context, uri: Uri): String {
    return try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val original = BitmapFactory.decodeStream(input) ?: return uri.toString()
            val maxDim = 480
            val ratio = minOf(maxDim.toFloat() / original.width, maxDim.toFloat() / original.height, 1f)
            val width = (original.width * ratio).toInt().coerceAtLeast(1)
            val height = (original.height * ratio).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(original, width, height, true)
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 65, out)
            val base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64"
        } ?: uri.toString()
    } catch (_: Exception) {
        uri.toString()
    }
}

@Composable
fun PaymentStatusBadge(paymentStatus: String, modifier: Modifier = Modifier) {
    val (bgColor, textColor, icon) = when (paymentStatus) {
        "Payment Verified" -> Triple(Color(0xFFDCFCE7), Color(0xFF15803D), Icons.Default.VerifiedUser)
        "Proof Submitted" -> Triple(Color(0xFFE0F2FE), Color(0xFF0369A1), Icons.Default.CheckCircle)
        "Under Review" -> Triple(Color(0xFFFEF3C7), Color(0xFFB45309), Icons.Default.Schedule)
        "Payment Rejected" -> Triple(Color(0xFFFEE2E2), DangerRed, Icons.Default.Warning)
        else -> Triple(Color(0xFFFEF9C3), Color(0xFF854D0E), Icons.Default.Schedule) // Awaiting Payment
    }
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = paymentStatus,
                tint = textColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = paymentStatus,
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PaymentMethodInstructionsCard(
    selectedMethod: PaymentMethodOption,
    paymentSettings: PaymentSettings,
    requiredAmountPkr: Long,
    orderReference: String,
    shopName: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("payment_instructions_card_${selectedMethod.key}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            when (selectedMethod) {
                PaymentMethodOption.BANK_TRANSFER -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Bank Transfer Instructions (Verified)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (paymentSettings.isBankConfiguredAndEnabled()) {
                        InstructionRow("Bank Name", paymentSettings.bankName)
                        InstructionRow("Account Title", paymentSettings.bankAccountTitle)
                        InstructionRow("Account Number / IBAN", paymentSettings.bankAccountNumberOrIban)
                        InstructionRow("Required Payment Amount", formatPkr(requiredAmountPkr))
                        InstructionRow("Payment / Order Reference", orderReference)
                        if (paymentSettings.bankInstructions.isNotBlank()) {
                            Text(
                                text = paymentSettings.bankInstructions,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = "Bank Transfer details have not yet been configured by the administrator.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DangerRed
                        )
                    }
                }

                PaymentMethodOption.EASYPAISA -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Easypaisa Payment Instructions (Verified)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (paymentSettings.isEasypaisaConfiguredAndEnabled()) {
                        InstructionRow("Account Holder Name", paymentSettings.easypaisaAccountHolderName)
                        InstructionRow("Easypaisa Mobile Account", paymentSettings.easypaisaMobileNumber)
                        InstructionRow("Required Payment Amount", formatPkr(requiredAmountPkr))
                        InstructionRow("Order Reference", orderReference)
                        if (paymentSettings.easypaisaInstructions.isNotBlank()) {
                            Text(
                                text = paymentSettings.easypaisaInstructions,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = "Easypaisa account details have not yet been configured by the administrator.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DangerRed
                        )
                    }
                }

                PaymentMethodOption.BINANCE_PAY -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CurrencyBitcoin,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Binance Pay — Manual Payment Instructions",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "MANUAL PAYMENT ONLY — Payment remains unverified until confirmed by an authorized verifier.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF92400E),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    if (paymentSettings.isBinanceConfiguredAndEnabled()) {
                        InstructionRow("Verified Merchant", paymentSettings.binanceMerchantName)
                        InstructionRow("Instructions / Pay ID", paymentSettings.binancePayIdOrInstructions)
                        InstructionRow("Equivalent PKR Amount", formatPkr(requiredAmountPkr))
                        InstructionRow("Order Reference", orderReference)
                    } else {
                        Text(
                            text = "Binance Pay merchant instructions have not yet been configured by the administrator.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DangerRed
                        )
                    }
                }

                PaymentMethodOption.CASH_ON_DELIVERY -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Cash on Delivery / Pay at Shop",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    InstructionRow("Order Subtotal", formatPkr(requiredAmountPkr))
                    InstructionRow("Order Reference", orderReference)
                    Text(
                        text = "Confirm final delivery charges and availability directly with $shopName on WhatsApp. Pay cash upon receiving your order.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun InstructionRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.45f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.55f)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WhatsAppDirectCheckoutDialog(
    product: Product,
    paymentSettings: PaymentSettings,
    defaultCustomerName: String,
    defaultCustomerPhone: String,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onConfirmAndCreateOrder: (
        idempotencyKey: String,
        quantity: Long,
        customerName: String,
        customerPhone: String,
        deliveryType: String,
        deliveryAddress: String,
        paymentMethod: String,
        onOrderSaved: (Order) -> Unit
    ) -> Unit,
    onSubmitPaymentProof: (
        orderId: String,
        transactionReference: String,
        proofScreenshotUri: String,
        onSuccess: () -> Unit
    ) -> Unit
) {
    val context = LocalContext.current
    val enabledMethods = remember(paymentSettings) {
        paymentSettings.enabledPaymentMethods().ifEmpty {
            listOf(PaymentMethodOption.CASH_ON_DELIVERY)
        }
    }

    // Unique idempotency key per checkout modal instance to prevent duplicate orders on repeated clicks
    val sessionIdempotencyKey = remember(product.id) {
        val rand = UUID.randomUUID().toString().replace("-", "").take(10)
        "chk_${product.id.takeLast(6)}_$rand"
    }
    val previewOrderReference = remember(sessionIdempotencyKey) {
        "MH-${sessionIdempotencyKey.takeLast(6).uppercase()}"
    }

    val maxStock = product.stockQuantity.coerceAtLeast(1L)
    var quantity by remember { mutableLongStateOf(1L) }
    var customerName by remember(defaultCustomerName) { mutableStateOf(defaultCustomerName) }
    var customerPhone by remember(defaultCustomerPhone) { mutableStateOf(defaultCustomerPhone) }
    var deliveryType by remember { mutableStateOf("delivery") } // "delivery" or "pickup"
    var deliveryAddress by remember { mutableStateOf("") }
    var selectedMethod by remember(enabledMethods) {
        mutableStateOf(enabledMethods.first())
    }

    // Saved order state once Step 1 completes
    var createdOrder by remember { mutableStateOf<Order?>(null) }
    var transactionRef by remember { mutableStateOf("") }
    var screenshotUri by remember { mutableStateOf("") }
    var proofSubmittedLocal by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            screenshotUri = encodePickedImageUriToCompactDataUri(context, uri)
        }
    }

    val subtotalPkr = quantity * product.pricePkr

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ShoppingBag,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (createdOrder == null) "Buy Now — WhatsApp Checkout" else "Order Recorded (${createdOrder?.orderNumber})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (createdOrder == null) {
                    // STEP 1: Order Confirmation & Payment Method Selection
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = product.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Shop: ${product.shopName} (${product.marketName})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Unit Price: ${formatPkr(product.pricePkr)}",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    text = "Subtotal: ${formatPkr(subtotalPkr)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Quantity Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Select Quantity",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${product.stockQuantity} available in stock",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (quantity > 1L) quantity -= 1L },
                                enabled = quantity > 1L,
                                modifier = Modifier.testTag("checkout_qty_decrease")
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease quantity")
                            }
                            Text(
                                text = quantity.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .testTag("checkout_qty_text")
                            )
                            IconButton(
                                onClick = { if (quantity < maxStock) quantity += 1L },
                                enabled = quantity < maxStock,
                                modifier = Modifier.testTag("checkout_qty_increase")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase quantity")
                            }
                        }
                    }

                    HorizontalDivider()

                    // Customer Details
                    Text(
                        text = "Customer & Delivery Details",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("Your Full Name *") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checkout_name_input")
                    )
                    OutlinedTextField(
                        value = customerPhone,
                        onValueChange = { customerPhone = it },
                        label = { Text("Mobile / WhatsApp Number *") },
                        placeholder = { Text("+92 300 1234567") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checkout_phone_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = deliveryType == "delivery",
                            onClick = { deliveryType = "delivery" },
                            label = { Text("Home Delivery") },
                            modifier = Modifier.testTag("checkout_delivery_chip")
                        )
                        FilterChip(
                            selected = deliveryType == "pickup",
                            onClick = { deliveryType = "pickup" },
                            label = { Text("Local Shop Pickup") },
                            modifier = Modifier.testTag("checkout_pickup_chip")
                        )
                    }

                    if (deliveryType == "delivery") {
                        OutlinedTextField(
                            value = deliveryAddress,
                            onValueChange = { deliveryAddress = it },
                            label = { Text("Delivery Address *") },
                            placeholder = { Text("House/Street, Block, Area, City") },
                            minLines = 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("checkout_address_input")
                        )
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Pickup Location: ${product.shopName}, ${product.marketName}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    HorizontalDivider()

                    // Payment Method Selection
                    Text(
                        text = "Select Payment Method",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        enabledMethods.forEach { method ->
                            FilterChip(
                                selected = selectedMethod == method,
                                onClick = { selectedMethod = method },
                                label = { Text(method.displayLabel) },
                                modifier = Modifier.testTag("payment_method_chip_${method.key}")
                            )
                        }
                    }

                    PaymentMethodInstructionsCard(
                        selectedMethod = selectedMethod,
                        paymentSettings = paymentSettings,
                        requiredAmountPkr = subtotalPkr,
                        orderReference = previewOrderReference,
                        shopName = product.shopName
                    )
                } else {
                    // STEP 2: Order Created -> Continue to WhatsApp & Optional Manual Payment Proof Submission
                    val order = createdOrder!!
                    val effectivePaymentStatus = if (proofSubmittedLocal) "Proof Submitted" else order.paymentStatus

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Order #${order.orderNumber}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                PaymentStatusBadge(paymentStatus = effectivePaymentStatus)
                            }
                            Text(
                                text = "${order.productName} × ${order.quantity} = ${formatPkr(order.subtotalPkr)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Shop: ${order.shopName} • WhatsApp: ${order.shopWhatsappNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Explicit WhatsApp Manual Send Notice + Continue to WhatsApp Button
                    Surface(
                        color = Color(0xFFDCFCE7),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Send Your Pre-Filled Order on WhatsApp",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF14532D)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Note: WhatsApp messages are never sent automatically. Please press Send inside WhatsApp to share your order with ${order.shopName}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF166534)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val msg = buildWhatsAppDirectPurchaseOrderMessage(order)
                                    launchWhatsAppIntent(context, order.shopWhatsappNumber, msg)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WhatsAppGreen,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("continue_to_whatsapp_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Continue to WhatsApp", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    PaymentMethodInstructionsCard(
                        selectedMethod = PaymentMethodOption.fromKey(order.paymentMethod),
                        paymentSettings = paymentSettings,
                        requiredAmountPkr = order.subtotalPkr,
                        orderReference = order.orderNumber,
                        shopName = order.shopName
                    )

                    // Manual Payment Proof Upload Section for Bank Transfer, Easypaisa & Binance Pay
                    if (order.paymentMethod != PaymentMethodOption.CASH_ON_DELIVERY.key) {
                        HorizontalDivider()
                        Text(
                            text = "Submit Manual Payment Proof",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "After completing your transfer, submit your Transaction Reference / TID and optional payment screenshot. Only an authorized administrator or payment verifier can mark a payment as Verified.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = transactionRef,
                            onValueChange = { transactionRef = it },
                            label = { Text("Transaction Reference / TID *") },
                            placeholder = { Text("e.g., TID-992837461 or Bank Ref #") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("payment_proof_tid_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (screenshotUri.isBlank()) "Attach Screenshot (Optional)" else "Change Screenshot")
                            }
                        }

                        if (screenshotUri.isNotBlank()) {
                            AsyncImage(
                                model = screenshotUri,
                                contentDescription = "Payment Proof Screenshot",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Button(
                            onClick = {
                                if (transactionRef.trim().length >= 2) {
                                    onSubmitPaymentProof(
                                        order.id,
                                        transactionRef.trim(),
                                        screenshotUri
                                    ) {
                                        proofSubmittedLocal = true
                                    }
                                }
                            },
                            enabled = !isBusy && transactionRef.trim().length >= 2 && !proofSubmittedLocal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("submit_payment_proof_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (proofSubmittedLocal) "Proof Submitted (Awaiting Review)" else "Submit Payment Proof")
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (createdOrder == null) {
                val canConfirm = !isBusy &&
                    customerName.trim().isNotBlank() &&
                    customerPhone.trim().length >= 5 &&
                    (deliveryType == "pickup" || deliveryAddress.trim().length >= 3)

                Button(
                    onClick = {
                        val effectiveAddress = if (deliveryType == "pickup") {
                            "${product.shopName}, ${product.marketName}"
                        } else {
                            deliveryAddress.trim()
                        }
                        onConfirmAndCreateOrder(
                            sessionIdempotencyKey,
                            quantity,
                            customerName.trim(),
                            customerPhone.trim(),
                            deliveryType,
                            effectiveAddress,
                            selectedMethod.key
                        ) { savedOrder ->
                            createdOrder = savedOrder
                            val waMessage = buildWhatsAppDirectPurchaseOrderMessage(savedOrder)
                            launchWhatsAppIntent(context, savedOrder.shopWhatsappNumber, waMessage)
                        }
                    },
                    enabled = canConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WhatsAppGreen,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_whatsapp_order_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isBusy) "Saving Order..." else "Confirm Order & Open WhatsApp")
                }
            } else {
                Button(onClick = onDismiss) {
                    Text("Done")
                }
            }
        },
        dismissButton = {
            if (createdOrder == null) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun CustomerOrderCard(
    order: Order,
    paymentSettings: PaymentSettings,
    onOpenWhatsApp: (Order) -> Unit,
    onSubmitPaymentProof: (orderId: String, tid: String, screenshotUri: String) -> Unit
) {
    val context = LocalContext.current
    var showProofModal by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_order_card_${order.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Order #${order.orderNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${order.shopName} • ${formatTimestampShort(order.createdAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                PaymentStatusBadge(paymentStatus = order.paymentStatus)
            }

            Text(
                text = "${order.productName} × ${order.quantity}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Method: ${PaymentMethodOption.fromKey(order.paymentMethod).displayLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Subtotal: ${formatPkr(order.subtotalPkr)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (order.transactionReference.isNotBlank()) {
                Text(
                    text = "Submitted Reference / TID: ${order.transactionReference}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (order.verifierNotes.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Verifier Note: ${order.verifierNotes}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (order.paymentMethod != PaymentMethodOption.CASH_ON_DELIVERY.key &&
                    (order.paymentStatus == "Awaiting Payment" || order.paymentStatus == "Payment Rejected")
                ) {
                    OutlinedButton(
                        onClick = { showProofModal = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Submit Payment Proof", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Button(
                    onClick = { onOpenWhatsApp(order) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WhatsAppGreen,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Continue to WhatsApp", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }

    if (showProofModal) {
        var tid by remember { mutableStateOf(order.transactionReference) }
        var screenshot by remember { mutableStateOf(order.proofScreenshotUri) }
        val picker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri: Uri? ->
            if (uri != null) {
                screenshot = encodePickedImageUriToCompactDataUri(context, uri)
            }
        }

        AlertDialog(
            onDismissRequest = { showProofModal = false },
            title = { Text("Submit Payment Proof — ${order.orderNumber}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PaymentMethodInstructionsCard(
                        selectedMethod = PaymentMethodOption.fromKey(order.paymentMethod),
                        paymentSettings = paymentSettings,
                        requiredAmountPkr = order.subtotalPkr,
                        orderReference = order.orderNumber,
                        shopName = order.shopName
                    )
                    OutlinedTextField(
                        value = tid,
                        onValueChange = { tid = it },
                        label = { Text("Transaction Reference / TID *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (screenshot.isBlank()) "Upload Payment Screenshot (Optional)" else "Screenshot Attached (Tap to Change)")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tid.trim().length >= 2) {
                            onSubmitPaymentProof(order.id, tid.trim(), screenshot)
                            showProofModal = false
                        }
                    },
                    enabled = tid.trim().length >= 2
                ) {
                    Text("Submit Proof")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProofModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
