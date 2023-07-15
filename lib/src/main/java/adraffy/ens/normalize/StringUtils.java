package adraffy.ens.normalize;

import java.util.ArrayList;
import java.util.List;

public class StringUtils {
    
    static final int UTF16_BMP = 0x10000;
    static final int UTF16_BITS = 10;
    static final int UTF16_HEAD = ~0 << UTF16_BITS;      // upper 6 bits
    static final int UTF16_DATA = (1 << UTF16_BITS) - 1; // lower 10 bits
    static final int UTF16_HI = 0xD800; // 110110*
    static final int UTF16_LO = 0xDC00; // 110111*
    
    static public int UTF16Width(int cp) {
        return cp < UTF16_BMP ? 1 : 2;
    }
    
    static public int UTF16Length(int[] cps) {
        int n = 0;
        for (int cp: cps) n += UTF16Width(cp);
        return n; 
    }
    
    static public void appendCodepoint(StringBuilder sb, int cp) {
        if (cp < UTF16_BMP) {
            sb.append((char)cp);
        } else {
            cp -= UTF16_BMP;
            sb.append((char)(UTF16_HI | ((cp >> UTF16_BITS) & UTF16_DATA)));
            sb.append((char)(UTF16_LO | (cp & UTF16_DATA)));
        }
    }
    
    static public void appendHex(StringBuilder sb, int cp) {
        if (cp < 16) sb.append('0');       
        sb.append(Integer.toHexString(cp).toUpperCase());
    }
    
    static public String toHex(int cp) {        
        String temp = Integer.toHexString(cp);
        return temp.length() == 1 ? '0' + temp : temp;
    }
    
    static public String toHexSequence(int[] cps) {
        int n = cps.length;
        if (n == 0) return "";
        StringBuilder sb = new StringBuilder(n * 5); // guess
        appendHex(sb, cps[0]);
        for (int i = 1; i < n; i++) {
            sb.append(' ');
            appendHex(sb, cps[i]);
        }
        return sb.toString();
    }
    
    static public String toHexSequence(String s) {
        return toHexSequence(explode(s));
    }

    static public String implode(int[] cps) {
        StringBuilder sb = new StringBuilder(UTF16Length(cps));
        for (int cp: cps) appendCodepoint(sb, cp);
        return sb.toString();
    }
    
    static public int[] explode(String s) { return explode(s, 0, s.length()); }
    static public int[] explode(String s, int a, int b) {
        IntList buf = new IntList(b - a);
        while (a < b) {
            int ch0 = s.charAt(a++);
            int ch1;
            int head = ch0 & UTF16_HEAD;
            if (head == UTF16_HI && a < b && ((ch1 = s.charAt(a)) & UTF16_HEAD) == UTF16_LO) { // valid pair
                buf.add(UTF16_BMP + (((ch0 & UTF16_DATA) << UTF16_BITS) | (ch1 & UTF16_DATA)));
                a++;
            } else {
                buf.add(ch0);
            }
        }
        return buf.consume();
    }
   
    static public List<String> split(String s, char c) {
        ArrayList<String> ret = new ArrayList<>();
        int prev = 0;
        while (true) {
            int next = s.indexOf(c, prev);
            if (next < 0) {
                ret.add(s.substring(prev));
                return ret;
            }
            ret.add(s.substring(prev, next));  
            prev = next + 1;          
        }
    }
    
}
