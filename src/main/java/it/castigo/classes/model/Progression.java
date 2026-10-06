package it.castigo.classes.model;

public record Progression(int maxLevel, double baseXp, double growth) {
    public Progression {
        if (maxLevel < 1 || maxLevel > 1000 || !Double.isFinite(baseXp) || baseXp < 1 ||
                !Double.isFinite(growth) || growth < 1 || growth > 10)
            throw new IllegalArgumentException("Curva esperienza non valida");
    }
    public long required(int level) {
        if (level >= maxLevel) return 0;
        return (long)Math.min(1_000_000_000_000L, Math.ceil(baseXp * Math.pow(growth, Math.max(0, level - 1))));
    }
    public void add(Profile p, long amount) {
        if (amount < 0) return;
        p.level = Math.max(1, Math.min(maxLevel, p.level));
        if (p.level >= maxLevel) { p.xp=0; return; }
        p.xp = Math.min(1_000_000_000_000_000L, p.xp + Math.min(amount, 1_000_000_000_000_000L));
        while (p.level < maxLevel && p.xp >= required(p.level)) { p.xp -= required(p.level); p.level++; }
        if (p.level == maxLevel) p.xp = 0;
    }
}
