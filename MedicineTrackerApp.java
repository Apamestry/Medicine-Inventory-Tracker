// =============================================================================
// FILE 3: MedicineTrackerApp.java  —  MENU-DRIVEN CLI (User Input Layer)
// =============================================================================
// Purpose  : Entry point and interactive console UI.
//            Responsibilities:
//              • Preload realistic test data into the inventory on startup
//              • Display a numbered menu with all available operations
//              • Read and validate user input via Scanner
//              • Delegate all business logic to InventoryManager
//              • Provide a clean, formatted console experience
//
// This file contains NO data-structure logic — it only orchestrates the UI.
// All DSA operations are handled by InventoryManager (File 2).
//
// Menu Options
// ────────────
//   1. Add Medicine          → InventoryManager.addMedicine()
//   2. Search by Name        → InventoryManager.searchByName()
//   3. Update Quantity       → InventoryManager.updateQuantity()
//   4. Remove Medicine       → InventoryManager.removeMedicine()
//   5. View All (by Expiry)  → InventoryManager.showAllByExpiry()
//   6. Expiring Soon         → InventoryManager.showExpiringSoon()
//   7. Low Stock Report      → InventoryManager.showLowStock()
//   8. Alert Dashboard       → InventoryManager.showAlerts()
//   9. Inventory Statistics  → InventoryManager.showStats()
//   0. Exit
//
// Test Data (preloaded on startup — covers all alert scenarios)
// ─────────────────────────────────────────────────────────────
//   • Mix of OK, expiring-soon, already-expired, and low-stock medicines
//   • Covers HashMap collision scenario (multiple medicines, same first letter)
//   • Covers BST branching (dates spread across past, near-future, far-future)
//
// Project   : Medicine Inventory Tracker — DSA Project
// Team      : Aakanksha (2501) · Apa (2512) · Brandon (2514)
//             Chetan (2516)   · Sherine (2544)
// =============================================================================

package MedicineInventory;

import java.time.LocalDate;
import java.util.Scanner;

/**
 * MedicineTrackerApp — CLI entry point.
 *
 * Run with:
 *   javac MedicineInventory/*.java
 *   java  MedicineInventory.MedicineTrackerApp
 */
public class MedicineTrackerApp {

    // ── Shared resources ─────────────────────────────────────────────────────
    private static final InventoryManager inventory = new InventoryManager();
    private static final Scanner          scanner   = new Scanner(System.in);

    // =========================================================================
    // MAIN — Application entry point
    // =========================================================================

    public static void main(String[] args) {

        printBanner();             // display project header
        loadTestData();            // preload realistic sample data
        runMenuLoop();             // start interactive menu

        scanner.close();
        System.out.println("\n  Goodbye! Stay healthy. 💊");
    }


    // =========================================================================
    // MENU LOOP
    // =========================================================================

    /**
     * Displays the main menu repeatedly until the user selects Exit (0).
     * Each iteration:
     *  1. Print the menu.
     *  2. Read user choice.
     *  3. Dispatch to the appropriate handler method.
     */
    private static void runMenuLoop() {
        boolean running = true;

        while (running) {
            printMenu();
            String choice = readLine("Select an option").trim();

            switch (choice) {
                case "1" -> handleAddMedicine();
                case "2" -> handleSearch();
                case "3" -> handleUpdateQuantity();
                case "4" -> handleRemoveMedicine();
                case "5" -> inventory.showAllByExpiry();
                case "6" -> inventory.showExpiringSoon();
                case "7" -> inventory.showLowStock();
                case "8" -> inventory.showAlerts();
                case "9" -> inventory.showStats();
                case "0" -> running = false;
                default  -> System.out.println("\n  ❌ Invalid option. Enter 0–9.");
            }
        }
    }


    // =========================================================================
    // MENU HANDLERS — one method per menu option
    // =========================================================================

    /**
     * Handler for option 1 — Add Medicine.
     * Prompts for all required fields, validates quantity as integer,
     * then delegates to InventoryManager.addMedicine().
     */
    private static void handleAddMedicine() {
        System.out.println("\n╔══ ADD NEW MEDICINE ══╗");

        String name  = readLine("  Medicine Name      ");
        String batch = readLine("  Batch Number       ");

        int qty = -1;
        while (qty < 0) {
            try {
                qty = Integer.parseInt(readLine("  Quantity           "));
                if (qty < 0) System.out.println("  ❌ Quantity must be ≥ 0.");
            } catch (NumberFormatException e) {
                System.out.println("  ❌ Please enter a valid integer.");
            }
        }

        String date = readLine("  Expiry Date (YYYY-MM-DD) ");

        inventory.addMedicine(name, batch, qty, date);
    }

