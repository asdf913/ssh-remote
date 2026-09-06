package com.jcraft.jsch;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import org.apache.commons.configuration2.ImmutableConfiguration;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.function.FailableFunction;
import org.apache.commons.lang3.function.FailablePredicate;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.common.base.Predicates;
import com.google.common.net.HostAndPort;
import com.google.common.reflect.Reflection;

import io.github.toolfactory.narcissus.Narcissus;

public class SshMainTest {

	private static Method METHOD_EXISTS, METHOD_IS_FILE, METHOD_CAN_READ, METHOD_AND, METHOD_CAST, METHOD_GET_SESSION,
			METHOD_TO_HOST_AND_PORT = null;

	@BeforeSuite
	void beforeSuite() throws NoSuchMethodException {
		//
		final Class<?> clz = SshMain.class;
		//
		(METHOD_EXISTS = clz.getDeclaredMethod("exists", File.class)).setAccessible(true);
		//
		(METHOD_IS_FILE = clz.getDeclaredMethod("isFile", File.class)).setAccessible(true);
		//
		(METHOD_CAN_READ = clz.getDeclaredMethod("canRead", File.class)).setAccessible(true);
		//
		(METHOD_AND = clz.getDeclaredMethod("and", Object.class, Predicate.class, Predicate.class, Predicate.class))
				.setAccessible(true);
		//
		(METHOD_CAST = clz.getDeclaredMethod("cast", Class.class, Object.class)).setAccessible(true);
		//
		(METHOD_GET_SESSION = clz.getDeclaredMethod("getSession", JSch.class, HostAndPort.class, String.class))
				.setAccessible(true);
		//
		(METHOD_TO_HOST_AND_PORT = clz.getDeclaredMethod("toHostAndPort", String.class, Integer.class))
				.setAccessible(true);
		//
	}

	private static class IH implements InvocationHandler {

		private Boolean test, add;

