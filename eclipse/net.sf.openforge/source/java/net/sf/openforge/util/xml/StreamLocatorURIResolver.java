/**
 * 
 */
package net.sf.openforge.util.xml;

import java.io.InputStream;
import java.net.URL;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

import net.sf.openforge.util.io.StreamLocator;

class StreamLocatorURIResolver implements URIResolver {

    public Source resolve(String href, String base) throws TransformerException
    {
        InputStream s = locator.getAsStream(href);
     
        if (s == null) {
            if (resolver != null)
                return resolver.resolve(href, base);
            else {
                return null;
            }
        } else {
            return new StreamSource(s);
        }
    }

    public StreamLocatorURIResolver(StreamLocator locator, URIResolver resolver) {
        this.locator = locator;
        this.resolver = resolver;
    }

    private StreamLocator locator;
    private URIResolver resolver;
}