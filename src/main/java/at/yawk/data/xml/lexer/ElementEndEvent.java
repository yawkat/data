package at.yawk.data.xml.lexer;

import lombok.EqualsAndHashCode;

/**
 * XML element end.
 *
 * @author Yawkat
 */
@EqualsAndHashCode(callSuper = true)
public class ElementEndEvent extends ElementEvent {
    public ElementEndEvent(String tagName) {
        super(tagName);
    }
}
