package util;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import java.io.StringReader;
import java.io.StringWriter;

public class XMLUtil {


    static final public Logger LOG = LogManager.getLogger(XMLUtil.class);


    public static String extractXmlNodeValue(String filter, String xml) throws Exception {

        DocumentBuilderFactory dbf = null;
        Document doc = null;
        try {
            dbf = DocumentBuilderFactory.newInstance();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            LOG.error("", e);
        }

        try {
            doc = dbf.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            LOG.error("", e);
        }
        XPath xPath = null;
        try {
            xPath = XPathFactory.newInstance().newXPath();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            LOG.error("", e);
        }
        String result = null;
        try {
            result = (String) xPath.evaluate(filter, doc, XPathConstants.STRING);

        } catch (RuntimeException e) {
            // TODO Auto-generated catch block
            LOG.error("", e);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            LOG.error("", e);
        }
        //return  (nodeToString(result));
        return result;
    }

    public static NodeList extractXmlNodeList(Document doc, String filter) {
        XPath xPath = null;

        try {
            xPath = XPathFactory.newInstance().newXPath();
        } catch (RuntimeException e) {
            LOG.error("", e);
        } catch (Exception e) {
            LOG.error("", e);
        }

        NodeList result = null;
        try {
            result = (NodeList) xPath.compile(filter).evaluate(doc, XPathConstants.NODESET);
        } catch (RuntimeException e) {
            LOG.error("", e);
        } catch (Exception e) {
            LOG.error("", e);
        }

        return result;
    }

    public static String extractXmlNodeValue(Document doc, String filter) {


        XPath xPath = null;

        try {
            xPath = XPathFactory.newInstance().newXPath();
        } catch (RuntimeException e) {
            LOG.error("", e);
        } catch (Exception e) {
            LOG.error("", e);
        }

        String result = null;
        try {
            result = (String) xPath.compile(filter).evaluate(doc, XPathConstants.STRING);
        } catch (RuntimeException e) {
            LOG.error("", e);
        } catch (Exception e) {
            LOG.error("", e);
        }

        return result;

    }

    public static String extractXmlNode(Document doc, String filter) throws Exception {


        XPath xPath = null;

        try {
            xPath = XPathFactory.newInstance().newXPath();
        } catch (Exception e) {
            LOG.error("", e);
        }

        Node result = null;
        try {
            result = (Node) xPath.compile(filter).evaluate(doc, XPathConstants.NODE);
        } catch (RuntimeException e) {
            LOG.error("", e);
            LOG.error("", e);
        } catch (Exception e) {
            LOG.error("", e);
        }

        return (nodeToString(result));

    }

    public static String extractXmlStr(Document doc, String filter) throws Exception {


        XPath xPath = null;

        try {
            xPath = XPathFactory.newInstance().newXPath();
        } catch (Exception e) {
            LOG.error("", e);
        }

        String result = null;
        try {
            result = (String) xPath.compile(filter).evaluate(doc, XPathConstants.STRING);
        } catch (RuntimeException e) {
            LOG.error("", e);
            LOG.error("", e);
        } catch (Exception e) {
            LOG.error("", e);
        }

        return result;

    }

    private static String nodeToString(Node node) {
        StringWriter buf = new StringWriter();
        Transformer xform = null;
        try {
            xform = TransformerFactory.newInstance().newTransformer();
            xform.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            xform.transform(new DOMSource(node), new StreamResult(buf));
        } catch (TransformerConfigurationException e) {
            // TODO Auto-generated catch block
            LOG.error("", e);
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            LOG.error("", e);
        } catch (TransformerFactoryConfigurationError e) {
            // TODO Auto-generated catch block
            LOG.error("", e);
        } catch (TransformerException e) {
            // TODO Auto-generated catch block
            LOG.error("", e);
        }
        return (buf.toString());
    }

    public final static Document getXmlDoc(String xmlStr) {
        Document doc = null;

        try {
            if (xmlStr != null) {
                doc = getXMLDoc(xmlStr);
            }
        } catch (RuntimeException e) {
            LOG.error("", e);
        } catch (Exception e) {
            LOG.error("", e);
        }
        return doc;
    }


    public static String evaluateAndRemoveXPath(Document document, String xpathExpression) throws Exception {
        // Create XPathFactory object
        XPathFactory xpathFactory = XPathFactory.newInstance();

        // Create XPath object
        XPath xpath = xpathFactory.newXPath();

        NodeList nodes = null;
        try {
            // Create XPathExpression object
            XPathExpression expr = xpath.compile(xpathExpression);

            // Evaluate expression result on XML document
            nodes = (NodeList) expr.evaluate(document, XPathConstants.NODESET);

            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                node.getParentNode().removeChild(node);

            }

        } catch (XPathExpressionException e) {
            LOG.error("", e);
        }

        return xmlDocToStr(document);
    }


    public static String xmlDocToStr(Document doc) {
        StringWriter buf = new StringWriter();
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer t = tf.newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.transform(new DOMSource(doc), new StreamResult(buf));
        } catch (TransformerFactoryConfigurationError e) {
            // TODO Auto-generated catch block
            LOG.error("", e);
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            LOG.error("", e);
        }
        return (buf.toString());
    }


    public static NodeList getXmlNodeSet(Document doc, String filter) throws Exception {


        XPath xPath = null;

        try {
            xPath = XPathFactory.newInstance().newXPath();
        } catch (Exception e) {
            LOG.error("", e);
        }

        NodeList result = null;
        try {
            result = (NodeList) xPath.evaluate(filter, doc, XPathConstants.NODESET);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


        return result;

    }

    static public Document getXMLDoc(String xmlStr) throws Exception {
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = domFactory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xmlStr)));

        return doc;
    }

    public static BaseBean reversalFrom(Node node) throws XPathExpressionException {
        XPath xPath = XPathFactory.newInstance().newXPath();
        String revId = (String) xPath.compile("RevTrnIdRec/RevTrnId/text()").evaluate(node, XPathConstants.STRING);
        String revDate = (String) xPath.compile("RevTrnIdRec/RevTrnDate/text()").evaluate(node, XPathConstants.STRING);

        BaseBean result = new BaseBean();
        if ( revId != null) {
            result.put("reversalId", revId);
        }

        if (revDate != null) {
            result.put("reversalDate", revDate);
        }

        return result;
    }

    public static void reversalFrom(NodeList list, BaseBean requestBean) throws XPathExpressionException {

        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            storeNodeInBean(node, requestBean, i);
        }
    }

    private static void storeNodeInBean(Node node, BaseBean requestBean, int i) throws XPathExpressionException {
        XPath xPath = XPathFactory.newInstance().newXPath();
        String revId = (String) xPath.compile("RevTrnId/text()").evaluate(node, XPathConstants.STRING);
        String revDate = (String) xPath.compile("RevTrnDate/text()").evaluate(node, XPathConstants.STRING);

        requestBean.setString("revId_"+i, revId);
        requestBean.setString("revDate_"+i, revDate);

    }



}
