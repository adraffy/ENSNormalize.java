package adraffy.ens.normalize;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

class Tests {
    
    @Test void codepointCoding() {
        StringBuilder sb = new StringBuilder();
        for (int cp = 0; cp < 0x110000; cp++) {
            sb.setLength(0);
            StringUtils.appendCodepoint(sb, cp);
            StringUtils.appendCodepoint(sb, cp);
            int[] cps = StringUtils.explode(sb.toString());
            if (cps.length != 2 || cps[0] != cp || cps[1] != cp) {
                Assertions.fail(String.format("Expect[%s] vs Got[%s]", StringUtils.toHex(cp), StringUtils.toHexSequence(cps)));
            }
        }
    }
    
    @Test void readme() {
        
        Assertions.assertEquals("raffy\uD83D\uDEB4\u200D\u2642.eth", ENSNormalize.ENSIP15.normalize("RaFFY🚴‍♂️.eTh"));
        Assertions.assertEquals("1\uFE0F\u20E32\uFE0F\u20E3.eth", ENSNormalize.ENSIP15.beautify("1⃣2⃣.eth"));
        
        List<Label> labels = ENSNormalize.ENSIP15.split("💩Raffy.eth_");
        Assertions.assertEquals(2, labels.size());
        Assertions.assertArrayEquals(labels.get(0).input, new int[]{ 128169, 82, 97, 102, 102, 121 });
        Assertions.assertEquals(2, labels.get(0).tokens.size());
        Assertions.assertNotNull(labels.get(0).tokens.get(0).emoji);        
        Assertions.assertArrayEquals(labels.get(0).normalized, new int[]{ 128169, 114, 97, 102, 102, 121 });
        Assertions.assertEquals(labels.get(0).group.name, "Latin");
        Assertions.assertArrayEquals(labels.get(1).tokens.get(0).cps, new int[]{ 101, 116, 104, 95 });
        Assertions.assertNotNull(labels.get(1).error);
        
        Assertions.assertThrows(InvalidLabelException.class, () -> ENSNormalize.ENSIP15.normalize("AB--"));
        Assertions.assertEquals("ab--", ENSNormalize.ENSIP15.normalizeFragment("AB--"));
        Assertions.assertThrows(InvalidLabelException.class, () -> ENSNormalize.ENSIP15.normalize("..\u0300"));
        Assertions.assertEquals("..\u0300", ENSNormalize.ENSIP15.normalizeFragment("..\u0300"));
        Assertions.assertThrows(InvalidLabelException.class, () -> ENSNormalize.ENSIP15.normalize("\u03BF\u043E"));
        Assertions.assertEquals("\u03BF\u043E", ENSNormalize.ENSIP15.normalizeFragment("\u03BF\u043E"));
          
        Assertions.assertEquals("\u25CC\u0303\u200E {303}", ENSNormalize.ENSIP15.safeCodepoint(0x303));
        Assertions.assertEquals("{FE0F}", ENSNormalize.ENSIP15.safeCodepoint(0xFE0F));
        Assertions.assertEquals("\u25CC\u0303{FE0F}\u200E", ENSNormalize.ENSIP15.safeImplode(0x303, 0xFE0F));
        
        Assertions.assertEquals(true, ENSNormalize.ENSIP15.shouldEscape.contains(0x202E));
        Assertions.assertEquals(true, ENSNormalize.ENSIP15.combiningMarks.contains(0x20E3));
       
        Assertions.assertEquals("\u00E8", ENSNormalize.NF.NFC("\u0065\u0300"));
        Assertions.assertEquals("\u0065\u0300", ENSNormalize.NF.NFD("\u00E8"));
        
        Assertions.assertArrayEquals(new int[]{ 0xE8 }, ENSNormalize.NF.NFC(new int[]{ 0x65, 0x300 }));
        Assertions.assertArrayEquals(new int[]{ 0x65, 0x300 }, ENSNormalize.NF.NFD(new int[]{ 0xE8 }));

    }
    
    @Test void NFTests() {
        int errors = 0;
        for (Entry<String,Object> section: new JSONObject(asUTF8(readFile("data/nf-tests.json"))).toMap().entrySet()) {
            for (Object test: (List)section.getValue()) {
                List list = (List)test;
                String input = (String)list.get(0);
                String nfd0 = (String)list.get(1);
                String nfc0 = (String)list.get(2);
                String nfd = ENSNormalize.NF.NFD(input);
                String nfc = ENSNormalize.NF.NFC(input);
                if (!nfd.equals(nfd0)) {
                    errors++;
                    System.out.println(String.format("Wrong NFD: Expect[%s] Got[%s]", StringUtils.toHexSequence(nfd0), StringUtils.toHexSequence(nfd)));
                }
                if (!nfc.equals(nfc0)) {
                    errors++;
                    System.out.println(String.format("Wrong NFC: Expect[%s] Got[%s]", StringUtils.toHexSequence(nfc0), StringUtils.toHexSequence(nfc)));
                }
            }
        }
        Assertions.assertEquals(0, errors);
    }
    
    @Test void validationTests() {
        int errors = 0;
        for (Object test: new JSONArray(asUTF8(readFile("data/tests.json")))) {
            JSONObject obj = (JSONObject)test;
            String name = obj.getString("name");
            String norm0 = obj.optString("norm", name);
            boolean shouldError = obj.optBoolean("error", false);
            try {
                String norm = ENSNormalize.ENSIP15.normalize(name);
                if (shouldError) {
                    errors++;
                    System.out.println(String.format("Expected Error: [%s] Got[%s]", StringUtils.toHexSequence(name), StringUtils.toHexSequence(norm)));
                } else if (!norm.equals(norm0)) {
                    errors++;
                    System.out.println(String.format("Wrong Norm: [%s] Expect[%s] Got[%s]", StringUtils.toHexSequence(name), StringUtils.toHexSequence(norm0), StringUtils.toHexSequence(norm)));
                }
            } catch (InvalidLabelException err) {
                if (!shouldError) {
                    errors++;
                    System.out.println(String.format("Unexpected Error: [%s] Expect[%s] %s", StringUtils.toHexSequence(name), StringUtils.toHexSequence(norm0), err.getMessage()));
                }
            }
        }
        Assertions.assertEquals(0, errors);
    }

    static byte[] readFile(String path) {
        try {
            return Files.readAllBytes(Paths.get(path));
        } catch (IOException err) {
            throw new UncheckedIOException(err);
        }
    }
    
    static String asUTF8(byte[] v) {
        return new String(v, StandardCharsets.UTF_8);
    }
    
}
