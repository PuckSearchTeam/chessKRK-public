package composite.rendering;

import composite.kinds.Component;

/** A client of the rendering operation, which lives in the kinds. */
public class Renderer {
    public String render(Component root) { return root.render(); }
}
