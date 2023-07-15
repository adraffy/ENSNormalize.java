package adraffy.ens.normalize;

import java.util.Arrays;
import java.util.stream.IntStream;

public class OutputToken {
    
    public final int[] cps;
    public final EmojiSequence emoji;
    
    OutputToken(int[] cps, EmojiSequence emoji) {
        this.cps = cps;
        this.emoji = emoji;
    }
 
    public IntStream stream() {
        return Arrays.stream(cps);
    }
    
    @Override
    public String toString() {
        return String.format("%s[%s]", emoji != null ? "Emoji" : "Text", StringUtils.toHexSequence(cps));
    }
}
