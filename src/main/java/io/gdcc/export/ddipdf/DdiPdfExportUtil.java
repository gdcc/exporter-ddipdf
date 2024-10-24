package io.gdcc.export.ddipdf;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;
import org.apache.tika.langdetect.optimaize.OptimaizeLangDetector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;

import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;

import java.net.URL;


public class DdiPdfExportUtil {

    private static final Logger logger = LoggerFactory.getLogger(DdiPdfExportUtil.class);
    public static class TitleAndDescription  {
        public String Title;
        public String Description;
        public String Language;
    }
    
    private DdiPdfExportUtil() {
        // As this is a util class, adding a private constructor disallows instances of this class.
    }

    private static String detectLanguage(TitleAndDescription td) {
        String lang = "en"; //default language
        LanguageDetector detector = new OptimaizeLangDetector().loadModels();
        LanguageResult result1 = detector.detect(td.Title );
        String lang1 = result1.getLanguage();
        if (result1.isReasonablyCertain()) {
            lang = lang1;
        } else {
            LanguageResult result2 = detector.detect(td.Description);
            if (result2.isReasonablyCertain()) {
                lang = result2.getLanguage();
            }
        }

        URL found = DdiPdfExportUtil.class.getResource("messages_" + lang + ".properties.xml");

        if (found != null) {
                return lang;
        } else {
                return null;
        }
    }

    private static TitleAndDescription getTitleAndDescription(InputStream datafile)  {

        TitleAndDescription titleAndDescription = new TitleAndDescription();
        String lang = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbf.newDocumentBuilder();
            Document doc = builder.parse(datafile);
            try {
                lang = doc.getDocumentElement().getAttribute("xml:lang");
            } catch (DOMException e) {
                lang = null;
                logger.warn("No language attribute");
            }
            if (lang != null && !lang.equals("") ) {
                titleAndDescription.Language = lang;
            } else {
                XPathFactory xPathfactory = XPathFactory.newInstance();
                XPath xpath = xPathfactory.newXPath();
                try {
                    XPathExpression expr = xpath.compile("/codeBook/stdyDscr/citation/titlStmt/titl/text()");
                    titleAndDescription.Title = (String) expr.evaluate(doc, XPathConstants.STRING);
                    expr = xpath.compile("/codeBook/stdyDscr/stdyInfo/abstract/text()");
                    titleAndDescription.Description = (String) expr.evaluate(doc, XPathConstants.STRING);
                } catch (XPathExpressionException e) {
                    logger.error("Error finding title and description");
                    logger.error(e.getMessage());
                }
            }

            return titleAndDescription;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            logger.warn(e.getMessage());
            return null;
        }

    }
    
    public static void datasetPdfDDI(InputStream datafile, OutputStream outputStream) throws XMLStreamException {
        try {
            String localeEnvVar = "en"; //default language
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            datafile.transferTo(baos);

            byte[] buffer = baos.toByteArray();
            InputStream clone1 = new ByteArrayInputStream(buffer);
            InputStream clone2 = new ByteArrayInputStream(buffer);

            TitleAndDescription td = getTitleAndDescription(clone1);
            if (td != null) {
                if (td.Language != null) {
                    localeEnvVar = td.Language;
                } else {
                    String lang = detectLanguage(td);
                    if (lang != null && !lang.equals("")) {
                        localeEnvVar = lang;
                    }
                }
            }

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

                transformer.setParameter("language-code", localeEnvVar);

                // Setup input for XSLT transformation
                Source src = new StreamSource(clone2);

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
