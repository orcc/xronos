package net.sf.orc2hdl.backend;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.eclipse.emf.common.util.EMap;
import org.stringtemplate.v4.AttributeRenderer;
import org.stringtemplate.v4.Interpreter;
import org.stringtemplate.v4.ModelAdaptor;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.misc.STNoSuchPropertyException;

import net.sf.orcc.OrccException;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.util.ExpressionPrinter;
import net.sf.orcc.ir.util.TypePrinter;
import net.sf.orcc.util.OrccUtil;

/**
 * This class defines a printer.
 * 
 * @author Endri Bezati
 * 
 */

public class Orc2HDLPrinter {

	protected static class EMapModelAdaptor implements ModelAdaptor {

		@Override
		public Object getProperty(Interpreter interp, ST st, Object o,
				Object property, String propertyName)
				throws STNoSuchPropertyException {
			return ((EMap<?, ?>) o).get(property);
		}
	}

	protected class ExpressionRenderer implements AttributeRenderer {

		@Override
		public String toString(Object o, String formatString, Locale locale) {
			return expressionPrinter.doSwitch((Expression) o);
		}

	}

	protected class TypeRenderer implements AttributeRenderer {

		@Override
		public String toString(Object o, String formatString, Locale locale) {
			return typePrinter.doSwitch((Type) o);
		}

	}

	protected Map<String, Object> customAttributes;

	private ExpressionPrinter expressionPrinter;

	protected STGroup group;

	protected Map<String, Object> options;

	private TypePrinter typePrinter;

	/**
	 * Creates a new printer.
	 * 
	 * @param templateName
	 *            the name of the template
	 */
	public Orc2HDLPrinter(String templateName) {
		group = OrccUtil.loadGroup(templateName, "net/sf/orc2hdl/templates/",
				Orc2HDLPrinter.class.getClassLoader());
		group.registerRenderer(Expression.class, new ExpressionRenderer());
		group.registerRenderer(Type.class, new TypeRenderer());

		group.registerModelAdaptor(EMap.class, new EMapModelAdaptor());

		options = new HashMap<String, Object>();
		customAttributes = new HashMap<String, Object>();
	}

	public Map<String, Object> getCustomAttributes() {
		return customAttributes;
	}

	public Map<String, Object> getOptions() {
		return options;
	}

	/**
	 * Prints the given network to a file whose name and path are given.
	 * 
	 * @param fileName
	 *            name of the output file
	 * @param path
	 *            path of the output file
	 * @param instanceName
	 *            name of the root ST rule
	 */
	public void print(String fileName, String path, String instanceName) {
		ST template = group.getInstanceOf(instanceName);
		printTemplate(template, path + File.separator + fileName);
	}

	protected void printTemplate(ST template, String file) {
		try {
			template.add("options", options);
			for (String attribute : customAttributes.keySet()) {
				template.add(attribute, customAttributes.get(attribute));
			}

			byte[] b = template.render(80).getBytes();
			OutputStream os = new FileOutputStream(file);
			os.write(b);
			os.close();
		} catch (IOException e) {
			new OrccException("I/O error", e);
		}
	}

	/**
	 * Registers a model adaptor for the given types.
	 * 
	 * @param attributeType
	 *            type of attribute
	 * @param adaptor
	 *            adaptor
	 */
	public void registerModelAdaptor(Class<?> attributeType,
			ModelAdaptor adaptor) {
		group.registerModelAdaptor(attributeType, adaptor);
	}

	/**
	 * Registers an attribute renderer for the given types.
	 * 
	 * @param attributeType
	 *            type of attribute
	 * @param renderer
	 *            renderer
	 */
	public void registerRenderer(Class<?> attributeType,
			AttributeRenderer renderer) {
		group.registerRenderer(attributeType, renderer);
	}

	public void setExpressionPrinter(ExpressionPrinter printer) {
		this.expressionPrinter = printer;
	}

	public void setTypePrinter(TypePrinter printer) {
		this.typePrinter = printer;
	}

}
