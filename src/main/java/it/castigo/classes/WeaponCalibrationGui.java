package it.castigo.classes;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import java.util.*;
public final class WeaponCalibrationGui implements Listener {
    private final CastigoClasses plugin;
    private static final List<String> LABELS=List.of("Prima persona: destra/sinistra","Prima persona: altezza","Prima persona: profondita","Prima persona: inclinazione","Prima persona: rotazione laterale","Prima persona: torsione","Mano di supporto: destra/sinistra","Mano di supporto: altezza","Mano di supporto: profondita","Braccio principale: inclinazione","Braccio principale: rotazione laterale","Braccio principale: torsione","Braccio supporto: inclinazione","Braccio supporto: rotazione laterale","Braccio supporto: torsione","Intensita in prima persona","Intensita in terza persona");
    private record Pending(String item,String field,long until){}
    private final Map<UUID,Pending> pending=new java.util.concurrent.ConcurrentHashMap<>();
    private static class Menu implements InventoryHolder{final UUID owner;final String item;Inventory inv;Menu(Player p,String item){owner=p.getUniqueId();this.item=item;}public Inventory getInventory(){return inv;}}
    public WeaponCalibrationGui(CastigoClasses p){plugin=p;}
    public void cancel(Player p){pending.remove(p.getUniqueId());}
    private ItemStack button(String label,String... lore){var i=new ItemStack(Material.PAPER);var m=i.getItemMeta();m.setDisplayName(label);m.setLore(List.of(lore));i.setItemMeta(m);return i;}
    public void open(Player p,String item){if(!p.hasPermission("castigo.classes.admin"))return;cancel(p);var m=new Menu(p,item);m.inv=Bukkit.createInventory(m,54,"Castigo · Pose armi");
        if(item==null)m.inv.setItem(4,button("Clicca l'arma nel tuo inventario","Supporta ItemsAdder; non consuma oggetti"));
        else{var values=plugin.calibration().get(item);for(int n=0;n<WeaponCalibration.FIELDS.size();n++){String f=WeaponCalibration.FIELDS.get(n);m.inv.setItem(n,button(LABELS.get(n),values.has(f)?values.get(f).getAsString():"Predefinito",f.endsWith("Intensity")?"Intensita 0–2":f.endsWith("X")||f.endsWith("Y")||f.endsWith("Z")?"Spostamento -2–2":"Angolo in gradi -180–180","Clicca e scrivi il valore in chat"));}m.inv.setItem(49,button("Ripristina posa dell'arma"));m.inv.setItem(45,button("Scegli altra arma"));}p.openInventory(m.inv);}
    @EventHandler public void click(InventoryClickEvent e){if(!(e.getView().getTopInventory().getHolder() instanceof Menu m))return;e.setCancelled(true);if(!(e.getWhoClicked() instanceof Player p)||!m.owner.equals(p.getUniqueId())||!p.hasPermission("castigo.classes.admin")||e.getClick()!=ClickType.LEFT)return;
        int slot=e.getRawSlot();String selected=null;try{if(slot>=54&&e.getClickedInventory()==p.getInventory())selected=SkillEquipment.identify(p.getInventory().getItem(e.getSlot()));}catch(Exception ex){p.sendMessage(ex.getMessage());return;}String item=selected;
        plugin.getServer().getScheduler().runTask(plugin,()->{if(!p.isOnline()||!p.hasPermission("castigo.classes.admin"))return;try{if(item!=null){open(p,item);return;}if(m.item==null)return;if(slot==45)open(p,null);else if(slot==49){plugin.calibration().set(m.item,"reset","");open(p,m.item);}else if(slot>=0&&slot<WeaponCalibration.FIELDS.size()){pending.put(p.getUniqueId(),new Pending(m.item,WeaponCalibration.FIELDS.get(slot),System.nanoTime()+120_000_000_000L));p.closeInventory();p.sendMessage("Scrivi il valore numerico per "+WeaponCalibration.FIELDS.get(slot)+", oppure annulla.");}}catch(Exception ex){p.sendMessage("Posa non salvata: "+ex.getMessage());}});
    }
    @EventHandler(priority=EventPriority.LOWEST) public void chat(AsyncPlayerChatEvent e){var in=pending.get(e.getPlayer().getUniqueId());if(in==null)return;if(System.nanoTime()>in.until){pending.remove(e.getPlayer().getUniqueId());return;}e.setCancelled(true);String value=e.getMessage().strip();Player p=e.getPlayer();plugin.getServer().getScheduler().runTask(plugin,()->{if(!p.isOnline()||!p.hasPermission("castigo.classes.admin")||pending.get(p.getUniqueId())!=in)return;try{if(!value.equalsIgnoreCase("annulla"))plugin.calibration().set(in.item,in.field,value);open(p,in.item);}catch(Exception ex){p.sendMessage(ex.getMessage()+". Riprova o annulla.");}});}
    @EventHandler public void drag(InventoryDragEvent e){if(e.getView().getTopInventory().getHolder() instanceof Menu)e.setCancelled(true);}
    @EventHandler public void quit(PlayerQuitEvent e){cancel(e.getPlayer());}
}
