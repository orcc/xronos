package net.sf.openforge.util.xml;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactory;

public class XmlImplementation {

	public DocumentBuilder getDocumentBuilder() {
		try {
			return documentBuilderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException exc) {
			throw new RuntimeException(exc);
		}
	}

	public TransformerFactory getTransformerFactory() {
		return transformerFactory;
	}

	public XmlImplementation(DocumentBuilderFactory dbf, TransformerFactory tf) {
		this.documentBuilderFactory = dbf;
		this.transformerFactory = tf;
	}

	private DocumentBuilderFactory documentBuilderFactory;
	private TransformerFactory transformerFactory;

}
