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
}
