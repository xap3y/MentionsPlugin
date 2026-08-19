package eu.xap3y.mentions.api.dto;

public class TextModifierDto {
    private final boolean withPrefix;
    private final boolean colored;

    public TextModifierDto(boolean withPrefix, boolean colored) {
        this.withPrefix = withPrefix;
        this.colored = colored;
    }

    public boolean withPrefix() {
        return withPrefix;
    }

    public boolean colored() {
        return colored;
    }
}
