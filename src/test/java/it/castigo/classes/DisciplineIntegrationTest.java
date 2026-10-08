package it.castigo.classes;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.*;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import static org.junit.jupiter.api.Assertions.*;

public class DisciplineIntegrationTest {
    // MockBukkit cannot instantiate the supplied final core plugin. Only its scheduling
    // bridge is substituted; ParticleStyle and atomic profile writes use the real core JAR.
    public static class TestClasses extends CastigoClasses {
        final java.util.List<com.google.gson.JsonObject> effects=new java.util.ArrayList<>();
        @Override public void broadcastEffect(Location from,Location at,com.google.gson.JsonObject effect) { effects.add(effect.deepCopy()); }
        @Override protected void scheduleTick(Runnable task) { getServer().getScheduler().runTaskTimer(this,task,5,5); }
        @Override protected void cancelTicks() { getServer().getScheduler().cancelTasks(this); }
    }
    ServerMock server;CastigoClasses plugin;PlayerMock player;
    @BeforeEach void setup() throws Exception {
        server=MockBukkit.mock();server.addSimpleWorld("world");
        MockBukkit.createMockPlugin("CastigoCore");
        plugin=MockBukkit.loadWith(TestClasses.class,getClass().getResourceAsStream("/plugin.yml"));
        assertTrue(plugin.isEnabled());player=server.addPlayer("Tester");
        player.addAttachment(plugin,"castigo.classes.use",true);
        player.getInventory().setItemInMainHand(new ItemStack(Material.STICK));
        assertNotNull(plugin.profile(player));
    }
    @AfterEach void cleanup() { MockBukkit.unmock(); }
    void command(String... args) { plugin.onCommand(server.getConsoleSender(),plugin.getCommand("classe"),"classe",args); }
    void use(String classId,int slot) {
        command("set",player.getName(),classId);command("livello",player.getName(),"50");
        assertEquals(classId,plugin.profile(player).classId);
        assertEquals(50,plugin.profile(player).level);
        assertTrue(player.hasPermission("castigo.classes.use"),"default player permission");
        plugin.profile(player).resource=1000;plugin.profile(player).globalReadyAt=0;
        plugin.onCommand(player,plugin.getCommand("classe"),"classe",new String[]{"skill",Integer.toString(slot)});
    }
    @Test void startsWithAllFiveDisciplinesAndKeepsEightSkills() {
        for(String id:new String[]{"mago_bianco","mago_nero","guerriero_scudo","guerriero_due_mani","arciere"})assertEquals(8,plugin.catalog().get(id).skills().size());
        assertEquals("mago_bianco",plugin.profile(player).classId);
    }
    @Test void selfHealChangesHealthAndSpendsResource() {
        player.setSneaking(true);player.setHealth(5);use("mago_bianco",1);
        assertTrue(player.getHealth()>5);assertTrue(plugin.profile(player).resource<1000);
    }
    @Test void wrongEquipmentDoesNotSpendResourceOrStartCooldown() {
        player.getInventory().clear();use("guerriero_scudo",3);
        assertEquals(1000,plugin.profile(player).resource);assertTrue(plugin.profile(player).cooldowns.isEmpty());
    }
    @Test void guardSlowsAndClassChangeRemovesModifier() {
        player.getInventory().setItemInMainHand(new ItemStack(Material.IRON_SWORD));player.getInventory().setItemInOffHand(new ItemStack(Material.SHIELD));
        double before=player.getAttribute(Attribute.MOVEMENT_SPEED).getValue();use("guerriero_scudo",3);
        assertTrue(player.getAttribute(Attribute.MOVEMENT_SPEED).getValue()<before);
        command("set",player.getName(),"mago_bianco");assertEquals(before,player.getAttribute(Attribute.MOVEMENT_SPEED).getValue(),1e-9);
    }
    @Test void preparedHealWaitsBeforeChangingHealth() {
        player.setSneaking(true);player.setHealth(5);use("mago_bianco",7);
        double before=player.getHealth();
        server.getScheduler().performTicks(29);assertEquals(before,player.getHealth());
        server.getScheduler().performTicks(1);assertTrue(player.getHealth()>before);
    }
    @Test void movingInterruptsPreparedHealWithoutRefundingCooldown() {
        player.setSneaking(true);player.setHealth(5);use("mago_bianco",7);
        double before=player.getHealth();
        player.teleport(player.getLocation().add(1,0,0));
        server.getScheduler().performTicks(31);
        assertEquals(before,player.getHealth());assertFalse(plugin.profile(player).cooldowns.isEmpty());
    }
    @Test void regenerationHealsOverTimeAndStopsAfterClassChange() {
        player.setSneaking(true);player.setHealth(5);use("mago_bianco",3);
        double before=player.getHealth();server.getScheduler().performTicks(20);
        assertTrue(player.getHealth()>before);
        command("set",player.getName(),"arciere");double after=player.getHealth();
        server.getScheduler().performTicks(40);assertEquals(after,player.getHealth());
    }
    @Test void guardExpiresAndRestoresMovement() {
        player.getInventory().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
        player.getInventory().setItemInOffHand(new ItemStack(Material.SHIELD));
        double before=player.getAttribute(Attribute.MOVEMENT_SPEED).getValue();use("guerriero_scudo",3);
        assertTrue(player.getAttribute(Attribute.MOVEMENT_SPEED).getValue()<before);
        server.getScheduler().performTicks(201);
        assertEquals(before,player.getAttribute(Attribute.MOVEMENT_SPEED).getValue(),1e-9);
    }
    @Test void archerCannotStartWithoutEnoughArrows() {
        player.getInventory().setItemInMainHand(new ItemStack(Material.BOW));
        player.getInventory().setItem(1,new ItemStack(Material.ARROW));use("arciere",7);
        assertEquals(1000,plugin.profile(player).resource);
        assertTrue(plugin.profile(player).cooldowns.isEmpty());assertEquals(1,player.getInventory().getItem(1).getAmount());
    }
    @Test void configuredRanksAndIdentifiersMatchTheFortySkills() {
        var ids=new java.util.HashSet<String>();
        for(String id:new String[]{"mago_bianco","mago_nero","guerriero_scudo","guerriero_due_mani","arciere"}) {
            var skills=plugin.catalog().get(id).skills();
            int[] levels={5,5,15,15,30,30,50,50};
            for(int i=0;i<8;i++) { assertEquals(levels[i],skills.get(i).unlockLevel());assertTrue(ids.add(skills.get(i).id()));assertTrue(skills.get(i).id().length()<=40); }
            String[] ranks={"Aspirante","Apprendista","Praticante","Esperto","Maestro"};int[] thresholds={1,5,15,30,50};
            for(int i=0;i<5;i++)assertEquals(ranks[i],it.castigo.classes.model.DisciplineRules.rank(thresholds[i],skills));
        }
        assertEquals(40,ids.size());
    }
    @Test void healWorksWithoutAnyTeamAndPvpEligibilityIgnoresTeams() {
        var friend=server.addPlayer("Friend");friend.teleport(player.getLocation());friend.setHealth(5);
        var engine=new SkillEngine(plugin);var disciplines=new DisciplineEngine(plugin,engine);
        assertTrue(disciplines.heal(player,friend,3));assertEquals(8,friend.getHealth());
        var team=player.getScoreboard().registerNewTeam("test");team.addEntry(player.getName());team.addEntry(friend.getName());team.setAllowFriendlyFire(false);
        player.getWorld().setPVP(true);assertTrue(engine.targetAllowed(player,friend));
        player.getWorld().setPVP(false);assertFalse(engine.targetAllowed(player,friend));
    }
    @Test void selectedHandAndSpecificMaterialAreEnforcedAndPersisted() throws Exception {
        var skill=plugin.catalog().get("mago_bianco").skills().getFirst();
        plugin.equipment().setItem("mago_bianco",skill.id(),new SkillEquipment.Requirement(true,"minecraft:blaze_rod"));
        player.getInventory().setItemInMainHand(new ItemStack(Material.BLAZE_ROD));assertFalse(plugin.equipped(player,skill));
        player.getInventory().setItemInOffHand(new ItemStack(Material.BLAZE_ROD));assertTrue(plugin.equipped(player,skill));
        var restored=new SkillEquipment(new java.io.File(plugin.getDataFolder(),"skill-equipment.yml"));
        assertEquals(new SkillEquipment.Requirement(true,"minecraft:blaze_rod"),restored.requirement("mago_bianco",skill,plugin.catalog().mechanics(skill)));
        restored.setItem("mago_bianco",skill.id(),new SkillEquipment.Requirement(false,"itemsadder:castigo:staff"));
        assertFalse(restored.matches(player,"mago_bianco",skill,plugin.catalog().mechanics(skill)),"Vanilla cannot substitute an unavailable ItemsAdder item");
    }
    @Test void emptyHandCannotCastEvenOldMagicSkills() {
        player.getInventory().clear();player.setHealth(5);use("mago",6);
        assertEquals(5,player.getHealth());assertEquals(1000,plugin.profile(player).resource);
        assertTrue(plugin.profile(player).cooldowns.isEmpty());
    }
    @Test void iconOverridePersistsAndRejectsInvalidPaths() throws Exception {
        var skill=plugin.catalog().get("mago_bianco").skills().getFirst();
        String icon="texture:castigo:textures/gui/skills/orison.png";
        plugin.equipment().setIcon("mago_bianco",skill.id(),icon);
        var restored=new SkillEquipment(new java.io.File(plugin.getDataFolder(),"skill-equipment.yml"));
        assertEquals(icon,restored.icon("mago_bianco",skill));
        assertThrows(IllegalArgumentException.class,()->restored.setIcon("mago_bianco",skill.id(),"texture:castigo:../../outside.png"));
        restored.setIcon("mago_bianco",skill.id(),"reset");assertEquals(skill.icon(),restored.icon("mago_bianco",skill));
    }
    private void clickMenu(int rawSlot) {
        var event=new org.bukkit.event.inventory.InventoryClickEvent(player.getOpenInventory(),org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER,rawSlot,
                org.bukkit.event.inventory.ClickType.LEFT,org.bukkit.event.inventory.InventoryAction.PICKUP_ALL);
        server.getPluginManager().callEvent(event);assertTrue(event.isCancelled());server.getScheduler().performTicks(1);
    }
    @Test void adminMenuAssignsClickedItemWithoutRemovingIt() {
        player.addAttachment(plugin,"castigo.classes.admin",true);
        plugin.onCommand(player,plugin.getCommand("classe"),"classe",new String[]{"admin"});
        var inventory=player.getOpenInventory().getTopInventory();int selected=-1;
        for(int i=0;i<45;i++)if(inventory.getItem(i)!=null&&inventory.getItem(i).getItemMeta().getLore().contains("mago_bianco"))selected=i;
        assertTrue(selected>=0);clickMenu(selected);clickMenu(0);
        player.getInventory().setItem(9,new ItemStack(Material.BLAZE_ROD,3));clickMenu(54);
        var skill=plugin.catalog().get("mago_bianco").skills().getFirst();
        assertEquals("minecraft:blaze_rod",plugin.equipment().requirement("mago_bianco",skill,plugin.catalog().mechanics(skill)).item());
        assertEquals(3,player.getInventory().getItem(9).getAmount());
    }
    @Test void normalPlayerCannotOpenAdminGui() {
        plugin.onCommand(player,plugin.getCommand("classe"),"classe",new String[]{"admin"});
        assertNull(player.getOpenInventory().getTopInventory());
    }
    @Test void orisonSelfCastEmitsBeamAtRecipientsFeet() {
        player.getWorld().loadChunk(player.getLocation().getChunk());
        player.setSneaking(true);player.setHealth(5);use("mago_bianco",1);
        var effects=((TestClasses)plugin).effects;assertEquals(1,effects.size());var effect=effects.getFirst();
        assertEquals("HEALING_BEAM",effect.get("shape").getAsString());
        assertEquals(2,effect.getAsJsonObject("mesh").get("rings").getAsInt());
        assertEquals("castigoclasses:textures/vfx/healing_column.png",effect.getAsJsonObject("mesh").get("columnTexture").getAsString());
        assertEquals(player.getUniqueId().toString(),effect.get("target").getAsString());
        assertEquals(player.getLocation().getY(),effect.getAsJsonArray("at").get(1).getAsDouble());
        assertEquals("castigoclasses:skill.orison",effect.getAsJsonObject("sound").get("id").getAsString());
    }
    @Test void targetedVisualUsesRecipientInsteadOfCaster() {
        var recipient=server.addPlayer("Recipient");recipient.teleport(player.getLocation().add(4,0,0));
        recipient.getWorld().loadChunk(recipient.getLocation().getChunk());
        var skill=plugin.catalog().get("mago_bianco").skills().getFirst();
        new SkillEngine(plugin).visual(player,skill,recipient);
        var effect=((TestClasses)plugin).effects.getLast();
        assertEquals(recipient.getUniqueId().toString(),effect.get("target").getAsString());
        assertEquals(recipient.getLocation().getX(),effect.getAsJsonArray("at").get(0).getAsDouble());
        assertEquals(effect.getAsJsonArray("at"),effect.getAsJsonArray("from"));
    }
    @Test void unsuccessfulOrisonDoesNotEmitGraphicsOrSound() {
        command("set",player.getName(),"mago_bianco");command("livello",player.getName(),"50");
        player.setSneaking(true);player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
        plugin.profile(player).resource=1000;
        plugin.onCommand(player,plugin.getCommand("classe"),"classe",new String[]{"skill","1"});
        assertTrue(((TestClasses)plugin).effects.isEmpty());
    }
    @Test void castingStateTracksPreparationAndClearsOnMovementAndCompletion() {
        player.setSneaking(true);player.setHealth(5);use("mago_bianco",7);
        assertEquals(1500,plugin.casting(player).get("totalMs").getAsInt());
        server.getScheduler().performTicks(10);assertEquals(1000,plugin.casting(player).get("remainingMs").getAsInt());
        player.teleport(player.getLocation().add(1,0,0));server.getScheduler().performTicks(1);assertTrue(plugin.casting(player).isEmpty());
        plugin.profile(player).cooldowns.clear();use("mago_bianco",7);
        server.getScheduler().performTicks(30);assertTrue(plugin.casting(player).isEmpty());
    }
    @Test void clientCannotPublishVfxWithoutAdminPermission() throws Exception {
        var field=CastigoClasses.class.getDeclaredField("connected");field.setAccessible(true);
        ((java.util.Set<java.util.UUID>)field.get(plugin)).add(player.getUniqueId());
        var skill=plugin.catalog().get("mago_bianco").skills().getFirst();
        var draft=VfxSettings.draft(plugin.presentation(player,skill).cues().get(SkillPresentation.Stage.CAST));draft.addProperty("shape","RING");draft.addProperty("particlesEnabled",true);
        var o=new com.google.gson.JsonObject();o.addProperty("v",1);o.addProperty("type","vfx_save");o.addProperty("classId","mago_bianco");o.addProperty("skillId",skill.id());o.addProperty("stage","CAST");o.add("draft",draft);
        plugin.onPluginMessageReceived(CastigoClasses.CHANNEL,player,o.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertFalse(new java.io.File(plugin.getDataFolder(),"vfx-overrides.json").exists());
        player.addAttachment(plugin,"castigo.classes.admin",true);
        var rate=CastigoClasses.class.getDeclaredField("lastRequest");rate.setAccessible(true);((java.util.Map<?,?>)rate.get(plugin)).clear();
        plugin.onPluginMessageReceived(CastigoClasses.CHANNEL,player,o.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertTrue(new java.io.File(plugin.getDataFolder(),"vfx-overrides.json").exists());
        assertEquals(SkillPresentation.Shape.RING,plugin.presentation(player,skill).cues().get(SkillPresentation.Stage.CAST).shape());
    }
}
