package webtools;

import org.htmlcleaner.*;
import org.jdom.*;
import java.io.*;
import java.net.*;
import java.util.*;


public class Test
{
    public static String retrievePage(String url) throws Exception
    {
        HttpManager mg = new HttpManager();
        String req = "{\"url\":\"" + url + "\", \"charset\":\"utf-8\", \"method\":\"GET\"}";
        return mg.execute(req);
    }

    public static void test1() throws Exception
    {
        HtmlCleaner cleaner = new HtmlCleaner();
        CleanerProperties props = cleaner.getProperties();

//        props.setUseCdataForScriptAndStyle(true);
        props.setRecognizeUnicodeChars(true);
//        props.setUseEmptyElementTags(true);
        props.setAdvancedXmlEscape(true);
        props.setTranslateSpecialEntities(true);


        String content = "<html><head></head><body><div att1='xxx'>abc<br><p>mmm</p></div><div>kkk</div></body></html>";
        System.out.println(content);
        TagNode node = cleaner.clean(content);
        Object[] ns = node.evaluateXPath("//div/@att1");
        for (Object iter : ns) {
            TagNode n = (TagNode)iter;
            XmlSerializer ser = new PrettyXmlSerializer(props);
            StringWriter wr = new StringWriter();
            ser.writeXml(n, wr, "utf-8");
            System.out.println(wr.toString());
            System.out.println(cleaner.getInnerHtml(n));
        }
    }

    public static void test2() throws Exception
    {
        HtmlCleaner cleaner = new HtmlCleaner();
        CleanerProperties props = cleaner.getProperties();

//        props.setUseCdataForScriptAndStyle(true);
        props.setRecognizeUnicodeChars(true);
//        props.setUseEmptyElementTags(true);
//        props.setAdvancedXmlEscape(true);
//        props.setTranslateSpecialEntities(true);

//        cleaner.setOmitXmlDeclaration(true);
//        cleaner.setOmitXmlEnvelope(true);
        TagNode node = cleaner.clean(new URL("http://www.baidu.com/"), "UTF-8");
        XmlSerializer xmlSerial = new PrettyXmlSerializer(props);
        StringWriter writer = new StringWriter();
        xmlSerial.writeXml(node, writer, "UTF-8");
 //       System.out.println(writer.toString());
        Object[] ns = node.evaluateXPath("//table/text()");
        for (Object iter : ns) {
//            TagNode n = (TagNode)iter;
//            System.out.println(n.getText());
            System.out.println(iter);
        }
    }

    public static void test3() throws Exception
    {
        String url = "http://9gag.com";
        String content = retrievePage(url);
        System.out.println(content);

        HtmlXPath x = HtmlXPath.build(content);

        HtmlXPath[] res = x.evaluateXPath("//title");
        System.out.println(res[0].getString());

    }

    public static void main(String[] args) throws Exception
    {
        test3();
    }

}

