package composite.editing;

import composite.kinds.Component;
import composite.kinds.Leaf;

/** A client that edits any component alike: transparent, and unsafe on a Leaf, which refuses at run time. */
public class Editor {
    public void addLeaf(Component target, String name) { target.add(new Leaf(name)); }
}
