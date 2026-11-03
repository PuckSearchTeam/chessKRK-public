package composite.editing;

import composite.kinds.Component;
import composite.kinds.Composite;
import composite.kinds.Leaf;

/** A client that edits the structure: safe, since only a Composite can be given a child, and bound to the kind it edits. */
public class Editor {
    public void addLeaf(Component target, String name) {
        if (target instanceof Composite composite) {
            composite.add(new Leaf(name));
        } else {
            throw new IllegalArgumentException("not a composite");
        }
    }
}
