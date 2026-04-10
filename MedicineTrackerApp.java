// =============================================================================
// FILE 3: MedicineTrackerApp.java  --  MENU-DRIVEN CLI (User Input Layer)
// =============================================================================
// Purpose  : Application entry point and interactive console interface.
//
//            Responsibilities
//            ----------------
//              - Print the application banner on startup.
//              - Preload 22 test medicines covering all alert scenarios.
//              - Present a numbered menu and read user input via Scanner.
//              - Validate inputs before delegating to InventoryManager.
//              - Provide clean, professional, emoji-free console output.
//
//            This file contains NO data-structure logic.
//            All DSA operations are handled by InventoryManager (File 2).
//
// Menu Options
// ------------
//   1. Add Medicine            -> InventoryManager.addMedicine()
//   2. Search by Name          -> InventoryManager.searchByName()
//   3. Update Quantity         -> InventoryManager.updateQuantity()
//   4. Remove Medicine         -> InventoryManager.removeMedicine()
//   5. View All (by Expiry)    -> InventoryManager.showAllByExpiry()
//   6. Expiring Soon           -> InventoryManager.showExpiringSoon()
//   7. Low Stock Report        -> InventoryManager.showLowStock()
//   8. Alert Dashboard         -> InventoryManager.showAlerts()
//   9. Inventory Statistics    -> InventoryManager.showStats()
//   0. Exit
//
// Test Data Groups (22 medicines, preloaded on startup)
// -----------------------------------------------------
//   Group A -- Healthy (adequate stock, far-future expiry)       7 items
//   Group B -- Expiring soon (within alert window, good stock)   3 items
//   Group C -- Already expired                                   3 items
//   Group D -- Low stock only (adequate expiry)                  3 items
//   Group E -- Dual alert: low stock AND expiring soon           3 items
//   Group F -- HashMap collision test (multiple 'A' names)       3 items
//
// Project   : Medicine Inventory Tracker -- DSA Project
// Team      : Aakanksha (2501) . Apa (2512) . Brandon (2514)
//             Chetan (2516)   . Sherine (2544)
// =============================================================================

package MedicineInventory;

import java.time.LocalDate;
import java.util.Scanner;

/**
 * MedicineTrackerApp -- CLI entry point.
 *
 * Compile and run:
 *   javac MedicineInventory/*.java
 *   java  MedicineInventory.MedicineTrackerApp
 */
public class MedicineTrackerApp {

    private static final InventoryManager inventory = new InventoryManager();
    private static final Scanner          scanner   = new Scanner(System.in);

    // =========================================================================
    // MAIN
    // =========================================================================

    public static void main(String[] args) {
        printBanner();
        TestDataLoader.loadTestData(inventory);
        runMenuLoop();
        scanner.close();
        System.out.println("\n  Session ended. Goodbye.");
        System.out.println(AppConstants.SEP_HEAVY);
    }


    // =========================================================================
    // MENU LOOP
    // =========================================================================

