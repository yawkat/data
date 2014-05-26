package at.yawk.data.xml.lexer;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Exception while reading.
 *
 * @author Yawkat
 */
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Getter
class ExceptionEvent extends Event {
    private final Exception exception;
}
