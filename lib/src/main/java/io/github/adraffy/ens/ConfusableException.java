package io.github.adraffy.ens;

public class ConfusableException extends ENSIPException {
    
    public final Group group;
    public final Group other;
    
    ConfusableException(Group group, Group other) {
        super(ENSIP15.WHOLE_CONFUSABLE, String.format("%s/%s", group, other));
        this.group = group;
        this.other = other;
    }      
    
}
