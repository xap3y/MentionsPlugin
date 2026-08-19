package eu.xap3y.mentions.manager;

import eu.xap3y.mentions.api.iface.CommandManagerIface;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.jetbrains.annotations.NotNull;

public class CommandManager implements CommandManagerIface<CommandSender> {

    private final AnnotationParser<CommandSender> parser;

    public CommandManager(JavaPlugin instance) {
        ExecutionCoordinator<CommandSender> coordinator = ExecutionCoordinator.asyncCoordinator();
        LegacyPaperCommandManager<CommandSender> manager = new LegacyPaperCommandManager<CommandSender>(
                instance,
                coordinator,
                SenderMapper.identity()
        );
        if (manager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
            manager.registerBrigadier();
            manager.brigadierManager().setNativeNumberSuggestions(false);
        }

        parser = new AnnotationParser<>(manager, CommandSender.class);
    }

    @Override
    public void parse(@NotNull Object... instances) {
        parser.parse(instances);
    }

    @Override
    public AnnotationParser<CommandSender> getParser() {
        return parser;
    }
}