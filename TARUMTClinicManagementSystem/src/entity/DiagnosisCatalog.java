package entity;

import control.PharmacyControl;
import entity.Medicine;

/**
 * Diagnosis → Medicine suggestion helper.
 */
public final class DiagnosisCatalog {

    private DiagnosisCatalog() {}

    public static class Suggestion {
        public final String medId;
        public final String medName;
        public final String dosage;   // e.g., "1 tablet, 3x/day"
        public final String instr;    // e.g., "After meal, 3 days"

        public Suggestion(String medId, String medName, String dosage, String instr) {
            this.medId = medId;
            this.medName = medName;
            this.dosage = dosage;
            this.instr  = instr;
        }

        @Override
        public String toString() {
            return String.format("%s (%s) - %s | %s", medName, medId, dosage, instr);
        }
    }

    /** Internal plan: list of preferred med IDs + default dosage/instructions for the diagnosis. */
    private static final class Plan {
        final String[] ids;
        final String dosage;
        final String instr;

        Plan(String[] ids, String dosage, String instr) {
            this.ids = ids;
            this.dosage = dosage;
            this.instr = instr;
        }
    }

    /**
     * Main API. Provide a diagnosis (free-text OK) and PharmacyControl.
     * Returns a Suggestion or null if we can't find a suitable medicine.
     */
    public static Suggestion suggest(String diagnosis, PharmacyControl pharmacy) {
        if (diagnosis == null || diagnosis.isBlank() || pharmacy == null) return null;

        String key = normalize(diagnosis);
        Plan plan = switch (key) {
            // Common cold / flu -> Panadol (symptomatic)
            case "commoncold", "cold", "uri", "upperrespiratoryinfection", "influenza", "flu", "d001", "d002" ->
                new Plan(ids("M3", "M8"), "1 tablet, 3x/day", "After meal, 3-5 days; rest & fluids");

            // Hypertension -> Amlodipine
            case "hypertension", "highbp", "highbloodpressure", "d003" ->
                new Plan(ids("M10"), "1 tablet/day", "Take at the same time daily");

            // Diabetes mellitus -> Metformin
            case "diabetesmellitus", "diabetes", "d004" ->
                new Plan(ids("M9"), "1 tablet, 2x/day", "After meal; monitor glucose");

            // Allergic rhinitis / allergies -> Loratadine
            case "allergicrhinitis", "allergyrhinitis", "allergies", "d005" ->
                new Plan(ids("M5"), "1 tablet/day", "May cause drowsiness in some");

            // Asthma -> Salbutamol Inhaler primary, Montelukast fallback
            case "asthma", "d006" ->
                new Plan(ids("M6", "M17"), "2 puffs PRN", "Use as needed for wheeze/shortness of breath");

            // Gastroenteritis -> ORS solution
            case "gastroenteritis", "stomachflu", "d007" ->
                new Plan(ids("M7"), "1 sachet/episode", "Dissolve in water; maintain hydration");

            // Migraine / headache -> Ibuprofen (or Panadol fallback)
            case "migraine", "d008" ->
                new Plan(ids("M8", "M3"), "1 tablet/6-8h PRN", "After meal; max daily dose per label");

            // Skin infection / inflammation -> Hydrocortisone cream
            case "skininfection", "skin", "dermatitis", "d009" ->
                new Plan(ids("M13"), "Apply thin layer, 2x/day", "Do not use on broken skin > 7 days");

            // COVID-19 suspected -> symptomatic relief Panadol
            case "covid19suspected", "covid", "covid19", "d010" ->
                new Plan(ids("M3"), "1 tablet, 3x/day", "Symptomatic relief; isolate & test");

            default -> null;
        };

        if (plan == null) return null;

        // pick first available med in stock
        String chosenId = firstAvailable(pharmacy, plan.ids);
        if (chosenId == null) return null;

        Medicine med = pharmacy.getMedicineById(chosenId);
        if (med == null) return null;

        return new Suggestion(chosenId, med.getName(), plan.dosage, plan.instr);
    }

    // ---- helpers ----

    /** Normalize diagnosis text (lowercase alnum). Accepts codes like D003 as-is (case-insensitive). */
    private static String normalize(String s) {
        String t = s.trim().toLowerCase();
        if (t.matches("d\\d{3}")) return t;
        return t.replaceAll("[^a-z0-9]", "");
    }

    private static String[] ids(String a) { return new String[]{a}; }
    private static String[] ids(String a, String b) { return new String[]{a, b}; }
    private static String[] ids(String a, String b, String c) { return new String[]{a, b, c}; }

    /** Return the first medicine ID that exists AND has stock > 0; otherwise null. */
    private static String firstAvailable(PharmacyControl pharmacy, String[] ids) {
        if (ids == null) return null;
        for (int i = 0; i < ids.length; i++) {
            Medicine m = pharmacy.getMedicineById(ids[i]);
            if (m != null && m.getQuantity() > 0) return m.getId();
        }
        return null;
    }
}
