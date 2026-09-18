package com.example.data

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Product(
    val id: String = "",
    val storeId: String = "",
    val name: String = "",
    val barcode: String = "",
    val price: Double = 0.0,
    val weightGrams: Int = 0,
    val yoloClass: String = "",
    val shelfX: Float = 0f,
    val shelfY: Float = 0f,
    val shelfZ: Float = 0f,
    val stockQuantity: Int = 50,
    val isNew: Boolean = false,
    val discount: Double = 0.0,
    val expiryDate: String = "2027-01-01",
    val category: String = ""
)

@IgnoreExtraProperties
data class Shelf(
    val id: String = "",
    val storeId: String = "",
    val name: String = "",
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 260f,
    val height: Float = 100f,
    val depth: Float = 100f,
    val shelfHeight: Float = 120f,
    val aisleNumber: Int = 1,
    val category: String = "",
    val tiers: Int = 3
)

@IgnoreExtraProperties
data class AnomalyEvent(
    val id: String = "",
    val storeId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val expectedWeightGrams: Int = 0,
    val actualWeightGrams: Int = 0,
    val discrepancyReason: String = "",
    val inferenceTimeMs: Long = 0,
    val status: String = "FLAGGED" // FLAGGED, APPROVED, REJECTED
)

data class CartItem(
    val product: Product,
    val quantity: Int,
    val isAiVerified: Boolean = false
)

data class AiVerificationResult(
    val product: Product,
    val confidence: Float = 0.98f,
    val detectedYoloClass: String = "",
    val inferenceTimeMs: Long = 142,
    val isMatch: Boolean = true,
    val notes: String = "",
    val photoDescription: String = ""
)

data class Store(
    val id: String,
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val itemCap: Int = 15
)
