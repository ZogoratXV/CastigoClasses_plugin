package it.castigo.classes;
import com.google.gson.*;
import org.bukkit.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/** Only model fields are copied. Base material, PDC identity, damage and modifiers stay intact. */
public final class WeaponSkins {
    private static final NamespacedKey ORIGINAL=new NamespacedKey("castigoclasses","skin_original"),SOURCE=new NamespacedKey("castigoclasses","skin_source");
    private WeaponSkins(){}
    public static String source(ItemStack item){return item==null||!item.hasItemMeta()?null:item.getItemMeta().getPersistentDataContainer().get(SOURCE,PersistentDataType.STRING);}
    private static JsonObject appearance(ItemMeta meta){
        var json=new JsonObject();if(meta.hasItemModel())json.addProperty("model",meta.getItemModel().toString());
        if(meta.hasCustomModelDataComponent()){var model=meta.getCustomModelDataComponent();var g=new Gson();json.add("floats",g.toJsonTree(model.getFloats()));json.add("flags",g.toJsonTree(model.getFlags()));json.add("strings",g.toJsonTree(model.getStrings()));json.add("colors",g.toJsonTree(model.getColors().stream().map(Color::asRGB).toList()));}return json;
    }
    public static ItemStack apply(ItemStack target,ItemStack source,String entry){
        var result=target.clone();var meta=result.getItemMeta();var data=meta.getPersistentDataContainer();
        if(!data.has(ORIGINAL))data.set(ORIGINAL,PersistentDataType.STRING,appearance(meta).toString());
        var visual=source.getItemMeta();meta.setItemModel(visual.hasItemModel()?visual.getItemModel():source.getType().getKey());meta.setCustomModelDataComponent(visual.hasCustomModelDataComponent()?visual.getCustomModelDataComponent():null);
        data.set(SOURCE,PersistentDataType.STRING,entry);result.setItemMeta(meta);return result;
    }
    public static ItemStack restore(ItemStack target){
        if(source(target)==null)return target;var result=target.clone();var meta=result.getItemMeta();var data=meta.getPersistentDataContainer();String saved=data.get(ORIGINAL,PersistentDataType.STRING);
        if(saved==null)throw new IllegalArgumentException("Aspetto originale mancante");var json=JsonParser.parseString(saved).getAsJsonObject();meta.setItemModel(json.has("model")?NamespacedKey.fromString(json.get("model").getAsString()):null);
        if(json.has("floats")){var model=meta.getCustomModelDataComponent();model.setFloats(json.getAsJsonArray("floats").asList().stream().map(JsonElement::getAsFloat).toList());model.setFlags(json.getAsJsonArray("flags").asList().stream().map(JsonElement::getAsBoolean).toList());model.setStrings(json.getAsJsonArray("strings").asList().stream().map(JsonElement::getAsString).toList());model.setColors(json.getAsJsonArray("colors").asList().stream().map(e->Color.fromRGB(e.getAsInt())).toList());meta.setCustomModelDataComponent(model);}else meta.setCustomModelDataComponent(null);
        data.remove(ORIGINAL);data.remove(SOURCE);result.setItemMeta(meta);return result;
    }
}
