/* Studio for kdb+ by Charles Skelton
   is licensed under a Creative Commons Attribution-Noncommercial-Share Alike 3.0 Germany License
   http://creativecommons.org/licenses/by-nc-sa/3.0
   except for the netbeans components which retain their original copyright notice
*/

package org.netbeans.editor.ext.q;

import java.util.HashMap;
import java.util.Map;
import org.netbeans.editor.Syntax;
import org.netbeans.editor.TokenID;

public class QSyntax extends Syntax
{
    private Map map;

    private void initMap()
    {
        map=new HashMap();
        Object o=new Object();

        String[] keywords=new String[]
        {
            "ej","aj0","insert","acos","exp","wavg","avgs","log","sin","cos","sqrt","exec","abs","neg","not","null","floor","string","reciprocal","ceiling","signum","div","mod","xbar","xlog","and","or","each","mmu","lsq","inv","md5","ltime","gtime","count","first","var","dev","med","cov","cor","all","any","rand","sums","prds","mins","maxs","fills","deltas","ratios","differ","prev","next","rank","reverse","iasc","idesc","asc","desc","msum","mcount","mavg","mdev","xrank","mmin","mmax","xprev","rotate","list","distinct","group","where","flip","type","key","til","get","value","attr","cut","set","upsert","raze","union","inter","except","cross","ss","sv","vs","sublist","read0","read1","hopen","hclose","hdel","hsym","hcount","peach","system","ltrim","rtrim","trim","lower","upper","ssr","view","tables","views","cols","xcols","keys","xkey","xcol","xasc","xdesc","fkeys","meta","uj","ij","lj","pj","aj","asof","fby","ungroup","xgroup","plist","enlist","txf","save","load","rsave","rload","show","csv","parse","eval","over","scan","select","from","where","within","update","in","delete","sum","avg","min","max","like","last","by","do","while","if","getenv","xexp"
        };

        //      String [] keywords= Config.getInstance().getQKeywords();
        for(int i=0;i<keywords.length;i++)
        {
            map.put(keywords[i],o);
        }
    }

    public TokenID parseToken()
    {
        int start=offset;
        Entry e=null;
        
        while(offset<stopOffset)
        {
            int documentPosition=stopPosition-(stopOffset-offset);

            char c=buffer[offset++];

            // hack to allow comment on first line
            if(documentPosition==0)
                if((c=='/')||(c=='\\') || (c=='-'))
                    state=30;
            
            boolean found=false;
            for(int i=0;i<entries.length;i++)
            {
                e=entries[i];
                if(e.state==state)
                {
                    for(int j=0;j<e.chars.length;j++)
                    {
                        if(e.chars[j]==c)
                        {
                            found=true;
                            break;
                        }
                    }
                    found=found || (e.chars.length==0);
                    if(found)
                    {
                        state=e.nextState;
                        if((e.action==ACTION_MATCHANDPUTBACK)||(e.action==ACTION_MATCHANDCONSUME))
                        {
                            if(e.action==ACTION_MATCHANDPUTBACK)
                                offset--;
                            if(e.tokenID==QTokenContext.IDENTIFIER)
                                if(map.get(new String(buffer,start,offset-start))!=null)
                                    return QTokenContext.KEYWORD;
                            return e.tokenID;
                        }
                        break;
                    }
                }
            }
            if(!found)
            {
                state=255;
                e=null;
            }
        }
        if(lastBuffer)
        {
            if(e!=null)
            {
                if(e.action==ACTION_LOOKSLIKE)
                    if(e.tokenID==QTokenContext.IDENTIFIER)
                        if(map.get(new String(buffer,start,offset-start))!=null)
                            return QTokenContext.KEYWORD;
                return e.tokenID;
            }
            else
                return QTokenContext.UNKNOWN;
        }
        return null;
    }
    private static final int ACTION_LOOKSLIKE=0;
    private static final int ACTION_MATCHANDCONSUME=1;
    private static final int ACTION_MATCHANDPUTBACK=2;

    public static class Entry
    {
        int state;
        char[] chars;
        int nextState;
        TokenID tokenID;
        int action;

