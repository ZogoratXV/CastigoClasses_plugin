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
    private record IconInput(String classId,int skill,boolean off,long expires) {}
    private final Map<UUID,IconInput> inputs=new java.util.concurrent.ConcurrentHashMap<>();
    public SkillAdminGui(CastigoClasses plugin) { this.plugin=plugin; }
    private static final class Menu implements InventoryHolder {
        final UUID owner;final String classId;final int skill,page;final boolean off,icons;Inventory inventory;
        Menu(Player p,String c,int s,int page,boolean off) { this(p,c,s,page,off,false); }
        Menu(Player p,String c,int s,int page,boolean off,boolean icons) { owner=p.getUniqueId();classId=c;skill=s;this.page=page;this.off=off;this.icons=icons; }
        public Inventory getInventory() { return inventory; }
    }
    private ItemStack button(Material material,String name,String... lore) {
        var item=new ItemStack(material);var meta=item.getItemMeta();meta.setDisplayName(name);meta.setLore(List.of(lore));item.setItemMeta(meta);return item;
    }
    public void cancelInput(Player p){inputs.remove(p.getUniqueId());}
    public void open(Player p) { plugin.closeProgressionInput(p);cancelInput(p);plugin.closeHudInput(p);show(p,null,-1,0,false); }
    private void show(Player p,String classId,int skill,int page,boolean off) {
        if(!p.hasPermission("castigo.classes.admin"))return;
        var menu=new Menu(p,classId,skill,page,off);menu.inventory=Bukkit.createInventory(menu,54,"Castigo · Configura skill");
        var inv=menu.inventory;
        if(classId==null) {
            inv.setItem(48,button(Material.EXPERIENCE_BOTTLE,"Progressione e sottoclassi","Cap XP, reset, gruppi e oggetti"));
            inv.setItem(49,button(Material.PAINTING,"HUD gruppi LuckPerms","Layout PNG, colori e posizione elementi"));
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
                inv.setItem(31,button(Material.PAINTING,"Configura icona",plugin.equipment().icon(c.id(),s),"Clicca per scegliere un oggetto","o una texture PNG personalizzata."));
            }
            inv.setItem(45,button(Material.ARROW,"Indietro"));
        }
        p.openInventory(inv);
    }
    private void icons(Player p,Menu previous) {
        if(!p.hasPermission("castigo.classes.admin"))return;
        var c=plugin.catalog().get(previous.classId);if(c==null||previous.skill>=c.skills().size())return;
        var s=c.skills().get(previous.skill);var m=new Menu(p,c.id(),previous.skill,0,previous.off,true);
        m.inventory=Bukkit.createInventory(m,54,"Castigo · Icona skill");
        m.inventory.setItem(4,button(Material.PAINTING,s.name(),"Attuale: "+plugin.equipment().icon(c.id(),s),"Clicca un oggetto vanilla nel tuo inventario","per usarlo come icona, senza consumarlo."));
        m.inventory.setItem(20,button(Material.NAME_TAG,"Texture PNG personalizzata","Clicca, poi scrivi il percorso in chat.","texture:castigo:textures/gui/skills/nome.png","La texture deve essere presente nel client."));
        m.inventory.setItem(24,button(Material.BARRIER,"Ripristina icona originale"));
        m.inventory.setItem(45,button(Material.ARROW,"Indietro"));p.openInventory(m.inventory);
    }
    @EventHandler public void click(InventoryClickEvent e) {
        if(!(e.getView().getTopInventory().getHolder() instanceof Menu m))return;e.setCancelled(true);
        if(!(e.getWhoClicked() instanceof Player p)||!p.getUniqueId().equals(m.owner)||!p.hasPermission("castigo.classes.admin"))return;
        if(e.getClick()!=ClickType.LEFT&&e.getClick()!=ClickType.RIGHT)return;
        int slot=e.getRawSlot();
        try {
            if(m.icons) {
                var c=plugin.catalog().get(m.classId);if(c==null||m.skill>=c.skills().size())return;var s=c.skills().get(m.skill);
                if(slot>=54&&e.getClickedInventory()==p.getInventory()) {
                    String id=SkillEquipment.identify(p.getInventory().getItem(e.getSlot()));
                    if(id.startsWith("itemsadder:"))throw new IllegalArgumentException("Per un'icona ItemsAdder scegli Texture PNG e indica la texture del resource pack.");
                    saveIcon(p,c.id(),s.id(),id);later(p,()->icons(p,m));
                } else if(slot==24) { saveIcon(p,c.id(),s.id(),"reset");later(p,()->icons(p,m)); }
                else if(slot==45)later(p,()->show(p,m.classId,m.skill,0,m.off));
                else if(slot==20)later(p,()-> {
                    inputs.put(p.getUniqueId(),new IconInput(m.classId,m.skill,m.off,System.nanoTime()+120_000_000_000L));
                    p.closeInventory();p.sendMessage("Scrivi il percorso texture:namespace:textures/gui/skills/nome.png, oppure annulla. Hai 2 minuti; il messaggio resta privato.");
                });
                return;
            }
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
                if(slot==48){later(p,()->plugin.openProgression(p));return;}
                if(slot==49){later(p,()->plugin.openHud(p));return;}
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
            else if(m.skill>=0&&slot==31)later(p,()->icons(p,m));
        } catch(Exception ex) { p.sendMessage("Configurazione non salvata: "+ex.getMessage()); }
    }
    private void later(Player p,Runnable action) { plugin.getServer().getScheduler().runTask(plugin,()-> { if(p.isOnline())action.run(); }); }
    private void saveIcon(Player p,String classId,String skill,String icon) throws Exception {
        plugin.equipment().setIcon(classId,skill,icon);plugin.refreshCatalogs();p.sendMessage("Icona salvata e inviata alla mod.");
    }
    @EventHandler(priority=EventPriority.LOWEST) public void iconText(org.bukkit.event.player.AsyncPlayerChatEvent e) {
        UUID id=e.getPlayer().getUniqueId();var input=inputs.get(id);if(input==null)return;
        if(System.nanoTime()>input.expires()) { inputs.remove(id,input);return; }
        e.setCancelled(true);String text=e.getMessage().strip();
        later(e.getPlayer(),()-> {
            Player p=e.getPlayer();if(inputs.get(id)!=input)return;
            if(!p.hasPermission("castigo.classes.admin")) { inputs.remove(id,input);return; }
            var c=plugin.catalog().get(input.classId());if(c==null||input.skill()>=c.skills().size()) { inputs.remove(id,input);return; }
            try {
                if(!text.equalsIgnoreCase("annulla")) {
                    if(!text.startsWith("texture:"))throw new IllegalArgumentException("Usa texture:namespace:textures/gui/skills/nome.png oppure annulla.");
                    saveIcon(p,c.id(),c.skills().get(input.skill()).id(),text);
                }
                inputs.remove(id,input);icons(p,new Menu(p,input.classId(),input.skill(),0,input.off()));
            } catch(Exception ex) { p.sendMessage("Icona non salvata: "+ex.getMessage()+" Riprova oppure scrivi annulla."); }
        });
    }
    @EventHandler public void quit(org.bukkit.event.player.PlayerQuitEvent e) { inputs.remove(e.getPlayer().getUniqueId()); }
    @EventHandler public void drag(InventoryDragEvent e) { if(e.getView().getTopInventory().getHolder() instanceof Menu)e.setCancelled(true); }
}
