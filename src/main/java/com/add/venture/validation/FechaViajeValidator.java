package com.add.venture.validation;

import java.time.LocalDate;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class FechaViajeValidator implements ConstraintValidator<FechaViaje, LocalDate> {

    @Override
    public void initialize(FechaViaje constraintAnnotation) {
    }

    @Override
    public boolean isValid(LocalDate fecha, ConstraintValidatorContext context) {
        if (fecha == null) {
            return true; // Dejar que @NotNull maneje esto
        }

        LocalDate hoy = LocalDate.now();
        LocalDate unaSemanaAntes = hoy.plusDays(7);

        // La fecha debe ser al menos 1 semana después de hoy
        return !fecha.isBefore(unaSemanaAntes);
    }
}
