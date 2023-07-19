package io.github.adraffy.ens;

import java.util.HashMap;

public class Whole {
    
    static final Whole UNIQUE_PH = new Whole(ReadOnlyIntSet.EMPTY, ReadOnlyIntSet.EMPTY);
         
    public final ReadOnlyIntSet valid;
    public final ReadOnlyIntSet confused;
    
    final HashMap<Integer,int[]> complements = new HashMap<>();
    
    Whole(ReadOnlyIntSet valid, ReadOnlyIntSet confused) {
        this.valid = valid;
        this.confused = confused;
    }
    
}
