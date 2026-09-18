# Scan-Go

Scan-Go is a smart, mobile-first **Scan & Go grocery shopping application** built to streamline the shopping experience from product discovery and scanning to navigation, checkout, and secure store exit.

The application combines barcode/QR scanning, AI-assisted product recognition, intelligent in-store navigation, cart management, smart checkout, shopping lists, budgeting, and store operations tools in one Android application.

---

## Features

### 1. Shopper & Cart Engine

#### Item Scanning & Recognition

- **Camera Barcode & QR Scanner** — Real-time barcode detection using Google ML Kit and CameraX.
- **AI Photo Item Identification** — Capture a product photo and identify grocery items using Gemini/ML verification. AI-identified products are marked with an **AI Verified** badge.
- **Fallback Manual Catalog Search** — Search the catalog by product name, category, barcode, or product ID and add items directly when scanning is unavailable.
- **Quick Test Scanner** — Simulate scans with realistic catalog items for testing and demonstrations.

#### Cart Management

- Smooth slide-in and fade-in animations for newly added items.
- Automatic list rearrangement using animated list updates.
- Configurable cart capacity, with a default limit of **10 items**.
- Live cumulative item-weight tracking in grams.
- Stepper controls to increase, decrease, or remove item quantities.
- Transient confirmation banners for successful item additions.

---

### 2. Interactive Store Map & Indoor Navigation

#### Interactive Store Floorplan

- Custom 2D store floorplan with **pan** and **pinch-to-zoom** controls.
- **90° rotation** controls with compass orientation reset.
- Store zones including:
  - Produce
  - Bakery
  - Dairy
  - Grocery aisles
  - Meat & Seafood
  - Deli
  - Checkout lanes
  - Customer service

#### Dynamic Routing & Pathfinding

- **A\*** / **Dijkstra** pathfinding for shortest-path navigation.
- Routing from the shopper's current location or from cart-item destinations.
- Dynamic avoidance of:
  - Closed aisles
  - Spill alerts
  - Congestion zones
- Product locators that highlight the exact aisle and shelf location for selected products.

---

### 3. Smart Checkout & Exit Security

#### Checkout Summary

- Itemized receipt with:
  - Subtotals
  - Local taxes
  - Promotional discounts
  - Bag fees
- Quantity stepper controls directly in the checkout summary.
- Multiple payment options, including:
  - Scan & Go in-app payment
  - Google Pay / card payment
  - Cash at exit

#### Weight Verification & Loss Prevention

- Exit-scale comparison between the measured cart weight and the expected catalog weight.
- Configurable tolerance thresholds for weight discrepancy detection.
- Flags potential unscanned items before exit-gate clearance.

#### Digital Exit Pass

- Generates a timestamped, signed QR exit pass after successful payment.
- The pass can be validated at the store exit/turnstile.

---

### 4. Smart Shopping Lists & Budget Tracker

#### Shopping Lists

- Create and manage personal shopping checklists.
- Automatically mark list items as completed when they are scanned into the cart.
- Use **Find on Map** to navigate directly to a product's store location.

#### Budget Tracking

- Set a spending budget before or during a shopping trip.
- Live budget progress indicators.
- Warnings when spending approaches or exceeds the configured budget.

---

### 5. Staff & Store Operations Portal

#### Security & Loss Prevention

- Real-time cart audit logs.
- Weight-discrepancy alerts.
- Random inspection flags.

#### Inventory & Pricing

- Stock-level adjustments.
- Barcode catalog maintenance.
- Product pricing updates.

#### Store Facility Management

- Temporarily close aisles.
- Report spills and other hazards.
- View active shopper heatmaps.

---

### 6. Authentication & Shopper Preferences

#### Accounts & Guest Sessions

- Guest mode for quick shopping trips.
- Saved shopper profiles.
- Saved payment methods.
- Purchase history and digital receipts.
- Allergy and dietary preference tags.

---

## How Scan-Go Works

A typical shopping session follows this flow:

```text
Start Shopping
      ↓
Scan Products / Search Catalog
      ↓
Items Added to Cart
      ↓
View Cart + Weight + Budget
      ↓
Get Product Locations / Store Route
      ↓
Continue Shopping
      ↓
Review Checkout Summary
      ↓
Pay
      ↓
Generate Digital Exit Pass
      ↓
Weight Verification
      ↓
Exit Store
```

The same system can also support store staff through the operations portal for inventory, facility, and loss-prevention workflows.

---

## Requirements

To build and run Scan-Go locally, install:

- **Android Studio**
- **Android SDK** required by the project
- **Android SDK Platform Tools**
- A compatible Android device or Android Emulator
- A compatible JDK (Android Studio's bundled JDK is recommended)

Some features may also require network access and configured API credentials.

---

## Getting Started

### 1. Clone the repository

```bash
git clone <repository-url>
cd Scan-Go
```

### 2. Open the project

Open the **Scan-Go** directory in Android Studio.

Open the project root, not the `app` directory:

```text
Scan-Go/
├── app/
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

### 3. Sync Gradle

Allow Android Studio to import the Gradle project and download the required dependencies.

Wait for Gradle synchronization to complete successfully before building the application.

### 4. Configure local credentials

Some features require external API access. Configure the required credentials using the project's expected local configuration mechanism.

**Never commit API keys, passwords, access tokens, or other secrets to Git.**

### 5. Run the application

Start an Android Emulator through **Device Manager** or connect a physical Android device with USB debugging enabled.

Select the `app` run configuration, choose the target device, and press **Run**.

---

## Building from the Command Line

From the project root:

```bash
./gradlew assembleDebug
```

To install the debug build on a connected Android device:

```bash
./gradlew installDebug
```

To clean the project:

```bash
./gradlew clean
```

The repository includes its own Gradle wrapper, so a separate Gradle installation is not required for normal development.

---

## Permissions

Scan-Go currently uses Android permissions/features for functionality such as:

- **Camera** — barcode/QR scanning and AI photo-based item identification.
- **Internet** — network-backed AI and application services.

Android may prompt the user to grant permissions when a feature first requires them.

---

## Project Structure

```text
Scan-Go/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/       # Application source code
│   │       ├── res/        # Android resources
│   │       └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── proguard-rules.pro
│
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
│
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

---

## Development Notes

Scan-Go is an Android application built with Kotlin and the Android toolchain. The project uses Gradle for dependency management and builds.

Core technologies used by the application include:

- Kotlin
- Android SDK
- Android Studio
- Gradle
- CameraX
- Google ML Kit
- Gemini-based AI verification

---

## Troubleshooting

### Gradle sync fails

From the project root, try:

```bash
./gradlew --stop
./gradlew clean
```

Then reopen the project or synchronize Gradle from Android Studio.

### Android device is not detected

Check:

```bash
adb devices
```

For a physical device, make sure USB debugging is enabled and the device has been authorized.

### Camera does not work

Check that camera permission has been granted to Scan-Go in Android system settings.

---

## Security

Do not commit any of the following to the repository:

- API keys
- Passwords
- Access tokens
- Private credentials
- Sensitive local configuration

Use local development configuration for secrets and keep them out of version control.

---

## License

Add the project's license information here.
