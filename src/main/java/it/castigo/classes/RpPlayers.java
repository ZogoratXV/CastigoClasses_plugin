package it.castigo.classes;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import it.castigo.core.PlayerNames;

/** Resolve the core's persistent RP identity, never a Bukkit display-name substring. */
final class RpPlayers {
    static Player online(String input){
        String name=input.trim();
        if(name.length()>1&&name.startsWith("\"")&&name.endsWith("\""))name=name.substring(1,name.length()-1);
        Player match=null;
        for(Player p:Bukkit.getOnlinePlayers())if(PlayerNames.name(p).equalsIgnoreCase(name)){
            if(match!=null)throw new IllegalArgumentException("Nome RP ambiguo: usa l'UUID del giocatore.");
            match=p;
        }
        if(match!=null)return match;
        var resolved=PlayerNames.uuid(name);
        if(resolved.isPresent())return Bukkit.getPlayer(resolved.get());
        return Bukkit.getPlayerExact(name); // Compatibility for console scripts and unregistered RP names.
    }
    private RpPlayers(){}
}
