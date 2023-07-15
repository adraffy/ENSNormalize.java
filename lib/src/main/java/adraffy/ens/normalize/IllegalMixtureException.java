package adraffy.ens.normalize;

public class IllegalMixtureException extends NormException {
    
    public final int cp;
    public final Group group;
    public final Group other; // nullable
    
    IllegalMixtureException(String reason, int cp, Group group, Group other) {
        super("illegal mixture", reason);
        this.cp = cp;
        this.group = group;
        this.other = other;
    }    
    
}
