package io.rong.imkit.usermanage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class GroupMembersCountLimitResourcesTest {

    @Test
    public void createGroupMemberLimitStringsMatchProductCopy() throws Exception {
        assertResourceEquals("values", "rc_group_members_count_exceed_limit", "群成员总数超过上限，创建失败");
        assertResourceEquals(
                "values-en",
                "rc_group_members_count_exceed_limit",
                "Total group member limit reached. Creation failed.");
        assertResourceEquals(
                "values-ar",
                "rc_group_members_count_exceed_limit",
                "تم تجاوز الحد الأقصى لإجمالي أعضاء المجموعة. فشل الإنشاء.");
    }

    @Test
    public void inviteGroupMemberLimitStringsMatchProductCopy() throws Exception {
        assertResourceEquals(
                "values", "rc_group_members_count_exceed_limit_invite", "群成员总数超过上限，邀请失败");
        assertResourceEquals(
                "values-en",
                "rc_group_members_count_exceed_limit_invite",
                "Total group member limit reached. Invitation failed.");
        assertResourceEquals(
                "values-ar",
                "rc_group_members_count_exceed_limit_invite",
                "تم تجاوز الحد الأقصى لإجمالي أعضاء المجموعة. فشلت الدعوة.");
    }

    @Test
    public void joinGroupMemberLimitStringsMatchProductCopy() throws Exception {
        assertResourceEquals(
                "values", "rc_group_members_count_exceed_limit_join", "群成员总数超过上限，加入失败");
        assertResourceEquals(
                "values-en",
                "rc_group_members_count_exceed_limit_join",
                "Total group member limit reached. Join failed.");
        assertResourceEquals(
                "values-ar",
                "rc_group_members_count_exceed_limit_join",
                "تم تجاوز الحد الأقصى لإجمالي أعضاء المجموعة. فشل الانضمام.");
    }

    private static void assertResourceEquals(String directory, String name, String expected)
            throws Exception {
        Element resource = findString(parseStrings(directory), name);
        assertNotNull("Missing resource " + name + " in " + directory, resource);
        assertEquals(expected, resource.getTextContent());
    }

    private static Document parseStrings(String directory) throws Exception {
        String relativePath = directory + "/strings.xml";
        File resource = new File("src/main/res", relativePath);
        if (!resource.isFile()) {
            resource = new File("IMKit/kit/src/main/res", relativePath);
        }
        if (!resource.isFile()) {
            resource = new File("android-imsdk/IMKit/kit/src/main/res", relativePath);
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
