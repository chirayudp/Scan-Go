package com.example.ui.screens.shopper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CartItem
import com.example.data.Product
import com.example.data.Store
import com.example.data.appRepository
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

        // Pick a random product to simulate scanning
        val product = products.random()
        
        val existing = _cart.value.find { it.product.id == product.id }
        if (existing != null) {
            _cart.value = _cart.value.map { 
                if (it.product.id == product.id) it.copy(quantity = it.quantity + 1) else it 
            }
        } else {
            _cart.value = _cart.value + CartItem(product, 1)
        }
    }

    fun addProductToCart(product: Product) {
        val currentCartCount = _cart.value.sumOf { it.quantity }
        if (currentCartCount >= maxItems.value) return
        val existing = _cart.value.find { it.product.id == product.id }
        if (existing != null) {
            _cart.value = _cart.value.map { 
                if (it.product.id == product.id) it.copy(quantity = it.quantity + 1) else it 
            }
        } else {
            _cart.value = _cart.value + CartItem(product, 1)
        }
    }

    fun checkout() {
        _checkoutStatus.value = CheckoutStatus.Polling
        // Trigger simulated verify-exit edge gate
        viewModelScope.launch {
            // Simulate inference latency
            kotlinx.coroutines.delay(200)
            
            // Randomly pass or block (90% pass, 10% anomaly)
            val isAnomaly = Math.random() < 0.1
            
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

sealed class CheckoutStatus {
    object Idle : CheckoutStatus()
    object Polling : CheckoutStatus()
    object Success : CheckoutStatus()
    data class Blocked(val reason: String) : CheckoutStatus()
}
