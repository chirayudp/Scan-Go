package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.data.Shelf
import kotlin.math.*

/**
 * 3D Coordinate in store space (X: width, Y: depth/aisle length, Z: height/tier)
 */
data class Vector3D(val x: Float, val y: Float, val z: Float)

/**
 * Projected 2D screen coordinate with depth for Z-sorting
 */
data class ProjectedPoint(
    val screenX: Float,
    val screenY: Float,
    val depth: Float,
    val scale: Float
)

/**
 * 3D Camera projection helper
 */
class Camera3D(
    var yawDeg: Float = -35f,
    var pitchDeg: Float = 50f,
    var zoom: Float = 1.0f,
    var panX: Float = 0f,
    var panY: Float = 0f,
    val centerX: Float = 360f,
    val centerY: Float = 240f
) {
    fun project(p: Vector3D, canvasW: Float, canvasH: Float): ProjectedPoint {
        val dx = p.x - centerX
        val dy = p.y - centerY
        val dz = p.z

        val radYaw = Math.toRadians(yawDeg.toDouble()).toFloat()
        val radPitch = Math.toRadians(pitchDeg.toDouble()).toFloat()

        // Rotate around vertical Z axis (Yaw)
        val cosY = cos(radYaw)
        val sinY = sin(radYaw)
        val rx = dx * cosY - dy * sinY
        val ry = dx * sinY + dy * cosY

        // Tilt around X axis (Pitch)
        val cosP = cos(radPitch)
        val sinP = sin(radPitch)
        val depth = ry * cosP - dz * sinP
        val screenY0 = ry * sinP - dz * cosP

        // Perspective scaling
        val cameraDistance = 1400f
        val perspective = (cameraDistance / (cameraDistance + depth)).coerceIn(0.2f, 3.0f)
        val finalScale = perspective * zoom

        val sx = canvasW / 2f + rx * finalScale + panX
        val sy = canvasH / 2f + screenY0 * finalScale + panY

        return ProjectedPoint(sx, sy, depth, finalScale)
    }
}

enum class GizmoAxis { NONE, X_AXIS, Y_AXIS, Z_AXIS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveStoreMap(
    products: List<Product>,
    shelves: List<Shelf>,
    selectedProduct: Product?,
    isEditable: Boolean = false,
    onProductMove: (String, Float, Float) -> Unit = { _, _, _ -> },
    onProductMove3D: (String, Float, Float, Float) -> Unit = { id, dx, dy, _ -> onProductMove(id, dx, dy) },
    onProductSelect: (String) -> Unit = {},
    onClearSelection: () -> Unit = {}
) {
    // Search query & category filter
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    // Camera 3D controls state
    var yaw by remember { mutableStateOf(-30f) }
    var pitch by remember { mutableStateOf(48f) }
    var zoom by remember { mutableStateOf(1.0f) }
    var panX by remember { mutableStateOf(0f) }
    var panY by remember { mutableStateOf(0f) }
    var isPanMode by remember { mutableStateOf(false) }

    // Admin Dragging & 3D Gizmo state
    var draggedProductId by remember { mutableStateOf<String?>(null) }
    var activeGizmoAxis by remember { mutableStateOf(GizmoAxis.NONE) }
    var dragDeltaX by remember { mutableStateOf(0f) }
    var dragDeltaY by remember { mutableStateOf(0f) }
    var dragDeltaZ by remember { mutableStateOf(0f) }

    // Pulsing animation for 3D search beacons
    val infiniteTransition = rememberInfiniteTransition(label = "beacon")
    val beaconPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "beaconPulse"
    )

    // Filter products based on search & category chip
    val filteredProductIds by remember(searchQuery, selectedCategoryFilter, products) {
        derivedStateOf {
            val q = searchQuery.trim().lowercase()
            products.filter { p ->
                val matchesCategory = when (selectedCategoryFilter) {
                    "All" -> true
                    "🔴 Out of Stock" -> p.stockQuantity <= 0
                    "🟢 New Arrivals" -> p.isNew
                    "🏷️ On Sale" -> p.discount > 0
                    else -> p.category.equals(selectedCategoryFilter, ignoreCase = true) ||
                            p.yoloClass.contains(selectedCategoryFilter, ignoreCase = true)
                }

                val matchesQuery = if (q.isEmpty()) true else {
                    p.name.lowercase().contains(q) ||
                    p.category.lowercase().contains(q) ||
                    p.yoloClass.lowercase().contains(q) ||
                    p.barcode.contains(q)
                }

                matchesCategory && matchesQuery
            }.map { it.id }.toSet()
        }
    }