		@Override
		public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
			//
			final String name = getName(method);
			//
			if (Objects.equals(getReturnType(method), Void.TYPE)) {
				//
				return null;
				//
			} // if
				//
			if (proxy instanceof FailableFunction && Objects.equals(name, "apply")) {
				//
				return null;
				//
			} else if (Boolean.logicalOr(proxy instanceof Predicate, proxy instanceof FailablePredicate)
					&& Objects.equals(name, "test")) {
				//
				return test;
				//
			} else if (proxy instanceof ImmutableConfiguration
					&& contains(Arrays.asList("getKeys", "getString"), name)) {
				//
				return null;
				//
			} else if (proxy instanceof Collection && Objects.equals(name, "add")) {
				//
				return add;
				//
			} else if (proxy instanceof Map && Objects.equals(name, "put")) {
				//
				return null;
				//
			} // if
				//
			throw new Throwable(name);
			//
		}

	}

	private static boolean contains(final Collection<?> instance, final Object item) {
		return instance != null && instance.contains(item);
	}

	private static Class<?> getReturnType(final Method instance) {
		return instance != null ? instance.getReturnType() : null;
	}

	private File file = null;

	@BeforeMethod
	void beforeMethod() {
		//
		file = new File("pom.xml");
		//
	}

	@Test
	void testNull() throws Throwable {
		//
		final Method[] ms = SshMain.class.getDeclaredMethods();
		//
		Method m = null;
		//
		Class<?>[] parameterTypes = null;
		//
		Object result = null;
		//
		String toString = null;
		//
		Collection<Object> collection = null;
		//
		for (int i = 0; ms != null && i < ms.length; i++) {
			//
			if ((m = ArrayUtils.get(ms, i)) == null || m.isSynthetic()
					|| (parameterTypes = m.getParameterTypes()) == null) {
				//
				continue;
				//
			} // if
				//
			clear(collection = ObjectUtils.getIfNull(collection, ArrayList::new));
			//
			for (int j = 0; j < parameterTypes.length; j++) {
				//
				if (Objects.equals(ArrayUtils.get(parameterTypes, j), Boolean.TYPE)) {
					//
					add(collection, Boolean.TRUE);
					//
				} else {
					//
					add(collection, null);
					//
				} // if
					//
			} // for

			result = Narcissus.invokeStaticMethod(m, toArray(collection));
			//
			toString = Objects.toString(m);
			//
			if (contains(Arrays.asList(Boolean.TYPE, Integer.TYPE), getReturnType(m))) {
				//
				Assert.assertNotNull(result, toString);
				//
			} else {
				//
				Assert.assertNull(result, toString);
				//
			} // if
				//
		} // for
			//
	}

	private static Object[] toArray(final Collection<?> instance) {
		return instance != null ? instance.toArray() : null;
	}

	@Test
	void testNotNull() throws Throwable {
		//
		final Method[] ms = SshMain.class.getDeclaredMethods();
		//
		Method m = null;
		//
		Class<?>[] parameterTypes = null;
		//
		Class<?> parameterType = null;
		//
		Object result = null;
		//
		String toString, name = null;
		//
		Collection<Object> collection = null;
		//
		IH ih = null;
		//
		Field[] fs = null;
		//
		Field f = null;
		//
		for (int i = 0; ms != null && i < ms.length; i++) {
			//
			if ((m = ArrayUtils.get(ms, i)) == null || m.isSynthetic()
					|| (parameterTypes = m.getParameterTypes()) == null
					|| Boolean.logicalAnd(Objects.equals(name = getName(m), "toMap"),
							Arrays.equals(parameterTypes, new Class<?>[] { String[].class }))) {
				//
				continue;
				//
			} // if
				//
			clear(collection = ObjectUtils.getIfNull(collection, ArrayList::new));
			//
			for (int j = 0; j < parameterTypes.length; j++) {
				//
				if ((parameterType = ArrayUtils.get(parameterTypes, j)) != null && parameterType.isArray()) {
					//
					add(collection, Array.newInstance(parameterType.getComponentType(), 0));
					//
				} else if (Objects.equals(parameterType, Boolean.TYPE)) {
					//
					add(collection, Boolean.TRUE);
					//
				} else if (Objects.equals(parameterType, Class.class)) {
					//
					add(collection, Class.class);
					//
				} else if (Objects.equals(parameterType, Channel.class)) {
					//
					add(collection, Narcissus.allocateInstance(ChannelExec.class));
					//
				} else if (parameterType != null && parameterType.isInterface()) {
					//
					if ((ih = ObjectUtils.getIfNull(ih, IH::new)) != null
							&& (fs = IH.class.getDeclaredFields()) != null) {
						//
						for (int k = 0; k < fs.length; k++) {
							//
							if ((f = ArrayUtils.get(fs, k)) == null) {
								//
								continue;
								//
							} // if
								//
							if (Objects.equals(f.getType(), Boolean.class)) {
								//
								Narcissus.setField(ih, f, Boolean.TRUE);
								//
							} // if
								//
						} // for
							//
					} // if
						//
					add(collection, Reflection.newProxy(parameterType, ih = ObjectUtils.getIfNull(ih, IH::new)));
					//
				} else {
					//
					add(collection, Narcissus.allocateInstance(parameterType));
					//
				} // if
					//
			} // for
				//
			result = Narcissus.invokeStaticMethod(m, toArray(collection));
			//
			toString = Objects.toString(m);
			//
			if (Objects.equals(getReturnType(m), Void.TYPE)
					|| Boolean.logicalAnd(Objects.equals(name, "getBytes"),
							Arrays.equals(parameterTypes, new Class<?>[] { String.class }))
					|| Boolean.logicalAnd(Objects.equals(name, "apply"),
							Arrays.equals(parameterTypes, new Class<?>[] { FailableFunction.class, Object.class }))
					|| Boolean.logicalAnd(Objects.equals(name, "testAndApply"),
							Arrays.equals(parameterTypes,
									new Class<?>[] { Predicate.class, Object.class, FailableFunction.class,
											FailableFunction.class }))
					|| Boolean.logicalAnd(Objects.equals(name, "getKeys"),
							Arrays.equals(parameterTypes, new Class<?>[] { ImmutableConfiguration.class }))
					|| Boolean.logicalAnd(Objects.equals(name, "getString"),
							Arrays.equals(parameterTypes,
									new Class<?>[] { ImmutableConfiguration.class, String.class }))
					|| Boolean.logicalAnd(Objects.equals(name, "cast"),
							Arrays.equals(parameterTypes, new Class<?>[] { Class.class, Object.class }))
					|| Boolean.logicalAnd(Objects.equals(name, "openChannel"),
							Arrays.equals(parameterTypes, new Class<?>[] { Session.class, String.class }))
					|| Boolean.logicalAnd(Objects.equals(name, "getInputStream"),
							Arrays.equals(parameterTypes, new Class<?>[] { Channel.class }))
					|| Boolean.logicalAnd(Objects.equals(name, "getSession"),
							Arrays.equals(parameterTypes,
									new Class<?>[] { JSch.class, HostAndPort.class, String.class }))
					|| Boolean.logicalAnd(Objects.equals(name, "toHostAndPort"),
							Arrays.equals(parameterTypes, new Class<?>[] { String.class, Integer.class }))) {
				//
				Assert.assertNull(result, toString);
				//
			} else {
				//
				Assert.assertNotNull(result, toString);
				//
			} // if
				//
		} // for
			//
	}

	private static String getName(final Member instance) {
		return instance != null ? instance.getName() : null;
	}

	private static <E> void add(final Collection<E> instance, final E item) {
		if (instance != null) {
			instance.add(item);
		}
	}

	private static void clear(final Collection<?> instance) {
		if (instance != null) {
			instance.clear();
		}
	}

	@Test
	public void testExists() throws IllegalAccessException, InvocationTargetException {
		//
		Assert.assertEquals(invoke(METHOD_EXISTS, null, file), Boolean.TRUE);
		//
	}

	@Test
	public void testIsFile() throws IllegalAccessException, InvocationTargetException {
		//
		Assert.assertEquals(invoke(METHOD_IS_FILE, null, file), Boolean.TRUE);
		//
		Assert.assertEquals(invoke(METHOD_IS_FILE, null, new File(".")), Boolean.FALSE);
		//
	}

	@Test
	public void testCanRead() throws IllegalAccessException, InvocationTargetException {
		//
		Assert.assertEquals(invoke(METHOD_CAN_READ, null, file), Boolean.TRUE);
		//
	}

	private static Object invoke(final Method method, final Object instance, final Object... args)
			throws IllegalAccessException, InvocationTargetException {
		return method != null && method.getDeclaringClass() != null ? method.invoke(instance, args) : null;
	}

	@Test
	public void testAnd() throws IllegalAccessException, InvocationTargetException {
		//
		final Predicate<?> alwaysTrue = Predicates.alwaysTrue();
		//
		Assert.assertEquals(invoke(METHOD_AND, null, null, alwaysTrue, null, null), Boolean.FALSE);
		//
		Assert.assertEquals(invoke(METHOD_AND, null, null, alwaysTrue, alwaysTrue, null), Boolean.FALSE);
		//
	}

	@Test
	public void testCast() throws Throwable {
		//
		final Object object = new Object();
		//
		Assert.assertSame(cast(Object.class, object), object);
		//
	}

	private static <T> T cast(final Class<T> clz, final Object instance) throws Throwable {
		try {
			return (T) invoke(METHOD_CAST, null, clz, instance);
		} catch (final InvocationTargetException e) {
			throw e.getTargetException();
		}
	}

	@Test
	public void testMain() throws Throwable {
		//
		SshMain.main(new String[] { "=", " =", "= ", "==", "a=b", "== ",
				cast(String.class, Narcissus.allocateInstance(String.class)) });
		//
	}

	@Test
	public void testGetSession() throws IllegalAccessException, InvocationTargetException {
		//
		final JSch jSch = new JSch();
		//
		Assert.assertNull(invoke(METHOD_GET_SESSION, null, jSch, Narcissus.allocateInstance(HostAndPort.class), null));
		//
		Assert.assertNotNull(invoke(METHOD_GET_SESSION, null, jSch, HostAndPort.fromHost(""), null));
		//
	}

	@Test
	public void testToHostAndPort() throws IllegalAccessException, InvocationTargetException {
		//
		Assert.assertNotNull(invoke(METHOD_TO_HOST_AND_PORT, null, "", Integer.valueOf(1)));
		//
	}

}