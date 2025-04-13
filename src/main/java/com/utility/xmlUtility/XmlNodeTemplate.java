package com.utility.xmlUtility;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.HashMap;
import java.util.Map;


public class XmlNodeTemplate {

    private static final Map<String, Element> fieldTemplates = new HashMap<>();

    static {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

            fieldTemplates.put("Text", createFieldTemplate(doc, "Text", "<db-field-length>255</db-field-length><validation><validation-type>None</validation-type></validation>"));
            fieldTemplates.put("TextArea", createFieldTemplate(doc, "TextArea", "<no-of-column>60</no-of-column><no-of-row>5</no-of-row>"));
            fieldTemplates.put("Date", createFieldTemplate(doc, "Date", "<group-by>true</group-by>"));
            fieldTemplates.put("Combo", createFieldTemplate(doc, "Combo", "<group-by>true</group-by>"));
            fieldTemplates.put("Radio", createFieldTemplate(doc, "Radio", "<group-by>true</group-by><field-option_view>0</field-option_view>"));
            fieldTemplates.put("Numeric", createFieldTemplate(doc, "Numeric", "<allow-null-values>true</allow-null-values><validation><validation-type>Integer</validation-type></validation>"));
            fieldTemplates.put("Checkbox", createFieldTemplate(doc, "Checkbox", "<field-option_view>0</field-option_view>"));
            fieldTemplates.put("MultiCombo", createFieldTemplate(doc, "Combo", "<is-multiselect>true</is-multiselect>"));  // special key for multiselect
            fieldTemplates.put("File", createFieldTemplate(doc, "File", ""));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Element createFieldTemplate(Document doc, String displayType, String extraContent) throws Exception {
        Element field = doc.createElement("field");
        field.setAttribute("summary", "true");

        // Base child nodes
        field.appendChild(createSimpleElement(doc, "field-name", "_templateField"));
        field.appendChild(createSimpleElement(doc, "display-name", "TEMPLATE FIELD"));
        field.appendChild(createSimpleElement(doc, "db-field", "_TEMPLATE_FIELD"));
        field.appendChild(createSimpleElement(doc, "data-type", "String"));
        field.appendChild(createSimpleElement(doc, "display-type", displayType));

        if (!extraContent.isEmpty()) {
            Element wrapper = XmlUtil.stringToElement("<wrapper>" + extraContent + "</wrapper>");
            NodeList extras = wrapper.getChildNodes();
            for (int i = 0; i < extras.getLength(); i++) {
                field.appendChild(doc.importNode(extras.item(i), true));
            }
        }

        field.appendChild(createSimpleElement(doc, "section", "1"));
        field.appendChild(createSimpleElement(doc, "is-active", "yes"));
        field.appendChild(createSimpleElement(doc, "is-mandatory", "false"));
        field.appendChild(createSimpleElement(doc, "build-field", "no"));
        field.appendChild(createSimpleElement(doc, "field-export", "true"));
        field.appendChild(createSimpleElement(doc, "order-by", "0"));
        field.appendChild(createSimpleElement(doc, "pii-enabled", "false"));

        /*Element mailmerge = doc.createElement("mailmerge");
        mailmerge.setAttribute("is-active", "true");
        mailmerge.setAttribute("keyword-name", "$template_keyword$");
        field.appendChild(mailmerge);*/

        return field;
    }

    private static Element createSimpleElement(Document doc, String name, String value) {
        Element el = doc.createElement(name);
        el.setTextContent(value);
        return el;
    }

    public static Element getTemplateByType(String displayType, boolean isMultiSelect) {
        String key = (displayType.equals("Combo") && isMultiSelect) ? "MultiCombo" : displayType;
        Element original = fieldTemplates.get(key);
        return (Element) (original != null ? original.cloneNode(true) : null);
    }
}
