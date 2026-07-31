package io.rong.imkit;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class OptionalLocationDependencyTest {
    private static final String LOCATION_MESSAGE_DESCRIPTOR =
            "io/rong/imlib/location/message/LocationMessage";

    private static final List<String> HIGH_RISK_IMKIT_CLASSES =
            Arrays.asList(
                    "io.rong.imkit.conversation.messgelist.viewmodel.MessageViewModel",
                    "io.rong.imkit.feature.resend.ResendManager",
                    "io.rong.imkit.feature.forward.ForwardManager");

    @Test
    public void highRiskImKitClassesDoNotReferenceOptionalLocationMessage() throws Exception {
        List<String> classesWithHardReference = new ArrayList<>();

        for (String className : HIGH_RISK_IMKIT_CLASSES) {
            if (readClassBytes(className).contains(LOCATION_MESSAGE_DESCRIPTOR)) {
                classesWithHardReference.add(className);
            }
        }

        assertTrue(
                "Optional LocationMessage hard references found in " + classesWithHardReference,
                classesWithHardReference.isEmpty());
    }

    private static String readClassBytes(String className) throws IOException {
        String resourceName = className.replace('.', '/') + ".class";
        ClassLoader classLoader = OptionalLocationDependencyTest.class.getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new IOException("Class resource not found: " + resourceName);
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return new String(outputStream.toByteArray(), StandardCharsets.ISO_8859_1);
        }
    }
}