    /**
     * Handler for option 2 — Search by Name.
     * Reads the search term and delegates to searchByName().
     */
    private static void handleSearch() {
        System.out.println("\n╔══ SEARCH MEDICINE ══╗");
        String name = readLine("  Enter Medicine Name ");
        inventory.searchByName(name);
    }

    /**
     * Handler for option 3 — Update Quantity.
     * Reads the medicine name and new quantity, validates the quantity,
     * then delegates to updateQuantity().
     */
    private static void handleUpdateQuantity() {
        System.out.println("\n╔══ UPDATE QUANTITY ══╗");
        String name = readLine("  Medicine Name      ");

        int qty = -1;
        while (qty < 0) {
            try {
                qty = Integer.parseInt(readLine("  New Quantity       "));
                if (qty < 0) System.out.println("  ❌ Quantity must be ≥ 0.");
            } catch (NumberFormatException e) {
                System.out.println("  ❌ Please enter a valid integer.");
            }
        }

        inventory.updateQuantity(name, qty);
    }

    /**
     * Handler for option 4 — Remove Medicine.
     * Confirms intent before calling removeMedicine().
     */
    private static void handleRemoveMedicine() {
        System.out.println("\n╔══ REMOVE MEDICINE ══╗");
        String name    = readLine("  Medicine Name to Remove ");
        String confirm = readLine("  Confirm removal of '" + name + "'? (yes/no) ");

        if (confirm.equalsIgnoreCase("yes") || confirm.equalsIgnoreCase("y")) {
            inventory.removeMedicine(name);
        } else {
            System.out.println("  ↩  Removal cancelled.");
        }
    }


    // =========================================================================
    // TEST DATA — Preloaded on startup
    // =========================================================================

    /**
     * Loads a realistic set of medicines into the inventory for testing.
     * Data is designed to exercise all code paths:
     *
     *  Group A — OK medicines (expiry far in future, adequate stock)
     *  Group B — Expiring soon (within EXPIRY_ALERT_DAYS days)
     *  Group C — Already expired
     *  Group D — Low stock (< LOW_STOCK_THRESHOLD units)
     *  Group E — Both low stock AND expiring soon (dual-alert scenario)
     *
     * The dates below are expressed relative to the run date for reliability.
     * Absolute dates target early-mid 2025 context but can be adjusted.
     */
    private static void loadTestData() {
        System.out.println("\n  ⏳ Loading test data...");
        System.out.println(AppConstants.SEPARATOR);

        // ── Compute dynamic dates relative to today ───────────────────────────
        String today          = LocalDate.now().toString();
        String yesterday      = LocalDate.now().minusDays(1).toString();
        String expired10      = LocalDate.now().minusDays(10).toString();
        String expired45      = LocalDate.now().minusDays(45).toString();
        String expiring5      = LocalDate.now().plusDays(5).toString();
        String expiring15     = LocalDate.now().plusDays(15).toString();
        String expiring29     = LocalDate.now().plusDays(29).toString();
        String safe90         = LocalDate.now().plusDays(90).toString();
        String safe180        = LocalDate.now().plusDays(180).toString();
        String safe365        = LocalDate.now().plusDays(365).toString();
        String safe500        = LocalDate.now().plusDays(500).toString();
        String safe730        = LocalDate.now().plusDays(730).toString();

        // ─────────────────────────────────────────────────────────────────────
        // GROUP A: Healthy stock — adequate quantity, no expiry concern
        // DSA: These spread the BST to the right (far-future keys)
        // ─────────────────────────────────────────────────────────────────────
        inventory.addMedicine("Paracetamol",    "B-001", 500,  safe365);
        inventory.addMedicine("Amoxicillin",    "B-002", 320,  safe180);
        inventory.addMedicine("Ibuprofen",      "B-003", 450,  safe730);
        inventory.addMedicine("Metformin",      "B-004", 600,  safe500);
        inventory.addMedicine("Omeprazole",     "B-005", 280,  safe365);
        inventory.addMedicine("Cetirizine",     "B-006", 350,  safe180);
        inventory.addMedicine("Azithromycin",   "B-007", 200,  safe90);

        // ─────────────────────────────────────────────────────────────────────
        // GROUP B: Expiring soon (within alert window) — adequate stock
        // DSA: These create left-leaning BST nodes (near-future dates)
        //      TreeMap.headMap() will include these in showExpiringSoon()
        // ─────────────────────────────────────────────────────────────────────
        inventory.addMedicine("Aspirin",        "B-008", 120,  expiring29);
        inventory.addMedicine("Doxycycline",    "B-009", 80,   expiring15);
        inventory.addMedicine("Ranitidine",     "B-010", 60,   expiring5);

        // ─────────────────────────────────────────────────────────────────────
        // GROUP C: Already expired — should appear as EXPIRED in all views
        // DSA: These are leftmost BST nodes (past dates < today)
        // ─────────────────────────────────────────────────────────────────────
        inventory.addMedicine("Cefixime",       "B-011", 40,   yesterday);
        inventory.addMedicine("Diclofenac",     "B-012", 90,   expired10);
        inventory.addMedicine("Tetracycline",   "B-013", 110,  expired45);

        // ─────────────────────────────────────────────────────────────────────
        // GROUP D: Low stock (< 20 units) — adequate expiry
        // DSA: Demonstrates showLowStock() O(n) HashMap scan
        // ─────────────────────────────────────────────────────────────────────
        inventory.addMedicine("Loratadine",     "B-014", 15,   safe365);
        inventory.addMedicine("Pantoprazole",   "B-015", 8,    safe180);
        inventory.addMedicine("Metronidazole",  "B-016", 3,    safe90);

        // ─────────────────────────────────────────────────────────────────────
        // GROUP E: Dual-alert — BOTH low stock AND expiring soon
        // These trigger both warnings simultaneously, exercising checkAlerts()
        // ─────────────────────────────────────────────────────────────────────
        inventory.addMedicine("Salbutamol",     "B-017", 12,   expiring15);
        inventory.addMedicine("Prednisolone",   "B-018", 5,    expiring5);
        inventory.addMedicine("Clonazepam",     "B-019", 1,    today);

        // ─────────────────────────────────────────────────────────────────────
        // GROUP F: HashMap collision test (names starting with 'A')
        // Multiple entries whose keys hash to the same bucket — Java handles
        // via chaining, but this exercises the collision code path
        // ─────────────────────────────────────────────────────────────────────
        inventory.addMedicine("Atorvastatin",   "B-020", 250,  safe730);
        inventory.addMedicine("Amlodipine",     "B-021", 190,  safe500);
        inventory.addMedicine("Atenolol",       "B-022", 175,  safe365);

        System.out.println(AppConstants.SEPARATOR);
        System.out.printf("  ✅ Test data loaded: %d medicines in inventory.%n", 22);
        System.out.println(AppConstants.SEPARATOR);
    }


