package it.castigo.classes;

import it.castigo.classes.model.*;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.io.File;
import java.nio.file.*;

/** Persisted per-class skill overrides; ItemsAdder IDs are read from its public API. */
public final class SkillEquipment {
    private static final com.google.gson.JsonObject ICONS=loadIcons();
    private static com.google.gson.JsonObject loadIcons(){
        try(var stream=SkillEquipment.class.getResourceAsStream("/skill-icons.json")){
            if(stream==null)return new com.google.gson.JsonObject();
            return com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        }catch(java.io.IOException e){throw new IllegalStateException(e);}
    }
    public static String defaultIcon(Skill skill){return skill.icon().startsWith("minecraft:")&&ICONS.has(skill.id())?ICONS.get(skill.id()).getAsString():skill.icon();}
    public record Requirement(boolean offhand,String item) {}
    private final File file;
    private YamlConfiguration data;
    public SkillEquipment(File file) throws Exception {
        this.file=file;data=new YamlConfiguration();if(file.exists())data.load(file);
        var classes=data.getConfigurationSection("classes");
        if(classes!=null)for(String c:classes.getKeys(false)) {
            var skills=data.getConfigurationSection("classes."+c);
            if(skills==null)throw new IllegalArgumentException("Configurazione skill non valida: "+c);
            for(String s:skills.getKeys(false)) {
                String path=path(c,s);String item=data.getString(path+".item");
                if(item!=null)validateItem(item);
                String hand=data.getString(path+".hand","MAIN");
                if(!hand.equals("MAIN")&&!hand.equals("OFF"))throw new IllegalArgumentException("Mano non valida");
                String icon=data.getString(path+".icon");if(icon!=null)validateIcon(icon);
            }
        }
    }
    private static String path(String c,String s) {
        if(!c.matches("[a-z0-9_]{1,40}")||!s.matches("[a-z0-9_]{1,40}"))throw new IllegalArgumentException("ID classe/skill non valido");
        return "classes."+c+"."+s;
    }
    public Requirement requirement(String classId,Skill skill,SkillMechanics rules) {
        String path=path(classId,skill.id());String item=data.getString(path+".item");
        if(item!=null)return new Requirement(data.getString(path+".hand","MAIN").equals("OFF"),item);
        return switch(rules.weapon()) {
            case SHIELD -> new Requirement(true,"minecraft:shield");
            case TWO_HANDED -> new Requirement(false,"minecraft:iron_sword");
            case BOW -> new Requirement(false,"minecraft:bow");
            case ANY -> new Requirement(false,classId.equals("arciere")?"minecraft:bow":"minecraft:stick");
        };
    }
    public boolean matches(Player p,String classId,Skill skill,SkillMechanics mechanics) {
        Requirement r=requirement(classId,skill,mechanics);
        ItemStack item=r.offhand()?p.getInventory().getItemInOffHand():p.getInventory().getItemInMainHand();
        try {
            if(!r.item().equals(identify(item)))return false;
            if(mechanics.weapon()==SkillMechanics.Weapon.BOW&&(r.offhand()||p.getInventory().getItemInMainHand().getType()!=Material.BOW))return false;
            return mechanics.weapon()!=SkillMechanics.Weapon.SHIELD||!p.hasCooldown(item.getType());
        } catch(ReflectiveOperationException|LinkageError|IllegalArgumentException e) { return false; }
    }
    public static String identify(ItemStack item) throws ReflectiveOperationException {
        if(item==null||item.getType().isAir())throw new IllegalArgumentException("Scegli un oggetto, non una mano vuota.");
        var ia=Bukkit.getPluginManager().getPlugin("ItemsAdder");
        if(ia!=null&&ia.isEnabled()) {
            Class<?> api=Class.forName("dev.lone.itemsadder.api.CustomStack",true,ia.getClass().getClassLoader());
            Object custom=api.getMethod("byItemStack",ItemStack.class).invoke(null,item);
            if(custom!=null)return "itemsadder:"+api.getMethod("getNamespacedID").invoke(custom);
        }
        return item.getType().getKey().toString();
    }
    public String icon(String classId,Skill skill) { return data.getString(path(classId,skill.id())+".icon",defaultIcon(skill)); }
    public void setItem(String c,String s,Requirement r) throws Exception {
        validateItem(r.item());var copy=copy();String path=path(c,s);
        copy.set(path+".item",r.item());copy.set(path+".hand",r.offhand()?"OFF":"MAIN");save(copy);
    }
    public void setIcon(String c,String s,String icon) throws Exception {
        if(!icon.equals("reset"))validateIcon(icon);var copy=copy();copy.set(path(c,s)+".icon",icon.equals("reset")?null:icon);save(copy);
    }
    public static void validateIcon(String icon) {
        String id=icon.startsWith("texture:")?icon.substring(8):icon;
        if(id.length()>200||!id.matches("[a-z0-9_]+:[a-z0-9_./-]+")||id.contains(".."))throw new IllegalArgumentException("Icona: minecraft:oggetto oppure texture:namespace:textures/gui/skills/nome.png");
        if(icon.startsWith("texture:")&&(!id.contains(":textures/")||!id.endsWith(".png")))throw new IllegalArgumentException("Texture PNG richiesta");
    }
    public static void validateItem(String id) {
        if(id.startsWith("itemsadder:")) {
            if(!id.substring(11).matches("[a-z0-9_]+:[a-z0-9_/.-]+")||id.length()>200)throw new IllegalArgumentException("ID ItemsAdder non valido");
        } else {
            if(!id.startsWith("minecraft:"))throw new IllegalArgumentException("Oggetto vanilla o ItemsAdder richiesto");
            Material m=Material.matchMaterial(id);if(m==null||!m.isItem()||m.isAir())throw new IllegalArgumentException("Oggetto non valido: "+id);
        }
    }
    private YamlConfiguration copy() throws Exception { var copy=new YamlConfiguration();copy.loadFromString(data.saveToString());return copy; }
    private void save(YamlConfiguration next) throws Exception {
        Files.createDirectories(file.toPath().getParent());Path temporary=Files.createTempFile(file.toPath().getParent(),"skill-equipment-",".tmp");
        try {
            Files.writeString(temporary,next.saveToString());
            try { Files.move(temporary,file.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
            catch(AtomicMoveNotSupportedException e) { Files.move(temporary,file.toPath(),StandardCopyOption.REPLACE_EXISTING); }
            data=next;
        } finally { Files.deleteIfExists(temporary); }
    }
}
