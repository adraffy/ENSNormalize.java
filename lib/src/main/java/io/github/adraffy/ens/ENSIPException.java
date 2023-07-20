package io.github.adraffy.ens;

public class ENSIPException extends RuntimeException {
    
    public final String kind;
    public final String reason;
    
    ENSIPException(String kind) {
        super(kind);
        this.kind = kind;
        this.reason = null;
    }
    ENSIPException(String kind, String reason) {
        super(String.format("%s: %s", kind, reason));
        this.kind = kind;
        this.reason = reason;
    }
    
}
