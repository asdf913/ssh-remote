package com.jcraft.jsch;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.ImmutableConfiguration;
import org.apache.commons.configuration2.SubnodeConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.function.FailableFunction;
import org.apache.commons.lang3.function.FailablePredicate;
import org.apache.commons.lang3.function.FailableRunnable;
import org.apache.commons.lang3.math.NumberUtils;

import com.google.common.net.HostAndPort;
import com.google.common.reflect.Reflection;

import io.github.toolfactory.narcissus.Narcissus;

public class SshMain {

	private static String VALUE = "value";

	private static class Config {

		private String user = null;

		private HostAndPort hostAndPort = null;

		private byte[] password = null;

		private Iterable<String> commands = null;

	}

	private static class IH implements InvocationHandler {

		@Override
		public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
			//
			final String name = method != null ? method.getName() : null;
			//
			if (proxy instanceof Comparator && Objects.equals(name, "compare") && args != null && args.length > 1) {
				//
				final String sa = Objects.toString(ArrayUtils.get(args, 0));
				//
				final String sb = Objects.toString(ArrayUtils.get(args, 1));
				//
				if (Boolean.logicalAnd(NumberUtils.isDigits(sa), NumberUtils.isDigits(sb))) {
					//
					return Integer.valueOf(Integer.compare(NumberUtils.toInt(sa), NumberUtils.toInt(sb)));
					//
				} // if
					//
				return Integer.valueOf(ObjectUtils.compare(sa, sb));
				//
			} // if
				//
			throw new Throwable(name);
			//
		}

	}

	public static void main(final String[] args) throws Exception {
		//
		final Map<String, String> map = toMap(args);
		//
		String user = null;
		//
		HostAndPort hostAndPort = null;
		//
		byte[] password = null;
		//
		Iterable<String> commands = null;
		//
		if (map != null && !map.isEmpty()) {
			//
			if (map.containsKey("file")) {
				//
				final Config config = toConfig(new File(map.get("file")));
				//
				if (config != null) {
					//
					user = config.user;
					//
					hostAndPort = config.hostAndPort;
					//
					password = config.password;
					//
					commands = config.commands;
					//
				} // if
					//
			} else {
				//
				user = map.get("user");
				//
				hostAndPort = toHostAndPort(map.get("host"),
						testAndApply(NumberUtils::isDigits, map.get("port"), Integer::valueOf, null));
				//
				password = getBytes(map.get("password"));
				//
				commands = Collections.singleton(map.get("command"));
				//
			} // if
				//
		} else {
			//
			final Config config = toConfig(new File("config.ini"));
			//
			if (config != null) {
				//
				user = config.user;
				//
				hostAndPort = config.hostAndPort;
				//
				password = config.password;
				//
				commands = config.commands;
				//
			} // if
				//
		} // if
			//
		if (commands instanceof List) {
			//
			Narcissus.invokeMethod(commands, List.class.getDeclaredMethod("sort", Comparator.class),
					Reflection.newProxy(Comparator.class, new IH()));
			//
		} // if
			//
		Session session = null;
		//
		try {
			//
			setPassword(session = getSession(new JSch(), hostAndPort, user), password);
			//
			final Properties config = new Properties();
			//
			put(config, "StrictHostKeyChecking", "no");
			//
			setConfig(session, config);
			//
			connect(session);
			//
			String string = null;
			//
			ChannelExec channel = null;
			//
			for (int i = 0; i < IterableUtils.size(commands); i++) {
				//
				System.out.println(string = IterableUtils.get(commands, i));
				//
				try (final InputStream is = getInputStream(
						channel = cast(ChannelExec.class, openChannel(session, "exec")))) {
					//
					setCommand(channel, string);
					//
					connect(channel);
					//
					System.out.println(cast(String.class, testAndApply(Objects::nonNull, is,
							x -> IOUtils.toString(x, StandardCharsets.UTF_8), null)));
					//
					disconnect(channel);
					//
				} // if
					//
			} // for
				//
		} finally {
			//
			testAndAccept(SshMain::isConnected, session, SshMain::disconnect);
			//
		} // try
			//
	}

	private static Config toConfig(final File file) throws Exception {
		//
		final INIConfiguration iniConfiguration = new INIConfiguration();
		//
		testAndRun(and(file, SshMain::exists, SshMain::isFile, SshMain::canRead), () -> {
			//
			try (final Reader reader = new FileReader(file)) {
				//
				iniConfiguration.read(reader);
				//
			} // try
				//
		});
		//
		final Config config = new Config();
		//
		config.user = getString(iniConfiguration, "user");
		//
		config.hostAndPort = toHostAndPort(getString(iniConfiguration, "host"),
				testAndApply(NumberUtils::isDigits, getString(iniConfiguration, "port"), Integer::valueOf, null));
		//
		config.password = getBytes(getString(iniConfiguration, "password"));
		//
		final SubnodeConfiguration subnodeConfiguration = iniConfiguration.getSection("command");
		//
		final List<String> keys = testAndApply(Objects::nonNull, getKeys(subnodeConfiguration), IteratorUtils::toList,
				null);
		//
		Collection<String> collection = null;
		//
		for (int i = 0; i < IterableUtils.size(keys); i++) {
			//
			add(collection = ObjectUtils.getIfNull(collection, ArrayList::new),
					getString(subnodeConfiguration, IterableUtils.get(keys, i)));
			//
		} // for
			//
		config.commands = collection;
		//
		return config;
		//
	}

	private static HostAndPort toHostAndPort(final String host, final Integer port) {
		//
		if (port != null) {
			//
			try {
				//
				if (and(host, Objects::nonNull,
						x -> Narcissus.getField(x, Narcissus.findField(getClass(x), VALUE)) == null)) {
					//
					return null;
					//
				} // if
					//
			} catch (final NoSuchFieldException e) {
				//
				throw new RuntimeException(e);
				//
			} // try
				//
			return HostAndPort.fromParts(host, port.intValue());
			//
		} // if
			//
		return testAndApply(Objects::nonNull, host, HostAndPort::fromHost, null);
		//
	}

	private static <E> void add(final Collection<E> instance, final E item) {
		if (instance != null) {
			instance.add(item);
		}
	}

	private static Map<String, String> toMap(final String... ss) {
		//
		String s = null;
		//
		Map<String, String> map = null;
		//
		for (int i = 0; i < length(ss); i++) {
			//
			try {
				//
				if (and(s = ArrayUtils.get(ss, i), Objects::nonNull,
						x -> Narcissus.getField(x, Narcissus.findField(getClass(x), VALUE)) == null)) {
					//
					continue;
					//
				} // if
					//
			} catch (final NoSuchFieldException e) {
				//
				throw new RuntimeException(e);
				//
			} // try
				//
			if (Objects.equals(s, "=")) {
				//
				put(map = ObjectUtils.getIfNull(map, LinkedHashMap::new), "", "");
				//
			} else if (s != null && s.length() == 2 && s.charAt(0) == '=') {
				//
				put(map = ObjectUtils.getIfNull(map, LinkedHashMap::new), "", s.substring(1, s.length()));
				//
			} else if (s != null && s.length() == 2 && s.charAt(s.length() - 1) == '=') {
				//
				put(map = ObjectUtils.getIfNull(map, LinkedHashMap::new), s.substring(0, s.length() - 1), "");
				//
			} else if (s != null && s.indexOf('=') >= 0 && s.indexOf('=') == s.lastIndexOf('=')) {
				//
				put(map = ObjectUtils.getIfNull(map, LinkedHashMap::new), StringUtils.substringBefore(s, '='),
						StringUtils.substringAfter(s, '='));
				//
			} else if (s != null && s.length() > 2 && s.indexOf('=') != s.lastIndexOf('=')) {
				//
				put(map = ObjectUtils.getIfNull(map, LinkedHashMap::new), StringUtils.substring(s, 0, s.indexOf('=')),
						StringUtils.substring(s, s.indexOf('=') + 1));
				//
			} // if
				//
		} // for
			//
		return map;
		// s
	}

	private static <T, E extends Exception> boolean and(final T value, final FailablePredicate<T, E> a,
			final FailablePredicate<T, E> b) throws E {
		return test(a, value) && test(b, value);
	}

	private static <T, E extends Exception> boolean test(final FailablePredicate<T, E> instance, final T value)
			throws E {
		return instance != null && instance.test(value);
	}

	private static int length(final Object[] instance) {
		return instance != null ? instance.length : 0;
	}

	private static <K, V> void put(final Map<K, V> instance, final K key, final V value) {
		if (instance != null) {
			instance.put(key, value);
		}
	}

	private static void disconnect(final Channel instance) {
		if (instance != null) {
			instance.disconnect();
		}
	}

	private static void connect(final Channel instance) throws JSchException {
		//
		if (instance == null) {
			//
			return;
			//
		} // if
			//
		try {
			//
			if (Narcissus.getField(instance, Narcissus.findField(getClass(instance), "session")) == null) {
				//
				return;
				//
			} // if
				//
		} catch (final NoSuchFieldException e) {
			//
			throw new RuntimeException(e);
			//
		} // try
			//
		instance.connect();
		//
	}

	private static void setCommand(final ChannelExec instance, final String command) {
		//
		if (instance == null) {
			//
			return;
			//
		} // if
			//
		try {
			//
			if (command != null && Narcissus.getField(command, Narcissus.findField(getClass(command), VALUE)) == null) {
				//
				return;
				//
			} // if
				//
		} catch (final NoSuchFieldException e) {
			//
			throw new RuntimeException(e);
			//
		} // try
			//
		instance.setCommand(command);
		//
	}

	private static InputStream getInputStream(final Channel instance) throws IOException {
		//
		if (instance == null) {
			//
			return null;
			//
		} // if
			//
		try {
			//
			if (Narcissus.getField(instance, Narcissus.findField(getClass(instance), "io")) == null) {
				//
				return null;
				//
			} // if
				//
		} catch (final NoSuchFieldException e) {
			//
			throw new RuntimeException(e);
			//
		} // try
			//
		return instance.getInputStream();
		//
	}

	private static <T> void testAndAccept(final Predicate<T> predicate, final T value, final Consumer<T> consumer) {
		if (test(predicate, value)) {
			accept(consumer, value);
		}
	}

	private static <T> void accept(final Consumer<T> instance, final T value) {
		if (instance != null) {
			instance.accept(value);
		}
	}

	private static <E extends Throwable> void testAndRun(final boolean condition, final FailableRunnable<E> runnable)
			throws E {
		if (condition && runnable != null) {
			runnable.run();
		}
	}

	private static Channel openChannel(final Session instance, final String type) throws JSchException {
		return instance != null && instance.isConnected() ? instance.openChannel(type) : null;
	}

	private static boolean isConnected(final Session instance) {
		return instance != null && instance.isConnected();
	}

	private static void disconnect(final Session instance) {
		if (instance != null) {
			instance.disconnect();
		}
	}

	private static void connect(final Session instance) throws JSchException {
		//
		if (instance == null) {
			//
			return;
			//
		} // if
			//
		try {
			//
			if (Narcissus.getField(instance, Narcissus.findField(getClass(instance), "jsch")) == null) {
				//
				return;
				//
			} // if
				//
		} catch (final NoSuchFieldException e) {
			//
			throw new RuntimeException(e);
			//
		} // try
			//
		instance.connect();
		//
	}

	private static void setConfig(final Session instance, final Properties newconf) {
		//
		if (instance == null) {
			//
			return;
			//
		} // if
			//
		try {
			//
			if (newconf != null && Narcissus.getField(newconf, Narcissus.findField(getClass(newconf), "map")) == null) {
				//
				return;
				//
			} // if
				//
		} catch (final NoSuchFieldException e) {
			//
			throw new RuntimeException(e);
			//
		} // try
			//
		instance.setConfig(newconf);
		//
	}

	private static void setPassword(final Session instnace, final byte[] password) {
		if (instnace != null) {
			instnace.setPassword(password);
		}
	}

	private static <T> T cast(final Class<T> clz, final Object instance) {
		return clz != null && clz.isInstance(instance) ? clz.cast(instance) : null;
	}

	private static String getString(final ImmutableConfiguration instance, final String key) {
		return instance != null ? instance.getString(key) : null;
	}

	private static Iterator<String> getKeys(final ImmutableConfiguration instance) {
		return instance != null ? instance.getKeys() : null;
	}

	private static boolean canRead(final File instance) {
		return instance != null && instance.getPath() != null && instance.canRead();
	}

	private static boolean isFile(final File instance) {
		return instance != null && instance.getPath() != null && instance.isFile();
	}

	private static boolean exists(final File instance) {
		return instance != null && instance.getPath() != null && instance.exists();
	}

	private static <T> boolean and(final T value, final Predicate<T> a, final Predicate<T> b, final Predicate<T> c) {
		return test(a, value) && test(b, value) && test(c, value);
	}

	private static <T> boolean test(final Predicate<T> instance, final T value) {
		return instance != null && instance.test(value);
	}

	private static Session getSession(final JSch instance, final HostAndPort hostAndPort, final String user)
			throws JSchException {
		//
		if (instance == null) {
			//
			return null;
			//
		} else if (hostAndPort != null) {
			//
			final String host = hostAndPort.getHost();
			//
			if (hostAndPort.hasPort()) {
				//
				return testAndApply(Objects::nonNull, host, x -> instance.getSession(user, x, hostAndPort.getPort()),
						null);
				//
			} // if
				//
			return instance.getSession(user, host);
			//
		} // if
			//
		return null;
		//
	}

	private static byte[] getBytes(final String instance) {
		//
		if (instance == null) {
			//
			return null;
			//
		} // if
			//
		try {
			//
			if (Narcissus.getField(instance, Narcissus.findField(getClass(instance), VALUE)) == null) {
				//
				return null;
				//
			} // if
				//
		} catch (final NoSuchFieldException e) {
			//
			throw new RuntimeException(e);
			//
		} // try
			//
		return instance.getBytes();
		//
	}

	private static Class<?> getClass(final Object instance) {
		return instance != null ? instance.getClass() : null;
	}

	private static <T, R, E extends Exception> R testAndApply(final Predicate<T> predicate, final T value,
			final FailableFunction<T, R, E> functionTrue, final FailableFunction<T, R, E> functionFalse) throws E {
		return test(predicate, value) ? apply(functionTrue, value) : apply(functionFalse, value);
	}

	private static <T, R, E extends Exception> R apply(final FailableFunction<T, R, E> instance, final T value)
			throws E {
		return instance != null ? instance.apply(value) : null;
	}

}