package io.github.adraffy.ens;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class ENSNormalize {
    
    static public final NF NF = new NF(decoder("/nf.bin"));
    static public final ENSIP15 ENSIP15 = new ENSIP15(NF, decoder("/spec.bin"));
    
    static Decoder decoder(String name) {
        try (InputStream in = ENSNormalize.class.getResourceAsStream(name)) {
            final int chunk = 8192;
            byte[] buf = new byte[chunk];
            int len = 0;
            while (true) {
                int read = in.read(buf, len, chunk);
                if (read == -1) break;
                len += read;
                if (buf.length - len < chunk) {
                    buf = Arrays.copyOf(buf, buf.length << 1);
                }
            }
            return new Decoder(ByteBuffer.wrap(buf, 0, len).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer());
        } catch (Exception err) {
            throw new IllegalStateException(name, err); 
        }
    }
    
}
