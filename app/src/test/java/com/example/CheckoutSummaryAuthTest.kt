package com.example

import com.example.auth.UserRole
import com.example.auth.appAuthManager
import com.example.data.appRepository
import com.example.ui.screens.shopper.CheckoutStatus
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
class CheckoutSummaryAuthTest {

    private lateinit var viewModel: ShopperViewModel

    @Before
    fun setup() {
        viewModel = ShopperViewModel()
        val store = viewModel.stores.value.first()
        viewModel.selectStore(store)
    }

    @Test
    fun testUserLoginAuthenticationSuccess() {
        appAuthManager.loginUser("alex.rivera@scanandgo.store", "Alex Rivera", UserRole.SHOPPER)
        assertTrue("User should be logged in", appAuthManager.isLoggedIn.value)
        assertEquals(UserRole.SHOPPER, appAuthManager.currentRole.value)
        
        val user = appAuthManager.currentUser.value
        assertNotNull(user)
        assertEquals("Alex Rivera", user?.displayName)
        assertEquals("Gold Member", user?.loyaltyTier)
        assertEquals(5, user?.memberDiscountPercent)
    }

    @Test
    fun testCartQuantityAdjustmentInCheckoutSummary() = runBlocking {
        val product = appRepository.products.value.first()
        
        // Add item with quantity 1
        viewModel.addProductToCart(product, quantityToAdd = 1)
        var item = viewModel.cart.value.find { it.product.id == product.id }
        assertEquals(1, item?.quantity)

        // Increment quantity by 1
        viewModel.updateCartQuantity(product.id, delta = 1)
        item = viewModel.cart.value.find { it.product.id == product.id }
        assertEquals(2, item?.quantity)

        // Decrement quantity by 1
        viewModel.updateCartQuantity(product.id, delta = -1)
        item = viewModel.cart.value.find { it.product.id == product.id }
        assertEquals(1, item?.quantity)

        // Decrementing below 1 removes the item from cart
        viewModel.updateCartQuantity(product.id, delta = -1)
        item = viewModel.cart.value.find { it.product.id == product.id }
        assertNull("Item should be removed when quantity reaches 0", item)
    }

    @Test
    fun testDirectRemoveItemFromCart() = runBlocking {
        val product = appRepository.products.value.first()
        viewModel.addProductToCart(product, quantityToAdd = 3)
        assertTrue(viewModel.cart.value.any { it.product.id == product.id })

        viewModel.removeProductFromCart(product.id)
        assertFalse(viewModel.cart.value.any { it.product.id == product.id })
    }

    @Test
    fun testPayNowTriggersCheckoutFlow() = runBlocking {
        val product = appRepository.products.value.first()
        viewModel.addProductToCart(product, quantityToAdd = 1)
        
        assertEquals(CheckoutStatus.Idle, viewModel.checkoutStatus.value)

        viewModel.checkout()
        val currentStatus = viewModel.checkoutStatus.value
        assertTrue(
            "Status should transition to Polling or Success",
            currentStatus is CheckoutStatus.Polling || currentStatus is CheckoutStatus.Success
        )
    }
}
