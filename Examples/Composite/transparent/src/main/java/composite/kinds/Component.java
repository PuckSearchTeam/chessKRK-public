package composite.kinds;

import java.util.List;

/**
 * The Composite pattern, transparent variant (Gamma et al. 1994, the choice their sample code takes): the
 * child-management operations are declared in Component, so that every client treats every component alike, and
 * a Leaf must implement operations it cannot honour. The safe variant differs in nothing else.
 */
public interface Component {
    String render();
    void add(Component child);
    void remove(Component child);
    List<Component> children();
}
