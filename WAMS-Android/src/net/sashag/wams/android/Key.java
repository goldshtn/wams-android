package net.sashag.wams.android;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to indicate that a field in a class decorated by {@link DataTable} is the key
 * in the mobile table. You can decorate only a single field in a class with this 
 * annotation. The field must be of type <code>int</code>.
 * 
 * @author Sasha Goldshtein
 *
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Key {
}
