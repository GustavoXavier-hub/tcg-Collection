package com.gustavo.tcgcollection;

import static org.junit.Assert.assertFalse;

import com.gustavo.tcgcollection.fonte.OnePieceFonte;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.regex.Pattern;

/** O Android usa ICU: (?U) crasha o app ao abrir e \b se comporta diferente. */
public class RegexIcuGuardTest {

    private static final Class<?>[] CLASSES_COM_REGEX = {
            OnePieceFonte.class,
    };

    @Test
    public void patternsCompativeisComOIcuDoAndroid() throws Exception {
        for (Class<?> k : CLASSES_COM_REGEX) {
            for (Field f : k.getDeclaredFields()) {
                if (!Pattern.class.equals(f.getType()) || !Modifier.isStatic(f.getModifiers())) continue;
                f.setAccessible(true);
                String p = ((Pattern) f.get(null)).pattern();
                assertFalse("(?U) em " + k.getSimpleName() + "." + f.getName(), p.contains("(?U)"));
                assertFalse("\\b em " + k.getSimpleName() + "." + f.getName(), p.contains("\\b"));
            }
        }
    }
}
