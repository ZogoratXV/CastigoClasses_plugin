package it.castigo.classes.model;

public record Stats(double strength, double dexterity, double health, double mana,
                    double intelligence, double attack, double defense) {
    public Stats {
        for (double n : new double[]{strength, dexterity, health, mana, intelligence, attack, defense})
            if (!Double.isFinite(n) || n < 0 || n > 1_000_000) throw new IllegalArgumentException("Attributo fuori intervallo");
    }
    public Stats atLevel(Stats growth, int level) {
        int n = Math.max(0, level - 1);
        return new Stats(strength + growth.strength*n, dexterity + growth.dexterity*n,
                Math.min(1024, Math.max(1, health + growth.health*n)), mana + growth.mana*n,
                intelligence + growth.intelligence*n, attack + growth.attack*n, defense + growth.defense*n);
    }
    public static double mitigate(double damage, double defense) { return damage * 100.0 / (100.0 + defense); }
}
