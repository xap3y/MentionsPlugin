package eu.xap3y.mentions.manager;

import eu.xap3y.mentions.api.iface.CommandManagerIface;
import org.bukkit.plugin.java.JavaPlugin;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.paper.util.sender.PaperSimpleSenderMapper;
import org.incendo.cloud.paper.util.sender.Source;
import org.jetbrains.annotations.NotNull;

public class CommandManager implements CommandManagerIface<Source> {

    private final AnnotationParser<Source> parser;

    public CommandManager(JavaPlugin instance) {
        PaperCommandManager<Source> manager = PaperCommandManager.builder(PaperSimpleSenderMapper.simpleSenderMapper())
                .executionCoordinator(ExecutionCoordinator.asyncCoordinator())
                .buildOnEnable(instance);

        parser = new AnnotationParser<>(manager, Source.class);
    }

    @Override
    public void parse(@NotNull Object... instances) {
        parser.parse(instances);
    }

    @Override
    public AnnotationParser<Source> getParser() {
        return parser;
    }
}
