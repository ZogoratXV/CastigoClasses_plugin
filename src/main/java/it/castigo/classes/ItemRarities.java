package it.castigo.classes;
import org.bukkit.*;
import org.bukkit.attribute.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import java.util.*;

public final class ItemRarities implements Listener {
    private final CastigoClasses plugin;
    private static final NamespacedKey TIER=key("rarity"),STAMP=key("rarity_style"),NAME=key("rarity_name"),TOOLTIP=key("rarity_tooltip");
    private static NamespacedKey key(String name){return new NamespacedKey("castigoclasses",name);}
    public ItemRarities(CastigoClasses plugin){this.plugin=plugin;}
    public RaritySettings.Tier tier(ItemStack item,String id){
        if(item!=null&&item.hasItemMeta()){String saved=item.getItemMeta().getPersistentDataContainer().get(TIER,PersistentDataType.STRING);try{if(saved!=null)return RaritySettings.Tier.valueOf(saved);}catch(IllegalArgumentException ignored){}}
        return plugin.rarities().tier(id);
    }
    public ItemStack pin(ItemStack item,String id){var meta=item.getItemMeta();meta.getPersistentDataContainer().set(TIER,PersistentDataType.STRING,plugin.rarities().tier(id).name());item.setItemMeta(meta);return decorate(item);}
    private static Component color(Component text,NamedTextColor color){return text.color(color).children(text.children().stream().map(c->color(c,color)).toList());}
    public ItemStack decorate(ItemStack source){
        if(source==null||source.getType().isAir())return source;
        var rule=plugin.gear().rule(source);var meta=source.getItemMeta();var data=meta.getPersistentDataContainer();String stamp=data.get(STAMP,PersistentDataType.STRING);
        if(rule==null&&stamp==null)return source;
        if(rule==null){var item=source.clone();String original=data.get(NAME,PersistentDataType.STRING);meta.displayName(original==null||original.equals("null")?null:GsonComponentSerializer.gson().deserialize(original));String tooltip=data.get(TOOLTIP,PersistentDataType.STRING);meta.setTooltipStyle(tooltip==null||tooltip.isEmpty()?null:NamespacedKey.fromString(tooltip));data.remove(STAMP);data.remove(NAME);data.remove(TOOLTIP);item.setItemMeta(meta);return item;}
        var tier=tier(source,rule.id());var style=plugin.rarities().style(tier);String signature=tier.name()+":"+style.color();if(signature.equals(stamp))return source;
        if(stamp==null){data.set(NAME,PersistentDataType.STRING,meta.hasDisplayName()?GsonComponentSerializer.gson().serialize(meta.displayName()):"null");data.set(TOOLTIP,PersistentDataType.STRING,meta.hasTooltipStyle()?meta.getTooltipStyle().toString():"");}
        String original=data.get(NAME,PersistentDataType.STRING);Component name=original==null||original.equals("null")?(meta.hasItemName()?meta.itemName():Component.translatable(source.getType().translationKey())):GsonComponentSerializer.gson().deserialize(original);
        var color=NamedTextColor.NAMES.value(style.color());meta.displayName(color(name,color).append(Component.text(" · "+tier.label,color)));meta.setTooltipStyle(new NamespacedKey("castigoclasses",style.color()));data.set(STAMP,PersistentDataType.STRING,signature);
        var item=source.clone();item.setItemMeta(meta);return item;
    }
    public void refresh(Player p){for(int i=0;i<p.getInventory().getSize();i++){var item=p.getInventory().getItem(i);var next=decorate(item);if(next!=item)p.getInventory().setItem(i,next);}}
    public double percent(ItemStack item,boolean armor){var rule=plugin.gear().rule(item);if(rule==null)return 0;var style=plugin.rarities().style(tier(item,rule.id()));return (armor?style.armor():style.damage())/100;}
    /** Read native item attributes, excluding the character's class and allocated stats. */
    public static double nativeValue(ItemStack item,Attribute attribute,EquipmentSlot slot){
        if(item==null||item.getType().isAir())return 0;
        var modifiers=item.getItemMeta().getAttributeModifiers();if(modifiers==null)modifiers=item.getType().getDefaultAttributeModifiers();
        double base=attribute.equals(Attribute.ATTACK_DAMAGE)?1:0,scalar=0,total=1;
        boolean found=false;for(var modifier:modifiers.get(attribute))if(modifier.getSlotGroup().test(slot)){
            found=true;switch(modifier.getOperation()){case ADD_NUMBER->base+=modifier.getAmount();case ADD_SCALAR->scalar+=modifier.getAmount();case MULTIPLY_SCALAR_1->total*=1+modifier.getAmount();}
        }
        return found?Math.max(0,base*(1+scalar)*total):0;
    }
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true) public void bow(EntityShootBowEvent e){if(e.getEntity() instanceof Player&&e.getProjectile() instanceof AbstractArrow arrow)arrow.setDamage(arrow.getDamage()*(1+percent(e.getBow(),false)));}
    @EventHandler(priority=EventPriority.NORMAL,ignoreCancelled=true) public void trident(EntityDamageByEntityEvent e){if(e.getDamager() instanceof Trident t&&t.getShooter() instanceof Player)e.setDamage(e.getDamage()*(1+percent(t.getItemStack(),false)));}
}
