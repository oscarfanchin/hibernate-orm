package org.hibernate.orm.test.bytecode.enhance.client;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.util.Map;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.TransformerException;

import net.bytebuddy.agent.ByteBuddyAgent;

import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.testing.orm.jpa.PersistenceUnitInfoImpl;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/// Load-time weaving as a container does it: the transformer passed to `PersistenceUnitInfo#addTransformer` is
/// registered with the JVM's [Instrumentation] and called for every class the persistence unit's loader defines.
///
/// Runs in its own JVM (see the `loadTimeWeavingTest` task): it can only fail while Byte Buddy's classes are not
/// loaded yet, as at application startup.
@JiraKey("HHH-20753")
class LoadTimeWeavingTest {

	private final Instrumentation instrumentation = ByteBuddyAgent.install();
	private ClassFileTransformer weaver;

	@AfterEach
	void removeWeaver() {
		if ( weaver != null ) {
			instrumentation.removeTransformer( weaver );
		}
	}

	@Test
	void bootstrapsEntityManagerFactory() {
		final ClassLoader classLoader = getClass().getClassLoader();
		final PersistenceUnitInfoImpl unit = new PersistenceUnitInfoImpl( "hhh-20753" ) {
			@Override
			public void addTransformer(ClassTransformer transformer) {
				weaver = weaver( transformer, classLoader );
				instrumentation.addTransformer( weaver );
			}

			@Override
			public ClassLoader getNewTempClassLoader() {
				return new URLClassLoader( new URL[0], classLoader );
			}
		};
		unit.setClassLoader( classLoader );
		unit.setExcludeUnlistedClasses( true );
		// By name, so the entity is not loaded before the transformer is registered
		unit.applyManagedClassNames( LoadTimeWeavingTest.class.getName() + "$DataEntity" );

		new HibernatePersistenceProvider().createContainerEntityManagerFactory( unit, Map.of() ).close();
	}

	private static ClassFileTransformer weaver(ClassTransformer transformer, ClassLoader classLoader) {
		return new ClassFileTransformer() {
			@Override
			public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
					ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
				if ( loader != classLoader ) {
					return null;
				}
				try {
					return transformer.transform( loader, className, classBeingRedefined, protectionDomain,
							classfileBuffer );
				}
				catch (TransformerException e) {
					throw (IllegalClassFormatException) new IllegalClassFormatException( e.getMessage() ).initCause( e );
				}
			}
		};
	}

	@MappedSuperclass
	public abstract static class GenericMappedSuperclass<T> {
		T payload;
	}

	@Entity(name = "Hhh20753Entity")
	public static class DataEntity extends GenericMappedSuperclass<String> {
		@Id
		long id;
	}
}
