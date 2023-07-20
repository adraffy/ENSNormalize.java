package io.github.adraffy.ens;

public class DisallowedCharacterException extends ENSIPException {
    
    public final int cp;
    
    DisallowedCharacterException(String reason, int cp) {
        super(ENSIP15.DISALLOWED_CHARACTER, reason);
        this.cp = cp;
    }
    
}
