package at.yawk.data.xml.lexer;

import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * XML element start with attributes.
 *
 * @author Yawkat
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class ElementStartEvent extends ElementEvent {
    private final Map<String, String> attributes;

    public ElementStartEvent(String tagName, Map<String, String> attributes) {
        super(tagName);
        this.attributes = attributes;
    }
}
