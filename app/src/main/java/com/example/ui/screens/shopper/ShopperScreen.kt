package com.example.ui.screens.shopper

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auth.appAuthManager
import com.example.data.CartItem
import com.example.data.Product
import com.example.data.Store
import com.example.ui.screens.InteractiveStoreMap
import com.example.ui.screens.auth.UserAuthDialog

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
    
    val isScannerOpen by viewModel.isScannerOpen.collectAsState()
    val scannerMode by viewModel.scannerMode.collectAsState()
    val isAnalyzingPhoto by viewModel.isAnalyzingPhoto.collectAsState()
    val aiVerificationResult by viewModel.aiVerificationResult.collectAsState()
    val scannedProduct by viewModel.scannedProduct.collectAsState()
    val scanMessage by viewModel.scanMessage.collectAsState()
    
    var selectedTabIndex by remember { mutableStateOf(0) }
    var isAuthDialogOpen by remember { mutableStateOf(false) }
    val currentUser by appAuthManager.currentUser.collectAsState()
    val isLoggedIn by appAuthManager.isLoggedIn.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(scanMessage) {
        scanMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearScanMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            selectedStore?.name ?: "Select a Store",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedStore != null) {
                            Text(
                                if (isLoggedIn && currentUser != null) "Hi, ${currentUser!!.displayName} • Member Perks Active" else "Scan items directly into your cart",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                },
                actions = {
                    // User Login / Auth Button
                    IconButton(
                        onClick = { isAuthDialogOpen = true },
                        modifier = Modifier.testTag("user_auth_button")
                    ) {
                        if (isLoggedIn && currentUser != null) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentUser!!.displayName.take(1).uppercase(),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = "User Login Auth",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    if (selectedStore != null) {
                        IconButton(
                            onClick = { viewModel.openScanner() },
                            modifier = Modifier.testTag("top_app_bar_scanner_button")
                        ) {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                contentDescription = "Scan Barcode",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            if (selectedStore != null && !isScannerOpen && selectedTabIndex != 1) {
                FloatingActionButton(
                    onClick = { viewModel.openScanner() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("floating_scan_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Scan Barcode", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (selectedStore == null) {
                StoreSelection(stores) { viewModel.selectStore(it) }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    TabRow(selectedTabIndex = selectedTabIndex) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            text = { 
                                val totalItems = cart.sumOf { it.quantity }
                                Text("Cart ($totalItems/$maxItems)") 
                            }
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Checkout Summary")
                                    if (cart.isNotEmpty()) {
                                        Spacer(Modifier.width(4.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        ) {
                                            Text(
                                                "${cart.sumOf { it.quantity }}",
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        )
                        Tab(
                            selected = selectedTabIndex == 2,
                            onClick = { selectedTabIndex = 2 },
                            text = { Text("Store Map") }
                        )
                    }
                    
                    when (selectedTabIndex) {
                        0 -> {
                            CartView(
                                cart = cart,
                                maxItems = maxItems,
                                checkoutStatus = checkoutStatus,
                                availableProducts = products,
                                onAddProduct = { product -> viewModel.addProductToCart(product) },
                                onScan = { viewModel.openScanner(ScannerMode.BARCODE) },
                                onScanAiPhoto = { viewModel.openScanner(ScannerMode.AI_PHOTO) },
                                onQuickMockScan = { viewModel.scanItemMock() },
                                onUpdateQuantity = { id, delta -> viewModel.updateCartQuantity(id, delta) },
                                onRemoveItem = { id -> viewModel.removeProductFromCart(id) },
                                onCheckout = { viewModel.checkout() },
                                onReset = { viewModel.resetCart() },
                                onOpenCheckoutSummary = { selectedTabIndex = 1 }
                            )
                        }
                        1 -> {
                            CheckoutSummaryScreen(
                                cart = cart,
                                maxItems = maxItems,
                                checkoutStatus = checkoutStatus,
                                storeName = selectedStore?.name ?: "Autonomous Store",
                                onUpdateQuantity = { id, delta -> viewModel.updateCartQuantity(id, delta) },
                                onRemoveItem = { id -> viewModel.removeProductFromCart(id) },
                                onPayNow = { viewModel.checkout() },
                                onReset = { viewModel.resetCart() },
                                onBackToScan = { selectedTabIndex = 0 },
                                onOpenAuth = { isAuthDialogOpen = true }
                            )
                        }
                        2 -> {
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
                                    ShopperProductDetailsDialog(
                                        product = selectedMapProduct!!,
                                        onClose = { viewModel.selectMapProduct(null) },
                                        onAddToCart = { quantity ->
                                            viewModel.addProductToCart(selectedMapProduct!!, quantity)
                                            viewModel.selectMapProduct(null)
                                        },
                                        onVerifyWithPhoto = {
                                            val target = selectedMapProduct!!
                                            viewModel.selectMapProduct(null)
                                            viewModel.openScanner(ScannerMode.AI_PHOTO)
                                            viewModel.processCapturedItemPhoto(target)
                                        },
                                        maxAllowed = (maxItems - cart.sumOf { it.quantity }).coerceAtLeast(0)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // CameraX Barcode Scanner & AI Photo Verification Fullscreen Modal
            if (isScannerOpen) {
                BarcodeScannerModal(
                    availableProducts = products,
                    currentMode = scannerMode,
                    isAnalyzingPhoto = isAnalyzingPhoto,
                    aiVerificationResult = aiVerificationResult,
                    onModeChanged = { mode -> viewModel.setScannerMode(mode) },
                    onBarcodeScanned = { barcode ->
                        viewModel.onBarcodeScanned(barcode)
                    },
                    onCapturePhoto = { targetProduct ->
                        viewModel.processCapturedItemPhoto(targetProduct)
                    },
                    onConfirmAiVerifiedProduct = { verifiedProduct ->
                        viewModel.addVerifiedProductToCart(verifiedProduct, 1)
                    },
                    onDismissAiVerification = {
                        viewModel.dismissAiVerification()
                    },
                    onClose = {
                        viewModel.closeScanner()
                    }
                )
            }

            // Scanned Product Details Dialog
            if (scannedProduct != null) {
                ShopperProductDetailsDialog(
                    product = scannedProduct!!,
                    onClose = { viewModel.selectScannedProduct(null) },
                    onAddToCart = { quantity ->
                        viewModel.addProductToCart(scannedProduct!!, quantity)
                        viewModel.selectScannedProduct(null)
                    },
                    onVerifyWithPhoto = {
                        val target = scannedProduct!!
                        viewModel.selectScannedProduct(null)
                        viewModel.openScanner(ScannerMode.AI_PHOTO)
                        viewModel.processCapturedItemPhoto(target)
                    },
                    maxAllowed = (maxItems - cart.sumOf { it.quantity }).coerceAtLeast(0)
                )
            }

            // User Login & Authentication Dialog
            if (isAuthDialogOpen) {
                UserAuthDialog(onDismiss = { isAuthDialogOpen = false })
            }
        }
    }
}

@Composable
fun ShopperProductDetailsDialog(
    product: Product,
    onClose: () -> Unit,
    onAddToCart: (Int) -> Unit,
    maxAllowed: Int,
    onVerifyWithPhoto: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var quantity by remember { mutableIntStateOf(1) }

    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .testTag("product_details_dialog"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(24.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                // Top Header with Category and Barcode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = if (product.category.isNotEmpty()) product.category else "Item",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "UPC: ${product.barcode}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(Modifier.height(6.dp))
                val tierName = when {
                    product.shelfZ >= 90f -> "Top Tier Shelf"
                    product.shelfZ >= 50f -> "Middle Tier Shelf"
                    else -> "Bottom Tier Shelf"
                }
                Text(
                    text = "📍 Shelf: $tierName (Bay ${(product.shelfX / 80).toInt() + 1})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "⚖️ Verified Unit Weight: ${product.weightGrams}g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(Modifier.height(14.dp))

                // Price and Stock Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Price", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$${product.price}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (product.discount > 0) {
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFDC2626)
                                ) {
                                    Text(
                                        text = "-$${product.discount} OFF",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Inventory", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        if (product.stockQuantity <= 0) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFEE2E2)
                            ) {
                                Text(
                                    "Out of Stock",
                                    color = Color(0xFFDC2626),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFDCFCE7)
                            ) {
                                Text(
                                    "${product.stockQuantity} in stock",
                                    color = Color(0xFF16A34A),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Expiry: ${product.expiryDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(16.dp))

                // Quantity Selector (if in stock)
                if (product.stockQuantity > 0 && maxAllowed > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Quantity to Add:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (quantity > 1) quantity-- },
                                    enabled = quantity > 1,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease")
                                }
                                Text(
                                    text = "$quantity",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                                IconButton(
                                    onClick = { if (quantity < maxAllowed && quantity < product.stockQuantity) quantity++ },
                                    enabled = quantity < maxAllowed && quantity < product.stockQuantity,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase")
                                }
                            }
                        }
                    }
                } else if (maxAllowed <= 0) {
                    Text(
                        "Cart limit reached. Remove items or checkout.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(Modifier.height(16.dp))

                if (onVerifyWithPhoto != null) {
                    OutlinedButton(
                        onClick = onVerifyWithPhoto,
                        modifier = Modifier.fillMaxWidth().testTag("dialog_verify_with_photo_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Capture Photo for AI Verification")
                    }
                    Spacer(Modifier.height(10.dp))
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onClose,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Dismiss")
                    }

                    val canAdd = product.stockQuantity > 0 && maxAllowed > 0
                    Button(
                        onClick = { onAddToCart(quantity) },
                        enabled = canAdd,
                        modifier = Modifier.weight(1.8f).testTag("dialog_add_to_cart_button")
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        val totalPriceStr = String.format("%.2f", product.price * quantity)
                        Text("Add to Cart ($$totalPriceStr)")
                    }
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
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        items(stores) { store ->
            Card(
                onClick = { onSelect(store) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(store.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(store.address, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
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
    availableProducts: List<Product> = emptyList(),
    onAddProduct: (Product) -> Unit = {},
    onScan: () -> Unit,
    onScanAiPhoto: () -> Unit = {},
    onQuickMockScan: () -> Unit,
    onUpdateQuantity: (String, Int) -> Unit,
    onRemoveItem: (String) -> Unit,
    onCheckout: () -> Unit,
    onReset: () -> Unit,
    onOpenCheckoutSummary: () -> Unit = {}
) {
    val totalCount = cart.sumOf { it.quantity }
    val totalPrice = cart.sumOf { it.product.price * it.quantity }
    val totalWeight = cart.sumOf { it.product.weightGrams * it.quantity }
    var searchQuery by remember { mutableStateOf("") }
    var itemAddedBannerMessage by remember { mutableStateOf<String?>(null) }
    val previouslySeenItemIds = remember { mutableSetOf<String>() }

    LaunchedEffect(itemAddedBannerMessage) {
        if (itemAddedBannerMessage != null) {
            kotlinx.coroutines.delay(2200)
            itemAddedBannerMessage = null
        }
    }

    val filteredProducts = remember(searchQuery, availableProducts) {
        val query = searchQuery.trim().lowercase()
        if (query.isEmpty()) {
            emptyList()
        } else {
            availableProducts.filter { product ->
                product.name.lowercase().contains(query) ||
                product.category.lowercase().contains(query) ||
                product.barcode.lowercase().contains(query) ||
                product.id.lowercase().contains(query)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        when (checkoutStatus) {
            is CheckoutStatus.Idle -> {
                // Top Action Bar with Scan Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "My Cart",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "$totalCount / $maxItems items (${maxItems - totalCount} remaining)",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (totalCount >= maxItems) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = onScanAiPhoto,
                            enabled = totalCount < maxItems,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("ai_photo_button")
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "AI Photo", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("AI Photo", fontSize = 12.sp)
                        }

                        Button(
                            onClick = onScan,
                            enabled = totalCount < maxItems,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("scan_button")
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Barcode", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar to manually add items if scan fails
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cart_search_input"),
                    placeholder = {
                        Text("Barcode scan failed? Search item name, code...", fontSize = 13.sp)
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search items",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.testTag("clear_cart_search_button")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                AnimatedVisibility(visible = itemAddedBannerMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag("cart_item_added_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                itemAddedBannerMessage ?: "",
                                color = Color(0xFF047857),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (searchQuery.trim().isNotEmpty()) {
                    // Search results pane for manual item addition
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("cart_search_results_container")
                    ) {
                        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Manual Catalog Results (${filteredProducts.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Tap to Add",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            if (filteredProducts.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.SearchOff,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            "No products matching \"$searchQuery\"",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.outline,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(filteredProducts, key = { "search_${it.id}" }) { product ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("search_product_${product.id}"),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface
                                            ),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        product.name,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    )
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            "$${String.format("%.2f", product.price)}",
                                                            color = MaterialTheme.colorScheme.primary,
                                                            fontWeight = FontWeight.SemiBold,
                                                            fontSize = 13.sp
                                                        )
                                                        Text("•", color = MaterialTheme.colorScheme.outline)
                                                        Text(
                                                            "${product.weightGrams}g",
                                                            color = MaterialTheme.colorScheme.outline,
                                                            fontSize = 12.sp
                                                        )
                                                        Text("•", color = MaterialTheme.colorScheme.outline)
                                                        Text(
                                                            product.category,
                                                            color = MaterialTheme.colorScheme.secondary,
                                                            fontSize = 12.sp
                                                        )
                                                    }
                                                    Text(
                                                        "Barcode: ${product.barcode}",
                                                        color = MaterialTheme.colorScheme.outline,
                                                        fontSize = 11.sp
                                                    )
                                                }

                                                Button(
                                                    onClick = {
                                                        onAddProduct(product)
                                                        itemAddedBannerMessage = "Added ${product.name} to cart"
                                                    },
                                                    enabled = totalCount < maxItems,
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                    modifier = Modifier.testTag("add_search_product_${product.id}")
                                                ) {
                                                    Icon(
                                                        Icons.Default.Add,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("Add", fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (cart.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Your cart is empty",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Use your camera to scan barcodes or capture item photos for instant visual AI verification.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(Modifier.height(20.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = onScan,
                                    modifier = Modifier.testTag("empty_cart_scan_button")
                                ) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Scan Barcode")
                                }

                                Button(
                                    onClick = onScanAiPhoto,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    modifier = Modifier.testTag("empty_cart_ai_photo_button")
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("AI Photo Verify")
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(cart, key = { it.product.id }) { item ->
                            val isRecentlyAdded = remember(item.product.id) {
                                !previouslySeenItemIds.contains(item.product.id)
                            }
                            var hasEntered by remember { mutableStateOf(!isRecentlyAdded) }

                            LaunchedEffect(item.product.id) {
                                previouslySeenItemIds.add(item.product.id)
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
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(item.product.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                                if (item.isAiVerified) {
                                                    Spacer(Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
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
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                "UPC: ${item.product.barcode} • ${item.product.weightGrams}g each",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                            Text(
                                                "$${item.product.price} each = $${String.format("%.2f", item.product.price * item.quantity)}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        // Quantity Controls
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { onUpdateQuantity(item.product.id, -1) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(18.dp))
                                            }

                                            Text(
                                                "${item.quantity}",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleMedium,
                                                modifier = Modifier.padding(horizontal = 8.dp)
                                            )

                                            IconButton(
                                                onClick = { onUpdateQuantity(item.product.id, 1) },
                                                enabled = totalCount < maxItems,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(18.dp))
                                            }

                                            IconButton(
                                                onClick = { onRemoveItem(item.product.id) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Verified Weight:", style = MaterialTheme.typography.bodyMedium)
                            Text("${totalWeight}g", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total Price:", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                "$${String.format("%.2f", totalPrice)}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenCheckoutSummary,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("review_checkout_summary_button"),
                        enabled = cart.isNotEmpty(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Checkout Summary", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = onCheckout,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("checkout_button")
                            .testTag("pay_now_button"),
                        enabled = cart.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Pay Now", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
            is CheckoutStatus.Polling -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Verifying Exit Gate Scales...", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Checking ${totalWeight}g load-cell measurement against visual YOLO inference",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
            is CheckoutStatus.Success -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(72.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Gate Open!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text("Digital Receipt generated. Thank you for shopping!")
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = onReset, shape = RoundedCornerShape(16.dp)) { 
                            Text("New Session", modifier = Modifier.padding(horizontal = 16.dp)) 
                        }
                    }
                }
            }
            is CheckoutStatus.Blocked -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Warning, contentDescription = "Blocked", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(72.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Gate Blocked", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            (checkoutStatus as CheckoutStatus.Blocked).reason,
                            modifier = Modifier.padding(horizontal = 24.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = onReset, shape = RoundedCornerShape(16.dp)) { 
                            Text("Acknowledge & Cancel") 
                        }
                    }
                }
            }
        }
    }
}

