package adraffy.ens.normalize;

public class DisallowedCharacterException extends NormException {
    
    public final int cp;
    
    DisallowedCharacterException(String reason, int cp) {
        super("disallowed character", reason);
        this.cp = cp;
    }
    
}
