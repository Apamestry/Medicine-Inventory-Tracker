// =============================================================================
// FILE 2: InventoryManager.java  —  IMPLEMENTATION (Logic / Business Layer)
// =============================================================================
// Purpose  : Implements the InventoryService interface declared in Medicine.java.
//            Contains ALL data-structure logic:
//              • HashMap  — O(1) insert / lookup / delete by medicine name
//              • TreeMap  — O(log n) sorted expiry index (Java's Red-Black BST)
//              • BSTNode  — manual Binary Search Tree for expiry traversal
//                           (demonstrates BST concepts explicitly for DSA)
//
// Key Operations & Complexities
// ──────────────────────────────
//   addMedicine       : O(1) HashMap + O(log n) TreeMap/BST insert
//   searchByName      : O(1) HashMap get
//   updateQuantity    : O(1) HashMap get + in-place mutation
//   removeMedicine    : O(1) HashMap remove + O(log n) TreeMap remove
//   showAllByExpiry   : O(n) in-order BST traversal
//   showExpiringSoon  : O(k) where k = medicines in the alert window
//   showLowStock      : O(n) full scan of HashMap values
//   showAlerts        : O(n) combined scan
//
// Project   : Medicine Inventory Tracker — DSA Project
// Team      : Aakanksha (2501) · Apa (2512) · Brandon (2514)
//             Chetan (2516)   · Sherine (2544)
// =============================================================================

