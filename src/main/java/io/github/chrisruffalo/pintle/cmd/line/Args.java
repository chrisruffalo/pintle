package io.github.chrisruffalo.pintle.cmd.line;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A binding annotation for injecting
 * the command line root in the event
 * that commands need to be read by beans.
 *
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Args {

}
