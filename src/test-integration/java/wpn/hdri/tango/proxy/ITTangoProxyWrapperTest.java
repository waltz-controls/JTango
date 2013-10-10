/*
 * The main contributor to this project is Institute of Materials Research,
 * Helmholtz-Zentrum Geesthacht,
 * Germany.
 *
 * This project is a contribution of the Helmholtz Association Centres and
 * Technische Universitaet Muenchen to the ESS Design Update Phase.
 *
 * The project's funding reference is FKZ05E11CG1.
 *
 * Copyright (c) 2012. Institute of Materials Research,
 * Helmholtz-Zentrum Geesthacht,
 * Germany.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package wpn.hdri.tango.proxy;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertArrayEquals;

/**
 * Some tests performs similar operation many times. It is just to make sure that it does not have any side effects.
 *
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 07.06.12
 */
public class ITTangoProxyWrapperTest {
    public static final String TANGO_DEV_NAME = "test/local/0";
    public static final int TANGO_PORT = 16547;
    public static final String TEST_TANGO = "tango://localhost:" + TANGO_PORT + "/" + TANGO_DEV_NAME + "#dbase=no";

    private static Process PRC;

    @BeforeClass
    public static void beforeClass() throws Exception {
        String crtDir = System.getProperty("user.dir");
        //TODO define executable according to current OS
        StringBuilder bld = new StringBuilder(crtDir).append("/../exec/tango/win64/").append("TangoTest");
        PRC = new ProcessBuilder(bld.toString(), "test", "-ORBendPoint", "giop:tcp::" + TANGO_PORT, "-nodb", "-dlist", TANGO_DEV_NAME)
                .start();

        //drain slave's out stream
        new Thread(new Runnable() {
            @Override
            public void run() {
                char bite;
                try {
                    while ((bite = (char) PRC.getInputStream().read()) > -1) {
                        System.out.print(bite);
                    }
                } catch (IOException ignore) {
                }
            }
        }).start();

        //drains slave's err stream
        new Thread(new Runnable() {
            @Override
            public void run() {
                char bite;
                try {
                    while ((bite = (char) PRC.getErrorStream().read()) > -1) {
                        System.err.print(bite);
                    }
                } catch (IOException ignore) {
                }
            }
        }).start();
    }

    @Test(expected = TangoProxyException.class)
    public void testReadAttribute_Failed() throws Exception {
        TangoProxyWrapper instance = new TangoProxyWrapperImpl(TEST_TANGO);

        instance.readAttribute("string_scalarxxx");//no such attribute
    }

    //@Test
    public void testReadAttribute_Exception() throws Exception {
        TangoProxyWrapper instance = new TangoProxyWrapperImpl(TEST_TANGO);

        instance.readAttribute("string_scalar");


        PRC.destroy();

        instance.readAttribute("string_scalar");
    }

    @Test
    public void testWriteReadAttribute_String() throws Exception {
        TangoProxyWrapper instance = new TangoProxyWrapperImpl(TEST_TANGO);

        instance.writeAttribute("string_scalar", "Some test value");
        instance.writeAttribute("string_scalar", "Some test value");
        instance.writeAttribute("string_scalar", "Some test value");
        instance.writeAttribute("string_scalar", "Some test value");

        instance.readAttribute("string_scalar");
        instance.readAttribute("string_scalar");
        instance.readAttribute("string_scalar");
        instance.readAttribute("string_scalar");
        instance.readAttribute("string_scalar");
        String result = instance.readAttribute("string_scalar");

        assertEquals("Some test value", result);
    }

    @Test
    public void testWriteReadAttribute_Double() throws Exception {
        TangoProxyWrapper instance = new TangoProxyWrapperImpl(TEST_TANGO);

        instance.writeAttribute("double_scalar_w", 0.1984D);
        instance.writeAttribute("double_scalar_w", 0.1984D);
        instance.writeAttribute("double_scalar_w", 0.1984D);

        instance.<Double>readAttribute("double_scalar_w");
        double result = instance.<Double>readAttribute("double_scalar_w");

        assertEquals(0.1984D, result);
    }

    @Test
    public void testWriteReadAttribute_DoubleSpectrum() throws Exception {
        TangoProxyWrapper instance = new TangoProxyWrapperImpl(TEST_TANGO);

        instance.writeAttribute("double_spectrum",new double[]{0.1D,0.2D,0.3D,0.4D});
        double[] result = instance.<double[]>readAttribute("double_spectrum");

        assertEquals(4, result.length);
        assertArrayEquals(new double[]{0.1D,0.2D,0.3D,0.4D},result,0.07D);
    }

    //WAttribute::check_written_value():API_IncompatibleAttrDataType(Incompatible attribute type, expected type is : Tango::DevVarCharArray (even for single value))
    @Test(expected = TangoProxyException.class)
    public void testWriteReadAttribute_UChar() throws Exception {
        TangoProxyWrapper instance = new TangoProxyWrapperImpl(TEST_TANGO);

        instance.writeAttribute("uchar_scalar", 'a');
        instance.writeAttribute("uchar_scalar", 'a');
        instance.writeAttribute("uchar_scalar", 'a');

        instance.readAttribute("uchar_scalar");
        char result = instance.<Character>readAttribute("double_scalar_w");

        assertEquals('a', result);
    }

