package io.github.precavidos.vagaviva.review;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import io.github.precavidos.vagaviva.review.ReviewContext.FailedTest;

/**
 * Reads the failed tests out of the zip a student's workflow uploads: the
 * Surefire reports of the Java template ({@code TEST-*.xml}) or the jest-junit
 * report of the TypeScript one ({@code junit.xml}). Both name a mission test's
 * class {@code Missão <id>: <título>}, which is how a test is tied to its
 * mission.
 * <p>
 * The zip comes from the student's repository, so it is untrusted: sizes are
 * capped and the XML parser resolves no DTD or external entity.
 */
final class JunitReports {

	/** Larger reports are not read: real ones stay far below. */
	static final long MAX_UNCOMPRESSED_BYTES = 20L * 1024 * 1024;

	static final int MAX_MESSAGE_LENGTH = 500;

	private static final Pattern MISSION = Pattern.compile("^Missão ([a-z0-9-]+):");

	/** Terminal colour codes some reporters leave in messages. */
	private static final Pattern ANSI = Pattern.compile("\u001B\\[[0-9;]*m");

	private JunitReports() {
	}

	/** The failed tests, in report order, of every mission test in the zip. */
	static List<FailedTest> failedTests(byte[] zip) {
		var failed = new ArrayList<FailedTest>();
		long read = 0;
		try (var entries = new ZipInputStream(new ByteArrayInputStream(zip))) {
			ZipEntry entry;
			while ((entry = entries.getNextEntry()) != null) {
				if (entry.isDirectory() || !entry.getName().toLowerCase(Locale.ROOT).endsWith(".xml")) {
					continue;
				}
				byte[] xml = readCapped(entries, MAX_UNCOMPRESSED_BYTES - read);
				read += xml.length;
				failed.addAll(failedTestsInXml(xml));
			}
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Unreadable test report");
		}
		return failed;
	}

	static List<FailedTest> failedTestsInXml(byte[] xml) {
		Document document;
		try {
			document = parser().parse(new ByteArrayInputStream(xml));
		}
		catch (SAXException | IOException e) {
			throw new IllegalArgumentException("Unreadable test report");
		}
		var failed = new ArrayList<FailedTest>();
		NodeList testCases = document.getElementsByTagName("testcase");
		for (int i = 0; i < testCases.getLength(); i++) {
			Element testCase = (Element) testCases.item(i);
			Element failure = firstChild(testCase, "failure");
			if (failure == null) {
				failure = firstChild(testCase, "error");
			}
			if (failure == null) {
				continue;
			}
			Matcher mission = MISSION.matcher(testCase.getAttribute("classname").strip());
			if (!mission.find()) {
				continue;
			}
			failed.add(new FailedTest(mission.group(1), testCase.getAttribute("name"), message(failure)));
		}
		return failed;
	}

	/**
	 * The Portuguese assertion text: the {@code [...]} description of an
	 * AssertJ failure (Java) or the line after {@code Custom message:} of
	 * jest-expect-message (TypeScript); otherwise the first line.
	 */
	static String message(Element failure) {
		String text = failure.getAttribute("message");
		if (text.isBlank()) {
			text = failure.getTextContent();
		}
		text = ANSI.matcher(text).replaceAll("").strip();
		String message;
		int custom = text.indexOf("Custom message:");
		if (custom >= 0) {
			message = text.substring(custom + "Custom message:".length()).strip().lines().findFirst().orElse("");
		}
		else if (text.startsWith("[") && text.indexOf(']') > 0) {
			message = text.substring(1, text.indexOf(']'));
		}
		else {
			message = text.lines().findFirst().orElse("");
		}
		message = message.strip();
		return message.length() <= MAX_MESSAGE_LENGTH ? message : message.substring(0, MAX_MESSAGE_LENGTH) + "…";
	}

	private static Element firstChild(Element parent, String name) {
		for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child instanceof Element element && name.equals(element.getTagName())) {
				return element;
			}
		}
		return null;
	}

	private static byte[] readCapped(InputStream in, long remaining) throws IOException {
		if (remaining <= 0) {
			throw new IOException("Report too large");
		}
		byte[] bytes = in.readNBytes((int) Math.min(remaining + 1, Integer.MAX_VALUE - 8));
		if (bytes.length > remaining) {
			throw new IOException("Report too large");
		}
		return bytes;
	}

	private static DocumentBuilder parser() {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			factory.setXIncludeAware(false);
			factory.setExpandEntityReferences(false);
			factory.setNamespaceAware(false);
			return factory.newDocumentBuilder();
		}
		catch (ParserConfigurationException e) {
			throw new IllegalStateException("No secure XML parser available", e);
		}
	}

}
