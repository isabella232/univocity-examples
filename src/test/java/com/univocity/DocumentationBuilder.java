/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity;

import static org.testng.Assert.*;

import java.awt.*;
import java.io.*;
import java.util.*;

import javax.swing.*;

import org.testng.annotations.*;

public class DocumentationBuilder {

	private static final String INCLUDE_METHOD = "@@INCLUDE_METHOD";
	private static final String INCLUDE_CONTENT = "@@INCLUDE_CONTENT";
	private static final String INCLUDE_CLASS = "@@INCLUDE_CLASS(";

	private static final String LINK = "@@LINK(";

	private static final String CLASS_END = "##CLASS_END";
	private static final String CODE_START = "##CODE_START";
	private static final String CODE_END = "##CODE_END";

	private static final String REPO_URL = "http://github.com/uniVocity/univocity-api/blob/master";

	@Test
	public void testDocumentationIsUpdated() {
		String currentDoc = readContent("README.md");
		String expectedDoc = generateDocumentation();

		assertEquals(currentDoc, expectedDoc);
	}

	private static String readMethod(String path, String content) {

		String methodName = path.substring(path.lastIndexOf(".") + 1, path.length());

		content = readClass(path, content);

		//System.out.println("---> " + methodName);

		int methodStart = content.indexOf(methodName);
		int codeStart = content.indexOf(CODE_START, methodStart) + CODE_START.length();
		int codeEnd = content.indexOf(CODE_END, codeStart) - 3; //-3 to take out the comment characters

		return content.substring(codeStart, codeEnd);

	}

	private static String readClass(String path, String content) {
		String className = path;

		className = path.substring(path.lastIndexOf("/") + 1, path.length());

		if (className.contains(".")) {
			className = className.substring(0, className.indexOf("."));
		}

		//System.out.println("--> " + className);

		int classStart = content.indexOf(className);

		int classEnd = content.indexOf(CLASS_END);
		if (classEnd == -1) {
			classEnd = content.length();
		}

		return "class " + content.substring(classStart, classEnd);
	}

	private static String readContent(String originalPath) {
		String originalExtension = "";
		String path = originalPath;
		if (path.contains(".")) {
			originalExtension = path.substring(path.lastIndexOf("."), path.length());
			path = path.substring(0, path.lastIndexOf("."));
		}

		if (path.contains(",")) {
			path = path.substring(path.indexOf(",") + 1, path.length());
		}

		path = path.trim();

		InputStream input = DocumentationBuilder.class.getResourceAsStream(path);

		if (input == null) {
			if (path.startsWith("/")) {
				path = path.substring(1, path.length());
			}
			File file = new File(path);

			if (!file.exists()) {
				file = new File(path + ".java");
			}
			if (!file.exists()) {
				file = new File(path + originalExtension);
			}

			try {
				input = new FileInputStream(file);
			} catch (FileNotFoundException e) {
				throw new IllegalStateException("Could not load template from path: " + file.getAbsolutePath() + ". Original path " + originalPath, e);
			}
		}

		Scanner scanner = null;
		try {
			scanner = new Scanner(input, "UTF-8").useDelimiter("\\A");
			return scanner.hasNext() ? scanner.next() : "";
		} finally {
			if (scanner != null) {
				scanner.close();
			}
		}
	}

	private static void appendln(StringBuilder out, Object content) {
		out.append(content).append('\n');
	}

	private static String resolveLinks(String content) {
		StringBuilder out = new StringBuilder(content.length());

		int startIndex = 0;
		int endIndex = 0;
		while ((startIndex = content.indexOf(LINK, startIndex)) != -1) {
			out.append(content, endIndex, startIndex);

			startIndex += LINK.length();

			endIndex = content.indexOf(')', startIndex);
			if (endIndex == -1) {
				throw new IllegalArgumentException("Illegal " + LINK + " at position " + startIndex + ": missing right parenthesis");
			}
			String name = content.substring(startIndex, endIndex);
			String link = resolveLinkTo(name);

			out.append('[').append(name).append(']');
			out.append('(').append(link).append(')');

			endIndex++;
			startIndex = endIndex;
		}

		out.append(content, endIndex, content.length());

		return out.toString();
	}

