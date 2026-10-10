package it.castigo.classes;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

public final class HudSettings {
    private final Path file;
    private JsonObject data=new JsonObject();
    public HudSettings(Path file) throws Exception {
        this.file=file;
        if(Files.exists(file)) {
            if(Files.size(file)>524288)throw new IllegalArgumentException("File HUD troppo grande");
            data=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            if(data.size()>256)throw new IllegalArgumentException("Massimo 256 temi HUD");
            for(String group:data.keySet()){validateGroup(group);theme(group);priority(group);}
        }
    }
    public static void validateGroup(String group){if(!group.matches("[a-z0-9_.-]{1,64}"))throw new IllegalArgumentException("Nome gruppo LuckPerms non valido");}
    public Set<String> groups(){return Set.copyOf(data.keySet());}
    public HudTheme theme(String group){var e=data.getAsJsonObject(group);return new HudTheme(e==null?null:e.getAsJsonObject("layout"));}
    public int priority(String group){var e=data.getAsJsonObject(group);int n=e==null?0:e.get("priority").getAsInt();if(n<0||n>10000)throw new IllegalArgumentException("Priorità 0–10000");return n;}
    public HudTheme resolve(Collection<String> groups,String primary) {
        String winner=groups.stream().filter(data::has).sorted(Comparator.<String>comparingInt(this::priority).reversed()
                .thenComparingInt(g->g.equals(primary)?0:1).thenComparing(Comparator.naturalOrder())).findFirst().orElse("default");
        return theme(winner);
    }
    public void set(String group,String field,String value) throws Exception {
        validateGroup(group);var next=data.deepCopy();var entry=new JsonObject();int priority=priority(group);HudTheme theme=theme(group);
        if(field.equals("priority")){priority=Integer.parseInt(value);if(priority<0||priority>10000)throw new IllegalArgumentException("Priorità 0–10000");}
        else theme=theme.with(field,value);
        if(!data.has(group)&&data.size()>=256)throw new IllegalArgumentException("Massimo 256 temi HUD");
        entry.addProperty("priority",priority);entry.add("layout",theme.json());next.add(group,entry);save(next);
    }
    public void preset(String group,String id)throws Exception{
        validateGroup(group);if(!data.has(group)&&data.size()>=256)throw new IllegalArgumentException("Massimo 256 temi HUD");
        var next=data.deepCopy();var entry=new JsonObject();entry.addProperty("priority",priority(group));entry.add("layout",HudPresets.theme(id).json());next.add(group,entry);save(next);
    }
    public void reset(String group)throws Exception{var next=data.deepCopy();next.remove(group);save(next);}
    private void save(JsonObject next)throws Exception {
        Files.createDirectories(file.getParent());Path temp=Files.createTempFile(file.getParent(),"hud-",".tmp");
        try {Files.writeString(temp,new GsonBuilder().setPrettyPrinting().create().toJson(next));
            try{Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException e){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}data=next;
        }finally{Files.deleteIfExists(temp);}
    }
}
