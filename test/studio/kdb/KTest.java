package studio.kdb;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Array;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class KTest {

    private void check(K.KBase base, String expectedNoType, String expectedWithType) {
        String actualNoType = base.toString(false);
        String actualWithType = base.toString(true);
        //uncomment below for easy debugging
//        System.out.println("\"" + actualNoType + "\", \"" + actualWithType + "\"");
        assertEquals(expectedNoType, actualNoType, "Test to not show type");
        assertEquals(expectedWithType, actualWithType, "Test to show type");
    }


    private K.KBaseVector vector(Class clazz, Object... values) throws Exception {
        K.KBaseVector baseVector = (K.KBaseVector) clazz.getConstructor(int.class).newInstance(values.length);
        Object anArray = baseVector.getArray();
        for (int index=0; index < values.length; index++) {
            Array.set(anArray, index, values[index]);
        }
        return baseVector;
    }


    @Test
    public void testIntegerToString() throws Exception {
        check(new K.KInteger(-123), "-123", "-123i");
        check(new K.KInteger(-Integer.MAX_VALUE), "-0W", "-0Wi");
        check(new K.KInteger(Integer. MAX_VALUE), "0W", "0Wi");
        check(new K.KInteger(Integer.MIN_VALUE), "0N", "0Ni");

        check(vector(K.KIntVector.class, -10, 10, 3), "-10 10 3", "-10 10 3i");
        check(vector(K.KIntVector.class), "`int$()", "`int$()");
        check(vector(K.KIntVector.class, 0), "enlist 0", "enlist 0i");
        check(vector(K.KIntVector.class, 5, Integer.MIN_VALUE, Integer.MAX_VALUE, -Integer.MAX_VALUE), "5 0N 0W -0W", "5 0N 0W -0Wi");
    }

    @Test
    public void testLongToString() throws Exception {
        check(new K.KLong(-123456789), "-123456789", "-123456789");
        check(new K.KLong(-Long.MAX_VALUE), "-0W", "-0W");
        check(new K.KLong(Long. MAX_VALUE), "0W", "0W");
        check(new K.KLong(Long.MIN_VALUE), "0N", "0N");

        check(vector(K.KLongVector.class, -10, 10, 3), "-10 10 3", "-10 10 3");
        check(vector(K.KLongVector.class), "`long$()", "`long$()");
        check(vector(K.KLongVector.class, 0), "enlist 0", "enlist 0");
        check(vector(K.KLongVector.class, 5, Long.MIN_VALUE, Long.MAX_VALUE, -Long.MAX_VALUE), "5 0N 0W -0W", "5 0N 0W -0W");
    }

    @Test
    public void testShortToString() throws Exception {
        check(new K.KShort((short)-123), "-123", "-123h");
        check(new K.KShort((short) -32767 ), "-0W", "-0Wh");
        check(new K.KShort(Short.MAX_VALUE), "0W", "0Wh");
        check(new K.KShort(Short.MIN_VALUE), "0N", "0Nh");

        check(vector(K.KShortVector.class, (short)-10, (short)10, (short)3), "-10 10 3", "-10 10 3h");
        check(vector(K.KShortVector.class), "`short$()", "`short$()");
        check(vector(K.KShortVector.class, (short)0), "enlist 0", "enlist 0h");
        check(vector(K.KShortVector.class, (short)5, Short.MIN_VALUE, Short.MAX_VALUE, (short)-Short.MAX_VALUE), "5 0N 0W -0W", "5 0N 0W -0Wh");
    }

    @Test
    public void testByteToString() throws Exception {
        check(new K.KByte((byte)123), "0x7b", "0x7b");
        check(new K.KByte((byte)0 ), "0x00", "0x00");
        check(new K.KByte((byte)-1 ), "0xff", "0xff");
        check(new K.KByte((byte)127), "0x7f", "0x7f");
        check(new K.KByte((byte)-128), "0x80", "0x80");
        check(new K.KByte((byte)-127), "0x81", "0x81");

        check(vector(K.KByteVector.class, (byte)-10, (byte)10, (byte)3), "0xf60a03", "0xf60a03");
        check(vector(K.KByteVector.class), "`byte$()", "`byte$()");
        check(vector(K.KByteVector.class, (byte)0), "enlist 0x00", "enlist 0x00");
        check(vector(K.KByteVector.class, (byte)5, (byte)-127, (byte)128, (byte)0), "0x05818000", "0x05818000");
    }


    @Test
    public void testDoubleToString() throws Exception {
        check(new K.KDouble(-1.23), "-1.23", "-1.23f");
        check(new K.KDouble(3), "3", "3f");
        check(new K.KDouble(0), "0", "0f");
        check(new K.KDouble(Double.POSITIVE_INFINITY ), "0w", "0wf");
        check(new K.KDouble(Double.NEGATIVE_INFINITY), "-0w", "-0wf");
        check(new K.KDouble(Double.NaN), "0n", "0nf");

        check(vector(K.KDoubleVector.class, (double)-10, (double)10, (double)3), "-10 10 3", "-10 10 3f");
        check(vector(K.KDoubleVector.class, (double)-10, 10.1, (double)3), "-10 10.1 3", "-10 10.1 3f");
        check(vector(K.KDoubleVector.class), "`float$()", "`float$()");
        check(vector(K.KDoubleVector.class, (double)0), "enlist 0", "enlist 0f");
        check(vector(K.KDoubleVector.class, (double)5, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NaN), "5 -0w 0w 0n", "5 -0w 0w 0nf");
    }

    @Test
    public void testFloatToString() throws Exception {
        check(new K.KFloat(-1.23f), "-1.23", "-1.23e");
        check(new K.KFloat(3), "3", "3e");
        check(new K.KFloat(0), "0", "0e");
        check(new K.KFloat(Float.POSITIVE_INFINITY ), "0w", "0we");
        check(new K.KFloat(Float.NEGATIVE_INFINITY), "-0w", "-0we");
        check(new K.KFloat(Float.NaN), "0N", "0Ne");

        check(vector(K.KFloatVector.class, -10f, 10f, 3f), "-10 10 3", "-10 10 3e");
        check(vector(K.KFloatVector.class, -10f, 10.1f, 3f), "-10 10.1000004 3", "-10 10.1000004 3e");
        check(vector(K.KFloatVector.class), "`real$()", "`real$()");
        check(vector(K.KFloatVector.class, 0f), "enlist 0", "enlist 0e");
        check(vector(K.KFloatVector.class, 5f, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NaN), "5 -0w 0w 0N", "5 -0w 0w 0Ne");
    }

    @Test
    public void testBooleanToString() throws Exception {
        check(new K.KBoolean(false), "0", "0b");
        check(new K.KBoolean(true), "1", "1b");

        check(vector(K.KBooleanVector.class, true, false), "10b", "10b");
        check(vector(K.KBooleanVector.class), "`boolean$()", "`boolean$()");
        check(vector(K.KBooleanVector.class, true), "enlist 1b", "enlist 1b");
    }

    @Test
    public void testCharacterToString() throws Exception {
        check(new K.KCharacter(' '), " ", "\" \"");
        check(new K.KCharacter('a'), "a", "\"a\"");

        check(new K.KCharacterVector(" a"), " a", "\" a\"");
        check(new K.KCharacterVector(""), "", "\"\"");
        check(new K.KCharacterVector("a"), "enlist a", "enlist \"a\"");
    }

    @Test
    public void testSymbolToString() throws Exception {
        check(new K.KSymbol(""), "", "`");
        check(new K.KSymbol("a"), "a", "`a");
        check(new K.KSymbol("ab"), "ab", "`ab");
        check(new K.KSymbol(" "), " ", "` ");

        check(vector(K.KSymbolVector.class, "b", "aa"), "`b`aa", "`b`aa");
        check(vector(K.KSymbolVector.class), "`symbol$()", "`symbol$()");
        check(vector(K.KSymbolVector.class, "", " ", "ab"), "`` `ab", "`` `ab");
    }

    @Test
    public void testGuidToString() throws Exception {
        check(new K.KGuid(new UUID(12345,-987654)), "00000000-0000-3039-ffff-fffffff0edfa", "00000000-0000-3039-ffff-fffffff0edfa");
        check(new K.KGuid(new UUID(0,0)), "00000000-0000-0000-0000-000000000000", "00000000-0000-0000-0000-000000000000");

        check(vector(K.KGuidVector.class, new UUID(1,-1), new UUID(0,1), new UUID(-1,0)), "00000000-0000-0001-ffff-ffffffffffff 00000000-0000-0000-0000-000000000001 ffffffff-ffff-ffff-0000-000000000000", "00000000-0000-0001-ffff-ffffffffffff 00000000-0000-0000-0000-000000000001 ffffffff-ffff-ffff-0000-000000000000");
        check(vector(K.KGuidVector.class), "`guid$()", "`guid$()");
        check(vector(K.KGuidVector.class, new UUID(0,0)), "enlist 00000000-0000-0000-0000-000000000000", "enlist 00000000-0000-0000-0000-000000000000");
    }

    @Test
    public void testTimestampToString() throws Exception {
        check(new K.KTimestamp(-123456789), "1999.12.31 23:59:59.876543211", "1999.12.31 23:59:59.876543211");
        check(new K.KTimestamp(123456), "2000.01.01 00:00:00.000123456", "2000.01.01 00:00:00.000123456");
        check(new K.KTimestamp(-Long.MAX_VALUE), "-0Wp", "-0Wp");
        check(new K.KTimestamp(Long. MAX_VALUE), "0Wp", "0Wp");
        check(new K.KTimestamp(Long.MIN_VALUE), "0Np", "0Np");

        check(vector(K.KTimestampVector.class, -10, 10, 3), "1999.12.31 23:59:59.999999990 2000.01.01 00:00:00.000000010 2000.01.01 00:00:00.000000003", "1999.12.31 23:59:59.999999990 2000.01.01 00:00:00.000000010 2000.01.01 00:00:00.000000003");
        check(vector(K.KTimestampVector.class), "`timestamp$()", "`timestamp$()");
        check(vector(K.KTimestampVector.class, 0), "enlist 2000.01.01 00:00:00.000000000", "enlist 2000.01.01 00:00:00.000000000");
        check(vector(K.KTimestampVector.class, 5, Long.MIN_VALUE, Long.MAX_VALUE, -Long.MAX_VALUE), "2000.01.01 00:00:00.000000005 0Np 0Wp -0Wp", "2000.01.01 00:00:00.000000005 0Np 0Wp -0Wp");
    }

    @Test
    public void testTimespanToString() throws Exception {
        check(new K.KTimespan(-765432123456789l), "-8D20:37:12.123456789", "-8D20:37:12.123456789");
        check(new K.KTimespan(123456), "00:00:00.000123456", "00:00:00.000123456");
        check(new K.KTimespan(-Long.MAX_VALUE), "-0Wn", "-0Wn");
        check(new K.KTimespan(Long. MAX_VALUE), "0Wn", "0Wn");
        check(new K.KTimespan(Long.MIN_VALUE), "0Nn", "0Nn");

        check(vector(K.KTimespanVector.class, -10, 10, 3), "-00:00:00.000000010 00:00:00.000000010 00:00:00.000000003", "-00:00:00.000000010 00:00:00.000000010 00:00:00.000000003");
        check(vector(K.KTimespanVector.class), "`timespan$()", "`timespan$()");
        check(vector(K.KTimespanVector.class, 0), "enlist 00:00:00.000000000", "enlist 00:00:00.000000000");
        check(vector(K.KTimespanVector.class, 5, Long.MIN_VALUE, Long.MAX_VALUE, -Long.MAX_VALUE), "00:00:00.000000005 0Nn 0Wn -0Wn", "00:00:00.000000005 0Nn 0Wn -0Wn");
    }

    @Test
    public void testDateToString() throws Exception {
        check(new K.KDate(-1234), "1996.08.15", "1996.08.15");
        check(new K.KDate(123456), "2338.01.05", "2338.01.05");
        check(new K.KDate(-Integer.MAX_VALUE), "-0Wd", "-0Wd");
        check(new K.KDate(Integer. MAX_VALUE), "0Wd", "0Wd");
        check(new K.KDate(Integer.MIN_VALUE), "0Nd", "0Nd");

        check(vector(K.KDateVector.class, -10, 10, 3), "1999.12.22 2000.01.11 2000.01.04", "1999.12.22 2000.01.11 2000.01.04");
        check(vector(K.KDateVector.class), "`date$()", "`date$()");
        check(vector(K.KDateVector.class, 0), "enlist 2000.01.01", "enlist 2000.01.01");
        check(vector(K.KDateVector.class, 5, Integer.MIN_VALUE, Integer.MAX_VALUE, -Integer.MAX_VALUE), "2000.01.06 0Nd 0Wd -0Wd", "2000.01.06 0Nd 0Wd -0Wd");
    }

    @Test
    public void testTimeToString() throws Exception {
        //@ToDo Fix me
        check(new K.KTime(-1234567890), "17:03:52.110", "17:03:52.110");
        check(new K.KTime(323456789), "17:50:56.789", "17:50:56.789");

        check(new K.KTime(-Integer.MAX_VALUE), "-0Wt", "-0Wt");
        check(new K.KTime(Integer. MAX_VALUE), "0Wt", "0Wt");
        check(new K.KTime(Integer.MIN_VALUE), "0Nt", "0Nt");

        check(vector(K.KTimeVector.class, -10, 10, 3), "23:59:59.990 00:00:00.010 00:00:00.003", "23:59:59.990 00:00:00.010 00:00:00.003");
        check(vector(K.KTimeVector.class), "`time$()", "`time$()");
        check(vector(K.KTimeVector.class, 0), "enlist 00:00:00.000", "enlist 00:00:00.000");
        check(vector(K.KTimeVector.class, 5, Integer.MIN_VALUE, Integer.MAX_VALUE, -Integer.MAX_VALUE), "00:00:00.005 0Nt 0Wt -0Wt", "00:00:00.005 0Nt 0Wt -0Wt");
    }

    @Test
    public void testMonthToString() throws Exception {
        check(new K.Month(-12345), "0971.04", "0971.04m");
        check(new K.Month(123456), "12288.01", "12288.01m");
        check(new K.Month(-Integer.MAX_VALUE), "-0W", "-0Wm");
        check(new K.Month(Integer. MAX_VALUE), "0W", "0Wm");
        check(new K.Month(Integer.MIN_VALUE), "0N", "0Nm");

        check(vector(K.KMonthVector.class, -10, 10, 3), "1999.03 2000.11 2000.04", "1999.03 2000.11 2000.04m");
        check(vector(K.KMonthVector.class), "`month$()", "`month$()");
        check(vector(K.KMonthVector.class, 0), "enlist 2000.01", "enlist 2000.01m");
        check(vector(K.KMonthVector.class, 5, Integer.MIN_VALUE, Integer.MAX_VALUE, -Integer.MAX_VALUE), "2000.06 0N 0W -0W", "2000.06 0N 0W -0Wm");
    }

    @Test
    public void testMinuteToString() throws Exception {
        //@ToDo Fix me
        check(new K.Minute(-12345), "-205:-45", "-205:-45");

        check(new K.Minute(123456), "2057:36", "2057:36");
        check(new K.Minute(-Integer.MAX_VALUE), "-0Wu", "-0Wu");
        check(new K.Minute(Integer. MAX_VALUE), "0Wu", "0Wu");
        check(new K.Minute(Integer.MIN_VALUE), "0Nu", "0Nu");

        check(vector(K.KMinuteVector.class, -10, 10, 3), "00:-10 00:10 00:03", "00:-10 00:10 00:03");
        check(vector(K.KMinuteVector.class), "`minute$()", "`minute$()");
        check(vector(K.KMinuteVector.class, 0), "enlist 00:00", "enlist 00:00");
        check(vector(K.KMinuteVector.class, 5, Integer.MIN_VALUE, Integer.MAX_VALUE, -Integer.MAX_VALUE), "00:05 0Nu 0Wu -0Wu", "00:05 0Nu 0Wu -0Wu");
    }

    @Test
    public void testSecondToString() throws Exception {
        //@ToDo Fix me
        check(new K.Second(-12345), "-03:-25:-45", "-03:-25:-45");

        check(new K.Second(123456), "34:17:36", "34:17:36");
        check(new K.Second(-Integer.MAX_VALUE), "-0Wv", "-0Wv");
        check(new K.Second(Integer. MAX_VALUE), "0Wv", "0Wv");
        check(new K.Second(Integer.MIN_VALUE), "0Nv", "0Nv");

        check(vector(K.KSecondVector.class, -10, 10, 3), "00:00:-10 00:00:10 00:00:03", "00:00:-10 00:00:10 00:00:03");
        check(vector(K.KSecondVector.class), "`second$()", "`second$()");
        check(vector(K.KSecondVector.class, 0), "enlist 00:00:00", "enlist 00:00:00");
        check(vector(K.KSecondVector.class, 5, Integer.MIN_VALUE, Integer.MAX_VALUE, -Integer.MAX_VALUE), "00:00:05 0Nv 0Wv -0Wv", "00:00:05 0Nv 0Wv -0Wv");
    }

    @Test
    public void testDatetimeToString() throws Exception {
        check(new K.KDatetime(-123456.789), "1661.12.26 05:03:50.401", "1661.12.26 05:03:50.401z");
        check(new K.KDatetime(123.456), "2000.05.03 10:56:38.400", "2000.05.03 10:56:38.400z");
        check(new K.KDatetime(Double.NEGATIVE_INFINITY), "-0w", "-0wz");
        check(new K.KDatetime(Double.POSITIVE_INFINITY), "0w", "0wz");
        check(new K.KDatetime(Double.NaN), "0N", "0Nz");

        check(vector(K.KDatetimeVector.class, -10.0, 10.0, 3.0), "1999.12.22 00:00:00.000 2000.01.11 00:00:00.000 2000.01.04 00:00:00.000", "1999.12.22 00:00:00.000 2000.01.11 00:00:00.000 2000.01.04 00:00:00.000z");
        check(vector(K.KDatetimeVector.class), "`datetime$()", "`datetime$()");
        check(vector(K.KDatetimeVector.class, 0.0), "enlist 2000.01.01 00:00:00.000", "enlist 2000.01.01 00:00:00.000z");
        check(vector(K.KDatetimeVector.class, 5.0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NaN), "2000.01.06 00:00:00.000 -0w 0w 0N", "2000.01.06 00:00:00.000 -0w 0w 0Nz");
    }


    @Test
    public void testListToString() throws Exception {
        check(vector(K.KList.class), "()", "()");
        check(vector(K.KList.class, new K.KLong(10), new K.KLong(Long.MAX_VALUE)), "(10;0W)", "(10;0W)");
        check(vector(K.KList.class, new K.KLong(10), new K.KInteger(10)), "(10;10)", "(10;10i)");
        check(vector(K.KList.class, new K.KLong(10), new K.KInteger(10), vector(K.KList.class, new K.KDouble(1.1))), "(10;10;enlist 1.1)", "(10;10i;enlist 1.1f)");
    }


    @Test
    public void testOtherToString() throws Exception {
        K.Function funcUnary = new K.Function(new K.KCharacterVector("{1+x}"));
        K.Function funcUnary2 = new K.Function(new K.KCharacterVector("{2*x}"));
        K.Function funcBinary = new K.Function(new K.KCharacterVector("{x+y}"));

        check(funcUnary,"{1+x}", "{1+x}");
        check(funcBinary,"{x+y}", "{x+y}");

        check(new K.FEachLeft(funcBinary), "{x+y}\\:", "{x+y}\\:");
        check(new K.FEachRight(funcBinary), "{x+y}/:", "{x+y}/:");
        check(new K.Feach(funcBinary), "{x+y}'", "{x+y}'");
        check(new K.Fover(funcBinary), "{x+y}/", "{x+y}/");
        check(new K.Fscan(funcBinary), "{x+y}\\", "{x+y}\\");
        check(new K.FPrior(funcBinary), "{x+y}':", "{x+y}':");

        check(new K.FComposition(new Object[] {funcUnary, funcBinary}), "","");
        check(new K.Projection((K.KList)vector(K.KList.class, funcBinary, new K.KLong(1), new K.UnaryPrimitive(-1))), "{x+y}[1;]", "{x+y}[1;]");
        check(new K.Projection((K.KList)vector(K.KList.class, funcBinary, new K.UnaryPrimitive(-1), new K.KLong(1))), "{x+y}[;1]", "{x+y}[;1]");

        check(new K.BinaryPrimitive(15), "~", "~");
        check(new K.UnaryPrimitive(0), "::", "::");
        check(new K.UnaryPrimitive(41), "enlist", "enlist");

        //the output from +1
        check(new K.Projection((K.KList)vector(K.KList.class, new K.BinaryPrimitive(1), new K.KLong(1))), "+[1]", "+[1]");
        //output from '[;]
        check(new K.Projection((K.KList)vector(K.KList.class, new K.TernaryOperator(0), new K.UnaryPrimitive(-1), new K.UnaryPrimitive(-1))), "'[;]", "'[;]");

    }



}
