package net.sashag.wams.android;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the field decorated by this annotation is mapped to a column
 * in a mobile table in Windows Azure Mobile Services. You can decorate multiple
 * fields by this annotation. The supported field types are <code>int</code>,
 * <code>long</code>, <code>double</code>, <code>boolean</code>, and <code>String</code>.
 * 
 * @author Sasha Goldshtein
 *
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataMember {
	
	/**
	 * The name of the column in the SQL Server table behind the Windows Azure Mobile Service.
	 * This is also the column name you use in any server-side scripts.
	 * 
	 * @return the name of the column
	 */
	String value();
}
