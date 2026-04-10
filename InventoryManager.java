// =============================================================================
// FILE 2: InventoryManager.java  --  IMPLEMENTATION (Logic / Business Layer)
// =============================================================================
// Purpose  : Implements the InventoryService interface declared in Medicine.java.
//            Contains ALL data-structure logic:
//
//              1. HashMap<String, Medicine>
//                    Key   = lowercase medicine name
//                    Value = Medicine object
//                    Complexity: O(1) average for insert / search / delete
//
//              2. B-Tree  (order 3, hand-written)
//                    Key   = LocalDate (expiry date)
//                    Value = List<Medicine> per node key
//                    Complexity: O(log n) insert / search; O(n) full traversal
//                    Self-balancing via node splitting -- no degenerate cases
//
// Key Operation Complexities
// --------------------------
//   addMedicine      : O(1) HashMap  +  O(log n) B-Tree insert
//   searchByName     : O(1) HashMap get
//   updateQuantity   : O(1) HashMap get + in-place field mutation
//   removeMedicine   : O(1) HashMap remove  +  O(n log n) B-Tree rebuild
//   showAllByExpiry  : O(n) B-Tree in-order traversal
//   showExpiringSoon : O(k log n) -- k results collected via B-Tree range scan
//   showLowStock     : O(n) HashMap value scan
//   showAlerts       : O(n) combined scan
//   showStats        : O(n) single pass  +  O(log n) B-Tree min/max query
//
// Project   : Medicine Inventory Tracker -- DSA Project
// Team      : Aakanksha (2501) . Apa (2512) . Brandon (2514)
//             Chetan (2516)   . Sherine (2544)
// =============================================================================

package MedicineInventory;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * InventoryManager -- concrete implementation of InventoryService.
 *
 * Two data structures are maintained in lock-step:
 *
 *   nameIndex  (HashMap)
 *     - Primary lookup structure.
 *     - Provides O(1) access by medicine name.
 *     - Stores the authoritative Medicine object reference.
 *
 *   btRoot  (B-Tree root node)
 *     - Secondary sorted structure, keyed on expiry date.
 *     - Provides O(log n) ordered access and range queries.
 *     - The same Medicine object references stored in nameIndex are
 *       also held inside the B-Tree, so a quantity update via nameIndex
 *       is automatically visible when the B-Tree is traversed.
 */
public class InventoryManager implements InventoryService {

    // -------------------------------------------------------------------------
    // DSA STRUCTURE 1: HashMap  -- name -> Medicine
    // -------------------------------------------------------------------------

    /**
     * Primary lookup index.
     * Key   = medicine name, lower-cased (case-insensitive lookups).
     * Value = Medicine object (shared reference with B-Tree).
     */
    private final Map<String, Medicine> nameIndex = new HashMap<>();

    // -------------------------------------------------------------------------
    // DSA STRUCTURE 2: B-Tree  -- expiry date -> [Medicines]
    // -------------------------------------------------------------------------

    /**
     * Root of the B-Tree expiry index.
     * Null only when the inventory is completely empty.
     *
     * B-Tree order = AppConstants.BTREE_ORDER = 3.
     *   Max keys per node   : 2
     *   Max children        : 3
     *   Guaranteed height   : O(log n)
     *
     * Invariants maintained by insert / split / rebuild:
     *   (a) Every node holds at most (ORDER-1) keys.
     *   (b) Every non-root node holds at least ceil(ORDER/2)-1 keys.
     *   (c) All leaf nodes are at the same depth.
     *   (d) Keys within each node are sorted in ascending order.
     */
    private BTreeNode btRoot = null;


    // =========================================================================
    // ADD MEDICINE
    // =========================================================================

    /**
     * Inserts a new medicine into both data structures.
     *
     * Algorithm
     * ---------
     *  1. Validate inputs (name not blank, quantity >= 0).
     *  2. Reject duplicates (name already exists in HashMap).
     *  3. Parse expiry date; construct Medicine object.
     *  4. HashMap put  : O(1).
     *  5. B-Tree insert: O(log n) -- may trigger node splits upward.
     *  6. Print post-add alerts if thresholds are breached.
     *
     * @param name       Medicine name (any case; normalised internally).
     * @param batchNo    Batch identifier string.
     * @param quantity   Initial stock count (must be >= 0).
     * @param expiryDate ISO-8601 date string "YYYY-MM-DD".
     */
    @Override
    public void addMedicine(String name, String batchNo, int quantity, String expiryDate) {

        // -- Input validation --------------------------------------------------
        if (name == null || name.isBlank()) {
            printError("Medicine name cannot be empty.");
            return;
        }
        if (quantity < 0) {
            printError("Quantity cannot be negative.");
            return;
        }

        // -- Duplicate check ---------------------------------------------------
        String key = name.trim().toLowerCase();
        if (nameIndex.containsKey(key)) {
            printError("'" + name.trim() + "' already exists. "
                    + "Use option 3 (Update Quantity) to adjust stock.");
            return;
        }

        // -- Parse & construct -------------------------------------------------
        Medicine med;
        try {
            med = new Medicine(name, batchNo, quantity, expiryDate);
        } catch (DateTimeParseException e) {
            printError("Invalid date format. Expected: " + AppConstants.DATE_FORMAT);
            return;
        }

        // -- Step 1: HashMap insert  O(1) --------------------------------------
        nameIndex.put(med.getKey(), med);

        // -- Step 2: B-Tree insert  O(log n) -----------------------------------
        btRoot = btInsert(btRoot, med);

        printSuccess("Medicine '" + med.name + "' added successfully.");
        printAlertsInline(med);
    }


