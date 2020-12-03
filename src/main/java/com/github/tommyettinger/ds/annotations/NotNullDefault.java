package com.github.tommyettinger.ds.annotations;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Documented
@NonNull
@DefaultQualifier(value = NonNull.class, locations = {TypeUseLocation.ALL})
@Retention(RetentionPolicy.RUNTIME)
public @interface NotNullDefault {
}
