package at.yawk.data.xml.lexer;

import java.util.function.Predicate;

/**
 * Base event.
 *
 * @author Yawkat
 */
public abstract class Event implements Predicate<Event> {
    @Override
    public boolean test(Event event) {
        return this.equals(event);
    }
}