    // =========================================================================
    // SEARCH BY NAME  (HashMap -- O(1))
    // =========================================================================

    /**
     * Performs an O(1) average-case HashMap lookup by medicine name.
     * Displays a single-row result table if found, or an error if not.
     *
     * @param searchName Medicine name to look up (case-insensitive).
     */
    @Override
    public void searchByName(String searchName) {
        if (searchName == null || searchName.isBlank()) {
            printError("Search name cannot be empty.");
            return;
        }

        // HashMap get: O(1) average
        Medicine med = nameIndex.get(searchName.trim().toLowerCase());

        if (med != null) {
            System.out.println();
            System.out.println("  SEARCH RESULT  [HashMap lookup -- O(1)]");
            System.out.println(AppConstants.SEP_HEAVY);
            System.out.println(AppConstants.TABLE_HEADER);
            System.out.println(AppConstants.SEP_LIGHT);
            System.out.println(med.toTableRow(getStatusTag(med)));
            System.out.println(AppConstants.SEP_HEAVY);
        } else {
            printError("Medicine '" + searchName.trim() + "' not found in inventory.");
        }
    }


    // =========================================================================
    // UPDATE QUANTITY
    // =========================================================================

    /**
     * Adjusts the stock quantity of an existing medicine in O(1).
     *
     * Because both nameIndex and the B-Tree hold the SAME Medicine object
     * reference, updating the quantity field in place is sufficient --
     * no structural changes to either data structure are required.
     *
     * @param name        Medicine name (case-insensitive).
     * @param newQuantity New stock count (must be >= 0).
     */
    @Override
    public void updateQuantity(String name, int newQuantity) {
        if (newQuantity < 0) {
            printError("Quantity cannot be negative.");
            return;
        }

        // HashMap lookup: O(1)
        Medicine med = nameIndex.get(name.trim().toLowerCase());
        if (med == null) {
            printError("Medicine '" + name.trim() + "' not found.");
            return;
        }

        int prev = med.quantity;
        med.quantity = newQuantity;   // in-place; shared reference updates B-Tree too

        printSuccess("Quantity updated for '" + med.name
                + "':  " + prev + "  ->  " + newQuantity + " units.");
        printAlertsInline(med);
    }


    // =========================================================================
    // REMOVE MEDICINE
    // =========================================================================

    /**
     * Removes a medicine from the inventory.
     *
     * Algorithm
     * ---------
     *  1. HashMap remove: O(1).
     *  2. B-Tree rebuild from remaining HashMap entries: O(n log n).
     *     Full B-Tree deletion is complex (borrow / merge rotations).
     *     A rebuild from the source-of-truth HashMap is equivalent in
     *     correctness and acceptable for an interactive CLI workload.
     *
     * @param name Medicine name to remove (case-insensitive).
     */
    @Override
    public void removeMedicine(String name) {
        String key = name.trim().toLowerCase();

        // HashMap remove: O(1)
        Medicine med = nameIndex.remove(key);
        if (med == null) {
            printError("Medicine '" + name.trim() + "' not found.");
            return;
        }

        // B-Tree rebuild from remaining entries: O(n log n)
        btRoot = buildBTreeFromMap(nameIndex);

        printSuccess("Medicine '" + med.name + "' removed from inventory.");
    }


    // =========================================================================
    // SHOW ALL BY EXPIRY  (B-Tree in-order traversal -- O(n))
    // =========================================================================

    /**
     * Displays the full inventory sorted chronologically by expiry date.
     *
     * Uses B-Tree in-order traversal:
     *   visit leftmost child, then process each (key, entries) pair
     *   interleaved with its right child -- produces ascending date order.
     *
     * Complexity: O(n) -- each medicine is visited exactly once.
     */
    @Override
    public void showAllByExpiry() {
        if (nameIndex.isEmpty()) {
            System.out.println("\n  Inventory is currently empty.");
            return;
        }

        System.out.println();
        System.out.println("  FULL INVENTORY  [B-Tree in-order traversal -- O(n)]");
        System.out.println(AppConstants.SEP_HEAVY);
        System.out.println(AppConstants.TABLE_HEADER);
        System.out.println(AppConstants.SEP_LIGHT);

        btInOrder(btRoot);

        System.out.println(AppConstants.SEP_HEAVY);
        System.out.printf("  Total medicines: %d%n", nameIndex.size());
    }


    // =========================================================================
    // SHOW EXPIRING SOON  (B-Tree range scan -- O(k log n))
    // =========================================================================