    val textMeasurer = rememberTextMeasurer()

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1E222B))) {
        // --- 3D CANVAS RENDERING ---
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isPanMode, isEditable, products, selectedProduct) {
                    detectTapGestures(
                        onTap = { tapOffset ->
                            // Find product closest to tap within screen projection radius
                            val camera = Camera3D(yaw, pitch, zoom, panX, panY)
                            var closest: Product? = null
                            var minDistance = 70f // hit tolerance

                            products.forEach { p ->
                                val pt = camera.project(
                                    Vector3D(p.shelfX, p.shelfY, p.shelfZ),
                                    size.width.toFloat(),
                                    size.height.toFloat()
                                )
                                val dist = sqrt((pt.screenX - tapOffset.x).pow(2) + (pt.screenY - tapOffset.y).pow(2))
                                if (dist < minDistance) {
                                    minDistance = dist
                                    closest = p
                                }
                            }

                            if (closest != null) {
                                onProductSelect(closest!!.id)
                            } else {
                                onClearSelection()
                            }
                        }
                    )
                }
                .pointerInput(isPanMode, isEditable, selectedProduct) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            if (isEditable && selectedProduct != null) {
                                // Check if user tapped near selected product or gizmo
                                val camera = Camera3D(yaw, pitch, zoom, panX, panY)
                                val p = selectedProduct
                                val pt = camera.project(
                                    Vector3D(p.shelfX, p.shelfY, p.shelfZ),
                                    size.width.toFloat(),
                                    size.height.toFloat()
                                )
                                val dist = sqrt((pt.screenX - startOffset.x).pow(2) + (pt.screenY - startOffset.y).pow(2))
                                if (dist < 90f) {
                                    draggedProductId = p.id
                                    dragDeltaX = 0f
                                    dragDeltaY = 0f
                                    dragDeltaZ = 0f
                                }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (draggedProductId != null && isEditable) {
                                // Moving product along 3D axes
                                val sensitivity = 1.2f / zoom
                                dragDeltaX += dragAmount.x * sensitivity
                                dragDeltaY += dragAmount.y * sensitivity
                            } else {
                                if (isPanMode) {
                                    // Pan camera
                                    panX += dragAmount.x
                                    panY += dragAmount.y
                                } else {
                                    // Orbit camera: Yaw and Pitch
                                    yaw = (yaw + dragAmount.x * 0.45f) % 360f
                                    pitch = (pitch - dragAmount.y * 0.35f).coerceIn(15f, 85f)
                                }
                            }
                        },
                        onDragEnd = {
                            if (draggedProductId != null) {
                                val p = products.find { it.id == draggedProductId }
                                if (p != null) {
                                    onProductMove3D(
                                        p.id,
                                        p.shelfX + dragDeltaX,
                                        p.shelfY + dragDeltaY,
                                        p.shelfZ + dragDeltaZ
                                    )
                                }
                                draggedProductId = null
                                dragDeltaX = 0f
                                dragDeltaY = 0f
                                dragDeltaZ = 0f
                            }
                        },
                        onDragCancel = {
                            draggedProductId = null
                            dragDeltaX = 0f
                            dragDeltaY = 0f
                            dragDeltaZ = 0f
                        }
                    )
                }
        ) {
            val canvasW = size.width
            val canvasH = size.height
            val camera = Camera3D(yaw, pitch, zoom, panX, panY)

            // 1. Draw 3D Store Floor Grid & Walkways
            drawStoreFloor3D(camera, canvasW, canvasH)

            // 2. Draw 3D Shelves & Aisles
            shelves.forEach { shelf ->
                drawShelfUnit3D(camera, shelf, canvasW, canvasH, textMeasurer)
            }

            // 3. Draw 3D Products & Subsection Blocks
            // Sort products by depth (Back-to-front Painter's algorithm)
            val sortedProducts = products.map { p ->
                val currX = p.shelfX + if (p.id == draggedProductId) dragDeltaX else 0f
                val currY = p.shelfY + if (p.id == draggedProductId) dragDeltaY else 0f
                val currZ = p.shelfZ + if (p.id == draggedProductId) dragDeltaZ else 0f
                val pt = camera.project(Vector3D(currX, currY, currZ), canvasW, canvasH)
                Triple(p, pt, Vector3D(currX, currY, currZ))
            }.sortedByDescending { it.second.depth }

            sortedProducts.forEach { (product, projectedPt, currentPos) ->
                val isSelected = product.id == selectedProduct?.id
                val isHighlighted = filteredProductIds.contains(product.id)
                val isSearchActive = searchQuery.isNotBlank() || selectedCategoryFilter != "All"

                drawProductBox3D(
                    camera = camera,
                    product = product,
                    pos = currentPos,
                    canvasW = canvasW,
                    canvasH = canvasH,
                    isSelected = isSelected,
                    isHighlighted = isHighlighted,
                    dimmed = isSearchActive && !isHighlighted,
                    beaconPulse = if (isHighlighted && isSearchActive) beaconPulse else 0f,
                    textMeasurer = textMeasurer
                )

                // 4. Draw 3D Transformation Gizmo if selected in editable Admin view
                if (isSelected && isEditable) {
                    drawTransformGizmo3D(camera, currentPos, canvasW, canvasH, textMeasurer)
                }
            }
        }

        // --- TOP INTERFACE: SEARCH BAR & CATEGORY CHIPS ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search item, category, or barcode...", fontSize = 14.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Quick Category / Status Filter Chips
            val filterChips = listOf(
                "All",
                "Produce",
                "Beverages",
                "Bakery",
                "Snacks",
                "🔴 Out of Stock",
                "🟢 New Arrivals",
                "🏷️ On Sale"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterChips.forEach { chip ->
                    val isSelected = selectedCategoryFilter == chip
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategoryFilter = chip },
                        label = { Text(chip, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                        )
                    )
                }
            }

            // Search Results Summary Banner
            AnimatedVisibility(
                visible = (searchQuery.isNotBlank() || selectedCategoryFilter != "All"),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = "Beacons",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${filteredProductIds.size} location(s) highlighted with 3D beacons",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.weight(1f))
                        if (filteredProductIds.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    val firstMatch = products.firstOrNull { filteredProductIds.contains(it.id) }
                                    if (firstMatch != null) {
                                        onProductSelect(firstMatch.id)
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Focus First", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // --- FLOATING 3D CAMERA HUD (CONTROLS & PRESETS) ---
        Card(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .shadow(6.dp, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("3D Camera", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Quick Preset: 3D Isometric
                    IconButton(
                        onClick = { yaw = -35f; pitch = 50f; zoom = 1.0f; panX = 0f; panY = 0f },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ViewInAr, contentDescription = "3D Isometric", modifier = Modifier.size(18.dp))
                    }
                    // Quick Preset: Top-Down
                    IconButton(
                        onClick = { yaw = 0f; pitch = 82f; zoom = 1.0f; panX = 0f; panY = 0f },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Layers, contentDescription = "Top Down", modifier = Modifier.size(18.dp))
                    }
                    // Quick Preset: Aisle Front View
                    IconButton(
                        onClick = { yaw = -90f; pitch = 25f; zoom = 1.1f; panX = 0f; panY = 0f },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Storefront, contentDescription = "Aisle Front", modifier = Modifier.size(18.dp))
                    }
                }

                HorizontalDivider(modifier = Modifier.width(100.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Rotate Left
                    IconButton(onClick = { yaw = (yaw - 25f) % 360f }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.RotateLeft, contentDescription = "Rotate Left", modifier = Modifier.size(18.dp))
                    }
                    // Orbit / Pan Mode toggle
                    IconButton(
                        onClick = { isPanMode = !isPanMode },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (isPanMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                CircleShape
                            )
                    ) {
                        Icon(
                            if (isPanMode) Icons.Default.PanTool else Icons.Default.Sync,
                            contentDescription = "Mode",
                            tint = if (isPanMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    // Rotate Right
                    IconButton(onClick = { yaw = (yaw + 25f) % 360f }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.RotateRight, contentDescription = "Rotate Right", modifier = Modifier.size(18.dp))
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Zoom In
                    IconButton(onClick = { zoom = (zoom + 0.2f).coerceAtMost(2.5f) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(18.dp))
                    }
                    // Reset View
                    IconButton(onClick = { yaw = -30f; pitch = 48f; zoom = 1.0f; panX = 0f; panY = 0f }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Reset", modifier = Modifier.size(18.dp))
                    }
                    // Zoom Out
                    IconButton(onClick = { zoom = (zoom - 0.2f).coerceAtLeast(0.5f) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // --- 3D INTERACTION INSTRUCTION BADGE ---
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xCC111827)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isPanMode) Icons.Default.PanTool else Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = Color.Cyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isPanMode) "1-Finger: Pan Store Floor" else "1-Finger: Rotate 3D View • Tap Item to Inspect",
                    color = Color.White,
                    fontSize = 11.sp
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 3D DRAWING IMPLEMENTATIONS (Floor, Shelving Units, Items, Beacons)
// -------------------------------------------------------------

/**
 * Draws the 3D Supermarket Tile Floor with aisles and entrance boundaries
 */
fun DrawScope.drawStoreFloor3D(camera: Camera3D, canvasW: Float, canvasH: Float) {
    val floorMinX = 20f
    val floorMaxX = 760f
    val floorMinY = 40f
    val floorMaxY = 460f
    val floorZ = 0f

    // 4 corners of the floor
    val c1 = camera.project(Vector3D(floorMinX, floorMinY, floorZ), canvasW, canvasH)
    val c2 = camera.project(Vector3D(floorMaxX, floorMinY, floorZ), canvasW, canvasH)
    val c3 = camera.project(Vector3D(floorMaxX, floorMaxY, floorZ), canvasW, canvasH)
    val c4 = camera.project(Vector3D(floorMinX, floorMaxY, floorZ), canvasW, canvasH)

    val floorPath = Path().apply {
        moveTo(c1.screenX, c1.screenY)
        lineTo(c2.screenX, c2.screenY)
        lineTo(c3.screenX, c3.screenY)
        lineTo(c4.screenX, c4.screenY)
        close()
    }

    // Floor Base (Polished dark supermarket tile)
    drawPath(path = floorPath, color = Color(0xFF2A2E39))
    drawPath(path = floorPath, color = Color(0xFF3F4555), style = Stroke(width = 3f))

    // 3D Grid Lines on the floor (Supermarket aisles walking lines)
    for (x in 100..700 step 100) {
        val start = camera.project(Vector3D(x.toFloat(), floorMinY, floorZ), canvasW, canvasH)
        val end = camera.project(Vector3D(x.toFloat(), floorMaxY, floorZ), canvasW, canvasH)
        drawLine(
            color = Color(0xFF353B4A),
            start = Offset(start.screenX, start.screenY),
            end = Offset(end.screenX, end.screenY),
            strokeWidth = 1.5f
        )
    }

    for (y in 80..420 step 80) {
        val start = camera.project(Vector3D(floorMinX, y.toFloat(), floorZ), canvasW, canvasH)
        val end = camera.project(Vector3D(floorMaxX, y.toFloat(), floorZ), canvasW, canvasH)
        drawLine(
            color = Color(0xFF353B4A),
            start = Offset(start.screenX, start.screenY),
            end = Offset(end.screenX, end.screenY),
            strokeWidth = 1.5f
        )
    }
}

/**
 * Draws a 3D Shelving Unit (Multi-tier shelf boards, 4 steel corner posts,
 * subsection bay dividers, and overhead 3D Aisle Signboard)
 */
fun DrawScope.drawShelfUnit3D(
    camera: Camera3D,
    shelf: Shelf,
    canvasW: Float,
    canvasH: Float,
    textMeasurer: TextMeasurer
) {
    val x0 = shelf.x
    val y0 = shelf.y
    val w = shelf.width
    val d = shelf.depth
    val totalH = shelf.shelfHeight
    val tiers = shelf.tiers

    // Corner pillar posts at (x0, y0), (x0+w, y0), (x0+w, y0+d), (x0, y0+d)
    val corners = listOf(
        Pair(x0, y0),
        Pair(x0 + w, y0),
        Pair(x0 + w, y0 + d),
        Pair(x0, y0 + d)
    )

    // 1. Draw vertical structural posts (extruded 3D steel columns)
    corners.forEach { (cx, cy) ->
        val b = camera.project(Vector3D(cx, cy, 0f), canvasW, canvasH)
        val t = camera.project(Vector3D(cx, cy, totalH), canvasW, canvasH)
        drawLine(
            color = Color(0xFF78839C),
            start = Offset(b.screenX, b.screenY),
            end = Offset(t.screenX, t.screenY),
            strokeWidth = 4f * b.scale
        )
    }

    // 2. Draw 3D Horizontal Shelf Boards (Tiers)
    val tierLevels = listOf(15f, 60f, 105f)
    tierLevels.forEachIndexed { tierIndex, zLevel ->
        val p1 = camera.project(Vector3D(x0, y0, zLevel), canvasW, canvasH)
        val p2 = camera.project(Vector3D(x0 + w, y0, zLevel), canvasW, canvasH)
        val p3 = camera.project(Vector3D(x0 + w, y0 + d, zLevel), canvasW, canvasH)
        val p4 = camera.project(Vector3D(x0, y0 + d, zLevel), canvasW, canvasH)

        val boardTop = Path().apply {
            moveTo(p1.screenX, p1.screenY)
            lineTo(p2.screenX, p2.screenY)
            lineTo(p3.screenX, p3.screenY)
            lineTo(p4.screenX, p4.screenY)
            close()
        }

        // Shelf board top surface
        drawPath(path = boardTop, color = Color(0xFF4B5568))
        drawPath(path = boardTop, color = Color(0xFF6B7A99), style = Stroke(width = 1.5f * p1.scale))

        // Shelf thickness front edge lip (at y0 + d)
        val lipP3 = camera.project(Vector3D(x0 + w, y0 + d, zLevel - 6f), canvasW, canvasH)
        val lipP4 = camera.project(Vector3D(x0, y0 + d, zLevel - 6f), canvasW, canvasH)
        val frontLip = Path().apply {
            moveTo(p4.screenX, p4.screenY)
            lineTo(p3.screenX, p3.screenY)
            lineTo(lipP3.screenX, lipP3.screenY)
            lineTo(lipP4.screenX, lipP4.screenY)
            close()
        }
        drawPath(path = frontLip, color = Color(0xFF353C4D))

        // Subsection dividers across the shelf width
        val subsectionCount = 3
        for (i in 1 until subsectionCount) {
            val subX = x0 + (w / subsectionCount) * i
            val s1 = camera.project(Vector3D(subX, y0, zLevel), canvasW, canvasH)
            val s2 = camera.project(Vector3D(subX, y0 + d, zLevel), canvasW, canvasH)
            drawLine(
                color = Color(0xFF8C9BAE),
                start = Offset(s1.screenX, s1.screenY),
                end = Offset(s2.screenX, s2.screenY),
                strokeWidth = 2f * s1.scale
            )
        }
    }

    // 3. Overhead 3D Aisle Signboard (Hanging above the shelf)
    val signZ = totalH + 30f
    val signCenterX = x0 + w / 2f
    val signCenterY = y0 + d / 2f
    val signPos = camera.project(Vector3D(signCenterX, signCenterY, signZ), canvasW, canvasH)
    val postBottom = camera.project(Vector3D(signCenterX, signCenterY, totalH), canvasW, canvasH)

    // Vertical mounting rod
    drawLine(
        color = Color(0xFFCBD5E1),
        start = Offset(postBottom.screenX, postBottom.screenY),
        end = Offset(signPos.screenX, signPos.screenY),
        strokeWidth = 3f * signPos.scale
    )

    // Signboard Billboard in 3D
    val signW = 120f * signPos.scale
    val signH = 34f * signPos.scale
    val signLeft = signPos.screenX - signW / 2f
    val signTop = signPos.screenY - signH / 2f

    drawRoundRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(signLeft, signTop),
        size = Size(signW, signH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
    )
    drawRoundRect(
        color = Color(0xFF38BDF8),
        topLeft = Offset(signLeft, signTop),
        size = Size(signW, signH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
        style = Stroke(width = 2f)
    )

    val labelText = "A${shelf.aisleNumber}: ${shelf.name.take(14)}"
    drawText(
        textMeasurer = textMeasurer,
        text = labelText,
        topLeft = Offset(signLeft + 8f, signTop + 6f),
        style = TextStyle(
            color = Color.White,
            fontSize = (11f * signPos.scale).coerceIn(8f, 15f).sp,
            fontWeight = FontWeight.Bold
        )
    )
}

/**
 * Draws an individual Product as a 3D Box/Container with shaded faces, status cues,
 * and vertical holographic 3D beacons when targeted by search
 */
fun DrawScope.drawProductBox3D(
    camera: Camera3D,
    product: Product,
    pos: Vector3D,
    canvasW: Float,
    canvasH: Float,
    isSelected: Boolean,
    isHighlighted: Boolean,
    dimmed: Boolean,
    beaconPulse: Float,
    textMeasurer: TextMeasurer
) {
    // 3D dimensions of product block
    val pw = 50f
    val pd = 50f
    val ph = 40f

    val x0 = pos.x
    val y0 = pos.y
    val z0 = pos.z

    // Color logic
    val baseColor = when {
        product.stockQuantity <= 0 -> Color(0xFFEF4444) // Out of stock -> Red
        product.isNew -> Color(0xFF22C55E)              // New arrival -> Green
        isHighlighted -> Color(0xFFF59E0B)              // Search highlight -> Amber
        else -> Color(0xFF3B82F6)                       // Normal in-stock -> Blue
    }

    val finalBaseColor = if (dimmed) baseColor.copy(alpha = 0.35f) else baseColor

    // 8 Corners of the 3D Box
    // Bottom 4: b1(x0,y0), b2(x0+pw,y0), b3(x0+pw,y0+pd), b4(x0,y0+pd)
    // Top 4:    t1(x0,y0), t2(x0+pw,y0), t3(x0+pw,y0+pd), t4(x0,y0+pd) at z0+ph
    val b1 = camera.project(Vector3D(x0, y0, z0), canvasW, canvasH)
    val b2 = camera.project(Vector3D(x0 + pw, y0, z0), canvasW, canvasH)
    val b3 = camera.project(Vector3D(x0 + pw, y0 + pd, z0), canvasW, canvasH)
    val b4 = camera.project(Vector3D(x0, y0 + pd, z0), canvasW, canvasH)

    val t1 = camera.project(Vector3D(x0, y0, z0 + ph), canvasW, canvasH)
    val t2 = camera.project(Vector3D(x0 + pw, y0, z0 + ph), canvasW, canvasH)
    val t3 = camera.project(Vector3D(x0 + pw, y0 + pd, z0 + ph), canvasW, canvasH)
    val t4 = camera.project(Vector3D(x0, y0 + pd, z0 + ph), canvasW, canvasH)

    // --- Face 1: Top Face (Lighter shade for overhead lighting) ---
    val topPath = Path().apply {
        moveTo(t1.screenX, t1.screenY)
        lineTo(t2.screenX, t2.screenY)
        lineTo(t3.screenX, t3.screenY)
        lineTo(t4.screenX, t4.screenY)
        close()
    }
    val topColor = finalBaseColor.copy(
        red = (finalBaseColor.red * 1.25f).coerceAtMost(1f),
        green = (finalBaseColor.green * 1.25f).coerceAtMost(1f),
        blue = (finalBaseColor.blue * 1.25f).coerceAtMost(1f)
    )
    drawPath(path = topPath, color = topColor)
    drawPath(path = topPath, color = Color.White.copy(alpha = if (dimmed) 0.1f else 0.5f), style = Stroke(width = 1.5f))

    // --- Face 2: Front Face (facing y0+pd) ---
    val frontPath = Path().apply {
        moveTo(t4.screenX, t4.screenY)
        lineTo(t3.screenX, t3.screenY)
        lineTo(b3.screenX, b3.screenY)
        lineTo(b4.screenX, b4.screenY)
        close()
    }
    drawPath(path = frontPath, color = finalBaseColor)
    drawPath(path = frontPath, color = Color.Black.copy(alpha = 0.2f), style = Stroke(width = 1.5f))

    // --- Face 3: Right Side Face (facing x0+pw) ---
    val rightSidePath = Path().apply {
        moveTo(t3.screenX, t3.screenY)
        lineTo(t2.screenX, t2.screenY)
        lineTo(b2.screenX, b2.screenY)
        lineTo(b3.screenX, b3.screenY)
        close()
    }
    val sideColor = finalBaseColor.copy(
        red = finalBaseColor.red * 0.75f,
        green = finalBaseColor.green * 0.75f,
        blue = finalBaseColor.blue * 0.75f
    )
    drawPath(path = rightSidePath, color = sideColor)
    drawPath(path = rightSidePath, color = Color.Black.copy(alpha = 0.25f), style = Stroke(width = 1.5f))

    // Product Title Text on the front/top
    if (!dimmed) {
        val labelScale = t3.scale
        val labelX = (t4.screenX + t3.screenX) / 2f - 18f
        val labelY = (t4.screenY + b4.screenY) / 2f - 6f
        val displayName = if (product.stockQuantity <= 0) "OUT" else product.name.take(5)
        drawText(
            textMeasurer = textMeasurer,
            text = displayName,
            topLeft = Offset(labelX, labelY),
            style = TextStyle(
                color = Color.White,
                fontSize = (9f * labelScale).coerceIn(7f, 13f).sp,
                fontWeight = FontWeight.Bold
            )
        )
    }

    // --- SELECTION HIGHLIGHT RING ---
    if (isSelected) {
        val selPath = Path().apply {
            moveTo(t1.screenX, t1.screenY)
            lineTo(t2.screenX, t2.screenY)
            lineTo(t3.screenX, t3.screenY)
            lineTo(t4.screenX, t4.screenY)
            close()
        }
        drawPath(path = selPath, color = Color(0xFFFDE047), style = Stroke(width = 4f * t1.scale))
    }

    // --- 3D SEARCH BEACON / PIN BEAM ---
    if (isHighlighted && beaconPulse > 0f) {
        val centerX = x0 + pw / 2f
        val centerY = y0 + pd / 2f
        val basePt = camera.project(Vector3D(centerX, centerY, z0 + ph), canvasW, canvasH)
        val beamTopPt = camera.project(Vector3D(centerX, centerY, z0 + ph + 130f), canvasW, canvasH)

        // Pulsing ground rings on the shelf surface
        val ringRadius = (16f + beaconPulse * 36f) * basePt.scale
        drawCircle(
            color = Color(0xFFF59E0B).copy(alpha = (1f - beaconPulse) * 0.8f),
            radius = ringRadius,
            center = Offset(basePt.screenX, basePt.screenY),
            style = Stroke(width = 3f * basePt.scale)
        )

        // Vertical glowing holographic light beam
        drawLine(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFF59E0B).copy(alpha = 0.9f), Color(0xFFF59E0B).copy(alpha = 0.1f)),
                startY = beamTopPt.screenY,
                endY = basePt.screenY
            ),
            start = Offset(basePt.screenX, basePt.screenY),
            end = Offset(beamTopPt.screenX, beamTopPt.screenY),
            strokeWidth = 6f * basePt.scale
        )

        // Floating Badge at top of the beacon
        drawCircle(
            color = Color(0xFFF59E0B),
            radius = 10f * beamTopPt.scale,
            center = Offset(beamTopPt.screenX, beamTopPt.screenY)
        )
        drawCircle(
            color = Color.White,
            radius = 5f * beamTopPt.scale,
            center = Offset(beamTopPt.screenX, beamTopPt.screenY)
        )

        // Floating Product Name & Price pill
        val tagText = "${product.name} • $${product.price}"
        val tagW = 110f * beamTopPt.scale
        val tagH = 26f * beamTopPt.scale
        val tagLeft = beamTopPt.screenX - tagW / 2f
        val tagTop = beamTopPt.screenY - tagH - 8f

        drawRoundRect(
            color = Color(0xE60F172A),
            topLeft = Offset(tagLeft, tagTop),
            size = Size(tagW, tagH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
        )
        drawRoundRect(
            color = Color(0xFFF59E0B),
            topLeft = Offset(tagLeft, tagTop),
            size = Size(tagW, tagH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
            style = Stroke(width = 2f)
        )
        drawText(
            textMeasurer = textMeasurer,
            text = tagText,
            topLeft = Offset(tagLeft + 6f, tagTop + 5f),
            style = TextStyle(
                color = Color.White,
                fontSize = (10f * beamTopPt.scale).coerceIn(8f, 13f).sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

/**
 * Draws a 3D Transformation Gizmo with 3 primary Cartesian axes (X, Y, Z)
 * anchored directly to the selected item for precise spatial manipulation
 */
fun DrawScope.drawTransformGizmo3D(
    camera: Camera3D,
    pos: Vector3D,
    canvasW: Float,
    canvasH: Float,
    textMeasurer: TextMeasurer
) {
    val origin = camera.project(Vector3D(pos.x + 25f, pos.y + 25f, pos.z + 20f), canvasW, canvasH)
    val scale = origin.scale
    val arrowLen = 70f

    // 1. Red X Axis (Shelf Bay width)
    val xEnd = camera.project(Vector3D(pos.x + 25f + arrowLen, pos.y + 25f, pos.z + 20f), canvasW, canvasH)
    drawLine(
        color = Color(0xFFEF4444),
        start = Offset(origin.screenX, origin.screenY),
        end = Offset(xEnd.screenX, xEnd.screenY),
        strokeWidth = 5f * scale
    )
    drawCircle(color = Color(0xFFEF4444), radius = 7f * scale, center = Offset(xEnd.screenX, xEnd.screenY))
    drawText(
        textMeasurer = textMeasurer,
        text = "X (Bay)",
        topLeft = Offset(xEnd.screenX + 8f, xEnd.screenY - 6f),
        style = TextStyle(color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
    )

    // 2. Green Y Axis (Aisle depth)
    val yEnd = camera.project(Vector3D(pos.x + 25f, pos.y + 25f + arrowLen, pos.z + 20f), canvasW, canvasH)
    drawLine(
        color = Color(0xFF22C55E),
        start = Offset(origin.screenX, origin.screenY),
        end = Offset(yEnd.screenX, yEnd.screenY),
        strokeWidth = 5f * scale
    )
    drawCircle(color = Color(0xFF22C55E), radius = 7f * scale, center = Offset(yEnd.screenX, yEnd.screenY))
    drawText(
        textMeasurer = textMeasurer,
        text = "Y (Aisle)",
        topLeft = Offset(yEnd.screenX + 8f, yEnd.screenY - 6f),
        style = TextStyle(color = Color(0xFF22C55E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
    )

    // 3. Blue Z Axis (Shelf Tier level)
    val zEnd = camera.project(Vector3D(pos.x + 25f, pos.y + 25f, pos.z + 20f + arrowLen), canvasW, canvasH)
    drawLine(
        color = Color(0xFF38BDF8),
        start = Offset(origin.screenX, origin.screenY),
        end = Offset(zEnd.screenX, zEnd.screenY),
        strokeWidth = 5f * scale
    )
    drawCircle(color = Color(0xFF38BDF8), radius = 7f * scale, center = Offset(zEnd.screenX, zEnd.screenY))
    drawText(
        textMeasurer = textMeasurer,
        text = "Z (Tier)",
        topLeft = Offset(zEnd.screenX + 8f, zEnd.screenY - 14f),
        style = TextStyle(color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
    )

    // Central anchor point
    drawCircle(color = Color.White, radius = 5f * scale, center = Offset(origin.screenX, origin.screenY))
}
