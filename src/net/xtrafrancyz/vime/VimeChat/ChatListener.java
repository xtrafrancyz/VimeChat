package net.xtrafrancyz.vime.VimeChat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 *
 * Created in: 14.04.2014
 * @author xtrafrancyz
 */
public class ChatListener implements Listener{
    private static final Pattern PATTERN_1 = Pattern.compile("([ !?])\\1+");
    private static final String BAD_REPLACER = "^_^";
    
    private final Main plugin;
    private final Map<String, PlayerInfo> players = new HashMap<>();
    
    private final Set<String> badWords = new HashSet<>();
    
    public ChatListener(Main plugin) {
        this.plugin = plugin;
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(plugin.getResource("badwords.txt"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = normalizeWord(line.trim());
                if (!line.isEmpty())
                    badWords.add(line);
            }
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "Could not read bad words");
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        final String name = event.getPlayer().getName();
        final PlayerInfo info = players.get(name);
        if (info != null) {
            info.removeTask = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                @Override
                public void run() {
                    players.remove(name);
                    info.removeTask = -1;
                }
            }, 20*60*5);
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        PlayerInfo info = players.get(event.getPlayer().getName());
        if (info != null && info.removeTask != -1) {
            Bukkit.getScheduler().cancelTask(info.removeTask);
            info.removeTask = -1;
        }
    }
    
    @EventHandler(ignoreCancelled = true)
    public void onChat(final AsyncPlayerChatEvent event){
        String message = replaceGarbage(event.getMessage());
        event.setMessage(message);
        
        final String messageToPlayeer = checkMessage(event.getPlayer().getName(), message);
        
        if (messageToPlayeer != null){
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable(){
                @Override
                public void run() {
                    event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', messageToPlayeer));
                }
            }, 1);
        }
    }
    
    private String replaceGarbage(String message) {
        // Восклицательные и вопросительные знаки не больше одного
        message = PATTERN_1.matcher(message).replaceAll("$1");
        
        // Убирание капса
        String[] words = message.split(" ");
        boolean[] caps = new boolean[words.length];
        int capsWords = 0;
        for (int i = 0; i < words.length; i++) {
            if (badWords.contains(normalizeWord(words[i]))) {
                words[i] = BAD_REPLACER;
            } else if (words[i].length() > 3 && isUpperCase(words[i])) {
                caps[i] = true;
                capsWords++;
            }
        }
        if (capsWords > 1)
            for (int i = 0; i < words.length; i++)
                if (caps[i])
                    words[i] = words[i].toLowerCase();
        return implode(words, " ");
    }
    
    public String checkMessage(final String name, String message){
        MessageInfo curr = new MessageInfo(message, System.currentTimeMillis());
        String returnMessage = null;
        
        PlayerInfo player;
        if (!players.containsKey(name)){
            player = new PlayerInfo();
            players.put(name, player);
        }else
            player = players.get(name);
        
        boolean checkSimilarity = message.length() > 4;
        int similar = 0;
        int fastMsgs = 0;
        
        while (!player.messages.isEmpty() && curr.time - player.messages.peekLast().time > 5 * 60_000)
            player.messages.removeLast();
        
        for (MessageInfo prev : player.messages) {
            long diff = curr.time - prev.time;
            if (checkSimilarity) {
                if (prev.message.length() > 4) {
                    double similarity = JaroWinkler.similarity(prev.message, curr.message);
                    double treshold = 1 - 0.02 / (0.2 + diff / (5 * 60000));
                    if (similarity >= treshold)
                        similar++;
                }
            }
            if (diff < 5000)
                fastMsgs++;
        }
        while (player.messages.size() > 7)
            player.messages.removeLast();
        player.messages.addFirst(curr);
        
        if (similar > 1 || similar == 1 && player.lastMsgIsSimilar) {
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable(){
                @Override
                public void run() {
                    plugin.mute.mute("#antiflood", name, Main.muteTimeFlood, "Флуд одинаковыми сообщениями");
                }
            }, 1);
        } else if (similar == 1) {
            player.lastMsgIsSimilar = true;
            returnMessage = "&f[&cАнтиФлуд&f]&6 Ваше сообщение идентично предыдущему. В следующий раз вам будет выдан мут.";
        } else {
            player.lastMsgIsSimilar = false;
        }
        
        if (similar == 0 && fastMsgs >= 2) {
            if (player.lastMsgIsTooFast) {
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable(){
                    @Override
                    public void run() {
                        plugin.mute.mute("#antiflood", name, Main.muteTimeFlood, "Слишком частые сообщения");
                    }
                }, 1);
                return null;
            } else {
                player.lastMsgIsTooFast = true;
                returnMessage = "&f[&cАнтиФлуд&f]&6 Полегче, зачем так часто писать в чат?";
            }
        } else {
            player.lastMsgIsTooFast = false;
        }
        
        return returnMessage;
    }
    
    public static String implode(String[] arr, String glue){
        if (arr.length == 0)
            return "";
        StringBuilder sb = new StringBuilder(arr[0]);
        for (int i=1; i<arr.length; i++)
            sb.append(glue).append(arr[i]);
        return sb.toString();
    }
    
    public static boolean isUpperCase(String str){
        for (char c: str.toCharArray())
            if (!Character.isUpperCase(c))
                return false;
        return true;
    }
    
    private static String normalizeWord(String str) {
        char[] chars = str.toCharArray();
        int len = chars.length;
        int st = 0;
        while (st < len && !Character.isAlphabetic(chars[st]))
            st++;
        while (st < len && !Character.isAlphabetic(chars[len - 1]))
            len--;
        str = ((st > 0) || (len < chars.length)) ? str.substring(st, len) : str;
        return str.toLowerCase()
            .replace('6', 'b')
            .replace('a', 'а')
            .replace('e', 'е')
            .replace('э', 'е')
            .replace('ё', 'е')
            .replace('y', 'у')
            .replace('p', 'р')
            .replace('x', 'х')
            .replace('o', 'о')
            .replace('c', 'с');
    }
    
    public static class Response{
        public String message = null;
        public String messageToPlayer = null;

        public Response(String message) {
            this.message = message;
        }
    }
    
    public static class PlayerInfo{
        public Deque<MessageInfo> messages = new ArrayDeque<>();
        public boolean lastMsgIsSimilar;
        public boolean lastMsgIsTooFast;
        public int removeTask = -1;
        
        public void limitMessages(int max){
            while (messages.size() > max)
                messages.removeFirst();
        }
    }
    
    public static class MessageInfo{
        public String message;
        public long time;

        public MessageInfo(String message, long time) {
            this.message = message;
            this.time = time;
        }
    }
}
