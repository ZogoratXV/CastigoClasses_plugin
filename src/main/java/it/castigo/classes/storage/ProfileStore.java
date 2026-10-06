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
        var cooldowns=y.getConfigurationSection("cooldowns");
        if(cooldowns!=null) for(String key:cooldowns.getKeys(false)) {
            long expiry=cooldowns.getLong(key);
            if(expiry>System.currentTimeMillis()) p.cooldowns.put(key,Math.min(expiry,System.currentTimeMillis()+86_400_000));
        }
        return p;
    }
    public void save(Profile p) throws IOException {
        YamlConfiguration y=new YamlConfiguration();
        y.set("schema",1); y.set("class",p.classId); y.set("level",p.level); y.set("xp",p.xp);
        y.set("resource",p.resource); y.set("slots",List.copyOf(p.slots));
        p.cooldowns.forEach((key,value)-> { if(value>System.currentTimeMillis()) y.set("cooldowns."+key,value); });
        CoreApi.atomicWrite(new File(directory,p.uuid+".yml").toPath(),y.saveToString());
    }
}
