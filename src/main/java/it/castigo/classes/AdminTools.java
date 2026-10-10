package it.castigo.classes;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import java.util.*;
/** Temporary training targets never drop loot or experience. */
public final class AdminTools implements Listener {
    private final CastigoClasses plugin;private final NamespacedKey key;
    private static class Session {final UUID mob;final long start=System.nanoTime();double damage;int hits;Session(UUID mob){this.mob=mob;}}
    private final Map<UUID,Session> sessions=new HashMap<>();
    public AdminTools(CastigoClasses p){plugin=p;key=new NamespacedKey(p,"training_target");}
    public boolean dummy(Entity e){return e.getPersistentDataContainer().has(key,PersistentDataType.STRING);}
    public void close(){for(UUID id:List.copyOf(sessions.keySet()))remove(id);}
    private void remove(UUID owner){var s=sessions.remove(owner);if(s!=null){var e=Bukkit.getEntity(s.mob);if(e!=null)e.remove();}}
    public void command(Player p,String action){if(!p.hasPermission("castigo.classes.admin"))return;
        if(action.equals("rimuovi")){remove(p.getUniqueId());p.sendMessage("Bersaglio di prova rimosso.");return;}
        if(action.equals("crea")){
            remove(p.getUniqueId());var direction=p.getLocation().getDirection().setY(0);if(direction.lengthSquared()<0.001)direction.setZ(1);var at=p.getLocation().add(direction.normalize().multiply(3));
            if(!at.getBlock().getType().isAir()||!at.clone().add(0,1,0).getBlock().getType().isAir())throw new IllegalArgumentException("Serve spazio libero davanti a te");
            Zombie mob=p.getWorld().spawn(at,Zombie.class,z->{z.setAI(false);z.setSilent(true);z.setPersistent(false);z.setCanPickupItems(false);z.setRemoveWhenFarAway(false);z.setCustomName("Prova · "+p.getName());z.setCustomNameVisible(true);z.getPersistentDataContainer().set(key,PersistentDataType.STRING,p.getUniqueId().toString());z.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(1024);z.setHealth(1024);});
            sessions.put(p.getUniqueId(),new Session(mob.getUniqueId()));
            plugin.getServer().getScheduler().runTaskLater(plugin,()->{var s=sessions.get(p.getUniqueId());if(s!=null&&s.mob.equals(mob.getUniqueId()))remove(p.getUniqueId());},12000);
            p.sendMessage("Bersaglio creato per 10 minuti. /classe prova report | reset | rimuovi. Nessuna XP o bottino.");return;
        }
        var s=sessions.get(p.getUniqueId());if(s==null)throw new IllegalArgumentException("Crea prima un bersaglio: /classe prova crea");
        if(action.equals("reset")){sessions.put(p.getUniqueId(),new Session(s.mob));p.sendMessage("Misure azzerate.");return;}
        if(!action.equals("report"))throw new IllegalArgumentException("Usa crea, report, reset o rimuovi");
        p.sendMessage(String.format(Locale.ROOT,"Prova: %d colpi · %.1f danni · %.2f DPS dal reset",s.hits,s.damage,s.damage/Math.max(1,(System.nanoTime()-s.start)/1e9)));
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void damage(EntityDamageEvent e){if(!dummy(e.getEntity()))return;
        if(!(e instanceof EntityDamageByEntityEvent hit)){e.setCancelled(true);return;}
        Entity attacker=hit.getDamager();if(attacker instanceof Projectile arrow&&arrow.getShooter() instanceof Entity shooter)attacker=shooter;
        if(!(attacker instanceof Player p)||!p.getUniqueId().toString().equals(e.getEntity().getPersistentDataContainer().get(key,PersistentDataType.STRING))){e.setCancelled(true);return;}
        e.setDamage(Math.min(900,e.getDamage()));
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void measured(EntityDamageEvent e){if(!dummy(e.getEntity()))return;
        var s=sessions.values().stream().filter(v->v.mob.equals(e.getEntity().getUniqueId())).findFirst().orElse(null);if(s==null)return;
        if(e.getFinalDamage()>0){s.hits++;s.damage+=Math.max(0,e.getFinalDamage());}
        if(e.getEntity() instanceof LivingEntity living){living.setHealth(living.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());}
    }
    @EventHandler(priority=EventPriority.HIGHEST) public void death(EntityDeathEvent e){if(dummy(e.getEntity())){e.getDrops().clear();e.setDroppedExp(0);}}
    @EventHandler public void quit(PlayerQuitEvent e){remove(e.getPlayer().getUniqueId());}
    @EventHandler public void unload(org.bukkit.event.world.ChunkUnloadEvent e){for(var entity:e.getChunk().getEntities())if(dummy(entity))entity.remove();}
    public void diagnose(Player p){
        p.sendMessage("Castigo: "+plugin.catalog().all().size()+" classi. LuckPerms: "+plugin.getServer().getPluginManager().isPluginEnabled("LuckPerms")+" · ItemsAdder: "+plugin.getServer().getPluginManager().isPluginEnabled("ItemsAdder"));
        int warnings=0;for(var c:plugin.catalog().all().values())if(c.parent()!=null&&!c.parent().isBlank()){
            var parent=plugin.catalog().get(c.parent());if(parent==null||plugin.classCap(c.id())<=plugin.classCap(parent.id())||parent!=null&&c.requiredLevel()>plugin.classCap(parent.id())){p.sendMessage("Cap da verificare: "+c.id());warnings++;}
            if(plugin.progressionSettings().item(c.id()).isEmpty()){p.sendMessage("Consumabile di promozione non impostato: "+c.id());warnings++;}
        }
        p.sendMessage("Controlli progressione: "+warnings+" segnalazioni. Verifica texture richiesta alla mod.");plugin.send(p,"diagnostics",new com.google.gson.JsonObject());
    }
}