    /**
     * Displays medicines expiring within EXPIRY_ALERT_DAYS days,
     * in ascending expiry date order (earliest first).
     *
     * Algorithm
     * ---------
     *  Collect matching medicines via a B-Tree range scan that stops
     *  descending into subtrees whose minimum key exceeds the threshold.
     *  Results are collected into a list that is already sorted because
     *  B-Tree in-order produces ascending date order.
     *
     * Complexity: O(k log n) where k = medicines within the alert window.
     */
    @Override
    public void showExpiringSoon() {
        LocalDate today     = LocalDate.now();
        LocalDate threshold = today.plusDays(AppConstants.EXPIRY_ALERT_DAYS);

        // Collect into a list via B-Tree range traversal (already date-sorted)
        List<Medicine> result = new ArrayList<>();
        btCollectRange(btRoot, today.minusDays(1), threshold, result);
        // btCollectRange includes dates > lowerBound, so pass yesterday to
        // include today's expiry as well (medicines expiring today need attention)

        if (result.isEmpty()) {
            System.out.println("\n  No medicines are expiring within the next "
                    + AppConstants.EXPIRY_ALERT_DAYS + " days.");
            return;
        }

        System.out.println();
        System.out.println("  EXPIRING WITHIN " + AppConstants.EXPIRY_ALERT_DAYS
                + " DAYS  (sorted by expiry date, earliest first)");
        System.out.println(AppConstants.SEP_HEAVY);
        System.out.println(AppConstants.TABLE_HEADER);
        System.out.println(AppConstants.SEP_LIGHT);

        for (Medicine med : result) {
            long days = ChronoUnit.DAYS.between(today, med.expiryDate);
            String tag;
            if (days < 0) {
                tag = "EXPIRED  (" + Math.abs(days) + " days ago)";
            } else if (days == 0) {
                tag = "EXPIRES TODAY";
            } else {
                tag = "EXPIRES IN " + days + " DAYS";
            }
            if (med.quantity < AppConstants.LOW_STOCK_THRESHOLD) {
                tag += "  |  LOW STOCK";
            }
            System.out.println(med.toTableRow(tag));
        }

        System.out.println(AppConstants.SEP_HEAVY);
        System.out.printf("  Found: %d medicine(s) in alert window.%n", result.size());
    }


    // =========================================================================
    // SHOW LOW STOCK  (HashMap scan -- O(n))
    // =========================================================================

    /**
     * Displays all medicines with quantity below LOW_STOCK_THRESHOLD.
     * Requires a full O(n) scan of HashMap values because the map is
     * keyed by name, not by quantity.
     */
    @Override
    public void showLowStock() {
        System.out.println();
        System.out.println("  LOW STOCK REPORT  (threshold: < "
                + AppConstants.LOW_STOCK_THRESHOLD + " units)");
        System.out.println(AppConstants.SEP_HEAVY);
        System.out.println(AppConstants.TABLE_HEADER);
        System.out.println(AppConstants.SEP_LIGHT);

        int count = 0;
        // O(n) scan -- no shortcut available for quantity-based filtering
        for (Medicine med : nameIndex.values()) {
            if (med.quantity < AppConstants.LOW_STOCK_THRESHOLD) {
                System.out.println(med.toTableRow(
                        "LOW STOCK  (" + med.quantity + " units)"));
                count++;
            }
        }

        if (count == 0) {
            System.out.println("  All medicines meet the minimum stock threshold.");
        }
        System.out.println(AppConstants.SEP_HEAVY);
        if (count > 0) {
            System.out.printf("  Found: %d medicine(s) below threshold.%n", count);
        }
    }


    // =========================================================================
    // SHOW ALERTS  (combined dashboard -- O(n))
    // =========================================================================

    /**
     * Prints a combined alert dashboard covering:
     *   - Medicines that have already expired.
     *   - Medicines expiring within EXPIRY_ALERT_DAYS days.
     *   - Medicines below LOW_STOCK_THRESHOLD units.
     *   - Medicines triggering both expiry and stock alerts simultaneously.
     *
     * Complexity: O(n) single pass over HashMap values.
     */
    @Override
    public void showAlerts() {
        LocalDate today     = LocalDate.now();
        LocalDate threshold = today.plusDays(AppConstants.EXPIRY_ALERT_DAYS);

        List<String> expired       = new ArrayList<>();
        List<String> expiringSoon  = new ArrayList<>();
        List<String> lowStock      = new ArrayList<>();

        for (Medicine med : nameIndex.values()) {
            long daysLeft = ChronoUnit.DAYS.between(today, med.expiryDate);
            String row    = med.toString();

            if (daysLeft < 0) {
                expired.add(row + "   [" + Math.abs(daysLeft) + " days past expiry]");
            } else if (daysLeft <= AppConstants.EXPIRY_ALERT_DAYS) {
                expiringSoon.add(row
                        + (daysLeft == 0 ? "   [EXPIRES TODAY]"
                                         : "   [" + daysLeft + " days remaining]"));
            }
            if (med.quantity < AppConstants.LOW_STOCK_THRESHOLD) {
                lowStock.add(row);
            }
        }

        System.out.println();
        System.out.println("  ALERT DASHBOARD");
        System.out.println(AppConstants.SEP_HEAVY);

        System.out.println("  [1] EXPIRED  (" + expired.size() + " medicine(s))");
        System.out.println(AppConstants.SEP_LIGHT);
        if (expired.isEmpty()) {
            System.out.println("      None.");
        } else {
            for (String s : expired) System.out.println("      " + s);
        }

        System.out.println();
        System.out.println("  [2] EXPIRING WITHIN " + AppConstants.EXPIRY_ALERT_DAYS
                + " DAYS  (" + expiringSoon.size() + " medicine(s))");
        System.out.println(AppConstants.SEP_LIGHT);
        if (expiringSoon.isEmpty()) {
            System.out.println("      None.");
        } else {
            for (String s : expiringSoon) System.out.println("      " + s);
        }

        System.out.println();
        System.out.println("  [3] LOW STOCK  (< " + AppConstants.LOW_STOCK_THRESHOLD
                + " units)  (" + lowStock.size() + " medicine(s))");
        System.out.println(AppConstants.SEP_LIGHT);
        if (lowStock.isEmpty()) {
            System.out.println("      None.");
        } else {
            for (String s : lowStock) System.out.println("      " + s);
        }

        System.out.println(AppConstants.SEP_HEAVY);
        int total = expired.size() + expiringSoon.size() + lowStock.size();
        if (total == 0) {
            System.out.println("  Status: No active alerts. Inventory is healthy.");
        } else {
            System.out.printf("  Total alerts raised: %d%n", total);
        }
    }


