package it.castigo.classes;

import it.castigo.core.Reflect;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.lang.reflect.Array;
import java.util.logging.Logger;

/** Preserve the old INTERACT policy: build permission is not a spell permission. */
public final class AbilityProtection {
    private final Logger logger;
    private long nextWarning;
    public AbilityProtection(Logger logger) { this.logger=logger; }
    public boolean interact(Player player,Location location) {
        return flag(player,location,"INTERACT");
    }
    public boolean pvp(Player player,Location location) { return flag(player,location,"PVP"); }
    private boolean flag(Player player,Location location,String flag) {
        var wg=Bukkit.getPluginManager().getPlugin("WorldGuard");
        if(wg==null||!wg.isEnabled())return true;
        try {
            Object instance=Reflect.call(Reflect.type("com.sk89q.worldguard.WorldGuard"),"getInstance");
            Object platform=Reflect.call(instance,"getPlatform");
            Object actor=Reflect.call(wg,"wrapPlayer",player);
            Class<?> adapter=Reflect.type("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Object world=Reflect.call(adapter,"adapt",location.getWorld());
            if(Boolean.TRUE.equals(Reflect.call(Reflect.call(platform,"getSessionManager"),"hasBypass",actor,world)))return true;
            Object query=Reflect.call(Reflect.call(platform,"getRegionContainer"),"createQuery");
            Object flags=Array.newInstance(Reflect.type("com.sk89q.worldguard.protection.flags.StateFlag"),1);
            Array.set(flags,0,Reflect.type("com.sk89q.worldguard.protection.flags.Flags").getField(flag).get(null));
            return Boolean.TRUE.equals(Reflect.call(query,"testState",Reflect.call(adapter,"adapt",location),actor,flags));
        } catch(ReflectiveOperationException|RuntimeException|LinkageError ex) {
            if(System.currentTimeMillis()>=nextWarning) {
                nextWarning=System.currentTimeMillis()+60000;
                logger.warning("Verifica WorldGuard non disponibile: abilità bloccata. "+ex.getClass().getSimpleName()+": "+ex.getMessage());
            }
            return false;
        }
    }
}
