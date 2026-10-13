package it.castigo.classes;
import java.nio.file.*;
import java.util.*;
import org.bukkit.configuration.file.YamlConfiguration;
/** Immutable snapshots are installed only after an atomic save succeeds. */
public final class CodexProgress {
    public record Data(Set<String> found,Set<String> claimed,Map<String,String> skins){public Data{found=Set.copyOf(found);claimed=Set.copyOf(claimed);skins=Map.copyOf(skins);}}
    private final Path dir;private final Map<UUID,Data> cache=new HashMap<>();
    public CodexProgress(Path dir){this.dir=dir;}
    public Data get(UUID id)throws Exception{if(cache.containsKey(id))return cache.get(id);var y=new YamlConfiguration();Path file=dir.resolve(id+".yml");if(Files.exists(file))y.load(file.toFile());var skins=new HashMap<String,String>();var section=y.getConfigurationSection("skins");if(section!=null)for(String category:section.getKeys(false))skins.put(category,section.getString(category));var data=new Data(new HashSet<>(y.getStringList("found")),new HashSet<>(y.getStringList("claimed")),skins);cache.put(id,data);return data;}
    public boolean discover(UUID player,Collection<String> keys)throws Exception{var old=get(player);var found=new HashSet<>(old.found());if(!found.addAll(keys))return false;save(player,new Data(found,old.claimed(),old.skins()));return true;}
    public boolean reserve(UUID player,String reward)throws Exception{var old=get(player);var claims=new HashSet<>(old.claimed());if(!claims.add(reward))return false;save(player,new Data(old.found(),claims,old.skins()));return true;}
    public void select(UUID player,String category,String key)throws Exception{var old=get(player);var skins=new HashMap<>(old.skins());if(key==null)skins.remove(category);else skins.put(category,key);save(player,new Data(old.found(),old.claimed(),skins));}
    public void clearSkins(UUID player)throws Exception{var old=get(player);save(player,new Data(old.found(),old.claimed(),Map.of()));}
    private void save(UUID id,Data data)throws Exception{var y=new YamlConfiguration();y.set("found",new ArrayList<>(new TreeSet<>(data.found())));y.set("claimed",new ArrayList<>(new TreeSet<>(data.claimed())));for(var entry:data.skins().entrySet())y.set("skins."+entry.getKey(),entry.getValue());AtomicConfig.save(dir.resolve(id+".yml"),y);cache.put(id,data);}
    public void forget(UUID id){cache.remove(id);}
}