    // =========================================================================
    // DISPLAY HELPERS
    // =========================================================================

    /**
     * Prints the application banner / header on startup.
     * Includes project name, DSA structures used, and team info.
     */
    private static void printBanner() {
        System.out.println();
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║       💊  MEDICINE INVENTORY TRACKER  — DSA PROJECT          ║");
        System.out.println("║                                                               ║");
        System.out.println("║   Data Structures:  HashMap (O(1) Search)                    ║");
        System.out.println("║                     BST / TreeMap (O(log n) Expiry Sort)      ║");
        System.out.println("║                                                               ║");
        System.out.println("║   Team: Aakanksha·2501  Apa·2512  Brandon·2514               ║");
        System.out.println("║         Chetan·2516     Sherine·2544                         ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    /**
     * Prints the numbered main menu to the console.
     */
    private static void printMenu() {
        System.out.println("\n" + AppConstants.SEPARATOR);
        System.out.println("  📋  MAIN MENU");
        System.out.println(AppConstants.SEPARATOR);
        System.out.println("  1.  ➕  Add Medicine");
        System.out.println("  2.  🔍  Search Medicine by Name     [HashMap — O(1)]");
        System.out.println("  3.  ✏   Update Quantity");
        System.out.println("  4.  🗑   Remove Medicine");
        System.out.println("  5.  📦  View All  (Sorted by Expiry) [BST In-Order — O(n)]");
        System.out.println("  6.  ⏰  Expiring Soon               [TreeMap headMap]");
        System.out.println("  7.  📉  Low Stock Report");
        System.out.println("  8.  🔔  Alert Dashboard");
        System.out.println("  9.  📊  Inventory Statistics");
        System.out.println("  0.  🚪  Exit");
        System.out.println(AppConstants.SEPARATOR);
    }

    /**
     * Reads a trimmed line of input from the scanner.
     * Displays a formatted prompt and appends ": " for clarity.
     *
     * @param prompt The label shown before the input cursor.
     * @return Trimmed user input string.
     */
    private static String readLine(String prompt) {
        System.out.print("  " + prompt + ": ");
        return scanner.nextLine().trim();
    }
}
