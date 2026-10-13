package it.castigo.classes;

import it.castigo.classes.model.*;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.entity.*;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.*;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class GearIntegrationTest {
    ServerMock server;CastigoClasses plugin;PlayerMock player;
    @BeforeEach void setup(){server=MockBukkit.mock();server.addSimpleWorld("world");MockBukkit.createMockPlugin("CastigoCore");plugin=MockBukkit.loadWith(DisciplineIntegrationTest.TestClasses.class,getClass().getResourceAsStream("/plugin.yml"));player=server.addPlayer();player.registerAttribute(Attribute.ARMOR);}
    @AfterEach void close(){MockBukkit.unmock();}
    GearSettings.Rule register(Material m,EquipmentSlot slot)throws Exception{var item=new ItemStack(m);plugin.gear().register(item);var rule=plugin.gearSettings().get(m.getKey().toString()).slot(slot);plugin.gearSettings().put(rule);return rule;}
    @Test void onlyEquippedCorrectSlotAddsStatsAndVanillaArmor()throws Exception{
        var r=register(Material.IRON_CHESTPLATE,EquipmentSlot.CHEST).field("bonus.health",15).field("bonus.strength",3).field("armor",4);plugin.gearSettings().put(r);
        double original=plugin.stats(player).health(),strength=plugin.stats(player).strength();
        player.getInventory().setItem(9,r.template());assertEquals(original,plugin.stats(player).health());
        player.getInventory().setItemInMainHand(r.template());assertEquals(original,plugin.stats(player).health());
        player.setHealth(5);player.getInventory().setChestplate(r.template());plugin.gear().refresh(player);assertEquals(5,player.getHealth());
        assertEquals(original+15,plugin.stats(player).health());assertEquals(strength+3,plugin.stats(player).strength());assertEquals(4,player.getAttribute(Attribute.ARMOR).getValue());
        player.getInventory().setChestplate(null);plugin.gear().refresh(player);assertEquals(original,player.getAttribute(Attribute.MAX_HEALTH).getValue());assertEquals(0,player.getAttribute(Attribute.ARMOR).getValue());
    }
    @Test void levelBlocksWeaponAndRemovesArmorWithoutLosingIt()throws Exception{
        var sword=register(Material.IRON_SWORD,EquipmentSlot.HAND).field("level",20).field("bonus.attack",8);plugin.gearSettings().put(sword);player.getInventory().setItemInMainHand(sword.template());
        assertFalse(plugin.gear().allowed(player,sword.template()));assertEquals(0,plugin.gear().bonus(player).attack());
        var enemy=server.addPlayer();var hit=new EntityDamageByEntityEvent(player,enemy,EntityDamageEvent.DamageCause.ENTITY_ATTACK,8);server.getPluginManager().callEvent(hit);assertTrue(hit.isCancelled());
        var armor=register(Material.IRON_HELMET,EquipmentSlot.HEAD).field("level",20);plugin.gearSettings().put(armor);player.getInventory().setHelmet(armor.template());plugin.gear().refresh(player);
        assertTrue(player.getInventory().getHelmet()==null||player.getInventory().getHelmet().getType().isAir());assertTrue(player.getInventory().contains(Material.IRON_HELMET));
        plugin.profile(player).level=20;assertTrue(plugin.gear().allowed(player,sword.template()));assertEquals(8,plugin.gear().bonus(player).attack());
    }
    @Test void randomRollPersistsAcrossSerializationAndDoesNotRerollWithSettings()throws Exception{
        var r=register(Material.IRON_SWORD,EquipmentSlot.HAND).field("bonus.attack",2).field("max.attack",7).field("min.attack",3);plugin.gearSettings().put(r);
        var item=plugin.gear().generate(r,new Random(14));var before=plugin.gear().rolled(item,r.id());assertTrue(before.get(StatAttribute.ATTACK)>=3&&before.get(StatAttribute.ATTACK)<=7);
        var y=new org.bukkit.configuration.file.YamlConfiguration();y.set("item",item);var copy=org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(new java.io.StringReader(y.saveToString())).getItemStack("item");assertEquals(before,plugin.gear().rolled(copy,r.id()));
        plugin.gearSettings().put(r.field("max.attack",9));player.getInventory().setItemInMainHand(copy);assertEquals(2+before.get(StatAttribute.ATTACK),plugin.gear().bonus(player).attack());
        var loaded=new GearSettings(new java.io.File(plugin.getDataFolder(),"gear-settings.yml").toPath());assertEquals(9,loaded.get(r.id()).max().get(StatAttribute.ATTACK));assertEquals(Material.IRON_SWORD,loaded.get(r.id()).template().getType());
        assertNotEquals(item,plugin.gear().generate(r,new Random(14)),"Each generated object has its own identity");
    }
    @Test void badRangesAndNonFiniteNumbersAreRejected()throws Exception{
        var r=register(Material.SHIELD,EquipmentSlot.OFF_HAND);assertThrows(IllegalArgumentException.class,()->r.field("min.attack",2));assertThrows(IllegalArgumentException.class,()->r.field("bonus.attack",Double.NaN));assertThrows(IllegalArgumentException.class,()->r.field("shield",101));assertThrows(IllegalArgumentException.class,()->r.field("level",1.2));
    }
    @Test void equippedGearAffectsSkillPowerAndNormalAttackAttribute()throws Exception{
        var r=register(Material.STICK,EquipmentSlot.HAND).field("bonus.attack",4).field("bonus.intelligence",6);plugin.gearSettings().put(r);double before=plugin.stats(player).attack();
        player.getInventory().setItemInMainHand(r.template());plugin.gear().refresh(player);assertEquals(before+4,plugin.stats(player).attack());
        var skill=plugin.catalog().get("mago").skills().getFirst();double base=plugin.catalog().mechanics(skill).power(plugin.stats(player),skill);player.getInventory().setItemInMainHand(null);plugin.gear().refresh(player);assertTrue(plugin.catalog().mechanics(skill).power(plugin.stats(player),skill)<base);
    }
    @Test void shieldProtectionUsesNativeComponentAndRestoresOriginalOnRuleRemoval()throws Exception{
        var r=register(Material.SHIELD,EquipmentSlot.OFF_HAND).field("shield",40);plugin.gearSettings().put(r);player.getInventory().setItemInOffHand(r.template());plugin.gear().refresh(player);
        var captured=((DisciplineIntegrationTest.TestClasses)plugin).nativeShieldPercent;assertEquals(40,captured.get(Material.SHIELD));
        plugin.gearSettings().put(r.field("level",20));plugin.gear().refresh(player);assertEquals(0,captured.get(Material.SHIELD));
        plugin.gearSettings().remove(r.id());plugin.gear().refresh(player);assertEquals(-1,captured.get(Material.SHIELD));
    }
    @Test void itemsAdderIdentitySurvivesGenerationAndDoesNotAffectVanillaBaseItem()throws Exception{
        MockBukkit.createMockPlugin("ItemsAdder");var custom=new ItemStack(Material.IRON_SWORD);var meta=custom.getItemMeta();meta.setDisplayName("Lama del regno");meta.getPersistentDataContainer().set(new NamespacedKey("itemsadder","test_id"),org.bukkit.persistence.PersistentDataType.STRING,"regno:lama");custom.setItemMeta(meta);
        plugin.gear().register(custom);var r=plugin.gearSettings().get("itemsadder:regno:lama").field("bonus.attack",4);plugin.gearSettings().put(r);
        var generated=plugin.gear().generate(r,new Random(1));assertEquals("itemsadder:regno:lama",SkillEquipment.identify(generated));assertEquals("Lama del regno · Comune",org.bukkit.ChatColor.stripColor(generated.getItemMeta().getDisplayName()));
        player.getInventory().setItemInMainHand(generated);assertEquals(4,plugin.gear().bonus(player).attack());player.getInventory().setItemInMainHand(new ItemStack(Material.IRON_SWORD));assertEquals(0,plugin.gear().bonus(player).attack());
    }
    void click(int slot){var view=player.getOpenInventory();server.getPluginManager().callEvent(new InventoryClickEvent(view,InventoryType.SlotType.CONTAINER,slot,ClickType.LEFT,InventoryAction.PICKUP_ALL));server.getScheduler().performTicks(1);}
    @Test void adminGuiCopiesTemplateEditsNumbersAndGeneratesOneItem()throws Exception{
        player.addAttachment(plugin,"castigo.classes.admin",true);player.getInventory().setItem(9,new ItemStack(Material.IRON_SWORD));plugin.openGear(player);click(54);
        assertEquals(Material.IRON_SWORD,player.getInventory().getItem(9).getType());assertNotNull(plugin.gearSettings().get("minecraft:iron_sword"),player.nextMessage());assertNotNull(player.getOpenInventory().getTopInventory().getItem(14));click(9+5);
        var chat=new org.bukkit.event.player.AsyncPlayerChatEvent(false,player,"3",Set.of(player));server.getPluginManager().callEvent(chat);assertTrue(chat.isCancelled());server.getScheduler().performTicks(1);
        assertEquals(3,plugin.gearSettings().get("minecraft:iron_sword").bonus().get(StatAttribute.ATTACK));click(50);
        long count=Arrays.stream(player.getInventory().getStorageContents()).filter(Objects::nonNull).filter(i->i.getType()==Material.IRON_SWORD).count();assertEquals(2,count);
    }
}
