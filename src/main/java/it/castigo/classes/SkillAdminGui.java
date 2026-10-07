package it.castigo.classes;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import java.util.*;

/** The menu reads a clicked inventory item without moving or consuming it. */
public final class SkillAdminGui implements Listener {
    private final CastigoClasses plugin;
    public SkillAdminGui(CastigoClasses plugin) { this.plugin=plugin; }
    private static final class Menu implements InventoryHolder {
        final UUID owner;final String classId;final int skill,page;final boolean off;Inventory inventory;
        Menu(Player p,String c,int s,int page,boolean off) { owner=p.getUniqueId();classId=c;skill=s;this.page=page;this.off=off; }
        public Inventory getInventory() { return inventory; }
    }
    private ItemStack button(Material material,String name,String... lore) {
        var item=new ItemStack(material);var meta=item.getItemMeta();meta.setDisplayName(name);meta.setLore(List.of(lore));item.setItemMeta(meta);return item;
    }
    public void open(Player p) { show(p,null,-1,0,false); }
    private void show(Player p,String classId,int skill,int page,boolean off) {
        if(!p.hasPermission("castigo.classes.admin"))return;
        var menu=new Menu(p,classId,skill,page,off);menu.inventory=Bukkit.createInventory(menu,54,"Castigo · Configura skill");
        var inv=menu.inventory;
        if(classId==null) {
            var classes=new ArrayList<>(plugin.catalog().all().values());
            for(int i=0;i<45&&page*45+i<classes.size();i++) { var c=classes.get(page*45+i);inv.setItem(i,button(Material.BOOK,c.name(),c.id())); }
            if(page>0)inv.setItem(45,button(Material.ARROW,"Pagina precedente"));
            if((page+1)*45<classes.size())inv.setItem(53,button(Material.ARROW,"Pagina successiva"));
        } else {
            var c=plugin.catalog().get(classId);if(c==null)return;
            if(skill<0)for(int i=0;i<8;i++) { var s=c.skills().get(i);var r=plugin.equipment().requirement(c.id(),s,plugin.catalog().mechanics(s));inv.setItem(i,button(Material.PAPER,s.name(),r.item(),r.offhand()?"Mano secondaria":"Mano principale","Clicca per modificare")); }
            else {
                var s=c.skills().get(skill);var r=plugin.equipment().requirement(c.id(),s,plugin.catalog().mechanics(s));
                inv.setItem(4,button(Material.BOOK,s.name(),"Attuale: "+r.item(),r.offhand()?"Secondaria":"Principale","Clicca un oggetto nel TUO inventario","per assegnarlo. Non viene consumato."));
                inv.setItem(20,button(Material.IRON_SWORD,"Seleziona mano principale",off?"Clicca per selezionare":"SELEZIONATA"));
                inv.setItem(24,button(Material.SHIELD,"Seleziona mano secondaria",off?"SELEZIONATA":"Clicca per selezionare"));
                inv.setItem(31,button(Material.PAINTING,"Icona personalizzata",plugin.equipment().icon(c.id(),s),"/classe icona "+c.id()+" "+s.id(),"texture:castigo:textures/gui/skills/nome.png"));
            }
            inv.setItem(45,button(Material.ARROW,"Indietro"));
        }
        p.openInventory(inv);
    }
    @EventHandler public void click(InventoryClickEvent e) {
        if(!(e.getView().getTopInventory().getHolder() instanceof Menu m))return;e.setCancelled(true);
        if(!(e.getWhoClicked() instanceof Player p)||!p.getUniqueId().equals(m.owner)||!p.hasPermission("castigo.classes.admin"))return;
        if(e.getClick()!=ClickType.LEFT&&e.getClick()!=ClickType.RIGHT)return;
        int slot=e.getRawSlot();
        try {
            if(slot>=54&&m.classId!=null&&m.skill>=0&&e.getClickedInventory()==p.getInventory()) {
                var c=plugin.catalog().get(m.classId);if(c==null)return;var s=c.skills().get(m.skill);
                ItemStack selected=p.getInventory().getItem(e.getSlot());
                String id=SkillEquipment.identify(selected);
                if(plugin.catalog().mechanics(s).weapon()==it.castigo.classes.model.SkillMechanics.Weapon.BOW&&(m.off||selected.getType()!=Material.BOW))throw new IllegalArgumentException("I tiri richiedono un arco nella mano principale, anche ItemsAdder.");
                plugin.equipment().setItem(c.id(),s.id(),new SkillEquipment.Requirement(m.off,id));
                p.sendMessage("Oggetto salvato: "+id);plugin.refreshCatalogs();
                later(p,()->show(p,m.classId,m.skill,0,m.off));return;
            }
            if(slot<0||slot>=54)return;
            if(m.classId==null) {
                var classes=new ArrayList<>(plugin.catalog().all().values());int index=m.page*45+slot;
                if(slot<45&&index<classes.size())later(p,()->show(p,classes.get(index).id(),-1,0,false));
                if(slot==45&&m.page>0)later(p,()->show(p,null,-1,m.page-1,false));
                if(slot==53&&(m.page+1)*45<classes.size())later(p,()->show(p,null,-1,m.page+1,false));
            } else if(slot==45)later(p,()->show(p,m.skill>=0?m.classId:null,-1,0,false));
            else if(m.skill<0&&slot<8) {
                var c=plugin.catalog().get(m.classId);if(c==null)return;var s=c.skills().get(slot);
                boolean off=plugin.equipment().requirement(c.id(),s,plugin.catalog().mechanics(s)).offhand();
                later(p,()->show(p,m.classId,slot,0,off));
            } else if(m.skill>=0&&(slot==20||slot==24))later(p,()->show(p,m.classId,m.skill,0,slot==24));
        } catch(Exception ex) { p.sendMessage("Configurazione non salvata: "+ex.getMessage()); }
    }
    private void later(Player p,Runnable action) { plugin.getServer().getScheduler().runTask(plugin,()-> { if(p.isOnline())action.run(); }); }
    @EventHandler public void drag(InventoryDragEvent e) { if(e.getView().getTopInventory().getHolder() instanceof Menu)e.setCancelled(true); }
}
