package it.castigo.classes.model;

import java.util.*;

public final class Profile {
    public final UUID uuid;
    public String classId;
    public int level = 1;
    public long xp;
    public double resource;
    public final Map<String, Long> cooldowns = new HashMap<>();
    public final List<String> slots = new ArrayList<>();
    public long globalReadyAt;
    public Profile(UUID uuid, String classId) { this.uuid = uuid; this.classId = classId; }
    public void normalize(ClassDefinition definition) {
        classId = definition.id();
        if (!validOrder(slots, definition.skills().stream().map(Skill::id).toList())) {
            slots.clear(); definition.skills().forEach(s -> slots.add(s.id()));
        }
        resource = Double.isFinite(resource) ? Math.max(0, Math.min(resource, definition.stats(level).mana())) : 0;
    }
    public static boolean validOrder(List<String> order, List<String> available) {
        return order.size() == 8 && new HashSet<>(order).size() == 8 && new HashSet<>(order).equals(new HashSet<>(available));
    }
}
