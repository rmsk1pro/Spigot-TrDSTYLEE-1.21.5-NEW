package org.bukkit.command.defaults;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

public class VersionCommand extends BukkitCommand {
    public VersionCommand(@NotNull String name) {
        super(name);

        this.description = "Gets the version of this server including any plugins in use";
        this.usageMessage = "/version [plugin name]";
        this.setPermission("bukkit.command.version");
        this.setAliases(Arrays.asList("ver", "versao"));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String currentAlias, @NotNull String[] args) {
        if (!testPermission(sender)) return true;

        if (args.length == 0) {
            sender.sendMessage(" §aEste servidor está executando com spigot 1.21.5 - §fTrDSTYLEE");
            sender.sendMessage(" §a(Implementando a versão da API spigot 1.21.5  - §fTrDSTYLEE");
            sendVersion(sender);
        } else {
            StringBuilder name = new StringBuilder();

            for (String arg : args) {
                if (name.length() > 0) {
                    name.append(' ');
                }

                name.append(arg);
            }

            String pluginName = name.toString();
            Plugin exactPlugin = Bukkit.getPluginManager().getPlugin(pluginName);
            if (exactPlugin != null) {
                describeToSender(exactPlugin, sender);
                return true;
            }

            boolean found = false;
            pluginName = pluginName.toLowerCase(Locale.ROOT);
            for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
                if (plugin.getName().toLowerCase(Locale.ROOT).contains(pluginName)) {
                    describeToSender(plugin, sender);
                    found = true;
                }
            }

            if (!found) {
                sender.sendMessage(" §aEste servidor não está executando nenhum plugin com esse nome.");
                sender.sendMessage(" §aUse /plugins para ver a lista de plugins.");
            }
        }
        return true;
    }

    private void describeToSender(@NotNull Plugin plugin, @NotNull CommandSender sender) {
        PluginDescriptionFile desc = plugin.getDescription();
        sender.sendMessage(ChatColor.GREEN + desc.getName() + ChatColor.WHITE + " version " + ChatColor.GREEN + desc.getVersion());

        if (desc.getDescription() != null) {
            sender.sendMessage(desc.getDescription());
        }

        if (desc.getWebsite() != null) {
            sender.sendMessage("Website: " + ChatColor.GREEN + desc.getWebsite());
        }

        if (!desc.getAuthors().isEmpty()) {
            if (desc.getAuthors().size() == 1) {
                sender.sendMessage("Author: editado por TrDSTYLEE: ");
            } else {
                sender.sendMessage("Author: editado por TrDSTYLEE:  ");
            }
        }

        if (!desc.getContributors().isEmpty()) {
            sender.sendMessage("Contributors: " + getNameList(desc.getContributors()));
        }
    }

    @NotNull
    private String getNameList(@NotNull final List<String> names) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < names.size(); i++) {
            if (result.length() > 0) {
                result.append(ChatColor.WHITE);

                if (i < names.size() - 1) {
                    result.append(", ");
                } else {
                    result.append(" and ");
                }
            }

            result.append(ChatColor.GREEN);
            result.append(names.get(i));
        }

        return result.toString();
    }

    @NotNull
    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        Preconditions.checkArgument(sender != null, "Sender cannot be null");
        Preconditions.checkArgument(args != null, "Arguments cannot be null");
        Preconditions.checkArgument(alias != null, "Alias cannot be null");

        if (args.length == 1) {
            List<String> completions = new ArrayList<String>();
            String toComplete = args[0].toLowerCase(Locale.ROOT);
            for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
                if (StringUtil.startsWithIgnoreCase(plugin.getName(), toComplete)) {
                    completions.add(plugin.getName());
                }
            }
            return completions;
        }
        return ImmutableList.of();
    }

    private final ReentrantLock versionLock = new ReentrantLock();
    private boolean hasVersion = false;
    private String versionMessage = null;
    private final Set<CommandSender> versionWaiters = new HashSet<CommandSender>();
    private boolean versionTaskStarted = false;
    private long lastCheck = 0;

    private void sendVersion(@NotNull CommandSender sender) {
        if (hasVersion) {
            if (System.currentTimeMillis() - lastCheck > 21600000) {
                lastCheck = System.currentTimeMillis();
                hasVersion = false;
            } else {
                sender.sendMessage(versionMessage);
                return;
            }
        }
        versionLock.lock();
        try {
            if (hasVersion) {
                sender.sendMessage(versionMessage);
                return;
            }
            versionWaiters.add(sender);
            sender.sendMessage("");
            sender.sendMessage(" §aVerificando a versão, por favor aguarde ...");
            sender.sendMessage("");
            if (!versionTaskStarted) {
                versionTaskStarted = true;
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        obtainVersion();
                    }
                }).start();
            }
        } finally {
            versionLock.unlock();
        }
    }

    private void obtainVersion() {
        String version = Bukkit.getVersion();
        if (version == null) version = "Custom";
        if (version.startsWith("git-Spigot-TrDSTYLEE")) {
            String[] parts = version.substring("git-Spigot-TrDSTYLEE".length()).split("-");
            int cbVersions = getDistance("craftbukkit", parts[1].substring(0, parts[1].indexOf(' ')));
            int spigotVersions = getDistance("spigot", parts[0]);
            if (cbVersions == -1 || spigotVersions == -1) {
                setVersionMessage("Erro ao obter informações sobre a versão");
            } else {
                if (cbVersions == 0 && spigotVersions == 0) {
                    setVersionMessage(" §aVocê está executando a versão mais recente");
                } else {
                    setVersionMessage("Você está " + cbVersions + " versão(ões) desatualizado(s)");
                }
            }

        } else if (version.startsWith("git-Bukkit-TrDSTYLEE")) {
            version = version.substring("git-Bukkit-TrDSTYLEE".length());
            int cbVersions = getDistance("craftbukkit", version.substring(0, version.indexOf(' ')));
            if (cbVersions == -1) {
                setVersionMessage("Erro ao obter informações sobre a versão");
            } else {
                if (cbVersions == 0) {
                    setVersionMessage("Você está executando a versão mais recente");
                } else {
                    setVersionMessage("Você está " + cbVersions + " versão(ões) desatualizado(s)");
                }
            }
        } else {
            setVersionMessage(" §cVersão exclusiva §fTrDSTYLEE -§b§l TrCraft NetWork");
        }
    }

    private void setVersionMessage(@NotNull String msg) {
        lastCheck = System.currentTimeMillis();
        versionMessage = msg;
        versionLock.lock();
        try {
            hasVersion = true;
            versionTaskStarted = false;
            for (CommandSender sender : versionWaiters) {
                sender.sendMessage(versionMessage);
            }
            versionWaiters.clear();
        } finally {
            versionLock.unlock();
        }
    }

    private static int getDistance(@NotNull String repo, @NotNull String hash) {
        try {
            BufferedReader reader = Resources.asCharSource(
                    new URL("https://hub.spigotmc.org/stash/rest/api/1.0/projects/SPIGOT/repos/" + repo + "/commits?since=" + URLEncoder.encode(hash, "UTF-8") + "&withCounts=true"),
                    Charsets.UTF_8
            ).openBufferedStream();
            try {
                JsonObject obj = new Gson().fromJson(reader, JsonObject.class);
                return obj.get("totalCount").getAsInt();
            } catch (JsonSyntaxException ex) {
                ex.printStackTrace();
                return -1;
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }
}
