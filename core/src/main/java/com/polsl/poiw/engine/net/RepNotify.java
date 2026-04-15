package com.polsl.poiw.engine.net;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * defines a replicated field that calls a callback after update from server
 * callback must be a no-args method in the same class
 * 
 * Example:
 * 
 * <pre>
 * {@literal @}Replicated
 * {@literal @}RepNotify("onHealthChanged")
 * private float health;
 *
 * private void onHealthChanged() {
 *   // do something after health update from server
 * }
 * </pre>
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RepNotify {
    // name of the callback method (without args) called after field update
    String value();
}
