package it.castigo.classes.model;

import java.util.Locale;

public enum StatAttribute {
    STRENGTH("strength", "Forza"), DEXTERITY("dexterity", "Destrezza"),
    HEALTH("health", "Vita"), MANA("mana", "Risorsa"),
    INTELLIGENCE("intelligence", "Intelligenza"), ATTACK("attack", "Attacco"), DEFENSE("defense", "Difesa");

    private final String id, label;
    StatAttribute(String id, String label) { this.id=id; this.label=label; }
    public String id() { return id; }
    public String label() { return label; }
    public static StatAttribute parse(String id) {
        String value=id.toLowerCase(Locale.ROOT);
        if(value.equals("risorsa"))return MANA;
        for(StatAttribute stat:values())
            if(stat.id.equals(value)||stat.label.toLowerCase(Locale.ROOT).equals(value))return stat;
        throw new IllegalArgumentException("Attributo sconosciuto: "+id);
    }
}
