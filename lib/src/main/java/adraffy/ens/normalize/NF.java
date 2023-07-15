package adraffy.ens.normalize;

import java.util.HashMap;

public class NF {
    
    static final int SHIFT = 24;
    static final int MASK = (1 << SHIFT) - 1;
    static final int NONE = -1;

    static final int S0 = 0xAC00;
    static final int L0 = 0x1100;
    static final int V0 = 0x1161;
    static final int T0 = 0x11A7;
    static final int L_COUNT = 19;
    static final int V_COUNT = 21;
    static final int T_COUNT = 28;
    static final int N_COUNT = V_COUNT * T_COUNT;
    static final int S_COUNT = L_COUNT * N_COUNT;
    static final int S1 = S0 + S_COUNT;
    static final int L1 = L0 + L_COUNT;
    static final int V1 = V0 + V_COUNT;
    static final int T1 = T0 + T_COUNT;
    
    static boolean isHangul(int cp) {
       return cp >= S0 && cp < S1;
    }
    static int unpackCC(int packed) {
       return packed >> SHIFT;
    }
    static int unpackCP(int packed) {
        return packed & MASK;
    }
    
    public final String unicodeVersion;
    
    final ReadOnlyIntSet exclusions;
    final ReadOnlyIntSet quickCheck;
    final HashMap<Integer,int[]> decomps = new HashMap<>();
    final HashMap<Integer,HashMap<Integer,Integer>> recomps = new HashMap<>();
    final HashMap<Integer,Integer> ranks = new HashMap<>();
    
    public NF(Decoder dec) {
        unicodeVersion = dec.readString();
        exclusions = ReadOnlyIntSet.fromOwnedUnsorted(dec.readUnique());
        quickCheck = ReadOnlyIntSet.fromOwnedUnsorted(dec.readUnique());
        int[] decomp1 = dec.readSortedUnique();
        int[] decomp1A = dec.readUnsortedDeltas(decomp1.length);
        for (int i = 0; i < decomp1.length; i++) {
            decomps.put(decomp1[i], new int[] { decomp1A[i] });
        }
        int[] decomp2 = dec.readSortedUnique();
        int n = decomp2.length;
        int[] decomp2A = dec.readUnsortedDeltas(n);
        int[] decomp2B = dec.readUnsortedDeltas(n);
        for (int i = 0; i < n; i++) {
            int cp = decomp2[i];
            int cpA = decomp2A[i];
            int cpB = decomp2B[i];
            decomps.put(cp, new int[] { cpB, cpA }); // reversed
            if (!exclusions.contains(cp)) {
                HashMap<Integer,Integer> recomp = recomps.get(cpA);
                if (recomp == null) {
                    recomp = new HashMap<>();
                    recomps.put(cpA, recomp);
                }
                recomp.put(cpB, cp);
            }
        }
        for (int rank = 0; ; ) {
            rank += 1 << SHIFT;
            int[] v = dec.readUnique();
            if (v.length == 0) break;
            Integer boxed = rank;
            for (int cp: v) {
                ranks.put(cp, boxed);
            }
        }
    }
    
    int composePair(int a, int b)  {
        if (a >= L0 && a < L1 && b >= V0 && b < V1) {
            return S0 + (a - L0) * N_COUNT + (b - V0) * T_COUNT;
        } else if (isHangul(a) && b > T0 && b < T1 && (a - S0) % T_COUNT == 0) {
            return a + (b - T0);
        } else {
            HashMap<Integer,Integer> map = recomps.get(a);
            if (map != null) {
                Integer boxed = map.get(b);
                if (boxed != null) {
                    return boxed;
                }                
            }
            return NONE;
        }
    }
    
    class Packer {
        final IntList buf = new IntList();
        boolean check = false;
        void add(int cp) {
            int cc = ranks.getOrDefault(cp, 0);
            if (cc != 0) {
                check = true;
                cp |= cc;
            }
            buf.add(cp);
        }
        void fixOrder() {
            if (!check) return;
            int[] v = buf.array; 
            int prev = unpackCC(v[0]);
            for (int i = 1, e = buf.count; i < e; i++) {
                int cc = unpackCC(v[i]);
                if (cc == 0 || prev <= cc) {
                    prev = cc;
                    continue;
                }
                int j = i - 1;
                while (true) {
                    int temp = v[j];
                    v[j] = v[j + 1];
                    v[j + 1] = temp;
                    if (j == 0) break;
                    prev = unpackCC(v[--j]);
                    if (prev <= cc) break;
                }
                prev = unpackCC(v[i]);
            }
        }
    }
    
    int[] decomposed(int[] cps) {
        Packer p = new Packer();
        IntList buf = new IntList();
        for (int cp0: cps) {
            int cp = cp0;
            while (true) {
                if (cp < 0x80) {
                    p.buf.add(cp);
                } else if (isHangul(cp)) {
                    int s_index = cp - S0;
                    int l_index = s_index / N_COUNT;
                    int v_index = (s_index % N_COUNT) / T_COUNT;
                    int t_index = s_index % T_COUNT;
                    p.add(L0 + l_index);
                    p.add(V0 + v_index);
                    if (t_index > 0) p.add(T0 + t_index);
                } else {
                    int[] decomp = decomps.get(cp);
                    if (decomp != null) {
                        for (int x: decomp) buf.add(x);
                    } else {
                        p.add(cp);
                    }
                }
                if (buf.count == 0) break;
                cp = buf.pop();
            }
        }
        p.fixOrder();
        return p.buf.consume();
    }
    
    int[] composedFromPacked(int[] packed) {
        IntList cps = new IntList();
        IntList stack = new IntList();
        int prev_cp = NONE;
        int prev_cc = 0;
        for (int p: packed) {
            int cc = unpackCC(p);
            int cp = unpackCP(p);
            if (prev_cp == NONE) {
                if (cc == 0) {
                    prev_cp = cp;
                } else {
                    cps.add(cp);
                }
            } else if (prev_cc > 0 && prev_cc >= cc) {
                if (cc == 0) {
                    cps.add(prev_cp);
                    cps.add(stack);
                    stack.count = 0;
                    prev_cp = cp;
                } else {
                    stack.add(cp);
                }
                prev_cc = cc;
            } else {
                int composed = composePair(prev_cp, cp);
                if (composed != NONE) {
                    prev_cp = composed;
                } else if (prev_cc == 0 && cc == 0) {
                    cps.add(prev_cp);
                    prev_cp = cp;
                } else {
                    stack.add(cp);
                    prev_cc = cc;
                }
            }
        }
        if (prev_cp != NONE) {
            cps.add(prev_cp);
            cps.add(stack);
        }
        return cps.consume();
    }
    
    public int[] NFD(int[] cps) {
        int[] v = decomposed(cps);
        for (int i = 0, e = v.length; i < e; i++) {
            v[i] = unpackCP(v[i]);
        }
        return v;
    }
    public int[] NFC(int[] cps) {
        return composedFromPacked(decomposed(cps));
    }
    
    // convenience
    public String NFD(String s) {
        return StringUtils.implode(NFD(StringUtils.explode(s)));
    }
    public String NFC(String s) {
        return StringUtils.implode(NFC(StringUtils.explode(s)));
    }    
    
}
