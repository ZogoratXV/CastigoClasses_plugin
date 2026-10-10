package it.castigo.classes;

import it.castigo.classes.model.DamageContributions;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerFishEvent;
import java.util.*;

public final class ExperienceListener implements Listener {
    private final CastigoClasses plugin;
    private final DamageContributions contributions=new DamageContributions();
    public ExperienceListener(CastigoClasses plugin){this.plugin=plugin;}
    public void prune(){contributions.prune(System.currentTimeMillis());}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void damage(EntityDamageByEntityEvent e){
        if(!(e.getEntity() instanceof LivingEntity victim))return;
        Entity source=e.getDamageSource().getCausingEntity();
        if(source==null)source=e.getDamager();
        if(source instanceof Projectile projectile&&projectile.getShooter() instanceof Entity owner)source=owner;
        if(source instanceof Tameable tame&&tame.getOwner() instanceof Player owner)source=owner;
        if(source instanceof Player p&&plugin.profile(p)!=null&&!p.getUniqueId().equals(victim.getUniqueId()))
            contributions.record(victim.getUniqueId(),p.getUniqueId(),Math.min(Math.max(0,e.getFinalDamage()),victim.getHealth()),System.currentTimeMillis());
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void death(EntityDeathEvent e){
        int raw=e.getDroppedExp();e.setDroppedExp(0);
        var eligible=new HashSet<UUID>();for(var p:e.getEntity().getWorld().getPlayers())if(plugin.profile(p)!=null&&!p.isDead()&&p.getLocation().distanceSquared(e.getEntity().getLocation())<=128*128)eligible.add(p.getUniqueId());
        long xp=e.getEntity() instanceof Player?0:scaled(raw);
        var rewards=contributions.take(e.getEntity().getUniqueId(),xp,System.currentTimeMillis(),eligible);
        if(rewards.isEmpty()&&xp>0&&e.getEntity().getKiller()!=null&&eligible.contains(e.getEntity().getKiller().getUniqueId()))rewards=Map.of(e.getEntity().getKiller().getUniqueId(),xp);
        for(var reward:rewards.entrySet()){var p=Bukkit.getPlayer(reward.getKey());if(p!=null)plugin.grantXp(p,reward.getValue());}
    }
    private long scaled(int xp){return (long)(Math.max(0,xp)*plugin.getConfig().getDouble("xp.vanilla-multiplier",1));}
    private void award(Player p,int xp){if(plugin.profile(p)!=null)plugin.grantXp(p,scaled(xp));}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void block(BlockBreakEvent e){int xp=e.getExpToDrop();e.setExpToDrop(0);award(e.getPlayer(),xp);}
    @EventHandler(priority=EventPriority.HIGHEST) public void furnace(FurnaceExtractEvent e){int xp=e.getExpToDrop();e.setExpToDrop(0);award(e.getPlayer(),xp);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void fishing(PlayerFishEvent e){int xp=e.getExpToDrop();e.setExpToDrop(0);award(e.getPlayer(),xp);}
    @EventHandler(priority=EventPriority.HIGHEST) public void bottle(ExpBottleEvent e){int xp=e.getExperience();e.setExperience(0);if(e.getEntity().getShooter() instanceof Player p)award(p,xp);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void breed(EntityBreedEvent e){int xp=e.getExperience();e.setExperience(0);if(e.getBreeder() instanceof Player p)award(p,xp);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void spawn(EntitySpawnEvent e){
        if(e.getEntity() instanceof ExperienceOrb orb){
            e.setCancelled(true);
            if(orb.getSpawnReason()==ExperienceOrb.SpawnReason.VILLAGER_TRADE||orb.getSpawnReason()==ExperienceOrb.SpawnReason.GRINDSTONE){
                UUID id=orb.getTriggerEntityId();Player p=id==null?null:Bukkit.getPlayer(id);if(p!=null)award(p,orb.getExperience());
            }
        }
    }
    @EventHandler public void chunk(org.bukkit.event.world.ChunkLoadEvent e){for(var entity:e.getChunk().getEntities())if(entity instanceof ExperienceOrb)entity.remove();}
}
