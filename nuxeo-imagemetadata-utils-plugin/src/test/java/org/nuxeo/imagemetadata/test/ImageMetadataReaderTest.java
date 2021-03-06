/*
 * (C) Copyright ${year} Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     thibaud
 */

package org.nuxeo.imagemetadata.test;

import static org.junit.Assert.*;

import java.io.File;
import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.imagemetadata.ImageMetadataConstants.RESOLUTION_UNITS;
import org.nuxeo.imagemetadata.ImageMetadataReader;
import org.nuxeo.imagemetadata.SavePictureMeadataInDocument;
import org.nuxeo.imagemetadata.ImageMetadataConstants.METADATA_KEYS;
import org.nuxeo.imagemetadata.XYResolutionDPI;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class, CoreFeature.class,
        EmbeddedAutomationServerFeature.class })
@Deploy({ /*"org.nuxeo.ecm.platform.picture.core",*/ "nuxeo-imagemetadata-utils" })
public class ImageMetadataReaderTest {

    private static final String IMAGE_GIF = "images/a.gif";

    private static final String IMAGE_JPEG = "images/a.jpg";

    private static final String IMAGE_PNG = "images/a.png";

    private static final String IMAGE_TIF = "images/a.tif";

    private static final METADATA_KEYS[] KEYS = { METADATA_KEYS.WIDTH,
            METADATA_KEYS.HEIGHT, METADATA_KEYS.COLORSPACE,
            METADATA_KEYS.RESOLUTION, METADATA_KEYS.UNITS };

    protected File filePNG;

    protected File fileGIF;

    protected File fileTIF;

    protected File fileJPEG;

    protected DocumentModel parentOfTestDocs;

    protected DocumentModel docPNG;

    protected DocumentModel docGIF;

    protected DocumentModel docTIF;

    protected DocumentModel docJPEG;

    @Inject
    CoreSession coreSession;

    @Inject
    AutomationService service;

    @Before
    public void setUp() {
        // Setup documents if needed, etc.
        filePNG = FileUtils.getResourceFileFromContext(IMAGE_PNG);
        fileGIF = FileUtils.getResourceFileFromContext(IMAGE_GIF);
        fileTIF = FileUtils.getResourceFileFromContext(IMAGE_TIF);
        fileJPEG = FileUtils.getResourceFileFromContext(IMAGE_JPEG);

        // Cleanup repo if needed and create the Picture documents
        //coreSession.removeChildren(coreSession.getRootDocument().getRef());
        parentOfTestDocs = coreSession.createDocumentModel("/",
                "test-pictures", "Folder");
        parentOfTestDocs.setPropertyValue("dc:title", "test-pictures");
        parentOfTestDocs = coreSession.createDocument(parentOfTestDocs);
        parentOfTestDocs = coreSession.saveDocument(parentOfTestDocs);

/*
        docPNG = createPictureDocument(filePNG);
        docGIF = createPictureDocument(fileGIF);
        docTIF = createPictureDocument(fileTIF);
        docJPEG = createPictureDocument(fileJPEG);
        coreSession.save();
*/
    }

    @After
    public void cleanup() {
        coreSession.removeDocument(parentOfTestDocs.getRef());
        coreSession.save();
    }

    protected DocumentModel createPictureDocument(File inFile) {
	
        DocumentModel pictDoc = coreSession.createDocumentModel(
                parentOfTestDocs.getPathAsString(), inFile.getName(), "Picture");
        pictDoc.setPropertyValue("dc:title", inFile.getName());
        pictDoc.setPropertyValue("file:content", new FileBlob(inFile));
        return coreSession.createDocument(pictDoc);

    }

    protected void checkValues(File inWhichOne, String inWidth, String inHeight,
            String inColorspace, String inResolution, String inUnits, int xDPI,
            int yDPI) throws Exception {

        String fileName = inWhichOne.getName();
        ImageMetadataReader imdr = new ImageMetadataReader(
                inWhichOne.getAbsolutePath());

        HashMap<METADATA_KEYS, String> result = imdr.getMetadata(KEYS);
        assertNotNull(fileName, result);

        assertEquals(fileName, inWidth, result.get(METADATA_KEYS.WIDTH));
        assertEquals(fileName, inHeight, result.get(METADATA_KEYS.HEIGHT));
        assertEquals(fileName, inColorspace,
                result.get(METADATA_KEYS.COLORSPACE));
        assertEquals(fileName, inResolution,
                result.get(METADATA_KEYS.RESOLUTION));
        assertEquals(fileName, inUnits, result.get(METADATA_KEYS.UNITS));

        // Resolution needs extra work
        XYResolutionDPI dpi = new XYResolutionDPI(
                result.get(METADATA_KEYS.RESOLUTION),
                result.get(METADATA_KEYS.UNITS));
        assertEquals(fileName, xDPI, dpi.getX());
        assertEquals(fileName, yDPI, dpi.getY());
    }

