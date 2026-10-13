package dev.lone.itemsadder.api;
/** Test fixture for the optional public API; never packaged in the plugin. */
public final class CustomStack {
    private final String id;
    private CustomStack(String id){this.id=id;}
    public static CustomStack byItemStack(org.bukkit.inventory.ItemStack item){
        if(item==null||!item.hasItemMeta())return null;
        String id=item.getItemMeta().getPersistentDataContainer().get(new org.bukkit.NamespacedKey("itemsadder","test_id"),org.bukkit.persistence.PersistentDataType.STRING);
        return id==null?null:new CustomStack(id);
    }
    public String getNamespacedID(){return id;}
}
