package net.xtrafrancyz.vime.VimeChat;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 *
 * Created in: 14.04.2014
 * @author xtrafrancyz
 */
public final class MuteManager implements Listener{
    private final Main plugin;
    private Map<String, MuteInfo> mutedPlayers;
    private final File saveFile;
    
    private int loop = -1;
    
    public MuteManager(Main plugin){
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        saveFile = new File(plugin.getDataFolder(), "mutes");
        
        startLoop();
    }
    
    public boolean mute(String admin, String player, int time, String reason){
        Player p = plugin.getServer().getPlayer(player);
        if (p == null)
            return false;
        String timeMsg;
        if (time == 0)
            timeMsg = "всегда.";
        else
            timeMsg = time+"&e "+plurals(time, "минуту", "минуты", "минут");
        
        String printableReason = "";
        if (reason.length() > 0)
            printableReason = " &eПричина: &a"+reason+"&e.";
        
        String prefix;
        
        if (admin.equals("#antiflood")){
            prefix = "&f[&cАнтиФлуд&f] ";
        } else if (admin.equals("#console")){
            prefix = "&f[&aСервер&f] ";
        } else if (Main.usePex){
            ru.tehkode.permissions.PermissionUser user = ru.tehkode.permissions.bukkit.PermissionsEx.getPermissionManager().getUser(admin);
            ru.tehkode.permissions.PermissionGroup[] groups = user.getGroups();
            
            if (Helper.containsGroup(groups, "Admin"))
                prefix = "&f[&3"+admin+"&f] ";
            else if (Helper.containsGroup(groups, "Admins"))
                prefix = "&f[&3&l"+admin+"&f] ";
            else if (Helper.containsGroup(groups, "Moder") || Helper.containsGroup(groups, "ServerModer"))
                prefix = "&f[&b"+admin+"&f] ";
            else if (Helper.containsGroup(groups, "MainModer"))
                prefix = "&f[&b&l"+admin+"&f] ";
            else if (Helper.containsGroup(groups, "Helper") || Helper.containsGroup(groups, "PHelper"))
                prefix = "&f[&5"+admin+"&f] ";
            else
                prefix = "&f[&a"+admin+"&f] ";
            
        } else {
            prefix = "&f[&a"+admin+"&f] ";
        }
        
        if (reason.isEmpty())
            reason = null;
        mutedPlayers.put(p.getName(), new MuteInfo(reason, time == 0 ? time : System.currentTimeMillis()+time*60000, admin));
        
        plugin.getServer().broadcastMessage(ChatColor.translateAlternateColorCodes('&', prefix+"&eИгроку &a"+p.getName()+"&e запрещено писать в чат на &a"+timeMsg+printableReason));
        return true;
    }
    
    private static class Helper{
        private static boolean containsGroup(ru.tehkode.permissions.PermissionGroup[] groups, String name){
            for (ru.tehkode.permissions.PermissionGroup g :groups)
                if (g.getName().equalsIgnoreCase(name))
                    return true;
            return false;
        }
    }
    
    public boolean unMute(String player){
        if (isMuted(player)){
            mutedPlayers.remove(player);
            plugin.getServer().broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&eИгрок &a"+player+"&e снова может писать в чат"));
            return true;
        }
        return false;
    }
    
    public boolean isMuted(String player){
        return mutedPlayers.containsKey(player);
    }
    
    @EventHandler(priority = EventPriority.LOW)
    public void onChat(AsyncPlayerChatEvent event){
        if (isMuted(event.getPlayer().getName())){
            MuteInfo mi = mutedPlayers.get(event.getPlayer().getName());
            int minutes = (int)Math.ceil((mi.muteto-System.currentTimeMillis())/60000);
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED+"Вам запрещено писать в чат" + (mi.reason != null ? ". Причина: "+ChatColor.YELLOW+mi.reason : "")+ChatColor.RED+". Осталось: "+ChatColor.YELLOW+minutes+" "+plurals(minutes, "минута", "минуты", "минут"));
        }
    }
    
    public void startLoop(){
        loop = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run(){
                for (Entry<String, MuteInfo> entry: mutedPlayers.entrySet())
                    if (entry.getValue().muteto > 0 && entry.getValue().muteto < System.currentTimeMillis()){
                        mutedPlayers.remove(entry.getKey());
                        Player pl = plugin.getServer().getPlayerExact(entry.getKey());
                        if (pl != null)
                            pl.sendMessage(ChatColor.GREEN+"Вы снова можете писать в чат");
                    }
            }
        }, 200, 50);
    }
    
    public void stopLoop(){
        plugin.getServer().getScheduler().cancelTask(loop);
    }
    
    public String minutesToString(int minutes){
        return minutes+"&e "+plurals(minutes, "минута", "минуты", "минут");
    }
    
    /**
     * 
     * @param n число
     * @param form1 1 письмо, минута
     * @param form2 2 письма, минуты
     * @param form3 90 писем, минут
     * @return Правиотная фор
     */
    public static String plurals(int n, String form1, String form2, String form3){
        if (n==0) return form3;
        n = Math.abs(n) % 100;
        if (n > 10 && n < 20) return form3;
        n = n % 10;
        if (n > 1 && n < 5) return form2;
        if (n == 1) return form1;
        return form3;
    }

    public void save(){
        try{
            if (!saveFile.exists())
                saveFile.createNewFile();
            ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(saveFile)));
            oos.writeObject(mutedPlayers);
            oos.close();
        }catch(IOException e){
            plugin.getLogger().log(Level.SEVERE, null, e);
        }
    }

    public void load(){
        if (!saveFile.exists()) {
            mutedPlayers = new ConcurrentHashMap<String, MuteInfo>();
        } else {
            try {
                ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(saveFile)));
                mutedPlayers = (Map) ois.readObject();
                if (mutedPlayers instanceof HashMap){
                    ConcurrentHashMap<String, MuteInfo> newmap = new ConcurrentHashMap<String, MuteInfo>();
                    for (Map.Entry<String, MuteInfo> e: mutedPlayers.entrySet())
                        newmap.put(e.getKey(), e.getValue());
                    mutedPlayers = newmap;
                }
                ois.close();
            } catch (IOException | ClassNotFoundException ex) {
                plugin.getLogger().log(Level.SEVERE, null, ex);
                mutedPlayers = new ConcurrentHashMap<String, MuteInfo>();
            }
        }
    }
    
    public Map<String, MuteInfo> getMutes(){
        return mutedPlayers;
    }
    
    public static class MuteInfo implements Serializable{
        public String reason;
        public String admin;
        public long muteto;
        
        public MuteInfo(String reason, long muteto, String admin){
            this.reason = reason;
            this.muteto = muteto;
            this.admin = admin;
        }
    }
}
