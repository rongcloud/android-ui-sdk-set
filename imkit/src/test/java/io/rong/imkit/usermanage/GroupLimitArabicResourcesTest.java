package io.rong.imkit.usermanage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class GroupLimitArabicResourcesTest {

    @Test
    public void groupLimitArabicStringsMatchIosApprovedTranslations() throws Exception {
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put(
                "rc_group_app_count_exceed_limit",
                "تم تجاوز الحد الأقصى لعدد المجموعات. فشل الإنشاء.");
        expected.put(
                "rc_group_join_count_exceed_limit", "تم بلوغ الحد الأقصى للمجموعات المنضم إليها.");
        expected.put(
                "rc_group_failed_users_join_count_exceed_limit_single",
                "بلغ %1$s الحد الأقصى للانضمام إلى المجموعات، ولا يمكنه الانضمام إلى مجموعات أخرى.");
        expected.put(
                "rc_group_failed_users_join_count_exceed_limit_multiple",
                "بلغ %1$s الحد الأقصى للانضمام إلى المجموعات، ولا يمكنهم الانضمام إلى مجموعات أخرى.");
        expected.put(
                "rc_group_application_join_count_exceed_limit",
                "فشل الانضمام. بلغ %1$s الحد الأقصى لعدد المجموعات.");
        expected.put("rc_group_member_invite_failed_description", "فشل الانضمام إلى المجموعة");
        expected.put("rc_group_limit_name_list_pair", "%1$s و%2$s");
        expected.put("rc_group_limit_name_list_middle", "%1$s، %2$s");
        expected.put("rc_group_limit_name_list_end", "%1$s، و%2$s");

        Document document = parseArabicStrings();
        for (Map.Entry<String, String> entry : expected.entrySet()) {
            Element resource = findString(document, entry.getKey());
            assertNotNull("Missing Arabic resource: " + entry.getKey(), resource);
            assertEquals(entry.getValue(), resource.getTextContent());
        }
    }

    private static Document parseArabicStrings() throws Exception {
        String modulePath = "IMKit/kit/src/main/res/values-ar/strings.xml";
        File resource = new File("src/main/res/values-ar/strings.xml");
        if (!resource.isFile()) {
            resource = new File(modulePath);
        }
        if (!resource.isFile()) {
            resource = new File("android-imsdk", modulePath);
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        return factory.newDocumentBuilder().parse(resource);
    }

    private static Element findString(Document document, String name) {
        NodeList strings = document.getElementsByTagName("string");
        for (int index = 0; index < strings.getLength(); index++) {
            Element element = (Element) strings.item(index);
            if (name.equals(element.getAttribute("name"))) {
                return element;
            }
        }
        return null;
    }
}
