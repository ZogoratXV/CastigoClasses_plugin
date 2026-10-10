package it.castigo.classes.model;

import java.util.*;

public final class Profile {
    public final UUID uuid;
    public String classId;
    public int level = 1;
    public long xp;
    public long dailyXp;
    public String xpDay="",xpResetToken="",managedClassGroup="";
    public double resource;
    public int earnedStatPoints;
    public int statPointsRewardedThroughLevel=1;
    public final EnumMap<StatAttribute,Integer> allocatedStats=new EnumMap<>(StatAttribute.class);
    public final Map<String, Long> cooldowns = new HashMap<>();
    public final List<String> slots = new ArrayList<>();
    public long globalReadyAt;
    public Profile(UUID uuid, String classId) { this.uuid = uuid; this.classId = classId; }
    public void normalize(ClassDefinition definition) {
        normalize(definition,definition.stats(level));
    }
    public void normalize(ClassDefinition definition, Stats total) {
        classId = definition.id();
        if (!validOrder(slots, definition.skills().stream().map(Skill::id).toList())) {
            slots.clear(); definition.skills().forEach(s -> slots.add(s.id()));
        }
        resource = Double.isFinite(resource) ? Math.max(0, Math.min(resource, total.mana())) : 0;
    }
    public int spentStatPoints() { return allocatedStats.values().stream().mapToInt(Integer::intValue).sum(); }
    public int availableStatPoints() { return Math.max(0,earnedStatPoints-spentStatPoints()); }
    public static boolean validOrder(List<String> order, List<String> available) {
        return order.size() == 8 && new HashSet<>(order).size() == 8 && new HashSet<>(order).equals(new HashSet<>(available));
    }
}