        public Entry(int state,char[] c,int nextState,TokenID tokenID,int action)
        {
            this.state=state;
            this.chars=c;
            this.nextState=nextState;
            this.tokenID=tokenID;
            this.action=action;
        }
    };
    static final char[] whitespace=" \n\r\t".toCharArray();
    static final char[] brackets="[](){}".toCharArray();
    static final char[] operators="|/&^:!+-*%$=~#;@\\.><,?_'".toCharArray();
    static final char[] delimiters="` \n\r\t\"|/&^:![](){}+-*%$=~#;@\\.><,?_'".toCharArray();
    static final char[] digits="0123456789".toCharArray();
    static final char[] a2z="abcdefghijklmnopqrstuvwxyz".toCharArray();
    static final char[] A2Z="ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();    // INIT state is defined as -1
    public static final Entry[] entries=new Entry[]
    {
        new Entry(INIT,"\"".toCharArray(),9,QTokenContext.CHAR_VECTOR,ACTION_LOOKSLIKE),
        new Entry(INIT,"`".toCharArray(),8,QTokenContext.SYMBOL,ACTION_LOOKSLIKE),
        new Entry(INIT,"\n".toCharArray(),30,QTokenContext.WHITESPACE,ACTION_MATCHANDCONSUME),
        new Entry(INIT,"\t ".toCharArray(),5,QTokenContext.WHITESPACE,ACTION_MATCHANDCONSUME),
        new Entry(INIT,".".toCharArray(),26,QTokenContext.IDENTIFIER,ACTION_LOOKSLIKE),
        new Entry(INIT,"0".toCharArray(),0,QTokenContext.INTEGER,ACTION_LOOKSLIKE),
        new Entry(INIT,"1".toCharArray(),41,QTokenContext.INTEGER,ACTION_LOOKSLIKE),
        new Entry(INIT,"23456789".toCharArray(),40,QTokenContext.INTEGER,ACTION_LOOKSLIKE),
        new Entry(INIT,a2z,7,QTokenContext.IDENTIFIER,ACTION_LOOKSLIKE),
        new Entry(INIT,A2Z,7,QTokenContext.IDENTIFIER,ACTION_LOOKSLIKE),
        new Entry(INIT,whitespace,13,QTokenContext.WHITESPACE,ACTION_LOOKSLIKE),
     //   new Entry(INIT,"-".toCharArray(),79,QTokenContext.OPERATOR,ACTION_LOOKSLIKE),
        new Entry(INIT,operators,79,QTokenContext.OPERATOR,ACTION_MATCHANDCONSUME),
        new Entry(INIT,brackets,81,QTokenContext.OPERATOR,ACTION_MATCHANDCONSUME),
        new Entry(0,":".toCharArray(),INIT,QTokenContext.OPERATOR,ACTION_MATCHANDCONSUME),
        new Entry(0,"D".toCharArray(),95,QTokenContext.TIMESPAN,ACTION_LOOKSLIKE),
        new Entry(0,".".toCharArray(),27,QTokenContext.FLOAT,ACTION_LOOKSLIKE),
        new Entry(0,"i".toCharArray(),24,QTokenContext.INTEGER,ACTION_LOOKSLIKE),
        new Entry(0,"j".toCharArray(),2,QTokenContext.LONG,ACTION_LOOKSLIKE),
        new Entry(0,"h".toCharArray(),3,QTokenContext.SHORT,ACTION_LOOKSLIKE),
        new Entry(0,"e".toCharArray(),28,QTokenContext.REAL,ACTION_LOOKSLIKE),
        new Entry(0,"f".toCharArray(),16,QTokenContext.FLOAT,ACTION_LOOKSLIKE),
        new Entry(0,"b".toCharArray(),17,QTokenContext.BOOLEAN,ACTION_LOOKSLIKE),
        new Entry(0,"p".toCharArray(),84,QTokenContext.TIMESTAMP,ACTION_LOOKSLIKE),
        new Entry(0,"t".toCharArray(),18,QTokenContext.TIME,ACTION_LOOKSLIKE),
        new Entry(0,"v".toCharArray(),19,QTokenContext.MINUTE,ACTION_LOOKSLIKE),
        new Entry(0,"u".toCharArray(),20,QTokenContext.SECOND,ACTION_LOOKSLIKE),
        new Entry(0,"wWnN".toCharArray(),1,QTokenContext.INTEGER,ACTION_LOOKSLIKE),
        new Entry(0,"01".toCharArray(),25,QTokenContext.INTEGER,ACTION_LOOKSLIKE),
        new Entry(0,"xX".toCharArray(),14,QTokenContext.BYTE,ACTION_LOOKSLIKE),
        new Entry(0,"23456789".toCharArray(),4,QTokenContext.INTEGER,ACTION_LOOKSLIKE),
        new Entry(0,delimiters,INIT,QTokenContext.INTEGER,ACTION_MATCHANDPUTBACK),
        new Entry(1,delimiters,INIT,QTokenContext.INTEGER,ACTION_MATCHANDPUTBACK),
        new Entry(1,"D".toCharArray(),95,QTokenContext.TIMESPAN,ACTION_LOOKSLIKE),
        new Entry(1,"j".toCharArray(),2,QTokenContext.LONG,ACTION_LOOKSLIKE),
        new Entry(1,"h".toCharArray(),3,QTokenContext.SHORT,ACTION_LOOKSLIKE),
        new Entry(1,"e".toCharArray(),28,QTokenContext.REAL,ACTION_LOOKSLIKE),
        new Entry(1,"f".toCharArray(),16,QTokenContext.FLOAT,ACTION_LOOKSLIKE),
        new Entry(1,"b".toCharArray(),17,QTokenContext.BOOLEAN,ACTION_LOOKSLIKE),
        new Entry(1,"t".toCharArray(),18,QTokenContext.TIME,ACTION_LOOKSLIKE),
        new Entry(1,"v".toCharArray(),19,QTokenContext.MINUTE,ACTION_LOOKSLIKE),
        new Entry(1,"u".toCharArray(),20,QTokenContext.SECOND,ACTION_LOOKSLIKE),
        new Entry(1,"d".toCharArray(),21,QTokenContext.DATE,ACTION_LOOKSLIKE),
        new Entry(1,"n".toCharArray(),105,QTokenContext.TIMESPAN,ACTION_LOOKSLIKE),
        new Entry(1,"p".toCharArray(),84,QTokenContext.TIMESTAMP,ACTION_LOOKSLIKE),
        new Entry(1,"z".toCharArray(),22,QTokenContext.DATETIME,ACTION_LOOKSLIKE),
        new Entry(1,"m".toCharArray(),23,QTokenContext.MONTH,ACTION_LOOKSLIKE),
        new Entry(2,delimiters,INIT,QTokenContext.LONG,ACTION_MATCHANDPUTBACK),
        new Entry(3,delimiters,INIT,QTokenContext.SHORT,ACTION_MATCHANDPUTBACK),
        new Entry(4,":".toCharArray(),44,QTokenContext.MINUTE,ACTION_LOOKSLIKE),
        new Entry(4,".".toCharArray(),27,QTokenContext.FLOAT,ACTION_LOOKSLIKE),
        new Entry(4,digits,43,QTokenContext.INTEGER,ACTION_LOOKSLIKE),
        new Entry(4,delimiters,INIT,QTokenContext.INTEGER,ACTION_MATCHANDPUTBACK),
        new Entry(4,"D".toCharArray(),95,QTokenContext.TIMESPAN,ACTION_LOOKSLIKE),
        new Entry(4,"j".toCharArray(),2,QTokenContext.LONG,ACTION_LOOKSLIKE),
        new Entry(4,"h".toCharArray(),3,QTokenContext.SHORT,ACTION_LOOKSLIKE),
        new Entry(4,"e".toCharArray(),28,QTokenContext.REAL,ACTION_LOOKSLIKE),
        new Entry(4,"f".toCharArray(),16,QTokenContext.FLOAT,ACTION_LOOKSLIKE),
        new Entry(4,"t".toCharArray(),18,QTokenContext.TIME,ACTION_LOOKSLIKE),
        new Entry(4,"v".toCharArray(),19,QTokenContext.MINUTE,ACTION_LOOKSLIKE),
        new Entry(4,"u".toCharArray(),20,QTokenContext.SECOND,ACTION_LOOKSLIKE),
        new Entry(4,"p".toCharArray(),84,QTokenContext.TIMESTAMP,ACTION_LOOKSLIKE),
        new Entry(4,"i".toCharArray(),24,QTokenContext.INTEGER,ACTION_LOOKSLIKE),
        new Entry(5,"-".toCharArray(),80,QTokenContext.OPERATOR,ACTION_LOOKSLIKE),
        new Entry(5,"/".toCharArray(),6,QTokenContext.EOL_COMMENT,ACTION_LOOKSLIKE),
        new Entry(5,"".toCharArray(),INIT,QTokenContext.WHITESPACE,ACTION_MATCHANDPUTBACK),
        new Entry(6,"\n".toCharArray(),INIT,QTokenContext.EOL_COMMENT,ACTION_MATCHANDPUTBACK),
        new Entry(6,"".toCharArray(),6,QTokenContext.EOL_COMMENT,ACTION_LOOKSLIKE),
        new Entry(7,"_.".toCharArray(),7,QTokenContext.IDENTIFIER,ACTION_LOOKSLIKE),
        new Entry(7,delimiters,INIT,QTokenContext.IDENTIFIER,ACTION_MATCHANDPUTBACK),
        new Entry(7,digits,7,QTokenContext.IDENTIFIER,ACTION_LOOKSLIKE),
        new Entry(7,a2z,7,QTokenContext.IDENTIFIER,ACTION_LOOKSLIKE),
        new Entry(7,A2Z,7,QTokenContext.IDENTIFIER,ACTION_LOOKSLIKE),
        new Entry(8,"/:_.`".toCharArray(),8,QTokenContext.SYMBOL,ACTION_LOOKSLIKE),
        new Entry(8,a2z,8,QTokenContext.SYMBOL,ACTION_LOOKSLIKE),
        new Entry(8,A2Z,8,QTokenContext.SYMBOL,ACTION_LOOKSLIKE),
        new Entry(8,digits,8,QTokenContext.SYMBOL,ACTION_LOOKSLIKE),
        new Entry(8,delimiters,INIT,QTokenContext.SYMBOL,ACTION_MATCHANDPUTBACK),
        new Entry(9,"\"".toCharArray(),INIT,QTokenContext.CHAR_VECTOR,ACTION_MATCHANDCONSUME),
        new Entry(9,"\\".toCharArray(),10,QTokenContext.CHAR_VECTOR,ACTION_LOOKSLIKE),
        new Entry(9,"".toCharArray(),9,QTokenContext.CHAR_VECTOR,ACTION_LOOKSLIKE),
        new Entry(10,"".toCharArray(),9,QTokenContext.CHAR_VECTOR,ACTION_LOOKSLIKE),
      //  new Entry(11,"".toCharArray(),INIT,QTokenContext.CHAR_VECTOR,ACTION_MATCHANDPUTBACK),
        //        new Entry(12,"".toCharArray(),INIT,null,QTokenContext.OPERATOR),
        new Entry(13,"".toCharArray(),INIT,QTokenContext.WHITESPACE,ACTION_MATCHANDPUTBACK),
        new Entry(14,digits,14,QTokenContext.BYTE,ACTION_LOOKSLIKE),
        new Entry(14,"abcdefABCDEF".toCharArray(),14,QTokenContext.BYTE,ACTION_LOOKSLIKE),
        new Entry(14,delimiters,INIT,QTokenContext.BYTE,ACTION_MATCHANDPUTBACK),
        new Entry(15,delimiters,INIT,QTokenContext.REAL,ACTION_MATCHANDPUTBACK),
        new Entry(16,delimiters,INIT,QTokenContext.FLOAT,ACTION_MATCHANDPUTBACK),
        new Entry(17,delimiters,INIT,QTokenContext.BOOLEAN,ACTION_MATCHANDPUTBACK),
        new Entry(18,delimiters,INIT,QTokenContext.TIME,ACTION_MATCHANDPUTBACK),
        new Entry(19,delimiters,INIT,QTokenContext.MINUTE,ACTION_MATCHANDPUTBACK),
        new Entry(20,delimiters,INIT,QTokenContext.SECOND,ACTION_MATCHANDPUTBACK),
        new Entry(21,delimiters,INIT,QTokenContext.DATE,ACTION_MATCHANDPUTBACK),
        new Entry(22,delimiters,INIT,QTokenContext.DATETIME,ACTION_MATCHANDPUTBACK),
        new Entry(23,delimiters,INIT,QTokenContext.MONTH,ACTION_MATCHANDPUTBACK),
        new Entry(24,delimiters,INIT,QTokenContext.INTEGER,ACTION_MATCHANDPUTBACK),
        new Entry(25,":".toCharArray(),44,QTokenContext.MINUTE,ACTION_LOOKSLIKE),
        new Entry(25,".".toCharArray(),27,QTokenContext.FLOAT,ACTION_LOOKSLIKE),
        new Entry(25,"01".toCharArray(),25,QTokenContext.INTEGER,ACTION_LOOKSLIKE),
        new Entry(25,"23456789".toCharArray(),43,QTokenContext.INTEGER,ACTION_LOOKSLIKE),
        new Entry(25,delimiters,INIT,QTokenContext.INTEGER,ACTION_MATCHANDPUTBACK),
        new Entry(25,"D".toCharArray(),95,QTokenContext.TIMESPAN,ACTION_LOOKSLIKE),
        new Entry(25,"j".toCharArray(),2,QTokenContext.LONG,ACTION_LOOKSLIKE),
        new Entry(25,"h".toCharArray(),3,QTokenContext.SHORT,ACTION_LOOKSLIKE),
        new Entry(25,"e".toCharArray(),15,QTokenContext.REAL,ACTION_LOOKSLIKE),
        new Entry(25,"f".toCharArray(),16,QTokenContext.FLOAT,ACTION_LOOKSLIKE),
        new Entry(25,"t".toCharArray(),18,QTokenContext.TIME,ACTION_LOOKSLIKE),
        new Entry(25,"v".toCharArray(),19,QTokenContext.MINUTE,ACTION_LOOKSLIKE),
        new Entry(25,"u".toCharArray(),20,QTokenContext.SECOND,ACTION_LOOKSLIKE),
        new Entry(25,"i".toCharArray(),24,QTokenContext.INTEGER,ACTION_LOOKSLIKE),
        new Entry(25,"b".toCharArray(),17,QTokenContext.BOOLEAN,ACTION_LOOKSLIKE),
        new Entry(26,a2z,7,QTokenContext.IDENTIFIER,ACTION_LOOKSLIKE),
        new Entry(26,A2Z,7,QTokenContext.IDENTIFIER,ACTION_LOOKSLIKE),
        new Entry(26,digits,27,QTokenContext.FLOAT,ACTION_LOOKSLIKE),
        new Entry(26,"".toCharArray(),INIT,QTokenContext.OPERATOR,ACTION_MATCHANDPUTBACK),
        new Entry(27,digits,27,QTokenContext.FLOAT,ACTION_LOOKSLIKE),
        new Entry(27,"e".toCharArray(),28,QTokenContext.REAL,ACTION_LOOKSLIKE),
        new Entry(27,"p".toCharArray(),84,QTokenContext.TIMESTAMP,ACTION_LOOKSLIKE),
        new Entry(27,".".toCharArray(),255,QTokenContext.UNKNOWN,ACTION_LOOKSLIKE),
        new Entry(27,delimiters,INIT,QTokenContext.FLOAT,ACTION_MATCHANDPUTBACK),
        new Entry(28,digits,29,QTokenContext.FLOAT,ACTION_LOOKSLIKE),
        new Entry(28,"-".toCharArray(),29,QTokenContext.FLOAT,ACTION_LOOKSLIKE),
        new Entry(28,".".toCharArray(),255,QTokenContext.UNKNOWN,ACTION_LOOKSLIKE),
        new Entry(28,delimiters,INIT,QTokenContext.REAL,ACTION_MATCHANDPUTBACK),
        new Entry(29,digits,29,QTokenContext.FLOAT,ACTION_LOOKSLIKE),
        new Entry(29,"f".toCharArray(),82,QTokenContext.FLOAT,ACTION_LOOKSLIKE),
        new Entry(29,".".toCharArray(),255,QTokenContext.UNKNOWN,ACTION_LOOKSLIKE),
        new Entry(29,delimiters,INIT,QTokenContext.FLOAT,ACTION_MATCHANDPUTBACK),
        new Entry(30,"/".toCharArray(),33,QTokenContext.EOL_COMMENT,ACTION_LOOKSLIKE),
        new Entry(30,"-".toCharArray(),80,QTokenContext.OPERATOR,ACTION_LOOKSLIKE),
        new Entry(30,"\\".toCharArray(),31,QTokenContext.EOL_COMMENT,ACTION_LOOKSLIKE),
        new Entry(30,"".toCharArray(),INIT,QTokenContext.WHITESPACE,ACTION_MATCHANDPUTBACK),
        new Entry(31,"abcdeflopstuvwxzBCPSTW".toCharArray(),38,QTokenContext.COMMAND,ACTION_LOOKSLIKE),
        new Entry(31," \t".toCharArray(),37,QTokenContext.EOL_COMMENT,ACTION_LOOKSLIKE),
        new Entry(31,"\n".toCharArray(),32,QTokenContext.EOL_COMMENT,ACTION_LOOKSLIKE),
        new Entry(31,"".toCharArray(),39,QTokenContext.EOL_COMMENT,ACTION_LOOKSLIKE),
        new Entry(32,"".toCharArray(),32,QTokenContext.EOL_COMMENT,ACTION_LOOKSLIKE),
        new Entry(33," \t".toCharArray(),33,QTokenContext.EOL_COMMENT,ACTION_LOOKSLIKE),
        new Entry(33,"\n".toCharArray(),34,QTokenContext.EOL_COMMENT,ACTION_LOOKSLIKE),
        new Entry(33,"".toCharArray(),6,QTokenContext.EOL_COMMENT,ACTION_LOOKSLIKE),
        new Entry(34,"\n".toCharArray(),34,QTokenContext.EOL_COMMENT,ACTION_LOOKSLIKE),
        new Entry(34,"\\".toCharArray(),36,QTokenContext.EOL_COMMENT,ACTION_LOOKSLIKE),
        new Entry(34,"".toCharArray(),35,QTokenContext.EOL_COMMENT,ACTION_LOOKSLIKE),
        new Entry(35,"\n".toCharArray(),34,QTokenContext.EOL_COMMENT,ACTION_LOOKSLIKE),
        new Entry(35,"".toCharArray(),35,QTokenContext.EOL_COMMENT,ACTION_LOOKSLIKE),
        new Entry(36," \t".toCharArray(),36,QTokenContext.EOL_COMMENT,ACTION_LOOKSLIKE),
        new Entry(36,"\n".toCharArray(),INIT,QTokenContext.EOL_COMMENT,ACTION_MATCHANDPUTBACK),
        new Entry(36,"".toCharArray(),35,QTokenContext.EOL_COMMENT,ACTION_LOOKSLIKE),
        new Entry(37," \t".toCharArray(),37,QTokenContext.EOL_COMMENT,ACTION_LOOKSLIKE),
        new Entry(37,"\n".toCharArray(),32,QTokenContext.EOL_COMMENT,ACTION_LOOKSLIKE),
        new Entry(37,"".toCharArray(),6,QTokenContext.EOL_COMMENT,ACTION_LOOKSLIKE),
        new Entry(38,"\n \t".toCharArray(),INIT,QTokenContext.COMMAND,ACTION_MATCHANDPUTBACK),
        new Entry(38,"".toCharArray(),39,QTokenContext.SYSTEM,ACTION_LOOKSLIKE),
        new Entry(39,"\n".toCharArray(),INIT,QTokenContext.SYSTEM,ACTION_MATCHANDPUTBACK),
        new Entry(39,"".toCharArray(),39,QTokenContext.SYSTEM,ACTION_LOOKSLIKE),
        new Entry(40,".".toCharArray(),27,QTokenContext.FLOAT,ACTION_LOOKSLIKE),
        new Entry(40,digits,4,QTokenContext.INTEGER,ACTION_LOOKSLIKE),
        new Entry(40,delimiters,INIT,QTokenContext.INTEGER,ACTION_MATCHANDPUTBACK),
        new Entry(40,"D".toCharArray(),95,QTokenContext.TIMESPAN,ACTION_LOOKSLIKE),
        new Entry(40,"j".toCharArray(),2,QTokenContext.LONG,ACTION_LOOKSLIKE),
        new Entry(40,"h".toCharArray(),3,QTokenContext.SHORT,ACTION_LOOKSLIKE),
        new Entry(40,"e".toCharArray(),28,QTokenContext.REAL,ACTION_LOOKSLIKE),
        new Entry(40,"f".toCharArray(),16,QTokenContext.FLOAT,ACTION_LOOKSLIKE),
        new Entry(40,"t".toCharArray(),18,QTokenContext.TIME,ACTION_LOOKSLIKE),
        new Entry(40,"v".toCharArray(),19,QTokenContext.MINUTE,ACTION_LOOKSLIKE),
        new Entry(40,"u".toCharArray(),20,QTokenContext.SECOND,ACTION_LOOKSLIKE),
        new Entry(40,"i".toCharArray(),24,QTokenContext.INTEGER,ACTION_LOOKSLIKE),
        new Entry(41,":".toCharArray(),INIT,QTokenContext.OPERATOR,ACTION_MATCHANDCONSUME),
        new Entry(41,".".toCharArray(),27,QTokenContext.FLOAT,ACTION_LOOKSLIKE),
        new Entry(41,"01".toCharArray(),42,QTokenContext.INTEGER,ACTION_LOOKSLIKE),
        new Entry(41,"23456789".toCharArray(),4,QTokenContext.INTEGER,ACTION_LOOKSLIKE),
        new Entry(41,delimiters,INIT,QTokenContext.INTEGER,ACTION_MATCHANDPUTBACK),
        new Entry(41,"D".toCharArray(),95,QTokenContext.TIMESPAN,ACTION_LOOKSLIKE),
        new Entry(41,"j".toCharArray(),2,QTokenContext.LONG,ACTION_LOOKSLIKE),
        new Entry(41,"h".toCharArray(),3,QTokenContext.SHORT,ACTION_LOOKSLIKE),
        new Entry(41,"e".toCharArray(),28,QTokenContext.REAL,ACTION_LOOKSLIKE),
        new Entry(41,"f".toCharArray(),16,QTokenContext.FLOAT,ACTION_LOOKSLIKE),
        new Entry(41,"t".toCharArray(),18,QTokenContext.TIME,ACTION_LOOKSLIKE),
        new Entry(41,"v".toCharArray(),19,QTokenContext.MINUTE,ACTION_LOOKSLIKE),
        new Entry(41,"u".toCharArray(),20,QTokenContext.SECOND,ACTION_LOOKSLIKE),
        new Entry(41,"i".toCharArray(),24,QTokenContext.INTEGER,ACTION_LOOKSLIKE),
        new Entry(41,"p".toCharArray(),84,QTokenContext.TIMESTAMP,ACTION_LOOKSLIKE),
        new Entry(41,"b".toCharArray(),17,QTokenContext.BOOLEAN,ACTION_LOOKSLIKE),
        new Entry(42,":".toCharArray(),44,QTokenContext.MINUTE,ACTION_LOOKSLIKE),
        new Entry(42,".".toCharArray(),27,QTokenContext.FLOAT,ACTION_LOOKSLIKE),
        new Entry(42,"01".toCharArray(),25,QTokenContext.INTEGER,ACTION_LOOKSLIKE),
        new Entry(42,"23456789".toCharArray(),43,QTokenContext.INTEGER,ACTION_LOOKSLIKE),
        new Entry(42,delimiters,INIT,QTokenContext.INTEGER,ACTION_MATCHANDPUTBACK),
        new Entry(42,"D".toCharArray(),95,QTokenContext.TIMESPAN,ACTION_LOOKSLIKE),
        new Entry(42,"j".toCharArray(),2,QTokenContext.LONG,ACTION_LOOKSLIKE),
        new Entry(42,"h".toCharArray(),3,QTokenContext.SHORT,ACTION_LOOKSLIKE),
        new Entry(42,"e".toCharArray(),28,QTokenContext.REAL,ACTION_LOOKSLIKE),
        new Entry(42,"f".toCharArray(),16,QTokenContext.FLOAT,ACTION_LOOKSLIKE),
        new Entry(42,"t".toCharArray(),18,QTokenContext.TIME,ACTION_LOOKSLIKE),
        new Entry(42,"v".toCharArray(),19,QTokenContext.MINUTE,ACTION_LOOKSLIKE),
        new Entry(42,"u".toCharArray(),20,QTokenContext.SECOND,ACTION_LOOKSLIKE),
        new Entry(42,"i".toCharArray(),24,QTokenContext.INTEGER,ACTION_LOOKSLIKE),
        new Entry(42,"b".toCharArray(),17,QTokenContext.BOOLEAN,ACTION_LOOKSLIKE),
        new Entry(43,".".toCharArray(),27,QTokenContext.FLOAT,ACTION_LOOKSLIKE),
        new Entry(43,digits,54,QTokenContext.INTEGER,ACTION_LOOKSLIKE),
        new Entry(43,delimiters,INIT,QTokenContext.INTEGER,ACTION_MATCHANDPUTBACK),
        new Entry(43,"D".toCharArray(),95,QTokenContext.TIMESPAN,ACTION_LOOKSLIKE),
        new Entry(43,"j".toCharArray(),2,QTokenContext.LONG,ACTION_LOOKSLIKE),
        new Entry(43,"h".toCharArray(),3,QTokenContext.SHORT,ACTION_LOOKSLIKE),
        new Entry(43,"e".toCharArray(),28,QTokenContext.REAL,ACTION_LOOKSLIKE),
        new Entry(43,"f".toCharArray(),16,QTokenContext.FLOAT,ACTION_LOOKSLIKE),
        new Entry(43,"t".toCharArray(),18,QTokenContext.TIME,ACTION_LOOKSLIKE),
        new Entry(43,"v".toCharArray(),19,QTokenContext.MINUTE,ACTION_LOOKSLIKE),
        new Entry(43,"p".toCharArray(),84,QTokenContext.TIMESTAMP,ACTION_LOOKSLIKE),
        new Entry(43,"u".toCharArray(),20,QTokenContext.SECOND,ACTION_LOOKSLIKE),
        new Entry(43,"i".toCharArray(),24,QTokenContext.INTEGER,ACTION_LOOKSLIKE),
        new Entry(44,digits,45,QTokenContext.MINUTE,ACTION_LOOKSLIKE),
        new Entry(45,digits,46,QTokenContext.MINUTE,ACTION_LOOKSLIKE),
        new Entry(46,":".toCharArray(),47,QTokenContext.SECOND,ACTION_LOOKSLIKE),

        new Entry(46,".".toCharArray(),50,QTokenContext.SECOND,ACTION_LOOKSLIKE),


        new Entry(46,delimiters,INIT,QTokenContext.MINUTE,ACTION_MATCHANDPUTBACK),
        new Entry(47,digits,48,QTokenContext.SECOND,ACTION_LOOKSLIKE),
        new Entry(48,digits,49,QTokenContext.SECOND,ACTION_LOOKSLIKE),
        new Entry(49,".".toCharArray(),50,QTokenContext.TIME,ACTION_LOOKSLIKE),
        new Entry(49,delimiters,INIT,QTokenContext.SECOND,ACTION_MATCHANDPUTBACK),
        new Entry(50,digits,51,QTokenContext.TIME,ACTION_LOOKSLIKE),
        new Entry(50,delimiters,INIT,QTokenContext.TIME,ACTION_MATCHANDPUTBACK),
        new Entry(51,digits,52,QTokenContext.TIME,ACTION_LOOKSLIKE),
        new Entry(51,delimiters,INIT,QTokenContext.TIME,ACTION_MATCHANDPUTBACK),
        new Entry(52,digits,53,QTokenContext.TIME,ACTION_LOOKSLIKE),
        new Entry(52,delimiters,INIT,QTokenContext.TIME,ACTION_MATCHANDPUTBACK),
        new Entry(53,digits,83,QTokenContext.TIMESPAN,ACTION_LOOKSLIKE),
        new Entry(53,delimiters,INIT,QTokenContext.TIME,ACTION_MATCHANDPUTBACK),

        new Entry(54,".".toCharArray(),58,QTokenContext.FLOAT,ACTION_LOOKSLIKE),
        new Entry(54,digits,57,QTokenContext.INTEGER,ACTION_LOOKSLIKE),
        new Entry(54,delimiters,INIT,QTokenContext.INTEGER,ACTION_MATCHANDPUTBACK),
        new Entry(54,"D".toCharArray(),95,QTokenContext.TIMESPAN,ACTION_LOOKSLIKE),
        new Entry(54,"j".toCharArray(),2,QTokenContext.LONG,ACTION_LOOKSLIKE),
        new Entry(54,"h".toCharArray(),3,QTokenContext.SHORT,ACTION_LOOKSLIKE),
        new Entry(54,"e".toCharArray(),28,QTokenContext.REAL,ACTION_LOOKSLIKE),
        new Entry(54,"f".toCharArray(),16,QTokenContext.FLOAT,ACTION_LOOKSLIKE),
        new Entry(54,"t".toCharArray(),18,QTokenContext.TIME,ACTION_LOOKSLIKE),
        new Entry(54,"v".toCharArray(),19,QTokenContext.MINUTE,ACTION_LOOKSLIKE),
        new Entry(54,"u".toCharArray(),20,QTokenContext.SECOND,ACTION_LOOKSLIKE),
        new Entry(54,"p".toCharArray(),84,QTokenContext.TIMESTAMP,ACTION_LOOKSLIKE),
        new Entry(54,"i".toCharArray(),24,QTokenContext.INTEGER,ACTION_LOOKSLIKE),
        new Entry(57,".".toCharArray(),27,QTokenContext.FLOAT,ACTION_LOOKSLIKE),
        new Entry(57,digits,57,QTokenContext.INTEGER,ACTION_LOOKSLIKE),
        new Entry(57,delimiters,INIT,QTokenContext.INTEGER,ACTION_MATCHANDPUTBACK),
        new Entry(57,"D".toCharArray(),95,QTokenContext.TIMESPAN,ACTION_LOOKSLIKE),
        new Entry(57,"j".toCharArray(),2,QTokenContext.LONG,ACTION_LOOKSLIKE),
        new Entry(57,"h".toCharArray(),3,QTokenContext.SHORT,ACTION_LOOKSLIKE),
        new Entry(57,"e".toCharArray(),28,QTokenContext.REAL,ACTION_LOOKSLIKE),
        new Entry(57,"f".toCharArray(),16,QTokenContext.FLOAT,ACTION_LOOKSLIKE),
        new Entry(57,"t".toCharArray(),18,QTokenContext.TIME,ACTION_LOOKSLIKE),
        new Entry(57,"v".toCharArray(),19,QTokenContext.MINUTE,ACTION_LOOKSLIKE),
        new Entry(57,"u".toCharArray(),20,QTokenContext.SECOND,ACTION_LOOKSLIKE),
        new Entry(57,"p".toCharArray(),84,QTokenContext.TIMESTAMP,ACTION_LOOKSLIKE),
        new Entry(57,"i".toCharArray(),24,QTokenContext.INTEGER,ACTION_LOOKSLIKE),
        new Entry(58,digits,59,QTokenContext.FLOAT,ACTION_LOOKSLIKE),
        new Entry(58,"e".toCharArray(),28,QTokenContext.REAL,ACTION_LOOKSLIKE),
        new Entry(58,".".toCharArray(),255,QTokenContext.UNKNOWN,ACTION_LOOKSLIKE),
        new Entry(58,delimiters,INIT,QTokenContext.FLOAT,ACTION_MATCHANDPUTBACK),
        new Entry(59,digits,60,QTokenContext.FLOAT,ACTION_LOOKSLIKE),
        new Entry(59,"e".toCharArray(),28,QTokenContext.REAL,ACTION_LOOKSLIKE),
        new Entry(59,".".toCharArray(),255,QTokenContext.UNKNOWN,ACTION_LOOKSLIKE),
        new Entry(59,delimiters,INIT,QTokenContext.FLOAT,ACTION_MATCHANDPUTBACK),
        new Entry(60,digits,27,QTokenContext.FLOAT,ACTION_LOOKSLIKE),
        new Entry(60,"e".toCharArray(),28,QTokenContext.REAL,ACTION_LOOKSLIKE),
        new Entry(60,"m".toCharArray(),61,QTokenContext.MONTH,ACTION_LOOKSLIKE),
        new Entry(60,".".toCharArray(),62,QTokenContext.DATE,ACTION_LOOKSLIKE),
        new Entry(60,delimiters,INIT,QTokenContext.FLOAT,ACTION_MATCHANDPUTBACK),
        new Entry(61,delimiters,INIT,QTokenContext.MONTH,ACTION_MATCHANDPUTBACK),
        new Entry(62,digits,63,QTokenContext.DATE,ACTION_LOOKSLIKE),
        new Entry(63,digits,64,QTokenContext.DATE,ACTION_LOOKSLIKE),
        new Entry(64,"T".toCharArray(),65,QTokenContext.DATETIME,ACTION_LOOKSLIKE),
        new Entry(64,"D".toCharArray(),85,QTokenContext.TIMESTAMP,ACTION_LOOKSLIKE),
        new Entry(64,".".toCharArray(),255,QTokenContext.UNKNOWN,ACTION_LOOKSLIKE),
        new Entry(64,delimiters,INIT,QTokenContext.DATE,ACTION_MATCHANDPUTBACK),
        new Entry(65,digits,66,QTokenContext.UNKNOWN,ACTION_LOOKSLIKE),
        new Entry(66,digits,67,QTokenContext.UNKNOWN,ACTION_LOOKSLIKE),
        new Entry(67,":".toCharArray(),68,QTokenContext.UNKNOWN,ACTION_LOOKSLIKE),
        new Entry(68,digits,69,QTokenContext.UNKNOWN,ACTION_LOOKSLIKE),
        new Entry(69,digits,70,QTokenContext.DATETIME,ACTION_LOOKSLIKE),
        new Entry(70,":".toCharArray(),71,QTokenContext.UNKNOWN,ACTION_LOOKSLIKE),
        new Entry(70,delimiters,INIT,QTokenContext.DATETIME,ACTION_MATCHANDPUTBACK),
        new Entry(71,digits,72,QTokenContext.UNKNOWN,ACTION_LOOKSLIKE),
        new Entry(72,digits,73,QTokenContext.DATETIME,ACTION_LOOKSLIKE),
        new Entry(73,".".toCharArray(),74,QTokenContext.DATETIME,ACTION_LOOKSLIKE),
        new Entry(73,delimiters,INIT,QTokenContext.DATETIME,ACTION_MATCHANDPUTBACK),
        new Entry(74,digits,74,QTokenContext.DATETIME,ACTION_LOOKSLIKE),
        new Entry(74,delimiters,INIT,QTokenContext.DATETIME,ACTION_MATCHANDPUTBACK),
        new Entry(79,"-".toCharArray(),80,QTokenContext.OPERATOR,ACTION_LOOKSLIKE),
        new Entry(79,"".toCharArray(),INIT,QTokenContext.OPERATOR,ACTION_MATCHANDPUTBACK),
        new Entry(80,digits,57,QTokenContext.INTEGER,ACTION_LOOKSLIKE),
        new Entry(80,"".toCharArray(),INIT,QTokenContext.OPERATOR,ACTION_MATCHANDPUTBACK),
        new Entry(81,"-".toCharArray(),80,QTokenContext.OPERATOR,ACTION_LOOKSLIKE),
        new Entry(81,"".toCharArray(),INIT,QTokenContext.OPERATOR,ACTION_MATCHANDPUTBACK),
        
        new Entry(82,".".toCharArray(),255,QTokenContext.UNKNOWN,ACTION_LOOKSLIKE),
        new Entry(82,delimiters,INIT,QTokenContext.FLOAT,ACTION_MATCHANDPUTBACK),

        new Entry(83,digits,83,QTokenContext.TIMESPAN,ACTION_LOOKSLIKE),
        new Entry(83,delimiters,INIT,QTokenContext.TIMESPAN,ACTION_MATCHANDPUTBACK),

        new Entry(84,delimiters,INIT,QTokenContext.TIMESTAMP,ACTION_MATCHANDPUTBACK),
        
        new Entry(85,digits,86,QTokenContext.UNKNOWN,ACTION_LOOKSLIKE),
        new Entry(85,delimiters,INIT,QTokenContext.TIMESPAN,ACTION_MATCHANDPUTBACK),
        new Entry(86,digits,87,QTokenContext.UNKNOWN,ACTION_LOOKSLIKE),
        new Entry(86,delimiters,INIT,QTokenContext.TIMESPAN,ACTION_MATCHANDPUTBACK),
        new Entry(87,":".toCharArray(),88,QTokenContext.UNKNOWN,ACTION_LOOKSLIKE),
        new Entry(87,delimiters,INIT,QTokenContext.TIMESPAN,ACTION_MATCHANDPUTBACK),
        new Entry(88,digits,89,QTokenContext.UNKNOWN,ACTION_LOOKSLIKE),
        new Entry(89,digits,90,QTokenContext.TIMESTAMP,ACTION_LOOKSLIKE),
        new Entry(90,":".toCharArray(),91,QTokenContext.UNKNOWN,ACTION_LOOKSLIKE),
        new Entry(90,delimiters,INIT,QTokenContext.TIMESTAMP,ACTION_MATCHANDPUTBACK),
        new Entry(91,digits,92,QTokenContext.UNKNOWN,ACTION_LOOKSLIKE),
        new Entry(92,digits,93,QTokenContext.TIMESTAMP,ACTION_LOOKSLIKE),
        new Entry(93,".".toCharArray(),94,QTokenContext.TIMESTAMP,ACTION_LOOKSLIKE),
        new Entry(93,delimiters,INIT,QTokenContext.TIMESTAMP,ACTION_MATCHANDPUTBACK),
        new Entry(94,digits,94,QTokenContext.TIMESTAMP,ACTION_LOOKSLIKE),
        new Entry(94,delimiters,INIT,QTokenContext.TIMESTAMP,ACTION_MATCHANDPUTBACK),

        new Entry(95,digits,96,QTokenContext.UNKNOWN,ACTION_LOOKSLIKE),
        new Entry(95,delimiters,INIT,QTokenContext.TIMESPAN,ACTION_MATCHANDPUTBACK),
        new Entry(96,digits,97,QTokenContext.UNKNOWN,ACTION_LOOKSLIKE),
        new Entry(96,delimiters,INIT,QTokenContext.TIMESPAN,ACTION_MATCHANDPUTBACK),
        new Entry(97,":".toCharArray(),98,QTokenContext.UNKNOWN,ACTION_LOOKSLIKE),
        new Entry(97,delimiters,INIT,QTokenContext.TIMESPAN,ACTION_MATCHANDPUTBACK),
        new Entry(98,digits,99,QTokenContext.UNKNOWN,ACTION_LOOKSLIKE),
        new Entry(99,digits,100,QTokenContext.TIMESTAMP,ACTION_LOOKSLIKE),
        new Entry(100,":".toCharArray(),101,QTokenContext.UNKNOWN,ACTION_LOOKSLIKE),
        new Entry(100,delimiters,INIT,QTokenContext.TIMESTAMP,ACTION_MATCHANDPUTBACK),
        new Entry(101,digits,102,QTokenContext.UNKNOWN,ACTION_LOOKSLIKE),
        new Entry(102,digits,103,QTokenContext.TIMESTAMP,ACTION_LOOKSLIKE),
        new Entry(103,".".toCharArray(),104,QTokenContext.TIMESTAMP,ACTION_LOOKSLIKE),
        new Entry(103,delimiters,INIT,QTokenContext.TIMESTAMP,ACTION_MATCHANDPUTBACK),
        new Entry(104,digits,104,QTokenContext.TIMESTAMP,ACTION_LOOKSLIKE),
        new Entry(104,delimiters,INIT,QTokenContext.TIMESTAMP,ACTION_MATCHANDPUTBACK),                

        new Entry(105,delimiters,INIT,QTokenContext.TIMESPAN,ACTION_MATCHANDPUTBACK),

        new Entry(255,delimiters,INIT,QTokenContext.UNKNOWN,ACTION_MATCHANDPUTBACK),
        new Entry(255,"".toCharArray(),255,QTokenContext.UNKNOWN,ACTION_LOOKSLIKE)
    };

    public QSyntax()
    {
        initMap();
        tokenContextPath=QTokenContext.contextPath;
    }
}