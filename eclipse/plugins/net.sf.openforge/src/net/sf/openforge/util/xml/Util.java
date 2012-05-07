/* 
BEGINCOPYRIGHT X
	
	Copyright (c) 2007, Xilinx Inc.
	All rights reserved.
	
	Redistribution and use in source and binary forms, 
	with or without modification, are permitted provided 
	that the following conditions are met:
	- Redistributions of source code must retain the above 
	  copyright notice, this list of conditions and the 
	  following disclaimer.
	- Redistributions in binary form must reproduce the 
	  above copyright notice, this list of conditions and 
	  the following disclaimer in the documentation and/or 
	  other materials provided with the distribution.
	- Neither the name of the copyright holder nor the names 
	  of its contributors may be used to endorse or promote 
	  products derived from this software without specific 
	  prior written permission.
	
	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND 
	CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, 
	INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
	MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
	DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
	CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
	SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
	NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
	LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
	HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
	CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
	OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
	SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
	
ENDCOPYRIGHT
 */

package net.sf.openforge.util.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.app.GenericJob;
import net.sf.openforge.app.OptionRegistry;
import net.sf.openforge.lim.CodeLabel;
import net.sf.openforge.util.io.ClassLoaderStreamLocator;
import net.sf.openforge.util.io.StreamLocator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ext.EntityResolver2;

public class Util {

	public static Element root(Object doc) {
		if (doc instanceof Document) {
			return ((Document) doc).getDocumentElement();
		} else if (doc instanceof Element) {
			return (Element) doc;
		} else {
			throw new RuntimeException("Cannot get root element.");
		}
	}

	public static String xpathEvalString(String expr, Object e) {
		return (String) xpathEval(expr, e, XPathConstants.STRING);
	}

	public static Node xpathEvalNode(String expr, Object e) {
		return (Node) xpathEval(expr, e, XPathConstants.NODE);
	}

	public static Element xpathEvalElement(String expr, Object e) {
		return (Element) xpathEval(expr, e, XPathConstants.NODE);
	}

	public static NodeList xpathEvalNodes(String expr, Object e) {
		return (NodeList) xpathEval(expr, e, XPathConstants.NODESET);
	}

	public static List<Element> xpathEvalElements(String expr, Object e) {
		NodeList nl = xpathEvalNodes(expr, e);
		List<Element> res = new ArrayList<Element>();
		for (int i = 0; i < nl.getLength(); i++) {
			res.add((Element) nl.item(i));
		}
		return res;
	}

	public static Object xpathEval(String expr, Object e, QName type) {
		try {
			return xpath.evaluate(expr, e, type);
		} catch (Exception exc) {
			throw new RuntimeException("Cannot evaluate xpath expression.", exc);
		}
	}

	private final static XPath xpath = XPathFactory.newInstance().newXPath();

	public static List<Node> listElements(Element parent, ElementPredicate p) {
		NodeList nl = parent.getChildNodes();
		List<Node> l = new ArrayList<Node>();
		for (int i = 0; i < nl.getLength(); i++) {
			Node n = nl.item(i);
			if (n instanceof Element && p.test((Element) n)) {
				l.add(n);
			}
		}
		return l;
	}

	public static Element optionalElement(Element parent, ElementPredicate p) {
		List<Node> nl = listElements(parent, p);

		assert nl.size() <= 1;

		return (nl.size() == 0) ? null : (Element) nl.get(0);
	}

	public static Element uniqueElement(Element parent, ElementPredicate p) {
		List<Node> nl = listElements(parent, p);

		assert nl.size() <= 1;

		return (nl.size() == 0) ? null : (Element) nl.get(0);
	}

	public static Transformer[] getTransformersAsResources(String[] resNames,
			StreamLocator resourceLocator) {
		return getTransformersAsResources(resNames, getDefaultImplementation(),
				resourceLocator);
	}

