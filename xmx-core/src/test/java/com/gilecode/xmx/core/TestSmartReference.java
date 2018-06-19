// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.core;

import org.junit.Before;
import org.junit.Test;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class TestSmartReference {

	private Object obj = new Object();
	private ManagedClassLoaderWeakRef uut;
	private Field weakRefField;
	private Field strongRefField;
	private Field smartReferncesField;

	@Before
	public void setup() throws Exception {
		uut = new ManagedClassLoaderWeakRef(TestSmartReference.class.getClassLoader(), null, null);
		weakRefField = ManagedClassLoaderWeakRef.SmartReference.class.getDeclaredField("weakReference");
		weakRefField.setAccessible(true);
		strongRefField = ManagedClassLoaderWeakRef.SmartReference.class.getDeclaredField("strongReference");
		strongRefField.setAccessible(true);
		smartReferncesField = ManagedClassLoaderWeakRef.class.getDeclaredField("smartReferences");
		smartReferncesField.setAccessible(true);
	}

	private Object getStrongRef(ManagedClassLoaderWeakRef.SmartReference<?> ref)  throws Exception {
		return strongRefField.get(ref);
	}

	private WeakReference<?> getWeakRef(ManagedClassLoaderWeakRef.SmartReference<?> ref)  throws Exception {
		return (WeakReference<?>) weakRefField.get(ref);
	}

	@SuppressWarnings("unchecked")
	private boolean isManaged(ManagedClassLoaderWeakRef.SmartReference<?> ref)  throws Exception {
		Collection<ManagedClassLoaderWeakRef.SmartReference<?>> c = (Collection<ManagedClassLoaderWeakRef.SmartReference<?>>) smartReferncesField.get(uut);
		return c.contains(ref);
	}

	private void assertStrong(ManagedClassLoaderWeakRef.SmartReference<?> ref) throws Exception {
		assertEquals(obj, getStrongRef(ref));
		assertNotNull(getWeakRef(ref));
		assertEquals(obj, ref.get());
	}

	private void assertWeak(ManagedClassLoaderWeakRef.SmartReference<?> ref) throws Exception {
		assertNull(getStrongRef(ref));
		assertNotNull(getWeakRef(ref));
		assertEquals(obj, getWeakRef(ref).get());
		assertEquals(obj, ref.get());
	}

	@Test
	public void testInitialWeak() throws Exception {
		ManagedClassLoaderWeakRef.SmartReference<Object> ref = uut.createSmartReference(obj);

		assertTrue(isManaged(ref));
		assertWeak(ref);
	}

	@Test
	public void testInitialStrong() throws Exception {
		ManagedClassLoaderWeakRef.SmartReference<Object> ref = uut.createSmartReference(obj);
		uut.incrementManagedInstancesCount();

		assertTrue(isManaged(ref));
		assertStrong(ref);
	}

	@Test
	public void testChanges() throws Exception {
		ManagedClassLoaderWeakRef.SmartReference<Object> ref = uut.createSmartReference(obj);

		uut.incrementManagedInstancesCount();
		uut.incrementManagedInstancesCount();
		assertStrong(ref);

		uut.decrementManagedInstancesCount();
		assertStrong(ref);

		uut.decrementManagedInstancesCount();
		assertWeak(ref);

		uut.incrementManagedInstancesCount();
		assertStrong(ref);
		assertTrue(isManaged(ref));
	}

	@Test
	public void testRemove() throws Exception {
		ManagedClassLoaderWeakRef.SmartReference<Object> ref = uut.createSmartReference(obj);
		assertTrue(isManaged(ref));

		uut.removeSmartReference(ref);
		assertFalse(isManaged(ref));

		assertWeak(ref);
		uut.incrementManagedInstancesCount();
		assertWeak(ref);
	}

	@Test
	public void testAutoRemove() throws Exception {
		Object temp = new Object();
		ManagedClassLoaderWeakRef.SmartReference<Object> ref = uut.createSmartReference(temp);
		WeakReference<Object> weakRef = new WeakReference<>(temp);
		temp = null;

		for (int i = 0; i < 10; i++) {
			System.gc();
			if (weakRef.get() == null) {
				assertNull(getWeakRef(ref).get());
				assertNull(ref.get());

				uut.incrementManagedInstancesCount();
				assertFalse(isManaged(ref));
				return;
			}
		}

		fail("Failed to test, as System.gc() did not collect weak ref in a reasonable time");
	}

	AtomicReference<Boolean> failureFlag = new AtomicReference<>(false);

	@Test
	public void testConcurrentChanges() throws Exception {
		final ManagedClassLoaderWeakRef.SmartReference<Object> ref = uut.createSmartReference(obj);
		ExecutorService executorService = Executors.newFixedThreadPool(4);

		for (int i = 0; i < 10; i++) {
			executorService.invokeAll(createTasks(ref, 100, 100));
			assertWeak(ref);
			assertFalse(failureFlag.get());
		}

		for (int i = 0; i < 10; i++) {
			executorService.invokeAll(createTasks(ref, 101, 100));
			assertStrong(ref);
			assertFalse(failureFlag.get());
			uut.decrementManagedInstancesCount();
			assertWeak(ref);
		}
	}

	private Collection<Callable<Void>> createTasks(final ManagedClassLoaderWeakRef.SmartReference<Object> ref,
	                                               int nIncrements, int nDecrements) {
		Collection<Callable<Void>> tasks = new HashSet<>(); // HashSet for random order
		for (int i = 0; i < nIncrements; i++) {
			tasks.add(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					uut.incrementManagedInstancesCount();
					if (ref.get() != obj) {
						failureFlag.set(true);
					}
					return null;
				}
			});
		}
		for (int i = 0; i < nDecrements; i++) {
			tasks.add(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					uut.decrementManagedInstancesCount();
					if (ref.get() != obj) {
						failureFlag.set(true);
					}
					return null;
				}
			});
		}
		return tasks;
	}
}
