package it.castigo.classes;
import org.bukkit.*;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.*;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import static org.junit.jupiter.api.Assertions.*;
class ProgressionIntegrationTest {
    ServerMock server;CastigoClasses plugin;PlayerMock player;
    @BeforeEach void setup(){server=MockBukkit.mock();server.addSimpleWorld("world");MockBukkit.createMockPlugin("CastigoCore");plugin=MockBukkit.loadWith(DisciplineIntegrationTest.TestClasses.class,getClass().getResourceAsStream("/plugin.yml"));player=server.addPlayer("Hero");player.addAttachment(plugin,"castigo.classes.use",true);assertTrue(plugin.isEnabled());}
    @AfterEach void cleanup(){MockBukkit.unmock();}
    @Test void defaultClassesHaveACompleteInheritedProgression(){
        for(String id:java.util.List.of("mago","mago_bianco","mago_nero","guerriero_scudo","guerriero_due_mani","arciere")){
            assertEquals(10,plugin.classCap(id));assertEquals(25,plugin.classCap(id+"_esperto"));assertEquals(50,plugin.classCap(id+"_maestro"));
            assertEquals(plugin.catalog().get(id).skills(),plugin.catalog().get(id+"_maestro").skills());
        }
    }
    @Test void dailyCapAndClassCapAreAppliedToRealGrants()throws Exception{
        plugin.progressionSettings().set(null,"cap","150");plugin.grantXp(player,999);var p=plugin.profile(player);
        assertEquals(150,p.dailyXp);assertEquals(2,p.level);assertEquals(50,p.xp);
        plugin.grantXp(player,999);assertEquals(50,p.xp);plugin.resetDaily(player);assertEquals(0,p.dailyXp);assertEquals(50,p.xp);
        plugin.progressionSettings().set(null,"cap","0");plugin.grantXp(player,100000000);assertEquals(10,p.level);assertEquals(0,p.xp);
        long spent=p.dailyXp;plugin.grantXp(player,1000);assertEquals(spent,p.dailyXp);
    }
    @Test void promotionRequiresCapAndExactItemConsumesOneAndKeepsPoints()throws Exception{
        String next="mago_bianco_esperto";plugin.progressionSettings().set(next,"promotion-item","minecraft:diamond");
        player.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND,3));
        assertThrows(IllegalArgumentException.class,()->plugin.promote(player,next));assertEquals(3,player.getInventory().getItemInMainHand().getAmount());
        plugin.grantXp(player,90000);var p=plugin.profile(player);int points=p.earnedStatPoints;long daily=p.dailyXp;
        player.getInventory().setItemInMainHand(new ItemStack(Material.EMERALD,3));assertThrows(IllegalArgumentException.class,()->plugin.promote(player,next));
        player.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND,3));plugin.promote(player,next);
        assertEquals(next,p.classId);assertEquals(10,p.level);assertEquals(points,p.earnedStatPoints);assertEquals(daily,p.dailyXp);assertEquals(2,player.getInventory().getItemInMainHand().getAmount());
        assertThrows(IllegalArgumentException.class,()->plugin.promote(player,next));assertEquals(2,player.getInventory().getItemInMainHand().getAmount());
        plugin.grantXp(player,1000);assertTrue(p.xp>0||p.level>10);
    }
    @Test void twoHandedGripRequiresConfiguredSwordCorrectFamilyAndFreeOffhand()throws Exception{
        plugin.progressionSettings().set("guerriero_due_mani","two-handed-items","minecraft:iron_sword");
        player.getInventory().setItemInMainHand(new ItemStack(Material.IRON_SWORD));assertFalse(plugin.twoHanded(player));
        plugin.onCommand(server.getConsoleSender(),plugin.getCommand("classe"),"classe",new String[]{"set",player.getName(),"guerriero_due_mani_maestro"});assertTrue(plugin.twoHanded(player));
        player.getInventory().setItemInOffHand(new ItemStack(Material.SHIELD));assertFalse(plugin.twoHanded(player));
        player.getInventory().setItemInOffHand(null);player.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND_SWORD));assertFalse(plugin.twoHanded(player));
    }
    @Test void staffLevelCommandCannotBypassClassCap(){
        plugin.onCommand(server.getConsoleSender(),plugin.getCommand("classe"),"classe",new String[]{"livello",player.getName(),"50"});assertEquals(1,plugin.profile(player).level);
    }
    private void click(int slot){var e=new org.bukkit.event.inventory.InventoryClickEvent(player.getOpenInventory(),org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER,slot,org.bukkit.event.inventory.ClickType.LEFT,org.bukkit.event.inventory.InventoryAction.PICKUP_ALL);server.getPluginManager().callEvent(e);assertTrue(e.isCancelled());server.getScheduler().performTicks(1);}
    private void chat(String value){var e=new org.bukkit.event.player.AsyncPlayerChatEvent(false,player,value,new java.util.HashSet<>(server.getOnlinePlayers()));server.getPluginManager().callEvent(e);assertTrue(e.isCancelled());server.getScheduler().performTicks(1);}
    @Test void adminGuiEditsDailySettingsAndResetIsExplicit()throws Exception{
        player.addAttachment(plugin,"castigo.classes.admin",true);plugin.openProgression(player);click(38);chat("250");assertEquals(250,plugin.progressionSettings().dailyCap());
        click(39);chat("04:30");assertEquals("04:30",plugin.progressionSettings().resetTime().toString());
        plugin.grantXp(player,200);click(40);assertEquals(200,plugin.profile(player).dailyXp);click(22);server.getScheduler().performTicks(5);assertEquals(0,plugin.profile(player).dailyXp);
        var saved=new ProgressionSettings(new java.io.File(plugin.getDataFolder(),"progression-settings.yml").toPath());assertEquals(250,saved.dailyCap());assertFalse(saved.token().isBlank());
    }
    @Test void adminGuiCopiesPromotionItemWithoutTakingIt(){
        player.addAttachment(plugin,"castigo.classes.admin",true);plugin.openProgression(player);int index=-1;
        for(int i=0;i<36;i++){var item=player.getOpenInventory().getTopInventory().getItem(i);if(item!=null&&item.getItemMeta().getLore().contains("mago_bianco_esperto"))index=i;}
        assertTrue(index>=0);click(index);click(14);player.getInventory().setItem(9,new ItemStack(Material.DIAMOND,3));click(54);
        assertEquals("minecraft:diamond",plugin.progressionSettings().item("mago_bianco_esperto"));assertEquals(3,player.getInventory().getItem(9).getAmount());
    }
    @Test void rightClickConsumesPromotionTokenThroughTheListener()throws Exception{
        plugin.progressionSettings().set("mago_bianco_esperto","promotion-item","minecraft:diamond");plugin.grantXp(player,90000);player.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND,2));
        var e=new org.bukkit.event.player.PlayerInteractEvent(player,org.bukkit.event.block.Action.RIGHT_CLICK_AIR,player.getInventory().getItemInMainHand(),null,org.bukkit.block.BlockFace.SELF,org.bukkit.inventory.EquipmentSlot.HAND);
        server.getPluginManager().callEvent(e);assertTrue(e.isCancelled());assertEquals("mago_bianco_esperto",plugin.profile(player).classId);assertEquals(1,player.getInventory().getItemInMainHand().getAmount());
    }
    @Test void mobDeathSharesXpAndSuppressesOrbs(){
        var second=server.addPlayer("Helper");var mob=(org.bukkit.entity.LivingEntity)player.getWorld().spawnEntity(player.getLocation(),org.bukkit.entity.EntityType.ZOMBIE);
        var a=new org.bukkit.event.entity.EntityDamageByEntityEvent(player,mob,org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_ATTACK,15);
        var b=new org.bukkit.event.entity.EntityDamageByEntityEvent(second,mob,org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_ATTACK,5);
        server.getPluginManager().callEvent(a);server.getPluginManager().callEvent(b);
        var death=new org.bukkit.event.entity.EntityDeathEvent(mob,a.getDamageSource(),new java.util.ArrayList<>(),100);server.getPluginManager().callEvent(death);
        assertEquals(0,death.getDroppedExp());assertEquals(75,plugin.profile(player).dailyXp);assertEquals(25,plugin.profile(second).dailyXp);
    }
    @Test void subclassesKeepParentEquipmentOverrides()throws Exception{
        var skill=plugin.catalog().get("mago_bianco").skills().getFirst();plugin.equipment().setItem("mago_bianco",skill.id(),new SkillEquipment.Requirement(false,"minecraft:blaze_rod"));
        assertEquals("minecraft:blaze_rod",plugin.equipment().requirement("mago_bianco_maestro",skill,plugin.catalog().mechanics(skill)).item());
    }
}
