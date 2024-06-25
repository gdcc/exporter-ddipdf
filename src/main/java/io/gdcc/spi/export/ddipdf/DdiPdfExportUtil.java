package io.gdcc.spi.export.ddipdf;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;


public class DdiPdfExportUtil {

    private static final Logger logger = LoggerFactory.getLogger(DdiPdfExportUtil.class);
    
    public static void datasetPdfDDI(InputStream datafile, OutputStream outputStream) throws XMLStreamException {
        try {
            InputStream  styleSheetInput = DdiPdfExportUtil.class.getResourceAsStream("ddi-to-fo.xsl");

            final FopFactory fopFactory = FopFactory.newInstance(new File(".").toURI());
            FOUserAgent foUserAgent = fopFactory.newFOUserAgent();

            try {
                Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, outputStream);
                // Setup XSLT
                TransformerFactory factory = TransformerFactory.newInstance();
                Source mySrc = new StreamSource(styleSheetInput);
                factory.setURIResolver(new FileResolver());
                Transformer transformer = factory.newTransformer(mySrc);
                
                // Set the value of a <param> in the stylesheet
                String localeEnvVar = System.getenv().get("LANG");
                if (localeEnvVar != null) {
                    if (localeEnvVar.indexOf('.') > 0) {
                        localeEnvVar = localeEnvVar.substring(0, localeEnvVar.indexOf('.'));
                    }
                } else {
                    localeEnvVar = "en";
                }
                transformer.setParameter("language-code", localeEnvVar);

                // Setup input for XSLT transformation
                Source src = new StreamSource(datafile);

                // Resulting SAX events (the generated FO) must be piped through to FOP
                Result res = new SAXResult(fop.getDefaultHandler());

                // Start XSLT transformation and FOP processing
                transformer.transform(src, res);

            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }  catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

}