    // =========================================================================
    // SHOW STATS  (O(n) scan + O(log n) B-Tree queries)
    // =========================================================================

    /**
     * Displays a statistics dashboard for the current inventory.
     *
     * Statistics reported
     * -------------------
     *   Total medicines       : nameIndex.size()
     *   Total stock units     : sum of all quantities  (O(n) scan)
     *   Next expiry date      : earliest FUTURE expiry in B-Tree (O(log n))
     *   Next-to-expire name   : name of that medicine
     *   Latest expiry date    : rightmost B-Tree key  (O(log n))
     *   Expired medicines     : count where expiryDate < today  (O(n) scan)
     *   Expiring soon         : count within alert window       (O(n) scan)
     *   Low-stock medicines   : count below threshold           (O(n) scan)
     *
     * NOTE on "Next Expiry":
     *   The B-Tree's leftmost key is the globally earliest date, which may be
     *   a past (expired) date.  "Next Expiry" intentionally reports the first
     *   FUTURE date so the statistic is actionable for inventory management.
     *   "Earliest date on record" is shown separately for completeness.
     */
    @Override
    public void showStats() {
        if (nameIndex.isEmpty()) {
            System.out.println("\n  Inventory is empty -- no statistics available.");
            return;
        }

        LocalDate today = LocalDate.now();

        int  totalUnits      = 0;
        int  expiredCount    = 0;
        int  expiringSoon    = 0;
        int  lowStockCount   = 0;

        // -- O(n) single pass over HashMap values ----------------------------
        for (Medicine med : nameIndex.values()) {
            totalUnits += med.quantity;

            long daysLeft = ChronoUnit.DAYS.between(today, med.expiryDate);
            if (daysLeft < 0) {
                expiredCount++;
            } else if (daysLeft <= AppConstants.EXPIRY_ALERT_DAYS) {
                expiringSoon++;
            }
            if (med.quantity < AppConstants.LOW_STOCK_THRESHOLD) {
                lowStockCount++;
            }
        }

        // -- B-Tree min / max queries: O(log n) each -------------------------
        // Leftmost leaf key  = earliest date on record (may be past)
        LocalDate earliestOnRecord = btMinKey(btRoot);

        // First FUTURE key   = next medicine to expire from today onward
        // Collected via B-Tree range scan starting from today
        List<Medicine> futureList = new ArrayList<>();
        btCollectRange(btRoot, today.minusDays(1), LocalDate.MAX, futureList);

        String nextExpiryDate = futureList.isEmpty()
                ? "N/A (all medicines expired)"
                : futureList.get(0).expiryDate.toString();
        String nextExpiryName = futureList.isEmpty()
                ? "-"
                : futureList.get(0).name;

        // Rightmost leaf key = latest expiry date on record
        LocalDate latestOnRecord = btMaxKey(btRoot);

        // -- Print dashboard -------------------------------------------------
        System.out.println();
        System.out.println("  INVENTORY STATISTICS");
        System.out.println(AppConstants.SEP_HEAVY);
        System.out.printf("  %-32s : %d%n",  "Total Medicines",        nameIndex.size());
        System.out.printf("  %-32s : %d units%n", "Total Stock Units",  totalUnits);
        System.out.println(AppConstants.SEP_LIGHT);
        System.out.printf("  %-32s : %s%n",  "Earliest Date on Record",earliestOnRecord);
        System.out.printf("  %-32s : %s%n",  "Next Expiry Date (future)", nextExpiryDate);
        System.out.printf("  %-32s : %s%n",  "Next to Expire",         nextExpiryName);
        System.out.printf("  %-32s : %s%n",  "Latest Expiry on Record",latestOnRecord);
        System.out.println(AppConstants.SEP_LIGHT);
        System.out.printf("  %-32s : %d%n",  "Expired Medicines",      expiredCount);
        System.out.printf("  %-32s : %d%n",  "Expiring Within Alert Window", expiringSoon);
        System.out.printf("  %-32s : %d%n",  "Low-Stock Medicines",    lowStockCount);
        System.out.println(AppConstants.SEP_LIGHT);
        System.out.printf("  %-32s : %d days%n", "Expiry Alert Window", AppConstants.EXPIRY_ALERT_DAYS);
        System.out.printf("  %-32s : %d units%n","Low-Stock Threshold", AppConstants.LOW_STOCK_THRESHOLD);
        System.out.println(AppConstants.SEP_HEAVY);
    }

    public Medicine findMedicineByName(String name) {
        if (name == null || name.isBlank()) return null;
        return nameIndex.get(name.trim().toLowerCase());
    }

    public List<Medicine> getAllMedicinesSortedByExpiry() {
        List<Medicine> result = new ArrayList<>();
        btInOrderCollect(btRoot, result);
        return result;
    }

