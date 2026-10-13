package it.castigo.classes;

import it.castigo.classes.model.*;
import com.google.gson.*;
import org.bukkit.*;
import org.bukkit.attribute.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import java.util.*;

/** Server-side equipment bonuses. Items in a backpack never contribute. */
public final class GearService implements Listener {
    private final CastigoClasses plugin;
    private final java.util.function.BiFunction<ItemStack,Double,ItemStack> shieldAdapter;
    private final NamespacedKey rolls,id,serial,armorKey;
    private record Snapshot(Stats bonus,double armor,double attack) {}
    private final Map<UUID,Snapshot> previous=new HashMap<>();
    public GearService(CastigoClasses plugin){this(plugin,NativeShield::configure);}
    GearService(CastigoClasses plugin,java.util.function.BiFunction<ItemStack,Double,ItemStack> adapter){this.shieldAdapter=adapter;this.plugin=plugin;rolls=new NamespacedKey(plugin,"gear_rolls");id=new NamespacedKey(plugin,"gear_id");serial=new NamespacedKey(plugin,"gear_serial");armorKey=new NamespacedKey(plugin,"gear_armor");}
    public GearSettings.Rule rule(ItemStack item){
        if(item==null||item.getType().isAir())return null;
        try{return plugin.gearSettings().get(SkillEquipment.identify(item));}catch(ReflectiveOperationException|LinkageError|IllegalArgumentException ex){return null;}
    }
    public boolean allowed(Player p,ItemStack item){var r=rule(item);return r==null||level(p)>=r.level();}
    private int level(Player p){var data=plugin.profile(p);return data==null?1:data.level;}
    private boolean valid(Player p,ItemStack item,EquipmentSlot slot){var r=rule(item);return r!=null&&r.slot()==slot&&level(p)>=r.level()&&(!Set.of(EquipmentSlot.HAND,EquipmentSlot.OFF_HAND).contains(slot)||plugin.profile(p)==null||plugin.equipment().allowed(plugin.profile(p).classId,slot==EquipmentSlot.OFF_HAND,item));}
    public Stats bonus(Player p){
        var result=new double[7];
        for(var slot:GearSettings.SLOTS){var item=p.getInventory().getItem(slot);if(!valid(p,item,slot))continue;var r=rule(item);var random=rolled(item,r.id());
            for(var s:StatAttribute.values())result[s.ordinal()]+=r.bonus().getOrDefault(s,0d)+random.getOrDefault(s,0d);
        }
        return new Stats(result[0],result[1],Math.min(1024,result[2]),result[3],result[4],result[5],result[6]);
    }
    public static Stats add(Stats base,Stats gear){return new Stats(base.strength()+gear.strength(),base.dexterity()+gear.dexterity(),Math.min(1024,base.health()+gear.health()),base.mana()+gear.mana(),base.intelligence()+gear.intelligence(),base.attack()+gear.attack(),base.defense()+gear.defense());}
    public Map<StatAttribute,Double> rolled(ItemStack item,String expected){
        var result=new EnumMap<StatAttribute,Double>(StatAttribute.class);if(item==null||!item.hasItemMeta())return result;var data=item.getItemMeta().getPersistentDataContainer();
        if(!expected.equals(data.get(id,PersistentDataType.STRING)))return result;
        String json=data.get(rolls,PersistentDataType.STRING);if(json==null||json.length()>2048)return result;
        try{var obj=JsonParser.parseString(json).getAsJsonObject();for(var s:StatAttribute.values())if(obj.has(s.id())){double v=obj.get(s.id()).getAsDouble();if(!Double.isFinite(v)||v<0||v>1000)return Map.of();result.put(s,v);}}catch(RuntimeException ex){return Map.of();}return result;
    }
    public ItemStack generate(GearSettings.Rule r,java.util.random.RandomGenerator rng){
        ItemStack item=r.template();var meta=item.getItemMeta();var data=meta.getPersistentDataContainer();var obj=new JsonObject();var lore=new ArrayList<>(meta.hasLore()?meta.getLore():List.<String>of());
        lore.add("§6Attributi Castigo · livello "+r.level());
        for(var s:StatAttribute.values()){double lo=r.min().getOrDefault(s,0d),hi=r.max().getOrDefault(s,0d);double value=lo==hi?lo:lo+rng.nextDouble()*(hi-lo);obj.addProperty(s.id(),value);double total=r.bonus().getOrDefault(s,0d)+value;if(total>0)lore.add("§7"+s.label()+": +"+String.format(Locale.ROOT,"%.2f",total));}
        if(r.armor()>0)lore.add("§7Protezione vanilla aggiuntiva: "+r.armor());if(r.shield()>=0)lore.add("§7Parata: "+r.shield()+"%");
        data.set(id,PersistentDataType.STRING,r.id());data.set(serial,PersistentDataType.STRING,UUID.randomUUID().toString());data.set(rolls,PersistentDataType.STRING,obj.toString());meta.setLore(lore);item.setItemMeta(meta);return plugin.itemRarities().pin(item,r.id());
    }
    static EquipmentSlot suggestedSlot(Material material){
        String name=material.name();
        if(name.endsWith("HELMET")||name.endsWith("HEAD")||name.endsWith("SKULL"))return EquipmentSlot.HEAD;
        if(name.endsWith("CHESTPLATE")||material==Material.ELYTRA)return EquipmentSlot.CHEST;
        if(name.endsWith("LEGGINGS"))return EquipmentSlot.LEGS;
        if(name.endsWith("BOOTS"))return EquipmentSlot.FEET;
        return material==Material.SHIELD?EquipmentSlot.OFF_HAND:EquipmentSlot.HAND;
    }
    public void register(ItemStack source)throws Exception {
        source=shieldAdapter.apply(source,-1d);
        String key=SkillEquipment.identify(source);if(plugin.gearSettings().get(key)!=null)return;
        var meta=source.getItemMeta();if(meta.getPersistentDataContainer().has(rolls))throw new IllegalArgumentException("Seleziona un oggetto originale, senza attributi casuali");
        EquipmentSlot slot=suggestedSlot(source.getType());if(!GearSettings.SLOTS.contains(slot))slot=EquipmentSlot.HAND;
        plugin.gearSettings().put(new GearSettings.Rule(key,slot,1,0,-1,Map.of(),Map.of(),Map.of(),source));
    }
    public void refresh(Player p){
        if(p.isDead())return;
        for(var slot:List.of(EquipmentSlot.HAND,EquipmentSlot.OFF_HAND)){
            var item=p.getInventory().getItem(slot);var r=rule(item);
            double percent=r==null?-1:!allowed(p,item)?0:r.shield();
            var changed=shieldAdapter.apply(item,percent);if(changed!=item)p.getInventory().setItem(slot,changed);
        }
        for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)){
            var item=p.getInventory().getItem(slot);if(allowed(p,item))continue;
            p.getInventory().setItem(slot,null);
            for(var overflow:p.getInventory().addItem(item).values()){var drop=p.getWorld().dropItem(p.getLocation(),overflow);drop.setOwner(p.getUniqueId());drop.setPickupDelay(20);}
            plugin.feedback(p,"Livello insufficiente: armatura rimossa. Se l'inventario è pieno cade ai tuoi piedi.");
        }
        double armor=0;for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)){var item=p.getInventory().getItem(slot);if(valid(p,item,slot))armor+=rule(item).armor()+rarityBonus(item,Attribute.ARMOR,slot);}
        var held=p.getInventory().getItemInMainHand();double attack=valid(p,held,EquipmentSlot.HAND)?rarityBonus(held,Attribute.ATTACK_DAMAGE,EquipmentSlot.HAND):0;
        var next=new Snapshot(bonus(p),Math.min(30,armor),attack);
        if(!next.equals(previous.put(p.getUniqueId(),next))){
            var attribute=p.getAttribute(Attribute.ARMOR);if(attribute!=null){var old=attribute.getModifier(armorKey);if(old!=null)attribute.removeModifier(old);
            if(armor>0)attribute.addTransientModifier(new AttributeModifier(armorKey,next.armor(),AttributeModifier.Operation.ADD_NUMBER));}
            var damage=p.getAttribute(Attribute.ATTACK_DAMAGE);var rarityKey=new NamespacedKey(plugin,"gear_rarity_attack");if(damage!=null){var old=damage.getModifier(rarityKey);if(old!=null)damage.removeModifier(old);if(attack>0)damage.addTransientModifier(new AttributeModifier(rarityKey,attack,AttributeModifier.Operation.ADD_NUMBER));}
            plugin.applyStats(p);var profile=plugin.profile(p);if(profile!=null)profile.resource=Math.min(profile.resource,plugin.stats(p).mana());plugin.sync(p);
        }
    }
    private double rarityBonus(ItemStack item,Attribute attribute,EquipmentSlot slot){double pct=plugin.itemRarities().percent(item,attribute.equals(Attribute.ARMOR));return pct==0?0:ItemRarities.nativeValue(item,attribute,slot)*pct;}
    public void invalidate(){previous.clear();for(var p:Bukkit.getOnlinePlayers())refresh(p);}
    private void later(Player p){Bukkit.getScheduler().runTask(plugin,()->{if(p.isOnline())refresh(p);});}
    @EventHandler public void quit(PlayerQuitEvent e){previous.remove(e.getPlayer().getUniqueId());}
    @EventHandler(ignoreCancelled=true) public void click(InventoryClickEvent e){
        if(!(e.getWhoClicked() instanceof Player p))return;
        ItemStack candidate=e.getHotbarButton()>=0?p.getInventory().getItem(e.getHotbarButton()):e.getCursor();
        if(e.getSlotType()==InventoryType.SlotType.ARMOR&&!allowed(p,candidate))e.setCancelled(true);
        if(e.isShiftClick()&&e.getView().getTopInventory().getType()==InventoryType.CRAFTING&&e.getCurrentItem()!=null&&suggestedSlot(e.getCurrentItem().getType())!=EquipmentSlot.HAND&&!allowed(p,e.getCurrentItem()))e.setCancelled(true);
        later(p);
    }
    @EventHandler(ignoreCancelled=true) public void drag(InventoryDragEvent e){if(e.getWhoClicked() instanceof Player p){for(var entry:e.getNewItems().entrySet())if(e.getView().getSlotType(entry.getKey())==InventoryType.SlotType.ARMOR&&!allowed(p,entry.getValue()))e.setCancelled(true);later(p);}}
    @EventHandler(ignoreCancelled=true) public void held(PlayerItemHeldEvent e){later(e.getPlayer());}
    @EventHandler(ignoreCancelled=true) public void swap(PlayerSwapHandItemsEvent e){later(e.getPlayer());}
    @EventHandler(ignoreCancelled=true) public void drop(PlayerDropItemEvent e){later(e.getPlayer());}
    @EventHandler(ignoreCancelled=true) public void broken(PlayerItemBreakEvent e){later(e.getPlayer());}
    @EventHandler(priority=EventPriority.LOWEST,ignoreCancelled=true) public void use(PlayerInteractEvent e){
        if(!e.getAction().isRightClick())return;
        refresh(e.getPlayer());
        if(!allowed(e.getPlayer(),e.getItem())){e.setCancelled(true);plugin.feedback(e.getPlayer(),"Livello insufficiente per usare questo oggetto.");}else later(e.getPlayer());
    }
    @EventHandler(ignoreCancelled=true) public void dispense(org.bukkit.event.block.BlockDispenseArmorEvent e){if(e.getTargetEntity() instanceof Player p&&!allowed(p,e.getItem()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.LOWEST,ignoreCancelled=true) public void shoot(EntityShootBowEvent e){if(e.getEntity() instanceof Player p&&!allowed(p,e.getBow())){e.setCancelled(true);e.setConsumeItem(false);}}
    @EventHandler(priority=EventPriority.LOWEST,ignoreCancelled=true) public void launch(ProjectileLaunchEvent e){if(e.getEntity() instanceof Trident trident&&trident.getShooter() instanceof Player p&&!allowed(p,trident.getItemStack()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.LOWEST,ignoreCancelled=true) public void hit(EntityDamageByEntityEvent e){if(e.getEntity() instanceof Player victim)refresh(victim);if(e.getDamager() instanceof Player p){refresh(p);if(!allowed(p,p.getInventory().getItemInMainHand()))e.setCancelled(true);}}
    public void restoreShields(){
        for(var p:Bukkit.getOnlinePlayers())for(int slot=0;slot<p.getInventory().getSize();slot++){
            var item=p.getInventory().getItem(slot);var restored=shieldAdapter.apply(item,-1d);if(restored!=item)p.getInventory().setItem(slot,restored);
        }
    }
}
