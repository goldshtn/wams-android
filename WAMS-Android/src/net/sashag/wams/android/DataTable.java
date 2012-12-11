package net.sashag.wams.android;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the class decorated by this annotation is mapped to
 * a mobile table in Windows Azure Mobile Services. There are no special
 * requirements of this class -- it may be a Plain Old Java Object (POJO).
 * The table columns are indicated by the {@link DataMember} and {@link Key}
 * annotations.
 * 
 * @author Sasha Goldshtein
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataTable {
	/**
	 * The name of the table. This is the name used by the SQL Server instance
	 * behind the Windows Azure Mobile Service.
	 * 
	 * @return the name of the table
	 */
	String value();
}
