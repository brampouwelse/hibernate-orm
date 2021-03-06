/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.tools;

import java.util.Map;

import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.cfg.Configuration;
import org.hibernate.envers.configuration.spi.AuditConfiguration;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.tools.Pair;
import org.hibernate.internal.util.collections.ConcurrentReferenceHashMap;
import org.hibernate.property.Getter;
import org.hibernate.property.PropertyAccessor;
import org.hibernate.property.PropertyAccessorFactory;
import org.hibernate.property.Setter;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public abstract class ReflectionTools {
	private static final Map<Pair<Class, String>, Getter> GETTER_CACHE = new ConcurrentReferenceHashMap<Pair<Class, String>, Getter>(
			10,
			ConcurrentReferenceHashMap.ReferenceType.SOFT,
			ConcurrentReferenceHashMap.ReferenceType.SOFT
	);
	private static final Map<Pair<Class, String>, Setter> SETTER_CACHE = new ConcurrentReferenceHashMap<Pair<Class, String>, Setter>(
					10,
					ConcurrentReferenceHashMap.ReferenceType.SOFT,
					ConcurrentReferenceHashMap.ReferenceType.SOFT
	);

	private static PropertyAccessor getAccessor(String accessorType) {
		return PropertyAccessorFactory.getPropertyAccessor( accessorType );
	}

	public static Getter getGetter(Class cls, PropertyData propertyData) {
		return getGetter( cls, propertyData.getBeanName(), propertyData.getAccessType() );
	}

	public static Getter getGetter(Class cls, String propertyName, String accessorType) {
		final Pair<Class, String> key = Pair.make( cls, propertyName );
		Getter value = GETTER_CACHE.get( key );
		if ( value == null ) {
			value = getAccessor( accessorType ).getGetter( cls, propertyName );
			// It's ok if two getters are generated concurrently
			GETTER_CACHE.put( key, value );
		}

		return value;
	}

	public static Setter getSetter(Class cls, PropertyData propertyData) {
		return getSetter( cls, propertyData.getBeanName(), propertyData.getAccessType() );
	}

	public static Setter getSetter(Class cls, String propertyName, String accessorType) {
		final Pair<Class, String> key = Pair.make( cls, propertyName );
		Setter value = SETTER_CACHE.get( key );
		if ( value == null ) {
			value = getAccessor( accessorType ).getSetter( cls, propertyName );
			// It's ok if two setters are generated concurrently
			SETTER_CACHE.put( key, value );
		}

		return value;
	}

	/**
	 * @param clazz Source class.
	 * @param propertyName Property name.
	 *
	 * @return Property object or {@code null} if none with expected name has been found.
	 */
	public static XProperty getProperty(XClass clazz, String propertyName) {
		XProperty property = getProperty( clazz, propertyName, "field" );
		if ( property == null ) {
			property = getProperty( clazz, propertyName, "property" );
		}
		return property;
	}

	/**
	 * @param clazz Source class.
	 * @param propertyName Property name.
	 * @param accessType Expected access type. Legal values are <i>field</i> and <i>property</i>.
	 *
	 * @return Property object or {@code null} if none with expected name and access type has been found.
	 */
	public static XProperty getProperty(XClass clazz, String propertyName, String accessType) {
		for ( XProperty property : clazz.getDeclaredProperties( accessType ) ) {
			if ( propertyName.equals( property.getName() ) ) {
				return property;
			}
		}
		return null;
	}

	/**
	 * Locate class with a given name.
	 *
	 * @param name Fully qualified class name.
	 * @param classLoaderService Class loading service. Passing {@code null} reference
	 * in case of {@link AuditConfiguration#getFor(Configuration)} usage.
	 *
	 * @return The cass reference.
	 *
	 * @throws ClassLoadingException Indicates the class could not be found.
	 */
	@SuppressWarnings("unchecked")
	public static <T> Class<T> loadClass(String name, ClassLoaderService classLoaderService)
			throws ClassLoadingException {
		try {
			if ( classLoaderService != null ) {
				return classLoaderService.classForName( name );
			}
			else {
				return (Class<T>) Thread.currentThread().getContextClassLoader().loadClass( name );
			}
		}
		catch (Exception e) {
			throw new ClassLoadingException( "Unable to load class [" + name + "]", e );
		}
	}
}