    /**
     * Displays the main menu and dispatches to handler methods until the
     * user selects option 0 (Exit).
     */
    private static void runMenuLoop() {
        boolean running = true;
        while (running) {
            printMenu();
            String choice = readLine("Select option");

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
                default  -> System.out.println("\n  ERROR: Invalid option. Enter 0 - 9.");
            }
        }
    }


    // =========================================================================
    // MENU HANDLERS
    // =========================================================================

    /**
     * Handler for option 1 -- Add Medicine.
     * Collects all required fields with input validation before calling
     * InventoryManager.addMedicine().
     */
    private static void handleAddMedicine() {
        System.out.println();
        System.out.println("  ADD NEW MEDICINE");
        System.out.println(AppConstants.SEP_LIGHT);

        String name  = readLine("  Medicine Name         ");
        String batch = readLine("  Batch Number          ");
        int    qty   = readPositiveInt("  Quantity             ");
        String date  = readLine("  Expiry Date (YYYY-MM-DD) ");

        inventory.addMedicine(name, batch, qty, date);
    }

    /**
     * Handler for option 2 -- Search by Name.
     */
    private static void handleSearch() {
        System.out.println();
        System.out.println("  SEARCH MEDICINE");
        System.out.println(AppConstants.SEP_LIGHT);
        String name = readLine("  Medicine Name ");
        inventory.searchByName(name);
    }

    /**
     * Handler for option 3 -- Update Quantity.
     */
    private static void handleUpdateQuantity() {
        System.out.println();
        System.out.println("  UPDATE STOCK QUANTITY");
        System.out.println(AppConstants.SEP_LIGHT);
        String name = readLine("  Medicine Name ");
        int    qty  = readPositiveInt("  New Quantity ");
        inventory.updateQuantity(name, qty);
    }

    /**
     * Handler for option 4 -- Remove Medicine.
     * Displays a confirmation prompt before proceeding with removal.
     */
    private static void handleRemoveMedicine() {
        System.out.println();
        System.out.println("  REMOVE MEDICINE");
        System.out.println(AppConstants.SEP_LIGHT);
        String name    = readLine("  Medicine Name              ");
        String confirm = readLine("  Confirm removal (yes / no) ");

        if (confirm.equalsIgnoreCase("yes") || confirm.equalsIgnoreCase("y")) {
            inventory.removeMedicine(name);
        } else {
            System.out.println("  Removal cancelled.");
        }
    }


    // =========================================================================
    // TEST DATA
    // =========================================================================

    /**
     * Preloads 22 medicines into the inventory at startup.
     *
     * All dates are computed relative to LocalDate.now() so that the test
     * data remains valid on any run date without manual adjustment.
     *
     * Group A -- 7 medicines: adequate stock, expiry 90 - 730 days away.
     *            These populate the right subtrees of the B-Tree (future keys).
     *
     * Group B -- 3 medicines: adequate stock, expiry within alert window.
     *            These will appear in showExpiringSoon() output.
     *
     * Group C -- 3 medicines: already expired (1, 10, 45 days past).
     *            These are the leftmost B-Tree keys; appear as EXPIRED in views.
     *
     * Group D -- 3 medicines: low stock only, adequate expiry.
     *            These exercise the showLowStock() scan path.
     *
     * Group E -- 3 medicines: BOTH low stock AND expiring soon.
     *            Dual-alert scenario; both thresholds triggered simultaneously.
     *
     * Group F -- 3 medicines: names all starting with 'A'.
     *            Tests HashMap chaining (potential collision in same bucket).
     */
    // DISPLAY HELPERS
    // =========================================================================

    /**
     * Prints the application banner on startup.
     * Contains project name, DSA structures, and team attribution.
     */
    private static void printBanner() {
        System.out.println();
        System.out.println(AppConstants.SEP_HEAVY);
        System.out.println("    MEDICINE INVENTORY TRACKER  --  DSA Project");
        System.out.println(AppConstants.SEP_LIGHT);
        System.out.println("    Data Structures Used:");
        System.out.println("      1. HashMap   --  O(1) average search by medicine name");
        System.out.println("      2. B-Tree    --  O(log n) sorted expiry index (order 3)");
        System.out.println("                       Self-balancing; guaranteed O(log n)");
        System.out.println("                       height regardless of insertion order");
        System.out.println(AppConstants.SEP_LIGHT);
        System.out.println("    Team: Aakanksha Sawant  (2501)     Apa Mestry     (2512)");
        System.out.println("          Brandon Noronha   (2514)     Chetan Mirashi (2516)");
        System.out.println("          Sherine Travasso  (2544)");
        System.out.println(AppConstants.SEP_HEAVY);
    }

    /**
     * Prints the numbered main menu with complexity annotations.
     */
    private static void printMenu() {
        System.out.println();
        System.out.println(AppConstants.SEP_HEAVY);
        System.out.println("    MAIN MENU");
        System.out.println(AppConstants.SEP_LIGHT);
        System.out.println("    1.  Add Medicine");
        System.out.println("    2.  Search Medicine by Name           [ HashMap  O(1)      ]");
        System.out.println("    3.  Update Stock Quantity");
        System.out.println("    4.  Remove Medicine");
        System.out.println("    5.  View All  (sorted by expiry)      [ B-Tree   O(n)      ]");
        System.out.println("    6.  Expiring Soon                     [ B-Tree   O(k log n)]");
        System.out.println("    7.  Low Stock Report");
        System.out.println("    8.  Alert Dashboard");
        System.out.println("    9.  Inventory Statistics");
        System.out.println("    0.  Exit");
        System.out.println(AppConstants.SEP_HEAVY);
    }

    /**
     * Reads a non-blank, trimmed line from standard input.
     *
     * @param prompt Label displayed before the input cursor.
     * @return Trimmed, non-blank string entered by the user.
     */
    private static String readLine(String prompt) {
        System.out.print(prompt + ": ");
        return scanner.nextLine().trim();
    }

    /**
     * Reads a non-negative integer from standard input, looping until
     * valid input is provided.
     *
     * @param prompt Label displayed before the input cursor.
     * @return Non-negative integer entered by the user.
     */
    private static int readPositiveInt(String prompt) {
        while (true) {
            try {
                int val = Integer.parseInt(readLine(prompt));
                if (val >= 0) return val;
                System.out.println("  ERROR: Value must be 0 or greater. Please re-enter.");
            } catch (NumberFormatException e) {
                System.out.println("  ERROR: Please enter a valid integer.");
            }
        }
    }
}
