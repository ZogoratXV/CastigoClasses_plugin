package it.castigo.classes;

import it.castigo.classes.model.StatAttribute;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import java.util.*;

/** Selection copies the item identity; numeric changes are entered in chat. */
public final class GearAdminGui implements Listener {
    private final CastigoClasses plugin;
    private record Input(String id,String field,long expires) {}
    private final Map<UUID,Input> inputs=new java.util.concurrent.ConcurrentHashMap<>();
    private static final class Menu implements InventoryHolder {
        final UUID owner;final String id;final int page;Inventory inventory;
        Menu(Player p,String id,int page){owner=p.getUniqueId();this.id=id;this.page=page;}
        public Inventory getInventory(){return inventory;}
    }
    public GearAdminGui(CastigoClasses plugin){this.plugin=plugin;}
    public void cancel(Player p){inputs.remove(p.getUniqueId());}
    private static ItemStack button(Material material,String title,String... lore){var item=new ItemStack(material);var meta=item.getItemMeta();meta.setDisplayName(title);meta.setLore(List.of(lore));item.setItemMeta(meta);return item;}
    public void open(Player p){cancel(p);plugin.closeProgressionInput(p);plugin.closeHudInput(p);show(p,null,0);}
    private void show(Player p,String id,int page){
        if(!p.hasPermission("castigo.classes.admin"))return;
        var all=plugin.gearSettings().all();page=Math.clamp(page,0,Math.max(0,(all.size()-1)/45));
        var menu=new Menu(p,id,page);menu.inventory=Bukkit.createInventory(menu,54,"Castigo · Equipaggiamento");var inv=menu.inventory;
        if(id==null){
            for(int i=0;i<45&&page*45+i<all.size();i++){var r=all.get(page*45+i);inv.setItem(i,button(r.template().getType(),r.id(),"Livello minimo: "+r.level(),"Slot: "+r.slot(),"Clic: configura"));}
            inv.setItem(45,button(Material.ARROW,"Pagina precedente"));inv.setItem(49,button(Material.BOOK,"Seleziona un oggetto nel tuo inventario","Vanilla o ItemsAdder; non viene consumato","La regola vale per tutti gli oggetti con lo stesso ID"));inv.setItem(53,button(Material.ARROW,"Pagina successiva"));
        }else{
            var r=plugin.gearSettings().get(id);if(r==null){show(p,null,page);return;}
            inv.setItem(0,button(r.template().getType(),id));
            inv.setItem(1,button(Material.EXPERIENCE_BOTTLE,"Livello minimo: "+r.level(),"Clic: inserisci un numero da 1 a 1000"));
            inv.setItem(2,button(Material.HOPPER,"Slot attivo: "+r.slot(),"Clic: cambia slot","Bonus attivi solo se equipaggiato in questo slot"));
            inv.setItem(3,button(Material.IRON_CHESTPLATE,"Protezione vanilla aggiuntiva: "+r.armor(),"Punti armatura, non percentuale","Si sommano alla protezione originale del pezzo","Solo negli slot armatura; 2 punti = 1 icona"));
            inv.setItem(4,button(Material.SHIELD,"Protezione scudo: "+r.shield(),"Da 0 a 100%; -1 mantiene la parata originale","Richiede uno scudo realmente in parata"));
            var tier=plugin.rarities().tier(id);var style=plugin.rarities().style(tier);
            inv.setItem(5,button(Material.DIAMOND,"Rarità: "+tier.label,"Clic: cambia rarità di questo ID","Gli oggetti generati conservano la loro rarità"));
            inv.setItem(6,button(Material.PAINTING,"Colore: "+style.color(),"Clic: cambia colore della rarità selezionata","Vale per tutti gli oggetti di questa rarità"));
            inv.setItem(7,button(Material.IRON_SWORD,"Danno base: +"+style.damage()+"%","Clic: percentuale della rarità selezionata"));
            inv.setItem(8,button(Material.IRON_CHESTPLATE,"Armatura base: +"+style.armor()+"%","Clic: percentuale della rarità selezionata"));
            var stats=StatAttribute.values();for(int i=0;i<stats.length;i++){var s=stats[i];inv.setItem(9+i,button(Material.PAPER,s.label()+" · bonus fisso: "+r.bonus().getOrDefault(s,0d),"Clic: modifica bonus"));inv.setItem(18+i,button(Material.REDSTONE,s.label()+" · casuale minimo: "+r.min().getOrDefault(s,0d),"Si aggiunge al bonus fisso"));inv.setItem(27+i,button(Material.GLOWSTONE_DUST,s.label()+" · casuale massimo: "+r.max().getOrDefault(s,0d),"Aumenta il massimo prima del minimo"));}
            inv.setItem(45,button(Material.ARROW,"Indietro"));inv.setItem(48,button(Material.BARRIER,"Rimuovi regola","Shift + clic destro per confermare","I bonus di questo ID diventeranno inattivi"));inv.setItem(50,button(Material.NETHER_STAR,"Genera un oggetto casuale","Crea 1 copia con valori salvati sull'oggetto","Non rilancia i valori degli oggetti già esistenti"));
        }
        p.openInventory(inv);
    }
    private void later(Player p,Runnable action){Bukkit.getScheduler().runTask(plugin,()->{if(p.isOnline())action.run();});}
    private void ask(Player p,String id,String field){plugin.closeProgressionInput(p);plugin.closeHudInput(p);inputs.put(p.getUniqueId(),new Input(id,field,System.nanoTime()+120_000_000_000L));later(p,()->{p.closeInventory();p.sendMessage("§e"+field+": scrivi il valore in chat (annulla per uscire). Per la parata: -1 originale, 0–100 percentuale.");});}
    @EventHandler public void click(InventoryClickEvent e){
        if(!(e.getView().getTopInventory().getHolder() instanceof Menu m))return;e.setCancelled(true);
        if(!(e.getWhoClicked() instanceof Player p)||!p.getUniqueId().equals(m.owner)||!p.hasPermission("castigo.classes.admin"))return;
        int slot=e.getRawSlot();
        try{
            if(m.id!=null&&slot==48&&e.getClick()==ClickType.SHIFT_RIGHT){plugin.gearSettings().remove(m.id);plugin.gear().invalidate();later(p,()->show(p,null,m.page));return;}
            if(e.getClick()!=ClickType.LEFT&&e.getClick()!=ClickType.RIGHT)return;
            if(m.id==null){
                if(slot>=54&&e.getClickedInventory()==p.getInventory()){var item=p.getInventory().getItem(e.getSlot());plugin.gear().register(item);String id=SkillEquipment.identify(item);later(p,()->show(p,id,m.page));}
                else if(slot>=0&&slot<45){var all=plugin.gearSettings().all();int index=m.page*45+slot;if(index<all.size())later(p,()->show(p,all.get(index).id(),m.page));}
                else if(slot==45||slot==53)later(p,()->show(p,null,m.page+(slot==45?-1:1)));
                return;
            }
            var r=plugin.gearSettings().get(m.id);if(r==null)return;
            if(slot==45){later(p,()->show(p,null,m.page));return;}
            if(slot==50){if(p.getInventory().firstEmpty()<0)throw new IllegalArgumentException("Libera uno slot nell'inventario");var item=plugin.gear().generate(r,java.util.concurrent.ThreadLocalRandom.current());p.getInventory().addItem(item);p.sendMessage("§aOggetto casuale generato.");return;}
            if(slot==2){plugin.gearSettings().put(r.slot(GearSettings.SLOTS.get((GearSettings.SLOTS.indexOf(r.slot())+1)%GearSettings.SLOTS.size())));plugin.gear().invalidate();later(p,()->show(p,m.id,m.page));return;}
            if(slot==5||slot==6){var tier=plugin.rarities().tier(m.id);if(slot==5)plugin.rarities().set(m.id,RaritySettings.Tier.values()[(tier.ordinal()+1)%4]);else{var colors=new ArrayList<>(net.kyori.adventure.text.format.NamedTextColor.NAMES.keys());Collections.sort(colors);var style=plugin.rarities().style(tier);plugin.rarities().set(tier,new RaritySettings.Style(colors.get((colors.indexOf(style.color())+1)%colors.size()),style.damage(),style.armor()));}plugin.gear().invalidate();later(p,()->show(p,m.id,m.page));return;}
            String field=switch(slot){case 1->"level";case 3->"armor";case 4->"shield";case 7->"rarity.damage";case 8->"rarity.armor";default->null;};
            for(int i=0;i<7;i++){String id=StatAttribute.values()[i].id();if(slot==9+i)field="bonus."+id;if(slot==18+i)field="min."+id;if(slot==27+i)field="max."+id;}
            if(field!=null)ask(p,m.id,field);
        }catch(Exception ex){p.sendMessage("§c"+ex.getMessage());}
    }
    @EventHandler public void drag(InventoryDragEvent e){if(e.getView().getTopInventory().getHolder() instanceof Menu)e.setCancelled(true);}
    @EventHandler(priority=EventPriority.LOWEST) public void chat(AsyncPlayerChatEvent e){
        var input=inputs.remove(e.getPlayer().getUniqueId());if(input==null)return;e.setCancelled(true);String text=e.getMessage();Player p=e.getPlayer();
        later(p,()->{if(!p.hasPermission("castigo.classes.admin")||System.nanoTime()>input.expires())return;if(text.equalsIgnoreCase("annulla")){show(p,input.id(),0);return;}
            try{var rule=plugin.gearSettings().get(input.id());if(rule==null)throw new IllegalArgumentException("Oggetto rimosso");double value=Double.parseDouble(text.replace(',','.'));if(input.field().startsWith("rarity.")){var tier=plugin.rarities().tier(input.id());var style=plugin.rarities().style(tier);plugin.rarities().set(tier,new RaritySettings.Style(style.color(),input.field().equals("rarity.damage")?value:style.damage(),input.field().equals("rarity.armor")?value:style.armor()));}else plugin.gearSettings().put(rule.field(input.field(),value));plugin.gear().invalidate();p.sendMessage("§aConfigurazione salvata.");}catch(Exception ex){p.sendMessage("§cValore non salvato: "+ex.getMessage());}show(p,input.id(),0);
        });
    }
    @EventHandler public void quit(PlayerQuitEvent e){cancel(e.getPlayer());}
}
