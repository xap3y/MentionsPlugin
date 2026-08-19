package eu.xap3y.mentions.api.iface;

import org.incendo.cloud.annotations.AnnotationParser;

public interface CommandManagerIface<T> {

    void parse(Object... objects);

    AnnotationParser<T> getParser();
}