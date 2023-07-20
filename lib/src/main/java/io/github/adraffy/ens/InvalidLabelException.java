package io.github.adraffy.ens;

public class InvalidLabelException extends RuntimeException {
   
    public final int pos;
    public final int end;
    
    InvalidLabelException(int pos, int end, String message, ENSIPException cause) {
        super(message, cause);
        this.pos = pos;
        this.end = end;
    }
    
    public ENSIPException getError() {
        return (ENSIPException)getCause();
    }
        
}
