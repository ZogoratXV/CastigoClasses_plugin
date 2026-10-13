package it.castigo.classes;
import org.bukkit.*;
import org.bukkit.attribute.*;
import org.bukkit.inventory.*;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.*;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class RarityIntegrationTest {
    ServerMock server;CastigoClasses plugin;PlayerMock player;
    @BeforeEach void setup(){server=MockBukkit.mock();server.addSimpleWorld("world");MockBukkit.createMockPlugin("CastigoCore");plugin=MockBukkit.loadWith(DisciplineIntegrationTest.TestClasses.class,getClass().getResourceAsStream("/plugin.yml"));player=server.addPlayer();player.registerAttribute(Attribute.ARMOR);player.registerAttribute(Attribute.ATTACK_DAMAGE);plugin.applyStats(player);}
    @AfterEach void close(){MockBukkit.unmock();}
    ItemStack nativeItem(Material type,Attribute attribute,double amount,EquipmentSlotGroup slot)throws Exception{var item=new ItemStack(type);var meta=item.getItemMeta();meta.addAttributeModifier(attribute,new AttributeModifier(new NamespacedKey("test","native"),amount,AttributeModifier.Operation.ADD_NUMBER,slot));item.setItemMeta(meta);plugin.gear().register(item);return item;}
    @Test void bonusIsPercentageOfNativeBaseNotClassStatsAndOnlyWhenEquipped()throws Exception{
        var sword=nativeItem(Material.IRON_SWORD,Attribute.ATTACK_DAMAGE,5,EquipmentSlotGroup.MAINHAND);plugin.rarities().set("minecraft:iron_sword",RaritySettings.Tier.LEGENDARY);double base=player.getAttribute(Attribute.ATTACK_DAMAGE).getValue();player.getInventory().setItem(9,sword);plugin.gear().refresh(player);assertEquals(base,player.getAttribute(Attribute.ATTACK_DAMAGE).getValue());player.getInventory().setItemInMainHand(sword);plugin.gear().refresh(player);assertEquals(.6,player.getAttribute(Attribute.ATTACK_DAMAGE).getModifier(new NamespacedKey(plugin,"gear_rarity_attack")).getAmount(),1e-9);
        plugin.profile(player).level=50;plugin.gear().invalidate();assertEquals(.6,player.getAttribute(Attribute.ATTACK_DAMAGE).getModifier(new NamespacedKey(plugin,"gear_rarity_attack")).getAmount(),1e-9);player.getInventory().setItemInMainHand(null);plugin.gear().refresh(player);assertNull(player.getAttribute(Attribute.ATTACK_DAMAGE).getModifier(new NamespacedKey(plugin,"gear_rarity_attack")));
    }
    @Test void armorPercentageUsesOriginalArmorAndIsAdditionalToConfiguredProtection()throws Exception{
        var armor=nativeItem(Material.IRON_CHESTPLATE,Attribute.ARMOR,6,EquipmentSlotGroup.CHEST);plugin.rarities().set("minecraft:iron_chestplate",RaritySettings.Tier.LEGENDARY);plugin.gearSettings().put(plugin.gearSettings().get("minecraft:iron_chestplate").field("armor",2));player.getInventory().setChestplate(armor);plugin.gear().refresh(player);assertEquals(2.6,player.getAttribute(Attribute.ARMOR).getModifier(new NamespacedKey(plugin,"gear_armor")).getAmount(),1e-9);
    }
    @Test void generatedRarityIsPinnedAndTooltipRestoresWithoutLosingName()throws Exception{
        var item=nativeItem(Material.IRON_SWORD,Attribute.ATTACK_DAMAGE,5,EquipmentSlotGroup.MAINHAND);var meta=item.getItemMeta();meta.setDisplayName("Lama unica");item.setItemMeta(meta);var r=plugin.gearSettings().get("minecraft:iron_sword");plugin.rarities().set(r.id(),RaritySettings.Tier.EPIC);var decorated=plugin.itemRarities().decorate(item);assertEquals("Lama unica · Epico",ChatColor.stripColor(decorated.getItemMeta().getDisplayName()));assertEquals(NamespacedKey.fromString("castigoclasses:light_purple"),decorated.getItemMeta().getTooltipStyle());assertSame(decorated,plugin.itemRarities().decorate(decorated));var generated=plugin.gear().generate(r,new Random(1));plugin.rarities().set(r.id(),RaritySettings.Tier.COMMON);assertEquals(RaritySettings.Tier.EPIC,plugin.itemRarities().tier(generated,r.id()));assertEquals(RaritySettings.Tier.COMMON,new RaritySettings(plugin.getDataFolder().toPath().resolve("rarities.yml")).tier(r.id()));plugin.gearSettings().remove(r.id());assertEquals("Lama unica",plugin.itemRarities().decorate(decorated).getItemMeta().getDisplayName());
    }
    @Test void invalidPercentAndColorAreRejected(){assertThrows(IllegalArgumentException.class,()->new RaritySettings.Style("gold",Double.NaN,1));assertThrows(IllegalArgumentException.class,()->new RaritySettings.Style("fake",1,1));assertThrows(IllegalArgumentException.class,()->new RaritySettings.Style("gold",101,1));}
}