	public static Transformer[] getTransformersAsResources(String[] resNames,
			XmlImplementation xmlImpl, StreamLocator resourceLocator) {
		Transformer[] xforms = new Transformer[resNames.length];

		for (int i = 0; i < resNames.length; i++) {
			InputStream is = resourceLocator.getAsStream(resNames[i]);
			try {
				// IDM. The transformer should use the same resource Locator as
				// the
				// loaded resources on the assumption that the resources and the
				// things
				// they reference are co-located.
				xforms[i] = createTransformer(is, xmlImpl, resourceLocator);
			} catch (Throwable e) {
				throw new RuntimeException(
						"Cannot create transform from resource " + resNames[i],
						e);
			} finally {
				try {
					if (is != null)
						is.close();
				} catch (IOException ioe) {
				}
			}
		}
		return xforms;
	}

	public static Node applyTransforms(Node document, Transformer[] xfs)
			throws Exception {
		Node doc = document;
		// String tmp = Util.nodeToString(doc);
		// System.out.println(tmp);
		// Logging.user().info(tmp);

		String[] transfo = { "XLIMLoopFix", "XLIMFixSelector", "XLIMTagify0",
				"XLIMMakePortNames", "XLIMSizeAndType", "XLIMAddVarReads",
				"XLIMInsertCasts", "XLIMAddVarReadScope", "XLIMBuildControl",
				"XLIMRoutePorts", "XLIMFixNames0", "XLIMMakeDeps",
				"XLIMProcessPHI", "XLIMFixNames1", "XLIMCreateExits",
				"XLIMTagify1", "XLIMAddControlDeps" };
		int i = 0;

		for (Transformer xf : xfs) {
			// System.out.println("call1");
			// Logging.user().info("call1");
			doc = applyTransform(doc, xf);
			printToXML(doc, Integer.toString(i) + "_" + transfo[i]);
			i++;
		}

		// tmp = Util.nodeToString(doc);

		// System.out.println(tmp);
		return doc;

	}

	static void printToXML(Node doc, String transfo) throws Exception {
		GenericJob gj = EngineThread.getGenericJob();
		String baseName = gj.getOutputBaseName();
		String outDir = gj.getOption(OptionRegistry.DESTINATION_DIR)
				.getValue(CodeLabel.UNSCOPED).toString()
				+ File.separator + "slim";
		new File(outDir).mkdir();
		new File(outDir + File.separator + baseName).mkdir();
		TransformerFactory tFactory = TransformerFactory.newInstance();
		Transformer transformer = tFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		DOMSource source = new DOMSource(doc);

		StreamResult result = new StreamResult(new File(outDir + File.separator
				+ baseName + File.separator + baseName + "_" + transfo
				+ ".slim"));
		transformer.transform(source, result);
	}

	public static String nodeToString(Node node) {
		StringWriter sw = new StringWriter();
		try {
			Transformer t = TransformerFactory.newInstance().newTransformer();
			t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			t.transform(new DOMSource(node), new StreamResult(sw));
		} catch (TransformerException te) {
			System.out.println("nodeToString Transformer Exception");
		}
		return sw.toString();
	}

	public static Node applyTransforms(Node document, String baseInputURI,
			String baseOutputURI, String[] fileNames) throws Exception {
		return applyTransforms(document, baseInputURI, baseOutputURI,
				fileNames, getSaxonImplementation());
	}

	public static Node applyTransforms(Node document, String[] fileNames)
			throws Exception {
		return applyTransforms(document, fileNames, getSaxonImplementation());
	}

	public static Node applyTransforms(Node document, String[] fileNames,
			XmlImplementation xmlImpl) throws Exception {
		return applyTransforms(document, null, null, fileNames, xmlImpl);
	}

	public static Node applyTransforms(Node document, String baseInputURI,
			String baseOutputURI, String[] fileNames, XmlImplementation xmlImpl)
			throws Exception {
		Node doc = document;
		for (int i = 0; i < fileNames.length; i++) {
			// File file = new File(fileNames[i]);
			Transformer xf = createTransformer(fileNames[i], xmlImpl);
			DOMResult res = new DOMResult();
			if (baseInputURI != null) {
				res.setSystemId(baseInputURI);
			}
			DOMSource src = (baseOutputURI == null) ? new DOMSource(doc)
					: new DOMSource(doc, baseOutputURI);
			doc = applyTransform(xf, src, res);
		}
		return doc;
	}

	public static Node applyTransform(Node document, Transformer xf)
			throws Exception {
		return applyTransform(xf, new DOMSource(document), new DOMResult());
	}

