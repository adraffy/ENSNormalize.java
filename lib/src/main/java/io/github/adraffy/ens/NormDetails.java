package io.github.adraffy.ens;

import java.util.HashSet;
import java.util.stream.Collectors;

public class NormDetails {

    public final String name;
    public final HashSet<Group> groups;
    public final HashSet<EmojiSequence> emojis;
    public final boolean possiblyConfusing;

    NormDetails(String norm, HashSet<Group> groups, HashSet<EmojiSequence> emojis, boolean confusing) {
        this.name = norm;
        this.groups = groups;
        this.emojis = emojis;
        this.possiblyConfusing = confusing;
    }
    
    public String groupDescription() {
        return groups.stream().map(g -> g.name).sorted().collect(Collectors.joining("+"));
    }
    
    public boolean hasZWJEmoji() {
        return emojis.stream().anyMatch(e -> e.hasZWJ());
    }
    
}
