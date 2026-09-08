package com.jcraft.jsch;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.function.FailableConsumer;
import org.apache.commons.lang3.function.FailableFunction;
import org.apache.commons.lang3.function.FailablePredicate;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.net.HostAndPort;

import io.github.toolfactory.narcissus.Narcissus;

public class SshMain {

	private static final Logger LOG = LoggerFactory.getLogger(SshMain.class);

	private static String VALUE = "value";

	private static class Config {

		private String user = null;

		private HostAndPort hostAndPort = null;

		private byte[] password = null;

		private Iterable<String> commands = null;

		private File privateKey = null;

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
		File privateKey = null;
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
					privateKey = config.privateKey;
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
				privateKey = testAndApply(Objects::nonNull, map.get("privateKey"), File::new, null);
				//
			} // if
				//
		} else {
			//
			final Config config = toConfig(new File("config.xml"));
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
				privateKey = config.privateKey;
				//
			} // if
				//
		} // if
			//
		info(LOG, "user       ={}", user);
		//
		info(LOG, "hostAndPort={}", hostAndPort);
		//
		info(LOG, "privateKey ={}", privateKey);
		//
		Session session = null;
		//
		try {
			//
			final JSch jSch = new JSch();
			//
			testAndAccept(x -> and(x, SshMain::exists, SshMain::isFile, SshMain::canRead), privateKey,
					x -> jSch.addIdentity(getAbsolutePath(x)));
			//
			session = getSession(jSch, hostAndPort, user);
			//
			if (getAbsolutePath(privateKey) != null) {
				//
				setPassword(session, password);
				//
			} // if
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
				info(LOG, string = IterableUtils.get(commands, i));
				//
				try (final InputStream is = getInputStream(
						channel = cast(ChannelExec.class, openChannel(session, "exec")));
						final InputStream err = getErrStream(channel)) {
					//
					setCommand(channel, string);
					//
					connect(channel);
					//
					info(LOG, cast(String.class, testAndApply(Objects::nonNull, is,
							x -> IOUtils.toString(x, StandardCharsets.UTF_8), null)));
					//
					testAndAccept(StringUtils::isNotBlank,
							testAndApply(Objects::nonNull, err, x -> IOUtils.toString(x, StandardCharsets.UTF_8), null),
							x -> error(LOG, x));
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

	private static void info(final Logger instance, final String msg) {
		if (instance != null) {
			instance.info(msg);
		}
	}

	private static void info(final Logger instance, final String format, final Object arg) {
		if (instance != null) {
			instance.info(format, arg);
		}
	}

	private static void error(final Logger instance, final String msg) {
		if (instance != null) {
			instance.error(msg);
		}
	}

	private static Config toConfig(final File file)
			throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
		//
		final DocumentBuilder db = newDocumentBuilder(DocumentBuilderFactory.newInstance());
		//
		final Document document = file != null && file.getPath() != null && exists(file) ? parse(db, file) : null;
		//
		final XPath xp = newXPath(XPathFactory.newInstance());
		//
		final Config config = new Config();
		//
		config.user = evaluate(xp, "/*/user", document);
		//
		config.hostAndPort = toHostAndPort(evaluate(xp, "/*/host", document),
				testAndApply(NumberUtils::isDigits, evaluate(xp, "/*/port", document), Integer::valueOf, null));
		//
		config.password = getBytes(evaluate(xp, "/*/password", document));
		//
		final NodeList nodeList = cast(NodeList.class, evaluate(xp, "/*/*/command", document, XPathConstants.NODESET));
		//
		Collection<String> collection = null;
		//
		for (int i = 0; nodeList != null && i < nodeList.getLength(); i++) {
			//
			add(collection = ObjectUtils.getIfNull(collection, ArrayList::new), getTextContent(nodeList.item(i)));
			//
		} // for
			//
		config.commands = collection;
		//
		config.privateKey = testAndApply(Objects::nonNull, evaluate(xp, "/*/privateKey", document), File::new, null);
		//
		return config;
		//
	}

	private static XPath newXPath(final XPathFactory instance) {
		return instance != null ? instance.newXPath() : null;
	}

	private static Document parse(final DocumentBuilder instance, final File file) throws SAXException, IOException {
		return instance != null && file != null && file.getPath() != null && and(file, SshMain::exists, SshMain::isFile)
				? instance.parse(file)
				: null;
	}

	private static DocumentBuilder newDocumentBuilder(final DocumentBuilderFactory instance)
			throws ParserConfigurationException {
		return instance != null ? instance.newDocumentBuilder() : null;
	}

	private static String getTextContent(final Node instnace) {
		return instnace != null ? instnace.getTextContent() : null;
	}

	private static String evaluate(final XPath instance, final String expression, final Object item)
			throws XPathExpressionException {
		return instance != null && item != null ? instance.evaluate(expression, item) : null;
	}

	private static Object evaluate(final XPath instance, final String expression, final Object item, final QName qName)
			throws XPathExpressionException {
		return instance != null && item != null ? instance.evaluate(expression, item, qName) : null;
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

	private static InputStream getErrStream(final ChannelExec instance) throws IOException {
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
		return instance.getErrStream();
		//
	}

	private static <T, E extends Exception> void testAndAccept(final Predicate<T> predicate, final T value,
			final FailableConsumer<T, E> consumer) throws E {
		if (test(predicate, value)) {
			accept(consumer, value);
		}
	}

	private static <T, E extends Exception> void accept(final FailableConsumer<T, E> instance, final T value) throws E {
		if (instance != null) {
			instance.accept(value);
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
		if (instance == null || StringUtils.isEmpty(instance.getHost())) {
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
		final List<Field> fields = testAndApply(Objects::nonNull, getClass(newconf), FieldUtils::getAllFieldsList,
				null);
		//
		Field field = testAndApply(x -> IterableUtils.size(x) == 1,
				collect(filter(stream(fields), x -> Objects.equals(getName(x), "table")), Collectors.toList()),
				x -> IterableUtils.get(x, 0), null);
		//
		if (field == null) {
			//
			field = testAndApply(x -> IterableUtils.size(x) == 1,
					collect(filter(stream(fields), x -> Objects.equals(getName(x), "map")), Collectors.toList()),
					x -> IterableUtils.get(x, 0), null);
			//
		} // if
			//
		if (newconf != null && Narcissus.getField(newconf, field) == null) {
			//
			return;
			//
		} // if
			//
		instance.setConfig(newconf);
		//
	}

	private static String getName(final Member instance) {
		return instance != null ? instance.getName() : null;
	}

	private static <E> Stream<E> stream(final Collection<E> instance) {
		return instance != null ? instance.stream() : null;
	}

	private static <T> Stream<T> filter(final Stream<T> instance, final Predicate<T> predicate) {
		return instance != null && (predicate != null || Proxy.isProxyClass(instance.getClass()))
				? instance.filter(predicate)
				: null;
	}

	private static <T, A, R> R collect(final Stream<T> instance, final Collector<? super T, A, R> collector) {
		return instance != null && (collector != null || Proxy.isProxyClass(instance.getClass()))
				? instance.collect(collector)
				: null;
	}

	private static void setPassword(final Session instnace, final byte[] password) {
		if (instnace != null) {
			instnace.setPassword(password);
		}
	}

	private static <T> T cast(final Class<T> clz, final Object instance) {
		return clz != null && clz.isInstance(instance) ? clz.cast(instance) : null;
	}

	private static boolean canRead(final File instance) {
		return instance != null && instance.getPath() != null && instance.canRead();
	}

	private static String getAbsolutePath(final File instance) {
		return instance != null && instance.getPath() != null ? instance.getAbsolutePath() : null;
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