    public List<Medicine> getExpiringSoonList() {
        LocalDate today     = LocalDate.now();
        LocalDate threshold = today.plusDays(AppConstants.EXPIRY_ALERT_DAYS);
        List<Medicine> result = new ArrayList<>();
        btCollectRange(btRoot, today.minusDays(1), threshold, result);
        return result;
    }

    public List<Medicine> getLowStockList() {
        List<Medicine> lowStock = new ArrayList<>();
        for (Medicine med : nameIndex.values()) {
            if (med.quantity < AppConstants.LOW_STOCK_THRESHOLD) {
                lowStock.add(med);
            }
        }
        return lowStock;
    }

    public Map<String, List<Medicine>> getAlertsDashboard() {
        LocalDate today     = LocalDate.now();
        LocalDate threshold = today.plusDays(AppConstants.EXPIRY_ALERT_DAYS);

        List<Medicine> expired = new ArrayList<>();
        List<Medicine> expiringSoon = new ArrayList<>();
        List<Medicine> lowStock = new ArrayList<>();

        for (Medicine med : nameIndex.values()) {
            long daysLeft = ChronoUnit.DAYS.between(today, med.expiryDate);
            if (daysLeft < 0) {
                expired.add(med);
            } else if (daysLeft <= AppConstants.EXPIRY_ALERT_DAYS) {
                expiringSoon.add(med);
            }
            if (med.quantity < AppConstants.LOW_STOCK_THRESHOLD) {
                lowStock.add(med);
            }
        }

        Map<String, List<Medicine>> alerts = new HashMap<>();
        alerts.put("expired", expired);
        alerts.put("expiringSoon", expiringSoon);
        alerts.put("lowStock", lowStock);
        return alerts;
    }

    public Map<String, Object> getInventoryStats() {
    Map<String, Object> stats = new HashMap<>();
    if (nameIndex.isEmpty()) {
        stats.put("totalMedicines", 0);
        stats.put("totalStockUnits", 0);
        stats.put("earliestOnRecord", "N/A");
        stats.put("nextExpiryDate", "N/A");
        stats.put("nextExpiryName", "N/A");
        stats.put("latestOnRecord", "N/A");
        stats.put("expiredCount", 0);
        stats.put("expiringSoonCount", 0);
        stats.put("lowStockCount", 0);
        return stats;
    }

    LocalDate today = LocalDate.now();
    int totalUnits = 0;
    int expiredCount = 0;
    int expiringSoon = 0;
    int lowStockCount = 0;

    for (Medicine med : nameIndex.values()) {
        totalUnits += med.quantity;
        long daysLeft = ChronoUnit.DAYS.between(today, med.expiryDate);
        if (daysLeft < 0) {
            expiredCount++;
        } else if (daysLeft <= AppConstants.EXPIRY_ALERT_DAYS) {
            expiringSoon++;
        }
        if (med.quantity < AppConstants.LOW_STOCK_THRESHOLD) {
            lowStockCount++;
        }
    }

    // Safe B-Tree min/max – handle null
    LocalDate earliestOnRecord = btMinKey(btRoot);
    LocalDate latestOnRecord = btMaxKey(btRoot);
    
    List<Medicine> futureList = new ArrayList<>();
    btCollectRange(btRoot, today.minusDays(1), LocalDate.MAX, futureList);

    String nextExpiryDate = futureList.isEmpty() 
        ? "N/A (all medicines expired)" 
        : futureList.get(0).expiryDate.toString();
    String nextExpiryName = futureList.isEmpty() 
        ? "-" 
        : futureList.get(0).name;

    stats.put("totalMedicines", nameIndex.size());
    stats.put("totalStockUnits", totalUnits);
    stats.put("earliestOnRecord", earliestOnRecord != null ? earliestOnRecord.toString() : "N/A");
    stats.put("nextExpiryDate", nextExpiryDate);
    stats.put("nextExpiryName", nextExpiryName);
    stats.put("latestOnRecord", latestOnRecord != null ? latestOnRecord.toString() : "N/A");
    stats.put("expiredCount", expiredCount);
    stats.put("expiringSoonCount", expiringSoon);
    stats.put("lowStockCount", lowStockCount);
    return stats;
}

    

    // public Map<String, Object> getInventoryStats() {
    //     Map<String, Object> stats = new HashMap<>();
    //     if (nameIndex.isEmpty()) {
    //         stats.put("totalMedicines", 0);
    //         stats.put("totalStockUnits", 0);
    //         stats.put("earliestOnRecord", "N/A");
    //         stats.put("nextExpiryDate", "N/A");
    //         stats.put("nextExpiryName", "N/A");
    //         stats.put("latestOnRecord", "N/A");
    //         stats.put("expiredCount", 0);
    //         stats.put("expiringSoonCount", 0);
    //         stats.put("lowStockCount", 0);
    //         return stats;
    //     }

    //     LocalDate today = LocalDate.now();
    //     int totalUnits = 0;
    //     int expiredCount = 0;
    //     int expiringSoon = 0;
    //     int lowStockCount = 0;

    //     for (Medicine med : nameIndex.values()) {
    //         totalUnits += med.quantity;
    //         long daysLeft = ChronoUnit.DAYS.between(today, med.expiryDate);
    //         if (daysLeft < 0) {
    //             expiredCount++;
    //         } else if (daysLeft <= AppConstants.EXPIRY_ALERT_DAYS) {
    //             expiringSoon++;
    //         }
    //         if (med.quantity < AppConstants.LOW_STOCK_THRESHOLD) {
    //             lowStockCount++;
    //         }
    //     }

