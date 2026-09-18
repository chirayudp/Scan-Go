package com.example.ui.screens.shopper

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.UserProfile
import com.example.auth.appAuthManager
import com.example.data.CartItem
import com.example.data.Product
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutSummaryScreen(
    cart: List<CartItem>,
    maxItems: Int,
    checkoutStatus: CheckoutStatus,
    storeName: String,
    onUpdateQuantity: (String, Int) -> Unit,
    onRemoveItem: (String) -> Unit,
    onPayNow: () -> Unit,
    onReset: () -> Unit,
    onBackToScan: () -> Unit,
    onOpenAuth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by appAuthManager.currentUser.collectAsState()
    val isLoggedIn by appAuthManager.isLoggedIn.collectAsState()

    var selectedPaymentMethod by remember { mutableStateOf("SCAN_AND_GO_PAY") }
    val previouslySeenCheckoutItemIds = remember { mutableSetOf<String>() }

    val totalCount = cart.sumOf { it.quantity }
    val totalWeight = cart.sumOf { it.product.weightGrams * it.quantity }
    val rawSubtotal = cart.sumOf { it.product.price * it.quantity }
    
    // Member loyalty discount (5% if authenticated)
    val discountPercent = if (isLoggedIn) (currentUser?.memberDiscountPercent ?: 5) else 0
    val discountAmount = if (discountPercent > 0) rawSubtotal * (discountPercent / 100.0) else 0.0
    val discountedSubtotal = rawSubtotal - discountAmount
    val estimatedTax = discountedSubtotal * 0.0825 // 8.25% sales tax
    val finalTotal = discountedSubtotal + estimatedTax

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (checkoutStatus) {
            is CheckoutStatus.Idle -> {
                if (cart.isEmpty()) {
                    // Empty Cart View
                    EmptyCheckoutView(onBackToScan = onBackToScan)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Header info
                        item {
                            CheckoutHeader(
                                storeName = storeName,
                                totalCount = totalCount,
                                maxItems = maxItems,
                                onBackToScan = onBackToScan
                            )
                        }

                        // User Login / Auth Card
                        item {
                            UserAuthBanner(
                                isLoggedIn = isLoggedIn,
                                currentUser = currentUser,
                                onOpenAuth = onOpenAuth
                            )
                        }

                        // Section Title: Scanned Items
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Scanned Items ($totalCount)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(
                                    onClick = onBackToScan,
                                    modifier = Modifier.testTag("scan_more_items_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Scan More", fontSize = 13.sp)
                                }
                            }
                        }

                        // List of Scanned Items with Steppers
                        items(cart, key = { it.product.id }) { item ->
                            val isRecentlyAdded = remember(item.product.id) {
                                !previouslySeenCheckoutItemIds.contains(item.product.id)
                            }
                            var hasEntered by remember { mutableStateOf(!isRecentlyAdded) }

                            LaunchedEffect(item.product.id) {
                                previouslySeenCheckoutItemIds.add(item.product.id)
                                if (!hasEntered) {
                                    hasEntered = true
                                }
                            }

                            AnimatedVisibility(
                                visible = hasEntered,
                                enter = slideInHorizontally(
                                    initialOffsetX = { fullWidth -> fullWidth / 2 },
                                    animationSpec = tween(durationMillis = 380)
                                ) + fadeIn(animationSpec = tween(durationMillis = 380)),
                                modifier = Modifier.animateItem()
                            ) {
                                ScannedItemCard(
                                    item = item,
                                    canIncrease = totalCount < maxItems,
                                    onIncrease = { onUpdateQuantity(item.product.id, 1) },
                                    onDecrease = { onUpdateQuantity(item.product.id, -1) },
                                    onRemove = { onRemoveItem(item.product.id) }
                                )
                            }
                        }

                        // Weight & Exit Sensor Scale Status Card
                        item {
                            WeightSensorStatusCard(totalWeight = totalWeight)
                        }

                        // Payment Breakdown Card
                        item {
                            PaymentBreakdownCard(
                                rawSubtotal = rawSubtotal,
                                discountPercent = discountPercent,
                                discountAmount = discountAmount,
                                estimatedTax = estimatedTax,
                                finalTotal = finalTotal
                            )
                        }

                        // Payment Method Selection
                        item {
                            PaymentMethodSelector(
                                selectedMethod = selectedPaymentMethod,
                                onSelect = { selectedPaymentMethod = it }
                            )
                        }
                    }

                    // Sticky Bottom Bar with 'Pay Now' Button
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(),
                        shadowElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "Total Amount Due",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        "$${String.format(Locale.US, "%.2f", finalTotal)}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Button(
                                    onClick = onPayNow,
                                    modifier = Modifier
                                        .height(52.dp)
                                        .testTag("pay_now_button")
                                        .testTag("checkout_button"),
                                    enabled = cart.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF059669)
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Pay Now",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            is CheckoutStatus.Polling -> {
                ExitGateVerifyingView(totalWeight = totalWeight)
            }

            is CheckoutStatus.Success -> {
                DigitalReceiptSuccessView(
                    cart = cart,
                    finalTotal = finalTotal,
                    storeName = storeName,
                    userEmail = currentUser?.email,
                    paymentMethod = selectedPaymentMethod,
                    onReset = onReset
                )
            }

            is CheckoutStatus.Blocked -> {
                GateBlockedView(
                    reason = checkoutStatus.reason,
                    onAcknowledge = onReset
                )
            }
        }
    }
}

