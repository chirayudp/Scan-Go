package com.example

import com.example.data.appRepository
import com.example.ui.screens.shopper.ShopperViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ShopperBarcodeTest {

    private lateinit var viewModel: ShopperViewModel

    @Before
    fun setup() {
        viewModel = ShopperViewModel()
        val store = viewModel.stores.value.first()
        viewModel.selectStore(store)
    }

    @Test
    fun testScanValidBarcodeGivesProductDetails() = runBlocking {
        // Find a known product barcode in catalog
        val targetProduct = appRepository.products.value.first()
        val scanned = viewModel.onBarcodeScanned(targetProduct.barcode)

        assertNotNull("Product should be found by barcode", scanned)
        assertEquals(targetProduct.id, scanned?.id)
        assertEquals(targetProduct.name, scanned?.name)
        assertEquals(targetProduct, viewModel.scannedProduct.value)
    }

    @Test
    fun testAddScannedProductToCart() = runBlocking {
        val targetProduct = appRepository.products.value.first()
        val initialCartCount = viewModel.cart.value.sumOf { it.quantity }

        viewModel.addProductToCart(targetProduct, quantityToAdd = 2)

        val updatedCart = viewModel.cart.value
        val itemInCart = updatedCart.find { it.product.id == targetProduct.id }
        assertNotNull("Item should be added to cart", itemInCart)
        assertEquals(2, itemInCart?.quantity)
        assertEquals(initialCartCount + 2, updatedCart.sumOf { it.quantity })
    }

    @Test
    fun testScanInvalidBarcodeReturnsNull() = runBlocking {
        val scanned = viewModel.onBarcodeScanned("NON_EXISTENT_99999")
        assertNull(scanned)
    }

    @Test
    fun testAiPhotoVerificationModeAndCartIntegration() = runBlocking {
        viewModel.openScanner(com.example.ui.screens.shopper.ScannerMode.AI_PHOTO)
        assertEquals(com.example.ui.screens.shopper.ScannerMode.AI_PHOTO, viewModel.scannerMode.value)
        assertTrue(viewModel.isScannerOpen.value)

        val targetProduct = appRepository.products.value.first()
        viewModel.processCapturedItemPhoto(targetProduct, simulateDelay = false)

        val result = viewModel.aiVerificationResult.value
        assertNotNull("AI verification result should be populated", result)
        assertEquals(targetProduct.id, result?.product?.id)
        assertTrue("Confidence should be above 90%", (result?.confidence ?: 0f) > 0.90f)

        // Confirm AI verified product into cart
        viewModel.addVerifiedProductToCart(result!!.product, quantityToAdd = 1)

        val cartItem = viewModel.cart.value.find { it.product.id == targetProduct.id }
        assertNotNull("Item should be in cart", cartItem)
        assertTrue("Item should be marked as AI verified", cartItem?.isAiVerified == true)
        assertNull("AI verification dialog state should be dismissed", viewModel.aiVerificationResult.value)
    }
}