    //     LocalDate earliestOnRecord = btMinKey(btRoot);
    //     List<Medicine> futureList = new ArrayList<>();
    //     btCollectRange(btRoot, today.minusDays(1), LocalDate.MAX, futureList);

    //     String nextExpiryDate = futureList.isEmpty() ? "N/A (all medicines expired)" : futureList.get(0).expiryDate.toString();
    //     String nextExpiryName = futureList.isEmpty() ? "-" : futureList.get(0).name;
    //     LocalDate latestOnRecord = btMaxKey(btRoot);

    //     stats.put("totalMedicines", nameIndex.size());
    //     stats.put("totalStockUnits", totalUnits);
    //     stats.put("earliestOnRecord", earliestOnRecord.toString());
    //     stats.put("nextExpiryDate", nextExpiryDate);
    //     stats.put("nextExpiryName", nextExpiryName);
    //     stats.put("latestOnRecord", latestOnRecord.toString());
    //     stats.put("expiredCount", expiredCount);
    //     stats.put("expiringSoonCount", expiringSoon);
    //     stats.put("lowStockCount", lowStockCount);
    //     return stats;
    // }

    private void btInOrderCollect(BTreeNode node, List<Medicine> result) {
        if (node == null) return;
        int k = node.keys.size();
        for (int i = 0; i < k; i++) {
            if (!node.isLeaf) {
                btInOrderCollect(node.children.get(i), result);
            }
            result.addAll(node.entries.get(i));
        }
        if (!node.isLeaf) {
            btInOrderCollect(node.children.get(k), result);
        }
    }


    // =========================================================================
    // B-TREE OPERATIONS -- Private Implementation
    // =========================================================================

    // -------------------------------------------------------------------------
    // btInsert  --  public entry point for B-Tree insertion
    // -------------------------------------------------------------------------

    /**
     * Inserts a medicine into the B-Tree, returning the (possibly new) root.
     *
     * If the root is full before insertion, the tree must grow in height:
     *   1. Create a new empty root node.
     *   2. Make the old root the new root's first child.
     *   3. Split the old root (which is now child 0 of the new root).
     *   4. Insert into the correct half.
     *
     * Otherwise, delegate directly to btInsertNonFull().
     *
     * Complexity: O(log n) -- traverses at most one path root-to-leaf.
     *
     * @param root Current tree root (may be null for empty tree).
     * @param med  Medicine to insert.
     * @return New tree root after insertion.
     */
    private BTreeNode btInsert(BTreeNode root, Medicine med) {
        if (root == null) {
            // Empty tree: create the first leaf node
            BTreeNode newRoot = new BTreeNode(true);
            newRoot.keys.add(med.expiryDate);
            List<Medicine> bucket = new ArrayList<>();
            bucket.add(med);
            newRoot.entries.add(bucket);
            return newRoot;
        }

        if (root.isFull()) {
            // Root is full: grow tree height by one level
            BTreeNode newRoot = new BTreeNode(false);
            newRoot.children.add(root);             // old root becomes first child
            btSplitChild(newRoot, 0);               // split it and push median up
            btInsertNonFull(newRoot, med);           // insert into correct half
            return newRoot;
        }

        btInsertNonFull(root, med);
        return root;
    }

    // -------------------------------------------------------------------------
    // btInsertNonFull  --  insert into a node guaranteed not to be full
    // -------------------------------------------------------------------------

    /**
     * Inserts a medicine into the subtree rooted at {@code node}, which is
     * guaranteed to have at least one free key slot.
     *
     * Algorithm
     * ---------
     *  1. Find the index i where med.expiryDate belongs.
     *  2. If med.expiryDate == keys[i] (duplicate date): append to bucket.
     *  3. If node is a leaf: insert at position i.
     *  4. If node is internal:
     *       a. If child[i] is full: split it first (pushes median up).
     *       b. After split, re-check which child to descend into.
     *       c. Recurse into the correct child.
     *
     * Complexity: O(log n) -- one recursive call per tree level.
     *
     * @param node Non-full node to insert into.
     * @param med  Medicine to insert.
     */
    private void btInsertNonFull(BTreeNode node, Medicine med) {
        int i = node.findKeyIndex(med.expiryDate);

        // Exact date already exists in this node -- append to bucket
        if (i < node.keys.size()
                && med.expiryDate.compareTo(node.keys.get(i)) == 0) {
            node.entries.get(i).add(med);
            return;
        }

        if (node.isLeaf) {
            // Leaf: insert key and bucket at position i
            node.keys.add(i, med.expiryDate);
            List<Medicine> bucket = new ArrayList<>();
            bucket.add(med);
            node.entries.add(i, bucket);
        } else {
            // Internal node: descend into child[i], splitting if necessary
            if (node.children.get(i).isFull()) {
                btSplitChild(node, i);
                // After split, the median key was promoted to node.keys[i].
                // Determine which of the two resulting children to descend.
                if (med.expiryDate.compareTo(node.keys.get(i)) > 0) {
                    i++;
                } else if (med.expiryDate.compareTo(node.keys.get(i)) == 0) {
                    // Median date matches: append to the promoted bucket
                    node.entries.get(i).add(med);
                    return;
                }
            }
            btInsertNonFull(node.children.get(i), med);
        }
    }