@Composable
fun CheckoutHeader(
    storeName: String,
    totalCount: Int,
    maxItems: Int,
    onBackToScan: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Checkout Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = storeName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = "$totalCount / $maxItems Items",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun UserAuthBanner(
    isLoggedIn: Boolean,
    currentUser: UserProfile?,
    onOpenAuth: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenAuth() }
            .testTag("user_auth_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLoggedIn) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, if (isLoggedIn) Color(0xFF86EFAC) else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isLoggedIn) Color(0xFF10B981) else MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoggedIn && currentUser != null) {
                        Text(
                            currentUser.displayName.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column {
                    if (isLoggedIn && currentUser != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                currentUser.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFDCFCE7)
                            ) {
                                Text(
                                    currentUser.loyaltyTier,
                                    color = Color(0xFF16A34A),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            "Member savings active • ${currentUser.email}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        Text(
                            "Guest Shopper",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Sign in for member discount & digital receipts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Button(
                onClick = onOpenAuth,
                modifier = Modifier.testTag("checkout_user_auth_button").testTag("user_auth_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLoggedIn) Color(0xFFE2E8F0) else MaterialTheme.colorScheme.primary,
                    contentColor = if (isLoggedIn) Color(0xFF1E293B) else MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = if (isLoggedIn) "Account" else "User Login",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ScannedItemCard(
    item: CartItem,
    canIncrease: Boolean,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    val lineSubtotal = item.product.price * item.quantity

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("scanned_item_${item.product.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Category Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        getCategoryIcon(item.product.category),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                // Product Details
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.product.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (item.isAiVerified) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFDCFCE7)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    Text(
                                        "AI Verified",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF16A34A)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(3.dp))
                    Text(
                        "UPC: ${item.product.barcode} • ${item.product.weightGrams}g each",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        "$${String.format(Locale.US, "%.2f", item.product.price)} each",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Line Total
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "$${String.format(Locale.US, "%.2f", lineSubtotal)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("remove_item_${item.product.id}")
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Remove Item",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(Modifier.height(10.dp))

            // Inline Quantity Adjustment Stepper
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Weight: ${item.product.weightGrams * item.quantity}g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Decrement button
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { onDecrease() }
                            .testTag("decrease_qty_${item.product.id}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (item.quantity == 1) Icons.Default.Delete else Icons.Default.Remove,
                                contentDescription = "Decrease Quantity",
                                tint = if (item.quantity == 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Current Quantity
                    Text(
                        text = "${item.quantity}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .testTag("qty_${item.product.id}")
                    )

                    // Increment button
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (canIncrease) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .size(36.dp)
                            .clickable(enabled = canIncrease) { onIncrease() }
                            .testTag("increase_qty_${item.product.id}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Increase Quantity",
                                tint = if (canIncrease) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeightSensorStatusCard(totalWeight: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFFBBF7D0))
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDCFCE7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Total Bag Weight: ${totalWeight}g",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF166534)
                )
                Text(
                    text = "Exit turnstile scale calibrated within ±15g tolerance",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF15803D)
                )
            }
        }
    }
}

@Composable
fun PaymentBreakdownCard(
    rawSubtotal: Double,
    discountPercent: Int,
    discountAmount: Double,
    estimatedTax: Double,
    finalTotal: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Payment Breakdown",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Subtotal", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                Text("$${String.format(Locale.US, "%.2f", rawSubtotal)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }

            if (discountPercent > 0) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Member Loyalty Discount ($discountPercent%)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF16A34A)
                    )
                    Text(
                        "-$${String.format(Locale.US, "%.2f", discountAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Estimated Tax (8.25%)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                Text("$${String.format(Locale.US, "%.2f", estimatedTax)}", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Grand Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "$${String.format(Locale.US, "%.2f", finalTotal)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun PaymentMethodSelector(
    selectedMethod: String,
    onSelect: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Select Payment Method",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))

        PaymentOptionRow(
            title = "Scan & Go Instant Pay",
            subtitle = "Charges primary card ending in •••• 4242",
            icon = Icons.Default.CreditCard,
            isSelected = selectedMethod == "SCAN_AND_GO_PAY",
            onClick = { onSelect("SCAN_AND_GO_PAY") }
        )

        Spacer(Modifier.height(8.dp))

        PaymentOptionRow(
            title = "Google Pay",
            subtitle = "Fast, biometrically protected checkout",
            icon = Icons.Default.ShoppingBag,
            isSelected = selectedMethod == "GOOGLE_PAY",
            onClick = { onSelect("GOOGLE_PAY") }
        )
    }
}

@Composable
fun PaymentOptionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
fun EmptyCheckoutView(onBackToScan: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "Your Cart is Empty",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Scan barcodes or capture item photos using the camera scanner to populate your checkout summary.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onBackToScan,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Start Scanning Items")
            }
        }
    }
}

@Composable
fun ExitGateVerifyingView(totalWeight: Int) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                strokeWidth = 5.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "Verifying Exit Gate Scales...",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Calibrating ${totalWeight}g weight against visual AI YOLO verification",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(0.6f).height(6.dp).clip(RoundedCornerShape(3.dp))
            )
        }
    }
}

