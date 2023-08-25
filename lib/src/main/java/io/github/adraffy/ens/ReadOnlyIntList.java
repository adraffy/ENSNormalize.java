package io.github.adraffy.ens;

import java.util.Arrays;
import java.util.stream.IntStream;

public class ReadOnlyIntList {

    final int[] array;
    
    ReadOnlyIntList(int[] v) {
        array = v;
    }
    
    public int size() {
        return array.length;
    }
    
    public int get(int index) {
        return array[index];
    }
    
    public boolean contains(int value) {
        for (int x: array) {
            if (x == value) {
                return true;
            }
        }
        return false;
    }
    
    public IntStream stream() {
        return Arrays.stream(array);
    }
    
    public int[] toArray() {
        return array.clone();
    }
    
}
