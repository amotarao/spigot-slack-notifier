package dev.amotarao.spigot.slacknotifier;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.logging.Level;

import javax.net.ssl.HttpsURLConnection;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class SlackNotifier extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        config.addDefault("url", "");
        saveConfig();

        getServer().getPluginManager().registerEvents(this, this);
        getCommand("slack-notifier").setExecutor(new SlackNotifierCommand());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        String name = e.getPlayer().getName();
        sendMessage("joined", name);
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent e) {
        String name = e.getPlayer().getName();
        sendMessage("left", name);
    }

    private void sendMessage(String type, String name) {
        HttpsURLConnection con = null;
        StringBuffer result = new StringBuffer();
        String JSON = "{\"type\":\"" + type + "\",\"name\":\"" + name + "\"}";

        FileConfiguration config = getConfig();
        String urlAddress = config.getString("url", "");

        if (urlAddress == "") {
            getLogger().log(Level.WARNING, "Invalid URL");
            return;
        }

        try {
            URL url = new URL(urlAddress);
            con = (HttpsURLConnection) url.openConnection();
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/JSON; charset=utf-8");
            con.setRequestProperty("Content-Length", String.valueOf(JSON.length()));

            OutputStreamWriter out = new OutputStreamWriter(con.getOutputStream());
            out.write(JSON);
            out.flush();
            con.connect();

            final int status = con.getResponseCode();
            if (status == HttpsURLConnection.HTTP_OK) {
                final InputStream in = con.getInputStream();
                String encoding = con.getContentEncoding();
                if (null == encoding) {
                    encoding = "UTF-8";
                }
                final InputStreamReader inReader = new InputStreamReader(in, encoding);
                final BufferedReader bufReader = new BufferedReader(inReader);
                String line = null;
                while ((line = bufReader.readLine()) != null) {
                    result.append(line);
                }
                bufReader.close();
                inReader.close();
                in.close();
            } else {
                System.out.println(status);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }

    public class SlackNotifierCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            FileConfiguration config = getConfig();
            String url = config.getString("url", "");

            switch (args[0]) {
                case "get": {
                    sender.sendMessage(url);
                    return true;
                }

                case "set": {
                    String newUrl = args[1];
                    if (newUrl == "" || args.length < 2) {
                        return false;
                    }
                    config.set("url", args[1]);
                    saveConfig();
                    sender.sendMessage("URL is set.");
                    return true;
                }

                default: {
                    return false;
                }
            }
        }
    }
}
