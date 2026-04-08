// =============================================================================
// FILE 1: Medicine.java  —  DATA MODEL & DEFINITIONS (Header File Equivalent)
// =============================================================================
// Purpose  : Defines the core data model (Medicine), shared constants,
//            the InventoryService interface (prototypes), and the BSTNode
//            used by the expiry-sorted Binary Search Tree.
//
// Analogous to a C/C++ ".h" header: contains struct definitions, constants,
// and method prototypes — NO business logic lives here.
//
// DSA Structures declared here:
//   • HashMap<String, Medicine>         → O(1) lookup by medicine name
//   • TreeMap<LocalDate,List<Medicine>> → O(log n) sorted expiry index (BST)
//
// Project   : Medicine Inventory Tracker — DSA Project
// Team      : Aakanksha (2501) · Apa (2512) · Brandon (2514)
//             Chetan (2516)   · Sherine (2544)
// =============================================================================

package MedicineInventory;

import java.time.LocalDate;
import java.util.List;

// ─────────────────────────────────────────────────────────────────────────────
// SECTION 1 ─ CONSTANTS  (shared across all files)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * AppConstants holds every magic number / string used across the project.
 * Centralising them here makes threshold tuning trivial.
 */
final class AppConstants {

    private AppConstants() {}   // Utility class — prevent instantiation

    /** Medicines with quantity BELOW this value trigger a low-stock alert. */
    public static final int    LOW_STOCK_THRESHOLD  = 20;

    /** Medicines expiring within this many days trigger an expiry alert. */
    public static final int    EXPIRY_ALERT_DAYS    = 30;

    /** Expected date format for all user / data input. */
    public static final String DATE_FORMAT          = "YYYY-MM-DD";

    /** Column width used in formatted table output. */
    public static final int    COL_NAME_WIDTH       = 20;
    public static final int    COL_BATCH_WIDTH      = 12;

    /** Separator line drawn in the console for readability. */
    public static final String SEPARATOR =
        "─────────────────────────────────────────────────────────────";

    /** Header row for the inventory table display. */
    public static final String TABLE_HEADER = String.format(
        "%-" + COL_BATCH_WIDTH + "s │ %-" + COL_NAME_WIDTH + "s │ %-6s │ %-12s │ %s",
        "Batch No.", "Medicine Name", "Qty", "Expiry Date", "Status"
    );
}


// ─────────────────────────────────────────────────────────────────────────────
// SECTION 2 ─ MEDICINE (Data Model / Struct)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Medicine — immutable value object representing a single medicine entry.
 *
 * Fields
 * ──────
 *  name       : Medicine name used as the HashMap key (lowercase-normalised).
 *  batchNo    : Unique batch identifier (e.g. "B-001").
 *  quantity   : Current stock count in units.
 *  expiryDate : Parsed LocalDate; also the key in the TreeMap expiry index.
 *
 * DSA role: this object is stored as the VALUE in the HashMap and as
 * elements inside each List<Medicine> bucket of the TreeMap.
 */
class Medicine {

    // ── Fields ──────────────────────────────────────────────────────────────
    public final String    name;
    public final String    batchNo;
    public       int       quantity;   // mutable — stock levels change
    public final LocalDate expiryDate;

    // ── Constructor ─────────────────────────────────────────────────────────

    /**
     * @param name       Medicine name (any case — normalised internally).
     * @param batchNo    Batch identifier string.
     * @param quantity   Initial stock quantity (must be ≥ 0).
     * @param expiryDate ISO date string "YYYY-MM-DD"; parsed to LocalDate.
     * @throws java.time.format.DateTimeParseException if date is malformed.
     */
    public Medicine(String name, String batchNo, int quantity, String expiryDate) {
        this.name       = name.trim();
        this.batchNo    = batchNo.trim().toUpperCase();
        this.quantity   = quantity;
        this.expiryDate = LocalDate.parse(expiryDate.trim());
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Returns the canonical HashMap key for this medicine
     * (lowercase, trimmed name — matches how the map is keyed).
     */
    public String getKey() {
        return name.toLowerCase();
    }

    /**
     * Formatted single-row string for table display.
     * Columns are padded to match AppConstants.TABLE_HEADER widths.
     *
     * @param statusTag  Short status string, e.g. "⚠ EXPIRING SOON"
     */
    public String toTableRow(String statusTag) {
        return String.format(
            "%-" + AppConstants.COL_BATCH_WIDTH + "s │ %-"
                + AppConstants.COL_NAME_WIDTH + "s │ %-6d │ %-12s │ %s",
            batchNo, name, quantity, expiryDate, statusTag
        );
    }

    /** Simple toString used in search results and alerts. */
    @Override
    public String toString() {
        return String.format(
            "Batch: %-" + AppConstants.COL_BATCH_WIDTH + "s | Name: %-"
                + AppConstants.COL_NAME_WIDTH + "s | Qty: %-6d | Expiry: %s",
            batchNo, name, quantity, expiryDate
        );
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// SECTION 3 ─ BST NODE (used by the expiry-sorted tree)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * BSTNode — node definition for the manual Binary Search Tree (expiry index).
 *
 * Each node stores:
 *  • key      : LocalDate (the expiry date — BST ordering criterion)
 *  • medicines: list of medicines sharing that exact expiry date
 *  • left/right children
 *
 * NOTE: Java's TreeMap already implements a Red-Black BST internally.
 * This explicit node is provided to satisfy the "demonstrate BST" requirement
 * of the DSA project. Both structures are used in InventoryManager.
 */
class BSTNode {

    public LocalDate       key;        // Expiry date — BST key
    public List<Medicine>  medicines;  // Medicines sharing this expiry
    public BSTNode         left;
    public BSTNode         right;

    /**
     * @param key       The expiry date this node represents.
     * @param medicines Initial list of medicines at this expiry date.
     */
    public BSTNode(LocalDate key, List<Medicine> medicines) {
        this.key       = key;
        this.medicines = medicines;
        this.left      = null;
        this.right     = null;
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// SECTION 4 ─ InventoryService INTERFACE (Method Prototypes)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * InventoryService — defines the contract (API) that InventoryManager must fulfil.
 *
 * Analogous to a C header's function prototypes: every public operation is
 * declared here; implementation lives in InventoryManager.java.
 *
 * Operations
 * ──────────
 *  addMedicine      — insert a new medicine into both DSA structures
 *  searchByName     — O(1) HashMap lookup
 *  updateQuantity   — adjust stock for an existing medicine
 *  removeMedicine   — delete from both structures
 *  showAllByExpiry  — in-order BST traversal (chronological display)
 *  showExpiringSoon — query medicines expiring within N days
 *  showLowStock     — query medicines below stock threshold
 *  showAlerts       — combined alert summary
 *  showStats        — inventory statistics dashboard
 */
interface InventoryService {

    /** Add a new medicine to the inventory. */
    void addMedicine(String name, String batchNo, int quantity, String expiryDate);

    /** Search for a medicine by name (O(1) HashMap). */
    void searchByName(String name);

    /** Update the stock quantity of an existing medicine by name. */
    void updateQuantity(String name, int newQuantity);

    /** Remove a medicine from the inventory by name. */
    void removeMedicine(String name);

    /** Display all medicines sorted by expiry date (BST in-order traversal). */
    void showAllByExpiry();

    /** Display medicines expiring within the configured alert window. */
    void showExpiringSoon();

    /** Display medicines with stock below the low-stock threshold. */
    void showLowStock();

    /** Show a combined summary of all active alerts. */
    void showAlerts();

    /** Show inventory statistics (total medicines, total units, etc.). */
    void showStats();
}

