package studio.kdb;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public class K {
    private static SimpleDateFormat formatter = new SimpleDateFormat();
    private static DecimalFormat nsFormatter = new DecimalFormat("000000000");

    static {
        formatter.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
    }

    private static final String enlist = "enlist ";
    private static final String flip = "flip ";

    public static void write(OutputStream o, byte b) throws IOException {
        o.write(b);
    }

    public static void write(OutputStream o, short h) throws IOException {
        write(o, (byte) (h >> 8));
        write(o, (byte) h);
    }

    public static void write(OutputStream o, int i) throws IOException {
        write(o, (short) (i >> 16));
        write(o, (short) i);
    }

    public static void write(OutputStream o, long j) throws IOException {
        write(o, (int) (j >> 32));
        write(o, (int) j);
    }

    private static synchronized String sd(String s, java.util.Date x) {
        formatter.applyPattern(s);
        return formatter.format(x);
    }

    public abstract static class KBase {
        public abstract String getDataType();

        public int type;

        public void serialise(OutputStream o) throws IOException {
            write(o, (byte) type);
        }

        public boolean isNull() {
            return false;
        }

        public final String toString() {
            return toString(true);
        }

        public final String toString(boolean showType) {
            return format(null, showType).toString();
        }

        public StringBuilder format(StringBuilder builder, boolean showType) {
            if (builder == null) builder = new StringBuilder();
            return builder;
        }

    }

    public abstract static class Adverb extends KBase {
        public String getDataType() {
            return "Adverb";
        }

        protected K.KBase o;

        public Adverb(K.KBase o) {
            this.o = o;
        }

        public K.KBase getObject() {
            return o;
        }

        @Override
        public StringBuilder format(StringBuilder builder, boolean showType) {
            return super.format(builder, showType).append(o.toString(showType));
        }
    }

    public static class BinaryPrimitive extends Primitive {
        private final static String[] ops = {":", "+", "-", "*", "%", "&", "|", "^", "=", "<", ">", "$", ",", "#", "_", "~", "!", "?", "@", ".", "0:", "1:", "2:", "in", "within", "like", "bin", "ss", "insert", "wsum", "wavg", "div", "xexp", "setenv", "binr", "cov", "cor"};

        public String getDataType() {
            return "Binary Primitive";
        }

        public BinaryPrimitive(int i) {
            super(ops, i);
            type = 102;
        }

        @Override
        public StringBuilder format(StringBuilder builder, boolean showType) {
            return super.format(builder, showType).append(getPrimitive());
        }

    }

    public static class FComposition extends KBase {
        Object[] objs;

        public String getDataType() {
            return "Function Composition";
        }

        public FComposition(Object[] objs) {
            this.objs = objs;
            type = 105;
        }

        //@TODO: implement
        @Override
        public StringBuilder format(StringBuilder builder, boolean showType) {
            return super.format(builder, showType);
        }
    }

    public static class FEachLeft extends Adverb {
        public FEachLeft(K.KBase o) {
            super(o);
            type = 111;
        }

        @Override
        public StringBuilder format(StringBuilder builder, boolean showType) {
            return super.format(builder, showType).append("\\:");
        }
    }

    public static class FEachRight extends Adverb {
        public FEachRight(K.KBase o) {
            super(o);
            type = 110;
        }

        @Override
        public StringBuilder format(StringBuilder builder, boolean showType) {
            return super.format(builder, showType).append("/:");
        }
    }

    public static class FPrior extends Adverb {
        public FPrior(K.KBase o) {
            super(o);
            type = 109;
        }

        @Override
        public StringBuilder format(StringBuilder builder, boolean showType) {
            return super.format(builder, showType).append("':");
        }
    }

    public static class Feach extends Adverb {
        public Feach(K.KBase o) {
            super(o);
            type = 106;
        }

        @Override
        public StringBuilder format(StringBuilder builder, boolean showType) {
            return super.format(builder, showType).append("'");
        }
    }

    public static class Fover extends Adverb {
        public Fover(K.KBase o) {
            super(o);
            type = 107;
        }

        @Override
        public StringBuilder format(StringBuilder builder, boolean showType) {
            return super.format(builder, showType).append("/");
        }
    }

    public static class Fscan extends Adverb {
        public Fscan(KBase o) {
            super(o);
            type = 108;
            this.o = o;
        }

        @Override
        public StringBuilder format(StringBuilder builder, boolean showType) {
            return super.format(builder, showType).append("\\");
        }
    }

    public static class Function extends KBase {
        public String getDataType() {
            return "Function";
        }

        private String body;

        public Function(KCharacterVector body) {
            type = 100;
            this.body = new String((char[]) body.getArray(), 0, body.getLength());
        }

        @Override
        public StringBuilder format(StringBuilder builder, boolean showType) {
            return super.format(builder, showType).append(body);
        }
    }

    public abstract static class Primitive extends KBase {
        public String getDataType() {
            return "Primitive";
        }

        private int primitive;
        private String s = " ";

        public Primitive(String[] ops, int i) {
            primitive = i;
            if (i >= 0 && i < ops.length)
                s = ops[i];
        }

        public String getPrimitive() {
            return s;
        }

        public int getPrimitiveAsInt() {
            return primitive;
        }
    }

    public static class Projection extends KBase {
        public String getDataType() {
            return "Projection";
        }

        private K.KList objs;

        public Projection(K.KList objs) {
            type = 104;
            this.objs = objs;
        }

        @Override
        public StringBuilder format(StringBuilder builder, boolean showType) {
            builder = super.format(builder, showType);
            if (objs.getLength() == 0) return builder; // not sure if such is possible

            KBase first = objs.at(0);

            boolean listProjection = false;
            if (first instanceof UnaryPrimitive) {
                UnaryPrimitive up = (UnaryPrimitive) objs.at(0);
                if (up.getPrimitiveAsInt() == 41) // plist
                    listProjection = true;
            }

            boolean isFunction = false;
            if ((first instanceof Function) || (first instanceof UnaryPrimitive) || (first instanceof BinaryPrimitive) || (first instanceof TernaryOperator)) {
                if (!listProjection) isFunction = true;
            }

            if (!listProjection) first.format(builder, showType);
            builder.append(isFunction ? "[" : "(");
            for (int i = 1; i < objs.getLength(); i++) {
                if (i > 1) builder.append(";");
                objs.at(i).format(builder, showType);
            }
            builder.append(isFunction ? "]" : ")");
            return builder;
        }
    }

    public static class TernaryOperator extends KBase {
        public String getDataType() {
            return "Ternary Operator";
        }

        private char charVal = ' ';

        public TernaryOperator(int i) {
            type = 103;
            if (i>=0 && i<=2) charVal = "'/\\".charAt(i);
        }

        @Override
        public StringBuilder format(StringBuilder builder, boolean showType) {
            return super.format(builder, showType).append(charVal);
        }
    }

    public static class UnaryPrimitive extends Primitive {
        private static String[] ops = {"::", "+:", "-:", "*:", "%:", "&:", "|:", "^:", "=:", "<:", ">:", "$:", ",:", "#:", "_:", "~:", "!:", "?:", "@:", ".:", "0::", "1::", "2::", "avg", "last", "sum", "prd", "min", "max", "exit", "getenv", "abs", "sqrt", "log", "exp", "sin", "asin", "cos", "acos", "tan", "atan", "enlist", "var", "dev", "hopen"};

        public UnaryPrimitive(int i) {
            super(ops, i);
            type = 101;
        }

        @Override
        public StringBuilder format(StringBuilder builder, boolean showType) {
            builder = super.format(builder, showType);
            if (getPrimitiveAsInt() != -1) builder.append(getPrimitive());
            return builder;
        }
    }

    public static class KBoolean extends KBase implements ToDouble {
        public String getDataType() {
            return "Boolean";
        }

        public boolean b;

        public KBoolean(boolean b) {
            this.b = b;
            type = -1;
        }

        @Override
        public StringBuilder format(StringBuilder builder, boolean showType) {
            return super.format(builder, showType)
                        .append(b ? "1" : "0")
                        .append(showType ? "b" : "");
        }

        public double toDouble() {
            return b ? 1.0 : 0.0;
        }

        public boolean toBoolean() {
            return b;
        }
    }

    public static class KByte extends KBase implements ToDouble {
        public String getDataType() {
            return "Byte";
        }

        public byte b;

        public double toDouble() {
            return b;
        }

        public KByte(byte b) {
            this.b = b;
            type = -4;
        }

        @Override
        public StringBuilder format(StringBuilder builder, boolean showType) {
            return super.format(builder, showType)
                        .append("0x")
                        .append(Integer.toHexString((b >> 4) & 0xf))
                        .append(Integer.toHexString(b & 0xf));
        }
    }

    public static class KShort extends KBase implements ToDouble {
        public String getDataType() {
            return "Short";
        }

        public short s;

        public double toDouble() {
            return s;
        }

        public KShort(short s) {
            this.s = s;
            type = -5;
        }

        public boolean isNull() {
            return s == Short.MIN_VALUE;
        }

        @Override
        public StringBuilder format(StringBuilder builder, boolean showType) {
            builder = super.format(builder, showType);
            if (isNull()) builder.append("0N");
            else if (s == Short.MAX_VALUE) builder.append("0W");
            else if (s == -Short.MAX_VALUE) builder.append("-0W");
            else builder.append(s);
            if (showType) builder.append("h");
            return builder;
        }
    }

    public static class KInteger extends KBase implements ToDouble {
        public String getDataType() {
            return "Integer";
        }

        public int i;

        public double toDouble() {
            return i;
        }

        public KInteger(int i) {
            this.i = i;
            type = -6;
        }

        public boolean isNull() {
            return i == Integer.MIN_VALUE;
        }

        @Override
        public StringBuilder format(StringBuilder builder, boolean showType) {
            builder = super.format(builder, showType);
            if (isNull()) builder.append("0N");
            else if (i == Integer.MAX_VALUE) builder.append("0W");
            else if (i == -Integer.MAX_VALUE) builder.append("-0W");
            else builder.append(i);
            if (showType) builder.append("i");
            return builder;
        }
    }

    public static class KSymbol extends KBase {
        public String getDataType() {
            return "Symbol";
        }

        public String s;

        public KSymbol(String s) {
            this.s = s;
            type = -11;
        }

        public boolean isNull() {
            return s.length() == 0;
        }

        @Override
        public StringBuilder format(StringBuilder builder, boolean showType) {
            builder = super.format(builder, showType);
            if (showType) builder.append("`");
            return builder.append(s);
        }

        public void serialise(OutputStream o) throws IOException {
            o.write(s.getBytes(Config.getInstance().getEncoding()));
        }
    }

    public static class KLong extends KBase implements ToDouble {
        public String getDataType() {
            return "Long";
        }

        public long j;

        public double toDouble() {
            return j;
        }

        public KLong(long j) {
            this.j = j;
            type = -7;
        }

        public boolean isNull() {
            return j == Long.MIN_VALUE;
        }

        @Override
        public StringBuilder format(StringBuilder builder, boolean showType) {
            builder = super.format(builder, showType);
            if (isNull()) builder.append("0N");
            else if (j == Long.MAX_VALUE) builder.append("0W");
            else if (j == -Long.MAX_VALUE) builder.append("-0W");
            else builder.append(j);
            return builder;
        }

        public void serialise(OutputStream o) throws IOException {
            super.serialise(o);
            write(o, j);
        }
    }

    public static class KCharacter extends KBase {
        public String getDataType() {
            return "Character";
        }

        public char c;

        public KCharacter(char c) {
            this.c = c;
            type = -10;
        }

        public boolean isNull() {
            return c == ' ';
        }

        @Override
        public StringBuilder format(StringBuilder builder, boolean showType) {
            builder = super.format(builder, showType);
            if (showType) builder.append("\"").append(c).append("\"");
            else builder.append(c);
            return builder;
        }

        public void serialise(OutputStream o) throws IOException {
            super.serialise(o);
            write(o, (byte) c);
        }
    }

    public static class KFloat extends KBase implements ToDouble {
        public String getDataType() {
            return "Float";
        }

        public float f;

        public double toDouble() {
            return f;
        }

        public KFloat(float f) {
            type = -8;
            this.f = f;
        }

        public boolean isNull() {
            return Float.isNaN(f);
        }

        @Override
        public StringBuilder format(StringBuilder builder, boolean showType) {
            builder = super.format(builder, showType);
            if (isNull()) builder.append("0N");
            else if (f == Float.POSITIVE_INFINITY) builder.append("0w");
            else if (f == Float.NEGATIVE_INFINITY) builder.append("-0w");
            else builder.append(Config.getInstance().getNumberFormat().format(f));
            if (showType) builder.append("e");
            return builder;
        }

        public void serialise(OutputStream o) throws IOException {
            super.serialise(o);
            int i = Float.floatToIntBits(f);
            write(o, i);
        }
    }

    public static class KDouble extends KBase implements ToDouble {
        public String getDataType() {
            return "Double";
        }

        public double d;

        public KDouble(double d) {
            type = -9;
            this.d = d;
        }

        public double toDouble() {
            return d;
        }

        public boolean isNull() {
            return Double.isNaN(d);
        }

        @Override
        public StringBuilder format(StringBuilder builder, boolean showType) {
            builder = super.format(builder, showType);
            if (isNull()) builder.append("0n");
            else if (d == Double.POSITIVE_INFINITY) builder.append("0w");
            else if (d == Double.NEGATIVE_INFINITY) builder.append("-0w");
            else builder.append(Config.getInstance().getNumberFormat().format(d));
            if (showType) builder.append("f");
            return builder;
        }

        public void serialise(OutputStream o) throws IOException {
            super.serialise(o);
            long j = Double.doubleToLongBits(d);
            write(o, j);
        }
    }

    public static class KDate extends KBase {
        public String getDataType() {
            return "Date";
        }

        int date;

        public KDate(int date) {
            type = -14;
            this.date = date;
        }

        public boolean isNull() {
            return date == Integer.MIN_VALUE;
        }

        @Override
        public StringBuilder format(StringBuilder builder, boolean showType) {
            builder = super.format(builder, showType);
            if (isNull()) builder.append("0Nd");
            else if (date == Integer.MAX_VALUE) builder.append("0Wd");
            else if (date == -Integer.MAX_VALUE) builder.append("-0Wd");
            else builder.append(sd("yyyy.MM.dd", toDate()));
            return builder;
        }

        public Date toDate() {
            return new Date(86400000L * (date + 10957));
        }
    }

    public static class KGuid extends KBase {
        static UUID nuuid = new UUID(0, 0);

        public String getDataType() {
            return "Guid";
        }

        UUID uuid;

        public KGuid(UUID uuid) {
            type = -2;
            this.uuid = uuid;
        }

        public boolean isNull() {
            return uuid == nuuid;
        }

        @Override
        public StringBuilder format(StringBuilder builder, boolean showType) {
            return super.format(builder, showType).append(uuid);
        }
    }

    public static class KTime extends KBase {
        public String getDataType() {
            return "Time";
        }

        int time;

        public KTime(int time) {
            type = -19;
            this.time = time;
        }

        public boolean isNull() {
            return time == Integer.MIN_VALUE;
        }

        @Override
        public StringBuilder format(StringBuilder builder, boolean showType) {
            builder = super.format(builder, showType);
            if (isNull()) builder.append("0Nt");
            else if (time == Integer.MAX_VALUE) builder.append("0Wt");
            else if (time == -Integer.MAX_VALUE) builder.append("-0Wt");
            else builder.append(sd("HH:mm:ss.SSS", toTime()));
            return builder;
        }

        public Time toTime() {
            return new Time(time);
        }
    }

    public static class KDatetime extends KBase {
        public String getDataType() {
            return "Datetime";
        }

        double time;

        public KDatetime(double time) {
            type = -15;
            this.time = time;
        }

        public boolean isNull() {
            return Double.isNaN(time);
        }

        @Override
        public StringBuilder format(StringBuilder builder, boolean showType) {
            builder = super.format(builder, showType);
            if (isNull()) builder.append("0N");
            else if (time == Double.POSITIVE_INFINITY) builder.append("0w");
            else if (time == Double.NEGATIVE_INFINITY) builder.append("-0w");
            else builder.append(sd("yyyy.MM.dd HH:mm:ss.SSS", toTimestamp()));
            if (showType) builder.append("z");
            return builder;
        }

        public Timestamp toTimestamp() {
            return new Timestamp(((long) (.5 + 8.64e7 * (time + 10957))));
        }
    }


    public static class KTimestamp extends KBase {
        public String getDataType() {
            return "Timestamp";
        }

        long time;

        public KTimestamp(long time) {
            type = -12;
            this.time = time;
        }

        public boolean isNull() {
            return time == Long.MIN_VALUE;
        }

        @Override
        public StringBuilder format(StringBuilder builder, boolean showType) {
            builder = super.format(builder, showType);
            if (isNull()) builder.append("0Np");
            else if (time == Long.MAX_VALUE) builder.append("0Wp");
            else if (time == -Long.MAX_VALUE) builder.append("-0Wp");
            else {
                Timestamp ts = toTimestamp();
                builder.append(sd("yyyy.MM.dd HH:mm:ss.", ts))
                        .append(nsFormatter.format(ts.getNanos()));
            }
            return builder;
        }

        public Timestamp toTimestamp() {
            long k = 86400000L * 10957;
            long n = 1000000000L;
            long d = time < 0 ? (time + 1) / n - 1 : time / n;
            long ltime = time == Long.MIN_VALUE ? time : (k + 1000 * d);
            int nanos = (int) (time - n * d);
            Timestamp ts = new Timestamp(ltime);
            ts.setNanos(nanos);
            return ts;
        }
    }

    public static class Dict extends KBase {
        public String getDataType() {
            return "Dictionary";
        }

        private byte attr = 0;

        public K.KBase x;
        public K.KBase y;

        public Dict(K.KBase X, K.KBase Y) {
            type = 99;
            x = X;
            y = Y;
        }

        public void setAttr(byte attr) {
            this.attr = attr;
        }

        @Override
        public StringBuilder format(StringBuilder builder, boolean showType) {
            builder = super.format(builder, showType);
            boolean useBrackets = attr != 0 || x instanceof Flip;
            if (useBrackets) builder.append("(");
            x.format(builder, showType);
            if (useBrackets) builder.append(")");
            builder.append("!");
            y.format(builder, showType);
            return builder;
        }
    }

    public static class Flip extends KBase {
        public String getDataType() {
            return "Flip";
        }

        public K.KSymbolVector x;
        public K.KBaseVector y;

        public Flip(Dict X) {
            type = 98;
            x = (K.KSymbolVector) X.x;
            y = (K.KBaseVector) X.y;
        }

        @Override
        public StringBuilder format(StringBuilder builder, boolean showType) {
            builder =  super.format(builder, showType);
            boolean usebracket = x.getLength() == 1;
            builder.append(flip);
            if (usebracket) builder.append("(");
            x.format(builder, showType);
            if (usebracket) builder.append(")");
            builder.append("!");
            y.format(builder, showType);
            return builder;
        }
    }

    public static class Month extends KBase {
        public String getDataType() {
            return "Month";
        }

        public int i;

        public Month(int x) {
            type = -13;
            i = x;
        }

        public boolean isNull() {
            return i == Integer.MIN_VALUE;
        }

        @Override
        public StringBuilder format(StringBuilder builder, boolean showType) {
            builder = super.format(builder, showType);
            if (isNull()) builder.append("0N");
            else if (i == Integer.MAX_VALUE) builder.append("0W");
            else if (i == -Integer.MAX_VALUE) builder.append("-0W");
            else {
                int m = i + 24000, y = m / 12;

                builder.append(i2(y / 100)).append(i2(y % 100))
                        .append(".").append(i2(1 + m % 12));
            }
            if (showType) builder.append("m");
            return builder;
        }

        public Date toDate() {
            int m = i + 24000, y = m / 12;
            Calendar cal = Calendar.getInstance();
            cal.set(y, m, 01);
            return cal.getTime();
        }
    }

    public static class Minute extends KBase {
        public String getDataType() {
            return "Minute";
        }

        public int i;

        public Minute(int x) {
            type = -17;
            i = x;
        }

        public boolean isNull() {
            return i == Integer.MIN_VALUE;
        }

        @Override
        public StringBuilder format(StringBuilder builder, boolean showType) {
            builder = super.format(builder, showType);
            if (isNull()) builder.append("0Nu");
            else if (i == Integer.MAX_VALUE) builder.append("0Wu");
            else if (i == -Integer.MAX_VALUE) builder.append("-0Wu");
            else builder.append(i2(i / 60)).append(":").append(i2(i % 60));
            return builder;
        }
        public Date toDate() {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR, i / 60);
            cal.set(Calendar.MINUTE, i % 60);
            return cal.getTime();
        }
    }

    public static class Second extends KBase {
        public String getDataType() {
            return "Second";
        }

        public int i;

        public Second(int x) {
            type = -18;
            i = x;
        }

        public boolean isNull() {
            return i == Integer.MIN_VALUE;
        }

        @Override
        public StringBuilder format(StringBuilder builder, boolean showType) {
            builder = super.format(builder, showType);
            if (isNull()) builder.append("0Nv");
            else if (i == Integer.MAX_VALUE) builder.append("0Wv");
            else if (i == -Integer.MAX_VALUE) builder.append("-0Wv");
            else {
                int s = i % 60;
                int m = i / 60 % 60;
                int h = i / 3600;
                builder.append(i2(h)).append(":").append(i2(m)).append(":").append(i2(s));
            }
            return builder;
        }

        public Date toDate() {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR, i / (60 * 60));
            cal.set(Calendar.MINUTE, (int) ((i % (60 * 60)) / 60));
            cal.set(Calendar.SECOND, i % 60);
            return cal.getTime();
        }
    }

    public static class KTimespan extends KBase {
        public long j;

        public KTimespan(long x) {
            j = x;
            type = -16;
        }

        public String getDataType() {
            return "Timespan";
        }

        public boolean isNull() {
            return j == Long.MIN_VALUE;
        }

        @Override
        public StringBuilder format(StringBuilder builder, boolean showType) {
            builder = super.format(builder, showType);
            if (isNull()) builder.append("0Nn");
            else if (j == Long.MAX_VALUE) builder.append("0Wn");
            else if (j == -Long.MAX_VALUE) builder.append("-0Wn");
            else {
                long jj = j;
                if (jj < 0) {
                    jj = -jj;
                    builder.append("-");
                }
                int d = ((int) (jj / 86400000000000L));
                if (d != 0) builder.append(d).append("D");
                builder.append(i2((int) ((jj % 86400000000000L) / 3600000000000L)))
                        .append(":").append(i2((int) ((jj % 3600000000000L) / 60000000000L)))
                        .append(":").append(i2((int) ((jj % 60000000000L) / 1000000000L)))
                        .append(".").append(nsFormatter.format((int) (jj % 1000000000L)));
            }
            return builder;
        }

        public Time toTime() {
            return new Time((j / 1000000));
        }
    }

    static java.text.DecimalFormat i2Formatter = new java.text.DecimalFormat("00");

    static String i2(int i) {
        return i2Formatter.format(i);
    }

    public static abstract class KBaseVector extends KBase {
        protected Object array;
        private int length;
        private byte attr;
        private final String typeName;
        private final String typeChar;


        protected KBaseVector(Class klass, int length, String typeName, String typeChar) {
            array = Array.newInstance(klass, length);
            this.length = length;
            this.typeName = typeName;
            this.typeChar = typeChar;
        }

        public abstract KBase at(int i);

        public byte getAttr() {
            return attr;
        }

        public void setAttr(byte attr) {
            this.attr = attr;
        }

        public int getLength() {
            return length;
        }

        public Object getArray() {
            return array;
        }

        public int[] gradeUp() {
            return Sorter.gradeUp(getArray(), getLength());
        }

        public int[] gradeDown() {
            return Sorter.gradeDown(getArray(), getLength());
        }

        private final static String[] sAttr = new String[]{"", "`s#", "`u#", "`p#", "`g#"};

        //default implementation
        protected StringBuilder formatVector(StringBuilder builder, boolean showType) {
            if (getLength() == 0) builder.append("`").append(typeName).append("$()");
            else {
                if (getLength() == 1) builder.append(enlist);
                for (int i = 0; i < getLength(); i++) {
                    if (i > 0) builder.append(" ");
                    at(i).format(builder, false);
                }
                if (showType) builder.append(typeChar);
            }
            return builder;
        }

        @Override
        public final StringBuilder format(StringBuilder builder, boolean showType) {
            builder = super.format(builder, showType);
            if (showType && attr <= sAttr.length) builder.append(sAttr[attr]);
            return formatVector(builder, showType);
        }
    }

    public static class KShortVector extends KBaseVector {
        public String getDataType() {
            return "Short Vector";
        }

        public KShortVector(int length) {
            super(short.class, length, "short", "h");
            type = 5;
        }

        public KBase at(int i) {
            return new KShort(Array.getShort(array, i));
        }
    }

    public static class KIntVector extends KBaseVector {
        public String getDataType() {
            return "Int Vector";
        }

        public KIntVector(int length) {
            super(int.class, length, "int", "i");
            type = 6;
        }

        public KBase at(int i) {
            return new KInteger(Array.getInt(array, i));
        }
    }

    public static class KList extends KBaseVector {
        public String getDataType() {
            return "List";
        }

        public KList(int length) {
            super(KBase.class, length, "", "");
            type = 0;
        }

        public KBase at(int i) {
            return (KBase) Array.get(array, i);
        }

        @Override
        protected StringBuilder formatVector(StringBuilder builder, boolean showType) {
            if (getLength() == 1) builder.append(enlist);
            else builder.append("(");
            for (int i = 0; i < getLength(); i++) {
                if (i > 0) builder.append(";");
                at(i).format(builder, showType);
            }
            if (getLength() != 1) builder.append(")");
            return builder;
        }
    }

    public static class KDoubleVector extends KBaseVector {
        public String getDataType() {
            return "Double Vector";
        }

        public KDoubleVector(int length) {
            super(double.class, length, "float", "f");
            type = 9;
        }

        public KBase at(int i) {
            return new KDouble(Array.getDouble(array, i));
        }
    }

    public static class KFloatVector extends KBaseVector {
        public String getDataType() {
            return "Float Vector";
        }

        public KFloatVector(int length) {
            super(float.class, length, "real", "e");
            type = 8;
        }

        public KBase at(int i) {
            return new KFloat(Array.getFloat(array, i));
        }

    }

    public static class KLongVector extends KBaseVector {
        public String getDataType() {
            return "Long Vector";
        }

        public KLongVector(int length) {
            super(long.class, length, "long", "");
            type = 7;
        }

        public KBase at(int i) {
            return new KLong(Array.getLong(array, i));
        }
    }

    public static class KMonthVector extends KBaseVector {
        public String getDataType() {
            return "Month Vector";
        }

        public KMonthVector(int length) {
            super(int.class, length, "month", "m");
            type = 13;
        }

        public KBase at(int i) {
            return new Month(Array.getInt(array, i));
        }
    }

    public static class KDateVector extends KBaseVector {
        public String getDataType() {
            return "Date Vector";
        }

        public KDateVector(int length) {
            super(int.class, length, "date", "");
            type = 14;
        }

        public KBase at(int i) {
            return new KDate(Array.getInt(array, i));
        }
    }

    public static class KGuidVector extends KBaseVector {
        public String getDataType() {
            return "Guid Vector";
        }

        public KGuidVector(int length) {
            super(UUID.class, length, "guid", "");
            type = 2;
        }

        public KBase at(int i) {
            return new KGuid((UUID) Array.get(array, i));
        }
    }

    public static class KMinuteVector extends KBaseVector {
        public String getDataType() {
            return "Minute Vector";
        }

        public KMinuteVector(int length) {
            super(int.class, length, "minute", "");
            type = 17;
        }

        public KBase at(int i) {
            return new Minute(Array.getInt(array, i));
        }
    }

    public static class KDatetimeVector extends KBaseVector {
        public String getDataType() {
            return "Datetime Vector";
        }

        public KDatetimeVector(int length) {
            super(double.class, length, "datetime", "z");
            type = 15;
        }

        public KBase at(int i) {
            return new KDatetime(Array.getDouble(array, i));
        }
    }

    public static class KTimestampVector extends KBaseVector {
        public String getDataType() {
            return "Timestamp Vector";
        }

        public KTimestampVector(int length) {
            super(long.class, length, "timestamp", "");
            type = 12;
        }

        public KBase at(int i) {
            return new KTimestamp(Array.getLong(array, i));
        }
    }

    public static class KTimespanVector extends KBaseVector {
        public String getDataType() {
            return "Timespan Vector";
        }

        public KTimespanVector(int length) {
            super(long.class, length, "timespan", "");
            type = 16;
        }

        public KBase at(int i) {
            return new KTimespan(Array.getLong(array, i));
        }
    }

    public static class KSecondVector extends KBaseVector {
        public String getDataType() {
            return "Second Vector";
        }

        public KSecondVector(int length) {
            super(int.class, length, "second", "");
            type = 18;
        }

        public KBase at(int i) {
            return new Second(Array.getInt(array, i));
        }

        public void serialise(OutputStream o) throws IOException {
            super.serialise(o);
            write(o, (byte) 0);
            write(o, getLength());
            for (int i = 0; i < getLength(); i++)
                write(o, Array.getInt(array, i));
        }
    }

    public static class KTimeVector extends KBaseVector {
        public String getDataType() {
            return "Time Vector";
        }

        public KTimeVector(int length) {
            super(int.class, length, "time", "");
            type = 19;
        }

        public KBase at(int i) {
            return new KTime(Array.getInt(array, i));
        }

        public void serialise(OutputStream o) throws IOException {
            super.serialise(o);
            write(o, (byte) 0);
            write(o, getLength());
            for (int i = 0; i < getLength(); i++)
                write(o, Array.getInt(array, i));
        }
    }

    public static class KBooleanVector extends KBaseVector {
        public String getDataType() {
            return "Boolean Vector";
        }

        public KBooleanVector(int length) {
            super(boolean.class, length, "boolean", "b");
            type = 1;
        }

        public KBase at(int i) {
            return new KBoolean(Array.getBoolean(array, i));
        }

        public void serialise(OutputStream o) throws IOException {
            super.serialise(o);
            write(o, (byte) 0);
            write(o, getLength());
            for (int i = 0; i < getLength(); i++)
                write(o, (byte) (Array.getBoolean(array, i) ? 1 : 0));
        }

        @Override
        protected StringBuilder formatVector(StringBuilder builder, boolean showType) {
            if (getLength() == 0) builder.append("`boolean$()");
            else {
                if (getLength() == 1) builder.append(enlist);
                for (int i = 0; i < getLength(); i++)
                    builder.append(Array.getBoolean(array, i) ? "1" : "0");
                builder.append("b");
            }
            return builder;
        }
    }

    public static class KByteVector extends KBaseVector {
        public String getDataType() {
            return "Byte Vector";
        }

        public KByteVector(int length) {
            super(byte.class, length, "byte", "x");
            type = 4;
        }

        public KBase at(int i) {
            return new KByte(Array.getByte(array, i));
        }

        public void serialise(OutputStream o) throws IOException {
            super.serialise(o);
            write(o, (byte) 0);
            write(o, getLength());
            for (int i = 0; i < getLength(); i++)
                write(o, Array.getByte(array, i));
        }

        @Override
        protected StringBuilder formatVector(StringBuilder builder, boolean showType) {
            if (getLength() == 0) builder.append("`byte$()");
            else {
                if (getLength() == 1) builder.append(enlist);
                builder.append("0x");
                for (int i = 0; i < getLength(); i++) {
                    byte b = Array.getByte(array, i);
                    builder.append(Integer.toHexString((b >> 4) & 0xf))
                            .append(Integer.toHexString(b & 0xf));
                }
            }
            return builder;
        }
    }

    public static class KSymbolVector extends KBaseVector {
        public String getDataType() {
            return "Symbol Vector";
        }

        public KSymbolVector(int length) {
            super(String.class, length, "symbol", "s");
            type = 11;
        }

        public KBase at(int i) {
            return new KSymbol((String) Array.get(array, i));
        }

        @Override
        protected StringBuilder formatVector(StringBuilder builder, boolean showType) {
            if (getLength() == 0) builder.append("`symbol$()");
            else {
                if (getLength() == 1) builder.append(enlist);
                for (int i = 0; i < getLength(); i++)
                    builder.append("`").append(Array.get(array, i));
            }
            return builder;
        }
    }

    public static class KCharacterVector extends KBaseVector {
        public String getDataType() {
            return "Character Vector";
        }


        public KCharacterVector(char[] ca) {
            super(char.class, ca.length, "char", "c");
            System.arraycopy(ca, 0, array, 0, ca.length);
            type = 10;
        }

        public KCharacterVector(String s) {
            this(s.toCharArray());
        }

        public KBase at(int i) {
            return new KCharacter(Array.getChar(array, i));
        }

        public void serialise(OutputStream o) throws IOException {
            super.serialise(o);
            byte[] b = new String((char[]) array).getBytes(Config.getInstance().getEncoding());
            write(o, (byte) 0);
            write(o, b.length);
            o.write(b);
        }

        @Override
        protected StringBuilder formatVector(StringBuilder builder, boolean showType) {
            if (getLength() == 1) builder.append(enlist);

            if (showType) builder.append("\"");
            for (int i = 0; i < getLength(); i++)
                builder.append(Array.getChar(array, i));
            if (showType) builder.append("\"");
            return builder;
        }
    }
}
