package it.castigo.classes.config;

import it.castigo.classes.model.*;
import it.castigo.classes.SkillPresentation;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.*;

public final class ClassCatalog {
    private final Map<String, ClassDefinition> definitions = new LinkedHashMap<>();
    private final Map<String, ConfigurationSection> raw = new LinkedHashMap<>();
    private final Map<Skill, SkillPresentation> presentations = new IdentityHashMap<>();
    public ClassCatalog(File directory) throws Exception {
        File[] files = directory.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null || files.length == 0) throw new IllegalArgumentException("Nessuna classe nella cartella classes");
        if (files.length>256) throw new IllegalArgumentException("Massimo 256 classi per catalogo");
        Arrays.sort(files);
        for (File file : files) {
            YamlConfiguration y = new YamlConfiguration(); y.load(file);
            String id = y.getString("id", file.getName().replace(".yml", ""));
            identifier(id);
            if (raw.put(id, y) != null) throw new IllegalArgumentException("Classe duplicata: " + id);
        }
        for (String id : raw.keySet()) resolve(id, new HashSet<>());
    }
    public Map<String, ClassDefinition> all() { return Collections.unmodifiableMap(definitions); }
    public ClassDefinition get(String id) { return definitions.get(id); }
    public SkillPresentation presentation(Skill skill) { return presentations.get(skill); }
    private ClassDefinition resolve(String id, Set<String> path) {
        if (definitions.containsKey(id)) return definitions.get(id);
        if (!path.add(id)) throw new IllegalArgumentException("Ciclo nelle sottoclassi: " + id);
        ConfigurationSection c = raw.get(id);
        if (c == null) throw new IllegalArgumentException("Classe genitore inesistente: " + id);
        String parentId = c.getString("parent", "");
        ClassDefinition p = parentId.isEmpty() ? null : resolve(parentId, path);
        Stats zero = new Stats(0,0,0,0,0,0,0);
        Stats base = stats(c.getConfigurationSection("attributes.base"), p == null ? new Stats(0,0,20,100,0,0,0) : p.base());
        Stats growth = stats(c.getConfigurationSection("attributes.per-level"), p == null ? zero : p.growth());
        List<Skill> skills = new ArrayList<>();
        ConfigurationSection section = c.getConfigurationSection("skills");
        if (section == null && p != null) skills.addAll(p.skills());
        else if (section != null) for (String sid : section.getKeys(false)) {
            identifier(sid); ConfigurationSection s = Objects.requireNonNull(section.getConfigurationSection(sid));
            Skill skill=new Skill(sid, text(s,"name",sid), text(s,"description",""),
                    Skill.Effect.valueOf(s.getString("effect", "BOLT").toUpperCase(Locale.ROOT)),
                    text(s,"icon","minecraft:amethyst_shard"), color(s.getString("color","AA77FF")),
                    (int)number(s,"unlock-level",1,1,1000), number(s,"cost",10,0,1000000),
                    (long)(number(s,"cooldown-seconds",2,0.1,86400)*1000), number(s,"power",5,0,10000),
                    number(s,"intelligence-scale",0.5,0,100), number(s,"range",24,1,64),
                    number(s,"radius",4,0.5,12), (int)(number(s,"duration-seconds",5,0.05,120)*20));
            skills.add(skill);
            if(s.contains("presentation")&&!s.isConfigurationSection("presentation"))throw new IllegalArgumentException("Sezione presentation richiesta: "+sid);
            try { presentations.put(skill,SkillPresentation.read(s.getConfigurationSection("presentation"),skill)); }
            catch(RuntimeException ex) { throw new IllegalArgumentException(id+"/"+sid+": "+ex.getMessage(),ex); }
        }
        if (skills.size()!=8) throw new IllegalArgumentException(id+": servono esattamente 8 skill");
        ClassDefinition result = new ClassDefinition(id, text(c,"name",id), text(c,"description",""), parentId,
                (int)number(c,"required-level",p == null ? 1 : 10,1,1000),
                text(c,"resource.name",p == null ? "Mana" : p.resourceName()),
                color(c.getString("resource.color",p == null ? "5588FF" : String.format("%06X",p.resourceColor()))),
                number(c,"resource.regen-per-second",p == null ? 4 : p.regenPerSecond(),0,10000),
                number(c,"resource.regen-per-level",p == null ? 0.1 : p.regenPerLevel(),0,100),base,growth,skills);
        result.stats(1000); // Detect overflowing growth before accepting the catalog.
        definitions.put(id,result); path.remove(id); return result;
    }
    private static Stats stats(ConfigurationSection s, Stats d) {
        if (s == null) return d;
        return new Stats(number(s,"strength",d.strength(),0,1000),number(s,"dexterity",d.dexterity(),0,1000),
                number(s,"health",d.health(),0,1024),number(s,"mana",d.mana(),0,1000),
                number(s,"intelligence",d.intelligence(),0,1000),number(s,"attack",d.attack(),0,1000),number(s,"defense",d.defense(),0,1000));
    }
    public static double number(ConfigurationSection s,String key,double fallback,double min,double max) {
        if(s.contains(key) && !(s.get(key) instanceof Number))throw new IllegalArgumentException("Numero richiesto: "+key);
        double n=s.getDouble(key,fallback);
        if (!Double.isFinite(n)||n<min||n>max) throw new IllegalArgumentException("Valore non valido: "+key);
        return n;
    }
    private static String text(ConfigurationSection c,String k,String fallback) {
        String v=c.getString(k,fallback); if(v.length()>240) throw new IllegalArgumentException("Testo troppo lungo: "+k); return v;
    }
    private static int color(String c) { if(!c.matches("[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Colore RGB non valido: "+c); return Integer.parseInt(c,16); }
    private static void identifier(String id) { if(!id.matches("[a-z0-9_]{1,40}")) throw new IllegalArgumentException("ID non valido: "+id); }
}
