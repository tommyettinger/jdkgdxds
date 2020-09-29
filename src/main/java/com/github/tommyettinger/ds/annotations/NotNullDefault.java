package com.github.tommyettinger.ds.annotations;

import javax.annotation.*;
import javax.annotation.meta.*;
import java.lang.annotation.*;

@Documented
@Nonnull
@TypeQualifierDefault({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
@Retention(RetentionPolicy.CLASS)
public @interface NotNullDefault {}
