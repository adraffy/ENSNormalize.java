package io.github.adraffy.ens;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ENSNormalize {
    
    static public final NF NF = new NF(decoder("/nf.bin"));
    static public final ENSIP15 ENSIP15 = new ENSIP15(NF, decoder("/spec.bin"));
    
    static Decoder decoder(String name) {
        try {
            Path file = Paths.get(ENSNormalize.class.getResource(name).toURI());
            ByteBuffer buf = ByteBuffer.wrap(Files.readAllBytes(file)).order(ByteOrder.LITTLE_ENDIAN);    
            return new Decoder(buf.asIntBuffer());
        } catch (Exception err) {  
            throw new IllegalStateException(name, err); 
        }
    }
    
}
