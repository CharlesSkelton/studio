package studio.kdb;

import java.util.HashMap;
import java.util.Map;

public class QErrors {
    private static Map map = new HashMap();

    public static String lookup(String s) {
        return (String) map.get(s);
    }
    

    static {
        map.put("access","attempt to read files above directory, run system commands or failed usr/pwd");
        map.put("assign","attempt to assign a value to a reserved word");
        map.put("conn","too many incoming connections (1022 max)");
        map.put("domain","out of domain");
        map.put("glim","`g# limit, kdb+ currently limited to 99 concurrent `g#'s ");
        map.put("length","incompatible lengths, e.g. 1 2 3 4 + 1 2 3");
        map.put("limit","tried to generate a list longer than 2,000,000,000");
        map.put("loop","dependency loop");
        map.put("mismatch","columns that can't be aligned for R,R or K,K ");
        map.put("Mlim","more than 999 nested columns in splayed tables");
        map.put("nyi","not yet implemented - suggests the\noperation you are tying to do makes sense\nbut it has not yet been implemented");
        map.put("os","operating system error");
        map.put("pl","peach can't handle parallel lambda's (2.3 only)");
        map.put("Q7","nyi op on file nested array");
        map.put("rank","invalid rank or valence");
        map.put("splay","nyi op on splayed table");
        map.put("stack","ran out of stack space");
        map.put("stop","user interrupt(ctrl-c) or time limit (-T)");
        map.put("stype","invalid type used to signal");
        map.put("type","wrong type, e.g `a+1");
        map.put("value","no value");
        map.put("vd1","attempted multithread update");
        map.put("wsfull","malloc failed. ran out of swap (or addressability on 32bit). or hit -w limit.");
        map.put("branch","a branch(if;do;while;$[.;.;.]) more than 255 byte codes away");
        map.put("char","invalid character");
        map.put("constants","too many constants (max 96)");
        map.put("globals","too many global variables (32 max)");
        map.put("locals","too many local variables (24 max)");
        map.put("params","too many parameters (8 max)");
        map.put("u-fail","cannot apply `u# to data (not unique values), e.g `u#1 1");
        map.put("s-fail","cannot apply `s# to data (not ascending values) , e.g `s#2 1");
        map.put("noamend","can't change global state inside an amend");
        map.put("elim","more than 57 distinct enumerations");
    }
}
