package com.example.ui.screens.shopper

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.CartItem
import com.example.data.Store
import com.example.ui.screens.InteractiveStoreMap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopperScreen(viewModel: ShopperViewModel = viewModel()) {
    val selectedStore by viewModel.selectedStore.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val checkoutStatus by viewModel.checkoutStatus.collectAsState()
    val maxItems by viewModel.maxItems.collectAsState()
    
    val products by viewModel.products.collectAsState(initial = emptyList())
    val shelves by viewModel.shelves.collectAsState(initial = emptyList())
    val selectedMapProduct by viewModel.selectedMapProduct.collectAsState()
    
    var selectedTabIndex by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedStore?.name ?: "Select a Store") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (selectedStore == null) {
                StoreSelection(stores) { viewModel.selectStore(it) }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    TabRow(selectedTabIndex = selectedTabIndex) {
                        Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }, text = { Text("Checkout") })
                        Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }, text = { Text("Store Map") })
                    }
                    
                    if (selectedTabIndex == 0) {
                        CartView(
                            cart = cart,
                            maxItems = maxItems,
                            checkoutStatus = checkoutStatus,
                            onScan = { viewModel.scanItemMock() },
                            onCheckout = { viewModel.checkout() },
                            onReset = { viewModel.resetCart() }
                        )
                    } else {
                        // Store Map View
                        Box(modifier = Modifier.fillMaxSize()) {
                            InteractiveStoreMap(
                                products = products,
                                shelves = shelves,
                                selectedProduct = selectedMapProduct,
                                isEditable = false,
                                onProductSelect = { id -> 
                                    viewModel.selectMapProduct(products.find { it.id == id })
                                },
                                onClearSelection = { viewModel.selectMapProduct(null) }
                            )
                            
                            if (selectedMapProduct != null) {
                                ShopperProductDetailsCard(
                                    product = selectedMapProduct!!,
                                    onClose = { viewModel.selectMapProduct(null) },
                                    onAddToCart = {
                                        viewModel.addProductToCart(selectedMapProduct!!)
                                    },
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShopperProductDetailsCard(
    product: com.example.data.Product,
    onClose: () -> Unit,
    onAddToCart: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(330.dp).padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(16.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = if (product.category.isNotEmpty()) product.category else "Aisle Item",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(product.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            
            Spacer(Modifier.height(4.dp))
            val tierName = when {
                product.shelfZ >= 90f -> "Top Tier Shelf"
                product.shelfZ >= 50f -> "Middle Tier Shelf"
                else -> "Bottom Tier Shelf"
            }
            Text(
                text = "📍 3D Location: $tierName (Bay ${(product.shelfX / 80).toInt() + 1})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Unit Price", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$${product.price}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (product.discount > 0) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                                color = Color(0xFFDC2626)
                            ) {
                                Text(
                                    text = "-$${product.discount}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Inventory Status", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    if (product.stockQuantity <= 0) {
                        Text("Out of Stock", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    } else {
                        Text("${product.stockQuantity} in stock", color = Color(0xFF16A34A), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("Expires: ${product.expiryDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Back")
                }
                Button(
                    onClick = {
                        onAddToCart()
                        onClose()
                    },
                    enabled = product.stockQuantity > 0,
                    modifier = Modifier.weight(1.5f)
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add to Cart")
                }
            }
        }
    }
}

@Composable
fun StoreSelection(stores: List<Store>, onSelect: (Store) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item {
            Text(
                "Nearby Stores",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        items(stores) { store ->
            Card(
                onClick = { onSelect(store) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(store.name, style = MaterialTheme.typography.titleLarge)
                    Text(store.address, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun CartView(
    cart: List<CartItem>,
    maxItems: Int,
    checkoutStatus: CheckoutStatus,
    onScan: () -> Unit,
    onCheckout: () -> Unit,
    onReset: () -> Unit
) {
    val totalCount = cart.sumOf { it.quantity }
    val totalPrice = cart.sumOf { it.product.price * it.quantity }
    val totalWeight = cart.sumOf { it.product.weightGrams * it.quantity }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        when (checkoutStatus) {
            is CheckoutStatus.Idle -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cart: $totalCount / $maxItems items", style = MaterialTheme.typography.titleMedium)
                    Button(
                        onClick = onScan,
                        enabled = totalCount < maxItems,
                        modifier = Modifier.testTag("scan_button")
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Scan")
                        Spacer(Modifier.width(8.dp))
                        Text("Mock Scan")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(cart) { item ->
                        ListItem(
                            headlineContent = { Text(item.product.name) },
                            supportingContent = { Text("${item.product.weightGrams}g") },
                            trailingContent = { Text("${item.quantity} x $${item.product.price}") }
                        )
                        HorizontalDivider()
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Total Weight: ${totalWeight}g", style = MaterialTheme.typography.bodyLarge)
                        Text("Total Price: $$totalPrice", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onCheckout,
                    modifier = Modifier.fillMaxWidth().height(56.dp).testTag("checkout_button"),
                    enabled = cart.isNotEmpty()
                ) {
                    Text("Pay & Open Gate")
                }
            }
            is CheckoutStatus.Polling -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("Verifying Exit Gate...")
                    }
                }
            }
            is CheckoutStatus.Success -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(72.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Gate Open!", style = MaterialTheme.typography.headlineMedium)
                        Text("Digital Receipt generated.")
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = onReset) { Text("Done") }
                    }
                }
            }
            is CheckoutStatus.Blocked -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Warning, contentDescription = "Blocked", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(72.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Gate Blocked", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.error)
                        Text((checkoutStatus as CheckoutStatus.Blocked).reason, modifier = Modifier.padding(16.dp))
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = onReset) { Text("Cancel") }
                    }
                }
            }
        }
    }
}
