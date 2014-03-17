package webtools;

import org.htmlcleaner.*;
import org.jdom.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class HtmlXPath
{
    private HtmlCleaner cleaner;
    private Object node;

    private HtmlXPath(HtmlCleaner cleaner, Object node)
    {
        this.cleaner = cleaner;
        this.node = node;
    }

    public static HtmlXPath build(String html) throws Exception
    {
        HtmlCleaner cleaner = new HtmlCleaner();
        CleanerProperties props = cleaner.getProperties();

        props = cleaner.getProperties();
        props.setRecognizeUnicodeChars(true);
        props.setTranslateSpecialEntities(true);
        /*
        props.setAdvancedXmlEscape(true);
        props.setUseCdataForScriptAndStyle(true);
        props.setUseEmptyElementTags(true);
        */

        TagNode root = cleaner.clean(html);

        return new HtmlXPath(cleaner, root);
    }

    public HtmlXPath[] evaluateXPath(String expression) throws Exception
    {
        if (!(node instanceof TagNode))
        {
            throw new Exception();
        }

        TagNode n = (TagNode)node;

        Object[] ns = n.evaluateXPath(expression);
        if (null == ns || 0 >= ns.length)
        {
            return null;
        }

        HtmlXPath[] ret = new HtmlXPath[ns.length];
        for (int i = 0; i < ns.length; i++) {
            ret[i] = new HtmlXPath(cleaner, ns[i]);
        }

        return ret;
    }

    public String getString() throws Exception
    {
        if (node instanceof TagNode)
        {
            TagNode n = (TagNode)node;
            return n.getText().toString();
        }
        else if (node instanceof String)
        {
            return (String)node;
        }

        throw new Exception();
    }

    public String getInnerHtml() throws Exception
    {
        if (node instanceof TagNode)
        {
            return cleaner.getInnerHtml((TagNode)node);
        }
        else if (node instanceof String)
        {
            return (String)node;
        }

        throw new Exception();
    }
}
