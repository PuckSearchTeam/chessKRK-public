package composite.kinds;

import java.util.List;

public class Leaf implements Component {
    private final String name;

    public Leaf(String name) { this.name = name; }

    @Override public String render() { return name + "\n"; }

    @Override public void add(Component child) { throw new UnsupportedOperationException("a leaf has no children"); }

    @Override public void remove(Component child) { throw new UnsupportedOperationException("a leaf has no children"); }

    @Override public List<Component> children() { return List.of(); }
}
