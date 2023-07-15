package adraffy.ens.normalize;

import java.util.List;

public class Label {
 
    public int[] input;
    public int start;
    public int end;
    public List<OutputToken> tokens;
    public NormException error;
    public int[] normalized;
    public Group group;

}
