package io.github.adraffy.ens;

public class NormException extends RuntimeException {
    
    public final String kind;
    public final String reason;
    
    NormException(String kind) {
        super(kind);
        this.kind = kind;
        this.reason = null;
    }
    NormException(String kind, String reason) {
        super(String.format("%s: %s", kind, reason));
        this.kind = kind;
        this.reason = reason;
    }
    
}
