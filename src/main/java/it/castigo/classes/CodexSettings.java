package it.castigo.classes;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import java.nio.file.*;
import java.util.*;

public final class CodexSettings {
    public enum Kind { MOB, BOSS, WEAPON }
    public record Entry(Kind kind,String identity,String name,String category,ItemStack icon){
        public Entry{CodexSettings.category(category);if(identity==null||identity.length()>200||name==null||name.isBlank()||name.length()>100)throw new IllegalArgumentException("Identità o nome non valido");if(kind==Kind.WEAPON)SkillEquipment.validateItem(identity);else if(!identity.matches("(?:minecraft:[a-z_]+|mythic:[A-Za-z0-9_./:-]+)"))throw new IllegalArgumentException("Usa minecraft:zombie oppure mythic:NomeInterno");icon=icon.clone();icon.setAmount(1);}
        @Override public ItemStack icon(){return icon.clone();}
        public String key(){return (kind==Kind.WEAPON?"WEAPON":"ENTITY")+"|"+identity;}
        public Entry category(String value){return new Entry(kind,identity,name,value,icon);}
        public Entry name(String value){return new Entry(kind,identity,value,category,icon);}
    }
    public record Reward(String id,String category,int threshold,ItemStack item,String command){
        public Reward{UUID.fromString(id);CodexSettings.category(category);if(threshold<1||threshold>10000||command==null||command.length()>512||command.contains("\n")||command.contains("\r"))throw new IllegalArgumentException("Traguardo o comando non valido");item=item==null?null:item.clone();}
        @Override public ItemStack item(){return item==null?null:item.clone();}
    }
    private final Path file;private boolean enabled=true;private String book="";
    private final Map<String,Entry> entries=new LinkedHashMap<>();private final Map<String,Reward> rewards=new LinkedHashMap<>();
    public CodexSettings(Path file)throws Exception{
        this.file=file;var y=new YamlConfiguration();if(Files.exists(file))y.load(file.toFile());else{
            for(var type:List.of(EntityType.ZOMBIE,EntityType.SKELETON,EntityType.CREEPER,EntityType.SPIDER,EntityType.ENDERMAN,EntityType.BLAZE,EntityType.WITHER,EntityType.ENDER_DRAGON)){
                boolean boss=type==EntityType.WITHER||type==EntityType.ENDER_DRAGON;var egg=Material.matchMaterial(type.name()+"_SPAWN_EGG");var entry=new Entry(boss?Kind.BOSS:Kind.MOB,type.getKey().toString(),type.name(),boss?"boss":"mob",new ItemStack(egg==null?Material.NETHER_STAR:egg));entries.put(entry.key(),entry);
            }save(true,"",entries,rewards);return;
        }
        enabled=y.getBoolean("enabled",true);book=y.getString("book-item","");if(!book.isEmpty())SkillEquipment.validateItem(book);
        var list=y.getConfigurationSection("entries");if(list!=null)for(String i:list.getKeys(false)){var c=list.getConfigurationSection(i);var entry=new Entry(Kind.valueOf(c.getString("kind")),c.getString("identity"),c.getString("name"),c.getString("category"),Objects.requireNonNull(c.getItemStack("icon")));if(entries.values().stream().anyMatch(e->e.identity().equals(entry.identity())))throw new IllegalArgumentException("Voce duplicata: "+entry.identity());entries.put(entry.key(),entry);}
        var r=y.getConfigurationSection("rewards");if(r!=null)for(String i:r.getKeys(false)){var c=r.getConfigurationSection(i);var reward=new Reward(c.getString("id"),c.getString("category"),c.getInt("threshold"),c.getItemStack("item"),c.getString("command",""));rewards.put(reward.id(),reward);}
    }
    public static void category(String id){if(id==null||!id.matches("[a-z0-9_]{1,40}"))throw new IllegalArgumentException("Categoria: lettere minuscole, numeri e underscore (max 40)");}
    public boolean enabled(){return enabled;}
    public String book(){return book;}
    public List<Entry> entries(){return List.copyOf(entries.values());}
    public List<Reward> rewards(){return List.copyOf(rewards.values());}
    public Entry get(String key){return entries.get(key);}
    public Entry identity(String id){return entries.values().stream().filter(e->e.identity().equals(id)).findFirst().orElse(null);}
    public Reward reward(String id){return rewards.get(id);}
    public List<String> categories(){var result=new TreeSet<String>();entries.values().forEach(e->result.add(e.category()));rewards.values().forEach(e->result.add(e.category()));return List.copyOf(result);}
    public void enabled(boolean value)throws Exception{save(value,book,entries,rewards);enabled=value;}
    public void book(String id)throws Exception{if(!id.isEmpty())SkillEquipment.validateItem(id);save(enabled,id,entries,rewards);book=id;}
    public void put(Entry entry)throws Exception{var next=new LinkedHashMap<>(entries);next.values().removeIf(e->e.identity().equals(entry.identity()));next.put(entry.key(),entry);save(enabled,book,next,rewards);entries.clear();entries.putAll(next);}
    public void remove(String key)throws Exception{var next=new LinkedHashMap<>(entries);next.remove(key);save(enabled,book,next,rewards);entries.clear();entries.putAll(next);}
    public void put(Reward reward)throws Exception{var next=new LinkedHashMap<>(rewards);next.put(reward.id(),reward);save(enabled,book,entries,next);rewards.clear();rewards.putAll(next);}
    public void removeReward(String id)throws Exception{var next=new LinkedHashMap<>(rewards);next.remove(id);save(enabled,book,entries,next);rewards.clear();rewards.putAll(next);}
    private void save(boolean on,String binding,Map<String,Entry> entries,Map<String,Reward> rewards)throws Exception{
        var y=new YamlConfiguration();y.set("enabled",on);y.set("book-item",binding);int i=0;
        for(var e:entries.values()){String p="entries."+i++;y.set(p+".kind",e.kind().name());y.set(p+".identity",e.identity());y.set(p+".name",e.name());y.set(p+".category",e.category());y.set(p+".icon",e.icon());}
        i=0;for(var r:rewards.values()){String p="rewards."+i++;y.set(p+".id",r.id());y.set(p+".category",r.category());y.set(p+".threshold",r.threshold());y.set(p+".item",r.item());y.set(p+".command",r.command());}AtomicConfig.save(file,y);
    }
}
