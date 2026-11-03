package composite.kinds;

/**
 * The Composite pattern, safe variant (Gamma et al. 1994, the "safe" choice; Martin's variation declares the common
 * operations only): the child-management operations are declared in Composite alone, so that the type system
 * refuses children on a leaf, and a client that edits the structure must know it holds a Composite. The
 * transparent variant differs in nothing else.
 */
public interface Component {
    String render();
}
