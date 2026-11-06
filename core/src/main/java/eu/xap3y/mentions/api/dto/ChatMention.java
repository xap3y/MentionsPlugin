package eu.xap3y.mentions.api.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class ChatMention {

    private final int start;
    private final int end;
    private final Player target;
    private final String replacement;
    private final String hoverText;

    @Setter
    private String clickSuggestion = "";

    @Setter
    private List<Player> recipients = Collections.emptyList();

    @Setter
    private String soundOverride = "";

    @Setter
    private boolean enableTitle = true;

    @Setter
    private boolean enableActionbar = true;
}
