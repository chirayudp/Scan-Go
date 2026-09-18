package com.example.ui.screens.shopper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CartItem
import com.example.data.Product
import com.example.data.Store
import com.example.data.appRepository
import com.example.util.SoundFeedback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ShopperViewModel : ViewModel() {
    private val _stores = MutableStateFlow(appRepository.mockStores)
    val stores = _stores.asStateFlow()

    private val _selectedStore = MutableStateFlow<Store?>(null)
    val selectedStore = _selectedStore.asStateFlow()
    
    val products = appRepository.products
    val shelves = appRepository.shelves

    private val _selectedMapProduct = MutableStateFlow<Product?>(null)
    val selectedMapProduct = _selectedMapProduct.asStateFlow()

    fun selectMapProduct(product: Product?) {
        _selectedMapProduct.value = product
    }

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart = _cart.asStateFlow()

    private val _isScannerOpen = MutableStateFlow(false)
    val isScannerOpen = _isScannerOpen.asStateFlow()

    private val _scannerMode = MutableStateFlow(ScannerMode.BARCODE)
    val scannerMode = _scannerMode.asStateFlow()

    private val _scannedProduct = MutableStateFlow<Product?>(null)
    val scannedProduct = _scannedProduct.asStateFlow()

    private val _aiVerificationResult = MutableStateFlow<com.example.data.AiVerificationResult?>(null)
    val aiVerificationResult = _aiVerificationResult.asStateFlow()

    private val _isAnalyzingPhoto = MutableStateFlow(false)
    val isAnalyzingPhoto = _isAnalyzingPhoto.asStateFlow()

    private val _scanMessage = MutableStateFlow<String?>(null)
    val scanMessage = _scanMessage.asStateFlow()

    private val _checkoutStatus = MutableStateFlow<CheckoutStatus>(CheckoutStatus.Idle)
    val checkoutStatus = _checkoutStatus.asStateFlow()

    val maxItems = MutableStateFlow(15) // Dynamic cap
    
    // Mock user trust score, dynamically scaling the cap
    val trustScore = MutableStateFlow(80) 

    init {
        // Adjust cap based on trust score
        if (trustScore.value > 90) maxItems.value = 20
        else if (trustScore.value < 50) maxItems.value = 5
    }

    fun openScanner(mode: ScannerMode = ScannerMode.BARCODE) {
        _scannerMode.value = mode
        _isScannerOpen.value = true
        _scanMessage.value = null
    }

    fun setScannerMode(mode: ScannerMode) {
        _scannerMode.value = mode
    }

    fun closeScanner() {
        _isScannerOpen.value = false
        _aiVerificationResult.value = null
    }

    fun selectScannedProduct(product: Product?) {
        _scannedProduct.value = product
    }

    fun dismissAiVerification() {
        _aiVerificationResult.value = null
    }

    fun processCapturedItemPhoto(targetProduct: Product? = null, simulateDelay: Boolean = true) {
        val candidate = targetProduct 
            ?: _scannedProduct.value 
            ?: appRepository.products.value.randomOrNull()

        if (!simulateDelay) {
            if (candidate != null) {
                val confidence = 0.95f + (Math.random().toFloat() * 0.048f)
                _aiVerificationResult.value = com.example.data.AiVerificationResult(
                    product = candidate,
                    confidence = confidence,
                    detectedYoloClass = candidate.yoloClass.ifEmpty { "packaged_good" },
                    inferenceTimeMs = (115..185).random().toLong(),
                    isMatch = true,
                    notes = "Edge visual model matched YOLO class '${candidate.yoloClass}' with scale target weight ${candidate.weightGrams}g",
                    photoDescription = "Captured frame: High-confidence ${candidate.name}"
                )
            } else {
                _scanMessage.value = "No item recognized in photo"
            }
            return
        }

        viewModelScope.launch {
            _isAnalyzingPhoto.value = true
            // Simulate edge AI visual inference (YOLOv8 + weight verification)
            kotlinx.coroutines.delay(600)
            if (candidate != null) {
                val confidence = 0.95f + (Math.random().toFloat() * 0.048f)
                _aiVerificationResult.value = com.example.data.AiVerificationResult(
                    product = candidate,
                    confidence = confidence,
                    detectedYoloClass = candidate.yoloClass.ifEmpty { "packaged_good" },
                    inferenceTimeMs = (115..185).random().toLong(),
                    isMatch = true,
                    notes = "Edge visual model matched YOLO class '${candidate.yoloClass}' with scale target weight ${candidate.weightGrams}g",
                    photoDescription = "Captured frame: High-confidence ${candidate.name}"
                )
            } else {
                _scanMessage.value = "No item recognized in photo"
            }
            _isAnalyzingPhoto.value = false
        }
    }

    fun addVerifiedProductToCart(product: Product, quantityToAdd: Int = 1) {
        val currentCartCount = _cart.value.sumOf { it.quantity }
        val allowedQuantity = (maxItems.value - currentCartCount).coerceAtLeast(0)
        val actualAdd = quantityToAdd.coerceAtMost(allowedQuantity)
        if (actualAdd <= 0) return

        val existing = _cart.value.find { it.product.id == product.id }
        if (existing != null) {
            _cart.value = _cart.value.map { 
                if (it.product.id == product.id) it.copy(quantity = it.quantity + actualAdd, isAiVerified = true) else it 
            }
        } else {
            _cart.value = _cart.value + CartItem(product, actualAdd, isAiVerified = true)
        }
        _aiVerificationResult.value = null
        _scanMessage.value = "AI Verified & Added: ${product.name}"
    }

    fun clearScanMessage() {
        _scanMessage.value = null
    }

    fun onBarcodeScanned(barcode: String): Product? {
        val cleanBarcode = barcode.trim()
        if (cleanBarcode.isEmpty()) return null

        val allProducts = appRepository.products.value
        val product = allProducts.find { it.barcode.equals(cleanBarcode, ignoreCase = true) }
            ?: allProducts.find { it.id.equals(cleanBarcode, ignoreCase = true) }

        if (product != null) {
            _scannedProduct.value = product
            _scanMessage.value = "Identified: ${product.name}"
            SoundFeedback.playBarcodeScanBeep()
        } else {
            _scanMessage.value = "Barcode $cleanBarcode not found in catalog"
        }
        return product
    }

    fun selectStore(store: Store) {
        _selectedStore.value = store
        viewModelScope.launch {
            appRepository.fetchProducts(store.id)
        }
    }

    fun scanItemMock() {
        val store = _selectedStore.value ?: return
        val currentCartCount = _cart.value.sumOf { it.quantity }
        if (currentCartCount >= maxItems.value) return

        val products = appRepository.products.value
        if (products.isEmpty()) return

        // Pick a product to simulate scanning and show its details
        val product = products.random()
        _scannedProduct.value = product
    }

    fun addProductToCart(product: Product, quantityToAdd: Int = 1) {
        val currentCartCount = _cart.value.sumOf { it.quantity }
        val allowedQuantity = (maxItems.value - currentCartCount).coerceAtLeast(0)
        val actualAdd = quantityToAdd.coerceAtMost(allowedQuantity)
        if (actualAdd <= 0) return

        val existing = _cart.value.find { it.product.id == product.id }
        if (existing != null) {
            _cart.value = _cart.value.map { 
                if (it.product.id == product.id) it.copy(quantity = it.quantity + actualAdd) else it 
            }
        } else {
            _cart.value = _cart.value + CartItem(product, actualAdd)
        }
    }

    fun updateCartQuantity(productId: String, delta: Int) {
        val current = _cart.value.find { it.product.id == productId } ?: return
        val newQuantity = current.quantity + delta
        if (newQuantity <= 0) {
            _cart.value = _cart.value.filter { it.product.id != productId }
        } else {
            val totalOthers = _cart.value.filter { it.product.id != productId }.sumOf { it.quantity }
            if (totalOthers + newQuantity > maxItems.value) return // Cart cap reached
            _cart.value = _cart.value.map {
                if (it.product.id == productId) it.copy(quantity = newQuantity) else it
            }
        }
    }

    fun removeProductFromCart(productId: String) {
        _cart.value = _cart.value.filter { it.product.id != productId }
    }

    fun checkout() {
        _checkoutStatus.value = CheckoutStatus.Polling
        // Trigger simulated verify-exit edge gate
        viewModelScope.launch {
            // Simulate inference latency
            kotlinx.coroutines.delay(200)
            
            // If all items are AI-verified with photo, 100% verified pass rate
            val allAiVerified = _cart.value.isNotEmpty() && _cart.value.all { it.isAiVerified }
            val isAnomaly = if (allAiVerified) false else Math.random() < 0.1
            
            val totalExpectedWeight = _cart.value.sumOf { it.product.weightGrams * it.quantity }
            
            if (isAnomaly) {
                // Generate anomaly event
                val anomaly = com.example.data.AnomalyEvent(
                    storeId = _selectedStore.value?.id ?: "",
                    expectedWeightGrams = totalExpectedWeight,
                    actualWeightGrams = totalExpectedWeight + 150, // Mock discrepancy
                    discrepancyReason = "Visual Discrepancy: Unscanned item detected",
                    inferenceTimeMs = 185,
                    status = "FLAGGED"
                )
                appRepository.reportAnomaly(anomaly)
                _checkoutStatus.value = CheckoutStatus.Blocked("Gate Blocked: Anomaly Detected. Awaiting Guard.")
            } else {
                _checkoutStatus.value = CheckoutStatus.Success
            }
        }
    }
    
    fun resetCart() {
        _cart.value = emptyList()
        _checkoutStatus.value = CheckoutStatus.Idle
    }
}

enum class ScannerMode {
    BARCODE,
    AI_PHOTO
}

sealed class CheckoutStatus {
    object Idle : CheckoutStatus()
    object Polling : CheckoutStatus()
    object Success : CheckoutStatus()
    data class Blocked(val reason: String) : CheckoutStatus()
}
