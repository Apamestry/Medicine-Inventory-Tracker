// =============================================================================
// FILE 1: Medicine.java  --  DATA MODEL & DEFINITIONS (Header File Equivalent)
// =============================================================================
// Purpose  : Defines the core data model (Medicine), shared constants,
//            the InventoryService interface (method prototypes), and the
//            BTreeNode used by the B-Tree expiry index.
//
// Analogous to a C/C++ ".h" header: contains struct definitions, constants,
// and method prototypes -- NO business logic lives here.
//
// DSA Structures declared / documented here:
//   1. HashMap<String, Medicine>  -- O(1) average lookup by medicine name
//   2. BTree (via BTreeNode)      -- O(log n) sorted expiry index
//                                    Self-balancing; order-3 (max 2 keys/node)
//
// Project   : Medicine Inventory Tracker -- DSA Project
// Team      : Aakanksha (2501) . Apa (2512) . Brandon (2514)
//             Chetan (2516)   . Sherine (2544)
// =============================================================================

package MedicineInventory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// -----------------------------------------------------------------------------
// SECTION 1 -- CONSTANTS
// -----------------------------------------------------------------------------

/**
 * AppConstants centralises every configurable value used across the project.
 * Modify thresholds here; changes propagate automatically to all modules.
 */
final class AppConstants {

    private AppConstants() {}   // Utility class -- prevent instantiation

    /** Stock quantity below which a low-stock alert is raised. */
    public static final int    LOW_STOCK_THRESHOLD = 20;

    /** Days until expiry below which an expiry alert is raised. */
    public static final int    EXPIRY_ALERT_DAYS   = 30;

    /** Expected date input format. */
    public static final String DATE_FORMAT         = "YYYY-MM-DD";

    // -- Column widths for aligned table output --
    public static final int    COL_BATCH_WIDTH     = 12;
    public static final int    COL_NAME_WIDTH      = 18;
    public static final int    COL_QTY_WIDTH       = 6;
    public static final int    COL_DATE_WIDTH      = 12;

    /** Full-width horizontal separator for console output. */
    public static final String SEP_HEAVY =
        "=============================================================================";

    /** Mid-weight separator for sub-sections. */
    public static final String SEP_LIGHT =
        "-----------------------------------------------------------------------------";

    /** Column header row, aligned to COL_*_WIDTH constants. */
    public static final String TABLE_HEADER = String.format(
        "  %-" + COL_BATCH_WIDTH + "s  %-" + COL_NAME_WIDTH
            + "s  %-" + COL_QTY_WIDTH + "s  %-" + COL_DATE_WIDTH
            + "s  %s",
        "Batch No.", "Medicine Name", "Qty", "Expiry Date", "Status"
    );

    /**
     * B-Tree order.
     * Each node may hold at most (ORDER - 1) keys and ORDER children.
     * A node is split when it contains (ORDER - 1) keys and a new key
     * must be inserted into it.
     */
    public static final int    BTREE_ORDER         = 3;
}


// -----------------------------------------------------------------------------
// SECTION 2 -- MEDICINE (Data Model)
// -----------------------------------------------------------------------------

/**
 * Medicine -- value object representing one inventory entry.
 *
 * Fields
 * ------
 *  name       : Display name; lower-cased form is the HashMap key.
 *  batchNo    : Batch identifier (upper-cased on construction).
 *  quantity   : Current stock units; mutable via updateQuantity().
 *  expiryDate : Parsed LocalDate; serves as the B-Tree key.
 *
 * DSA roles
 * ---------
 *  HashMap : Medicine is the VALUE; name.toLowerCase() is the KEY.
 *  B-Tree  : Medicine objects are stored inside BTreeNode.entries
 *            lists, keyed on expiryDate.
 */
class Medicine {

    public final String    name;
    public final String    batchNo;
    public       int       quantity;    // mutable -- stock changes over time
    public final LocalDate expiryDate;

    /**
     * Constructs a Medicine record.
     *
     * @param name       Medicine name (trimmed; any case accepted).
     * @param batchNo    Batch identifier (trimmed; stored upper-case).
     * @param quantity   Initial stock count (caller must ensure >= 0).
     * @param expiryDate ISO-8601 date string "YYYY-MM-DD".
     * @throws java.time.format.DateTimeParseException if date is malformed.
     */
    public Medicine(String name, String batchNo, int quantity, String expiryDate) {
        this.name       = name.trim();
        this.batchNo    = batchNo.trim().toUpperCase();
        this.quantity   = quantity;
        this.expiryDate = LocalDate.parse(expiryDate.trim());
    }

    /**
     * Returns the canonical HashMap key for this medicine.
     * Always lower-case so lookups are case-insensitive.
     */
    public String getKey() {
        return name.toLowerCase();
    }

