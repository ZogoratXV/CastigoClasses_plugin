package it.castigo.classes;
import java.nio.file.*;
import org.bukkit.configuration.file.YamlConfiguration;
final class AtomicConfig {
    private AtomicConfig(){}
    static void save(Path file,YamlConfiguration data)throws Exception{
        Files.createDirectories(file.getParent());Path temp=Files.createTempFile(file.getParent(),"config-",".tmp");
        try{Files.writeString(temp,data.saveToString());try{Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(temp);}
    }
}
