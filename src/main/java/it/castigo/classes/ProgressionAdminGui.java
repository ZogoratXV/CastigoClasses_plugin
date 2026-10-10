package it.castigo.classes;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import java.util.*;

/** All writes recheck ownership and permission on the main thread. Inventory selections never move items. */
public final class ProgressionAdminGui implements Listener {
    private final CastigoClasses plugin;
    private record Input(String id,String field,long deadline){}
    private final Map<UUID,Input> inputs=new java.util.concurrent.ConcurrentHashMap<>();
    private static final class Menu implements InventoryHolder {
        final UUID owner;final String id,select;final int page;Inventory inv;
        Menu(Player p,String id,String select,int page){owner=p.getUniqueId();this.id=id;this.select=select;this.page=page;}
        public Inventory getInventory(){return inv;}
    }
    public ProgressionAdminGui(CastigoClasses p){plugin=p;}
    public void cancelInput(Player p){inputs.remove(p.getUniqueId());}
    public void open(Player p){cancelInput(p);show(p,null,"",0);}
    private ItemStack button(Material mat,String title,String... lore){var s=new ItemStack(mat);var meta=s.getItemMeta();meta.setDisplayName(title);meta.setLore(List.of(lore));s.setItemMeta(meta);return s;}
    private void show(Player p,String id,String select,int page){
        if(!p.hasPermission("castigo.classes.admin"))return;
        var m=new Menu(p,id,select,page);m.inv=Bukkit.createInventory(m,54,"Castigo · Progressione");var s=plugin.progressionSettings();
        if(id==null){
            var classes=new ArrayList<>(plugin.catalog().all().values());
            for(int i=0;i<36&&page*36+i<classes.size();i++){var c=classes.get(page*36+i);m.inv.setItem(i,button(Material.BOOK,c.name(),c.id(),"Cap livello: "+plugin.classCap(c.id())));}
            m.inv.setItem(38,button(Material.EXPERIENCE_BOTTLE,"Cap XP giornaliero: "+s.dailyCap(),"0 = illimitato; vale per giocatore","Clicca e scrivi il nuovo limite"));
            m.inv.setItem(39,button(Material.CLOCK,"Orario reset: "+s.resetTime(),"Fuso Europe/Rome (ora italiana)","Clicca e scrivi HH:mm"));
            m.inv.setItem(40,button(Material.REDSTONE,"Reset XP giornaliera di tutti","Resetta il contatore, non i livelli","Include anche i giocatori offline"));
            m.inv.setItem(41,button(Material.PLAYER_HEAD,"Reset XP giornaliera giocatore","Scrivi il nome di un giocatore online"));
            if(page>0)m.inv.setItem(45,button(Material.ARROW,"Precedente"));
            if((page+1)*36<classes.size())m.inv.setItem(53,button(Material.ARROW,"Successiva"));
        }else if(select.equals("confirm-reset")){
            m.inv.setItem(22,button(Material.EMERALD_BLOCK,"Conferma reset giornaliero di tutti","Azzera soltanto la quota giornaliera"));
            m.inv.setItem(45,button(Material.ARROW,"Annulla"));
        }else{
            var c=plugin.catalog().get(id);if(c==null)return;
            if(!select.isEmpty()){
                m.inv.setItem(4,button(Material.PAPER,select.equals("promotion-item")?"Oggetto consumabile di promozione":"Spade con impugnatura a due mani","Clicca un oggetto nel TUO inventario","Supporta ItemsAdder. Non viene consumato.",select.equals("two-handed-items")?"Oggetto gia presente: clicca per rimuoverlo":"Serve per ENTRARE nella classe selezionata"));
                m.inv.setItem(31,button(Material.BARRIER,"Svuota impostazione"));
            }else{
                m.inv.setItem(10,button(Material.EXPERIENCE_BOTTLE,"Cap livello: "+plugin.classCap(id),"Clicca e scrivi il nuovo cap"));
                m.inv.setItem(12,button(Material.NAME_TAG,"Gruppo LuckPerms",s.group(id,plugin.catalog()),"Clicca per modificare"));
                m.inv.setItem(14,button(Material.NETHER_STAR,"Oggetto per entrare nella sottoclasse",s.item(id).isEmpty()?"NON CONFIGURATO":s.item(id),c.parent().isEmpty()?"Classe base: nessuna promozione in ingresso":"Da: "+c.parent()+"; livello minimo: "+c.requiredLevel()));
                m.inv.setItem(16,button(Material.IRON_SWORD,"Spade a due mani",s.weapons(id).toString(),"Attivo sulla famiglia Guerriero a due mani","Mano secondaria libera richiesta"));
            }
            m.inv.setItem(45,button(Material.ARROW,"Indietro"));
        }
        p.openInventory(m.inv);
    }
    private void later(Player p,Runnable r){plugin.getServer().getScheduler().runTask(plugin,()->{if(p.isOnline()&&p.hasPermission("castigo.classes.admin"))r.run();});}
    @EventHandler public void click(InventoryClickEvent e){
        if(!(e.getView().getTopInventory().getHolder() instanceof Menu m))return;e.setCancelled(true);
        if(!(e.getWhoClicked() instanceof Player p)||!m.owner.equals(p.getUniqueId())||!p.hasPermission("castigo.classes.admin")||e.getClick()!=ClickType.LEFT)return;
        int slot=e.getRawSlot();
        String selected=null;
        if(slot>=54&&e.getClickedInventory()==p.getInventory()&&!m.select.isEmpty()&&!m.select.equals("confirm-reset"))try{selected=SkillEquipment.identify(p.getInventory().getItem(e.getSlot()));}catch(Exception ex){p.sendMessage("§c"+ex.getMessage());return;}
        final String item=selected;
        later(p,()->{try{
            if(item!=null){plugin.progressionSettings().set(m.id,m.select,item);show(p,m.id,m.select,0);return;}
            if(slot<0||slot>=54)return;
            if(m.select.equals("confirm-reset")){
                if(slot==22){plugin.progressionSettings().set(null,"reset-token","");plugin.syncHud();p.sendMessage("§aQuota giornaliera azzerata per tutti.");show(p,null,"",0);}
                else if(slot==45)show(p,null,"",0);return;
            }
            if(m.id==null){
                var classes=new ArrayList<>(plugin.catalog().all().values());int index=m.page*36+slot;
                if(slot<36&&index<classes.size())show(p,classes.get(index).id(),"",0);
                else if(slot==38)ask(p,null,"cap");else if(slot==39)ask(p,null,"reset-time");
                else if(slot==40)show(p,"reset","confirm-reset",0);else if(slot==41)ask(p,null,"player-reset");
                else if(slot==45&&m.page>0)show(p,null,"",m.page-1);else if(slot==53)show(p,null,"",m.page+1);
            }else if(slot==45)show(p,m.select.isEmpty()?null:m.id,"",0);
            else if(!m.select.isEmpty()&&slot==31){plugin.progressionSettings().set(m.id,m.select,"");show(p,m.id,m.select,0);}
            else if(m.select.isEmpty()){
                if(slot==10)ask(p,m.id,"level-cap");else if(slot==12)ask(p,m.id,"group");
                else if(slot==14)show(p,m.id,"promotion-item",0);else if(slot==16)show(p,m.id,"two-handed-items",0);
            }
        }catch(Exception ex){p.sendMessage("§cImpostazione non salvata: "+ex.getMessage());}});
    }
    private void ask(Player p,String id,String field){inputs.put(p.getUniqueId(),new Input(id,field,System.nanoTime()+120_000_000_000L));p.closeInventory();p.sendMessage("Scrivi "+(field.equals("reset-time")?"l'orario HH:mm italiano":field.equals("player-reset")?"il nome del giocatore online":"il valore per "+field)+". annulla per tornare; 2 minuti.");}
    @EventHandler(priority=EventPriority.LOWEST) public void chat(AsyncPlayerChatEvent e){
        var in=inputs.get(e.getPlayer().getUniqueId());if(in==null)return;
        if(System.nanoTime()>in.deadline){inputs.remove(e.getPlayer().getUniqueId(),in);return;}
        e.setCancelled(true);String value=e.getMessage().strip();var p=e.getPlayer();
        later(p,()->{if(inputs.get(p.getUniqueId())!=in)return;try{
            if(!value.equalsIgnoreCase("annulla")){
                if(in.field.equals("player-reset")){Player target=Bukkit.getPlayerExact(value);if(target==null)throw new IllegalArgumentException("Giocatore non online");plugin.resetDaily(target);}
                else {if(in.field.equals("level-cap"))plugin.validateClassCap(in.id,Integer.parseInt(value));plugin.progressionSettings().set(in.id,in.field,value);if(in.field.equals("group"))for(var online:Bukkit.getOnlinePlayers())if(plugin.profile(online)!=null&&plugin.profile(online).classId.equals(in.id))plugin.syncClassGroup(online);}
                plugin.syncHud();p.sendMessage("§aImpostazione salvata.");
            }
            cancelInput(p);show(p,in.id,"",0);
        }catch(Exception ex){p.sendMessage("§c"+ex.getMessage()+". Riprova oppure annulla.");}});
    }
    @EventHandler public void drag(InventoryDragEvent e){if(e.getView().getTopInventory().getHolder() instanceof Menu)e.setCancelled(true);}
    @EventHandler public void quit(PlayerQuitEvent e){cancelInput(e.getPlayer());}
}
