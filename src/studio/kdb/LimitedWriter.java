package studio.kdb;

import java.io.CharArrayWriter;
import java.io.IOException;

public class LimitedWriter extends CharArrayWriter {
    private int limit;

    public static class LimitException extends RuntimeException {
    }

    public LimitedWriter(int limit) {
        this.limit = limit;
    }

    public void write(char c) throws IOException {
        if ((1 + size()) > limit) {
            super.write(" ... ");
            throw new LimitException();
        }
        super.write(c);
    }

    public void write(String s) throws IOException {
        if ((size() + s.length()) > limit) {
            if (limit>size()) {
                super.write(s.substring(0,limit - size()));
            }
            super.write(" ... ");
            throw new LimitException();
        }
        super.write(s);
    }
}
