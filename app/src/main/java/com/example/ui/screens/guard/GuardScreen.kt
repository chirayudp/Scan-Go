package com.example.ui.screens.guard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AnomalyEvent
import com.example.data.Product
import com.example.ui.screens.InteractiveStoreMap
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardScreen(viewModel: GuardViewModel = viewModel()) {
    val anomalies by viewModel.anomalies.collectAsState(initial = emptyList())
    val selectedAnomaly by viewModel.selectedAnomaly.collectAsState()
    
    val products by viewModel.products.collectAsState(initial = emptyList())
    val shelves by viewModel.shelves.collectAsState(initial = emptyList())
    val selectedMapProduct by viewModel.selectedMapProduct.collectAsState()
    
    var selectedTabIndex by remember { mutableStateOf(0) }
    val activeAnomalies = anomalies.filter { it.status == "FLAGGED" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Worker / Security Dashboard") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    titleContentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }, text = { Text("Security Queue") })
                Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }, text = { Text("Store Map") })
            }
            
            if (selectedTabIndex == 0) {
                // Security Queue View
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left sidebar: Anomaly Queue
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceVariant)) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                Text("Real-Time Queue", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                            }
                            if (activeAnomalies.isEmpty()) {
                                item {
                                    Text("No active anomalies.", modifier = Modifier.padding(16.dp))
                                }
                            }
                            items(activeAnomalies) { anomaly ->
                                val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(anomaly.timestamp))
                                ListItem(
                                    headlineContent = { Text("Discrepancy: ${anomaly.actualWeightGrams - anomaly.expectedWeightGrams}g") },
                                    supportingContent = { Text(time) },
                                    modifier = Modifier.clickable { viewModel.selectAnomaly(anomaly) },
                                    colors = ListItemDefaults.colors(
                                        containerColor = if (selectedAnomaly?.id == anomaly.id) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                    )
                                )
                                HorizontalDivider()
                            }
                        }
                    }

                    // Right Panel: Inspection
                    Box(modifier = Modifier.weight(2f).fillMaxHeight().padding(16.dp)) {
                        if (selectedAnomaly != null) {
                            InspectionPanel(
                                anomaly = selectedAnomaly!!,
                                onApprove = { viewModel.approveOverride() }
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Select an anomaly to inspect")
                            }
                        }
                    }
                }
            } else {
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
                        ProductDetailsCard(
                            product = selectedMapProduct!!,
                            onClose = { viewModel.selectMapProduct(null) },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProductDetailsCard(product: Product, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.width(300.dp).padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Product Details", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text("Name: ${product.name}")
            Text("Price: $${product.price}")
            Text("Discount: $${product.discount}")
            Text("Quantity in Stock: ${product.stockQuantity}")
            Text("Expiry Date: ${product.expiryDate}")
            Spacer(Modifier.height(16.dp))
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                Text("Close")
            }
        }
    }
}

@Composable
fun InspectionPanel(anomaly: AnomalyEvent, onApprove: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Visual Inspection", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        
        // Mock Camera Feed with YOLO Bounding Boxes
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.DarkGray)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val boxWidth = size.width * 0.3f
                val boxHeight = size.height * 0.4f
                val left = size.width / 2 - boxWidth / 2
                val top = size.height / 2 - boxHeight / 2
                
                // Draw mock "unscanned item" bounding box
                drawRect(
                    color = Color.Red,
                    topLeft = Offset(left, top),
                    size = Size(boxWidth, boxHeight),
                    style = Stroke(width = 8f)
                )
            }
            Text(
                "YOLOv8 Inference: Unscanned Item [0.92]",
                color = Color.Red,
                modifier = Modifier.padding(8.dp).background(Color.Black.copy(alpha = 0.5f)).padding(4.dp)
            )
        }

        Spacer(Modifier.height(16.dp))
        
        // Telemetry
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Telemetry Data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Expected Weight: ${anomaly.expectedWeightGrams}g")
                Text("Actual Weight: ${anomaly.actualWeightGrams}g")
                Text("Weight Discrepancy: ${anomaly.actualWeightGrams - anomaly.expectedWeightGrams}g")
                Text("Inference Time: ${anomaly.inferenceTimeMs}ms")
                Text("Reason: ${anomaly.discrepancyReason}")
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onApprove,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Approve Override (Open Gate)")
        }
    }
}
