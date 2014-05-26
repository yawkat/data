package at.yawk.data.xml.lexer;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Text.
 *
 * @author Yawkat
 */
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Getter
public class TextEvent extends Event {
    private final String text;
}
