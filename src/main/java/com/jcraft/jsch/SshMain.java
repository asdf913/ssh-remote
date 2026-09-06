package com.jcraft.jsch;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.ImmutableConfiguration;
import org.apache.commons.configuration2.SubnodeConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.function.FailableRunnable;
import org.apache.commons.lang3.math.NumberUtils;

import io.github.toolfactory.narcissus.Narcissus;

public class SshMain {

	public static void main(final String[] args) throws Exception {
		//
		Session session = null;
		//
		ChannelExec channel = null;
		//
		final INIConfiguration iniConfiguration = new INIConfiguration();
		//
		final File file = new File("config.ini");
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
		try {
			//
			final String user = getString(iniConfiguration, "user");
			//
			final String host = getString(iniConfiguration, "host");
			//
			final Integer port = testAndApply(NumberUtils::isDigits, getString(iniConfiguration, "port"),
					Integer::valueOf, null);
			//
			setPassword(session = getSession(new JSch(), host, port, user),
					getBytes(getString(iniConfiguration, "password")));
			//
			final Properties config = new Properties();
			//
			config.put("StrictHostKeyChecking", "no");
			//
			setConfig(session, config);
			//
			connect(session);
			//
			final SubnodeConfiguration subnodeConfiguration = iniConfiguration.getSection("command");
			//
			final List<String> keys = testAndApply(Objects::nonNull, getKeys(subnodeConfiguration),
					IteratorUtils::toList, null);
			//
			sort(keys, (a, b) -> {
				//
				if (Boolean.logicalAnd(NumberUtils.isDigits(a), NumberUtils.isDigits(b))) {
					//
					return Integer.compare(NumberUtils.toInt(a), NumberUtils.toInt(b));
					//
				} // if
					//
				return ObjectUtils.compare(a, b);
				//
			});
			//
			String string = null;
			//
			for (int i = 0; i < IterableUtils.size(keys); i++) {
				//
				System.out.println(string = getString(subnodeConfiguration, IterableUtils.get(keys, i)));
				//
				try (final InputStream is = getInputStream(
						channel = cast(ChannelExec.class, openChannel(session, "exec")))) {
					//
					setCommand(channel, string);
					//
					connect(channel);
					//
					System.out.println(IOUtils.toString(is, StandardCharsets.UTF_8));
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
			if (command != null
					&& Narcissus.getField(command, Narcissus.findField(getClass(command), "value")) == null) {
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

	private static <E> void sort(final List<E> instance, final Comparator<? super E> c) {
		if (instance != null) {
			instance.sort(c);
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

	private static Session getSession(final JSch instance, final String host, final Integer port, final String user)
			throws JSchException {
		//
		if (instance == null) {
			//
			return null;
			//
		} else if (port != null) {
			//
			return instance.getSession(user, host, port.intValue());
			//
		} // if
			//
		return host != null ? instance.getSession(user, host) : null;
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
			if (Narcissus.getField(instance, Narcissus.findField(getClass(instance), "value")) == null) {
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

	private static <T, R> R testAndApply(final Predicate<T> predicate, final T value, final Function<T, R> functionTrue,
			final Function<T, R> functionFalse) {
		return test(predicate, value) ? apply(functionTrue, value) : apply(functionFalse, value);
	}

	private static <T, R> R apply(final Function<T, R> instance, final T value) {
		return instance != null ? instance.apply(value) : null;
	}

}