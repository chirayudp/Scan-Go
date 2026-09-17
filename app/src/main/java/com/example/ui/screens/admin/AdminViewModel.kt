package com.example.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiClient
import com.example.data.Product
import com.example.data.appRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminViewModel : ViewModel() {
    private val _products = appRepository.products
    val products = _products

    private val _shelves = appRepository.shelves
    val shelves = _shelves

    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct = _selectedProduct.asStateFlow()
    
    private val _storeInfo = MutableStateFlow<String>("")
    val storeInfo = _storeInfo.asStateFlow()

    init {
        viewModelScope.launch {
            appRepository.fetchProducts("store_001")
        }
    }

    fun selectProduct(product: Product?) {
        _selectedProduct.value = product
    }

    fun updateProductLocation(productId: String, newX: Float, newY: Float, newZ: Float = 0f) {
        val product = _products.value.find { it.id == productId } ?: return
        val updated = product.copy(shelfX = newX, shelfY = newY, shelfZ = newZ)
        viewModelScope.launch {
            appRepository.saveProduct(updated)
        }
    }

    fun saveProductDetails(updatedProduct: Product) {
        viewModelScope.launch {
            appRepository.saveProduct(updatedProduct)
            _selectedProduct.value = null
        }
    }
    
    fun searchStoreGrounding(query: String) {
        viewModelScope.launch {
            _storeInfo.value = "Searching Google..."
            val result = GeminiClient.searchStoreInfo(query)
            _storeInfo.value = result
        }
    }
}