    // -------------------------------------------------------------------------
    // btSplitChild  --  split a full child and push its median key up
    // -------------------------------------------------------------------------

    /**
     * Splits child[idx] of {@code parent} into two nodes and promotes the
     * median key into {@code parent}.
     *
     * Before split (child has ORDER-1 = 2 keys, indices 0..1):
     *   child: [k0 | k1]  children: [c0 | c1 | c2]
     *
     * After split  (ORDER = 3, split at index t-1 = 0):
     *   left  (stays at child[idx]) : [k0]       children: [c0 | c1]
     *   right (new node, child[idx+1]) : [k1]   children: [c2]
     *   parent gains k_mid (the median) at keys[idx]
     *
     * Complexity: O(ORDER) = O(1) for fixed order.
     *
     * @param parent The parent node whose child will be split.
     * @param idx    Index of the full child in parent.children.
     */
    private void btSplitChild(BTreeNode parent, int idx) {
        int t         = AppConstants.BTREE_ORDER;   // order = 3
        int medianIdx = t / 2 - 1;                  // index of median in full child (= 0 for t=3)

        BTreeNode fullChild = parent.children.get(idx);
        BTreeNode newChild  = new BTreeNode(fullChild.isLeaf);

        // -- Promote median key and its bucket into parent ------------------
        LocalDate       medianKey    = fullChild.keys.get(medianIdx);
        List<Medicine>  medianBucket = fullChild.entries.get(medianIdx);

        parent.keys.add(idx, medianKey);
        parent.entries.add(idx, medianBucket);
        parent.children.add(idx + 1, newChild);

        // -- Move keys/entries after median into newChild -------------------
        // For t=3: fullChild originally has indices 0,1; median = 0; right = [1]
        while (fullChild.keys.size() > medianIdx + 1) {
            newChild.keys.add(0, fullChild.keys.remove(medianIdx + 1));
            newChild.entries.add(0, fullChild.entries.remove(medianIdx + 1));
        }

        // -- Remove median from fullChild (it has been promoted) -------------
        fullChild.keys.remove(medianIdx);
        fullChild.entries.remove(medianIdx);

        // -- Move children after median into newChild (if internal node) ----
        if (!fullChild.isLeaf) {
            // children to move: those after the median (index medianIdx+1 onward)
            while (fullChild.children.size() > medianIdx + 1) {
                newChild.children.add(0,
                        fullChild.children.remove(medianIdx + 1));
            }
        }
    }

    // -------------------------------------------------------------------------
    // btInOrder  --  in-order traversal: visit left, key, right
    // -------------------------------------------------------------------------

    /**
     * Performs an in-order traversal of the B-Tree, printing each medicine
     * in ascending expiry-date order.
     *
     * For a B-Tree node with keys [k0, k1] and children [c0, c1, c2]:
     *   traverse(c0), print(k0), traverse(c1), print(k1), traverse(c2)
     *
     * Complexity: O(n) -- each medicine is visited exactly once.
     *
     * @param node Current node (null terminates recursion).
     */
    private void btInOrder(BTreeNode node) {
        if (node == null) return;

        int k = node.keys.size();

        for (int i = 0; i < k; i++) {
            // Visit left child before key i
            if (!node.isLeaf) {
                btInOrder(node.children.get(i));
            }
            // Print all medicines at key i
            for (Medicine med : node.entries.get(i)) {
                System.out.println(med.toTableRow(getStatusTag(med)));
            }
        }
        // Visit the rightmost child
        if (!node.isLeaf) {
            btInOrder(node.children.get(k));
        }
    }

    // -------------------------------------------------------------------------
    // btCollectRange  --  collect medicines with key in (lowerBound, upperBound]
    // -------------------------------------------------------------------------

    /**
     * Collects medicines whose expiry date is strictly after {@code lowerBound}
     * and on or before {@code upperBound}, in ascending date order.
     *
     * This is a B-Tree range scan: subtrees whose minimum possible key is
     * beyond upperBound are pruned, avoiding unnecessary traversal.
     *
     * Complexity: O(k log n) where k = number of results.
     *
     * @param node        Current B-Tree node.
     * @param lowerBound  Exclusive lower bound (pass today.minusDays(1) to include today).
     * @param upperBound  Inclusive upper bound.
     * @param result      List to which matching medicines are appended.
     */
    private void btCollectRange(BTreeNode node, LocalDate lowerBound,
                                LocalDate upperBound, List<Medicine> result) {
        if (node == null) return;

        int k = node.keys.size();

        for (int i = 0; i < k; i++) {
            LocalDate key = node.keys.get(i);

            // Visit left child before key i if it might contain relevant entries
            if (!node.isLeaf) {
                btCollectRange(node.children.get(i), lowerBound, upperBound, result);
            }

            // Add entries at this key if in range
            if (key.isAfter(lowerBound) && !key.isAfter(upperBound)) {
                result.addAll(node.entries.get(i));
            }

            // Prune: once the key exceeds upperBound, no further keys or right
            // children can be in range
            if (key.isAfter(upperBound)) {
                return;
            }
        }

        // Visit rightmost child
        if (!node.isLeaf) {
            btCollectRange(node.children.get(k), lowerBound, upperBound, result);
        }
    }

    // -------------------------------------------------------------------------
    // btMinKey  --  O(log n) leftmost (minimum) key
    // -------------------------------------------------------------------------