@Composable
fun DigitalReceiptSuccessView(
    cart: List<CartItem>,
    finalTotal: Double,
    storeName: String,
    userEmail: String?,
    paymentMethod: String,
    onReset: () -> Unit
) {
    val transactionId = remember { "TXN-${System.currentTimeMillis().toString().takeLast(6)}" }
    val timeStamp = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.US).format(Date()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDCFCE7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        item {
            Text(
                "Gate Open!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF15803D)
            )
            Text(
                "Payment successfully verified. Turnstile unlocked.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }

        // Digital Receipt Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Scan & Go Autonomous Market", fontWeight = FontWeight.Bold)
                        Text(transactionId, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    }
                    Text(storeName, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    Text(timeStamp, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(14.dp))

                    cart.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${item.quantity}x ${item.product.name}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "$${String.format(Locale.US, "%.2f", item.product.price * item.quantity)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Paid", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "$${String.format(Locale.US, "%.2f", finalTotal)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (userEmail != null) {
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF16A34A))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Digital receipt sent to $userEmail",
                                fontSize = 11.sp,
                                color = Color(0xFF16A34A)
                            )
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("new_trip_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Start New Shopping Session", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun GateBlockedView(
    reason: String,
    onAcknowledge: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "Blocked",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(44.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "Exit Gate Blocked",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(8.dp))
            Text(
                reason,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onAcknowledge,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Acknowledge & Return to Cart")
            }
        }
    }
}

fun getCategoryIcon(category: String): ImageVector {
    return when (category.lowercase()) {
        "produce", "fruits", "vegetables" -> Icons.Default.Fastfood
        "beverages", "drinks" -> Icons.Default.LocalDrink
        "snacks", "candy" -> Icons.Default.Fastfood
        "bakery", "bread" -> Icons.Default.Fastfood
        else -> Icons.Default.ShoppingBag
    }
}
