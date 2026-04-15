package com.polsl.poiw.engine.net;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * component field that should be replicated over the network
 * replicationsystem scans for fields with this annotation and sends changed values to clients
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Replicated {
    ReplicationCondition condition() default ReplicationCondition.ALWAYS;
}
