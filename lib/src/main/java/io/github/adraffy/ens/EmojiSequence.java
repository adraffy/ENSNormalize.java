package io.github.adraffy.ens;

import java.util.Arrays;

public class EmojiSequence {
    
    public final String form;
    public final ReadOnlyIntList beautified;
    public final ReadOnlyIntList normalized;
 
    EmojiSequence(int[] cps) {
        form = StringUtils.implode(cps);
        beautified = new ReadOnlyIntList(cps);
        int[] norm = Arrays.stream(cps).filter(cp -> cp != 0xFE0F).toArray();        
        normalized = norm.length < cps.length ? new ReadOnlyIntList(norm) : beautified;
    }
    
    public boolean isMangled() {
        return beautified != normalized;
    }
    
    @Override
    public String toString() {
        return String.format("Emoji[%s]", StringUtils.toHexSequence(beautified.array));
    }
    
}
