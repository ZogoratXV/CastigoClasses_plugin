package it.castigo.classes;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import java.util.*;

/** Inventory copies are previews: nothing can be taken from a collection or admin menu. */
public final class CodexGui implements Listener {
    private final CastigoClasses plugin;
    private enum View { CATEGORIES, COLLECTION, REWARDS, ADMIN, ENTRY, REWARD_LIST, REWARD, BOOK }
    private static final class Menu implements InventoryHolder {
        final UUID owner;final View view;final String key;final int page;final List<String> keys=new ArrayList<>();Inventory inventory;
        Menu(Player p,View view,String key,int page){owner=p.getUniqueId();this.view=view;this.key=key;this.page=page;}
        public Inventory getInventory(){return inventory;}
        boolean admin(){return view.ordinal()>=View.ADMIN.ordinal();}
    }
    private record Input(View view,String key,String field,long expires){}
    private final Map<UUID,Input> inputs=new java.util.concurrent.ConcurrentHashMap<>();
    public CodexGui(CastigoClasses plugin){this.plugin=plugin;}
    public void cancel(Player p){inputs.remove(p.getUniqueId());}
    public void closePlayers(){for(var p:Bukkit.getOnlinePlayers())if(p.getOpenInventory().getTopInventory().getHolder() instanceof Menu m&&!m.admin())p.closeInventory();}
    private static ItemStack icon(ItemStack item,String name,String... lore){var copy=item.clone();copy.setAmount(1);var meta=copy.getItemMeta();meta.setDisplayName(name);meta.setLore(List.of(lore));copy.setItemMeta(meta);return copy;}
    private static ItemStack button(Material material,String name,String... lore){return icon(new ItemStack(material),name,lore);}
    private void later(Player p,Runnable task){Bukkit.getScheduler().runTask(plugin,()->{if(p.isOnline())task.run();});}
    public void open(Player p,boolean admin){cancel(p);show(p,admin?View.ADMIN:View.CATEGORIES,null,0);}
    private void show(Player p,View view,String key,int requestedPage){
        if(view.ordinal()>=View.ADMIN.ordinal()&&!p.hasPermission("castigo.classes.admin"))return;
        if(view.ordinal()<View.ADMIN.ordinal()&&!plugin.codexSettings().enabled()){p.sendMessage("§eCodex disabilitato.");return;}
        try{
            var settings=plugin.codexSettings();var all=new ArrayList<String>();
            switch(view){
                case CATEGORIES->all.addAll(settings.categories());
                case COLLECTION->settings.entries().stream().filter(e->e.category().equals(key)).forEach(e->all.add(e.key()));
                case ADMIN->settings.entries().forEach(e->all.add(e.key()));
                case REWARDS,REWARD_LIST->settings.rewards().forEach(r->all.add(r.id()));
                default->{}
            }
            int page=Math.clamp(requestedPage,0,Math.max(0,(all.size()-1)/45));var m=new Menu(p,view,key,page);m.keys.addAll(all.subList(Math.min(all.size(),page*45),Math.min(all.size(),page*45+45)));
            m.inventory=Bukkit.createInventory(m,54,"Castigo · Codex"+(m.admin()?" · Admin":""));var inv=m.inventory;
            var found=m.admin()?Set.<String>of():plugin.codex().progress(p).found();
            for(int i=0;i<m.keys.size();i++){
                String id=m.keys.get(i);
                if(view==View.CATEGORIES){long total=settings.entries().stream().filter(e->e.category().equals(id)).count();inv.setItem(i,button(Material.BOOK,id,plugin.codex().count(p,id)+" / "+total+" registrati"));}
                else if(view==View.REWARDS||view==View.REWARD_LIST){var r=settings.reward(id);boolean claimed=!m.admin()&&plugin.codex().progress(p).claimed().contains(id);inv.setItem(i,button(Material.CHEST,r.category()+" · "+r.threshold()+" registrazioni",m.admin()?"Clic: configura ricompensa":claimed?"Già riscossa":"Clic: riscuoti se hai raggiunto il traguardo"));}
                else{var e=settings.get(id);boolean unlocked=m.admin()||found.contains(id);inv.setItem(i,icon(unlocked?e.icon():new ItemStack(Material.GRAY_DYE),e.name(),e.category(),m.admin()?e.identity():unlocked?"Registrato":"Da scoprire",m.admin()?"Clic: configura":e.kind()==CodexSettings.Kind.WEAPON?"Clic: applica aspetto all'arma in mano":"Si registra dopo l'abbattimento"));}
            }
            if(view==View.ENTRY){var e=settings.get(key);if(e==null){show(p,View.ADMIN,null,0);return;}
                inv.setItem(0,icon(e.icon(),e.name(),e.identity()));inv.setItem(10,button(Material.NAME_TAG,"Nome: "+e.name()));inv.setItem(11,button(Material.BOOK,"Categoria: "+e.category(),"Stessa categoria esatta = skin compatibili","Separa spade, staffe, asce, archi..."));inv.setItem(12,button(Material.NETHER_STAR,"Tipo: "+e.kind(),"Clic: alterna mob / boss"));
                inv.setItem(22,button(Material.PAPER,"Clicca un oggetto nel tuo inventario",e.kind()==CodexSettings.Kind.WEAPON?"Aggiorna il modello solo con lo stesso ID":"Imposta l'icona della voce"));inv.setItem(48,button(Material.BARRIER,"Elimina voce","Shift + clic destro"));
            }
            if(view==View.REWARD){var r=settings.reward(key);if(r==null){show(p,View.REWARD_LIST,null,0);return;}
                inv.setItem(10,button(Material.BOOK,"Categoria: "+r.category()));inv.setItem(11,button(Material.EXPERIENCE_BOTTLE,"Traguardo: "+r.threshold()));inv.setItem(12,button(Material.COMMAND_BLOCK,"Comando console",r.command().isEmpty()?"Nessuno":r.command(),"Clic: modifica; usa {player} o {uuid}","Scrivi - per rimuoverlo"));
                inv.setItem(22,r.item()==null?button(Material.CHEST,"Clicca un oggetto nel tuo inventario","Copia anche la quantità come ricompensa"):icon(r.item(),"Premio: "+r.item().getAmount()+" oggetti","Clic destro qui: rimuovi oggetto","Clic inventario: sostituisci"));inv.setItem(48,button(Material.BARRIER,"Elimina traguardo","Shift + clic destro"));
            }
            if(view==View.BOOK){inv.setItem(22,button(Material.BOOK,"Seleziona il Codex nel tuo inventario","Vanilla o ItemsAdder: clic destro aprirà il Codex","ID attuale: "+settings.book()));inv.setItem(48,button(Material.BARRIER,"Rimuovi associazione","Shift + clic destro"));}
            inv.setItem(45,button(Material.ARROW,"Pagina precedente"));inv.setItem(53,button(Material.ARROW,"Pagina successiva"));inv.setItem(49,button(Material.ARROW,"Indietro"));
            if(view==View.ADMIN){inv.setItem(46,button(Material.BOOK,"Oggetto Codex"));inv.setItem(47,button(Material.ZOMBIE_HEAD,"Aggiungi mob o boss"));inv.setItem(48,button(Material.CHEST,"Traguardi e ricompense"));inv.setItem(50,button(Material.IRON_SWORD,"Aggiungi arma: clicca il tuo inventario","Gli oggetti non vengono consumati"));inv.setItem(51,button(Material.LEVER,"Codex: "+(settings.enabled()?"attivo":"disattivo"),"Clic: attiva/disattiva, conservando i progressi"));}
            if(view==View.REWARD_LIST)inv.setItem(50,button(Material.EMERALD,"Nuovo traguardo"));
            if(view==View.CATEGORIES||view==View.COLLECTION){inv.setItem(47,button(Material.CHEST,"Ricompense"));inv.setItem(51,button(Material.MILK_BUCKET,"Ripristina aspetto originale","Dell'arma nella mano principale"));}
            p.openInventory(inv);
        }catch(Exception ex){p.sendMessage("§cCodex: "+ex.getMessage());}
    }
    private void ask(Player p,Menu m,String field){plugin.closeProgressionInput(p);plugin.closeHudInput(p);inputs.put(p.getUniqueId(),new Input(m.view,m.key,field,System.nanoTime()+120_000_000_000L));later(p,()->{p.closeInventory();p.sendMessage("§eCodex · "+field+": scrivi in chat; annulla per uscire."+(field.equals("mob")?" ID: minecraft:zombie oppure mythic:NomeInterno":""));});}
    private static String weaponCategory(String identity,Material material){if(!identity.startsWith("minecraft:"))return "da_configurare";String name=material.name();if(name.endsWith("_SWORD"))return "spade";if(name.endsWith("_AXE"))return "asce";return switch(material){case BOW->"archi";case CROSSBOW->"balestre";case TRIDENT->"tridenti";case MACE->"mazze";default->"da_configurare";};}
    @EventHandler public void click(InventoryClickEvent e){
        if(!(e.getView().getTopInventory().getHolder() instanceof Menu m))return;e.setCancelled(true);
        if(!(e.getWhoClicked() instanceof Player p)||!m.owner.equals(p.getUniqueId())||m.admin()&&!p.hasPermission("castigo.classes.admin"))return;
        if(!m.admin()&&!plugin.codexSettings().enabled()){later(p,p::closeInventory);return;}
        int slot=e.getRawSlot();var s=plugin.codexSettings();
        try{
            if(slot==48&&m.admin()&&e.getClick()==ClickType.SHIFT_RIGHT){if(m.view==View.ENTRY)s.remove(m.key);else if(m.view==View.REWARD)s.removeReward(m.key);else if(m.view==View.BOOK)s.book("");later(p,()->show(p,View.ADMIN,null,0));return;}
            if(e.getClick()!=ClickType.LEFT&&e.getClick()!=ClickType.RIGHT)return;
            if(slot>=54&&e.getClickedInventory()==p.getInventory()&&m.admin()){
                var item=p.getInventory().getItem(e.getSlot());if(item==null||item.getType().isAir())return;
                if(m.view==View.BOOK)s.book(SkillEquipment.identify(item));
                else if(m.view==View.ADMIN){var original=WeaponSkins.restore(item);String id=SkillEquipment.identify(original);var existing=s.identity(id);if(existing!=null){later(p,()->show(p,View.ENTRY,existing.key(),0));return;}var entry=new CodexSettings.Entry(CodexSettings.Kind.WEAPON,id,id,weaponCategory(id,item.getType()),original);s.put(entry);later(p,()->show(p,View.ENTRY,entry.key(),0));return;}
                else if(m.view==View.ENTRY){var entry=s.get(m.key);if(entry==null)return;if(entry.kind()==CodexSettings.Kind.WEAPON&&!entry.identity().equals(SkillEquipment.identify(item)))throw new IllegalArgumentException("Seleziona lo stesso ID arma");s.put(new CodexSettings.Entry(entry.kind(),entry.identity(),entry.name(),entry.category(),WeaponSkins.restore(item)));}
                else if(m.view==View.REWARD){var r=s.reward(m.key);if(r!=null)s.put(new CodexSettings.Reward(r.id(),r.category(),r.threshold(),item,r.command()));}
                later(p,()->show(p,m.view,m.key,m.page));return;
            }
            if(slot==45||slot==53){later(p,()->show(p,m.view,m.key,m.page+(slot==45?-1:1)));return;}
            if(slot==49){later(p,()->show(p,m.admin()?View.ADMIN:View.CATEGORIES,null,0));return;}
            if(slot>=0&&slot<m.keys.size()){
                String key=m.keys.get(slot);switch(m.view){
                    case CATEGORIES->later(p,()->show(p,View.COLLECTION,key,0));
                    case ADMIN->later(p,()->show(p,View.ENTRY,key,0));
                    case REWARD_LIST->later(p,()->show(p,View.REWARD,key,0));
                    case REWARDS->{plugin.codex().claim(p,key);later(p,()->show(p,m.view,m.key,m.page));}
                    case COLLECTION->{var entry=s.get(key);if(entry!=null&&entry.kind()==CodexSettings.Kind.WEAPON){plugin.codex().skin(p,key);p.sendMessage("§aAspetto applicato.");}}
                    default->{}
                }return;
            }
            switch(m.view){
                case ADMIN->{if(slot==46)later(p,()->show(p,View.BOOK,null,0));if(slot==47)ask(p,m,"mob");if(slot==48)later(p,()->show(p,View.REWARD_LIST,null,0));if(slot==51){s.enabled(!s.enabled());closePlayers();later(p,()->show(p,m.view,null,0));}}
                case ENTRY->{var entry=s.get(m.key);if(entry==null)return;if(slot==10)ask(p,m,"nome");if(slot==11)ask(p,m,"categoria");if(slot==12&&entry.kind()!=CodexSettings.Kind.WEAPON){var next=new CodexSettings.Entry(entry.kind()==CodexSettings.Kind.MOB?CodexSettings.Kind.BOSS:CodexSettings.Kind.MOB,entry.identity(),entry.name(),entry.category(),entry.icon());s.put(next);later(p,()->show(p,View.ENTRY,next.key(),0));}}
                case REWARD_LIST->{if(slot==50){var r=new CodexSettings.Reward(UUID.randomUUID().toString(),"mob",5,null,"");s.put(r);later(p,()->show(p,View.REWARD,r.id(),0));}}
                case REWARD->{if(slot==10)ask(p,m,"categoria");if(slot==11)ask(p,m,"traguardo");if(slot==12)ask(p,m,"comando");if(slot==22&&e.getClick()==ClickType.RIGHT){var r=s.reward(m.key);if(r!=null)s.put(new CodexSettings.Reward(r.id(),r.category(),r.threshold(),null,r.command()));later(p,()->show(p,m.view,m.key,0));}}
                case CATEGORIES,COLLECTION->{if(slot==47)later(p,()->show(p,View.REWARDS,null,0));if(slot==51){plugin.codex().restore(p);p.sendMessage("§aAspetto originale ripristinato.");}}
                default->{}
            }
        }catch(Exception ex){p.sendMessage("§cCodex: "+ex.getMessage());}
    }
    @EventHandler public void drag(InventoryDragEvent e){if(e.getView().getTopInventory().getHolder() instanceof Menu)e.setCancelled(true);}
    @EventHandler(priority=EventPriority.LOWEST) public void chat(AsyncPlayerChatEvent e){
        var input=inputs.remove(e.getPlayer().getUniqueId());if(input==null)return;e.setCancelled(true);var p=e.getPlayer();String text=e.getMessage().trim();
        later(p,()->{if(!p.hasPermission("castigo.classes.admin")||System.nanoTime()>input.expires())return;String key=input.key();try{
            if(!text.equalsIgnoreCase("annulla")){var s=plugin.codexSettings();
                if(input.field().equals("mob")){var entry=new CodexSettings.Entry(CodexSettings.Kind.MOB,text,text,"mob",new ItemStack(Material.ZOMBIE_HEAD));if(s.identity(text)!=null)throw new IllegalArgumentException("ID già presente");s.put(entry);key=entry.key();}
                else if(input.view()==View.ENTRY){var entry=s.get(key);if(entry==null)throw new IllegalArgumentException("Voce rimossa");s.put(input.field().equals("nome")?entry.name(text):entry.category(text));}
                else if(input.view()==View.REWARD){var r=s.reward(key);if(r==null)throw new IllegalArgumentException("Traguardo rimosso");s.put(new CodexSettings.Reward(r.id(),input.field().equals("categoria")?text:r.category(),input.field().equals("traguardo")?Integer.parseInt(text):r.threshold(),r.item(),input.field().equals("comando")?(text.equals("-")?"":text):r.command()));}
            }
        }catch(Exception ex){p.sendMessage("§cNon salvato: "+ex.getMessage());}show(p,input.field().equals("mob")&&key!=null?View.ENTRY:input.view(),key,0);});
    }
    @EventHandler public void quit(PlayerQuitEvent e){cancel(e.getPlayer());}
}
