package com.polsl.poiw.engine.net;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// defines a method as an rpc called on the server (clinet -> server)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ServerRPC {
    boolean reliable() default true;
}
