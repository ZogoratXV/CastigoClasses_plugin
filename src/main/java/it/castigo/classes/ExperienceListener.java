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
    private final it.castigo.classes.model.SupportContributions support=new it.castigo.classes.model.SupportContributions();
    public ExperienceListener(CastigoClasses plugin){this.plugin=plugin;}
    public void prune(){long now=System.currentTimeMillis();contributions.prune(now);support.prune(now);}
    private final Map<EntityRegainHealthEvent,UUID> healers=new IdentityHashMap<>();
    private record Protection(UUID mob,UUID owner,double amount){}
    private final Map<EntityDamageByEntityEvent,List<Protection>> protections=new WeakHashMap<>();
    public void prepareHeal(EntityRegainHealthEvent event,Player healer){healers.put(event,healer.getUniqueId());}
    public void finishHeal(EntityRegainHealthEvent event){healers.remove(event);}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void regain(EntityRegainHealthEvent e){
        if(!(e.getEntity() instanceof Player p))return;
        var max=p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);if(max==null)return;
        double actual=Math.min(Math.max(0,e.getAmount()),Math.max(0,max.getValue()-p.getHealth()));
        support.heal(healers.getOrDefault(e,p.getUniqueId()),p.getUniqueId(),actual,System.currentTimeMillis());
    }
    public void protectedDamage(EntityDamageByEntityEvent event,Player protector,double amount){
        Entity source=event.getDamageSource().getCausingEntity();if(source==null)source=event.getDamager();
        if(source instanceof Projectile arrow&&arrow.getShooter() instanceof Entity shooter)source=shooter;
        if(source instanceof LivingEntity&&!(source instanceof Player)&&!(source instanceof Tameable tame&&tame.isTamed())&&protections.size()<4096)
            protections.computeIfAbsent(event,k->new ArrayList<>()).add(new Protection(source.getUniqueId(),protector.getUniqueId(),Math.max(0,amount)));
    }
    @EventHandler(priority=EventPriority.MONITOR) public void protectionResult(EntityDamageByEntityEvent e){
        var saved=protections.remove(e);if(saved==null||e.isCancelled())return;
        for(var value:saved)support.protect(value.mob,value.owner,value.amount,System.currentTimeMillis());
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void damage(EntityDamageByEntityEvent e){
        if(!(e.getEntity() instanceof LivingEntity victim)||plugin.adminTools().dummy(victim))return;
        Entity source=e.getDamageSource().getCausingEntity();
        if(source==null)source=e.getDamager();
        if(source instanceof Projectile projectile&&projectile.getShooter() instanceof Entity owner)source=owner;
        if(source instanceof Tameable tame&&tame.getOwner() instanceof Player owner)source=owner;
        if(victim instanceof Player&&source instanceof LivingEntity&&!(source instanceof Player)){
            support.wound(victim.getUniqueId(),source.getUniqueId(),Math.min(Math.max(0,e.getFinalDamage()),victim.getHealth()),System.currentTimeMillis());
            if(e.isApplicable(EntityDamageEvent.DamageModifier.BLOCKING))support.protect(source.getUniqueId(),victim.getUniqueId(),Math.max(0,-e.getDamage(EntityDamageEvent.DamageModifier.BLOCKING)),System.currentTimeMillis());
        }
        if(source instanceof Player p&&plugin.profile(p)!=null&&!p.getUniqueId().equals(victim.getUniqueId()))
            contributions.record(victim.getUniqueId(),p.getUniqueId(),Math.min(Math.max(0,e.getFinalDamage()),victim.getHealth()),System.currentTimeMillis());
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void death(EntityDeathEvent e){
        if(e.getEntity() instanceof Player player)support.clearWounds(player.getUniqueId());
        int raw=e.getDroppedExp();e.setDroppedExp(0);if(plugin.adminTools().dummy(e.getEntity()))return;
        var eligible=new HashSet<UUID>();for(var p:e.getEntity().getWorld().getPlayers())if(plugin.profile(p)!=null&&!p.isDead()&&p.getLocation().distanceSquared(e.getEntity().getLocation())<=128*128)eligible.add(p.getUniqueId());
        long xp=e.getEntity() instanceof Player?0:scaled(raw);
        long now=System.currentTimeMillis();UUID mob=e.getEntity().getUniqueId();
        var credit=support.take(mob,contributions.total(mob,now,eligible),plugin.getConfig().getDouble("xp.support-max-ratio",0.35),now,eligible);
        var rewards=contributions.take(mob,xp,now,eligible,credit);
        if(rewards.isEmpty()&&xp>0&&e.getEntity().getKiller()!=null&&eligible.contains(e.getEntity().getKiller().getUniqueId()))rewards=Map.of(e.getEntity().getKiller().getUniqueId(),xp);
        for(var reward:rewards.entrySet()){var p=Bukkit.getPlayer(reward.getKey());if(p!=null)plugin.grantXp(p,reward.getValue());}
    }
    @EventHandler public void quit(org.bukkit.event.player.PlayerQuitEvent e){support.clearWounds(e.getPlayer().getUniqueId());}
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
