package eu.xap3y.mentions.manager;

import org.bukkit.configuration.file.YamlConfiguration;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/** Adds missing defaults and comments without reserializing the user's existing YAML. */
public final class ConfigurationFileUpdater {

    private final List<String> current;
    private final List<String> template;
    private final Map<Integer, List<Insertion>> insertions = new HashMap<>();
    private final Map<Integer, String> replacements = new HashMap<>();
    private final Set<Integer> skippedLines = new HashSet<>();
    private final YamlConfiguration merged;
    private final String newline;

    private ConfigurationFileUpdater(String current, String template) {
        this.current = new ArrayList<>(Arrays.asList(current.split("\\r?\\n", -1)));
        this.template = Arrays.asList(template.split("\\r?\\n", -1));
        this.newline = current.contains("\r\n") ? "\r\n" : "\n";
        this.merged = YamlConfiguration.loadConfiguration(new StringReader(current));
        ConfigurationDefaults.mergeMissing(merged,
                YamlConfiguration.loadConfiguration(new StringReader(template)));
    }

    public static String merge(String current, String template) {
        String updated = mergeDocument(removeTutorialLinks(current), template);
        YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new StringReader(template));
        if (!defaults.isConfigurationSection("group-pings")) {
            return updated;
        }
        YamlConfiguration existing = YamlConfiguration.loadConfiguration(new StringReader(updated));
        org.bukkit.configuration.ConfigurationSection groups = existing.getConfigurationSection("group-pings");
        if (groups == null || groups.getKeys(false).isEmpty()) {
            return updated;
        }
        YamlConfiguration presetDefaults = new YamlConfiguration();
        for (String key : groups.getKeys(false)) {
            if (groups.isConfigurationSection(key)) {
                presetDefaults.set("group-pings." + key + ".replace", "&a&l%text%&r");
            }
        }
        List<String> documented = new ArrayList<>();
        for (String line : presetDefaults.saveToString().split("\n")) {
            if (line.trim().startsWith("replace:")) {
                int indent = line.length() - line.trim().length();
                documented.add(spaces(indent) + (template.contains("# Zobrazený text;")
                        ? "# Zobrazený text; %text% je nastavený spouštěč."
                        : "# Display text; %text% is the configured trigger."));
            }
            documented.add(line);
        }
        return mergeDocument(updated, String.join("\n", documented));
    }

    private static String mergeDocument(String current, String template) {
        if (current.trim().isEmpty()) {
            return template;
        }
        ConfigurationFileUpdater updater = new ConfigurationFileUpdater(current, template);
        Node actual = new Yaml().compose(new StringReader(current));
        Node defaults = new Yaml().compose(new StringReader(template));
        if (!(defaults instanceof MappingNode)) {
            return current;
        }
        if (actual == null) {
            return current + (current.endsWith("\n") ? "" : "\n") + template;
        }
        if (!(actual instanceof MappingNode)) {
            throw new IllegalArgumentException("Config root must be a YAML mapping");
        }
        if (updater.isFlow(actual)) {
            List<String> comments = new ArrayList<>();
            for (String line : updater.current) {
                String comment = inlineComment(line);
                if (!comment.isEmpty()) {
                    comments.add(comment);
                }
            }
            return mergeDocument(String.join("\n", comments) + "\n" + updater.merged.saveToString(), template);
        }
        updater.visit((MappingNode) actual, (MappingNode) defaults, "", 0);
        List<String> output = new ArrayList<>();
        for (int line = 0; line <= updater.current.size(); line++) {
            List<Insertion> additions = updater.insertions.get(line);
            if (additions != null) {
                // Nested children must precede new sibling/root entries at the same position.
                additions.sort(Comparator.comparingInt((Insertion insertion) -> insertion.indent).reversed());
                for (Insertion insertion : additions) {
                    output.addAll(insertion.lines);
                }
            }
            if (line < updater.current.size() && !updater.skippedLines.contains(line)) {
                output.add(updater.replacements.getOrDefault(line, updater.current.get(line)));
            }
        }
        return String.join(updater.newline, output);
    }

    private static String removeTutorialLinks(String yaml) {
        Set<Integer> scalarLines = new HashSet<>();
        collectScalarLines(new Yaml().compose(new StringReader(yaml)), scalarLines, new HashSet<Node>());
        String separator = yaml.contains("\r\n") ? "\r\n" : "\n";
        List<String> lines = Arrays.asList(yaml.split("\r?\n", -1));
        List<String> retained = new ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            boolean tutorialLink = line.matches("\\h*# https://plugins\\.xap3y\\.eu/mentions#"
                    + "(?:setup|group-pings|everyone-ping|click-actions|hover-colors|preferences|updates|troubleshooting)\\h*");
            if (!tutorialLink || scalarLines.contains(index)) {
                retained.add(line);
            }
        }
        return String.join(separator, retained);
    }

    private static void collectScalarLines(Node node, Set<Integer> lines, Set<Node> visited) {
        if (node == null || !visited.add(node)) {
            return;
        }
        if (node instanceof ScalarNode) {
            for (int line = node.getStartMark().getLine() + 1; line <= lastLine(node); line++) {
                lines.add(line);
            }
        } else if (node instanceof MappingNode) {
            for (NodeTuple entry : ((MappingNode) node).getValue()) {
                collectScalarLines(entry.getKeyNode(), lines, visited);
                collectScalarLines(entry.getValueNode(), lines, visited);
            }
        } else if (node instanceof SequenceNode) {
            for (Node entry : ((SequenceNode) node).getValue()) {
                collectScalarLines(entry, lines, visited);
            }
        }
    }

    private void visit(MappingNode actual, MappingNode defaults, String parent, int indent) {
        Map<String, NodeTuple> entries = entries(actual);
        int insertionLine = lastLine(actual) + 1;
        for (NodeTuple entry : defaults.getValue()) {
            String key = ((ScalarNode) entry.getKeyNode()).getValue();
            String path = parent.isEmpty() ? key : parent + "." + key;
            NodeTuple existing = entries.get(key);
            if (existing == null) {
                add(insertionLine, indent, snippet(entry, indent));
                continue;
            }
            addComments(existing, entry);
            Node value = existing.getValueNode();
            if (Tag.NULL.equals(value.getTag())) {
                int keyIndent = existing.getKeyNode().getStartMark().getColumn();
                List<String> replacement = new ArrayList<>();
                int defaultIndent = entry.getKeyNode().getStartMark().getColumn();
                for (int index = entry.getKeyNode().getStartMark().getLine(); index <= lastLine(entry.getValueNode()); index++) {
                    replacement.add(spaces(keyIndent) + template.get(index).substring(defaultIndent));
                }
                String originalComment = inlineComment(current.get(existing.getKeyNode().getStartMark().getLine()));
                if (!originalComment.isEmpty() && inlineComment(replacement.get(0)).isEmpty()) {
                    replacement.set(0, replacement.get(0) + " " + originalComment);
                }
                replacements.put(existing.getKeyNode().getStartMark().getLine(), String.join("\n", replacement));
                continue;
            }
            if (value instanceof MappingNode && entry.getValueNode() instanceof MappingNode) {
                int keyIndent = existing.getKeyNode().getStartMark().getColumn();
                if (isFlow(value)) {
                    if (((MappingNode) entry.getValueNode()).getValue().isEmpty()) {
                        continue;
                    }
                    YamlConfiguration section = new YamlConfiguration();
                    section.set(key, merged.get(path));
                    String expanded = section.saveToString();
                    String original = current.get(existing.getKeyNode().getStartMark().getLine());
                    String inlineComment = inlineComment(original);
                    List<String> lines = new ArrayList<>();
                    for (String line : expanded.split("\n")) {
                        lines.add(spaces(keyIndent) + line);
                    }
                    if (!inlineComment.isEmpty()) {
                        lines.set(0, lines.get(0) + " " + inlineComment);
                    }
                    // Expand flow mappings so individual child keys can receive comments.
                    int firstLine = existing.getKeyNode().getStartMark().getLine();
                    int endLine = value.getEndMark().getLine();
                    for (int index = firstLine + 1; index <= endLine; index++) {
                        String comment = inlineComment(current.get(index));
                        if (!comment.isEmpty()) {
                            lines.add(spaces(keyIndent) + comment);
                        }
                        skippedLines.add(index);
                    }
                    List<String> nestedLines = snippet(entry, keyIndent);
                    int parentComments = precedingComments(template, entry.getKeyNode().getStartMark().getLine()).size();
                    String nestedTemplate = String.join("\n", nestedLines.subList(parentComments, nestedLines.size()));
                    String decorated = mergeDocument(String.join("\n", lines), nestedTemplate);
                    replacements.put(existing.getKeyNode().getStartMark().getLine(), decorated);
                } else {
                    int childIndent = ((MappingNode) value).getValue().isEmpty()
                            ? keyIndent + 4 : ((MappingNode) value).getValue().get(0).getKeyNode().getStartMark().getColumn();
                    visit((MappingNode) value, (MappingNode) entry.getValueNode(), path, childIndent);
                }
            }
        }
    }

    private boolean isFlow(Node node) {
        int line = node.getStartMark().getLine();
        int column = node.getStartMark().getColumn();
        return line < current.size() && column < current.get(line).length()
                && current.get(line).charAt(column) == '{';
    }

    private void addComments(NodeTuple existing, NodeTuple defaults) {
        int line = existing.getKeyNode().getStartMark().getLine();
        int indent = existing.getKeyNode().getStartMark().getColumn();
        int templateLine = defaults.getKeyNode().getStartMark().getLine();
        List<String> oldComments = precedingComments(current, line);
        List<String> missing = new ArrayList<>();
        for (String comment : precedingComments(template, templateLine)) {
            if (!oldComments.contains(comment) && !missing.contains(spaces(indent) + comment)) {
                missing.add(spaces(indent) + comment);
            }
        }
        if (!missing.isEmpty()) {
            add(line, indent, missing);
        }
        String inline = inlineComment(template.get(templateLine));
        if (!inline.isEmpty() && inlineComment(current.get(line)).isEmpty()) {
            replacements.put(line, current.get(line) + " " + inline);
        }
    }

    private static List<String> precedingComments(List<String> lines, int keyLine) {
        List<String> comments = new ArrayList<>();
        for (int index = keyLine - 1; index >= 0; index--) {
            String trimmed = lines.get(index).trim();
            if (trimmed.startsWith("#")) {
                comments.add(trimmed);
            } else if (!trimmed.isEmpty()) {
                break;
            }
        }
        Collections.reverse(comments);
        return comments;
    }

    private List<String> snippet(NodeTuple entry, int indent) {
        int start = entry.getKeyNode().getStartMark().getLine();
        int end = lastLine(entry.getValueNode());
        int originalIndent = entry.getKeyNode().getStartMark().getColumn();
        List<String> lines = new ArrayList<>();
        for (String comment : precedingComments(template, start)) {
            lines.add(spaces(indent) + comment);
        }
        for (int index = start; index <= end; index++) {
            String line = template.get(index);
            lines.add(line.trim().isEmpty() ? "" : spaces(indent) + line.substring(Math.min(originalIndent, line.length())));
        }
        return lines;
    }

    private static int lastLine(Node node) {
        if (node instanceof MappingNode && !((MappingNode) node).getValue().isEmpty()) {
            List<NodeTuple> entries = ((MappingNode) node).getValue();
            return lastLine(entries.get(entries.size() - 1).getValueNode());
        }
        if (node instanceof SequenceNode && !((SequenceNode) node).getValue().isEmpty()) {
            List<Node> entries = ((SequenceNode) node).getValue();
            return lastLine(entries.get(entries.size() - 1));
        }
        return node.getEndMark().getLine() - (node.getEndMark().getColumn() == 0 ? 1 : 0);
    }

    private static Map<String, NodeTuple> entries(MappingNode mapping) {
        Map<String, NodeTuple> entries = new LinkedHashMap<>();
        for (NodeTuple entry : mapping.getValue()) {
            if (entry.getKeyNode() instanceof ScalarNode) {
                entries.put(((ScalarNode) entry.getKeyNode()).getValue(), entry);
            }
        }
        return entries;
    }

    private void add(int line, int indent, List<String> lines) {
        insertions.computeIfAbsent(Math.max(0, line), ignored -> new ArrayList<>())
                .add(new Insertion(indent, lines));
    }

    private static String spaces(int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, ' ');
        return new String(chars);
    }

    private static String inlineComment(String line) {
        boolean single = false;
        boolean doubleQuoted = false;
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '\\' && doubleQuoted) {
                index++;
                continue;
            }
            boolean quoteBoundary = index == 0 || Character.isWhitespace(line.charAt(index - 1))
                    || ":,[{".indexOf(line.charAt(index - 1)) >= 0;
            if (character == '\'' && !doubleQuoted && (single || quoteBoundary)) {
                single = !single;
            } else if (character == '"' && !single && (doubleQuoted || quoteBoundary)) {
                doubleQuoted = !doubleQuoted;
            } else if (character == '#' && !single && !doubleQuoted
                    && (index == 0 || Character.isWhitespace(line.charAt(index - 1)))) {
                return line.substring(index);
            }
        }
        return "";
    }

    private static final class Insertion {
        private final int indent;
        private final List<String> lines;

        private Insertion(int indent, List<String> lines) {
            this.indent = indent;
            this.lines = lines;
        }
    }
}
