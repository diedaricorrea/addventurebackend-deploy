package com.add.venture.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FechaViajeValidator.class)
public @interface FechaViaje {
    String message() default "La fecha debe ser al menos 1 semana después de hoy";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
