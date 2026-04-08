package com.polsl.poiw.engine.binding;

/**
 * Uchwyt do bindingu — pozwala odłączyć listener od PropertyBinding.
 * <p>
 * Po wywołaniu {@link #unbind()} listener nie będzie już wywoływany
 * i zostanie usunięty przy następnej notyfikacji.
 */
public interface BindingHandle {

    /** Odłącza binding — listener nie będzie już wywoływany */
    void unbind();

    /** Czy binding jest nadal aktywny? */
    boolean isBound();
}