    /**
     * Returns the minimum key in the B-Tree by following left-child pointers
     * to the leftmost leaf.
     *
     * Complexity: O(log n) -- height of tree.
     *
     * @param node Tree root.
     * @return Minimum LocalDate key, or null if tree is empty.
     */
    // private LocalDate btMinKey(BTreeNode node) {
    //     if (node == null) return null;
    //     while (!node.isLeaf) {
    //         node = node.children.get(0);
    //     }
    //     return node.keys.get(0);
    // }

    private LocalDate btMinKey(BTreeNode node) {
    if (node == null) return null;
    while (!node.isLeaf) {
        if (node.children.isEmpty()) return null; // safety
        node = node.children.get(0);
    }
    if (node.keys.isEmpty()) return null;
    return node.keys.get(0);
}

private LocalDate btMaxKey(BTreeNode node) {
    if (node == null) return null;
    while (!node.isLeaf) {
        if (node.children.isEmpty()) return null;
        node = node.children.get(node.children.size() - 1);
    }
    if (node.keys.isEmpty()) return null;
    return node.keys.get(node.keys.size() - 1);
}

    // -------------------------------------------------------------------------
    // btMaxKey  --  O(log n) rightmost (maximum) key
    // -------------------------------------------------------------------------

    /**
     * Returns the maximum key in the B-Tree by following right-child pointers
     * to the rightmost leaf.
     *
     * Complexity: O(log n) -- height of tree.
     *
     * @param node Tree root.
     * @return Maximum LocalDate key, or null if tree is empty.
     */
    // private LocalDate btMaxKey(BTreeNode node) {
    //     if (node == null) return null;
    //     while (!node.isLeaf) {
    //         node = node.children.get(node.children.size() - 1);
    //     }
    //     return node.keys.get(node.keys.size() - 1);
    // }

    // -------------------------------------------------------------------------
    // buildBTreeFromMap  --  O(n log n) full rebuild from HashMap
    // -------------------------------------------------------------------------

    /**
     * Builds a fresh B-Tree by inserting every medicine in the provided map.
     * Called after a remove operation (full deletion with borrow/merge is
     * complex; rebuild is simpler and correct).
     *
     * Complexity: O(n log n) -- n insertions each costing O(log n).
     *
     * @param map Source map of medicines.
     * @return New B-Tree root (null if map is empty).
     */
    // private BTreeNode buildBTreeFromMap(Map<String, Medicine> map) {
    //     BTreeNode root = null;
    //     for (Medicine med : map.values()) {
    //         root = btInsert(root, med);
    //     }
    //     return root;
    // }

    private BTreeNode buildBTreeFromMap(Map<String, Medicine> map) {
    BTreeNode root = null;
    for (Medicine med : map.values()) {
        root = btInsert(root, med);
    }
    // If the map is not empty but root somehow is null (shouldn't happen), create a leaf with first medicine
    if (root == null && !map.isEmpty()) {
        Medicine first = map.values().iterator().next();
        root = new BTreeNode(true);
        root.keys.add(first.expiryDate);
        List<Medicine> bucket = new ArrayList<>();
        bucket.add(first);
        root.entries.add(bucket);
    }
    return root;
}


    // =========================================================================
    // PRIVATE DISPLAY HELPERS
    // =========================================================================

    /**
     * Prints inline alerts for a medicine immediately after add or update.
     * Checks both expiry proximity and stock level.
     *
     * @param med Medicine to evaluate.
     */
    private void printAlertsInline(Medicine med) {
        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), med.expiryDate);

        if (daysLeft < 0) {
            System.out.println("  ALERT: '" + med.name
                    + "' has already expired (" + Math.abs(daysLeft) + " days ago).");
        } else if (daysLeft == 0) {
            System.out.println("  ALERT: '" + med.name + "' expires TODAY.");
        } else if (daysLeft <= AppConstants.EXPIRY_ALERT_DAYS) {
            System.out.println("  ALERT: '" + med.name
                    + "' expires in " + daysLeft + " days  (" + med.expiryDate + ").");
        }

        if (med.quantity < AppConstants.LOW_STOCK_THRESHOLD) {
            System.out.println("  ALERT: '" + med.name
                    + "' is low on stock  (" + med.quantity + " units remaining).");
        }
    }

    /**
     * Produces a short status label for a medicine, used in table rows.
     *
     * Priority: EXPIRED > EXPIRING IN N DAYS > OK.
     * Low-stock suffix appended if applicable.
     *
     * @param med Medicine to evaluate.
     * @return Status string without any emoji characters.
     */
    private String getStatusTag(Medicine med) {
        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), med.expiryDate);
        String tag;

        if (daysLeft < 0) {
            tag = "EXPIRED";
        } else if (daysLeft == 0) {
            tag = "EXPIRES TODAY";
        } else if (daysLeft <= AppConstants.EXPIRY_ALERT_DAYS) {
            tag = "EXPIRING  (" + daysLeft + "d)";
        } else {
            tag = "OK";
        }

        if (med.quantity < AppConstants.LOW_STOCK_THRESHOLD) {
            tag += "  |  LOW STOCK";
        }

        return tag;
    }

    /** Prints a success message with a consistent prefix. */
    private static void printSuccess(String msg) {
        System.out.println("\n  SUCCESS: " + msg);
    }

    /** Prints an error message with a consistent prefix. */
    private static void printError(String msg) {
        System.out.println("\n  ERROR: " + msg);
    }
}
