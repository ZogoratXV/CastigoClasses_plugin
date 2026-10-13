package it.castigo.classes;
import org.bukkit.*;
import org.bukkit.attribute.*;
import org.bukkit.entity.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.*;
import org.mockbukkit.mockbukkit.entity.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CodexIntegrationTest {
    ServerMock server;CastigoClasses plugin;PlayerMock player;
    @BeforeEach void setup(){server=MockBukkit.mock();server.addSimpleWorld("world");MockBukkit.createMockPlugin("CastigoCore");plugin=MockBukkit.loadWith(DisciplineIntegrationTest.TestClasses.class,getClass().getResourceAsStream("/plugin.yml"));player=server.addPlayer();player.registerAttribute(Attribute.ARMOR);}
    @AfterEach void close(){MockBukkit.unmock();}
    CodexSettings.Entry weapon(Material material,String category)throws Exception{var e=new CodexSettings.Entry(CodexSettings.Kind.WEAPON,material.getKey().toString(),material.name(),category,new ItemStack(material));plugin.codexSettings().put(e);return e;}
    void staff(){player.addAttachment(plugin,"castigo.classes.admin",true);}
    void click(int slot){server.getPluginManager().callEvent(new InventoryClickEvent(player.getOpenInventory(),InventoryType.SlotType.CONTAINER,slot,ClickType.LEFT,InventoryAction.PICKUP_ALL));server.getScheduler().performTicks(1);}
    void chat(String text){var e=new AsyncPlayerChatEvent(false,player,text,Set.of(player));server.getPluginManager().callEvent(e);assertTrue(e.isCancelled());server.getScheduler().performTicks(1);}
    @Test void toggleRequiresAdminPersistsAndPreservesCollectionAndClaims()throws Exception{
        var sword=weapon(Material.IRON_SWORD,"spade");player.getInventory().setItemInMainHand(sword.icon());plugin.codex().scan(player);
        var reward=new CodexSettings.Reward(UUID.randomUUID().toString(),"spade",1,new ItemStack(Material.EMERALD),"");plugin.codexSettings().put(reward);plugin.codex().claim(player,reward.id());
        server.dispatchCommand(player,"classe codex off");assertTrue(plugin.codexSettings().enabled());staff();server.dispatchCommand(player,"classe codex off");assertFalse(plugin.codexSettings().enabled());assertFalse(new CodexSettings(plugin.getDataFolder().toPath().resolve("codex.yml")).enabled());
        var axe=weapon(Material.IRON_AXE,"asce");player.getInventory().setItemInMainHand(axe.icon());plugin.codex().scan(player);assertFalse(plugin.codex().progress(player).found().contains(axe.key()));assertThrows(IllegalArgumentException.class,()->plugin.codex().claim(player,reward.id()));
        server.dispatchCommand(player,"classe codex on");assertTrue(plugin.codex().progress(player).found().contains(sword.key()));assertTrue(plugin.codex().progress(player).claimed().contains(reward.id()));assertThrows(IllegalArgumentException.class,()->plugin.codex().claim(player,reward.id()));
        var stored=new CodexProgress(plugin.getDataFolder().toPath().resolve("codex-players")).get(player.getUniqueId());assertEquals(plugin.codex().progress(player),stored);
    }
    @Test void onlyDeathUnlocksMobAndOnlyForKiller()throws Exception{
        var mob=(LivingEntityMock)player.getWorld().spawnEntity(player.getLocation(),EntityType.ZOMBIE);var entry=plugin.codexSettings().identity("minecraft:zombie");
        var hit=new EntityDamageByEntityEvent(player,mob,EntityDamageEvent.DamageCause.ENTITY_ATTACK,1);server.getPluginManager().callEvent(hit);assertFalse(plugin.codex().progress(player).found().contains(entry.key()));
        mob.setKiller(player);server.getPluginManager().callEvent(new EntityDeathEvent(mob,hit.getDamageSource(),new ArrayList<>(),0));assertTrue(plugin.codex().progress(player).found().contains(entry.key()));assertFalse(plugin.codex().progress(server.addPlayer()).found().contains(entry.key()));
        var boss=new CodexSettings.Entry(CodexSettings.Kind.BOSS,entry.identity(),entry.name(),"boss",entry.icon());assertEquals(entry.key(),boss.key(),"Changing mob/boss classification keeps discovery identity");
    }
    @Test void disabledDeathDoesNotRegister()throws Exception{
        plugin.codexSettings().enabled(false);var mob=(LivingEntityMock)player.getWorld().spawnEntity(player.getLocation(),EntityType.ZOMBIE);mob.setKiller(player);var hit=new EntityDamageByEntityEvent(player,mob,EntityDamageEvent.DamageCause.ENTITY_ATTACK,1);server.getPluginManager().callEvent(new EntityDeathEvent(mob,hit.getDamageSource(),new ArrayList<>(),0));assertTrue(plugin.codex().progress(player).found().isEmpty());
    }
    @Test void slotAcquisitionRegistersBeforePolling()throws Exception{
        var entry=weapon(Material.IRON_SWORD,"spade");player.openInventory(new org.mockbukkit.mockbukkit.inventory.SimpleInventoryViewMock(player,Bukkit.createInventory(null,27),player.getInventory(),InventoryType.CHEST){@Override public int convertSlot(int slot){return slot;}});var event=new io.papermc.paper.event.player.PlayerInventorySlotChangeEvent(player,0,new ItemStack(Material.AIR),entry.icon());plugin.codex().acquired(event);assertTrue(plugin.codex().progress(player).found().contains(entry.key()));
    }
    @Test void rewardFullInventoryDoesNotConsumeClaimAndRepeatedClickCannotDuplicate()throws Exception{
        var entry=weapon(Material.IRON_SWORD,"spade");player.getInventory().setItemInMainHand(entry.icon());plugin.codex().scan(player);var r=new CodexSettings.Reward(UUID.randomUUID().toString(),"spade",1,new ItemStack(Material.DIAMOND,2),"");plugin.codexSettings().put(r);
        for(int i=0;i<36;i++)player.getInventory().setItem(i,new ItemStack(Material.STONE,64));assertThrows(IllegalArgumentException.class,()->plugin.codex().claim(player,r.id()));assertFalse(plugin.codex().progress(player).claimed().contains(r.id()));player.getInventory().setItem(7,null);plugin.codex().claim(player,r.id());assertEquals(2,player.getInventory().getItem(7).getAmount());assertThrows(IllegalArgumentException.class,()->plugin.codex().claim(player,r.id()));
    }
    @Test void skinCopiesOnlyAppearanceAndRejectsWrongCategoryAndLockedSource()throws Exception{
        var iron=weapon(Material.IRON_SWORD,"spade");var gold=weapon(Material.GOLDEN_SWORD,"spade");var staff=weapon(Material.BLAZE_ROD,"staffe");
        var item=iron.icon();var meta=item.getItemMeta();meta.setDisplayName("Arma originale");meta.getPersistentDataContainer().set(new NamespacedKey("test","damage"),PersistentDataType.INTEGER,99);meta.setItemModel(NamespacedKey.fromString("test:original"));item.setItemMeta(meta);player.getInventory().setItemInMainHand(item);
        assertThrows(IllegalArgumentException.class,()->plugin.codex().skin(player,gold.key()));player.getInventory().setItem(9,gold.icon());player.getInventory().setItem(10,staff.icon());plugin.codex().scan(player);assertThrows(IllegalArgumentException.class,()->plugin.codex().skin(player,staff.key()));plugin.codex().skin(player,gold.key());var skinned=player.getInventory().getItemInMainHand();assertEquals(Material.IRON_SWORD,skinned.getType());assertEquals("Arma originale",skinned.getItemMeta().getDisplayName());assertEquals(99,skinned.getItemMeta().getPersistentDataContainer().get(new NamespacedKey("test","damage"),PersistentDataType.INTEGER));assertEquals(NamespacedKey.minecraft("golden_sword"),skinned.getItemMeta().getItemModel());
        plugin.codex().restore(player);assertEquals(item,player.getInventory().getItemInMainHand());
    }
    @Test void tradingSkinnedWeaponDoesNotUnlockSkinForRecipient()throws Exception{
        var iron=weapon(Material.IRON_SWORD,"spade");var gold=weapon(Material.GOLDEN_SWORD,"spade");var skin=WeaponSkins.apply(iron.icon(),gold.icon(),gold.key());player.getInventory().setItemInMainHand(skin);plugin.codex().scan(player);assertTrue(plugin.codex().progress(player).found().contains(iron.key()));assertFalse(plugin.codex().progress(player).found().contains(gold.key()));assertNull(WeaponSkins.source(player.getInventory().getItemInMainHand()));
    }
    @Test void adminGuiCopiesItemsEditsCategoryRewardsAndBookWithoutTakingItems()throws Exception{
        staff();player.getInventory().setItem(9,new ItemStack(Material.IRON_SWORD));plugin.openCodex(player,true);click(54);assertNotNull(plugin.codexSettings().identity("minecraft:iron_sword"));assertEquals(Material.IRON_SWORD,player.getInventory().getItem(9).getType());click(11);chat("spade_due_mani");assertEquals("spade_due_mani",plugin.codexSettings().identity("minecraft:iron_sword").category());
        click(49);click(48);click(50);click(10);chat("spade_due_mani");click(11);chat("1");click(54);var reward=plugin.codexSettings().rewards().getFirst();assertEquals("spade_due_mani",reward.category());assertEquals(1,reward.threshold());assertEquals(Material.IRON_SWORD,reward.item().getType());
        click(49);click(46);click(54);assertEquals("minecraft:iron_sword",plugin.codexSettings().book());assertEquals(Material.IRON_SWORD,player.getInventory().getItem(9).getType());
    }
    @Test void nonAdminCannotOpenAdminGuiAndPlayersCanOpenWithoutClass()throws Exception{plugin.removeClass(player);plugin.openCodex(player,true);assertNotEquals("Castigo · Codex · Admin",player.getOpenInventory().getTitle());server.dispatchCommand(player,"classe codex");assertEquals("Castigo · Codex",player.getOpenInventory().getTitle());}
    @Test void itemsAdderCategoriesAreExplicitAndIdentityIsPreserved()throws Exception{
        MockBukkit.createMockPlugin("ItemsAdder");var item=new ItemStack(Material.IRON_SWORD);var meta=item.getItemMeta();meta.getPersistentDataContainer().set(new NamespacedKey("itemsadder","test_id"),PersistentDataType.STRING,"regno:staff");item.setItemMeta(meta);staff();player.getInventory().setItem(9,item);plugin.openCodex(player,true);click(54);var entry=plugin.codexSettings().identity("itemsadder:regno:staff");assertNotNull(entry);assertEquals("da_configurare",entry.category());assertFalse(CodexService.compatible(entry,entry));assertEquals("itemsadder:regno:staff",SkillEquipment.identify(WeaponSkins.apply(item,new ItemStack(Material.BLAZE_ROD),"test")));
    }
    @Test void fullCollectionShowsRealLockedIconsAndUnlockRestoresColorWithoutChangingTemplate()throws Exception{
        var wood=weapon(Material.WOODEN_SWORD,"spade");var netherite=weapon(Material.NETHERITE_SWORD,"spade");
        var model=netherite.icon();var meta=model.getItemMeta();meta.setItemModel(NamespacedKey.fromString("test:custom_sword"));meta.setEnchantmentGlintOverride(true);model.setItemMeta(meta);netherite=new CodexSettings.Entry(netherite.kind(),netherite.identity(),netherite.name(),netherite.category(),model);plugin.codexSettings().put(netherite);
        player.getInventory().setItemInMainHand(wood.icon());plugin.openCodex(player,false);var menu=player.getOpenInventory().getTopInventory();int woodSlot=plugin.codexSettings().entries().indexOf(wood),netheriteSlot=plugin.codexSettings().entries().indexOf(netherite);
        assertEquals(Material.NETHERITE_SWORD,menu.getItem(netheriteSlot).getType());assertEquals(NamespacedKey.fromString("test:custom_sword"),menu.getItem(netheriteSlot).getItemMeta().getItemModel());assertEquals(NamespacedKey.fromString("castigoclasses:codex_locked"),menu.getItem(netheriteSlot).getItemMeta().getTooltipStyle());assertFalse(menu.getItem(netheriteSlot).getItemMeta().getEnchantmentGlintOverride());assertFalse(menu.getItem(woodSlot).getItemMeta().hasTooltipStyle());
        click(netheriteSlot);assertNull(WeaponSkins.source(player.getInventory().getItemInMainHand()));assertEquals(model,plugin.codexSettings().get(netherite.key()).icon());
        player.getInventory().setItem(9,model);plugin.openCodex(player,false);var unlocked=player.getOpenInventory().getTopInventory().getItem(netheriteSlot);assertFalse(unlocked.getItemMeta().hasTooltipStyle());assertTrue(unlocked.getItemMeta().getEnchantmentGlintOverride());assertEquals(Material.NETHERITE_SWORD,unlocked.getType());
        click(netheriteSlot);assertEquals(netherite.key(),WeaponSkins.source(player.getInventory().getItemInMainHand()));
    }
}
