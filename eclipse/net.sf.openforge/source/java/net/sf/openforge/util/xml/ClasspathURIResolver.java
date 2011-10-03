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

class ClasspathURIResolver implements URIResolver {

    public Source resolve(String href, String base) throws TransformerException
    {
        InputStream s = loader.getResourceAsStream(href);
     
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

    URL getLocation (String href)
    {
        return loader.getResource(href);
    }

    public ClasspathURIResolver(ClassLoader loader, URIResolver resolver) {
        this.loader = loader;
        this.resolver = resolver;
    }

    private ClassLoader loader;
    private URIResolver resolver;
}