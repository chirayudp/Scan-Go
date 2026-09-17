package com.example.data

import android.util.Log
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

class Repository {
    private val db = Firebase.firestore
    
    // Mock Stores
    val mockStores = listOf(
        Store("store_001", "FreshMart Central", "123 Main St", 37.7749, -122.4194, 15),
        Store("store_002", "QuickStop Local", "456 Oak St", 37.7849, -122.4094, 5)
    )

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products

    private val _shelves = MutableStateFlow<List<Shelf>>(emptyList())
    val shelves: StateFlow<List<Shelf>> = _shelves

    private val _anomalies = MutableStateFlow<List<AnomalyEvent>>(emptyList())
    val anomalies: StateFlow<List<AnomalyEvent>> = _anomalies

    init {
        // Fallback mock data in case Firestore is empty or fails
        _shelves.value = listOf(
            Shelf("s1", "store_001", "Produce & Fresh", 80f, 100f, 300f, 120f, depth = 120f, shelfHeight = 140f, aisleNumber = 1, category = "Produce", tiers = 3),
            Shelf("s2", "store_001", "Beverages & Drinks", 80f, 280f, 300f, 120f, depth = 120f, shelfHeight = 140f, aisleNumber = 2, category = "Beverages", tiers = 3),
            Shelf("s3", "store_001", "Bakery & Deli", 450f, 100f, 260f, 120f, depth = 120f, shelfHeight = 140f, aisleNumber = 3, category = "Bakery", tiers = 3),
            Shelf("s4", "store_001", "Snacks & Treats", 450f, 280f, 260f, 120f, depth = 120f, shelfHeight = 140f, aisleNumber = 4, category = "Snacks", tiers = 3)
        )
        
        _products.value = listOf(
            Product("p1", "store_001", "Organic Banana", "12345", 0.99, 120, "banana", 100f, 120f, 20f, 45, false, 0.0, "2026-10-01", "Produce"),
            Product("p2", "store_001", "Hass Avocados", "12346", 2.49, 180, "avocado", 180f, 120f, 60f, 25, true, 0.50, "2026-09-20", "Produce"),
            Product("p3", "store_001", "Honeycrisp Apples", "12347", 1.99, 150, "apple", 260f, 120f, 100f, 0, false, 0.0, "2026-09-30", "Produce"), // Out of stock (Red)
            Product("p4", "store_001", "Classic Coke Can", "67890", 1.50, 390, "coke_can", 100f, 300f, 20f, 0, false, 0.0, "2027-12-31", "Beverages"), // Out of stock (Red)
            Product("p5", "store_001", "Spring Water 6-Pk", "67891", 3.99, 1200, "water", 180f, 300f, 60f, 50, false, 0.0, "2028-01-01", "Beverages"),
            Product("p6", "store_001", "Energy Drink Bolt", "12222", 2.99, 350, "energy_drink", 260f, 300f, 100f, 40, true, 0.75, "2027-06-15", "Beverages"), // New & Discount (Green)
            Product("p7", "store_001", "Sourdough Loaf", "11121", 4.50, 450, "bread", 470f, 120f, 20f, 15, true, 0.50, "2026-09-12", "Bakery"), // New & Discount (Green)
            Product("p8", "store_001", "Butter Croissant", "11122", 2.75, 85, "pastry", 560f, 120f, 60f, 0, false, 0.0, "2026-09-08", "Bakery"), // Out of stock (Red)
            Product("p9", "store_001", "Sea Salt Chips", "33301", 3.49, 180, "chips", 470f, 300f, 20f, 35, false, 0.40, "2027-03-01", "Snacks"),
            Product("p10", "store_001", "Dark Chocolate 70%", "33302", 2.99, 100, "chocolate", 560f, 300f, 60f, 28, true, 0.0, "2027-11-20", "Snacks") // New (Green)
        )
    }

    fun startListeningToAnomalies(storeId: String) {
        try {
            db.collection("stores").document(storeId).collection("anomalies")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.w("Repository", "Listen failed.", e)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { it.toObject(AnomalyEvent::class.java) }
                        _anomalies.value = list
                    }
                }
        } catch (e: Exception) {
            Log.e("Repository", "Firestore error", e)
        }
    }

    suspend fun saveProduct(product: Product) {
        try {
            val docRef = db.collection("stores").document(product.storeId)
                .collection("products").document(product.id.ifEmpty { java.util.UUID.randomUUID().toString() })
            docRef.set(product.copy(id = docRef.id)).await()
            fetchProducts(product.storeId) // Refresh
        } catch (e: Exception) {
            Log.e("Repository", "Failed to save product", e)
            // Local fallback
            _products.value = _products.value.filter { it.id != product.id } + product
        }
    }

    suspend fun fetchProducts(storeId: String) {
        try {
            val result = db.collection("stores").document(storeId).collection("products").get().await()
            val list = result.documents.mapNotNull { it.toObject(Product::class.java) }
            if (list.isNotEmpty()) {
                _products.value = list
            }
        } catch (e: Exception) {
             Log.e("Repository", "Failed to fetch products", e)
        }
    }

    suspend fun reportAnomaly(anomaly: AnomalyEvent) {
        try {
            val docRef = db.collection("stores").document(anomaly.storeId).collection("anomalies").document()
            docRef.set(anomaly.copy(id = docRef.id)).await()
        } catch (e: Exception) {
             Log.e("Repository", "Failed to report anomaly", e)
             _anomalies.value = listOf(anomaly.copy(id = "local_mock_id")) + _anomalies.value
        }
    }
    
    suspend fun updateAnomalyStatus(storeId: String, anomalyId: String, newStatus: String) {
        try {
            db.collection("stores").document(storeId).collection("anomalies")
                .document(anomalyId).update("status", newStatus).await()
        } catch (e: Exception) {
             Log.e("Repository", "Failed to update anomaly", e)
             // Local mock update
             _anomalies.value = _anomalies.value.map { 
                 if (it.id == anomalyId || anomalyId == "local_mock_id") it.copy(status = newStatus) else it 
             }
        }
    }
}

// Global instance for demo purposes
val appRepository by lazy { Repository() }
