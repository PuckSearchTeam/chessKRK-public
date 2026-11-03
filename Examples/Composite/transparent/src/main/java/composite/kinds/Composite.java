package composite.kinds;

import java.util.ArrayList;
import java.util.List;

public class Composite implements Component {
    private final List<Component> children = new ArrayList<>();

    @Override public String render() {
        StringBuilder out = new StringBuilder("(\n");
        for (Component child : children) {
            out.append(child.render());
        }
        return out.append(")\n").toString();
    }

    @Override public void add(Component child) { children.add(child); }

    @Override public void remove(Component child) { children.remove(child); }

    @Override public List<Component> children() { return children; }
}
