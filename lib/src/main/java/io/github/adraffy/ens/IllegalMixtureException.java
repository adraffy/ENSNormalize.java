package io.github.adraffy.ens;

public class IllegalMixtureException extends NormException {
    
    public final int cp;
    public final Group group;
    public final Group other; // nullable
    
    IllegalMixtureException(String reason, int cp, Group group, Group other) {
        super(ENSIP15.ILLEGAL_MIXTURE, reason);
        this.cp = cp;
        this.group = group;
        this.other = other;
    }    
    
}