	private static Node applyTransform(Transformer xf, DOMSource source,
			DOMResult res) {
		ErrorListener oldListener = xf.getErrorListener();

		// Catch and report errors during transformation here.
		if (true) { // Redirect output of errors/warnings to the debug stream
			xf.setErrorListener(new ErrorListener() {
				@Override
				public void error(TransformerException e) {
				}

				@Override
				public void fatalError(TransformerException e) {
				}

				@Override
				public void warning(TransformerException e) {
				}
			});
		}

		try {
			xf.transform(source, res);
		} catch (TransformerException te) {
			xf.setErrorListener(oldListener);

			throw new RuntimeException(te);
		}
		xf.setErrorListener(oldListener);

		return res.getNode();
	}

	public static Node applyTransformsAsResources(Node document,
			String[] resNames, XmlImplementation xmlImpl,
			StreamLocator resourceLocator) throws Exception {
		Node doc = document;
		Transformer[] xforms = getTransformersAsResources(resNames, xmlImpl,
				resourceLocator);

		for (int i = 0; i < resNames.length; i++) {
			try {
				doc = applyTransform(doc, xforms[i]);
			} catch (Throwable e) {
				throw new RuntimeException(
						"Cannot apply transform as resource " + resNames[i], e);
			}
		}
		return doc;
	}

	/*
	 * public static Node applyTransformsAsResources(Node document, String []
	 * resNames) throws Exception { return applyTransformsAsResources(document,
	 * resNames, getDefaultLocator()); }
	 */
	public static Node applyTransformsAsResources(Node document,
			String[] resNames, StreamLocator resourceLocator) throws Exception {
		return applyTransformsAsResources(document, resNames,
				getSaxonImplementation(), resourceLocator);
	}

	public static Node applyTransformAsResource(Node document, String resName,
			String[] parNames, Object[] parValues, XmlImplementation xmlImpl,
			StreamLocator resourceLocator) throws Exception {
		assert parNames.length == parValues.length;

		Transformer[] xforms = getTransformersAsResources(
				new String[] { resName }, xmlImpl, resourceLocator);
		assert xforms.length == 1;

		for (int i = 0; i < parNames.length; i++) {
			xforms[0].setParameter(parNames[i], parValues[i]);
		}
		Node doc = null;
		try {
			doc = applyTransform(document, xforms[0]);
		} catch (Throwable e) {
			throw new RuntimeException("Cannot apply transform as resource "
					+ resName, e);
		}
		return doc;
	}

	/*
	 * public static Node applyTransformAsResource(Node document, String
	 * resName) throws Exception { return applyTransformAsResource(document,
	 * resName, getDefaultLocator()); }
	 */
	public static Node applyTransformAsResource(Node document, String resName,
			StreamLocator resourceLocator) throws Exception {
		return applyTransformAsResource(document, resName,
				getSaxonImplementation(), resourceLocator);
	}

	public static Node applyTransformAsResource(Node document, String resName,
			XmlImplementation xmlImpl, StreamLocator resourceLocator)
			throws Exception {
		return applyTransformAsResource(document, resName, new String[] {},
				new Object[] {}, xmlImpl, resourceLocator);
	}

	/*
	 * public static Node applyTransformAsResource(Node document, String
	 * resName, String [] parNames, Object [] parValues) throws Exception {
	 * return applyTransformAsResource(document, resName, parNames, parValues,
	 * getDefaultLocator()); }
	 */

	public static Node applyTransformAsResource(Node document, String resName,
			String[] parNames, Object[] parValues, StreamLocator resourceLocator)
			throws Exception {
		return applyTransformAsResource(document, resName, parNames, parValues,
				getSaxonImplementation(), resourceLocator);
	}

	/*
	 * public static Node applyTransformAsResource(Node document, String
	 * resName, XmlImplementation xmlImpl) throws Exception { return
	 * applyTransformAsResource(document, resName, new String[] {}, new Object
	 * [] {}, xmlImpl); }
	 * 
	 * public static Node applyTransformAsResource(Node document, String
	 * resName, String [] parNames, Object [] parValues, XmlImplementation
	 * xmlImpl) throws Exception { return applyTransformAsResource(document,
	 * resName, parNames, parValues, xmlImpl, getDefaultLocator()); }
	 */

