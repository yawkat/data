package at.yawk.data.xml.lexer;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * General element-related event with tag name.
 *
 * @author Yawkat
 */
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Getter
public abstract class ElementEvent extends Event {
    private final String tagName;
}
