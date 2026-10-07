package it.castigo.classes.storage;

import it.castigo.classes.model.*;
import it.castigo.core.CoreApi;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.*;
import java.util.*;

public final class ProfileStore {
    private final File directory;
    public ProfileStore(File directory) throws IOException {
        this.directory = directory;
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Impossibile creare "+directory);
    }
    public Profile load(UUID id, ClassDefinition fallback, int maxLevel) throws Exception {
        File file = new File(directory,id+".yml");
        Profile p = new Profile(id,fallback.id());
        if (!file.exists()) { p.resource=fallback.stats(1).mana(); p.normalize(fallback); return p; }
        YamlConfiguration y = new YamlConfiguration(); y.load(file);
        p.classId=y.getString("class",fallback.id()); p.level=Math.max(1,Math.min(maxLevel,y.getInt("level",1)));
        p.xp=Math.max(0,y.getLong("xp")); p.resource=y.getDouble("resource"); p.slots.addAll(y.getStringList("slots"));
        p.earnedStatPoints=pointCount(y,"stat-points.earned");
        p.statPointsRewardedThroughLevel=y.contains("stat-points.earned")?p.level:1;
        if(y.contains("stat-points.rewarded-through-level")) {
            p.statPointsRewardedThroughLevel=pointCount(y,"stat-points.rewarded-through-level");
            if(p.statPointsRewardedThroughLevel<1||p.statPointsRewardedThroughLevel>1000)
                throw new IOException("Livello ricompense non valido nel profilo "+id);
        }
        for(StatAttribute stat:StatAttribute.values()) {
            int spent=pointCount(y,"stat-points.allocated."+stat.id());
            if(spent>0)p.allocatedStats.put(stat,spent);
        }
        if(p.spentStatPoints()>p.earnedStatPoints)throw new IOException("Punti spesi superiori ai punti guadagnati nel profilo "+id);
        var cooldowns=y.getConfigurationSection("cooldowns");
        if(cooldowns!=null) for(String key:cooldowns.getKeys(false)) {
            long expiry=cooldowns.getLong(key);
            if(expiry>System.currentTimeMillis()) p.cooldowns.put(key,Math.min(expiry,System.currentTimeMillis()+86_400_000));
        }
        return p;
    }
    public void save(Profile p) throws IOException {
        YamlConfiguration y=new YamlConfiguration();
        y.set("schema",2); y.set("class",p.classId); y.set("level",p.level); y.set("xp",p.xp);
        y.set("stat-points.earned",p.earnedStatPoints);
        y.set("stat-points.rewarded-through-level",p.statPointsRewardedThroughLevel);
        p.allocatedStats.forEach((stat,count)->y.set("stat-points.allocated."+stat.id(),count));
        y.set("resource",p.resource); y.set("slots",List.copyOf(p.slots));
        p.cooldowns.forEach((key,value)-> { if(value>System.currentTimeMillis()) y.set("cooldowns."+key,value); });
        CoreApi.atomicWrite(new File(directory,p.uuid+".yml").toPath(),y.saveToString());
    }
    private static int pointCount(YamlConfiguration y,String key) throws IOException {
        if(!y.contains(key))return 0; // Profiles from beta.1 migrate without losing any other fields.
        Object raw=y.get(key);
        if(!(raw instanceof Number n)||!Double.isFinite(n.doubleValue())||n.doubleValue()!=n.intValue()||n.intValue()<0||n.intValue()>1_000_000)
            throw new IOException("Conteggio punti non valido: "+key);
        return ((Number)raw).intValue();
    }
}
