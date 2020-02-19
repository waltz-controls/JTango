package org.tango.transport;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 19.02.2020
 */
public class Message {
    public static final String PATTERN_STR = "(read|write|exec|response):([\\w\\W]+);(double|float|int|long|String|null):([\\w.]+)";
    public static final Pattern PATTERN = Pattern.compile(PATTERN_STR);

    public static final String TARGET_PATTERN_STR = "(\\w+)/(\\w+)/(\\w+)/(\\w+)";
    public static final Pattern TARGET_PATTERN = Pattern.compile(TARGET_PATTERN_STR);

    public String action;
    public String target;
    public String dataType;
    public String value;

    public Message(String action, String target, String dataType, String value) {
        this.action = action;
        this.target = target;
        this.dataType = dataType;
        this.value = value;
    }

    public static Message fromString(String str) {
        Matcher matcher = PATTERN.matcher(str);

        if (matcher.matches())
            return new Message(matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4));
        else
            throw new IllegalArgumentException("Unrecognized message: " + str);
    }

    @Override
    public String toString() {
        return String.format(
                "%s:%s;%s:%s", action, target, dataType, value
        );
    }

    public static class Target {
        public final String device;
        public final String member;

        public Target(String device, String member) {
            this.device = device;
            this.member = member;
        }

        public static Target fromString(String str) {
            Matcher matcher = TARGET_PATTERN.matcher(str);

            if (matcher.matches())
                return new Target(String.format("%s/%s/%s", matcher.group(1), matcher.group(2), matcher.group(3)), matcher.group(4));
            else
                throw new IllegalArgumentException("Unrecognized message: " + str);
        }
    }

    public static class Error extends Message {
        public Error(String value) {
            super("response", "error", "String", value);
        }
    }

    public static class Ok extends Message {
        public Ok() {
            super("response", "ok", null, null);
        }
    }
}