    /**
     * Formats this medicine as one aligned table row.
     *
     * @param statusTag Short status label (e.g. "EXPIRED", "OK", "LOW STOCK").
     * @return Formatted row string ready for System.out.println().
     */
    public String toTableRow(String statusTag) {
        return String.format(
            "  %-" + AppConstants.COL_BATCH_WIDTH
                + "s  %-" + AppConstants.COL_NAME_WIDTH
                + "s  %-" + AppConstants.COL_QTY_WIDTH
                + "d  %-" + AppConstants.COL_DATE_WIDTH
                + "s  %s",
            batchNo, name, quantity, expiryDate, statusTag
        );
    }

    /** Compact single-line representation used in alert messages. */
    @Override
    public String toString() {
        return String.format("[%-" + AppConstants.COL_BATCH_WIDTH
                + "s]  %-" + AppConstants.COL_NAME_WIDTH
                + "s  Qty: %-6d  Expires: %s",
                batchNo, name, quantity, expiryDate);
    }
}


// -----------------------------------------------------------------------------
// SECTION 3 -- B-TREE NODE
// -----------------------------------------------------------------------------

/**
 * BTreeNode -- a single node in the B-Tree expiry index.
 *
 * B-Tree Properties  (order T = AppConstants.BTREE_ORDER = 3)
 * ------------------------------------------------------------
 *  - Max keys per node   : T - 1  =  2
 *  - Max children        : T      =  3
 *  - Min keys (non-root) : ceil(T/2) - 1  =  1
 *  - All leaves are at the same depth  (self-balancing guarantee).
 *  - Keys within each node are in strict ascending order.
 *
 * Key   : LocalDate (expiry date of the medicines in that slot).
 * Value : List<Medicine> -- all medicines sharing that expiry date.
 *
 * Why B-Tree instead of a plain BST?
 * -----------------------------------
 *  A plain BST degrades to O(n) height when keys are inserted in sorted
 *  (or near-sorted) order.  Medicines are typically entered with expiry
 *  dates in roughly ascending order, which is the pathological case for
 *  a BST.  The B-Tree splits overflowing nodes upward, guaranteeing
 *  O(log n) for insert, search, and traversal regardless of input order.
 */
class BTreeNode {

    /**
     * Keys stored in this node (expiry dates).
     * Invariant: keys is sorted in ascending order at all times.
     */
    public List<LocalDate>       keys;

    /**
     * Parallel list of medicine buckets.
     * entries.get(i) holds all medicines whose expiry date == keys.get(i).
     */
    public List<List<Medicine>>  entries;

    /**
     * Child pointers.
     * For an internal node with k keys, children has exactly k+1 elements.
     * For a leaf node, children is empty.
     */
    public List<BTreeNode>       children;

    /** True when this node has no children (is a leaf). */
    public boolean               isLeaf;

    /** Constructs a new, empty node. */
    public BTreeNode(boolean isLeaf) {
        this.isLeaf   = isLeaf;
        this.keys     = new ArrayList<>();
        this.entries  = new ArrayList<>();
        this.children = new ArrayList<>();
    }

    /**
     * Returns true when this node has reached maximum key capacity and
     * must be split before another key can be inserted into it.
     * Capacity limit = BTREE_ORDER - 1 keys.
     */
    public boolean isFull() {
        return keys.size() == AppConstants.BTREE_ORDER - 1;
    }

    /**
     * Finds the insertion/descent index for a given date.
     * Returns the smallest index i such that date <= keys.get(i),
     * or keys.size() if date is greater than all current keys.
     *
     * Used by insert and search to identify which child subtree to visit.
     *
     * @param date The date to locate.
     * @return Index in [0, keys.size()].
     */
    public int findKeyIndex(LocalDate date) {
        int i = 0;
        while (i < keys.size() && date.compareTo(keys.get(i)) > 0) {
            i++;
        }
        return i;
    }
}


// -----------------------------------------------------------------------------
// SECTION 4 -- InventoryService INTERFACE (Method Prototypes)
// -----------------------------------------------------------------------------

/**
 * InventoryService -- contract (API) that InventoryManager must implement.
 *
 * Analogous to a C header's function prototypes: operations are declared here;
 * all implementation code lives in InventoryManager.java.
 *
 * Method summary
 * --------------
 *  addMedicine      -- insert new record into HashMap + B-Tree
 *  searchByName     -- O(1) HashMap lookup by name
 *  updateQuantity   -- adjust stock for an existing medicine
 *  removeMedicine   -- delete from HashMap; rebuild B-Tree
 *  showAllByExpiry  -- B-Tree in-order traversal (chronological)
 *  showExpiringSoon -- medicines expiring within EXPIRY_ALERT_DAYS, sorted
 *  showLowStock     -- medicines below LOW_STOCK_THRESHOLD
 *  showAlerts       -- combined alert summary
 *  showStats        -- inventory statistics dashboard
 */
interface InventoryService {

    void addMedicine(String name, String batchNo, int quantity, String expiryDate);

    void searchByName(String name);

    void updateQuantity(String name, int newQuantity);

    void removeMedicine(String name);

    void showAllByExpiry();

    void showExpiringSoon();

    void showLowStock();

    void showAlerts();

    void showStats();
}
