package io.github.adraffy.ens;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ENSIP15 {
 
    // error kinds
    static public final String DISALLOWED_CHARACTER = "disallowed character";
    static public final String ILLEGAL_MIXTURE = "illegal mixture";
    static public final String WHOLE_CONFUSABLE = "whole-script confusable";
    static public final String EMPTY_LABEL = "empty label";
    static public final String NSM_DUPLICATE = "duplicate non-spacing marks";
    static public final String NSM_EXCESSIVE = "excessive non-spacing marks";
    static public final String CM_LEADING = "leading combining mark";
    static public final String CM_AFTER_EMOJI = "emoji + combining mark";
    static public final String FENCED_LEADING = "leading fenced";
    static public final String FENCED_ADJACENT = "adjacent fenced";
    static public final String FENCED_TRAILING = "trailing fenced";
    static public final String INVALID_LABEL_EXTENSION = "invalid label extension";
    static public final String INVALID_UNDERSCORE = "underscore allowed only at start";
    
    static public final char STOP_CH = '.';
    
    public final NF NF;
    public final int maxNonSpacingMarks;
    public final ReadOnlyIntSet shouldEscape;
    public final ReadOnlyIntSet ignored;
    public final ReadOnlyIntSet combiningMarks;
    public final ReadOnlyIntSet nonSpacingMarks;
    public final ReadOnlyIntSet NFCCheck;
    public final ReadOnlyIntSet possiblyValid;
    public final Map<Integer,String> fenced;
    public final Map<Integer,ReadOnlyIntList> mapped;
    public final List<Group> groups;
    public final List<EmojiSequence> emojis;
    public final List<Whole> wholes;
    
    final HashMap<Integer,Whole> confusables = new HashMap<>();
    final EmojiNode emojiRoot = new EmojiNode();
    final Group LATIN, GREEK, ASCII, EMOJI;

    // experimental    
    private final String[] POSSIBLY_CONFUSING = {"ą", "ç", "ę", "ş", "ì", "í", "î", "ï", "ǐ", "ł"};
    
    ENSIP15(NF NF, Decoder dec) {
        this.NF = NF;
        shouldEscape = ReadOnlyIntSet.fromOwnedUnsorted(dec.readUnique());
        ignored = ReadOnlyIntSet.fromOwnedUnsorted(dec.readUnique());
        combiningMarks = ReadOnlyIntSet.fromOwnedUnsorted(dec.readUnique());
        maxNonSpacingMarks = dec.readUnsigned();
        nonSpacingMarks = ReadOnlyIntSet.fromOwnedUnsorted(dec.readUnique());
        NFCCheck = ReadOnlyIntSet.fromOwnedUnsorted(dec.readUnique());
        fenced = Collections.unmodifiableMap(decodeNamedCodepoints(dec));
        mapped = Collections.unmodifiableMap(decodeMapped(dec));
        groups = Collections.unmodifiableList(decodeGroups(dec));
        emojis = Collections.unmodifiableList(dec.readTree(cps -> new EmojiSequence(cps)));
        
        // precompute: confusable-extent complements
        wholes = Collections.unmodifiableList(decodeWholes(dec));
        
        // precompute: emoji trie
        for (EmojiSequence emoji: emojis) {
            ArrayList<EmojiNode> nodes = new ArrayList<>();
            nodes.add(emojiRoot);
            for (int cp: emoji.beautified.array) {
                if (cp == 0xFE0F) {
                    for (int i = 0, e = nodes.size(); i < e; i++) {
                        nodes.add(nodes.get(i).then(cp));
                    }
                } else {
                    for (int i = 0, e = nodes.size(); i < e; i++) {
                        nodes.set(i, nodes.get(i).then(cp));
                    }
                }
            }
            for (EmojiNode x: nodes) {
                x.emoji = emoji;
            }
        }
        
        // precompute: possibly valid
        HashSet<Integer> union = new HashSet<>();
        HashSet<Integer> multi = new HashSet<>();
        for (Group g: groups) {
            IntStream.concat(g.primary.stream(), g.secondary.stream()).forEach(cp -> {
                if (union.contains(cp)) {
                    multi.add(cp);
                } else {
                    union.add(cp);
                }
            });
        }
        HashSet<Integer> valid = new HashSet<>(union);
        for (int cp: NF.NFD(union.stream().mapToInt(x -> x).toArray())) valid.add(cp);
        possiblyValid = ReadOnlyIntSet.fromOwnedUnsorted(valid.stream().mapToInt(x -> x).toArray());
                
        // precompute: unique non-confusables
        HashSet<Integer> unique = new HashSet<>(union);
        unique.removeAll(multi);
        unique.removeAll(confusables.keySet());
        for (Integer cp: unique) confusables.put(cp, Whole.UNIQUE_PH); // stay boxed
        
         // precompute: special groups
        LATIN = groups.stream().filter(g -> g.name.equals("Latin")).findFirst().get();
        GREEK = groups.stream().filter(g -> g.name.equals("Greek")).findFirst().get();
        ASCII = new Group(-1, GroupKind.ASCII, "ASCII", false, ReadOnlyIntSet.fromOwnedUnsorted(possiblyValid.stream().filter(cp -> cp < 0x80).toArray()), ReadOnlyIntSet.EMPTY);
        EMOJI = new Group(-1, GroupKind.Emoji, "Emoji", false, ReadOnlyIntSet.EMPTY, ReadOnlyIntSet.EMPTY);
    }
    
    private ArrayList<Whole> decodeWholes(Decoder dec) {
        class Extent {
            final HashSet<Group> groups = new HashSet<>();
            final ArrayList<Integer> cps = new ArrayList<>();
        }
        ArrayList<Whole> ret = new ArrayList<>();
        while (true) {
            int[] confused = dec.readUnique();
            if (confused.length == 0) break;
            int[] valid = dec.readUnique();
            Whole w = new Whole(ReadOnlyIntSet.fromOwnedUnsorted(valid), ReadOnlyIntSet.fromOwnedUnsorted(confused));
            for (int cp: confused) confusables.put(cp, w);
            HashSet<Group> cover = new HashSet<>();
            ArrayList<Extent> extents = new ArrayList<>();
            IntConsumer fn = cp -> {
                List<Group> gs = groups.stream().filter(g -> g.contains(cp)).collect(Collectors.toList());
                Extent extent = extents.stream().filter(e -> gs.stream().anyMatch(g -> e.groups.contains(g))).findFirst().orElseGet(() -> {
                    Extent temp = new Extent();
                    extents.add(temp);
                    return temp;
                });
                extent.cps.add(cp);
                extent.groups.addAll(gs);
                cover.addAll(gs);
            };
            for (int cp: valid) fn.accept(cp);
            for (int cp: confused) fn.accept(cp);
            for (Extent extent: extents) {
                int[] complement = cover.stream().filter(g -> !extent.groups.contains(g)).mapToInt(g -> g.index).sorted().toArray();
                for (Integer cp: extent.cps) { // stay boxed
                    w.complements.put(cp, complement);
                }
            }
        }
        return ret;
    }
    
    static HashMap<Integer,String> decodeNamedCodepoints(Decoder dec) {
        HashMap<Integer,String> ret = new HashMap<>();
        for (int cp: dec.readSortedAscending(dec.readUnsigned())) {
            ret.put(cp, dec.readString());
        }
        return ret;        
    }
    
    static HashMap<Integer,ReadOnlyIntList> decodeMapped(Decoder dec) {
        HashMap<Integer,ReadOnlyIntList> ret = new HashMap<>();
        while (true) {
            int w = dec.readUnsigned();
            if (w == 0) break;
            int[] keys = dec.readSortedUnique();
            int n = keys.length;
            int[][] m = new int[n][w];
            for (int j = 0; j < w; j++) {
                int[] v = dec.readUnsortedDeltas(n);
                for (int i = 0; i < n; i++) m[i][j] = v[i];
            }
            for (int i = 0; i < n; i++) {
                ret.put(keys[i], new ReadOnlyIntList(m[i]));
            }
        }
        return ret;
    }
    
    static ArrayList<Group> decodeGroups(Decoder dec) {
        ArrayList<Group> ret = new ArrayList<>();
        while (true)  {
            String name = dec.readString();
            if (name.isEmpty()) break;
            int bits = dec.readUnsigned();
            GroupKind kind = (bits & 1) != 0 ? GroupKind.Restricted : GroupKind.Script;
            boolean cm = (bits & 2) != 0;
            ret.add(new Group(ret.size(), kind, name, cm, ReadOnlyIntSet.fromOwnedUnsorted(dec.readUnique()), ReadOnlyIntSet.fromOwnedUnsorted(dec.readUnique())));
        }
        return ret;
    }
    
     // format as {HEX}
    static void appendHexEscape(StringBuilder sb, int cp) {
        sb.append('{');
        StringUtils.appendHex(sb, cp);
        sb.append('}');
    }
    
    // printable: "X" {HEX}
    // otherwise: {HEX} 
    public String safeCodepoint(int cp) {
        StringBuilder sb = new StringBuilder();
        if (!shouldEscape.contains(cp)) {
            sb.append('"');
            safeImplode(sb, new int[]{ cp });
            sb.append('"');
            sb.append(' ');
        }
        appendHexEscape(sb, cp);
        return sb.toString();
    }
    
    public String safeImplode(int... cps) { 
        StringBuilder sb = new StringBuilder(cps.length + 16);
        safeImplode(sb, cps); 
        return sb.toString();
    }
    
    public void safeImplode(StringBuilder sb, int[] cps) {
        if (cps.length == 0) return;
        if (combiningMarks.contains(cps[0])) {
            StringUtils.appendCodepoint(sb, 0x25CC);
        }
        for (int cp: cps) {
            if (shouldEscape.contains(cp)) {
                appendHexEscape(sb, cp);
            } else {
                StringUtils.appendCodepoint(sb, cp);
            }
        }
        // some messages can be mixed-directional and result in spillover
        // use 200E after a input string to reset the bidi direction
        // https://www.w3.org/International/questions/qa-bidi-unicode-controls#exceptions
        StringUtils.appendCodepoint(sb, 0x200E);
    }
        
    static int[] flatten(List<OutputToken> tokens) {
        return tokens.stream().flatMapToInt(t -> t.stream()).toArray();
    }
    
    public String normalize(String name) {
        return transform(name, cps -> outputTokenize(cps, NF::NFC, e -> e.normalized.array), tokens -> {
            int[] norm = flatten(tokens);
            checkValidLabel(norm, tokens);
            return norm;
        });
    }
    
    public String beautify(String name) {
        return transform(name, cps -> outputTokenize(cps, NF::NFC, e -> e.beautified.array), tokens -> {
            int[] norm = flatten(tokens);
            Group group = checkValidLabel(norm, tokens);
            if (group != GREEK) {
                for (int i = 0, e = norm.length; i < e; i++) {
                    if (norm[i] == 0x3BE) norm[i] = 0x39E;
                }
            }
            return norm;
        });        
    }
    
    public NormDetails normalizeDetails(String name)  {
        HashSet<Group> groups = new HashSet<>();
        HashSet<EmojiSequence> emojis = new HashSet<>();
        String normed = transform(name, cps -> outputTokenize(cps, NF::NFC, e -> e.normalized.array), tokens -> {
            int[] norm = flatten(tokens);
            Group group = checkValidLabel(norm, tokens);
            for (OutputToken t: tokens) {
                if (t.emoji != null) {
                    emojis.add(t.emoji);
                }
            }
            if (group == LATIN && tokens.stream().allMatch(t -> t.emoji != null || t.stream().allMatch(cp -> cp < 0x80))) {
                group = ASCII;
            }
            groups.add(group);
            return norm;
        });
        if (groups.contains(LATIN)) {
            groups.remove(ASCII);
        }
        if (!emojis.isEmpty()) {
            groups.add(EMOJI);
        }
        boolean confusing = Arrays.stream(POSSIBLY_CONFUSING).anyMatch(s -> normed.contains(s));
        return new NormDetails(normed, groups, emojis, confusing);
    }
    
    public String normalizeFragment(String name) { return normalizeFragment(name, false); }
    public String normalizeFragment(String name, boolean decompose) {
        return transform(name, cps -> outputTokenize(cps, decompose ? NF::NFD : NF::NFC, e -> e.normalized.array), ENSIP15::flatten);
    }
    
    String transform(String name, Function<int[], List<OutputToken>> tokenizer, Function<List<OutputToken>, int[]> normalizer) {
        int n = name.length();
        if (n == 0) return ""; // empty name allowance
        StringBuilder sb = new StringBuilder(n + 16); // guess
        int prev = 0;
        boolean more = true;
        while (more) {
            int next = name.indexOf(STOP_CH, prev);
            if (next < 0) {
                next = n;
                more = false;
            }
            int[] cps = StringUtils.explode(name, prev, next);
            try {
                List<OutputToken> tokens = tokenizer.apply(cps);
                for (int cp: normalizer.apply(tokens)) {
                    StringUtils.appendCodepoint(sb, cp);
                }
                if (more) sb.append(STOP_CH);
            } catch (NormException e) {
                throw new InvalidLabelException(prev, next, String.format("Invalid label \"%s\": %s", safeImplode(cps), e.getMessage()), e);
            }
            prev = next + 1;
        }
        return sb.toString();
    }
    
    public List<Label> split(String name) {
        if (name.isEmpty()) return Collections.emptyList(); // empty name allowance
        ArrayList<Label> labels = new ArrayList<>();
        int prev = 0;
        boolean more = true;
        while (more) {
            int next = name.indexOf(STOP_CH, prev);
            if (next < 0) {
                next = name.length();
                more = false;
            }
            Label l = new Label();
            l.input = StringUtils.explode(name, prev, next);
            l.start = prev;
            l.end = next;
            try {
                l.tokens = outputTokenize(l.input, NF::NFC, e -> e.normalized.toArray()); // make copy
                l.normalized = flatten(l.tokens);
                l.group = checkValidLabel(l.normalized, l.tokens);
            } catch (NormException err) {
                l.error = err;
            }
            labels.add(l);
            prev = next + 1;
        }
        labels.trimToSize();
        return labels;
    }
    
    static class EmojiNode {
        EmojiSequence emoji;
        HashMap<Integer, EmojiNode> map;
        EmojiNode then(int cp) {
            if (map == null) map = new HashMap<>();
            EmojiNode node = map.get(cp);
            if (node == null) {
                node = new EmojiNode();
                map.put(cp, node);
            }
            return node;
        }
    }
    
    static class EmojiResult {
        final int pos;
        final EmojiSequence emoji;
        EmojiResult(int pos, EmojiSequence emoji) {
            this.pos = pos;
            this.emoji = emoji;
        }
    }
    
    EmojiResult findEmoji(int[] cps, int i) {
        EmojiNode node = emojiRoot;
        EmojiResult last = null;
        for (int e = cps.length; i < e; ) {
            if (node.map == null) break;
            node = node.map.get(cps[i++]);
            if (node == null) break;
            if (node.emoji != null) {
                last = new EmojiResult(i, node.emoji); 
            }
        }
        return last;
    }
    
    ArrayList<OutputToken> outputTokenize(int[] cps, Function<int[], int[]> nf, Function<EmojiSequence, int[]> emojiStyler) {
        ArrayList<OutputToken> tokens = new ArrayList<>();
        int n = cps.length;
        IntList buf = new IntList(n);
        for (int i = 0; i < n; ) {
            EmojiResult match = findEmoji(cps, i);
            if (match != null) {
                if (buf.count > 0) {
                    tokens.add(new OutputToken(nf.apply(buf.consume()), null));
                    buf.count = 0;
                }
                tokens.add(new OutputToken(emojiStyler.apply(match.emoji), match.emoji)); 
                i = match.pos;
            } else {
                int cp = cps[i++];
                if (possiblyValid.contains(cp)) {
                    buf.add(cp);
                } else {
                    ReadOnlyIntList replace = mapped.get(cp);
                    if (replace != null) {
                        buf.add(replace.array);
                    } else if (!ignored.contains(cp)) {
                        throw new DisallowedCharacterException(safeCodepoint(cp), cp);
                    }
                }
            }
        }
        if (buf.count > 0) {
            tokens.add(new OutputToken(nf.apply(buf.consume()), null));
        }
        return tokens;
    }
    
    Group checkValidLabel(int[] norm, List<OutputToken> tokens) {
        if (norm.length == 0) {
            throw new NormException(EMPTY_LABEL);
        }
        checkLeadingUnderscore(norm);
        boolean emoji = tokens.size() > 1 || tokens.get(0).emoji != null;
        if (!emoji && Arrays.stream(norm).allMatch(cp -> cp < 0x80)) {
            checkLabelExtension(norm);
            return ASCII;
        }
        int[] chars = tokens.stream().filter(t -> t.emoji == null).flatMapToInt(t -> t.stream()).toArray();
        if (emoji && chars.length == 0) {
            return EMOJI;
        }
        checkCombiningMarks(tokens);
        checkFenced(norm);
        int[] unique = Arrays.stream(chars).distinct().toArray();
        Group group = determineGroup(unique);
        checkGroup(group, chars); // need text in order
        checkWhole(group, unique); // only need unique text
        return group;
    }
    
    static void checkLeadingUnderscore(int[] cps) {
        final int UNDERSCORE = 0x5F;
        boolean allowed = true;
        for (int cp: cps) {
            if (allowed) {
                if (cp != UNDERSCORE) allowed = false;
            } else {
                if (cp == UNDERSCORE) {
                    throw new NormException(INVALID_UNDERSCORE);
                }
            }
        }
    }
    
    static void checkLabelExtension(int[] cps)  {
        final int HYPHEN = 0x2D;
        if (cps.length >= 4 && cps[2] == HYPHEN && cps[3] == HYPHEN) {
            throw new NormException(INVALID_LABEL_EXTENSION, StringUtils.implode(Arrays.copyOf(cps, 4)));
        }
    }
    
    void checkFenced(int[] cps)  {
        String name = fenced.get(cps[0]);
        if (name != null) {
            throw new NormException(FENCED_LEADING, name);
        }
        int n = cps.length;
        int last = -1;
        String prev = "";
        for (int i = 1; i < n; i++) {
            name = fenced.get(cps[i]);
            if (name != null) {
                if (last == i) {
                    throw new NormException(FENCED_ADJACENT, String.format("%s + %s", prev, name));
                }
                last = i + 1;
                prev = name;
            }
        }
        if (last == n) {
            throw new NormException(FENCED_TRAILING, prev);
        }
    }
    
    void checkCombiningMarks(List<OutputToken> tokens) {
        for (int i = 0, e = tokens.size(); i < e; i++) {
            OutputToken t = tokens.get(i);
            if (t.emoji != null) continue;
            int cp = t.cps[0];
            if (combiningMarks.contains(cp)) {
                if (i == 0) {
                    throw new NormException(CM_LEADING, safeCodepoint(cp));
                } else {
                    throw new NormException(CM_AFTER_EMOJI, String.format("%s + %s", tokens.get(i - 1).emoji.form, safeCodepoint(cp)));
                }
            }
        }
    }
    
    Group determineGroup(int[] unique) {
        int prev = groups.size();
        Group[] gs = groups.toArray(new Group[prev]);
        for (int cp: unique) {
            int next = 0;
            for (int i = 0; i < prev; i++) {
                if (gs[i].contains(cp)) {
                    gs[next++] = gs[i];                    
                }
            }
            if (next == 0) {   
                if (!groups.stream().anyMatch(g -> g.contains(cp))) {
                    // the character was composed of valid parts
                    // but it's NFC form is invalid
                    throw new DisallowedCharacterException(safeCodepoint(cp), cp);
                } else {
                    // there is no group that contains all these characters
                    // throw using the highest priority group that matched
                    // https://www.unicode.org/reports/tr39/#mixed_script_confusables
                    throw createMixtureException(gs[0], cp);
                }
            }    
            prev = next;
            if (prev == 1) break; // there is only one group left
        }
        return gs[0];
    }
    
    void checkGroup(Group group, int[] cps) {
        for (int cp: cps) {
            if (!group.contains(cp)) {
                throw createMixtureException(group, cp);
            }
        }
        if (group.CMWhitelisted) return;
        int[] decomposed = NF.NFD(cps);
        for (int i = 1, e = decomposed.length; i < e; i++) {
            // https://www.unicode.org/reports/tr39/#Optional_Detection
            if (nonSpacingMarks.contains(decomposed[i])) {
                int j = i + 1;
                for (int cp; j < e && nonSpacingMarks.contains(cp = decomposed[j]); j++) {
                    for (int k = i; k < j; k++) {
                        // a. Forbid sequences of the same nonspacing mark.
                        if (decomposed[k] == cp) {
                            throw new NormException(NSM_DUPLICATE, safeCodepoint(cp));
                        }
                    }
                }
                // b. Forbid sequences of more than 4 nonspacing marks (gc=Mn or gc=Me).
                int n = j - i;
                if (n > maxNonSpacingMarks) {
                    throw new NormException(NSM_EXCESSIVE, String.format("%s (%d/%d)", safeImplode(Arrays.copyOfRange(decomposed, i-1, j)), n, maxNonSpacingMarks));
                }
                i = j;
            }
        }
    }
    
    void checkWhole(Group group, int[] unique) {
        int bound = 0;
        int[] maker = null;
        IntList shared = new IntList(unique.length);
        for (int cp: unique) {
            Whole w = confusables.get(cp);
            if (w == null) {
                shared.add(cp);
            } else if (w == Whole.UNIQUE_PH) {
                return; // unique, non-confusable
            } else {
                int[] comp = w.complements.get(cp); // exists by construction
                if (bound == 0) {
                    maker = comp.clone();
                    bound = comp.length; 
                } else {
                    int b = 0;
                    for (int i = 0; i < bound; i++) {
                        if (Arrays.binarySearch(comp, maker[i]) >= 0) {
                            maker[b++] = maker[i];
                        }
                    }
                    bound = b;
                }
                if (bound == 0) {
                    return; // confusable intersection is empty
                }
            }
        }
        if (bound > 0) {
            next: for (int i = 0; i < bound; i++) {
                Group other = groups.get(maker[i]);
                if (shared.stream().allMatch(other::contains)) {
                    throw new ConfusableException(group, other);
                }
            }
        }
    }
    
    IllegalMixtureException createMixtureException(Group group, int cp) {
        String conflict = safeCodepoint(cp);
        Group other = groups.stream().filter(g -> g.primary.contains(cp)).findFirst().orElse(null);
        if (other != null) {
            conflict = String.format("%s %s", other, conflict);
        }
        return new IllegalMixtureException(String.format("%s + %s", group, conflict), cp, group, other);
    }
    
}