package MedicineInventory;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * InventoryManager — concrete implementation of InventoryService.
 *
 * Internal data structures
 * ────────────────────────
 *  nameIndex   : HashMap<String, Medicine>
 *                  Key   = lowercase medicine name
 *                  Value = Medicine object
 *                  → O(1) average-case for insert / search / delete
 *
 *  expiryIndex : TreeMap<LocalDate, List<Medicine>>
 *                  Key   = expiry date (auto-sorted by Java's Red-Black BST)
 *                  Value = list of medicines sharing that expiry
 *                  → O(log n) insert; O(n) in-order traversal
 *
 *  expiryBST   : BSTNode (manual BST root)
 *                  Explicit BST to demonstrate the data structure for the
 *                  DSA project.  Mirrors expiryIndex but uses hand-written
 *                  insert / traversal methods.
 */
public class InventoryManager implements InventoryService {

    // ── DSA Structure 1: HashMap — name → Medicine ──────────────────────────
    /** Primary lookup index. Enables O(1) search by medicine name. */
    private final Map<String, Medicine> nameIndex = new HashMap<>();

    // ── DSA Structure 2: TreeMap — expiry date → [Medicines] ────────────────
    /**
     * Secondary sorted index backed by Java's Red-Black BST (TreeMap).
     * Automatically keeps entries sorted by LocalDate key.
     * Used for displaying inventory in expiry order and range queries.
     */
    private final TreeMap<LocalDate, List<Medicine>> expiryIndex = new TreeMap<>();

    // ── DSA Structure 3: Manual Binary Search Tree ───────────────────────────
    /**
     * Root of a hand-written BST whose nodes key on LocalDate.
     * Demonstrates BST insert and in-order traversal explicitly.
     * Kept in sync with expiryIndex for correctness.
     */
    private BSTNode expiryBST = null;

    // ─────────────────────────────────────────────────────────────────────────
    // ADD MEDICINE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Inserts a new medicine into all three data structures.
     *
     * Steps:
     *  1. Parse and validate input → construct Medicine object.
     *  2. Reject duplicate names (same key already exists in HashMap).
     *  3. Put into HashMap          → O(1)
     *  4. Put into TreeMap bucket   → O(log n)
     *  5. Insert into manual BST    → O(log n) average, O(n) worst-case
     *  6. Run post-add alert checks.
     *
     * @param name       Medicine name (case-insensitive key).
     * @param batchNo    Batch identifier.
     * @param quantity   Initial stock count.
     * @param expiryDate ISO date string "YYYY-MM-DD".
     */
    @Override
    public void addMedicine(String name, String batchNo, int quantity, String expiryDate) {
        // ── Input validation ─────────────────────────────────────────────────
        if (name == null || name.isBlank()) {
            printError("Medicine name cannot be empty.");
            return;
        }
        if (quantity < 0) {
            printError("Quantity cannot be negative.");
            return;
        }

        // ── Duplicate check ──────────────────────────────────────────────────
        String key = name.trim().toLowerCase();
        if (nameIndex.containsKey(key)) {
            printError("'" + name + "' already exists. Use Update Quantity to adjust stock.");
            return;
        }

        // ── Parse & construct ────────────────────────────────────────────────
        Medicine med;
        try {
            med = new Medicine(name, batchNo, quantity, expiryDate);
        } catch (DateTimeParseException e) {
            printError("Invalid date format. Use " + AppConstants.DATE_FORMAT + ".");
            return;
        }

        // ── 1. HashMap insert: O(1) ───────────────────────────────────────────
        nameIndex.put(med.getKey(), med);

        // ── 2. TreeMap insert: O(log n) ───────────────────────────────────────
        // computeIfAbsent creates a new ArrayList bucket only if key is absent
        expiryIndex
            .computeIfAbsent(med.expiryDate, k -> new ArrayList<>())
            .add(med);

        // ── 3. Manual BST insert: O(log n) avg ───────────────────────────────
        expiryBST = bstInsert(expiryBST, med);

        printSuccess("Medicine '" + med.name + "' added successfully.");
        checkAndPrintAlerts(med);   // immediate post-add alerts
    }


    // ─────────────────────────────────────────────────────────────────────────
    // SEARCH BY NAME  (HashMap — O(1))
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Performs an O(1) HashMap lookup by medicine name.
     *
     * @param searchName The medicine name to look up (case-insensitive).
     */
    @Override
    public void searchByName(String searchName) {
        if (searchName == null || searchName.isBlank()) {
            printError("Search name cannot be empty.");
            return;
        }

        // ── HashMap get: O(1) ────────────────────────────────────────────────
        Medicine med = nameIndex.get(searchName.trim().toLowerCase());

        if (med != null) {
            System.out.println("\n🔍 Search Result:");
            System.out.println(AppConstants.SEPARATOR);
            System.out.println(AppConstants.TABLE_HEADER);
            System.out.println(AppConstants.SEPARATOR);
            System.out.println(med.toTableRow(getStatusTag(med)));
            System.out.println(AppConstants.SEPARATOR);
        } else {
            printError("Medicine '" + searchName + "' not found in inventory.");
        }
    }


    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE QUANTITY
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Updates the quantity of an existing medicine in place.
     * Because quantity is mutable on the Medicine object and the same object
     * reference is shared by HashMap and TreeMap, a single field update
     * propagates to both structures — O(1).
     *
     * @param name        Medicine name (case-insensitive).
     * @param newQuantity New stock count (must be ≥ 0).
     */
    @Override
    public void updateQuantity(String name, int newQuantity) {
        if (newQuantity < 0) {
            printError("Quantity cannot be negative.");
            return;
        }

        // ── HashMap lookup: O(1) ─────────────────────────────────────────────
        Medicine med = nameIndex.get(name.trim().toLowerCase());
        if (med == null) {
            printError("Medicine '" + name + "' not found.");
            return;
        }

        int oldQty = med.quantity;
        med.quantity = newQuantity;   // in-place mutation — both indices updated
        printSuccess("Quantity updated: '" + med.name
                + "' changed from " + oldQty + " → " + newQuantity + " units.");
        checkAndPrintAlerts(med);
    }


    // ─────────────────────────────────────────────────────────────────────────
    // REMOVE MEDICINE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Removes a medicine from the inventory.
     *
     *  1. HashMap remove: O(1)
     *  2. TreeMap bucket removal: O(log n) + O(k) to remove from bucket list
     *     (k = medicines sharing the same expiry date — typically very small)
     *  3. Manual BST: rebuild from TreeMap (BST deletion is complex; full
     *     rebuild is O(n) but acceptable for this interactive CLI use-case).
     *
     * @param name Medicine name to remove (case-insensitive).
     */
    @Override
    public void removeMedicine(String name) {
        String key = name.trim().toLowerCase();

        // ── HashMap remove: O(1) ─────────────────────────────────────────────
        Medicine med = nameIndex.remove(key);
        if (med == null) {
            printError("Medicine '" + name + "' not found.");
            return;
        }

        // ── TreeMap bucket removal: O(log n) ─────────────────────────────────
        List<Medicine> bucket = expiryIndex.get(med.expiryDate);
        if (bucket != null) {
            bucket.removeIf(m -> m.getKey().equals(key));
            if (bucket.isEmpty()) {
                expiryIndex.remove(med.expiryDate);  // prune empty bucket
            }
        }

        // ── Manual BST rebuild from TreeMap: O(n) ────────────────────────────
        expiryBST = rebuildBST();

        printSuccess("Medicine '" + med.name + "' removed from inventory.");
    }


    // ─────────────────────────────────────────────────────────────────────────
    // SHOW ALL BY EXPIRY  (BST in-order traversal — O(n))
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Displays all medicines sorted chronologically by expiry date.
     * Uses in-order traversal of the manual BST (Left → Node → Right),
     * which visits nodes in ascending key (date) order.
     */
    @Override
    public void showAllByExpiry() {
        if (nameIndex.isEmpty()) {
            System.out.println("\n📭 Inventory is empty.");
            return;
        }

        System.out.println("\n📦 Full Inventory — Sorted by Expiry Date (BST In-Order Traversal)");
        System.out.println(AppConstants.SEPARATOR);
        System.out.println(AppConstants.TABLE_HEADER);
        System.out.println(AppConstants.SEPARATOR);

        // ── BST in-order traversal ────────────────────────────────────────────
        inOrderTraversal(expiryBST);

        System.out.println(AppConstants.SEPARATOR);
        System.out.println("Total medicines: " + nameIndex.size());
    }


    // ─────────────────────────────────────────────────────────────────────────
    // SHOW EXPIRING SOON
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Displays medicines expiring within EXPIRY_ALERT_DAYS days.
     * Uses TreeMap.headMap() to get only the relevant date range in O(log n),
     * then iterates the result in O(k) where k = medicines in range.
     */
    @Override
    public void showExpiringSoon() {
        LocalDate threshold = LocalDate.now()
                .plusDays(AppConstants.EXPIRY_ALERT_DAYS);

        // headMap returns the sub-map with keys STRICTLY less than threshold
        // Adding 1 day ensures medicines expiring exactly on threshold are included
        Map<LocalDate, List<Medicine>> soonExpiring =
                expiryIndex.headMap(threshold.plusDays(1));

        if (soonExpiring.isEmpty()) {
            System.out.println("\n✅ No medicines expiring within the next "
                    + AppConstants.EXPIRY_ALERT_DAYS + " days.");
            return;
        }

        System.out.println("\n⏰ Medicines Expiring Within "
                + AppConstants.EXPIRY_ALERT_DAYS + " Days:");
        System.out.println(AppConstants.SEPARATOR);
        System.out.println(AppConstants.TABLE_HEADER);
        System.out.println(AppConstants.SEPARATOR);

        for (List<Medicine> bucket : soonExpiring.values()) {
            for (Medicine med : bucket) {
                long days = ChronoUnit.DAYS.between(LocalDate.now(), med.expiryDate);
                String tag = days <= 0 ? "❌ EXPIRED" : "⚠ " + days + " days left";
                System.out.println(med.toTableRow(tag));
            }
        }
        System.out.println(AppConstants.SEPARATOR);
    }


    // ─────────────────────────────────────────────────────────────────────────
    // SHOW LOW STOCK
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Displays medicines whose quantity is below LOW_STOCK_THRESHOLD.
     * Requires a full O(n) scan of HashMap values — no shortcut available
     * since the HashMap is keyed by name, not quantity.
     */
    @Override
    public void showLowStock() {
        System.out.println("\n📉 Low Stock Medicines (< "
                + AppConstants.LOW_STOCK_THRESHOLD + " units):");
        System.out.println(AppConstants.SEPARATOR);
        System.out.println(AppConstants.TABLE_HEADER);
        System.out.println(AppConstants.SEPARATOR);

        boolean found = false;

        // ── O(n) scan of HashMap values ───────────────────────────────────────
        for (Medicine med : nameIndex.values()) {
            if (med.quantity < AppConstants.LOW_STOCK_THRESHOLD) {
                System.out.println(med.toTableRow("⚠ LOW STOCK (" + med.quantity + ")"));
                found = true;
            }
        }

        if (!found) {
            System.out.println("  ✅ All medicines are adequately stocked.");
        }
        System.out.println(AppConstants.SEPARATOR);
    }


    // ─────────────────────────────────────────────────────────────────────────
    // SHOW ALERTS  (combined summary)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Prints a combined dashboard of all active alerts:
     *  • Expired medicines
     *  • Expiring soon
     *  • Low stock
     */
    @Override
    public void showAlerts() {
        System.out.println("\n🔔 ALERT DASHBOARD");
        System.out.println(AppConstants.SEPARATOR);

        int alertCount = 0;
        LocalDate today = LocalDate.now();
        LocalDate alertThreshold = today.plusDays(AppConstants.EXPIRY_ALERT_DAYS);

        for (Medicine med : nameIndex.values()) {
            long daysLeft = ChronoUnit.DAYS.between(today, med.expiryDate);
            boolean hasAlert = false;
            StringBuilder alerts = new StringBuilder();

            if (daysLeft < 0) {
                alerts.append("❌ EXPIRED  ");
                hasAlert = true;
            } else if (daysLeft <= AppConstants.EXPIRY_ALERT_DAYS) {
                alerts.append("⏰ Expiring in ").append(daysLeft).append(" days  ");
                hasAlert = true;
            }

            if (med.quantity < AppConstants.LOW_STOCK_THRESHOLD) {
                alerts.append("📉 Low Stock (").append(med.quantity).append(" units)");
                hasAlert = true;
            }

            if (hasAlert) {
                System.out.printf("  %-" + AppConstants.COL_NAME_WIDTH + "s → %s%n",
                        med.name, alerts);
                alertCount++;
            }
        }

        if (alertCount == 0) {
            System.out.println("  ✅ No active alerts. Inventory is healthy!");
        } else {
            System.out.println(AppConstants.SEPARATOR);
            System.out.println("  Total active alerts: " + alertCount);
        }
        System.out.println(AppConstants.SEPARATOR);
    }


    // ─────────────────────────────────────────────────────────────────────────
    // SHOW STATS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Displays a summary statistics dashboard of the current inventory.
     * Calculates: total medicines, total units, earliest/latest expiry,
     * expired count, low-stock count.
     * Complexity: O(n) single pass over HashMap values.
     */
    @Override
    public void showStats() {
        if (nameIndex.isEmpty()) {
            System.out.println("\n📊 Inventory is empty — no statistics available.");
            return;
        }

        int totalUnits   = 0;
        int expiredCount = 0;
        int lowStockCount = 0;
        LocalDate today  = LocalDate.now();

        // ── Single O(n) pass ──────────────────────────────────────────────────
        for (Medicine med : nameIndex.values()) {
            totalUnits += med.quantity;
            if (med.expiryDate.isBefore(today))          expiredCount++;
            if (med.quantity < AppConstants.LOW_STOCK_THRESHOLD) lowStockCount++;
        }

        // TreeMap gives us earliest (firstKey) and latest (lastKey) in O(log n)
        LocalDate earliest = expiryIndex.firstKey();
        LocalDate latest   = expiryIndex.lastKey();

        System.out.println("\n📊 INVENTORY STATISTICS");
        System.out.println(AppConstants.SEPARATOR);
        System.out.printf("  %-30s : %d%n",  "Total Medicines",      nameIndex.size());
        System.out.printf("  %-30s : %d%n",  "Total Stock Units",    totalUnits);
        System.out.printf("  %-30s : %s%n",  "Earliest Expiry",      earliest);
        System.out.printf("  %-30s : %s%n",  "Latest Expiry",        latest);
        System.out.printf("  %-30s : %d%n",  "Expired Medicines",    expiredCount);
        System.out.printf("  %-30s : %d%n",  "Low-Stock Medicines",  lowStockCount);
        System.out.printf("  %-30s : %d days%n","Alert Window",       AppConstants.EXPIRY_ALERT_DAYS);
        System.out.printf("  %-30s : %d units%n","Low-Stock Threshold", AppConstants.LOW_STOCK_THRESHOLD);
        System.out.println(AppConstants.SEPARATOR);
    }


    // =========================================================================
    // PRIVATE HELPERS — Manual BST Operations
    // =========================================================================

    /**
     * BST INSERT — recursive.
     * Inserts a medicine into the manual BST by its expiry date key.
     *
     * Algorithm:
     *  • If tree is empty → create new node.
     *  • If med.expiryDate < node.key → recurse LEFT.
     *  • If med.expiryDate > node.key → recurse RIGHT.
     *  • If med.expiryDate == node.key → append to existing node's list.
     *
     * Complexity: O(log n) average, O(n) worst-case (unbalanced tree).
     *
     * @param node Current BST node (root on first call).
     * @param med  Medicine to insert.
     * @return Updated root node.
     */
    private BSTNode bstInsert(BSTNode node, Medicine med) {
        if (node == null) {
            // Base case: empty subtree → create new leaf node
            List<Medicine> list = new ArrayList<>();
            list.add(med);
            return new BSTNode(med.expiryDate, list);
        }

        int cmp = med.expiryDate.compareTo(node.key);

        if (cmp < 0) {
            // Medicine expires BEFORE this node → go LEFT
            node.left = bstInsert(node.left, med);
        } else if (cmp > 0) {
            // Medicine expires AFTER this node → go RIGHT
            node.right = bstInsert(node.right, med);
        } else {
            // Same expiry date → add to this node's list
            node.medicines.add(med);
        }

        return node;
    }

    /**
     * BST IN-ORDER TRAVERSAL — Left → Node → Right.
     * Visits nodes in ascending date order (chronological).
     * Prints each medicine row with a status tag.
     *
     * Complexity: O(n) — visits every node exactly once.
     *
     * @param node Current BST node.
     */
    private void inOrderTraversal(BSTNode node) {
        if (node == null) return;

        inOrderTraversal(node.left);          // ← visit left subtree first

        for (Medicine med : node.medicines) { // ← process this node
            System.out.println(med.toTableRow(getStatusTag(med)));
        }

        inOrderTraversal(node.right);         // → visit right subtree last
    }

    /**
     * Rebuilds the entire manual BST from the current TreeMap state.
     * Called after a remove operation because in-place BST deletion is
     * complex and the project scope focuses on insert and traversal.
     *
     * Complexity: O(n log n) rebuild — acceptable for interactive CLI.
     *
     * @return New BST root (or null if inventory is empty).
     */
    private BSTNode rebuildBST() {
        BSTNode root = null;
        for (List<Medicine> bucket : expiryIndex.values()) {
            for (Medicine med : bucket) {
                root = bstInsert(root, med);
            }
        }
        return root;
    }


    // =========================================================================
    // PRIVATE HELPERS — Alert & Display Utilities
    // =========================================================================

    /**
     * Checks a single medicine against both alert thresholds and prints
     * any relevant warnings inline (used immediately after add/update).
     *
     * @param med Medicine to evaluate.
     */
    private void checkAndPrintAlerts(Medicine med) {
        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), med.expiryDate);

        if (daysLeft < 0) {
            System.out.println("  ❌ ALERT: '" + med.name + "' has ALREADY EXPIRED!");
        } else if (daysLeft <= AppConstants.EXPIRY_ALERT_DAYS) {
            System.out.println("  ⚠  ALERT: '" + med.name + "' expires in "
                    + daysLeft + " days (" + med.expiryDate + ").");
        }

        if (med.quantity < AppConstants.LOW_STOCK_THRESHOLD) {
            System.out.println("  ⚠  ALERT: '" + med.name
                    + "' is low on stock — only " + med.quantity + " units remaining.");
        }
    }

    /**
     * Returns a concise status tag string for a medicine row.
     *
     * @param med The medicine to evaluate.
     * @return Status string (e.g. "✅ OK", "⚠ 15 days left", "❌ EXPIRED").
     */
    private String getStatusTag(Medicine med) {
        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), med.expiryDate);
        String expiryTag;

        if (daysLeft < 0) {
            expiryTag = "❌ EXPIRED";
        } else if (daysLeft <= AppConstants.EXPIRY_ALERT_DAYS) {
            expiryTag = "⚠ " + daysLeft + "d left";
        } else {
            expiryTag = "✅ OK";
        }

        if (med.quantity < AppConstants.LOW_STOCK_THRESHOLD) {
            expiryTag += " | 📉 LOW STOCK";
        }

        return expiryTag;
    }

    /**
     * Prints a styled success message to console.
     * @param msg The success message text.
     */
    private static void printSuccess(String msg) {
        System.out.println("\n  ✅ " + msg);
    }

    /**
     * Prints a styled error message to console.
     * @param msg The error message text.
     */
    private static void printError(String msg) {
        System.out.println("\n  ❌ ERROR: " + msg);
    }
}
