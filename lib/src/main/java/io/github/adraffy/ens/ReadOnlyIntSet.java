package io.github.adraffy.ens;

import java.util.Arrays;

public class ReadOnlyIntSet extends ReadOnlyIntList {
    
    static public final ReadOnlyIntSet EMPTY = new ReadOnlyIntSet(new int[0]);
    
    static ReadOnlyIntSet fromOwnedUnsorted(int[] v) {        
        Arrays.sort(v);
        return new ReadOnlyIntSet(v);
    }
    
    ReadOnlyIntSet(int[] v) {
        super(v);
    }
    
    public boolean contains(int x) {
        return Arrays.binarySearch(array, x) >= 0;
    }
    
    /*
    public boolean contains(int x);    
    public int size();
    public IntStream stream();
    
    static public final ReadOnlyIntSet EMPTY = new ReadOnlyIntSet() {
        @Override
        public boolean contains(int x) {
            return false;
        }
        @Override
        public int size() {
            return 0;
        }
        @Override
        public IntStream stream() {
            return IntStream.empty();
        }

    };
    
    static ReadOnlyIntSet fromOwnedUnsorted(int[] v) {        
        Arrays.sort(v);
        return new Presorted(v);
        //return new Boxed(v);
    }
    
    static class Presorted extends ReadOnlyIntList implements ReadOnlyIntSet {
        Presorted(int[] sorted) {
            super(sorted);
        }
        @Override
        public boolean contains(int x) {
            return Arrays.binarySearch(array, x) >= 0;
        }
    }
    
    static class Boxed implements ReadOnlyIntSet {
        private final HashSet<Integer> set = new HashSet<>();
        Boxed(int[] raw) {
            for (int x: raw) {
                set.add(x);
            }
        }
        @Override
        public boolean contains(int x) {
            return set.contains(x);
        }
        @Override
        public int size() {
            return set.size();
        }
        @Override
        public IntStream stream() {
            return set.stream().mapToInt(x -> x);
        }
    }
    */
    
}
