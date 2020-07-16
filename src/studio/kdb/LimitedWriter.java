package studio.kdb;

import java.io.CharArrayWriter;
import java.io.IOException;

public class LimitedWriter extends CharArrayWriter {
    private static final String TOO_LONG = " ... ";

    private final int limit;

    public static class LimitException extends RuntimeException {
    }

    public LimitedWriter(int limit) {
        this.limit = limit;
    }

    public void write(char c) throws IOException {
        if ((1 + size()) > limit) {
            super.write(TOO_LONG);
            throw new LimitException();
        }
        super.write(c);
    }

    public void write(String s, int off, int len) {
        if ((size() + s.length()) > limit) {
            if (limit > size()) {
                String substring = s.substring(0, limit - size());
                super.write(substring, 0, substring.length());
            }
            super.write(TOO_LONG, 0, TOO_LONG.length());
            throw new LimitException();
        }
        super.write(s, off, len);
    }
}
