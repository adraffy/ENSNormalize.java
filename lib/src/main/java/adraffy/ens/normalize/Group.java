package adraffy.ens.normalize;

public class Group {
    
    public final int index;
    public final GroupKind kind;
    public final String name;
    public final boolean CMWhitelisted;
    public final ReadOnlyIntSet primary;
    public final ReadOnlyIntSet secondary;
    
    Group(int index, GroupKind kind, String name, boolean cm, ReadOnlyIntSet primary, ReadOnlyIntSet secondary) {
        this.index = index;
        this.kind = kind;
        this.name = name;
        CMWhitelisted = cm;
        this.primary = primary;
        this.secondary = secondary;
    }
    
    public boolean contains(int cp) {
        return primary.contains(cp) || secondary.contains(cp);
    }
    
    public boolean isRestricted() {
        return kind == GroupKind.Restricted;
    }
    
    @Override
    public String toString() {
        return isRestricted() ? String.format("Restricted[%s]", name) : name;
    }
    
}
