package cn.jeyor1337.zanticheat.bootstrap;

import cn.jeyor1337.zanticheat.Main;
import cn.jeyor1337.zanticheat.check.Check;
import com.kireiko.utils.registry.ServiceRegistry;
import com.kireiko.utils.registry.ServiceRegistryImpl;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public final class PluginContext {

    private final Main plugin;
    private final ServiceRegistry services;

    public PluginContext(Main plugin) {
        this.plugin = plugin;
        this.services = new ServiceRegistryImpl();
    }

    public Main getPlugin() {
        return plugin;
    }

    public <T> void putService(Class<T> type, T service) {
        services.put(type, service);
    }

    public <T> T getService(Class<T> type) {
        return services.getOrNull(type);
    }

    public void registerListener(Listener listener) {
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }

    public void unregisterListener(Listener listener) {
        HandlerList.unregisterAll(listener);
    }

    public void registerCommand(String commandName, TabExecutor executor) {
        PluginCommand command = plugin.getCommand(commandName);
        if (command == null) {
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    public void registerCheck(Check check) {
        if (!(check instanceof Listener)) {
            throw new IllegalArgumentException(check.getClass().getName() + " must implement Listener");
        }
        Check.registerListener(check.getCheckSetting().name, (Listener) check);
    }
}
