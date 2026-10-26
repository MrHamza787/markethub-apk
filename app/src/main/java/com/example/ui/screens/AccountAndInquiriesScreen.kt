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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Inquiry
import com.example.data.model.UserProfile
import com.example.data.model.formatPkr
import com.example.ui.components.formatTimestampShort
import com.example.ui.theme.DangerRed

@Composable
fun AccountAndInquiriesScreen(
    userProfile: UserProfile?,
    fallbackName: String,
    fallbackEmail: String,
    isAdmin: Boolean,
    customerInquiries: List<Inquiry>,
    onUpdateProfile: (displayName: String, phone: String, role: String) -> Unit,
    onSignOut: () -> Unit
) {
    var displayName by remember(userProfile?.displayName) {
        mutableStateOf(userProfile?.displayName?.ifBlank { fallbackName } ?: fallbackName)
    }
    var phone by remember(userProfile?.phone) {
        mutableStateOf(userProfile?.phone ?: "")
    }
    var selectedRole by remember(userProfile?.role) {
        mutableStateOf(userProfile?.role ?: "customer")
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("account_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "My MarketHub Account",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = userProfile?.email?.ifBlank { fallbackEmail } ?: fallbackEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isAdmin) "ADMIN" else (userProfile?.role ?: "CUSTOMER").uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone / WhatsApp Number") },
                        placeholder = { Text("+92 300 1234567") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!isAdmin) {
                        Text(
                            text = "Account Mode",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = selectedRole == "customer",
                                onClick = { selectedRole = "customer" },
                                label = { Text("Customer") }
                            )
                            FilterChip(
                                selected = selectedRole == "shopkeeper",
                                onClick = { selectedRole = "shopkeeper" },
                                label = { Text("Shopkeeper / Vendor") }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onUpdateProfile(
                                    displayName.trim().ifBlank { "MarketHub User" },
                                    phone.trim(),
                                    if (isAdmin) "admin" else selectedRole
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Profile")
                        }

                        OutlinedButton(
                            onClick = onSignOut,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sign Out")
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "My Sent Product & Shop Inquiries (${customerInquiries.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Track your inquiries sent to shopkeepers and view their responses",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (customerInquiries.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "You haven't sent any inquiries yet. Tap 'WhatsApp' or 'Send Inquiry' on any shop or product page to contact a shopkeeper.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(customerInquiries, key = { it.id }) { inquiry ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = inquiry.shopName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${inquiry.channel.uppercase()} • ${inquiry.status.uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${inquiry.productName} (${formatPkr(inquiry.displayedPricePkr)}) • ${formatTimestampShort(inquiry.createdAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = inquiry.message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (inquiry.shopkeeperReply.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Shopkeeper Reply: ${inquiry.shopkeeperReply}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}
