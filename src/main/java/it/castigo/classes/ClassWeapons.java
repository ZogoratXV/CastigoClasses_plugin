package it.castigo.classes;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import java.util.*;

/** Item IDs are copied from inventory; menu clicks never move or consume items. */
public final class ClassWeapons implements Listener {
    private final CastigoClasses plugin;
    public ClassWeapons(CastigoClasses plugin){this.plugin=plugin;}
    private static final class Menu implements InventoryHolder {
        final UUID owner;final String classId;final boolean off;final int page;final List<String> entries;Inventory inventory;
        Menu(Player p,String id,boolean off,int page,List<String> entries){owner=p.getUniqueId();classId=id;this.off=off;this.page=page;this.entries=entries;}
        public Inventory getInventory(){return inventory;}
    }
    private static ItemStack button(Material type,String name,String... lines){
        var item=new ItemStack(type);var meta=item.getItemMeta();meta.setDisplayName(name);meta.setLore(List.of(lines));item.setItemMeta(meta);return item;
    }
    public void open(Player p,String id,boolean off,int page){
        if(!p.hasPermission("castigo.classes.admin"))return;
        var definition=plugin.catalog().get(id);if(definition==null)throw new IllegalArgumentException("Classe sconosciuta");
        var pool=plugin.equipment().pool(id,off);var entries=pool==null?List.<String>of():pool;
        page=Math.clamp(page,0,Math.max(0,(entries.size()-1)/45));
        var menu=new Menu(p,id,off,page,entries);menu.inventory=Bukkit.createInventory(menu,54,"Armi · "+definition.name());
        for(int i=0;i<45&&page*45+i<entries.size();i++){
            String item=entries.get(page*45+i);ItemStack display=null;
            try{if(item.startsWith("itemsadder:")){
                var ia=Bukkit.getPluginManager().getPlugin("ItemsAdder");if(ia!=null&&ia.isEnabled()){
                    var api=Class.forName("dev.lone.itemsadder.api.CustomStack",true,ia.getClass().getClassLoader());
                    Object custom=api.getMethod("getInstance",String.class).invoke(null,item.substring(11));
                    if(custom!=null)display=((ItemStack)api.getMethod("getItemStack").invoke(custom)).clone();
                }
            }else{var material=Material.matchMaterial(item);if(material!=null)display=new ItemStack(material);}}catch(Exception|LinkageError ignored){}
            if(display==null)display=new ItemStack(Material.PAPER);display.setAmount(1);var meta=display.getItemMeta();meta.setLore(List.of(item,"Clic destro: rimuovi dalla lista"));display.setItemMeta(meta);menu.inventory.setItem(i,display);
        }
        menu.inventory.setItem(45,button(Material.ARROW,"Pagina precedente"));
        menu.inventory.setItem(46,button(Material.BOOK,"Torna alle classi"));
        menu.inventory.setItem(48,button(Material.BARRIER,"Ripristina ereditarietà / nessun limite","Rimuove solo la lista locale di questa mano"));
        menu.inventory.setItem(49,button(off?Material.SHIELD:Material.IRON_SWORD,off?"Mano secondaria · scudi":"Mano principale · armi","Clic: cambia mano","Clicca un oggetto nel TUO inventario per aggiungerlo",pool==null?"Nessuna lista configurata":plugin.equipment().ownPool(id,off)?"Lista della classe: "+entries.size():"Lista ereditata: "+entries.size()));
        menu.inventory.setItem(50,button(Material.HOPPER,"Attiva lista vuota","Nessuna arma consentita finché non aggiungi oggetti"));
        menu.inventory.setItem(53,button(Material.ARROW,"Pagina successiva"));p.openInventory(menu.inventory);
    }
    @EventHandler public void click(InventoryClickEvent event){
        if(!(event.getView().getTopInventory().getHolder() instanceof Menu m))return;event.setCancelled(true);
        if(!(event.getWhoClicked() instanceof Player p)||!p.getUniqueId().equals(m.owner)||!p.hasPermission("castigo.classes.admin"))return;
        if(event.getClick()!=ClickType.LEFT&&event.getClick()!=ClickType.RIGHT)return;
        int slot=event.getRawSlot();
        try{
            var current=plugin.equipment().pool(m.classId,m.off);var list=new ArrayList<>(current==null?List.<String>of():current);boolean changed=false;
            if(slot>=54&&event.getClickedInventory()==p.getInventory()){
                String id=SkillEquipment.identify(p.getInventory().getItem(event.getSlot()));if(!list.contains(id))list.add(id);changed=true;
            }else if(slot>=0&&slot<45&&event.getClick()==ClickType.RIGHT&&m.page*45+slot<m.entries.size()){
                list.remove(m.entries.get(m.page*45+slot));changed=true;
            }else if(slot==48){plugin.equipment().setPool(m.classId,m.off,null);plugin.refreshCatalogs();}
            else if(slot==50){list.clear();changed=true;}
            else if(slot==46){plugin.getServer().getScheduler().runTask(plugin,()->{if(p.isOnline())plugin.onCommand(p,plugin.getCommand("classe"),"classe",new String[]{"admin"});});return;}
            if(changed){plugin.equipment().setPool(m.classId,m.off,list);plugin.refreshCatalogs();p.sendMessage("Lista armi salvata. Gli oggetti restano nel tuo inventario.");}
            int page=slot==45?m.page-1:slot==53?m.page+1:m.page;boolean off=slot==49?!m.off:m.off;
            plugin.getServer().getScheduler().runTask(plugin,()->{if(p.isOnline())open(p,m.classId,off,page);});
        }catch(Exception ex){p.sendMessage("§c"+ex.getMessage());}
    }
    @EventHandler public void drag(InventoryDragEvent e){if(e.getView().getTopInventory().getHolder() instanceof Menu)e.setCancelled(true);}
    public boolean allowed(Player p,boolean off,ItemStack item){var data=plugin.profile(p);return data==null||plugin.equipment().allowed(data.classId,off,item);}
    @EventHandler(priority=EventPriority.LOWEST,ignoreCancelled=true) public void hit(EntityDamageByEntityEvent e){
        if(e.getDamager() instanceof Player p&&!allowed(p,false,p.getInventory().getItemInMainHand())){e.setCancelled(true);plugin.feedback(p,"Arma non consentita per la tua classe.");}
    }
    @EventHandler(priority=EventPriority.LOWEST,ignoreCancelled=true) public void bow(EntityShootBowEvent e){
        if(e.getEntity() instanceof Player p&&!allowed(p,e.getHand()==EquipmentSlot.OFF_HAND,e.getBow())){e.setCancelled(true);e.setConsumeItem(false);plugin.feedback(p,"Arma non consentita per la tua classe.");}
    }
    @EventHandler(priority=EventPriority.LOWEST,ignoreCancelled=true) public void use(org.bukkit.event.player.PlayerInteractEvent e){
        if(!e.getAction().isRightClick()||e.getItem()==null)return;
        if(Set.of(Material.BOW,Material.CROSSBOW,Material.TRIDENT,Material.SHIELD).contains(e.getItem().getType())&&!allowed(e.getPlayer(),e.getHand()==EquipmentSlot.OFF_HAND,e.getItem())){e.setCancelled(true);plugin.feedback(e.getPlayer(),"Oggetto non consentito per la tua classe.");}
    }
}
