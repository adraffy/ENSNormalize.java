package adraffy.ens.normalize;

public class InvalidLabelException extends RuntimeException {
   
    public final int pos;
    public final int end;
    
    InvalidLabelException(int pos, int end, String message, NormException cause) {
        super(message, cause);
        this.pos = pos;
        this.end = end;
    }
    
    public NormException getError() {
        return (NormException)getCause();
    }
        
}
