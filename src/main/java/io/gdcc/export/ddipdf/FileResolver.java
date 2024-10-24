package io.gdcc.export.ddipdf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;

public class FileResolver implements URIResolver {
    
    private static final Logger logger = LoggerFactory.getLogger(FileResolver.class);

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        logger.info("In File Resolver: {} {}", href, base);
        if (href.startsWith("file:")) {
            int index = href.lastIndexOf("/");
            String url =href.substring(index + 1); // some calculation from its parameters
            InputStream is = this.getClass().getResourceAsStream(url);
            return new StreamSource(is);

        } else {
            return null;
        }
    }

}
