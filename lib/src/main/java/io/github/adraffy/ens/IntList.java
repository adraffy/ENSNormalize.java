package io.github.adraffy.ens;

import java.util.Arrays;
import java.util.stream.IntStream;

class IntList {
 
    int[] array;
    int count = 0;
    
    IntList() { this(16); }
    IntList(int capacity) {
        array = new int[capacity];
    }
    
    void add(int x) {
        if (array.length == count) {
            array = Arrays.copyOf(array, count << 1);
        }
        array[count++] = x;
    }
    void add(int[] xs) {
        for (int x: xs) {
            add(x);
        }
    }
    void add(IntList other) {
        for (int i = 0; i < other.count; i++) {
            add(other.array[i]);
        }
    }
    
    int pop() {
        return array[--count];
    }
    
    int[] consume() {
        return count == array.length ? array : toArray();
    }
    
    int[] toArray() {
        return Arrays.copyOf(array, count);
    }
    
    IntStream stream() {
        return Arrays.stream(array, 0, count);
    }
    
}
