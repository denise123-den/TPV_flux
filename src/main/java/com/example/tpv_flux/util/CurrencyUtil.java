package com.example.tpv_flux.util;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utilidad para formatear precios y montos de moneda como en el mockup ($1,040.52).
 */
public final class CurrencyUtil {

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.US);

    private CurrencyUtil() {
    }

    public static String format(double amount) {
        return CURRENCY_FORMAT.format(amount);
    }
}
