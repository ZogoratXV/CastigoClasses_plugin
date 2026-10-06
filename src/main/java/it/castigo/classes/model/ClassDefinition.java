package it.castigo.classes.model;

import java.util.List;

public record ClassDefinition(String id, String name, String description, String parent,
        int requiredLevel, String resourceName, int resourceColor, double regenPerSecond,
        double regenPerLevel, Stats base, Stats growth, List<Skill> skills) {
    public ClassDefinition { skills = List.copyOf(skills); }
    public Stats stats(int level) { return base.atLevel(growth, level); }
    public Skill skill(String id) { return skills.stream().filter(s -> s.id().equals(id)).findFirst().orElse(null); }
}
