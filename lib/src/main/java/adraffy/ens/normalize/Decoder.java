package adraffy.ens.normalize;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;

public class Decoder {
    
    static int asSigned(int i) {
        return (i & 1) != 0 ? ~i >> 1 : i >> 1;
    }

    private final IntBuffer buf;
    private final int[] magic;
    private int word = 0;
    private int bits = 0;
    
    public Decoder(IntBuffer buf) {
        this.buf = buf;
        this.magic = readMagic();
    }
    
    private int[] readMagic() {
        ArrayList<Integer> list = new ArrayList<>();
        int w = 0;
        while (true) {
            int dw = readUnary();
            if (dw == 0) break;
            list.add(w += dw);
        }
        return list.stream().mapToInt(x -> x).toArray();
    }
    
    public boolean readBit() {
        if (bits == 0) {
            word = buf.get();
            bits = 1;
        }
        boolean bit = (word & bits) != 0;
        bits <<= 1;
        return bit;
    }
    
    public int readUnary() {
        int x = 0;
        while (readBit()) x++;
        return x;
    }
    
    public int readBinary(int w) {
        int x = 0;
        for (int b = 1 << (w - 1); b != 0; b >>= 1) {
            if (readBit()) {
                x |= b;
            }
        }  
        return x;
    }
    
    public int readUnsigned() {
        int a = 0;
        int w;
        int n;
        for (int i = 0; ; )
        {
            w = magic[i];
            n = 1 << w;
            if (++i == magic.length || !readBit()) break;
            a += n;
        }
        return a + readBinary(w);
    }
    
    public int[] readSortedAscending(int n) { return readArray(n, (prev, x) -> prev + 1 + x); }
    public int[] readUnsortedDeltas(int n) { return readArray(n, (prev, x) -> prev + asSigned(x)); }

    static public interface ReadArrayFunction {
        int get(int prev, int x);
    }
    
    public int[] readArray(int count, ReadArrayFunction fn) {
        int[] v = new int[count];
        int prev = -1;
        for (int i = 0; i < count; i++) {
            v[i] = prev = fn.get(prev, readUnsigned());
        }
        return v;
    }
    
    public int[] readUnique() {
        int pos = readUnsigned();
        int[] v = readSortedAscending(pos);
        int n = readUnsigned();          
        if (n > 0) {
            int[] vX = readSortedAscending(n);
            int[] vS = readUnsortedDeltas(n);
            v = Arrays.copyOf(v, pos + Arrays.stream(vS).sum());
            for (int i = 0; i < n; i++) {                
                for (int x = vX[i], e = x + vS[i]; x < e; x++) {
                    v[pos++] = x;
                }
            }
        }
        return v;
    }
    
    public <T> ArrayList<T> readTree(Function<int[],T> fn) {
        ArrayList<T> ret = new ArrayList<>();
        readTree(ret, fn, new IntList(16));
        return ret;
    }
    
    private <T> void readTree(ArrayList<T> ret, Function<int[],T> fn, IntList path) {
        int i = path.count;
        path.add(0);
        for (int x: readSortedAscending(readUnsigned())) {
            path.array[i] = x;
            ret.add(fn.apply(path.toArray()));
        }
        for (int x: readSortedAscending(readUnsigned())) {
            path.array[i] = x;
            readTree(ret, fn, path);
        }
        path.count = i;
    }
    
    public String readString() {
        return StringUtils.implode(readUnsortedDeltas(readUnsigned()));        
    }
    
    public int[] readSortedUnique() {
        int[] v = readUnique();
        Arrays.sort(v);
        return v;
    }
    
}
