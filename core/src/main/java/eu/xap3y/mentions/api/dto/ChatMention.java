package eu.xap3y.mentions.api.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

@Getter
@RequiredArgsConstructor
public class ChatMention {

    private final int start;
    private final int end;
    private final Player target;
    private final String replacement;
    private final String hoverText;
}
