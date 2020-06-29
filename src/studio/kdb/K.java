package studio.kdb;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.Array;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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


        /*        public String toString(boolean showType) {
                    return "";
                }
                ;
        */
        public String toString() {
            return toString(true);
        }

        public boolean isNull() {
            return false;
        }

        private byte attr;

        public byte getAttr() {
            return attr;
        }

        public void setAttr(byte attr) {
            this.attr = attr;
        }

        private static String[] sAttr = new String[]{"", "`s#", "`u#", "`p#", "`g#"};

        public String toString(boolean showType) {
            if (attr <= sAttr.length)
                return sAttr[attr];
            return "";
        }

        public void toString(Writer w, boolean showType) throws IOException {
            w.write(toString(showType));
        }

        //      public KBase(int type){this.type=type;}
    }

    public static class Adverb extends KBase {
        public String getDataType() {
            return "Adverb";
        }

        protected K.KBase o;

        public Adverb(K.KBase o) {
            this.o = o;
        }

        public Object getObject() {
            return o;
        }
    }

    public static class BinaryPrimitive extends Primitive {
        private static final String[] ops = {":", "+", "-", "*", "%", "&", "|", "^", "=", "<", ">", "$", ",", "#", "_", "~", "!", "?", "@", ".", "0:", "1:", "2:", "in", "within", "like", "bin", "ss", "insert", "wsum", "wavg", "div", "xexp", "setenv", "binr", "cov", "cor"};

        public String getDataType() {
            return "Binary Primitive";
        }

        public BinaryPrimitive(int i) {
            super(ops, i);
            type = 102;
        }

        public void toString(Writer w, boolean showType) throws IOException {
            w.write(getPrimitive());
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

        public Object[] getObjs() {
            return objs;
        }
    }

    public static class FEachLeft extends Adverb {
        public FEachLeft(K.KBase o) {
            super(o);
            type = 111;
        }

        public void toString(Writer w, boolean showType) throws IOException {
            o.toString(w, showType);
            w.write("\\:");
        }
    }

    public static class FEachRight extends Adverb {
        public FEachRight(K.KBase o) {
            super(o);
            type = 110;
        }

        public void toString(Writer w, boolean showType) throws IOException {
            o.toString(w, showType);
            w.write("/:");
        }
    }

    public static class FPrior extends Adverb {
        public FPrior(K.KBase o) {
            super(o);
            type = 109;
        }

        public void toString(Writer w, boolean showType) throws IOException {
            o.toString(w, showType);
            w.write("':");
        }
    }

    public static class Feach extends Adverb {
        public Feach(K.KBase o) {
            super(o);
            type = 106;
        }

        public void toString(Writer w, boolean showType) throws IOException {
            o.toString(w, showType);
            w.write("'");
        }
    }

    public static class Fover extends Adverb {
        public Fover(K.KBase o) {
            super(o);
            type = 107;
        }

        public void toString(Writer w, boolean showType) throws IOException {
            o.toString(w, showType);
            w.write("/");
        }
    }

    public static class Fscan extends Adverb {
        public Fscan(KBase o) {
            super(o);
            type = 108;
            this.o = o;
        }

        public void toString(Writer w, boolean showType) throws IOException {
            o.toString(w, showType);
            w.write("\\");
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

        public String getBody() {
            return body;
        }

        public String toString(boolean showType) {
            return body;
        }

        public void toString(Writer w, boolean showType) throws IOException {
            w.write(body);
        }
    }

    public static class Primitive extends KBase {
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

        public void toString(Writer w, boolean showType) throws IOException {
            boolean listProjection = false;
            if ((objs.getLength() > 0) && (objs.at(0) instanceof UnaryPrimitive)) {
                UnaryPrimitive up = (UnaryPrimitive) objs.at(0);
                if (up.getPrimitiveAsInt() == 41) // plist
                    listProjection = true;
            }

            if (listProjection) {
                w.write("(");
                for (int i = 1; i < objs.getLength(); i++) {
                    if (i > 1)
                        w.write(";");

                    objs.at(i).toString(w, showType);
                }
                w.write(")");
            } else {
                boolean isFunction = false;

                for (int i = 0; i < objs.getLength(); i++) {
                    if (i == 0)
                        if ((objs.at(0) instanceof Function) || (objs.at(0) instanceof UnaryPrimitive) || (objs.at(0) instanceof BinaryPrimitive))
                            isFunction = true;
                        else
                            w.write("(");

                    if (i > 0)
                        if (i == 1)
                            if (isFunction)
                                w.write("[");
                            else
                                w.write(";");
                        else
                            w.write(";");

                    objs.at(i).toString(w, showType);
                }

                if (isFunction)
                    w.write("]");
                else
                    w.write(")");
            }
        }
    }

    public static class TernaryOperator extends KBase {
        public String getDataType() {
            return "Ternary Operator";
        }

        private static final Map map = new HashMap();

        public static void init(char[] ops, int[] values) {
            for (int i = 0; i < values.length; i++)
                map.put(values[i], ops[i]);
        }

        private int primitive;
        private char charVal = ' ';


        static {
            init("'/\\".toCharArray(), new int[]{0, 1, 2});
        }

        public TernaryOperator(int i) {
            type = 103;
            primitive = i;
            Character c = (Character) map.get(i);
            if (c != null)
                charVal = c;
        }

        public char getPrimitive() {
            return charVal;
        }

        public int getPrimitiveAsInt() {
            return primitive;
        }

        public void toString(Writer w, boolean showType) throws IOException {
            w.write(charVal);
        }
    }

    public static class UnaryPrimitive extends Primitive {
        private static String[] ops = {"::", "+:", "-:", "*:", "%:", "&:", "|:", "^:", "=:", "<:", ">:", "$:", ",:", "#:", "_:", "~:", "!:", "?:", "@:", ".:", "0::", "1::", "2::", "avg", "last", "sum", "prd", "min", "max", "exit", "getenv", "abs", "sqrt", "log", "exp", "sin", "asin", "cos", "acos", "tan", "atan", "enlist", "var", "dev", "hopen"};

        public UnaryPrimitive(int i) {
            super(ops, i);
            type = 101;
        }

        public void toString(Writer w, boolean showType) throws IOException {
            if (getPrimitiveAsInt() == -1)
                return;
            w.write(getPrimitive());
        }
    }

    public static class Variable extends KBase {
        public String getDataType() {
            return "Variable";
        }

        public String getContext() {
            return context;
        }

        public void setContext(String context) {
            this.context = context;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getType() {
            return type;
        }

        public void setType(short type) {
            this.type = type;
        }

        private String name;
        private String context;
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

        public String toString(boolean showType) {
            String s = b ? "1" : "0";
            if (showType)
                s += "b";
            return s;
        }

        public void toString(Writer w, boolean showType) throws IOException {
            w.write(toString(showType));
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

        public String toString(boolean showType) {
            return "0x" + Integer.toHexString((b >> 4) & 0xf) + Integer.toHexString(b & 0xf);
        }

        public void toString(Writer w, boolean showType) throws IOException {
            w.write(toString(showType));
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

        public String toString(boolean showType) {
            String t;
            if (s == Short.MIN_VALUE)
                t = "0N";
            else if (s == Short.MAX_VALUE)
                t = "0W";
            else if (s == -Short.MAX_VALUE)
                t = "-0W";
            else
                t = Short.toString(s);
            if (showType)
                t += "h";
            return t;
        }

        public void toString(Writer w, boolean showType) throws IOException {
            w.write(toString(showType));
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

        public String toString(boolean showType) {
            String s;
            if (isNull())
                s = "0N";
            else if (i == Integer.MAX_VALUE)
                s = "0W";
            else if (i == -Integer.MAX_VALUE)
                s = "-0W";
            else
                s = Integer.toString(i);
            if (showType)
                s += "i";
            return s;
        }

        public void toString(Writer w, boolean showType) throws IOException {
            w.write(toString(showType));
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

        public String toString(boolean showType) {
            return s;
        }

        public boolean isNull() {
            return s.length() == 0;
        }

        public void toString(Writer w, boolean showType) throws IOException {
            if (showType)
                w.write("`");
            w.write(s);
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

        public String toString(boolean showType) {
            String s;
            if (isNull())
                s = "0N";
            else if (j == Long.MAX_VALUE)
                s = "0W";
            else if (j == -Long.MAX_VALUE)
                s = "-0W";
            else {
                s = Long.toString(j);
            }
            if (showType)
                s += "j";

            return s;
        }

        public void toString(Writer w, boolean showType) throws IOException {
            w.write(toString(showType));
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

        public String toString(boolean showType) {
            if (showType)
                return "\"" + c + "\"";
            else
                return "" + c;
        }

        public void toString(Writer w, boolean showType) throws IOException {
            w.write(toString(showType));
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

        public String toString(boolean showType) {
            if (isNull())
                return "0ne";
            else if (f == Float.POSITIVE_INFINITY)
                return "0we";
            else if (f == Float.NEGATIVE_INFINITY)
                return "-0we";
            else {
                String s = Config.getInstance().getNumberFormat().format(f);
                if (showType) {
                    double epsilon = 1e-9;
                    double diff = f - Math.round(f);
                    if ((diff < epsilon) && (diff > -epsilon))
                        s += "e";
                }
                return s;
            }
        }

        public void toString(Writer w, boolean showType) throws IOException {
            w.write(toString(showType));
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

        public String toString(boolean showType) {
            if (isNull())
                return "0n";
            else if (d == Double.POSITIVE_INFINITY)
                return "0w";
            else if (d == Double.NEGATIVE_INFINITY)
                return "-0w";
            else {
                String s = Config.getInstance().getNumberFormat().format(d);
                if (showType) {
                    double epsilon = 1e-9;
                    double diff = d - Math.round(d);
                    if ((diff < epsilon) && (diff > -epsilon))
                        s += "f";
                }
                return s;
            }
        }

        public void toString(Writer w, boolean showType) throws IOException {
            w.write(toString(showType));
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

        public String toString(boolean showType) {
            if (isNull())
                return "0Nd";
            else if (date == Integer.MAX_VALUE)
                return "0Wd";
            else if (date == -Integer.MAX_VALUE)
                return "-0Wd";
            else
                return sd("yyyy.MM.dd", new Date(86400000L * (date + 10957)));
        }

        public void toString(Writer w, boolean showType) throws IOException {
            w.write(toString(showType));
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

        public String toString(boolean showType) {
            return uuid.toString();
        }

        public void toString(Writer w, boolean showType) throws IOException {
            w.write(toString(showType));
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

        public String toString(boolean showType) {
            if (isNull())
                return "0Nt";
            else if (time == Integer.MAX_VALUE)
                return "0Wt";
            else if (time == -Integer.MAX_VALUE)
                return "-0Wt";
            else
                return sd("HH:mm:ss.SSS", new Time(time));
        }

        public void toString(Writer w, boolean showType) throws IOException {
            w.write(toString(showType));
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

        public String toString(boolean showType) {
            if (isNull())
                return "0nz";
            else if (time == Double.POSITIVE_INFINITY)
                return "0wz";
            else if (time == Double.NEGATIVE_INFINITY)
                return "-0wz";
            else
                return sd("yyyy.MM.dd HH:mm:ss.SSS", toTimestamp());
        }

        public void toString(Writer w, boolean showType) throws IOException {
            w.write(toString(showType));
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

        public String toString(boolean showType) {
            if (isNull())
                return "0Np";
            else if (time == Long.MAX_VALUE)
                return "0Wp";
            else if (time == -Long.MAX_VALUE)
                return "-0Wp";
            else {
                Timestamp ts = toTimestamp();
                return sd("yyyy.MM.dd HH:mm:ss.", ts) + nsFormatter.format(ts.getNanos());
            }
        }

        public void toString(Writer w, boolean showType) throws IOException {
            w.write(toString(showType));
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

        public K.KBase x;
        public K.KBase y;

        public Dict(K.KBase X, K.KBase Y) {
            type = 99;
            x = X;
            y = Y;
        }

        public void upsert(K.Dict upd) {
            //if dict is not table
            if (!(x instanceof K.Flip) || !(y instanceof K.Flip))
                return;
            //if upd is not table
            if (!(upd.x instanceof K.Flip) || !(upd.y instanceof K.Flip))
                return;
            Flip cx = (K.Flip) x;
            Flip cy = (K.Flip) y;
            Flip updx = (K.Flip) upd.x;
            Flip updy = (K.Flip) upd.y;
            cx.append(updx);
            cy.append(updy);
        }

        public void toString(Writer w, boolean showType) throws IOException {
            boolean useBrackets = getAttr() != 0 || x instanceof Flip;
            super.toString(w, showType);
            if (useBrackets)
                w.write("(");
            x.toString(w, showType);
            if (useBrackets)
                w.write(")");
            w.write("!");
            y.toString(w, showType);
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

        public void toString(Writer w, boolean showType) throws IOException {
            boolean usebracket = x.getLength() == 1;
            w.write(flip);
            if (usebracket)
                w.write("(");
            x.toString(w, showType);
            if (usebracket)
                w.write(")");
            w.write("!");
            y.toString(w, showType);
        }

        public void append(Flip nf) {
            for (int i = 0; i < y.getLength(); i++)
                ((KBaseVector) y.at(i)).append((KBaseVector) nf.y.at(i));
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

        public String toString(boolean showType) {
            if (isNull())
                return "0Nm";
            else if (i == Integer.MAX_VALUE)
                return "0Wm";
            else if (i == -Integer.MAX_VALUE)
                return "-0Wm";
            else {
                int m = i + 24000, y = m / 12;
                String s = i2(y / 100) + i2(y % 100) + "." + i2(1 + m % 12);
                if (showType)
                    s += "m";
                return s;
            }
        }

        public Date toDate() {
            int m = i + 24000, y = m / 12;
            Calendar cal = Calendar.getInstance();
            cal.set(y, m, 01);
            return cal.getTime();
        }

        public void toString(Writer w, boolean showType) throws IOException {
            w.write(toString(showType));
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

        public String toString(boolean showType) {
            if (isNull())
                return "0Nu";
            else if (i == Integer.MAX_VALUE)
                return "0Wu";
            else if (i == -Integer.MAX_VALUE)
                return "-0Wu";
            else
                return i2(i / 60) + ":" + i2(i % 60);
        }

        public void toString(Writer w, boolean showType) throws IOException {
            w.write(toString(showType));
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

        public String toString(boolean showType) {
            if (isNull())
                return "0Nv";
            else if (i == Integer.MAX_VALUE)
                return "0Wv";
            else if (i == -Integer.MAX_VALUE)
                return "-0Wv";
            else
                return new Minute(i / 60).toString() + ':' + i2(i % 60);
        }

        public void toString(Writer w, boolean showType) throws IOException {
            w.write(toString(showType));
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

        public String toString(boolean showType) {
            if (isNull())
                return "0Nn";
            else if (j == Long.MAX_VALUE)
                return "0Wn";
            else if (j == -Long.MAX_VALUE)
                return "-0Wn";
            else {
                String s = "";
                long jj = j;
                if (jj < 0) {
                    jj = -jj;
                    s = "-";
                }
                int d = ((int) (jj / 86400000000000L));
                if (d != 0)
                    s += d + "D";
                return s + i2((int) ((jj % 86400000000000L) / 3600000000000L)) +
                        ":" + i2((int) ((jj % 3600000000000L) / 60000000000L)) +
                        ":" + i2((int) ((jj % 60000000000L) / 1000000000L)) +
                        "." + nsFormatter.format((int) (jj % 1000000000L));
            }
        }

        public void toString(Writer w, boolean showType) throws IOException {
            w.write(toString(showType));
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

        protected KBaseVector(Class klass, int length) {
            //array=Array.newInstance(klass, calcCapacity(length));
            array = Array.newInstance(klass, length);
            this.length = length;
        }

        public abstract KBase at(int i);

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

        protected int calcCapacity(int length) {
            return (int) (1.1 * length);
        }

        public void append(KBaseVector x) {
            if ((x.getLength() + getLength()) > Array.getLength(getArray())) {
                int newLength = Array.getLength(getArray()) + x.getLength();
                Object tmp = Array.newInstance(getArray().getClass().getComponentType(), 2 * calcCapacity(newLength));
                System.arraycopy(getArray(), 0, tmp, 0, getLength());
                array = tmp;
            }
            System.arraycopy(x.getArray(), 0, getArray(), getLength(), x.getLength());
            length += x.getLength();
        }
    }

    public static class KShortVector extends KBaseVector {
        public String getDataType() {
            return "Short Vector";
        }

        public KShortVector(int length) {
            super(short.class, length);
            type = 5;
        }

        public KBase at(int i) {
            return new KShort(Array.getShort(array, i));
        }

        public void toString(Writer w, boolean showType) throws IOException {
            w.write(super.toString(showType));

            if (getLength() == 0)
                w.write("`short$()");
            else {
                if (getLength() == 1)
                    w.write(enlist);
                for (int i = 0; i < getLength(); i++) {
                    if (i > 0)
                        w.write(" ");
                    short v = Array.getShort(array, i);
                    if (v == Short.MIN_VALUE)
                        w.write("0N");
                    else if (v == Short.MAX_VALUE)
                        w.write("0W");
                    else if (v == -Short.MAX_VALUE)
                        w.write("-0W");
                    else {
                        w.write("" + v);
                    }
                }
                if (showType)
                    w.write("h");
            }
        }
    }

    public static class KIntVector extends KBaseVector {
        public String getDataType() {
            return "Int Vector";
        }

        public KIntVector(int length) {
            super(int.class, length);
            type = 6;
        }

        public KBase at(int i) {
            return new KInteger(Array.getInt(array, i));
        }

        public void toString(Writer w, boolean showType) throws IOException {
            w.write(super.toString(showType));

            if (getLength() == 0)
                w.write("`int$()");
            else {
                if (getLength() == 1)
                    w.write(enlist);
                for (int i = 0; i < getLength(); i++) {
                    if (i > 0)
                        w.write(" ");
                    int v = Array.getInt(array, i);
                    if (v == Integer.MIN_VALUE)
                        w.write("0N");
                    else if (v == Integer.MAX_VALUE)
                        w.write("0W");
                    else if (v == -Integer.MAX_VALUE)
                        w.write("-0W");
                    else
                        w.write("" + v);
                }
                if (showType)
                    w.write("i");
            }
        }
    }

    public static class KList extends KBaseVector {
        public String getDataType() {
            return "List";
        }

        public KList(int length) {
            super(KBase.class, length);
            type = 0;
        }

        public KBase at(int i) {
            return (KBase) Array.get(array, i);
        }

        public void toString(Writer w, boolean showType) throws IOException {
            w.write(super.toString(showType));

            if (getLength() == 1)
                w.write(enlist);
            else
                w.write("(");
            for (int i = 0; i < getLength(); i++) {
                if (i > 0)
                    w.write(";");
                at(i).toString(w, showType);
            }
            if (getLength() != 1)
                w.write(")");
        }
    }

    public static class KDoubleVector extends KBaseVector {
        public String getDataType() {
            return "Double Vector";
        }

        public KDoubleVector(int length) {
            super(double.class, length);
            type = 9;
        }

        public KBase at(int i) {
            return new KDouble(Array.getDouble(array, i));
        }

        public void toString(Writer w, boolean showType) throws IOException {
            w.write(super.toString(showType));

            if (getLength() == 0)
                w.write("`float$()");
            else {
                if (getLength() == 1)
                    w.write(enlist);

                boolean printedP = false;
                NumberFormat nf = Config.getInstance().getNumberFormat();
                for (int i = 0; i < getLength(); i++) {
                    double d = Array.getDouble(array, i);
                    if (i > 0)
                        w.write(" ");
                    if (Double.isNaN(d)) {
                        w.write("0n");
                        printedP = true;
                    } else if (d == Double.POSITIVE_INFINITY) {
                        w.write("0w");
                        printedP = true;
                    } else if (d == Double.NEGATIVE_INFINITY) {
                        w.write("-0w");
                        printedP = true;
                    } else {
                        double epsilon = 1e-9;
                        double diff = d - Math.round(d);
                        if (!((diff < epsilon) && (diff > -epsilon)))
                            printedP = true;
                        w.write(nf.format(d));
                    }
                }
                if (!printedP)
                    w.write("f");
            }
        }
    }

    public static class KFloatVector extends KBaseVector {
        public String getDataType() {
            return "Float Vector";
        }

        public KFloatVector(int length) {
            super(float.class, length);
            type = 8;
        }

        public KBase at(int i) {
            return new KFloat(Array.getFloat(array, i));
        }

        public void toString(Writer w, boolean showType) throws IOException {
            w.write(super.toString(showType));

            if (getLength() == 0)
                w.write("`real$()");
            else {
                if (getLength() == 1)
                    w.write(enlist);

                boolean printedP = false;
                NumberFormat nf = Config.getInstance().getNumberFormat();
                for (int i = 0; i < getLength(); i++) {
                    float d = Array.getFloat(array, i);
                    if (i > 0)
                        w.write(" ");
                    if (Float.isNaN(d)) {
                        w.write("0N");
                        printedP = true;
                    } else if (d == Float.POSITIVE_INFINITY) {
                        w.write("0W");
                        printedP = true;
                    } else if (d == Float.NEGATIVE_INFINITY) {
                        w.write("-0W");
                        printedP = true;
                    } else {
                        if (d != ((int) d))
                            printedP = true;
                        w.write(nf.format(d));
                    }
                }
                if (!printedP)
                    w.write("e");
            }
        }
    }

    public static class KLongVector extends KBaseVector {
        public String getDataType() {
            return "Long Vector";
        }

        public KLongVector(int length) {
            super(long.class, length);
            type = 7;
        }

        public KBase at(int i) {
            return new KLong(Array.getLong(array, i));
        }

        public void toString(Writer w, boolean showType) throws IOException {
            w.write(super.toString(showType));

            if (getLength() == 0)
                w.write("`long$()");
            else {
                if (getLength() == 1)
                    w.write(enlist);
                for (int i = 0; i < getLength(); i++) {
                    if (i > 0)
                        w.write(" ");
                    long v = Array.getLong(array, i);
                    if (v == Long.MIN_VALUE)
                        w.write("0N");
                    else if (v == Long.MAX_VALUE)
                        w.write("0W");
                    else if (v == -Long.MAX_VALUE)
                        w.write("-0W");
                    else {
                        w.write("" + v);
                    }
                }
                if (showType)
                    w.write("j");
            }
        }
    }

    public static class KMonthVector extends KBaseVector {
        public String getDataType() {
            return "Month Vector";
        }

        public KMonthVector(int length) {
            super(int.class, length);
            type = 13;
        }

        public KBase at(int i) {
            return new Month(Array.getInt(array, i));
        }

        public void toString(Writer w, boolean showType) throws IOException {
            w.write(super.toString(showType));

            if (getLength() == 0)
                w.write("`month$()");
            else {
                if (getLength() == 1)
                    w.write(enlist);
                for (int i = 0; i < getLength(); i++) {
                    if (i > 0)
                        w.write(" ");
                    int v = Array.getInt(array, i);
                    if (v == Integer.MIN_VALUE)
                        w.write("0N");
                    else if (v == Integer.MAX_VALUE)
                        w.write("0W");
                    else if (v == -Integer.MAX_VALUE)
                        w.write("-0W");
                    else {
                        int m = v + 24000, y = m / 12;
                        String s = i2(y / 100) + i2(y % 100) + "." + i2(1 + m % 12);
                        w.write(s);
                    }
                }
                if (showType)
                    w.write("m");
            }
        }
    }

    public static class KDateVector extends KBaseVector {
        public String getDataType() {
            return "Date Vector";
        }

        public KDateVector(int length) {
            super(int.class, length);
            type = 14;
        }

        public KBase at(int i) {
            return new KDate(Array.getInt(array, i));
        }

        public void toString(Writer w, boolean showType) throws IOException {
            w.write(super.toString(showType));

            if (getLength() == 0)
                w.write("`date$()");
            else {
                boolean printD = true;
                if (getLength() == 1)
                    w.write(enlist);
                for (int i = 0; i < getLength(); i++) {
                    if (i > 0)
                        w.write(" ");
                    int v = Array.getInt(array, i);
                    if (v == Integer.MIN_VALUE)
                        w.write("0N");
                    else if (v == Integer.MAX_VALUE)
                        w.write("0W");
                    else if (v == -Integer.MAX_VALUE)
                        w.write("-0W");
                    else {
                        printD = false;
                        w.write(sd("yyyy.MM.dd", new Date(86400000L * (v + 10957))));
                    }
                }
                if (printD)
                    w.write("d");
            }
        }
    }

    public static class KGuidVector extends KBaseVector {
        public String getDataType() {
            return "Guid Vector";
        }

        public KGuidVector(int length) {
            super(UUID.class, length);
            type = 2;
        }

        public KBase at(int i) {
            return new KGuid((UUID) Array.get(array, i));
        }

        public void toString(Writer w, boolean showType) throws IOException {
            w.write(super.toString(showType));

            if (getLength() == 0)
                w.write("`guid$()");
            else {
                if (getLength() == 1)
                    w.write(enlist);
                for (int i = 0; i < getLength(); i++) {
                    if (i > 0)
                        w.write(" ");
                    w.write(((UUID) Array.get(array, i)).toString());
                }
            }
        }
    }

    public static class KMinuteVector extends KBaseVector {
        public String getDataType() {
            return "Minute Vector";
        }

        public KMinuteVector(int length) {
            super(int.class, length);
            type = 17;
        }

        public KBase at(int i) {
            return new Minute(Array.getInt(array, i));
        }

        public void toString(Writer w, boolean showType) throws IOException {
            w.write(super.toString(showType));

            if (getLength() == 0)
                w.write("`minute$()");
            else {
                if (getLength() == 1)
                    w.write(enlist);
                for (int i = 0; i < getLength(); i++) {
                    if (i > 0)
                        w.write(" ");
                    int v = Array.getInt(array, i);
                    if (v == Integer.MIN_VALUE)
                        w.write("0Nu");
                    else if (v == Integer.MAX_VALUE)
                        w.write("0Wu");
                    else if (v == -Integer.MAX_VALUE)
                        w.write("-0Wu");
                    else
                        w.write(i2(v / 60) + ":" + i2(v % 60));
                }
            }
        }
    }

    public static class KDatetimeVector extends KBaseVector {
        public String getDataType() {
            return "Datetime Vector";
        }

        public KDatetimeVector(int length) {
            super(double.class, length);
            type = 15;
        }

        public KBase at(int i) {
            return new KDatetime(Array.getDouble(array, i));
        }

        public void toString(Writer w, boolean showType) throws IOException {
            w.write(super.toString(showType));

            if (getLength() == 0)
                w.write("`datetime$()");
            else {
                boolean printZ = true;
                if (getLength() == 1)
                    w.write(enlist);
                for (int i = 0; i < getLength(); i++) {
                    if (i > 0)
                        w.write(" ");
                    double d = Array.getDouble(array, i);
                    if (i > 0)
                        w.write(" ");
                    if (Double.isNaN(d))
                        w.write("0N");
                    else if (d == Double.POSITIVE_INFINITY)
                        w.write("0w");
                    else if (d == Double.NEGATIVE_INFINITY)
                        w.write("-0w");
                    else {
                        printZ = false;
                        w.write(sd("yyyy.MM.dd HH:mm:ss.SSS", new Timestamp(((long) (.5 + 8.64e7 * (d + 10957))))));
                    }
                }
                if (printZ)
                    w.write("z");
            }
        }
    }

    public static class KTimestampVector extends KBaseVector {
        public String getDataType() {
            return "Timestamp Vector";
        }

        public KTimestampVector(int length) {
            super(long.class, length);
            type = 12;
        }

        public KBase at(int i) {
            return new KTimestamp(Array.getLong(array, i));
        }

        public void toString(Writer w, boolean showType) throws IOException {
            w.write(super.toString(showType));

            if (getLength() == 0)
                w.write("`timestamp$()");
            else {
                if (getLength() == 1)
                    w.write(enlist);
                for (int i = 0; i < getLength(); i++) {
                    if (i > 0)
                        w.write(" ");
                    w.write(at(i).toString(false));
                }
            }
        }
    }

    public static class KTimespanVector extends KBaseVector {
        public String getDataType() {
            return "Timespan Vector";
        }

        public KTimespanVector(int length) {
            super(long.class, length);
            type = 16;
        }

        public KBase at(int i) {
            return new KTimespan(Array.getLong(array, i));
        }

        public void toString(Writer w, boolean showType) throws IOException {
            w.write(super.toString(showType));

            if (getLength() == 0)
                w.write("`timespan$()");
            else {
                if (getLength() == 1)
                    w.write(enlist);
                for (int i = 0; i < getLength(); i++) {
                    if (i > 0)
                        w.write(" ");
                    w.write(at(i).toString(false));
                }
            }
        }
    }

    public static class KSecondVector extends KBaseVector {
        public String getDataType() {
            return "Second Vector";
        }

        public KSecondVector(int length) {
            super(int.class, length);
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

        public void toString(Writer w, boolean showType) throws IOException {
            w.write(super.toString(showType));

            if (getLength() == 0)
                w.write("`second$()");
            else {
                if (getLength() == 1)
                    w.write(enlist);
                for (int i = 0; i < getLength(); i++) {
                    if (i > 0)
                        w.write(" ");
                    int v = Array.getInt(array, i);
                    if (v == Integer.MIN_VALUE)
                        w.write("0Nv");
                    else if (v == Integer.MAX_VALUE)
                        w.write("0Wv");
                    else if (v == -Integer.MAX_VALUE)
                        w.write("-0Wv");
                    else
                        w.write(new Minute(v / 60).toString() + ':' + i2(v % 60));
                }
            }
        }
    }

    public static class KTimeVector extends KBaseVector {
        public String getDataType() {
            return "Time Vector";
        }

        public KTimeVector(int length) {
            super(int.class, length);
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

        public void toString(Writer w, boolean showType) throws IOException {
            w.write(super.toString(showType));

            if (getLength() == 0)
                w.write("`time$()");
            else {
                if (getLength() == 1)
                    w.write(enlist);
                for (int i = 0; i < getLength(); i++) {
                    if (i > 0)
                        w.write(" ");
                    int v = Array.getInt(array, i);
                    if (v == Integer.MIN_VALUE)
                        w.write("0Nt");
                    else if (v == Integer.MAX_VALUE)
                        w.write("0Wt");
                    else if (v == -Integer.MAX_VALUE)
                        w.write("-0Wt");
                    else
                        w.write(sd("HH:mm:ss.SSS", new Time(v)));
                }
            }
        }
    }

    public static class KBooleanVector extends KBaseVector {
        public String getDataType() {
            return "Boolean Vector";
        }

        public KBooleanVector(int length) {
            super(boolean.class, length);
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

        public void toString(Writer w, boolean showType) throws IOException {
            w.write(super.toString(showType));
            if (getLength() == 0)
                w.write("`boolean$()");
            else {
                if (getLength() == 1)
                    w.write(enlist);
                for (int i = 0; i < getLength(); i++)
                    w.write((Array.getBoolean(array, i) ? "1" : "0"));
                w.write("b");
            }
        }
    }

    public static class KByteVector extends KBaseVector {
        public String getDataType() {
            return "Byte Vector";
        }

        public KByteVector(int length) {
            super(byte.class, length);
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

        public void toString(Writer w, boolean showType) throws IOException {
            w.write(super.toString(showType));
            if (getLength() == 0)
                w.write("`byte$()");
            else {
                if (getLength() == 1)
                    w.write(enlist);

                w.write("0x");
                for (int i = 0; i < getLength(); i++) {
                    byte b = Array.getByte(array, i);
                    w.write(Integer.toHexString((b >> 4) & 0xf) + Integer.toHexString(b & 0xf));
                }
            }
        }
    }

    public static class KSymbolVector extends KBaseVector {
        public String getDataType() {
            return "Symbol Vector";
        }

        public KSymbolVector(int length) {
            super(String.class, length);
            type = 11;
        }

        public KBase at(int i) {
            return new KSymbol((String) Array.get(array, i));
        }

        public void toString(Writer w, boolean showType) throws IOException {
            w.write(super.toString(showType));
            if (getLength() == 0)
                w.write("0#`");
            else if (getLength() == 1)
                w.write(enlist);

            for (int i = 0; i < getLength(); i++)
                w.write("`" + Array.get(array, i));
        }
    }

    public static class KCharacterVector extends KBaseVector {
        public String getDataType() {
            return "Character Vector";
        }


        public KCharacterVector(int length) {
            super(char.class, length);
            type = 10;
        }

        public KCharacterVector(char[] ca) {
            super(char.class, ca.length);
            System.arraycopy(ca, 0, array, 0, ca.length);
            type = 10;
        }

        public KCharacterVector(String s) {
            super(char.class, s.toCharArray().length);
            System.arraycopy(s.toCharArray(), 0, array, 0, s.toCharArray().length);
            type = 10;
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

        public void toString(Writer w, boolean showType) throws IOException {
            w.write(super.toString(showType));
            if (getLength() == 1)
                w.write(enlist);

            if (showType)
                w.write("\"");
            for (int i = 0; i < getLength(); i++)
                w.write(Array.getChar(array, i));
            if (showType)
                w.write("\"");
        }

        public String toString(boolean showType) {
            CharArrayWriter w = new CharArrayWriter();
            try {
                toString(w, showType);
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
            return w.toString();
        }
    }

    public static String decode(KBase obj, boolean showType) {
        CharArrayWriter w = new CharArrayWriter();
        try {
            obj.toString(w, showType);
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }

        return w.toString();
    }
}
