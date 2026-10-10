package it.castigo.classes;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import java.util.*;

/** Admin-only group layouts. Text inputs are captured privately, persisted, then synchronized. */
public final class HudAdminGui implements Listener {
    private final CastigoClasses plugin;
    private record Input(String group,String field,long expires){}
    private final Map<UUID,Input> pending=new java.util.concurrent.ConcurrentHashMap<>();
    private static final List<String> FIELDS;
    static{var list=new ArrayList<>(HudTheme.DEFAULTS.keySet());list.add("priority");FIELDS=List.copyOf(list);}
    private static final Map<String,String> LABELS=Map.ofEntries(
        Map.entry("textScale","Scala testo %"),Map.entry("barTextScale","Scala testo barre %"),Map.entry("barHeight","Altezza barre"),Map.entry("texture","Layout PNG"),Map.entry("frame","Stile cornice"),Map.entry("background","Colore sfondo"),Map.entry("border","Colore cornice"),
        Map.entry("text","Colore testo"),Map.entry("healthColor","Colore vita"),Map.entry("resourceColor","Colore risorsa"),Map.entry("width","Larghezza HUD"),Map.entry("height","Altezza HUD"),
        Map.entry("headX","Testa: X"),Map.entry("headY","Testa: Y"),Map.entry("headSize","Dimensione testa"),Map.entry("textX","Testi: X"),
        Map.entry("nameY","Nome: Y"),Map.entry("classY","Classe: Y"),Map.entry("groupY","Gruppo: Y"),Map.entry("barsX","Barre: X"),
        Map.entry("healthY","Vita: Y"),Map.entry("resourceY","Risorsa: Y"),Map.entry("barsWidth","Larghezza barre"),Map.entry("xpY","Esperienza: Y"),Map.entry("priority","Priorità gruppo"));
    private static class Menu implements InventoryHolder {
        final UUID owner;final String group;final int page;final List<String> groups;Inventory inventory;
        Menu(Player p,String g,int page,Collection<String> groups){owner=p.getUniqueId();group=g;this.page=page;this.groups=List.copyOf(groups);}
        public Inventory getInventory(){return inventory;}
    }
    public HudAdminGui(CastigoClasses plugin){this.plugin=plugin;}
    public void cancelInput(Player p){pending.remove(p.getUniqueId());}
    public void open(Player p){cancelInput(p);show(p,null,0);}
    private ItemStack button(Material type,String title,String... lore){var item=new ItemStack(type);var meta=item.getItemMeta();meta.setDisplayName(title);meta.setLore(List.of(lore));item.setItemMeta(meta);return item;}
    private void show(Player p,String group,int page){
        if(!p.hasPermission("castigo.classes.admin"))return;
        var menu=new Menu(p,group,page,plugin.hudGroups());menu.inventory=Bukkit.createInventory(menu,54,"Castigo · HUD LuckPerms");
        if(group==null){
            for(int i=0;i<45&&page*45+i<menu.groups.size();i++){String g=menu.groups.get(page*45+i);menu.inventory.setItem(i,button(Material.PAINTING,g,"Configura layout del gruppo",g.equals("default")?"Anche tema di riserva":"Priorità: "+plugin.hudSettings().priority(g)));}
            if(page>0)menu.inventory.setItem(45,button(Material.ARROW,"Pagina precedente"));
            if((page+1)*45<menu.groups.size())menu.inventory.setItem(53,button(Material.ARROW,"Pagina successiva"));
            menu.inventory.setItem(49,button(Material.NAME_TAG,"Aggiungi gruppo","Scrivi il nome del gruppo in chat"));
        }else{
            var theme=plugin.hudSettings().theme(group);
            for(int i=0;i<FIELDS.size();i++){String f=FIELDS.get(i);menu.inventory.setItem(i,button(f.equals("texture")?Material.PAINTING:Material.PAPER,LABELS.get(f),"Gruppo: "+group,"Attuale: "+(f.equals("priority")?plugin.hudSettings().priority(group):theme.text(f)),"Clicca per impostare"));}
            for(int n=0;n<HudPresets.NAMES.size();n++)menu.inventory.setItem(28+n,button(Material.PAINTING,"Preset: "+HudPresets.NAMES.get(n),"Applica PNG e coordinate compatte","Richiede il pacchetto CastigoHUD ItemsAdder"));
            menu.inventory.setItem(45,button(Material.ARROW,"Elenco gruppi"));
            menu.inventory.setItem(49,button(Material.BARRIER,"Ripristina tema","Rimuove la personalizzazione del gruppo"));
        }
        p.openInventory(menu.inventory);
    }
    private void later(Player p,Runnable action){plugin.getServer().getScheduler().runTask(plugin,()->{if(p.isOnline())action.run();});}
    @EventHandler public void click(InventoryClickEvent e){
        if(!(e.getView().getTopInventory().getHolder() instanceof Menu m))return;e.setCancelled(true);
        if(!(e.getWhoClicked() instanceof Player p)||!m.owner.equals(p.getUniqueId())||!p.hasPermission("castigo.classes.admin"))return;
        if(e.getClick()!=ClickType.LEFT)return;int slot=e.getRawSlot();if(slot<0||slot>=54)return;
        later(p,()->{if(!p.hasPermission("castigo.classes.admin"))return;try{
            if(m.group==null){
                if(slot<45&&m.page*45+slot<m.groups.size())show(p,m.groups.get(m.page*45+slot),0);
                else if(slot==45&&m.page>0)show(p,null,m.page-1);
                else if(slot==53&&(m.page+1)*45<m.groups.size())show(p,null,m.page+1);
                else if(slot==49)ask(p,null,"group");
            }else if(slot<FIELDS.size())ask(p,m.group,FIELDS.get(slot));
            else if(slot>=28&&slot<28+HudPresets.NAMES.size()){plugin.hudSettings().preset(m.group,HudPresets.NAMES.get(slot-28));plugin.syncHud();show(p,m.group,0);}
            else if(slot==45)show(p,null,0);
            else if(slot==49){plugin.hudSettings().reset(m.group);plugin.syncHud();show(p,m.group,0);}
        }catch(Exception ex){p.sendMessage("HUD non salvata: "+ex.getMessage());}});
    }
    private void ask(Player p,String group,String field){
        pending.put(p.getUniqueId(),new Input(group,field,System.nanoTime()+120_000_000_000L));p.closeInventory();
        String hint=switch(field){case "texture"->"namespace:textures/gui/hud/nome.png oppure nessuna";case "frame"->"MEDIEVAL, FLAT oppure NONE";
            case "resourceColor"->"RGB esadecimale oppure class";case "group"->"nome gruppo LuckPerms";default->HudTheme.DEFAULTS.get(field) instanceof String?"RGB esadecimale, ad esempio FFD700":"numero intero in pixel (priorità: 0–10000)";};
        p.sendMessage("HUD — "+LABELS.getOrDefault(field,"Gruppo")+": "+hint+". Scrivi in chat entro 2 minuti; annulla per tornare.");
    }
    @EventHandler(priority=EventPriority.LOWEST) public void input(AsyncPlayerChatEvent e){
        var in=pending.get(e.getPlayer().getUniqueId());if(in==null)return;
        if(System.nanoTime()>in.expires()){pending.remove(e.getPlayer().getUniqueId(),in);return;}
        e.setCancelled(true);String value=e.getMessage().strip();
        later(e.getPlayer(),()->{Player p=e.getPlayer();if(pending.get(p.getUniqueId())!=in)return;
            if(!p.hasPermission("castigo.classes.admin")){cancelInput(p);return;}
            try{
                if(value.equalsIgnoreCase("annulla")){cancelInput(p);show(p,in.group(),0);return;}
                if(in.field().equals("group")){HudSettings.validateGroup(value);plugin.hudSettings().set(value,"priority","0");cancelInput(p);plugin.syncHud();show(p,value,0);return;}
                plugin.hudSettings().set(in.group(),in.field(),in.field().equals("texture")&&value.equalsIgnoreCase("nessuna")?"":value);
                cancelInput(p);plugin.syncHud();p.sendMessage("HUD salvata e aggiornata.");show(p,in.group(),0);
            }catch(Exception ex){p.sendMessage("HUD non salvata: "+ex.getMessage()+". Riprova o scrivi annulla.");}
        });
    }
    @EventHandler public void drag(InventoryDragEvent e){if(e.getView().getTopInventory().getHolder() instanceof Menu)e.setCancelled(true);}
    @EventHandler public void quit(PlayerQuitEvent e){cancelInput(e.getPlayer());}
}
