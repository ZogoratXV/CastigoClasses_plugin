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
    public double value(StatAttribute attribute) {
        return switch(attribute) {
            case STRENGTH -> strength; case DEXTERITY -> dexterity; case HEALTH -> health;
            case MANA -> mana; case INTELLIGENCE -> intelligence; case ATTACK -> attack; case DEFENSE -> defense;
        };
    }
    public Stats addAllocated(java.util.Map<StatAttribute,Integer> allocated, Stats gain) {
        double[] values=new double[7];
        for(StatAttribute stat:StatAttribute.values())
            values[stat.ordinal()]=Math.min(stat==StatAttribute.HEALTH?1024:1_000_000,
                    value(stat)+(double)allocated.getOrDefault(stat,0)*gain.value(stat));
        return new Stats(values[0],values[1],values[2],values[3],values[4],values[5],values[6]);
    }
}
