package composite.kinds;

public class Leaf implements Component {
    private final String name;

    public Leaf(String name) { this.name = name; }

    @Override public String render() { return name + "\n"; }
}
