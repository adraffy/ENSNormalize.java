package adraffy.ens.normalize;

public class ConfusableException extends NormException {
    
    public final Group group;
    public final Group other;
    
    ConfusableException(Group group, Group other) {
        super("whole-script confusable", String.format("%s/%s", group, other));
        this.group = group;
        this.other = other;
    }      
    
}
