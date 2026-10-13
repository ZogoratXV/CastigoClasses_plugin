package it.castigo.classes;
import java.nio.file.*;
import java.util.*;
import org.bukkit.configuration.file.YamlConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
public final class RaritySettings {
    public enum Tier { COMMON("Comune"), RARE("Raro"), EPIC("Epico"), LEGENDARY("Leggendario");public final String label;Tier(String label){this.label=label;} }
    public record Style(String color,double damage,double armor){
        public Style{if(NamedTextColor.NAMES.value(color)==null||!Double.isFinite(damage)||!Double.isFinite(armor)||damage<0||damage>100||armor<0||armor>100)throw new IllegalArgumentException("Colore vanilla e percentuali da 0 a 100 richiesti");}
    }
    private final Path file;private final EnumMap<Tier,Style> styles=new EnumMap<>(Tier.class);
    private final Map<String,Tier> items=new LinkedHashMap<>();
    public RaritySettings(Path file)throws Exception{
        this.file=file;String[] colors={"white","blue","light_purple","gold"};double[] amount={0,3,6,10};
        var y=new YamlConfiguration();if(Files.exists(file))y.load(file.toFile());
        for(var t:Tier.values()){String p="tiers."+t.name();styles.put(t,new Style(y.getString(p+".color",colors[t.ordinal()]),y.getDouble(p+".damage-percent",amount[t.ordinal()]),y.getDouble(p+".armor-percent",amount[t.ordinal()])));}
        var entries=y.getConfigurationSection("items");if(entries!=null)for(String k:entries.getKeys(false)){String id=entries.getString(k+".id");SkillEquipment.validateItem(id);items.put(id,Tier.valueOf(entries.getString(k+".tier")));}
    }
    public Style style(Tier tier){return styles.get(tier);}
    public Tier tier(String id){return items.getOrDefault(id,Tier.COMMON);}
    public void set(String id,Tier tier)throws Exception{SkillEquipment.validateItem(id);var next=new LinkedHashMap<>(items);next.put(id,tier);save(styles,next);items.clear();items.putAll(next);}
    public void set(Tier tier,Style style)throws Exception{var next=new EnumMap<>(styles);next.put(tier,style);save(next,items);styles.clear();styles.putAll(next);}
    private void save(Map<Tier,Style> values,Map<String,Tier> ids)throws Exception{
        var y=new YamlConfiguration();for(var e:values.entrySet()){String p="tiers."+e.getKey().name();y.set(p+".color",e.getValue().color());y.set(p+".damage-percent",e.getValue().damage());y.set(p+".armor-percent",e.getValue().armor());}
        int i=0;for(var e:ids.entrySet()){String p="items."+i++;y.set(p+".id",e.getKey());y.set(p+".tier",e.getValue().name());}AtomicConfig.save(file,y);
    }
}
