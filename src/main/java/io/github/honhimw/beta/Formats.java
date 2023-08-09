package io.github.honhimw.beta;

import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

/**
 * @author hon_him
 * @since 2023-06-05
 */

@SuppressWarnings("unused")
public class Formats {

    public static String format(String format, Object... args) {
        FormattingTuple formattingTuple = MessageFormatter.arrayFormat(format, args);
        return formattingTuple.getMessage();
    }

}
