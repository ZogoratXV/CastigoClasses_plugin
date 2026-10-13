package it.castigo.classes;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import java.util.*;

public final class CodexService implements Listener {
    private final CastigoClasses plugin;private final CodexProgress progress;
    private record Remembered(String id,long time){}
    private final Map<UUID,Remembered> mobs=new LinkedHashMap<>();
    private long nextWarning;
    public CodexService(CastigoClasses plugin){this.plugin=plugin;progress=new CodexProgress(plugin.getDataFolder().toPath().resolve("codex-players"));}
    public CodexProgress.Data progress(Player p)throws Exception{return progress.get(p.getUniqueId());}
    private void failure(Exception ex){if(System.currentTimeMillis()>nextWarning){nextWarning=System.currentTimeMillis()+60000;plugin.getLogger().warning("Codex: "+ex.getMessage());}}
    public void scan(Player p){
        if(!plugin.codexSettings().enabled())return;
        try{
            var found=new HashSet<String>();for(var item:p.getInventory().getContents())if(item!=null&&!item.getType().isAir()){var entry=plugin.codexSettings().identity(SkillEquipment.identify(item));if(entry!=null&&entry.kind()==CodexSettings.Kind.WEAPON)found.add(entry.key());}
            if(progress.discover(p.getUniqueId(),found))plugin.feedback(p,"Codex: nuovi aspetti di armi registrati.");
            var data=progress(p);for(int i=0;i<p.getInventory().getSize();i++){var item=p.getInventory().getItem(i);String skin=WeaponSkins.source(item);if(skin==null)continue;var entry=plugin.codexSettings().get(skin);var target=plugin.codexSettings().identity(SkillEquipment.identify(item));if(entry==null||!data.found().contains(skin)||!compatible(target,entry))p.getInventory().setItem(i,WeaponSkins.restore(item));}
        }catch(Exception ex){failure(ex);}
    }
    public static boolean compatible(CodexSettings.Entry target,CodexSettings.Entry source){return target!=null&&source!=null&&target.kind()==CodexSettings.Kind.WEAPON&&source.kind()==CodexSettings.Kind.WEAPON&&!target.category().equals("da_configurare")&&target.category().equals(source.category());}
    public void skin(Player p,String key)throws Exception{
        requireEnabled();scan(p);var item=p.getInventory().getItemInMainHand();var target=plugin.codexSettings().identity(SkillEquipment.identify(item));var source=plugin.codexSettings().get(key);
        if(!progress(p).found().contains(key))throw new IllegalArgumentException("Aspetto non ancora collezionato");
        if(!compatible(target,source))throw new IllegalArgumentException("Serve un'arma della stessa categoria esatta nella mano principale");
        p.getInventory().setItemInMainHand(WeaponSkins.apply(item,source.icon(),source.key()));plugin.gear().refresh(p);
    }
    public void restore(Player p)throws Exception{requireEnabled();var item=p.getInventory().getItemInMainHand();p.getInventory().setItemInMainHand(WeaponSkins.restore(item));}
    public int count(Player p,String category)throws Exception{var found=progress(p).found();return (int)plugin.codexSettings().entries().stream().filter(e->e.category().equals(category)&&found.contains(e.key())).count();}
    public void claim(Player p,String id)throws Exception{
        requireEnabled();var reward=plugin.codexSettings().reward(id);if(reward==null||count(p,reward.category())<reward.threshold())throw new IllegalArgumentException("Traguardo non raggiunto");
        if(reward.item()==null&&reward.command().isBlank())throw new IllegalArgumentException("Ricompensa non ancora configurata");
        int free=-1;var storage=p.getInventory().getStorageContents();for(int i=0;i<storage.length;i++)if(storage[i]==null||storage[i].getType().isAir()){free=i;break;}
        if(reward.item()!=null&&free<0)throw new IllegalArgumentException("Libera uno slot nell'inventario");
        // Durable reservation precedes external side effects: repeated clicks cannot issue rewards twice.
        if(!progress.reserve(p.getUniqueId(),id))throw new IllegalArgumentException("Ricompensa già riscossa");
        if(reward.item()!=null)p.getInventory().setItem(free,reward.item());
        if(!reward.command().isBlank()){
            String command=reward.command().replace("{player}",p.getName()).replace("{uuid}",p.getUniqueId().toString());
            if(!Bukkit.dispatchCommand(Bukkit.getConsoleSender(),command.startsWith("/")?command.substring(1):command)){plugin.getLogger().warning("Ricompensa Codex "+id+" riservata a "+p.getUniqueId()+": comando non riuscito. Verifica manuale richiesta.");p.sendMessage("§eComando ricompensa non riuscito: contatta lo staff.");return;}
        }
        p.sendMessage("§aRicompensa del Codex riscossa.");
    }
    private void requireEnabled(){if(!plugin.codexSettings().enabled())throw new IllegalArgumentException("Codex disabilitato");}
    @EventHandler(priority=EventPriority.MONITOR) public void acquired(io.papermc.paper.event.player.PlayerInventorySlotChangeEvent e){
        if(!plugin.codexSettings().enabled()||e.getNewItemStack().getType().isAir())return;
        try{var entry=plugin.codexSettings().identity(SkillEquipment.identify(e.getNewItemStack()));if(entry!=null&&entry.kind()==CodexSettings.Kind.WEAPON&&progress.discover(e.getPlayer().getUniqueId(),Set.of(entry.key())))plugin.feedback(e.getPlayer(),"Codex: nuovo aspetto di arma registrato.");}catch(Exception ex){failure(ex);}
    }
    public String mobIdentity(LivingEntity entity)throws Exception{
        var mythic=Bukkit.getPluginManager().getPlugin("MythicMobs");
        if(mythic!=null&&mythic.isEnabled()){
            var api=Class.forName("io.lumine.mythic.bukkit.MythicBukkit",true,mythic.getClass().getClassLoader());var instance=api.getMethod("inst").invoke(null);var manager=api.getMethod("getMobManager").invoke(instance);
            var active=(Optional<?>)manager.getClass().getMethod("getActiveMob",UUID.class).invoke(manager,entity.getUniqueId());
            if(active.isPresent()){var mob=active.get();var type=mob.getClass().getMethod("getType").invoke(mob);return "mythic:"+type.getClass().getMethod("getInternalName").invoke(type);}
        }
        return entity.getType().getKey().toString();
    }
    private static Player attacker(Entity entity){return entity instanceof Player p?p:entity instanceof Projectile projectile&&projectile.getShooter() instanceof Player p?p:null;}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void remember(EntityDamageByEntityEvent e){
        if(!plugin.codexSettings().enabled()||attacker(e.getDamager())==null||!(e.getEntity() instanceof LivingEntity living)||living instanceof Player)return;
        try{mobs.put(living.getUniqueId(),new Remembered(mobIdentity(living),System.currentTimeMillis()));if(mobs.size()>2048)mobs.remove(mobs.keySet().iterator().next());}catch(Exception ex){failure(ex);}
    }
    @EventHandler(priority=EventPriority.MONITOR) public void death(EntityDeathEvent e){
        var remembered=mobs.remove(e.getEntity().getUniqueId());if(!plugin.codexSettings().enabled()||e.getEntity() instanceof Player)return;Player killer=e.getEntity().getKiller();if(killer==null)return;
        try{String identity=remembered!=null&&System.currentTimeMillis()-remembered.time()<300000?remembered.id():mobIdentity(e.getEntity());var entry=plugin.codexSettings().identity(identity);if(entry!=null&&entry.kind()!=CodexSettings.Kind.WEAPON&&progress.discover(killer.getUniqueId(),Set.of(entry.key())))plugin.feedback(killer,"Codex: "+entry.name()+" abbattuto e registrato.");}catch(Exception ex){failure(ex);}
    }
    @EventHandler(priority=EventPriority.LOWEST) public void book(PlayerInteractEvent e){
        if(!e.getAction().isRightClick()||e.getItem()==null||plugin.codexSettings().book().isEmpty()||!plugin.codexSettings().enabled())return;
        try{if(SkillEquipment.identify(e.getItem()).equals(plugin.codexSettings().book())){e.setCancelled(true);var main=e.getPlayer().getInventory().getItemInMainHand();if(e.getHand()==EquipmentSlot.HAND||main.getType().isAir()||!SkillEquipment.identify(main).equals(plugin.codexSettings().book()))plugin.openCodex(e.getPlayer(),false);}}catch(Exception ex){failure(ex);}
    }
    @EventHandler public void quit(PlayerQuitEvent e){progress.forget(e.getPlayer().getUniqueId());}
}
