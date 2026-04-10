package MedicineInventory;

import java.time.LocalDate;

public final class TestDataLoader {

    private TestDataLoader() {}

    public static void loadTestData(InventoryManager inventory) {
        String today      = LocalDate.now().toString();
        String exp1d      = LocalDate.now().minusDays(1).toString();
        String exp10d     = LocalDate.now().minusDays(10).toString();
        String exp45d     = LocalDate.now().minusDays(45).toString();
        String soon5d     = LocalDate.now().plusDays(5).toString();
        String soon15d    = LocalDate.now().plusDays(15).toString();
        String soon29d    = LocalDate.now().plusDays(29).toString();
        String safe90d    = LocalDate.now().plusDays(90).toString();
        String safe180d   = LocalDate.now().plusDays(180).toString();
        String safe365d   = LocalDate.now().plusDays(365).toString();
        String safe500d   = LocalDate.now().plusDays(500).toString();
        String safe730d   = LocalDate.now().plusDays(730).toString();

        inventory.addMedicine("Paracetamol",   "B-001", 500,  safe365d);
        inventory.addMedicine("Amoxicillin",   "B-002", 320,  safe180d);
        inventory.addMedicine("Ibuprofen",     "B-003", 450,  safe730d);
        inventory.addMedicine("Metformin",     "B-004", 600,  safe500d);
        inventory.addMedicine("Omeprazole",    "B-005", 280,  safe365d);
        inventory.addMedicine("Cetirizine",    "B-006", 350,  safe180d);
        inventory.addMedicine("Azithromycin",  "B-007", 200,  safe90d);

        inventory.addMedicine("Aspirin",       "B-008", 120,  soon29d);
        inventory.addMedicine("Doxycycline",   "B-009", 80,   soon15d);
        inventory.addMedicine("Ranitidine",    "B-010", 60,   soon5d);

        inventory.addMedicine("Cefixime",      "B-011", 40,   exp1d);
        inventory.addMedicine("Diclofenac",    "B-012", 90,   exp10d);
        inventory.addMedicine("Tetracycline",  "B-013", 110,  exp45d);

        inventory.addMedicine("Loratadine",    "B-014", 15,   safe365d);
        inventory.addMedicine("Pantoprazole",  "B-015", 8,    safe180d);
        inventory.addMedicine("Metronidazole", "B-016", 3,    safe90d);

        inventory.addMedicine("Salbutamol",    "B-017", 12,   soon15d);
        inventory.addMedicine("Prednisolone",  "B-018", 5,    soon5d);
        inventory.addMedicine("Clonazepam",    "B-019", 1,    today);

        inventory.addMedicine("Atorvastatin",  "B-020", 250,  safe730d);
        inventory.addMedicine("Amlodipine",    "B-021", 190,  safe500d);
        inventory.addMedicine("Atenolol",      "B-022", 175,  safe365d);
    }
}
