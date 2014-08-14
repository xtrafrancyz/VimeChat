package net.xtrafrancyz.vime.VimeChat;

import java.util.LinkedList;
import java.util.Map;
import static net.xtrafrancyz.vime.VimeChat.MuteManager.plurals;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * Project: VimeChat
 * Created in: 14.04.2014
 * @author xtrafrancyz
 */
public class Main extends JavaPlugin{
    public static final int muteTimeFlood = 30;
    public static final int muteTimeCaps = 10;
    
    public MuteManager mute;
    
    public static boolean usePex;
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
        usePex = getConfig().getBoolean("usePex", true);
        
        mute = new MuteManager(this);
        mute.load();
        
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        
        Commands cmds = new Commands();
        getCommand("mute").setExecutor(cmds);
        getCommand("unmute").setExecutor(cmds);
        getCommand("mutelist").setExecutor(cmds);
    }

    @Override
    public void onDisable() {
        mute.stopLoop();
        mute.save();
    }
    
    public class Commands implements CommandExecutor{

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            String senderName;
            if (sender instanceof ConsoleCommandSender){
                senderName = "#console";
            }else{
                senderName = sender.getName();
            }

            System.out.println(command.getName());

            //Мут
            if (command.getName().equalsIgnoreCase("mute")){
                if (args.length == 0){
                    sender.sendMessage(ChatColor.RED+"Использование: /mute <ник> [время в минутах] [причина]");
                }else{
                    int time = 0;
                    String reason = "";
                    int i = 1;
                    if (args.length > 1)
                        try{
                            time = Integer.parseInt(args[1]);
                            i = 2;
                        }catch(NumberFormatException e){
                            time = 0;
                        }
                    for (; i<args.length; i++)
                        reason += args[i] + (i==args.length-1 ? "" : " ");

                    if (!mute.mute(senderName, args[0], time, reason))
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&eТакого игрока не существует"));
                }
            }else

            //Размут
            if (command.getName().equalsIgnoreCase("unmute")){
                if (args.length == 0){
                    sender.sendMessage(ChatColor.RED+"Использование: /unmute <ник>");
                }else{
                    Player player = getServer().getPlayer(args[0]);
                    if (player == null){
                        if (!mute.unMute(args[0]))
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&eИгрок &a"+args[0]+"&e не был замучен"));
                    }else{
                        if (!mute.unMute(player.getName()))
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&eИгрок &a"+player.getName()+"&e не был замучен"));
                    }
                }
            }else

            //Списочегг
            if (command.getName().equalsIgnoreCase("mutelist")){
                if (mute.getMutes().size() > 0){
                    LinkedList<String> list = new LinkedList<String>();
                    for (Map.Entry<String, MuteManager.MuteInfo> entry: mute.getMutes().entrySet()){
                        MuteManager.MuteInfo mi = entry.getValue();
                        String remaining;
                        if (mi.muteto > 0){
                            int minutes = (int)Math.ceil((mi.muteto-System.currentTimeMillis())/60000);
                            remaining = minutes+" "+plurals(minutes, "минута", "минуты", "минут");
                        }else{
                            remaining = "Вечный мут";
                        }
                        list.addLast(ChatColor.translateAlternateColorCodes('&', "&a"+entry.getKey()+"&e Осталось: &a"+remaining+"&e. Замутил: &a"+mi.admin+"&e. Причина: &a"+(mi.reason != null ? mi.reason : "Не указана")));
                    }
                    sender.sendMessage(list.toArray(new String[0]));
                }else{
                    sender.sendMessage(ChatColor.GREEN+"Список мутов пуст");
                }
            }

            return true;
        }
        
    }
}
