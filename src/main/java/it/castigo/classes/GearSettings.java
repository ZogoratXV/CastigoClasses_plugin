package it.castigo.classes;

import it.castigo.classes.model.StatAttribute;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.*;
import java.nio.file.*;
import java.util.*;

/** Rules apply by exact vanilla/ItemsAdder ID. Rolls are stored separately on each item. */
public final class GearSettings {
    public static final List<EquipmentSlot> SLOTS=List.of(EquipmentSlot.HAND,EquipmentSlot.OFF_HAND,EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET);
    public record Rule(String id,EquipmentSlot slot,int level,double armor,double shield,Map<StatAttribute,Double> bonus,Map<StatAttribute,Double> min,Map<StatAttribute,Double> max,ItemStack template) {
        public Rule {
            SkillEquipment.validateItem(id);
            if(!SLOTS.contains(slot)||level<1||level>1000)throw new IllegalArgumentException("Slot o livello non valido");
            range(armor,0,30);range(shield,-1,100);if(shield<0&&shield!=-1)throw new IllegalArgumentException("Parata: -1 oppure da 0 a 100");
            bonus=Map.copyOf(bonus);min=Map.copyOf(min);max=Map.copyOf(max);template=template.clone();template.setAmount(1);
            for(var s:StatAttribute.values()){range(bonus.getOrDefault(s,0d),0,1000);range(min.getOrDefault(s,0d),0,1000);range(max.getOrDefault(s,0d),min.getOrDefault(s,0d),1000);}
        }
        @Override public ItemStack template(){return template.clone();}
        public Rule field(String key,double value){
            var b=new EnumMap<StatAttribute,Double>(StatAttribute.class);b.putAll(bonus);
            var lo=new EnumMap<StatAttribute,Double>(StatAttribute.class);lo.putAll(min);
            var hi=new EnumMap<StatAttribute,Double>(StatAttribute.class);hi.putAll(max);
            if(key.contains(".")){String[] parts=key.split("\\.");var stat=StatAttribute.parse(parts[1]);switch(parts[0]){case "bonus"->b.put(stat,value);case "min"->lo.put(stat,value);case "max"->hi.put(stat,value);default->throw new IllegalArgumentException("Campo sconosciuto");}}
            else if(!Set.of("level","armor","shield").contains(key))throw new IllegalArgumentException("Campo sconosciuto");
            if(key.equals("level")&&value!=Math.rint(value))throw new IllegalArgumentException("Livello intero richiesto");
            return new Rule(id,slot,key.equals("level")?(int)value:level,key.equals("armor")?value:armor,key.equals("shield")?value:shield,b,lo,hi,template);
        }
        public Rule slot(EquipmentSlot next){return new Rule(id,next,level,armor,shield,bonus,min,max,template);}
    }
    private final Path file;
    private final Map<String,Rule> rules=new LinkedHashMap<>();
    public GearSettings(Path file)throws Exception {
        this.file=file;if(!Files.exists(file))return;
        var y=new YamlConfiguration();y.load(file.toFile());var entries=y.getConfigurationSection("items");if(entries==null)return;
        for(String k:entries.getKeys(false)){
            var c=entries.getConfigurationSection(k);String id=c.getString("id");
            var b=new EnumMap<StatAttribute,Double>(StatAttribute.class);var lo=new EnumMap<StatAttribute,Double>(StatAttribute.class);var hi=new EnumMap<StatAttribute,Double>(StatAttribute.class);
            for(var s:StatAttribute.values()){b.put(s,number(c,"bonus."+s.id(),0));lo.put(s,number(c,"random-min."+s.id(),0));hi.put(s,number(c,"random-max."+s.id(),0));}
            var r=new Rule(id,EquipmentSlot.valueOf(c.getString("slot")),(int)number(c,"level",1),number(c,"armor",0),number(c,"shield",-1),b,lo,hi,Objects.requireNonNull(c.getItemStack("template"),"Modello oggetto mancante"));
            if(rules.put(id,r)!=null)throw new IllegalArgumentException("Oggetto duplicato: "+id);
        }
    }
    private static double number(org.bukkit.configuration.ConfigurationSection c,String key,double fallback){if(!c.contains(key))return fallback;Object v=c.get(key);if(!(v instanceof Number n)||!Double.isFinite(n.doubleValue()))throw new IllegalArgumentException("Numero non valido: "+key);double d=n.doubleValue();if(key.equals("level")&&d!=Math.rint(d))throw new IllegalArgumentException("Livello intero richiesto");return d;}
    private static void range(double v,double lo,double hi){if(!Double.isFinite(v)||v<lo||v>hi)throw new IllegalArgumentException("Valore richiesto tra "+lo+" e "+hi);}
    public Rule get(String id){return rules.get(id);}
    public List<Rule> all(){return List.copyOf(rules.values());}
    public void put(Rule r)throws Exception {var next=new LinkedHashMap<>(rules);next.put(r.id(),r);save(next);}
    public void remove(String id)throws Exception {var next=new LinkedHashMap<>(rules);next.remove(id);save(next);}
    private void save(Map<String,Rule> next)throws Exception {
        var y=new YamlConfiguration();int i=0;
        for(var r:next.values()){
            String k="items."+(i++);y.set(k+".id",r.id());y.set(k+".slot",r.slot().name());y.set(k+".level",r.level());y.set(k+".armor",r.armor());y.set(k+".shield",r.shield());y.set(k+".template",r.template());
            for(var s:StatAttribute.values()){y.set(k+".bonus."+s.id(),r.bonus().getOrDefault(s,0d));y.set(k+".random-min."+s.id(),r.min().getOrDefault(s,0d));y.set(k+".random-max."+s.id(),r.max().getOrDefault(s,0d));}
        }
        Files.createDirectories(file.getParent());Path tmp=Files.createTempFile(file.getParent(),"gear-",".tmp");
        try{Files.writeString(tmp,y.saveToString());try{Files.move(tmp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(tmp);}
        rules.clear();rules.putAll(next);
    }
}