    @Test
    public void testWriteReadAttribute_DoubleArr() throws Exception {
        TangoProxyWrapper instance = new TangoProxyWrapperImpl(TEST_TANGO);

        instance.writeAttribute("double_spectrum", new double[]{0.1D, 0.9D, 0.8D, 0.4D});
        instance.writeAttribute("double_spectrum", new double[]{0.1D, 0.9D, 0.8D, 0.4D});
        instance.writeAttribute("double_spectrum", new double[]{0.1D, 0.9D, 0.8D, 0.4D});
        instance.writeAttribute("double_spectrum", new double[]{0.1D, 0.9D, 0.8D, 0.4D});
        instance.writeAttribute("double_spectrum", new double[]{0.1D, 0.9D, 0.8D, 0.4D});

        instance.readAttribute("double_spectrum");
        instance.readAttribute("double_spectrum");
        double[] result = instance.readAttribute("double_spectrum");

        assertArrayEquals(new double[]{0.1D, 0.9D, 0.8D, 0.4D}, result, 0.0);
    }

    @Test
    public void testWriteReadAttribute_DoubleArrArr() throws Exception {
        TangoProxyWrapper instance = new TangoProxyWrapperImpl(TEST_TANGO);

        instance.writeAttribute("double_image", new double[][]{{0.1D, 0.4D}, {0.9D, 0.8D}, {0.8D, 0.9D}, {0.4D, 0.1D}});
        instance.writeAttribute("double_image", new double[][]{{0.1D, 0.4D}, {0.9D, 0.8D}, {0.8D, 0.9D}, {0.4D, 0.1D}});
        instance.writeAttribute("double_image", new double[][]{{0.1D, 0.4D}, {0.9D, 0.8D}, {0.8D, 0.9D}, {0.4D, 0.1D}});
        instance.writeAttribute("double_image", new double[][]{{0.1D, 0.4D}, {0.9D, 0.8D}, {0.8D, 0.9D}, {0.4D, 0.1D}});
        instance.writeAttribute("double_image", new double[][]{{0.1D, 0.4D}, {0.9D, 0.8D}, {0.8D, 0.9D}, {0.4D, 0.1D}});

        instance.readAttribute("double_image");
        instance.readAttribute("double_image");
        instance.readAttribute("double_image");
        double[][] result = instance.readAttribute("double_image");

        assertArrayEquals(new double[]{0.1D, 0.4D}, result[0], 0.0);
        assertArrayEquals(new double[]{0.9D, 0.8D}, result[1], 0.0);
        assertArrayEquals(new double[]{0.8D, 0.9D}, result[2], 0.0);
        assertArrayEquals(new double[]{0.4D, 0.1D}, result[3], 0.0);
    }

    @Test(expected = TangoProxyException.class)
    public void testWriteReadAttribute_DoubleArrArr_Failed() throws Exception {
        TangoProxyWrapper instance = new TangoProxyWrapperImpl(TEST_TANGO);

        instance.writeAttribute("double_image", new double[][]{{0.1D, 0.4D}, {0.9D}});
    }

    @Test(expected = TangoProxyException.class)
    public void testWriteReadAttribute_DoubleArrArr_TooBig() throws Exception {
        TangoProxyWrapper instance = new TangoProxyWrapperImpl(TEST_TANGO);

        instance.writeAttribute("double_image", new double[256][256]);//251 max
    }

    @Test
    public void testSubscribeEvent() throws Exception {
        //TODO
    }

    @Test
    public void testUnsubscribeEvent() throws Exception {
        //TODO
    }

    @Test
    public void testExecuteCommand_String() throws Exception {
        TangoProxyWrapper instance = new TangoProxyWrapperImpl(TEST_TANGO);

        String result = instance.executeCommand("DevString", "Some test value");

        assertEquals("Some test value", result);
    }

    @Test
    public void testExecuteCommand_Void() throws Exception {
        TangoProxyWrapper instance = new TangoProxyWrapperImpl(TEST_TANGO);

        Void result = instance.executeCommand("DevVoid", null);

        assertNull(result);
    }

    @Test
    public void testExecuteCommand_DblArr() throws Exception {
        TangoProxyWrapper instance = new TangoProxyWrapperImpl(TEST_TANGO);

        double[] result = instance.executeCommand("DevVarDoubleArray", new double[]{0.1D, 0.9D, 0.8D, 0.4D});

        assertArrayEquals(new double[]{0.1D, 0.9D, 0.8D, 0.4D}, result, 0.0);
    }

    @Test
    public void testExecuteCommand_FltArr() throws Exception {
        TangoProxyWrapper instance = new TangoProxyWrapperImpl(TEST_TANGO);

        float[] result = instance.executeCommand("DevVarFloatArray", new float[]{0.1F, 0.9F, 0.8F, 0.4F});

        assertArrayEquals(new float[]{0.1F, 0.9F, 0.8F, 0.4F}, result, 0.0F);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        PRC.destroy();
    }
}
