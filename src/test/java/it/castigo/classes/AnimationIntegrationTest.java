package it.castigo.classes;
import com.google.gson.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.*;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class AnimationIntegrationTest {
    public static class VisualPlugin extends DisciplineIntegrationTest.TestClasses {
        record Sent(UUID player,String type,JsonObject data){}
        final List<Sent> sent=new ArrayList<>();
        @Override public void send(Player p,String type,JsonObject data){sent.add(new Sent(p.getUniqueId(),type,data.deepCopy()));}
    }
    ServerMock server;VisualPlugin plugin;PlayerMock player,observer;
    @BeforeEach void setup(){server=MockBukkit.mock();server.addSimpleWorld("world");MockBukkit.createMockPlugin("CastigoCore");plugin=MockBukkit.loadWith(VisualPlugin.class,getClass().getResourceAsStream("/plugin.yml"));player=server.addPlayer("Fighter");observer=server.addPlayer("Observer");player.addAttachment(plugin,"castigo.classes.use",true);player.getInventory().setItemInMainHand(new ItemStack(Material.IRON_SWORD));}
    @AfterEach void cleanup(){MockBukkit.unmock();}
    void packet(Player p,String json){plugin.onPluginMessageReceived(CastigoClasses.CHANNEL,p,json.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    void hello(Player p,boolean modern){packet(p,"{\"v\":1,\"type\":\"hello\",\"clientVfx\":1,\"weaponAnimations\":"+(modern?2:0)+"}");}
    @Test void visualRequestsDoNotChangeCombatAndDeduplicate()throws Exception{
        hello(player,true);hello(observer,true);plugin.sent.clear();double health=player.getHealth(),resource=plugin.profile(player).resource;long xp=plugin.profile(player).xp;
        String attack="{\"v\":1,\"type\":\"attack_visual\",\"id\":1}";
        assertFalse(player.isDead());assertFalse(player.isHandRaised());assertEquals("SWORD",plugin.animationProfile(player,player.getInventory().getItemInMainHand(),true));
        packet(player,attack);packet(player,attack);
        assertEquals(2,plugin.sent.stream().filter(s->s.type().equals("weapon_motion_v2")).count());assertEquals(health,player.getHealth());assertEquals(resource,plugin.profile(player).resource);assertEquals(xp,plugin.profile(player).xp);assertTrue(plugin.profile(player).cooldowns.isEmpty());
    }
    @Test void oldObserversReceiveOnlyLegacySkillAnimations(){
        hello(player,true);hello(observer,false);plugin.sent.clear();packet(player,"{\"v\":1,\"type\":\"attack_visual\",\"id\":1}");assertTrue(plugin.sent.stream().noneMatch(s->s.player().equals(observer.getUniqueId())));
        plugin.sent.clear();plugin.weaponMotion(player,plugin.catalog().get("guerriero_due_mani").skills().getFirst(),"release",12);
        assertTrue(plugin.sent.stream().anyMatch(s->s.player().equals(observer.getUniqueId())&&s.type().equals("weapon_motion")));
        assertTrue(plugin.sent.stream().anyMatch(s->s.player().equals(player.getUniqueId())&&s.type().equals("weapon_motion_v2")));
    }
    @Test void handshakeAndSpectatorAndVanillaOverrideGateVisuals()throws Exception{
        packet(player,"{\"v\":1,\"type\":\"attack_visual\",\"id\":1}");assertTrue(plugin.sent.stream().noneMatch(s->s.type().equals("weapon_motion_v2")));
        hello(player,true);plugin.sent.clear();player.setGameMode(GameMode.SPECTATOR);packet(player,"{\"v\":1,\"type\":\"attack_visual\",\"id\":2}");assertTrue(plugin.sent.isEmpty());
        player.setGameMode(GameMode.SURVIVAL);plugin.calibration().set("minecraft:iron_sword","profile","VANILLA");packet(player,"{\"v\":1,\"type\":\"attack_visual\",\"id\":3}");assertTrue(plugin.sent.isEmpty());
    }
    @Test void defaultsOverridesAndTwoHandedPrecedencePersist()throws Exception{
        assertEquals("SWORD",plugin.animationProfile(player,new ItemStack(Material.IRON_SWORD),true));assertEquals("AXE",plugin.animationProfile(player,new ItemStack(Material.IRON_AXE),true));assertEquals("VANILLA",plugin.animationProfile(player,new ItemStack(Material.STICK),true));
        plugin.calibration().set("minecraft:iron_sword","firstY","-0.2");plugin.calibration().set("minecraft:iron_sword","profile","THRUST");assertEquals("THRUST",plugin.animationProfile(player,player.getInventory().getItemInMainHand(),true));
        var restored=new WeaponCalibration(plugin.getDataFolder().toPath().resolve("weapon-poses.json"));assertEquals(-.2,restored.get("minecraft:iron_sword").get("firstY").getAsDouble());assertEquals("THRUST",restored.get("minecraft:iron_sword").get("profile").getAsString());
        plugin.progressionSettings().set("guerriero_due_mani","two-handed-items","minecraft:iron_sword");plugin.profile(player).classId="guerriero_due_mani";assertEquals("TWO_HANDED",plugin.animationProfile(player,player.getInventory().getItemInMainHand(),true));
    }
    @Test void guiProfileSelectionPreservesCalibration()throws Exception{
        player.addAttachment(plugin,"castigo.classes.admin",true);plugin.calibration().set("minecraft:iron_sword","firstY","-0.2");plugin.openCalibration(player);player.getInventory().setItem(9,new ItemStack(Material.IRON_SWORD));click(54);click(30);
        assertEquals("TWO_HANDED",plugin.calibration().get("minecraft:iron_sword").get("profile").getAsString());assertEquals(-.2,plugin.calibration().get("minecraft:iron_sword").get("firstY").getAsDouble());assertEquals(Material.IRON_SWORD,player.getInventory().getItem(9).getType());
    }
    void click(int slot){var e=new org.bukkit.event.inventory.InventoryClickEvent(player.getOpenInventory(),org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER,slot,org.bukkit.event.inventory.ClickType.LEFT,org.bukkit.event.inventory.InventoryAction.PICKUP_ALL);server.getPluginManager().callEvent(e);assertTrue(e.isCancelled());server.getScheduler().performTicks(1);}
}