	private static TransformerFactory createTransformerFactory(
			XmlImplementation xmlImpl, StreamLocator resourceLocator) {
		TransformerFactory xff = xmlImpl.getTransformerFactory();
		xff.setURIResolver(new StreamLocatorURIResolver(resourceLocator, xff
				.getURIResolver()));
		return xff;
	}

	public static Transformer createTransformer(String fileName)
			throws Exception {
		return createTransformer(fileName, getSaxonImplementation(),
				getDefaultLocator());
	}

	public static Transformer createTransformer(InputStream is)
			throws Exception {
		return createTransformer(is, getSaxonImplementation(),
				getDefaultLocator());
	}

	public static Transformer createTransformer(String fileName,
			XmlImplementation xmlImpl) throws Exception {
		return createTransformer(fileName, xmlImpl, getDefaultLocator());
	}

	public static Transformer createTransformer(InputStream is,
			XmlImplementation xmlImpl) throws Exception {
		return createTransformer(is, xmlImpl, getDefaultLocator());
	}

	public static Transformer createTransformer(String fileName,
			XmlImplementation xmlImpl, StreamLocator resourceLocator)
			throws Exception {
		File file = new File(fileName);
		TransformerFactory xff = createTransformerFactory(xmlImpl,
				resourceLocator);
		Transformer xf = xff.newTransformer(new StreamSource(file));
		return xf;
	}

	public static Transformer createTransformer(InputStream is,
			XmlImplementation xmlImpl, StreamLocator resourceLocator)
			throws Exception {
		TransformerFactory xff = createTransformerFactory(xmlImpl,
				resourceLocator);
		Transformer xf = xff.newTransformer(new StreamSource(is));
		is.close();
		return xf;
	}

	// For jdk1.5
	public static void validateWXS(Node document, String fileName)
			throws Exception {
		// create a SchemaFactory capable of understanding WXS schemas
		SchemaFactory factory = SchemaFactory
				.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

		// load a WXS schema, represented by a Schema instance
		Source schemaFile = new StreamSource(new File(fileName));
		Schema schema = factory.newSchema(schemaFile);

		// create a Validator instance, which can be used to
		// validate an instance document
		Validator validator = schema.newValidator();

		// validate the DOM tree
		validator.validate(new DOMSource(document));
	}

	public static void validateRNG(Node document, String fileName)
			throws Exception {
		// create a SchemaFactory capable of understanding WXS schemas
		SchemaFactory factory = SchemaFactory
				.newInstance(XMLConstants.RELAXNG_NS_URI);

		// load a WXS schema, represented by a Schema instance
		Source schemaFile = new StreamSource(new File(fileName));
		Schema schema = factory.newSchema(schemaFile);

		// create a Validator instance, which can be used to
		// validate an instance document
		Validator validator = schema.newValidator();

		// validate the DOM tree
		validator.validate(new DOMSource(document));
	}

	public static Node saxonify(Node n) {
		try {
			String xml = createXML(n);
			// InputStream sis = new StringBufferInputStream(xml);
			InputStream sis = new ByteArrayInputStream(xml.getBytes("UTF-8"));
			return getSaxonImplementation().getDocumentBuilder().parse(sis);
		} catch (Exception exc) {
			throw new RuntimeException("Could not saxonify node.", exc);
		}
	}

	public static Node xercify(Node n) {
		try {
			String xml = createXML(n);
			// InputStream sis = new StringBufferInputStream(xml);
			InputStream sis = new ByteArrayInputStream(xml.getBytes("UTF-8"));
			return getDefaultImplementation().getDocumentBuilder().parse(sis);
		} catch (Exception exc) {
			throw new RuntimeException("Could not xercify node.", exc);
		}
	}

