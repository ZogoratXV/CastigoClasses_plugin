package it.castigo.classes;

import com.google.gson.*;
import it.castigo.classes.config.ClassCatalog;
import it.castigo.classes.model.*;
import it.castigo.classes.storage.ProfileStore;
import it.castigo.core.CoreApi;
import net.luckperms.api.LuckPerms;
import org.bukkit.*;
import org.bukkit.attribute.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class CastigoClasses extends JavaPlugin implements Listener, PluginMessageListener, TabExecutor {
    public static final String CHANNEL="castigo:classes";
    private final Gson gson=new Gson();
    private final Map<UUID,Profile> profiles=new HashMap<>();
    private final Set<UUID> connected=new HashSet<>();
    private final Set<UUID> clientEffects=new HashSet<>();
    private final Set<UUID> healingEffects=new HashSet<>();
    private final Set<UUID> meshEffects=new HashSet<>();
    private final Map<UUID,Integer> effectPackets=new HashMap<>();
    private int effectTick=-1;
    private final Map<UUID,Long> lastRequest=new HashMap<>();
    private final Map<UUID,Long> lastCatalog=new HashMap<>();
    private ProfileStore store;
    private ClassCatalog catalog;
    private Progression progression;
    private StatPointRules statPoints;
    private SkillEngine engine;
    private SkillEquipment equipment;
    private VfxSettings vfxSettings;
    private SkillAdminGui adminGui;
    private int ticks;

    @Override public void onEnable() {
        saveDefaultConfig();
        if(!new File(getDataFolder(),"classes/mago.yml").exists()) saveResource("classes/mago.yml",false);
        if(!new File(getDataFolder(),"classes/piromante.yml.example").exists()) saveResource("classes/piromante.yml.example",false);
        if(!new File(getDataFolder(),"examples/presentation.yml.example").exists()) saveResource("examples/presentation.yml.example",false);
        for(String id:List.of("mago_bianco","mago_nero","guerriero_scudo","guerriero_due_mani","arciere"))
            if(!new File(getDataFolder(),"classes/"+id+".yml").exists())saveResource("classes/"+id+".yml",false);
        try {
            Class.forName("it.castigo.core.ParticleStyle").getMethod("read",org.bukkit.configuration.ConfigurationSection.class);
            loadConfiguration();
            getConfig().options().copyDefaults(true);
            saveConfig();
            store=new ProfileStore(new File(getDataFolder(),"players"));
        } catch(Exception e) { getLogger().severe("Configurazione non valida: "+e.getMessage()); getServer().getPluginManager().disablePlugin(this); return; }
        engine=new SkillEngine(this);
        adminGui=new SkillAdminGui(this);
        getServer().getPluginManager().registerEvents(adminGui,this);
        getServer().getPluginManager().registerEvents(this,this);
        getServer().getPluginManager().registerEvents(engine,this);
        getServer().getMessenger().registerIncomingPluginChannel(this,CHANNEL,this);
        getServer().getMessenger().registerOutgoingPluginChannel(this,CHANNEL);
        Objects.requireNonNull(getCommand("classe")).setExecutor(this);
        Objects.requireNonNull(getCommand("classe")).setTabCompleter(this);
        for(Player p:Bukkit.getOnlinePlayers()) load(p);
        scheduleTick(this::tick);
    }
    protected void scheduleTick(Runnable task) { CoreApi.repeat(this,task,5,5); }
    protected void cancelTicks() { CoreApi.cancel(this); }
    private void loadConfiguration() throws Exception {
        ClassCatalog next=new ClassCatalog(new File(getDataFolder(),"classes"));
        if(next.get(getConfig().getString("default-class","mago"))==null) throw new IllegalArgumentException("Classe predefinita inesistente");
        for(Profile p:profiles.values()) if(next.get(p.classId)==null) throw new IllegalArgumentException("Classe in uso rimossa: "+p.classId);
        Progression curve=new Progression(getConfig().getInt("max-level",100),getConfig().getDouble("xp.base",100),getConfig().getDouble("xp.growth",1.18));
        ClassCatalog.number(getConfig(),"xp.vanilla-multiplier",1,0,10000);
        ClassCatalog.number(getConfig(),"combat.strength-melee-factor",0.5,0,100);
        ClassCatalog.number(getConfig(),"combat.dexterity-speed-factor",0.005,0,1);
        ClassCatalog.number(getConfig(),"combat.global-cooldown-ms",350,100,60000);
        ClassCatalog.number(getConfig(),"autosave-seconds",60,10,3600);
        int interval=configInteger("stat-points.every-levels",2,1,1000);
        int amount=configInteger("stat-points.points-per-award",1,0,1000);
        Stats gain=new Stats(pointGain("strength",1),pointGain("dexterity",1),pointGain("health",2),
                pointGain("mana",5),pointGain("intelligence",1),pointGain("attack",1),pointGain("defense",1));
        StatPointRules pointRules=new StatPointRules(interval,amount,gain);
        SkillEquipment nextEquipment=new SkillEquipment(new File(getDataFolder(),"skill-equipment.yml"));
        VfxSettings nextVfx=new VfxSettings(new File(getDataFolder(),"vfx-overrides.json"));
        catalog=next; progression=curve; statPoints=pointRules;equipment=nextEquipment;
        vfxSettings=nextVfx;
    }
    private int configInteger(String key,int fallback,int min,int max) {
        double value=ClassCatalog.number(getConfig(),key,fallback,min,max);
        if(value!=Math.rint(value))throw new IllegalArgumentException("Numero intero richiesto: "+key);
        return (int)value;
    }
    private double pointGain(String stat,double fallback) { return ClassCatalog.number(getConfig(),"stat-points.per-point."+stat,fallback,0,1000); }
    private double strengthFactor() { return getConfig().getDouble("combat.strength-melee-factor",0.5); }
    private double dexterityFactor() { return getConfig().getDouble("combat.dexterity-speed-factor",0.005); }
    public Profile profile(Player p) { return profiles.get(p.getUniqueId()); }
    public ClassDefinition definition(Profile p) { return catalog.get(p.classId); }
    public ClassCatalog catalog() { return catalog; }
    public SkillEquipment equipment() { return equipment; }
    public SkillPresentation presentation(Player p,Skill skill) { return vfxSettings.resolve(profile(p).classId,skill,catalog.presentation(skill)); }
    public JsonObject casting(Player p) { return engine.casting(p.getUniqueId()); }
    public boolean equipped(Player p,Skill skill) {
        return profile(p)!=null&&equipment.matches(p,profile(p).classId,skill,catalog.mechanics(skill));
    }
    public void refreshCatalogs() {
        engine.shutdown();
        for(Player p:Bukkit.getOnlinePlayers())if(connected.contains(p.getUniqueId()))catalog(p);
    }
    public Stats stats(Player p) { return stats(profile(p)); }
    private Stats stats(Profile p) { return statPoints.total(p,definition(p)); }
    private void load(Player player) {
        try {
            Profile p=store.load(player.getUniqueId(),catalog.get(getConfig().getString("default-class","mago")),progression.maxLevel());
            if(catalog.get(p.classId)==null) throw new IllegalStateException("Classe salvata non trovata: "+p.classId);
            progression.add(p,0); statPoints.reconcile(p);p.normalize(definition(p),stats(p));
            store.save(p); // Persist migration/retroactive awards before the profile becomes usable.
            profiles.put(player.getUniqueId(),p); applyStats(player); displayXp(player,p);
        } catch(Exception e) { getLogger().severe("Caricamento profilo "+player.getUniqueId()+": "+e.getMessage()); player.kickPlayer("Profilo classi non disponibile. Contatta lo staff."); }
    }
    private void save(Profile p) {
        try { store.save(p); } catch(Exception e) { getLogger().severe("Salvataggio profilo "+p.uuid+": "+e.getMessage()); }
    }
    @Override public void onDisable() {
        if(engine!=null)engine.shutdown();
        cancelTicks();
        for(Player p:Bukkit.getOnlinePlayers()) {
            Profile data=profile(p); if(data!=null) save(data);
            if(connected.contains(p.getUniqueId())) send(p,"disabled",new JsonObject());
            removeModifiers(p);
        }
        profiles.clear(); connected.clear();clientEffects.clear();healingEffects.clear();meshEffects.clear();effectPackets.clear();
    }
    @EventHandler public void join(PlayerJoinEvent e) { load(e.getPlayer()); }
    @EventHandler public void quit(PlayerQuitEvent e) {
        Player p=e.getPlayer(); Profile data=profiles.remove(p.getUniqueId()); if(data!=null) save(data);
        connected.remove(p.getUniqueId()); lastRequest.remove(p.getUniqueId()); lastCatalog.remove(p.getUniqueId()); removeModifiers(p);
        clientEffects.remove(p.getUniqueId());effectPackets.remove(p.getUniqueId());
        healingEffects.remove(p.getUniqueId());meshEffects.remove(p.getUniqueId());
        engine.clear(p.getUniqueId());
    }
    @EventHandler public void respawn(PlayerRespawnEvent e) {
        getServer().getScheduler().runTask(this,()-> {
            Player p=e.getPlayer(); Profile data=profile(p); if(data==null)return;
            applyStats(p); data.resource=stats(p).mana();
            p.setHealth(p.getAttribute(Attribute.MAX_HEALTH).getValue()); sync(p);
        });
    }
    @EventHandler public void death(PlayerDeathEvent e) {
        if(profile(e.getEntity())==null)return;
        e.setDroppedExp(0); e.setKeepLevel(true); e.setNewExp(0); e.setNewLevel(0); e.setNewTotalExp(0);
        engine.clear(e.getEntity().getUniqueId());
    }
    @EventHandler public void worldChanged(PlayerChangedWorldEvent e) {
        if(engine!=null)engine.clear(e.getPlayer().getUniqueId());sync(e.getPlayer());
    }
    @EventHandler(priority=EventPriority.HIGHEST) public void xp(PlayerExpChangeEvent e) {
        if(profile(e.getPlayer())==null)return;
        long amount=(long)(Math.max(0,e.getAmount())*Math.max(0,getConfig().getDouble("xp.vanilla-multiplier",1)));
        e.setAmount(0); grantXp(e.getPlayer(),amount);
    }
    // Vanilla level consumers are disabled: MMO levels are never an enchanting/anvil currency.
    @EventHandler(ignoreCancelled=true) public void inventory(InventoryOpenEvent e) {
        if(e.getPlayer() instanceof Player p && profile(p)!=null &&
                (e.getInventory().getType()==InventoryType.ANVIL || e.getInventory().getType()==InventoryType.ENCHANTING)) {
            e.setCancelled(true); p.sendMessage("§eIncudini e tavoli da incantamento non usano i livelli MMO.");
        }
    }
    @EventHandler(ignoreCancelled=true,priority=EventPriority.HIGH) public void defense(EntityDamageByEntityEvent e) {
        if(e.getEntity() instanceof Player p && profile(p)!=null) e.setDamage(Stats.mitigate(e.getDamage(),stats(p).defense()*engine.defenseFactor(p.getUniqueId())));
    }
    public void grantXp(Player player,long amount) {
        Profile p=profile(player); if(p==null)return;
        int before=p.level; progression.add(p,amount);
        int awarded=statPoints.reconcile(p);
        if(awarded>0) {
            save(p);player.sendMessage("§6Hai guadagnato "+awarded+" punti attributo. Disponibili: "+p.availableStatPoints());
        }
        if(p.level!=before) {
            applyStats(player); player.sendMessage("§dLivello "+p.level+" raggiunto!");
            player.playSound(player.getLocation(),Sound.ENTITY_PLAYER_LEVELUP,0.6f,1.1f);
        }
        displayXp(player,p); sync(player);
    }
    private void displayXp(Player player,Profile p) {
        player.setLevel(p.level); player.setTotalExperience(0);
        player.setExp(p.level==progression.maxLevel()?1f:(float)Math.min(0.999999,(double)p.xp/progression.required(p.level)));
    }
    private void tick() {
        ticks+=5;
        for(Player player:Bukkit.getOnlinePlayers()) {
            Profile p=profile(player); if(p==null)continue;
            ClassDefinition d=definition(p);
            if(!player.isDead()) p.resource=AbilityRules.regenerate(p.resource,stats(p).mana(),d.regenPerSecond()+d.regenPerLevel()*(p.level-1),0.25);
            p.cooldowns.entrySet().removeIf(e->e.getValue()<=System.currentTimeMillis());
            displayXp(player,p); sync(player);
        }
        if(ticks>=Math.max(10,getConfig().getInt("autosave-seconds",60))*20) { profiles.values().forEach(this::save); ticks=0; }
    }
    private NamespacedKey key(String name) { return new NamespacedKey(this,name); }
    private void removeModifiers(Player p) {
        for(Attribute a:List.of(Attribute.MAX_HEALTH,Attribute.ATTACK_DAMAGE,Attribute.ATTACK_SPEED)) {
            AttributeInstance attr=p.getAttribute(a); if(attr==null)continue;
            for(AttributeModifier m:List.copyOf(attr.getModifiers())) if(m.getKey().getNamespace().equals("castigoclasses")) attr.removeModifier(m);
        }
        if(p.getHealth()>p.getAttribute(Attribute.MAX_HEALTH).getValue()) p.setHealth(p.getAttribute(Attribute.MAX_HEALTH).getValue());
    }
    private void modifier(Player p,Attribute attribute,String id,double value,AttributeModifier.Operation operation) {
        AttributeInstance a=p.getAttribute(attribute); if(a==null)return;
        AttributeModifier old=a.getModifier(key(id)); if(old!=null)a.removeModifier(old);
        a.addTransientModifier(new AttributeModifier(key(id),value,operation));
    }
    public void applyStats(Player p) {
        Stats s=stats(p); double hp=p.getHealth();
        modifier(p,Attribute.MAX_HEALTH,"health",s.health()-20,AttributeModifier.Operation.ADD_NUMBER);
        modifier(p,Attribute.ATTACK_DAMAGE,"attack",CombatMath.meleeBonus(s,strengthFactor()),AttributeModifier.Operation.ADD_NUMBER);
        modifier(p,Attribute.ATTACK_SPEED,"dexterity",CombatMath.speedBonus(s,dexterityFactor()),AttributeModifier.Operation.ADD_SCALAR);
        if(!p.isDead()) p.setHealth(Math.min(hp,p.getAttribute(Attribute.MAX_HEALTH).getValue()));
    }
    public void send(Player player,String type,JsonObject body) {
        if(!isEnabled())return;
        body.addProperty("v",1); body.addProperty("type",type);
        byte[] bytes=gson.toJson(body).getBytes(StandardCharsets.UTF_8);
        if(bytes.length<=30000) player.sendPluginMessage(this,CHANNEL,bytes);
        else getLogger().warning("Messaggio troppo grande: "+type);
    }
    private void catalog(Player player) {
        send(player,"catalog_begin",new JsonObject());
        for(ClassDefinition d:catalog.all().values()) {
            JsonObject obj=gson.toJsonTree(d).getAsJsonObject();
            for(int i=0;i<d.skills().size();i++)obj.getAsJsonArray("skills").get(i).getAsJsonObject().addProperty("icon",equipment.icon(d.id(),d.skills().get(i)));
            send(player,"class",obj);
        }
        send(player,"catalog_end",new JsonObject()); sync(player);
    }
    public void sync(Player player) {
        if(!connected.contains(player.getUniqueId()))return;
        Profile p=profile(player); if(p==null)return;
        JsonObject obj=new JsonObject();
        obj.add("casting",engine.casting(player.getUniqueId()));obj.addProperty("vfxAdmin",player.hasPermission("castigo.classes.admin"));
        obj.addProperty("world",player.getWorld().getUID().toString());
        obj.addProperty("name",net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(player.displayName())); obj.addProperty("classId",p.classId);
        obj.addProperty("level",p.level); obj.addProperty("xp",p.xp); obj.addProperty("xpNext",progression.required(p.level));
        obj.addProperty("health",player.getHealth()); obj.addProperty("maxHealth",player.getAttribute(Attribute.MAX_HEALTH).getValue());
        obj.addProperty("resource",p.resource); obj.addProperty("maxResource",stats(player).mana());
        obj.addProperty("group",group(player)); obj.add("stats",gson.toJsonTree(stats(player)));
        JsonObject points=new JsonObject(),allocated=new JsonObject();JsonArray allocatable=new JsonArray();
        for(StatAttribute stat:StatAttribute.values()) {
            allocated.addProperty(stat.id(),p.allocatedStats.getOrDefault(stat,0));
            if(player.hasPermission("castigo.classes.use")&&!player.isDead()&&statPoints.canAllocate(p,definition(p),stat,strengthFactor(),dexterityFactor()))allocatable.add(stat.id());
        }
        points.addProperty("available",p.availableStatPoints());points.addProperty("earned",p.earnedStatPoints);
        points.addProperty("everyLevels",statPoints.everyLevels());points.addProperty("pointsPerAward",statPoints.pointsPerAward());
        points.add("allocated",allocated);points.add("perPoint",gson.toJsonTree(statPoints.perPoint()));points.add("canAllocate",allocatable);
        obj.add("statPoints",points);
        JsonObject combat=new JsonObject();
        combat.addProperty("attackDamage",player.getAttribute(Attribute.ATTACK_DAMAGE).getValue());
        combat.addProperty("attackSpeed",player.getAttribute(Attribute.ATTACK_SPEED).getValue());
        combat.addProperty("meleeBonus",CombatMath.meleeBonus(stats(p),strengthFactor()));
        combat.addProperty("defenseReductionPercent",CombatMath.defenseReduction(stats(p)));
        obj.add("combat",combat);
        obj.add("slots",gson.toJsonTree(p.slots)); JsonObject cooldowns=new JsonObject();
        p.cooldowns.forEach((id,end)->cooldowns.addProperty(id,Math.max(0,end-System.currentTimeMillis())));
        obj.add("cooldowns",cooldowns); send(player,"state",obj);
    }
    private String group(Player p) {
        if(!getServer().getPluginManager().isPluginEnabled("LuckPerms"))return "";
        try {
            LuckPerms lp=getServer().getServicesManager().load(LuckPerms.class);
            var user=lp==null?null:lp.getUserManager().getUser(p.getUniqueId());
            return user==null?"":user.getPrimaryGroup();
        } catch(LinkageError e) { return ""; }
    }
    public void feedback(Player p,String message) {
        p.sendActionBar(message);
        if(connected.contains(p.getUniqueId())) { JsonObject o=new JsonObject();o.addProperty("message",message);send(p,"feedback",o); }
    }
    @Override public void onPluginMessageReceived(String channel,Player player,byte[] bytes) {
        if(!channel.equals(CHANNEL)||bytes.length>2048||profile(player)==null)return;
        long now=System.currentTimeMillis();
        if(now-lastRequest.getOrDefault(player.getUniqueId(),0L)<80)return;
        lastRequest.put(player.getUniqueId(),now);
        try {
            JsonObject o=JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();
            if(o.get("v").getAsInt()!=1) { feedback(player,"Versione mod non compatibile."); return; }
            String type=o.get("type").getAsString();
            if(type.equals("hello")) {
                if(now-lastCatalog.getOrDefault(player.getUniqueId(),0L)<1000)return;
                int vfx=o.has("clientVfx")?o.get("clientVfx").getAsInt():0;
                if(vfx==1||vfx==2)clientEffects.add(player.getUniqueId());
                else clientEffects.remove(player.getUniqueId());
                if(o.has("healingBeam")&&o.get("healingBeam").getAsInt()==1)healingEffects.add(player.getUniqueId());else healingEffects.remove(player.getUniqueId());
                if(o.has("meshVfx")&&o.get("meshVfx").getAsInt()==2)meshEffects.add(player.getUniqueId());else meshEffects.remove(player.getUniqueId());
                lastCatalog.put(player.getUniqueId(),now);connected.add(player.getUniqueId()); catalog(player); return;
            }
            if(!connected.contains(player.getUniqueId())||!player.hasPermission("castigo.classes.use"))return;
            if(type.startsWith("vfx_")) { editVfx(player,type,o);return; }
            switch(type) {
                case "cast" -> cast(player,o.get("slot").getAsInt());
                case "allocate" -> allocate(player,StatAttribute.parse(o.get("attribute").getAsString()));
                case "reorder" -> {
                    List<String> order=new ArrayList<>();o.getAsJsonArray("slots").forEach(e->order.add(e.getAsString()));
                    Profile p=profile(player);
                    if(Profile.validOrder(order,definition(p).skills().stream().map(Skill::id).toList())) {
                        p.slots.clear();p.slots.addAll(order);save(p);sync(player);
                    }
                }
                default -> { }
            }
        } catch(RuntimeException ignored) { /* Invalid/untrusted client payload: discard. */ }
    }
    private void cast(Player p,int slot) {
        if(slot<0||slot>=8||profile(p)==null||!p.hasPermission("castigo.classes.use"))return;
        engine.cast(p,definition(profile(p)).skill(profile(p).slots.get(slot)));
    }
    private void editVfx(Player p,String type,JsonObject input) {
        JsonObject reply=new JsonObject();
        try {
            String c=input.get("classId").getAsString(),sid=input.get("skillId").getAsString();
            var stage=SkillPresentation.Stage.valueOf(input.get("stage").getAsString());
            reply.addProperty("classId",c);reply.addProperty("skillId",sid);reply.addProperty("stage",stage.name());
            var definition=catalog.get(c);var skill=definition==null?null:definition.skill(sid);
            if(skill==null)throw new IllegalArgumentException("Skill sconosciuta");
            if(!Set.of("vfx_get","vfx_save","vfx_reset").contains(type))throw new IllegalArgumentException("Azione VFX sconosciuta");
            if(!type.equals("vfx_get")) {
                if(!p.hasPermission("castigo.classes.admin"))throw new IllegalArgumentException("Serve castigo.classes.admin per modificare il server");
                vfxSettings.save(c,sid,stage,type.equals("vfx_reset")?null:input.getAsJsonObject("draft"));
            }
            reply.add("draft",VfxSettings.draft(vfxSettings.resolve(c,skill,catalog.presentation(skill)).cues().get(stage)));
            reply.addProperty("message",type.equals("vfx_get")?"Preset caricato dal server":type.equals("vfx_reset")?"Ripristinato il preset della classe":"Preset salvato sul server");
            reply.addProperty("ok",true);
        } catch(Exception e) { reply.addProperty("ok",false);reply.addProperty("message","Preset non applicato: "+e.getMessage()); }
        send(p,"vfx_editor",reply);
    }
    public void stopEffect(UUID world,UUID handle) {
        JsonObject o=new JsonObject();o.addProperty("handle",handle.toString());
        for(Player observer:Bukkit.getOnlinePlayers())if(observer.getWorld().getUID().equals(world)&&meshEffects.contains(observer.getUniqueId()))send(observer,"vfx_stop",o);
    }
    public void weaponMotion(Player p,it.castigo.classes.model.Skill skill,String phase,int ticks) {
        JsonObject o=new JsonObject();o.addProperty("world",p.getWorld().getUID().toString());
        o.addProperty("player",p.getUniqueId().toString());o.addProperty("entity",p.getEntityId());
        o.addProperty("phase",phase);o.addProperty("duration",Math.clamp(ticks,0,600));
        o.addProperty("style",skill==null?"CAST":WeaponMotion.style(skill.effect()));
        boolean off=false;
        if(skill!=null&&profile(p)!=null)off=equipment().requirement(profile(p).classId,skill,catalog.mechanics(skill)).offhand();
        o.addProperty("offhand",off);
        for(Player observer:p.getWorld().getPlayers())if(clientEffects.contains(observer.getUniqueId())&&observer.getLocation().distanceSquared(p.getLocation())<=64*64)send(observer,"weapon_motion",o);
    }
    public void broadcastEffect(Location from,Location at,JsonObject effect) {
        int tick=Bukkit.getCurrentTick();if(tick!=effectTick) { effectTick=tick;effectPackets.clear(); }
        var a=(from==null?at:from).toVector();var delta=at.toVector().subtract(a);double length=delta.lengthSquared();
        for(Player observer:at.getWorld().getPlayers()) {
            UUID id=observer.getUniqueId();if(!clientEffects.contains(id)||effectPackets.getOrDefault(id,0)>=32)continue;
            var eye=observer.getEyeLocation().toVector();double t=length<0.001?0:Math.max(0,Math.min(1,eye.clone().subtract(a).dot(delta)/length));
            if(eye.distanceSquared(a.clone().add(delta.clone().multiply(t)))>64*64)continue;
            effectPackets.merge(id,1,Integer::sum);
            JsonObject outgoing=effect;
            if(effect.get("shape").getAsString().equals("HEALING_BEAM")&&!healingEffects.contains(id)) {
                outgoing=effect.deepCopy();outgoing.addProperty("shape","SPIRAL");outgoing.remove("target");
                outgoing.getAsJsonObject("sound").addProperty("id","minecraft:block.amethyst_block.chime");
            }
            if(effect.get("shape").getAsString().startsWith("MESH_")&&!meshEffects.contains(id)) {
                outgoing=effect.deepCopy();outgoing.addProperty("shape","RING");outgoing.remove("mesh");
                outgoing.getAsJsonObject("particles").addProperty("enabled",true);
            }
            send(observer,"vfx",outgoing);
        }
    }
    private void allocate(Player player,StatAttribute stat) {
        Profile p=profile(player);
        if(p==null||!player.hasPermission("castigo.classes.use")||player.isDead())return;
        var before=new EnumMap<>(p.allocatedStats);
        if(!statPoints.allocate(p,definition(p),stat,strengthFactor(),dexterityFactor())) {
            feedback(player,"Punti insufficienti oppure attributo al limite/disabilitato.");sync(player);return;
        }
        try { store.save(p); }
        catch(Exception e) {
            p.allocatedStats.clear();p.allocatedStats.putAll(before);
            getLogger().severe("Salvataggio assegnazione punti "+p.uuid+": "+e.getMessage());
            feedback(player,"Assegnazione annullata: impossibile salvare il profilo.");sync(player);return;
        }
        applyStats(player);sync(player);
        String label=stat==StatAttribute.MANA?definition(p).resourceName():stat.label();
        feedback(player,label+" +"+statPoints.perPoint().value(stat)+" | Punti rimasti: "+p.availableStatPoints());
    }
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
        try {
            if(args.length>0&&Set.of("admin","icona").contains(args[0])) {
                if(!sender.hasPermission("castigo.classes.admin")) { sender.sendMessage("Permesso mancante.");return true; }
                if(args[0].equals("admin")) {
                    if(sender instanceof Player p)adminGui.open(p);else sender.sendMessage("Apri la GUI da un giocatore.");return true;
                }
                if(args.length!=4)throw new IllegalArgumentException("Uso: /classe icona <classe> <skill> <minecraft:oggetto|texture:namespace:textures/gui/skills/nome.png|reset>");
                var definition=catalog.get(args[1]);if(definition==null||definition.skill(args[2])==null)throw new IllegalArgumentException("Classe o skill sconosciuta");
                equipment.setIcon(args[1],args[2],args[3]);refreshCatalogs();sender.sendMessage("Icona salvata e inviata alla mod.");return true;
            }
            if(args.length>0 && Set.of("reload","set","xp","livello").contains(args[0])) {
                if(!sender.hasPermission("castigo.classes.admin")) { sender.sendMessage("Permesso mancante.");return true; }
                if(args[0].equals("reload")) {
                    String oldConfig=getConfig().saveToString();
                    try { reloadConfig(); loadConfiguration(); }
                    catch(Exception invalid) { getConfig().loadFromString(oldConfig); throw invalid; }
                    engine.shutdown();
                    for(Player p:Bukkit.getOnlinePlayers()) if(profile(p)!=null) {
                        Profile profile=profile(p);progression.add(profile,0);statPoints.reconcile(profile);
                        profile.normalize(definition(profile),stats(profile));save(profile);
                        applyStats(p);if(connected.contains(p.getUniqueId()))catalog(p);
                    }
                    sender.sendMessage("Classi ricaricate.");return true;
                }
                if(args.length!=3)throw new IllegalArgumentException("Uso: /classe "+args[0]+" <giocatore> <valore>");
                Player target=Bukkit.getPlayerExact(args[1]);if(target==null||profile(target)==null)throw new IllegalArgumentException("Giocatore non disponibile");
                if(args[0].equals("xp")) {
                    long amount=Long.parseLong(args[2]);if(amount<0)throw new IllegalArgumentException("Usa XP positivi");grantXp(target,amount);
                } else if(args[0].equals("livello")) {
                    int level=Integer.parseInt(args[2]);if(level<1||level>progression.maxLevel())throw new IllegalArgumentException("Livello da 1 a "+progression.maxLevel());
                    Profile data=profile(target);engine.clear(target.getUniqueId());data.level=level;data.xp=0;statPoints.reconcile(data);
                    data.normalize(definition(data),stats(data));applyStats(target);displayXp(target,data);sync(target);
                } else changeClass(target,args[2]);
                save(profile(target));sender.sendMessage("Profilo aggiornato.");return true;
            }
            if(!(sender instanceof Player p)) { sender.sendMessage("Comandi staff: /classe set|xp|reload");return true; }
            if(profile(p)==null||!p.hasPermission("castigo.classes.use"))return true;
            Profile data=profile(p);
            if(args.length==2&&args[0].equals("assegna")) { allocate(p,StatAttribute.parse(args[1]));return true; }
            if(args.length==2&&args[0].equals("skill")) { cast(p,Integer.parseInt(args[1])-1);return true; }
            if(args.length==3&&args[0].equals("scambia")) {
                int a=Integer.parseInt(args[1])-1,b=Integer.parseInt(args[2])-1;
                if(a<0||a>7||b<0||b>7)throw new IllegalArgumentException("Slot da 1 a 8");
                Collections.swap(data.slots,a,b);save(data);sync(p);return true;
            }
            if(args.length==2&&args[0].equals("sottoclasse")) {
                ClassDefinition next=catalog.get(args[1]);
                if(next==null||!next.parent().equals(data.classId)||data.level<next.requiredLevel())throw new IllegalArgumentException("Sottoclasse non disponibile al tuo livello");
                changeClass(p,next.id());save(data);return true;
            }
            if(args.length>0&&args[0].equals("lista")) {
                catalog.all().values().forEach(d->p.sendMessage("§d"+d.id()+" §f"+d.name()+(d.parent().isEmpty()?"":" (da "+d.parent()+", lv. "+d.requiredLevel()+")")));return true;
            }
            ClassDefinition d=definition(data);Stats s=stats(p);
            p.sendMessage("§d"+d.name()+" §fLv. "+data.level+" | XP "+data.xp+"/"+progression.required(data.level));
            String rank=DisciplineRules.rank(data.level,d.skills());if(!rank.isEmpty())p.sendMessage("§6Grado: "+rank);
            p.sendMessage("§b"+d.resourceName()+": "+Math.round(data.resource)+"/"+Math.round(s.mana()));
            p.sendMessage("§7Forza "+s.strength()+" | Destrezza "+s.dexterity()+" | Vita "+s.health()+" | Intelligenza "+s.intelligence()+" | Attacco "+s.attack()+" | Difesa "+s.defense());
            p.sendMessage("§6Punti attributo: "+data.availableStatPoints()+" | /classe assegna <attributo>");
            p.sendMessage(String.format(Locale.ITALIAN,"§7Danno d'attacco: %.2f | Velocità: %.2f | Riduzione da difesa: %.2f%%",
                    p.getAttribute(Attribute.ATTACK_DAMAGE).getValue(),p.getAttribute(Attribute.ATTACK_SPEED).getValue(),CombatMath.defenseReduction(s)));
            for(int i=0;i<8;i++)p.sendMessage((i+1)+". "+d.skill(data.slots.get(i)).name());
            p.sendMessage("§7/classe skill <1-8> | scambia <1-8> <1-8> | lista | sottoclasse <id>");
        } catch(Exception e) { sender.sendMessage("§c"+e.getMessage()); }
        return true;
    }
    private void changeClass(Player p,String id) {
        ClassDefinition next=catalog.get(id);if(next==null)throw new IllegalArgumentException("Classe sconosciuta");
        Profile data=profile(p);double fraction=data.resource/Math.max(1,stats(p).mana());
        data.classId=id;data.normalize(next,stats(data));data.resource=stats(data).mana()*fraction;
        engine.clear(p.getUniqueId());applyStats(p);sync(p);p.sendMessage("§dClasse: "+next.name());
    }
    @Override public List<String> onTabComplete(CommandSender s,Command c,String label,String[] args) {
        List<String> choices=args.length==1?new ArrayList<>(List.of("lista","skill","scambia","sottoclasse","assegna")):new ArrayList<>();
        if(args.length==2&&args[0].equals("assegna"))for(StatAttribute stat:StatAttribute.values())choices.add(stat.label().toLowerCase(Locale.ROOT));
        if(args.length==1&&s.hasPermission("castigo.classes.admin"))choices.addAll(List.of("reload","set","xp","livello","admin","icona"));
        if(s.hasPermission("castigo.classes.admin")&&args[0].equals("icona")) {
            if(args.length==2)choices.addAll(catalog.all().keySet());
            if(args.length==3&&catalog.get(args[1])!=null)catalog.get(args[1]).skills().forEach(skill->choices.add(skill.id()));
            if(args.length==4)choices.addAll(List.of("reset","texture:castigo:textures/gui/skills/nome.png"));
        }
        if(args.length==2&&args[0].equals("sottoclasse")||args.length==3&&args[0].equals("set"))choices.addAll(catalog.all().keySet());
        if(args.length==2&&Set.of("set","xp","livello").contains(args[0])&&s.hasPermission("castigo.classes.admin"))Bukkit.getOnlinePlayers().forEach(p->choices.add(p.getName()));
        String prefix=args[args.length-1].toLowerCase(Locale.ROOT);return choices.stream().filter(x->x.startsWith(prefix)).toList();
    }
}


