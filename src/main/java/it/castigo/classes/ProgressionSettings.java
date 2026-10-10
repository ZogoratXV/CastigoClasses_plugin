package it.castigo.classes;

import org.bukkit.configuration.file.YamlConfiguration;
import it.castigo.classes.config.ClassCatalog;
import it.castigo.core.CoreApi;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** Admin overrides are separate from class definitions and survive catalog reloads. */
public final class ProgressionSettings {
    private final Path file;
    private YamlConfiguration data;
    public ProgressionSettings(Path file) throws Exception {
        this.file=file;data=new YamlConfiguration();if(Files.exists(file))data.load(file.toFile());
        dailyCap();resetTime();token();
    }
    public long dailyCap(){long n=data.getLong("daily.cap",100000);if(n<0||n>1_000_000_000_000L)throw new IllegalArgumentException("Cap XP da 0 (illimitato) a 1000000000000");return n;}
    public LocalTime resetTime(){return LocalTime.parse(data.getString("daily.reset-time","00:00"));}
    public String token(){return data.getString("daily.reset-token","");}
    public String day(Instant now){var zone=ZoneId.of("Europe/Rome");var date=now.atZone(zone).toLocalDate();var boundary=date.atTime(resetTime()).atZone(zone).toInstant();return (now.isBefore(boundary)?date.minusDays(1):date).toString();}
    public Instant nextReset(Instant now){var date=LocalDate.parse(day(now)).plusDays(1);return date.atTime(resetTime()).atZone(ZoneId.of("Europe/Rome")).toInstant();}
    public int cap(String id,ClassCatalog catalog,int global){int fallback=Set.of("mago","mago_bianco","mago_nero","guerriero_scudo","guerriero_due_mani","arciere").contains(id)?10:global;return Math.min(global,data.getInt("classes."+id+".level-cap",catalog.levelCap(id,fallback)));}
    public String group(String id,ClassCatalog catalog){return data.getString("classes."+id+".group",catalog.group(id));}
    public String item(String id){return data.getString("classes."+id+".promotion-item","");}
    public List<String> weapons(String id){return data.getStringList("classes."+id+".two-handed-items");}
    public void set(String id,String field,String value) throws Exception {
        var copy=new YamlConfiguration();copy.loadFromString(data.saveToString());
        String path=id==null?"daily."+field:"classes."+id+"."+field;
        switch(field){
            case "cap" -> {long n=Long.parseLong(value);if(n<0||n>1_000_000_000_000L)throw new IllegalArgumentException("Cap non valido");copy.set(path,n);}
            case "reset-time" -> {if(!value.matches("[0-2][0-9]:[0-5][0-9]"))throw new IllegalArgumentException("Usa HH:mm, ora italiana");LocalTime.parse(value);copy.set(path,value);}
            case "level-cap" -> {int n=Integer.parseInt(value);if(n<1||n>1000)throw new IllegalArgumentException("Livello da 1 a 1000");copy.set(path,n);}
            case "group" -> {HudSettings.validateGroup(value);copy.set(path,value);}
            case "promotion-item" -> {if(!value.isEmpty())SkillEquipment.validateItem(value);copy.set(path,value);}
            case "two-handed-items" -> {var list=new ArrayList<>(weapons(id));if(value.isEmpty())list.clear();else {SkillEquipment.validateItem(value);if(!list.remove(value)){if(list.size()>=64)throw new IllegalArgumentException("Massimo 64 armi");list.add(value);}}copy.set(path,list);}
            case "reset-token" -> copy.set(path,UUID.randomUUID().toString());
            default -> throw new IllegalArgumentException("Impostazione sconosciuta");
        }
        CoreApi.atomicWrite(file,copy.saveToString());data=copy;
    }
}
