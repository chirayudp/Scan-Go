package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.Product
import com.example.ui.screens.InteractiveStoreMap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(viewModel: AdminViewModel = viewModel()) {
    val products by viewModel.products.collectAsState(initial = emptyList())
    val shelves by viewModel.shelves.collectAsState(initial = emptyList())
    val selectedProduct by viewModel.selectedProduct.collectAsState()
    val storeInfo by viewModel.storeInfo.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Map Editor") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Row(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Left Side: 2D Spatial Canvas simulating 3D floor plan
            Box(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
                    .background(Color.LightGray)
            ) {
                    InteractiveStoreMap(
                        products = products,
                        shelves = shelves,
                        selectedProduct = selectedProduct,
                        isEditable = true,
                        onProductMove = { id, newX, newY ->
                            viewModel.updateProductLocation(id, newX, newY)
                        },
                        onProductMove3D = { id, newX, newY, newZ ->
                            viewModel.updateProductLocation(id, newX, newY, newZ)
                        },
                        onProductSelect = { id ->
                            viewModel.selectProduct(products.find { it.id == id })
                        },
                        onClearSelection = { viewModel.selectProduct(null) }
                    )
            }

            // Right Side: Context Menu / SKU Editor
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                    Text("Store Grounding Search", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { viewModel.searchStoreGrounding(searchQuery) }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                        }
                    )
                    if (storeInfo.isNotEmpty()) {
                        Text(storeInfo, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    if (selectedProduct != null) {
                        ProductEditor(selectedProduct!!, onSave = { updatedProduct ->
                            viewModel.saveProductDetails(updatedProduct)
                        })
                    } else {
                        Text("Select a shelf item to edit.")
                    }
                }
            }
        }
    }
}

@Composable
fun ProductEditor(product: Product, onSave: (Product) -> Unit) {
    var name by remember(product.id) { mutableStateOf(product.name) }
    var price by remember(product.id) { mutableStateOf(product.price.toString()) }
    var weight by remember(product.id) { mutableStateOf(product.weightGrams.toString()) }
    var yoloClass by remember(product.id) { mutableStateOf(product.yoloClass) }
    var stockQuantity by remember(product.id) { mutableStateOf(product.stockQuantity.toString()) }
    var isNew by remember(product.id) { mutableStateOf(product.isNew) }
    var discount by remember(product.id) { mutableStateOf(product.discount.toString()) }
    var expiryDate by remember(product.id) { mutableStateOf(product.expiryDate) }

    Column {
        Text("Shelf SKU Editor", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Weight (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = stockQuantity, onValueChange = { stockQuantity = it }, label = { Text("Stock Qty") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            OutlinedTextField(value = discount, onValueChange = { discount = it }, label = { Text("Discount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
        }
        OutlinedTextField(value = yoloClass, onValueChange = { yoloClass = it }, label = { Text("YOLO Class") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = expiryDate, onValueChange = { expiryDate = it }, label = { Text("Expiry Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
        
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
            Checkbox(checked = isNew, onCheckedChange = { isNew = it })
            Text("Mark as New (Green)")
        }

        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            onSave(product.copy(
                name = name,
                price = price.toDoubleOrNull() ?: 0.0,
                weightGrams = weight.toIntOrNull() ?: 0,
                yoloClass = yoloClass,
                stockQuantity = stockQuantity.toIntOrNull() ?: 0,
                discount = discount.toDoubleOrNull() ?: 0.0,
                expiryDate = expiryDate,
                isNew = isNew
            ))
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Save Settings")
        }
    }
}

