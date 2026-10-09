package it.castigo.classes;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.*;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import static org.junit.jupiter.api.Assertions.*;

public class DisciplineIntegrationTest {
    // MockBukkit does not implement visibility ray tests; these fixtures use an unobstructed world.
    static class VisiblePlayer extends PlayerMock {
        VisiblePlayer(ServerMock server,String name){super(server,name);}
        @Override public boolean hasLineOfSight(org.bukkit.entity.Entity other){return true;}
    }
    // MockBukkit cannot instantiate the supplied final core plugin. Only its scheduling
    // bridge is substituted; ParticleStyle and atomic profile writes use the real core JAR.
    public static class TestClasses extends CastigoClasses {
        final java.util.List<com.google.gson.JsonObject> effects=new java.util.ArrayList<>();
        final java.util.List<java.util.UUID> stopped=new java.util.ArrayList<>();
        final java.util.List<String> motions=new java.util.ArrayList<>();
        @Override public void weaponMotion(org.bukkit.entity.Player p,it.castigo.classes.model.Skill skill,String phase,int ticks) { motions.add(phase);super.weaponMotion(p,skill,phase,ticks); }
        @Override public void broadcastEffect(Location from,Location at,com.google.gson.JsonObject effect) { effects.add(effect.deepCopy()); }
        @Override public void stopEffect(java.util.UUID world,java.util.UUID handle) { stopped.add(handle); }
        @Override protected void scheduleTick(Runnable task) { getServer().getScheduler().runTaskTimer(this,task,5,5); }
        @Override protected void cancelTicks() { getServer().getScheduler().cancelTasks(this); }
    }
    ServerMock server;CastigoClasses plugin;PlayerMock player;
    @BeforeEach void setup() throws Exception {
        server=MockBukkit.mock();server.addSimpleWorld("world");
        MockBukkit.createMockPlugin("CastigoCore");
        plugin=MockBukkit.loadWith(TestClasses.class,getClass().getResourceAsStream("/plugin.yml"));
        assertTrue(plugin.isEnabled());player=new VisiblePlayer(server,"Tester");server.addPlayer(player);
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
    @Test void weaponPreparationReleasesOnlyAfterSuccessfulCast() {
        player.setSneaking(true);player.setHealth(5);use("mago_bianco",7);var t=(TestClasses)plugin;
        assertTrue(t.motions.contains("prepare"));assertFalse(t.motions.contains("release"));
        server.getScheduler().performTicks(45);assertTrue(t.motions.contains("release"));
    }
    @Test void castTimeAuraIsIvoryTracksTargetAndStopsOnRelease() {
        player.getWorld().loadChunk(player.getLocation().getChunk());player.setSneaking(true);player.setHealth(5);use("mago_bianco",7);
        var test=(TestClasses)plugin;
        var aura=test.effects.stream().filter(e->e.get("shape").getAsString().equals("MESH_COLUMN")).findFirst().orElseThrow();
        assertEquals("FFFFD8",aura.getAsJsonObject("mesh").get("tint").getAsString());
        assertEquals(player.getUniqueId().toString(),aura.get("target").getAsString());
        assertEquals(player.getEntityId(),aura.get("targetEntity").getAsInt());
        server.getScheduler().performTicks(30);
        assertTrue(test.stopped.contains(java.util.UUID.fromString(aura.get("handle").getAsString())));
    }
    @Test void interruptedCastRemovesTargetAuraAndInstantSkillsDoNotCreateIt() {
        player.getWorld().loadChunk(player.getLocation().getChunk());player.setSneaking(true);player.setHealth(5);use("mago_bianco",7);
        var test=(TestClasses)plugin;var aura=test.effects.stream().filter(e->e.get("shape").getAsString().equals("MESH_COLUMN")).findFirst().orElseThrow();
        player.teleport(player.getLocation().add(1,0,0));server.getScheduler().performTicks(1);
        assertTrue(test.stopped.contains(java.util.UUID.fromString(aura.get("handle").getAsString())));
        test.effects.clear();use("mago_bianco",1);
        assertTrue(test.effects.stream().noneMatch(e->e.get("shape").getAsString().equals("MESH_COLUMN")));
    }
    @Test void otherDisciplineAurasUseRequestedColorsAndRecipient() {
        var target=server.addPlayer("Marked");target.teleport(player.getLocation().add(2,0,0));target.getWorld().loadChunk(target.getLocation().getChunk());
        var engine=new SkillEngine(plugin);
        for(String id:java.util.List.of("mago_nero","arciere")) {
            command("set",player.getName(),id);var skill=plugin.catalog().get(id).skills().getFirst();
            engine.targetAura(player,skill,target,()->true);
            var packet=((TestClasses)plugin).effects.getLast();
            assertEquals(id.equals("mago_nero")?"59209B":"43D66D",packet.getAsJsonObject("mesh").get("tint").getAsString());
            assertEquals(target.getUniqueId().toString(),packet.get("target").getAsString());
        }
    }
    @Test void preparedHealKeepsOriginalRecipientWhenCasterTurnsAway() {
        player.setRotation(0,0);player.setSneaking(false);
        var target=server.addPlayer("FirstTarget");target.teleport(player.getLocation().add(0,0,4));target.setHealth(5);
        target.getWorld().loadChunk(target.getLocation().getChunk());
        // MockBukkit does not implement world ray tracing. Substitute only the initial selection.
        var aiming=new java.util.concurrent.atomic.AtomicReference<org.bukkit.entity.LivingEntity>(target);
        var engine=new SkillEngine(plugin);var disciplines=new DisciplineEngine(plugin,engine,(p,r)->aiming.get());
        assertTrue(disciplines.cast(player,plugin.catalog().get("mago_bianco").skills().get(6),3));
        var test=(TestClasses)plugin;
        var aura=test.effects.stream().filter(e->e.get("shape").getAsString().equals("MESH_COLUMN")).findFirst().orElseThrow();
        assertEquals(target.getUniqueId().toString(),aura.get("target").getAsString());
        player.setRotation(180,0);aiming.set(player);
        for(int i=0;i<30;i++){disciplines.tick();server.getScheduler().performTicks(1);}
        assertTrue(target.getHealth()>5);assertTrue(disciplines.casting(player.getUniqueId()).isEmpty());
    }
    @Test void selectedTargetLeavingRangeCancelsPreparationAndAura() {
        player.setRotation(0,0);var target=server.addPlayer("MovingTarget");
        target.teleport(player.getLocation().add(0,0,4));target.setHealth(5);target.getWorld().loadChunk(target.getLocation().getChunk());
        var engine=new SkillEngine(plugin);var disciplines=new DisciplineEngine(plugin,engine,(p,r)->target);
        assertTrue(disciplines.cast(player,plugin.catalog().get("mago_bianco").skills().get(6),3));var test=(TestClasses)plugin;
        var aura=test.effects.stream().filter(e->e.get("shape").getAsString().equals("MESH_COLUMN")).findFirst().orElseThrow();
        target.teleport(target.getLocation().add(100,0,0));
        for(int i=0;i<30;i++){disciplines.tick();server.getScheduler().performTicks(1);}
        assertEquals(5,target.getHealth());assertTrue(disciplines.casting(player.getUniqueId()).isEmpty());
        assertTrue(test.stopped.contains(java.util.UUID.fromString(aura.get("handle").getAsString())));
    }
    @Test void failedEquipmentNeverAnimatesAWeapon() {
        player.getInventory().clear();use("guerriero_scudo",3);var t=(TestClasses)plugin;
        assertFalse(t.motions.contains("release"));assertFalse(t.motions.contains("prepare"));
    }
    @Test void classChangeCancelsTheWeaponPreparation() {
        player.setSneaking(true);player.setHealth(5);use("mago_bianco",7);var t=(TestClasses)plugin;t.motions.clear();
        command("set",player.getName(),"mago_nero");server.getScheduler().performTicks(45);
        assertTrue(t.motions.contains("stop"));assertFalse(t.motions.contains("release"));
    }
    @Test void selfHealChangesHealthAndSpendsResource() {
        player.setSneaking(true);player.setHealth(5);use("mago_bianco",1);
        assertTrue(player.getHealth()>5);assertTrue(plugin.profile(player).resource<1000);
    }
    @Test void legacyHealNowHonorsConfiguredCastingTimeThroughNormalCommand() throws Exception {
        var skill=plugin.catalog().get("mago").skills().get(5);
        assertEquals(it.castigo.classes.model.Skill.Effect.HEAL,skill.effect());
        var file=new java.io.File(plugin.getDataFolder(),"classes/mago.yml");
        var config=org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        config.set("skills."+skill.id()+".preparation-seconds",2);config.save(file);command("reload");
        player.setHealth(5);use("mago",6);assertEquals(5,player.getHealth());assertEquals(2000,plugin.casting(player).get("totalMs").getAsInt());
        server.getScheduler().performTicks(39);assertEquals(5,player.getHealth());
        server.getScheduler().performTicks(1);assertTrue(player.getHealth()>5);assertTrue(plugin.casting(player).isEmpty());
    }
    @Test void consecratedBoltPreparesAndLocksItsRecipient()throws Exception{
        var original=plugin.catalog().get("mago_bianco").skills().get(1);
        var file=new java.io.File(plugin.getDataFolder(),"classes/mago_bianco.yml");var config=org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        config.set("skills."+original.id()+".preparation-seconds",1);config.save(file);command("reload");
        var target=server.addPlayer("BoltTarget");target.teleport(player.getLocation().add(0,0,4));target.getWorld().loadChunk(target.getLocation().getChunk());target.getWorld().setPVP(true);
        var engine=new SkillEngine(plugin);server.getPluginManager().registerEvents(engine,plugin);
        var disciplines=new DisciplineEngine(plugin,engine,(p,r)->target);double health=target.getHealth();
        assertTrue(disciplines.cast(player,plugin.catalog().get("mago_bianco").skills().get(1),3));
        for(int i=0;i<19;i++)disciplines.tick();assertEquals(health,target.getHealth());
        player.setRotation(180,0);disciplines.tick();assertTrue(target.getHealth()<health);
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
    @Test void guardVfxRenewsOneHandleAndStopsWhenClassChanges() {
        player.getWorld().loadChunk(player.getLocation().getChunk());
        player.getInventory().setItemInMainHand(new ItemStack(Material.IRON_SWORD));player.getInventory().setItemInOffHand(new ItemStack(Material.SHIELD));
        use("guerriero_scudo",3);var test=(TestClasses)plugin;
        var first=test.effects.stream().filter(e->e.has("handle")).findFirst().orElseThrow();
        assertEquals("MESH_SHIELD",first.get("shape").getAsString());
        assertEquals(player.getEntityId(),first.get("targetEntity").getAsInt());
        server.getScheduler().performTicks(21);
        var renewed=test.effects.stream().filter(e->e.has("handle")).toList();assertTrue(renewed.size()>=2);
        assertEquals(first.get("handle"),renewed.getLast().get("handle"));
        assertFalse(renewed.getLast().getAsJsonObject("sound").get("enabled").getAsBoolean());
        command("set",player.getName(),"mago_bianco");server.getScheduler().performTicks(1);
        assertTrue(test.stopped.contains(java.util.UUID.fromString(first.get("handle").getAsString())));
    }
    @Test void regenerationVfxStopsAtActualExpiry() {
        player.getWorld().loadChunk(player.getLocation().getChunk());player.setSneaking(true);player.setHealth(5);use("mago_bianco",3);
        var test=(TestClasses)plugin;var first=test.effects.stream().filter(e->e.has("handle")).findFirst().orElseThrow();
        assertEquals("MESH_RING",first.get("shape").getAsString());
        server.getScheduler().performTicks(121);
        assertTrue(test.stopped.contains(java.util.UUID.fromString(first.get("handle").getAsString())));
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
        restored.setIcon("mago_bianco",skill.id(),"reset");assertEquals(SkillEquipment.defaultIcon(skill),restored.icon("mago_bianco",skill));
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
        clickMenu(31);clickMenu(54);
        assertEquals("minecraft:blaze_rod",plugin.equipment().icon("mago_bianco",skill));
        assertEquals(3,player.getInventory().getItem(9).getAmount());
        clickMenu(24);assertEquals(SkillEquipment.defaultIcon(skill),plugin.equipment().icon("mago_bianco",skill));
        clickMenu(20);
        String path="texture:castigo:textures/gui/skills/orison.png";
        var input=new org.bukkit.event.player.AsyncPlayerChatEvent(false,player,path,new java.util.HashSet<>(server.getOnlinePlayers()));
        server.getPluginManager().callEvent(input);assertTrue(input.isCancelled());server.getScheduler().performTicks(1);
        assertEquals(path,plugin.equipment().icon("mago_bianco",skill));
    }
    @Test void normalPlayerCannotOpenAdminGui() {
        plugin.onCommand(player,plugin.getCommand("classe"),"classe",new String[]{"admin"});
        assertNull(player.getOpenInventory().getTopInventory());
    }
    @Test void hudMenuSavesTextureWithoutConsumingOrBroadcastingChat() throws Exception {
        player.addAttachment(plugin,"castigo.classes.admin",true);
        plugin.onCommand(player,plugin.getCommand("classe"),"classe",new String[]{"admin"});clickMenu(49);clickMenu(0);clickMenu(0);
        String texture="castigo:textures/gui/hud/default.png";
        var input=new org.bukkit.event.player.AsyncPlayerChatEvent(false,player,texture,new java.util.HashSet<>(server.getOnlinePlayers()));
        server.getPluginManager().callEvent(input);assertTrue(input.isCancelled());server.getScheduler().performTicks(1);
        assertEquals(texture,plugin.hudSettings().theme("default").text("texture"));
        assertEquals(texture,new HudSettings(new java.io.File(plugin.getDataFolder(),"hud-themes.json").toPath()).theme("default").text("texture"));
        clickMenu(49);assertEquals("",plugin.hudSettings().theme("default").text("texture"));
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
        assertEquals("castigoclasses_audio:cleric.orbs",effect.getAsJsonObject("sound").get("id").getAsString());
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
    @Test void removedEditorRejectsVfxWritesEvenForAdmins() throws Exception {
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
        assertFalse(new java.io.File(plugin.getDataFolder(),"vfx-overrides.json").exists());
        assertNotEquals(SkillPresentation.Shape.RING,plugin.presentation(player,skill).cues().get(SkillPresentation.Stage.CAST).shape());
    }
}
