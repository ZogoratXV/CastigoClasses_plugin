package it.castigo.classes;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.BlocksAttacks;
import io.papermc.paper.datacomponent.item.blocksattacks.DamageReduction;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/** Modify native blocking before damage is calculated, preserving armor/enchantment ordering. */
final class NativeShield {
    private static final NamespacedKey BACKUP=new NamespacedKey("castigoclasses","shield_original");
    private static final NamespacedKey APPLIED=new NamespacedKey("castigoclasses","shield_percent");
    static ItemStack configure(ItemStack item,double percent){
        if(item==null||item.getType().isAir())return item;
        byte[] backup=item.hasItemMeta()?item.getItemMeta().getPersistentDataContainer().get(BACKUP,PersistentDataType.BYTE_ARRAY):null;
        if(percent<0){
            if(backup==null)return item;
            var original=ItemStack.deserializeBytes(backup);var result=item.clone();
            if(!original.isDataOverridden(DataComponentTypes.BLOCKS_ATTACKS))result.resetData(DataComponentTypes.BLOCKS_ATTACKS);
            else if(original.hasData(DataComponentTypes.BLOCKS_ATTACKS))result.setData(DataComponentTypes.BLOCKS_ATTACKS,original.getData(DataComponentTypes.BLOCKS_ATTACKS));else result.unsetData(DataComponentTypes.BLOCKS_ATTACKS);
            var meta=result.getItemMeta();meta.getPersistentDataContainer().remove(BACKUP);meta.getPersistentDataContainer().remove(APPLIED);result.setItemMeta(meta);return result;
        }
        if(backup!=null&&Double.valueOf(percent).equals(item.getItemMeta().getPersistentDataContainer().get(APPLIED,PersistentDataType.DOUBLE)))return item;
        var original=backup==null?item:ItemStack.deserializeBytes(backup);
        var component=original.getData(DataComponentTypes.BLOCKS_ATTACKS);if(component==null)return item;
        var builder=BlocksAttacks.blocksAttacks().blockDelaySeconds(component.blockDelaySeconds()).disableCooldownScale(component.disableCooldownScale()).itemDamage(component.itemDamage()).bypassedBy(component.bypassedBy()).blockSound(component.blockSound()).disableSound(component.disableSound());
        for(var reduction:component.damageReductions()){
            var damage=DamageReduction.damageReduction().horizontalBlockingAngle(reduction.horizontalBlockingAngle()).base((float)(reduction.base()*percent/100)).factor((float)(reduction.factor()*percent/100));
            if(reduction.type()!=null)damage.type(reduction.type());
            builder.addDamageReduction(damage.build());
        }
        var next=builder.build();if(next.equals(item.getData(DataComponentTypes.BLOCKS_ATTACKS)))return item;
        var result=item.clone();if(backup==null){var meta=result.getItemMeta();meta.getPersistentDataContainer().set(BACKUP,PersistentDataType.BYTE_ARRAY,item.serializeAsBytes());result.setItemMeta(meta);}
        result.setData(DataComponentTypes.BLOCKS_ATTACKS,next);var meta=result.getItemMeta();meta.getPersistentDataContainer().set(APPLIED,PersistentDataType.DOUBLE,percent);result.setItemMeta(meta);return result;
    }
}