	private static String resolveLinkTo(String name) {
		name = name + ".java";
		File projectRoot = new File(".");

		File linkedFile = find(projectRoot, name);
		if (linkedFile != null) {
			return "." + getRelativePath(projectRoot, linkedFile);
		}

		//look for class in univocity-api (hacky, but works)
		projectRoot = new File("../univocity-api");

		if (!projectRoot.isDirectory()) {
			throw new IllegalStateException("univocity-api project must be available alongside univocity-examples project in order to resolve link to class " + name);
		}

		linkedFile = find(projectRoot, name);
		if (linkedFile != null) {
			return REPO_URL + getRelativePath(projectRoot, linkedFile);
		}

		throw new IllegalStateException("Unable to resolve find link to class " + name);
	}

	private static String getRelativePath(File dir, File fileInDir) {
		String absolutePath = fileInDir.getAbsolutePath();
		String relativePath = absolutePath.substring(dir.getAbsolutePath().length());
		return relativePath;
	}

	private static File find(File dir, String name) {
		File out = null;
		for (File file : dir.listFiles()) {
			if (file.getName().startsWith(".")) {
				continue;
			}

			if (file.isDirectory()) {
				out = find(file, name);
				if (out != null) {
					return out;
				}
			} else if (file.getName().equals(name)) {
				return file;
			}
		}
		return out;
	}

	private static void include(StringBuilder out, String line) {
		String path = line.substring(line.indexOf("(") + 1, line.lastIndexOf(")"));
		String fileName = path.substring(path.lastIndexOf("/") + 1, path.length());

		String content = readContent(path);

		//System.out.println("-> Read: " + path);

		if (line.startsWith(INCLUDE_CLASS)) {
			appendJavaBlock(out);
			appendContents(out, readClass(path, content));

		} else if (line.startsWith(INCLUDE_METHOD)) {
			appendJavaBlock(out);
			appendContents(out, readMethod(path, content));

		} else if (line.startsWith(INCLUDE_CONTENT)) {
			appendBlockStart(out, fileName);

			String rowsToRead = line.substring(line.indexOf("(") + 1, line.indexOf(",", line.indexOf("(")));
			int rows = Integer.parseInt(rowsToRead.trim());
			if (rows == 0) {
				rows = Integer.MAX_VALUE;
			}
			String[] lines = content.split("\n");
			for (String l : lines) {
				if (l.startsWith("#")) {
					continue;
				}
				out.append("\t");
				appendln(out, l.trim());
				rows--;
				if (rows < 0) {
					appendln(out, "\t...");
					break;
				}
			}

		}

		appendBlockEnd(out);
	}

	private static void appendContents(StringBuilder out, String contents) {
		for (String line : contents.split("\n")) {
			int tabCount = 0;

			for (int i = 0; i < line.length(); i++) {
				if (line.charAt(i) == '\t') {
					tabCount++;
				} else {
					break;
				}
			}

			if (tabCount == 0) {
				line = '\t' + line;
			} else if (tabCount >= 2) {
				//removes one tab
				line = line.substring(1);
			}

			appendln(out, line);
		}
	}

	private static void appendJavaBlock(StringBuilder out) {
		out.append("\n```java\n\n");
	}

	private static void appendBlockEnd(StringBuilder out) {
		out.append("\n\n```\n");
	}

	private static void appendBlockStart(StringBuilder out, String fileName) {
		out.append("\n``` ");
		if (fileName.contains(".")) {
			out.append(fileName);
		}
		out.append("\n\n");
	}

	private static final String generateDocumentation() {
		String template = readContent("/src/test/resources/README_template.md");
		template = resolveLinks(template);
		StringBuilder out = new StringBuilder();

		String[] lines = template.split("\n");

		for (String line : lines) {
			if (!line.startsWith("@@")) {
				appendln(out, line);
			} else {
				include(out, line);
			}
		}

		return out.toString();
	}

	public static void main(String... args) {
		String doc = generateDocumentation();

		JTextArea txt = new JTextArea(doc);
		JDialog d = new JDialog((Frame) null, true);
		d.add(new JScrollPane(txt));

		d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		d.setSize(800, 600);
		d.setLocation(100, 100);
		d.setVisible(true);

		System.exit(0);
	}
}