    @Test
    public void testAllImages() throws Exception {
        checkValues(filePNG, "100", "100", "sRGB", "37.79x37.79",
                "PixelsPerCentimeter", 96, 96);
        checkValues(fileGIF, "328", "331", "sRGB", "72x72", "Undefined", 72, 72);
        checkValues(fileTIF, "438", "640", "sRGB", "72x72", "PixelsPerInch",
                72, 72);
        checkValues(fileJPEG, "1597", "232", "sRGB", "96x96", "PixelsPerInch",
                96, 96);
    }

    @Test
    public void testGetAllMetadata() throws Exception {
        ImageMetadataReader imdr = new ImageMetadataReader(
                filePNG.getAbsolutePath());
        String all = imdr.getAllMetadata();
        assertTrue(all != null);
        assertTrue(!all.isEmpty());

        // Just for an example:
        assertTrue(all.indexOf("Format=PNG") > -1);
    }

    @Test
    public void testXYResolutionDPI() throws Exception {
        XYResolutionDPI xyDPI = new XYResolutionDPI("180x180",
                RESOLUTION_UNITS.PIXELS_PER_INCH.toString());
        assertEquals(180, xyDPI.getX());
        assertEquals(180, xyDPI.getY());

        xyDPI = new XYResolutionDPI("37.89x37.89",
                RESOLUTION_UNITS.PIXELS_PER_CENTIMETER.toString());
        assertEquals(96, xyDPI.getX());
        assertEquals(96, xyDPI.getY());

        xyDPI = new XYResolutionDPI("72x72",
                RESOLUTION_UNITS.UNDEFINED.toString());
        assertEquals(72, xyDPI.getX());
        assertEquals(72, xyDPI.getY());

        xyDPI = new XYResolutionDPI("",
                RESOLUTION_UNITS.PIXELS_PER_INCH.toString());
        assertEquals(0, xyDPI.getX());
        assertEquals(0, xyDPI.getY());
    }


    @Test
    public void testSavePictureMetadataInDocument() throws Exception {
        OperationContext ctx = new OperationContext(coreSession);
/*
        assertNotNull(ctx);

        // ========================================
        // TEST WITH DEFAULT VALUES
        // ========================================
        String changeToken = docPNG.getChangeToken();
        ctx.setInput(docPNG);
        OperationChain chain = new OperationChain("testChain");
        // let default value for "xpath", "properties" and "save"
        chain.add(SavePictureMeadataInDocument.ID);
        service.run(ctx, chain);

        // Check the doc was modified
        assertNotSame(changeToken, docPNG.getChangeToken());

        // Check value for this PNG
        assertEquals((long) 100, docPNG.getPropertyValue("imd:pixel_xdimension"));
        assertEquals((long) 100, docPNG.getPropertyValue("imd:pixel_ydimension"));
        assertEquals("sRGB", docPNG.getPropertyValue("imd:color_space"));
        assertEquals((long) 96, docPNG.getPropertyValue("imd:xresolution"));
        assertEquals((long) 96, docPNG.getPropertyValue("imd:yresolution"));

        // ========================================
        // ASK FOR ALL PROPERTIES
        // ========================================
        changeToken = docPNG.getChangeToken();
        ctx.setInput(docPNG);
        chain = new OperationChain("testChain");
        // Let xpath and save the default values
        Properties props = new Properties();
        props.put("dc:description", "all");
        chain.add(SavePictureMeadataInDocument.ID).set("properties", props);
        service.run(ctx, chain);

        // Check the doc was modified
        assertNotSame(changeToken, docPNG.getChangeToken());

        // Check value for this PNG
        String all = (String) docPNG.getPropertyValue("dc:description");
        assertTrue(all != null && !all.isEmpty());
        // Possibly, check some values are available
        assertTrue(all.indexOf("Page geometry") > -1);
        assertTrue(all.indexOf("Units") > -1);

        // ========================================
        // NO SAVE MUST, WELL, NOT SAVE
        // ========================================
        changeToken = docPNG.getChangeToken();
        ctx.setInput(docPNG);
        chain = new OperationChain("testChain");
        // Let xpath and properties the default values
        chain.add(SavePictureMeadataInDocument.ID).set("save", false);
        service.run(ctx, chain);

        assertEquals(changeToken, docPNG.getChangeToken());
*/
    }
}