	public static String createXML(Node doc) {
		TransformerFactory xff = TransformerFactory.newInstance();
		Transformer serializer = null;
		try {
			serializer = xff.newTransformer();
		} catch (TransformerConfigurationException te) {
			throw new RuntimeException("Could not create transformer. "
					+ te.getMessage());
		}

		serializer.setOutputProperty(OutputKeys.INDENT, "yes");
		serializer
				.setOutputProperty("{http://saxon.sf.net/}indent-spaces", "4");
		serializer.setOutputProperty(OutputKeys.METHOD, "xml");
		OutputStream os = new ByteArrayOutputStream();
		try {
			serializer.transform(new DOMSource(doc), new StreamResult(os));
			os.close();
		} catch (Exception e) {
			throw new RuntimeException("Could not create transformer.", e);
		}

		return os.toString();
	}

	public static String createTXT(Transformer xf, Node doc) throws Exception {
		OutputStream os = new ByteArrayOutputStream();
		StreamResult res = new StreamResult(os);
		xf.transform(new DOMSource(doc), res);
		os.close();
		return os.toString();
	}

	public static DOMImplementationRegistry getDOMImplementationRegistry() {
		try {
			return DOMImplementationRegistry.newInstance();
		} catch (Exception e) {
			return null;
		}
	}

	public static XmlImplementation getSaxonImplementation() {
		return saxonImpl;
	}

	public static XmlImplementation getDefaultImplementation() {
		return defaultImpl;
	}

	private static StreamLocator getDefaultLocator() {
		return new ClassLoaderStreamLocator(Util.class.getClassLoader());
	}

	private static XmlImplementation saxonImpl = new XmlImplementation(
			new net.sf.saxon.dom.DocumentBuilderFactoryImpl(),
			new net.sf.saxon.TransformerFactoryImpl());

	private static XmlImplementation defaultImpl = new XmlImplementation(
			javax.xml.parsers.DocumentBuilderFactory.newInstance(),
			javax.xml.transform.TransformerFactory.newInstance());

	@SuppressWarnings("unused")
	private static String defaultDBFI = javax.xml.parsers.DocumentBuilderFactory
			.newInstance().getClass().getName();

	// public static void setDefaultDBFI() {
	// setDBFI(defaultDBFI);
	// }
	//
	// public static void setDBFI(String name) {
	// System.setProperty("javax.xml.parsers.DocumentBuilderFactory", name);
	// }
	//
	// public static void setSAXON() {
	// System.setProperty("javax.xml.transform.TransformerFactory",
	// "net.sf.saxon.TransformerFactoryImpl");
	// }
	//
	public static class RemappingEntityResolver implements EntityResolver2 {
		public RemappingEntityResolver(String oldID, String newSystemID) {
			this(oldID, newSystemID, null);
		}

		public RemappingEntityResolver(String oldID, String newSystemID,
				EntityResolver resolver) {
			_oldID = oldID;
			_newSystemID = newSystemID;
			_resolver = resolver;
		}

		@Override
		public InputSource getExternalSubset(String name, String baseURI)
				throws SAXException, IOException {
			return null;
		}

		@Override
		public InputSource resolveEntity(String name, String publicId,
				String baseURI, String systemId) throws SAXException,
				IOException {
			return resolveEntity(publicId, systemId);
		}

		@Override
		public InputSource resolveEntity(String publicId, String systemId)
				throws SAXException, IOException {
			if (_resolver != null) {
				InputSource source = _resolver
						.resolveEntity(publicId, systemId);
				if (source != null) {
					return source;
				}
			}
			if (publicId.equals(_oldID) || systemId.equals(_oldID)) {
				return new InputSource(_newSystemID);
			} else {
				// use the default behaviour
				return null;
			}
		}

		private final String _oldID;
		private final String _newSystemID;
		private final EntityResolver _resolver;
	}

	/**
	 * Utility method for displaying an Element
	 */
	public static void printElement(Element e) {
		printElement(e, "");
	}

	/**
	 * Utility method for displaying an Element, with indent control
	 */
	public static void printElement(Element e, String prefix) {
		System.out.print(prefix + e.getTagName() + " ");
		NamedNodeMap attrs = e.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++) {
			Node att = attrs.item(i);
			System.out
					.print(att.getNodeName() + "=" + att.getNodeValue() + " ");
		}
		System.out.println();

		NodeList children = e.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Element)
				printElement((Element) child, prefix + "  ");
			else
				System.out.println(prefix + child.getNodeName() + " "
						+ child.getNodeValue());
		}
	}

}
