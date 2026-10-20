package net.decimation.fixes;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** docs/performance.md 10: the textures the warm up loads, read from Decimation's jar. */
class ModelWarmupTest
{
    @Test
    void picksThePicturesNpcsAreDrawnWith(@TempDir File dir) throws Exception
    {
        File jar = new File(dir, "Decimation.jar");
        ZipOutputStream z = new ZipOutputStream(new FileOutputStream(jar));
        for (String n : new String[] {"assets/deci/textures/model/guns/rifle/m4a4/m4a4.png",
                                      "assets/deci/textures/model/armor/marine.png",
                                      "assets/deci/textures/model/entities/mob/human/details/hair_1.png",
                                      "assets/deci/textures/model/masks/balaclava.png",
                                      "assets/deci/textures/model/guns/rifle/m4a4/notes.txt",   // not a picture
                                      "assets/deci/textures/gui/menu/banner.png",               // menu, not a model
                                      "assets/deci/models/guns/rifle/m4a4.bmodel",
                                      "deci/n/f.class"})
        {
            z.putNextEntry(new ZipEntry(n));
            z.write(1);
            z.closeEntry();
        }
        z.close();
        ZipFile in = new ZipFile(jar);
        List<String> got = ModelWarmup.texturePaths(in);
        in.close();
        assertEquals(Arrays.asList("textures/model/guns/rifle/m4a4/m4a4.png", "textures/model/armor/marine.png",
                                   "textures/model/entities/mob/human/details/hair_1.png", "textures/model/masks/balaclava.png"),
                     got);
    }
}
