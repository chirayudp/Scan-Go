package com.example.ui.screens.guard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AnomalyEvent
import com.example.data.Product
import com.example.data.appRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GuardViewModel : ViewModel() {
    val anomalies = appRepository.anomalies
    val products = appRepository.products
    val shelves = appRepository.shelves

    private val _selectedAnomaly = MutableStateFlow<AnomalyEvent?>(null)
    val selectedAnomaly = _selectedAnomaly.asStateFlow()
    
    private val _selectedMapProduct = MutableStateFlow<Product?>(null)
    val selectedMapProduct = _selectedMapProduct.asStateFlow()

    init {
        // In a real app, you'd select the store the guard is assigned to
        appRepository.startListeningToAnomalies("store_001")
        viewModelScope.launch {
            appRepository.fetchProducts("store_001")
        }
    }

    fun selectAnomaly(anomaly: AnomalyEvent) {
        _selectedAnomaly.value = anomaly
    }

    fun selectMapProduct(product: Product?) {
        _selectedMapProduct.value = product
    }

    fun approveOverride() {
        val anomaly = _selectedAnomaly.value ?: return
        viewModelScope.launch {
            appRepository.updateAnomalyStatus(anomaly.storeId, anomaly.id, "APPROVED")
            _selectedAnomaly.value = null
        }
    }
}
