package com.thefirstlineofcode.chalk.android.binary;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.thefirstlineofcode.basalt.oxm.android.IXmlParserFactory;
import com.thefirstlineofcode.basalt.oxm.android.XmlParserFactory;
import com.thefirstlineofcode.basalt.oxm.binary.AbstractBinaryXmppProtocolConverter;
import com.thefirstlineofcode.basalt.oxm.binary.Element;
import com.thefirstlineofcode.basalt.oxm.binary.Element.NameAndValue;
import com.thefirstlineofcode.basalt.oxm.parsing.BadMessageException;

/**
 * @author xb.zou
 * @date 2020/6/27
 * @option
 */
public class BinaryXmppProtocolConverter extends AbstractBinaryXmppProtocolConverter<XmlPullParser> {
    private IXmlParserFactory xmlParserFactory;

    public BinaryXmppProtocolConverter() {
        try {
            xmlParserFactory = createXmlParserFactory();
        } catch (XmlPullParserException e) {
            throw new RuntimeException("Can't create XmlPullParser.", e);
        }
    }

    protected IXmlParserFactory createXmlParserFactory() throws XmlPullParserException {
        return new XmlParserFactory();
    }

    @Override
    protected XmlPullParser createXmlParser(String message) throws XmlPullParserException {
        return xmlParserFactory.createParserWrapper(message);
    }

    @Override
    protected Element readDocument(XmlPullParser parser) {
        try {
            int eventType = parser.next();
            if (eventType != XmlPullParser.START_TAG) {
                throw new BadMessageException("No element start tag.");
            }

            Element element = readProtocol(parser, null);

            int next = parser.next();
            if (next != XmlPullParser.END_DOCUMENT) {
                throw new BadMessageException("End document expected.");
            }

            return element;
        } catch (XmlPullParserException | IOException e) {
            throw new BadMessageException("Can't parse XML document.", e);
        }
    }

    private Element readProtocol(XmlPullParser parser, Element parent) throws XmlPullParserException, IOException {
        return readElement(parser, parent);
    }

    @Override
    protected Element readElement(XmlPullParser parser, Element parent) throws XmlPullParserException, IOException {
        Element element = new Element();
        element.namespace = parser.getNamespace();
        element.localName = parser.getName();
        if (parser.getPrefix() != null && !parser.getPrefix().equals("")) {
            element.localName = parser.getPrefix() + ":" + element.localName;
        }

        element.attributes = readAttributes(parser);

        StringBuilder text = null;
        while (true) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.TEXT) {
                text = ParsingUtils.appendText(parser, text);
            } else if (eventType == XmlPullParser.END_TAG) {
                element.text = text == null ? null : text.toString();

                if (parent != null) {
                    parent.children.add(element);

                    if (!isEmpty(parent.namespace) && parent.namespace.equals(element.namespace))
                        element.namespace = null;
                }

                break;
            } else if (eventType == XmlPullParser.START_TAG) {
                String parentNamespace = null;
                if (parent != null) {
                    parentNamespace = element.namespace;
                }
                String namespace = parser.getNamespace();

                if (!isEmpty(namespace) && !namespace.equals(parentNamespace)) {
                    readProtocol(parser, element);
                } else {
                    readElement(parser, element);
                }
            } else {
                throw ParsingUtils.newParsingException(parser, "Unsupported XML event type.");
            }
        }

        return element;
    }

    private List<NameAndValue> readAttributes(XmlPullParser parser) {
        if (parser.getAttributeCount() == 0) {
            return new ArrayList<>();
        }

        List<NameAndValue> attributes = new ArrayList<>();
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String prefix = parser.getAttributePrefix(i);
            if ("".equals(prefix)) {
                prefix = null;
            }

            String attributeName = parser.getAttributeName(i);
            if ("xmlns".equals(attributeName) || attributeName.startsWith("xmlns:")) {
                continue;
            }

            attributes.add(new NameAndValue(prefix == null ? attributeName : prefix + ":" + attributeName,
                    parser.getAttributeValue(i)));
        }

        return attributes;
    }
}
