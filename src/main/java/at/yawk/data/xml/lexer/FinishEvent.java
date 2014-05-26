package at.yawk.data.xml.lexer;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Document complete.
 *
 * @author Yawkat
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
class FinishEvent extends Event {
    @Getter private static final FinishEvent instance = new FinishEvent();
}
