package stirling.software.SPDF.utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.*;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipInputStream;

public class PdfUtilsWhiteboxTest {
    private static boolean imageEquals(BufferedImage a, BufferedImage b) {
        // The images must be the same size.
        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
            return false;
        }

        int width = a.getWidth();
        int height = a.getHeight();

        // Loop over every pixel.
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Compare the pixels for equality.
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static Stream<Arguments> provideTextAndRectangle() {
        return Stream.of(
                Arguments.of("A0", PDRectangle.A0),
                Arguments.of("A1", PDRectangle.A1),
                Arguments.of("A2", PDRectangle.A2),
                Arguments.of("A3", PDRectangle.A3),
                Arguments.of("A4", PDRectangle.A4),
                Arguments.of("A5", PDRectangle.A5),
                Arguments.of("A6", PDRectangle.A6),
                Arguments.of("LETTER", PDRectangle.LETTER),
                Arguments.of("LEGAL", PDRectangle.LEGAL)
        );
    }

    private static void saveFile(byte[] fileBytes, String filePath) throws IOException {
        // Write the PDF bytes to a file
        FileOutputStream fos = new FileOutputStream(filePath);
        fos.write(fileBytes);
    }

    private static PDDocument txtAndImg;
    private static PDDocument imgOnly;
    private static PDDocument txtOnly;
    private static PDDocument multiPage;
    private static final String conversionPath = "testFiles/outputs/";

    @BeforeEach
    public void setup() {
        try {
            txtAndImg = Loader.loadPDF(new File("testFiles/inputs/txtAndImg.pdf"));
            imgOnly = Loader.loadPDF(new File("testFiles/inputs/imgOnly.pdf"));
            txtOnly = Loader.loadPDF(new File("testFiles/inputs/txtOnly.pdf"));
            multiPage = Loader.loadPDF(new File("testFiles/inputs/multiPage.pdf"));
        } catch (IOException ex) {
            fail("Could not complete test setup");
        }
    }

    @AfterEach
    public void tearDown() {
        try {
            txtAndImg.close();
            imgOnly.close();
            txtOnly.close();
            multiPage.close();
        } catch (IOException ex) {
            fail("Could not complete test setup");
        }
    }

    @ParameterizedTest
    @MethodSource("provideTextAndRectangle")
    void testTextToPageSizeSuccess(String size, PDRectangle rect) {
        assertEquals(rect, PdfUtils.textToPageSize(size));
    }

    @Test
    void testTextToPageSizeThrows() {
        assertThrows(IllegalArgumentException.class, () -> PdfUtils.textToPageSize("A7"));
    }

    @Nested
    class Image {
        @Test
        void testGetAllImages() {
            PDDocument doc = new PDDocument();
            doc.addPage(new PDPage());

            try {
                PDImageXObject img = PDImageXObject.createFromFile("testFiles/inputs/blue.png", doc);

                PDPage page = txtAndImg.getPage(0);
                List<RenderedImage> imgList = PdfUtils.getAllImages(page.getResources());
                BufferedImage buf1 = img.getImage();
                BufferedImage buf2 = (BufferedImage) imgList.get(0);

                assertEquals(1, imgList.size());
                assertTrue(imageEquals(buf1, buf2));
            } catch (IOException ex) {
                fail(ex.toString());
            }
        }

        @Test
        // Must manually create PDF to add FormXObject
        void testGetAllImagesForm() {
            try {
                PDDocument doc = new PDDocument();
                PDPage page = new PDPage();
                doc.addPage(page);

                // Create a PDFormXObject
                PDFormXObject formXObject = new PDFormXObject(doc);
                PDResources formResources = new PDResources();

                // Add an image to the formXObject
                PDImageXObject img = PDImageXObject.createFromFile("testFiles/inputs/blue.png", doc);
                PDPageContentStream contentStream = new PDPageContentStream(doc, page);
                BufferedImage buf = img.getImage();

                // Convert BufferedImage to byte array and draw image
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(buf, "png", baos);
                PDImageXObject pdImage = PDImageXObject.createFromByteArray(doc, baos.toByteArray(), "image");
                contentStream.drawImage(pdImage, 50, 50);
                formResources.put(COSName.getPDFName("ImageXObject"), pdImage);
                formXObject.setResources(formResources);

                // Add the formXObject to the page's resources
                PDResources pageResources = new PDResources();
                pageResources.put(COSName.getPDFName("FormXObject"), formXObject);
                page.setResources(pageResources);

                List<RenderedImage> imgList = PdfUtils.getAllImages(page.getResources());
                BufferedImage buf1 = img.getImage();
                BufferedImage buf2 = (BufferedImage) imgList.get(0);

                assertEquals(1, imgList.size());
                assertTrue(imageEquals(buf1, buf2));
            } catch (Exception ex) {
                fail(ex.toString());
            }

        }

        @Test
        void testHasImagesFalse() {
            try {
                assertFalse(PdfUtils.hasImages(txtOnly, "all"));
            } catch (IOException ex) {
                fail(ex.toString());
            }
        }

        @Test
        // Found fault when inputting "0" as page to check, trace back to generalUtils line 220
        // hasImage seems very inefficient by calling getPage multiple times (which calls getPages under the hood)
        // Note we use 1 based pages from here on for these functions
        void testHasImagesTrue() {
            try {
                assertTrue(PdfUtils.hasImages(imgOnly, "all"));
            } catch (IOException ex) {
                fail(ex.toString());
            }
        }

        @Tag("fails")
        @Test
        // Found fault when inputting "0" as page to check, trace back to generalUtils line 220
        // hasImage seems very inefficient by calling getPage multiple times (which calls getPages under the hood)
        // Note we use 1 based pages from here on for these functions
        void testHasImagesTruePage0() {
            try {
                assertTrue(PdfUtils.hasImages(imgOnly, "0"));
            } catch (IOException ex) {
                fail(ex.toString());
            }
        }

        @Test
        void testHasImagesOnPageFalse() {
            try {
                assertFalse(PdfUtils.hasImagesOnPage(txtOnly.getPage(0)));
            } catch (IOException ex) {
                fail(ex.toString());
            }
        }

        @Test
        void testHasImagesOnPageTrue() {
            try {
                assertTrue(PdfUtils.hasImagesOnPage(imgOnly.getPage(0)));
            } catch (IOException ex) {
                fail(ex.toString());
            }
        }
    }

    @Nested
    class Text {
        @Test
        void testHasTextTrue() {
            try {
                assertTrue(PdfUtils.hasText(txtOnly, "all", "text"));
            } catch (IOException ex) {
                fail(ex.toString());
            }
        }

        @Tag("fails")
        @Test
        // Same as testHasImagesTruePage0
        void testHasTextTruePage0() {
            try {
                assertTrue(PdfUtils.hasText(txtOnly, "0", "text"));
            } catch (IOException ex) {
                fail(ex.toString());
            }
        }

        @Test
        void testHasTextFalse() {
            try {
                assertFalse(PdfUtils.hasText(txtOnly, "all", "not present"));
            } catch (IOException ex) {
                fail(ex.toString());
            }
        }

        @Test
        void testHasTextOnPageTrue() {
            try {
                assertTrue(PdfUtils.hasTextOnPage(txtOnly.getPage(0), "ex"));
            } catch (IOException ex) {
                fail(ex.toString());
            }
        }

        @Test
        void testHasTextOnPageFalse() {
            try {
                assertFalse(PdfUtils.hasTextOnPage(txtOnly.getPage(0), "texts"));
            } catch (IOException ex) {
                fail(ex.toString());
            }
        }

        @Test
        void testContainsTextFileNullPageTrue() {
            try {
                assertTrue(PdfUtils.containsTextInFile(txtOnly, "text", null));
            } catch (IOException ex) {
                fail(ex.toString());
            }
        }

        @Test
        void testContainsTextFileAllPageTrue() {
            try {
                assertTrue(PdfUtils.containsTextInFile(txtOnly, "text", "all"));
            } catch (IOException ex) {
                fail(ex.toString());
            }
        }

        @Test
        void testContainsTextFileSinglePageTrue() {
            try {
                assertTrue(PdfUtils.containsTextInFile(multiPage, "text", "4"));
            } catch (IOException ex) {
                fail(ex.toString());
            }
        }

        @Test
        void testContainsTextFileRangeFalse() {
            try {
                assertFalse(PdfUtils.containsTextInFile(multiPage, "text", "1-2, 3"));
            } catch (IOException ex) {
                fail(ex.toString());
            }
        }

        @Test
        void testContainsTextFileFalse() {
            try {
                assertFalse(PdfUtils.containsTextInFile(txtOnly, "not present", "0"));
            } catch (IOException ex) {
                fail(ex.toString());
            }
        }
    }

    @Nested
    class PageCount {
        @Test
        public void testGreaterTrue() {
            try {
                assertTrue(PdfUtils.pageCount(multiPage, 2, "greater"));
            } catch (IOException ex) {
                fail(ex.toString());
            }
        }

        @Test
        public void testGreaterFalse() {
            try {
                assertFalse(PdfUtils.pageCount(multiPage, 4, "GREATER"));
            } catch (IOException ex) {
                fail(ex.toString());
            }
        }

        @Test
        public void testLessFalse() {
            try {
                assertFalse(PdfUtils.pageCount(multiPage, 4, "LESS"));
            } catch (IOException ex) {
                fail(ex.toString());
            }
        }

        @Test
        public void testLessTrue() {
            try {
                assertTrue(PdfUtils.pageCount(multiPage, 5, "less"));
            } catch (IOException ex) {
                fail(ex.toString());
            }
        }

        @Test
        public void testEqualTrue() {
            try {
                assertTrue(PdfUtils.pageCount(multiPage, 4, "eQual"));
            } catch (IOException ex) {
                fail(ex.toString());
            }
        }

        @Test
        public void testEqualFalse() {
            try {
                assertFalse(PdfUtils.pageCount(multiPage, 3, "equal"));
            } catch (IOException ex) {
                fail(ex.toString());
            }
        }

        @Test
        public void testInvalid() {
            assertThrows(IllegalArgumentException.class, () -> PdfUtils.pageCount(imgOnly, 8, "lessOrEqual"));
        }
    }

    @Nested
    class PageSize {
        @Test
        public void testCorrectSize() {
            try {
                assertTrue(PdfUtils.pageSize(txtAndImg, "612x792"));
            } catch (IOException ex) {
                fail(ex.toString());
            }
        }

        @Test
        public void testIncorrectSize() {
            try {
                assertFalse(PdfUtils.pageSize(txtAndImg, "792x612"));
            } catch (IOException ex) {
                fail(ex.toString());
            }
        }

        @Test
        public void testNearSize() {
            try {
                assertFalse(PdfUtils.pageSize(txtAndImg, "612x612"));
            } catch (IOException ex) {
                fail(ex.toString());
            }
        }
    }

    @Nested
    class ConvertToImage {
        @Test
        public void testInvalidPDF() {
            assertThrows(IOException.class, () -> {
                byte[] bytes = FileUtils.readFileToByteArray(new File("testFiles/inputs/blue.png"));
                byte[] res = PdfUtils.convertFromPdf(bytes, "png", ImageType.RGB, true, 300, "img");

                saveFile(res, conversionPath + "toImage/shouldNotExist.png");
            });
        }

        @Test
        // Multi-paged but single image
        public void testConvertPNGSingle() {
            try {
                byte[] bytes = FileUtils.readFileToByteArray(new File("testFiles/inputs/multiPage.pdf"));
                byte[] res = PdfUtils.convertFromPdf(bytes, "png", ImageType.RGB, true, 300, "img");

                saveFile(res, conversionPath + "toImage/multiPage.png");
                assertTrue(isValidImage(new File(conversionPath + "toImage/multiPage.png")));
            } catch (Exception ex) {
                fail(ex.toString());
            }
        }

        @Test
        public void testConvertTIFFSingle() {
            try {
                byte[] bytes = FileUtils.readFileToByteArray(new File("testFiles/inputs/imgOnly.pdf"));
                byte[] tiffImageBytes = PdfUtils.convertFromPdf(bytes, "tiff", ImageType.RGB, true, 300, "img");

                saveFile(tiffImageBytes, conversionPath + "toImage/imgPDF.tiff");
                assertTrue(isValidImage(new File(conversionPath + "toImage/imgPDF.tiff")));
            } catch (Exception ex) {
                fail(ex.toString());
            }
        }

        @Tag("Presentation")
        @Test
        // multi-paged but single image
        public void testConvertTIFSingle() {
            try {
                byte[] bytes = FileUtils.readFileToByteArray(new File("testFiles/inputs/multiPage.pdf"));
                byte[] tiffImageBytes = PdfUtils.convertFromPdf(bytes, "tif", ImageType.RGB, true, 300, "img");

                saveFile(tiffImageBytes, conversionPath + "toImage/multiPage.tif");
                assertTrue(isValidImage(new File(conversionPath + "toImage/multiPage.tif")));
            } catch (Exception ex) {
                fail(ex.toString());
            }
        }

        @Test
        public void testConvertJPGMulti() {
            try {
                byte[] bytes = FileUtils.readFileToByteArray(new File("testFiles/inputs/multiPage.pdf"));
                byte[] zipBytes = PdfUtils.convertFromPdf(bytes, "jpg", ImageType.RGB, false, 300, "img");

                extractImagesFromZip(zipBytes, conversionPath + "zipJPG/", "jpg");
                // assert num images == num pages
                assertEquals(multiPage.getNumberOfPages(), new File(conversionPath + "zipJPG").listFiles().length);
            } catch (Exception ex) {
                fail(ex.toString());
            }
        }

        public static boolean isValidImage(File f) {
            boolean isValid = true;
            try {
                ImageIO.read(f).flush();
            } catch (Exception e) {
                isValid = false;
            }
            return isValid;
        }

        public static void extractImagesFromZip(byte[] zipBytes, String dirPath, String ext) throws IOException {
            try (ByteArrayInputStream bis = new ByteArrayInputStream(zipBytes);
                 ZipInputStream zis = new ZipInputStream(bis)) {

                int i = 0;
                while (zis.getNextEntry() != null) {
                    // Read the image bytes
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = zis.read(buffer)) != -1) {
                        bos.write(buffer, 0, bytesRead);
                    }

                    // Convert image bytes to BufferedImage
                    ByteArrayInputStream imageInputStream = new ByteArrayInputStream(bos.toByteArray());
                    BufferedImage image = ImageIO.read(imageInputStream);
                    imageInputStream.close();

                    // Save the BufferedImage
                    String outputFileName = dirPath + "page_" + (++i) + "." + ext;
                    FileOutputStream fos = new FileOutputStream(outputFileName);
                    ImageIO.write(image, ext, fos);
                    fos.close();

                    System.out.println("Image extracted and saved: " + outputFileName);
                }
            }
        }
    }

    @Nested
    class ConvertToPdf {
        // This approach is close to each choice coverage -- the goal here is to optimize for statement coverage
        @Test
        public void testPNGToPdf() {
            try {
                File imageFile = new File("testFiles/inputs/blue.png");
                MultipartFile image = new MockMultipartFile("file.png", IOUtils.toByteArray(new FileInputStream(imageFile)));
                byte[] pdf = PdfUtils.imageToPdf(new MultipartFile[]{image}, "fillPage", false, "color");
                saveFile(pdf, conversionPath + "toPDF/bluePNG.pdf");

                // Load the pdf and ensure extracted image is the same as the original
                PDDocument generatedPDF = Loader.loadPDF(new File(conversionPath + "toPDF/bluePNG.pdf"));
                PDImageXObject img = PDImageXObject.createFromFile("testFiles/inputs/blue.png", generatedPDF);

                PDPage page = generatedPDF.getPage(0);
                List<RenderedImage> imgList = PdfUtils.getAllImages(page.getResources());
                BufferedImage buf1 = img.getImage();
                BufferedImage buf2 = (BufferedImage) imgList.get(0);

                assertEquals(1, imgList.size());
                assertTrue(imageEquals(buf1, buf2));
            } catch (Exception ex) {
                fail(ex.toString());
            }
        }

        // This is a semi-fault. Unfortunately, we can't really perform easy assertions on the "black/whiteness" of the generated PDF.
        // While the image is converted to black and white, the image type is BYTE_GREY, so it appears the image properties
        // are not preserved in the conversion. However, the generated pdf can be manually inspected to be correctly black and white
        @Tag("fails")
        @Test
        public void testTIFToPdfBlackWhite() {
            // with autoRotate = true
            try {
                File imageFile = new File("testFiles/inputs/marbles.tif");
                MultipartFile image = new MockMultipartFile("file.tif", "file.tif", "image/tif", IOUtils.toByteArray(new FileInputStream(imageFile)));
                byte[] pdf = PdfUtils.imageToPdf(new MultipartFile[]{image}, "fillPage", false, "blackwhite");
                saveFile(pdf, conversionPath + "toPDF/marblesBlackWhite.pdf");

                // Load the pdf and ensure the image type is blackwhite
                PDDocument generatedPDF = Loader.loadPDF(new File(conversionPath + "toPDF/marblesBlackWhite.pdf"));

                PDPage page = generatedPDF.getPage(0);
                List<RenderedImage> imgList = PdfUtils.getAllImages(page.getResources());
                BufferedImage img = (BufferedImage) imgList.get(0);

                assertEquals(1, imgList.size());
                assertEquals(BufferedImage.TYPE_BYTE_BINARY, img.getType());
            } catch (Exception ex) {
                fail(ex.toString());
            }
        }

        // Same as above, except the type is regular RGB.
        @Tag("fails")
        @Test
        public void testTIFFToPdfGreyscale() {
            try {
                File imageFile = new File("testFiles/inputs/marbles.tif");
                MultipartFile image = new MockMultipartFile("file.tiff", "file.tiff", "image/tiff", IOUtils.toByteArray(new FileInputStream(imageFile)));
                byte[] pdf = PdfUtils.imageToPdf(new MultipartFile[]{image}, "fitDocumentToImage", false, "greyscale");
                saveFile(pdf, conversionPath + "toPDF/marblesGreyscale.pdf");

                // Load the pdf and ensure the image type is greyscale
                PDDocument generatedPDF = Loader.loadPDF(new File(conversionPath + "toPDF/marblesGreyscale.pdf"));

                PDPage page = generatedPDF.getPage(0);
                List<RenderedImage> imgList = PdfUtils.getAllImages(page.getResources());
                BufferedImage img = (BufferedImage) imgList.get(0);

                assertEquals(1, imgList.size());
                assertEquals(BufferedImage.TYPE_BYTE_GRAY, img.getType());
            } catch (Exception ex) {
                fail(ex.toString());
            }
        }

        // Output page size should be inverted A4 due to rotation
        @Test
        public void testLandscapeAutoRotate() {
            try {
                File imageFile = new File("testFiles/inputs/marbles.tif");
                MultipartFile image = new MockMultipartFile("file.tiff", "file.tiff", "image/tiff", IOUtils.toByteArray(new FileInputStream(imageFile)));
                byte[] pdf = PdfUtils.imageToPdf(new MultipartFile[]{image}, "fillPage", true, "color");
                saveFile(pdf, conversionPath + "toPDF/marblesRotated.pdf");

                // Ensure page size has been rotated
                PDDocument generatedPDF = Loader.loadPDF(new File(conversionPath + "toPDF/marblesRotated.pdf"));
                float height = generatedPDF.getPage(0).getMediaBox().getHeight();
                float width = generatedPDF.getPage(0).getMediaBox().getWidth();
                assertTrue(height == PDRectangle.A4.getWidth() && width == PDRectangle.A4.getHeight());
            } catch (Exception ex) {
                fail(ex.toString());
            }
        }

        @Test
        public void testPortraitAutoRotate() {
            try {
                File imageFile = new File("testFiles/inputs/bluePortrait.png");
                MultipartFile image = new MockMultipartFile("file.png", "file.png", "image/png", IOUtils.toByteArray(new FileInputStream(imageFile)));
                byte[] pdf = PdfUtils.imageToPdf(new MultipartFile[]{image}, "fillPage", true, "color");
                saveFile(pdf, conversionPath + "toPDF/bluePortrait.pdf");

                // Ensure page size has not been rotated
                PDDocument generatedPDF = Loader.loadPDF(new File(conversionPath + "toPDF/bluePortrait.pdf"));
                float height = generatedPDF.getPage(0).getMediaBox().getHeight();
                float width = generatedPDF.getPage(0).getMediaBox().getWidth();
                assertTrue(height == PDRectangle.A4.getHeight() && width == PDRectangle.A4.getWidth());
            } catch (Exception ex) {
                fail(ex.toString());
            }
        }

        // Manually inspected PDF looks good. The difference between this test and the next one is that this
        // marks the content type as jpeg, causing the code to use JPEGFactory to convert. It appears that the
        // regular LossLessFactory may be more reliable than JPEGFactory for jpegs...
        // We consider this another "semi-fault". There is likely some loss in the JPEG conversion, but it seems
        // too minuscule to be considered practically relevant, by the manual inspection.
        @Tag("Presentation")
        @Tag("fails")
        @Test
        public void testJPEGToPdf() {
            try {
                File imageFile = new File("testFiles/inputs/blue.jpeg");
                MultipartFile image = new MockMultipartFile("file.jpeg", "file.jpeg", "image/jpeg", IOUtils.toByteArray(new FileInputStream(imageFile)));
                byte[] pdf = PdfUtils.imageToPdf(new MultipartFile[]{image}, "fillPage", false, "color");
                saveFile(pdf, conversionPath + "toPDF/blueJPEG.pdf");

                // Load the pdf and ensure extracted image is the same as the original
                PDDocument generatedPDF = Loader.loadPDF(new File(conversionPath + "toPDF/blueJPEG.pdf"));
                PDImageXObject img = PDImageXObject.createFromFile("testFiles/inputs/blue.jpeg", generatedPDF);

                PDPage page = generatedPDF.getPage(0);
                List<RenderedImage> imgList = PdfUtils.getAllImages(page.getResources());
                BufferedImage buf1 = img.getImage();
                BufferedImage buf2 = (BufferedImage) imgList.get(0);

                assertEquals(1, imgList.size());
                assertTrue(imageEquals(buf1, buf2));
            } catch (Exception ex) {
                fail(ex.toString());
            }
        }

        // Above test passes if we don't specify JPEG!
        @Tag("Presentation")
        @Test
        public void testUnspecifiedToPdf() {
            try {
                File imageFile = new File("testFiles/inputs/blue.jpeg");
                MultipartFile image = new MockMultipartFile("file", IOUtils.toByteArray(new FileInputStream(imageFile)));
                byte[] pdf = PdfUtils.imageToPdf(new MultipartFile[]{image}, "fillPage", false, "color");
                saveFile(pdf, conversionPath + "toPDF/blueUnspecified.pdf");

                // Load the pdf and ensure extracted image is the same as the original
                PDDocument generatedPDF = Loader.loadPDF(new File(conversionPath + "toPDF/blueUnspecified.pdf"));
                PDImageXObject img = PDImageXObject.createFromFile("testFiles/inputs/blue.jpeg", generatedPDF);

                PDPage page = generatedPDF.getPage(0);
                List<RenderedImage> imgList = PdfUtils.getAllImages(page.getResources());
                BufferedImage buf1 = img.getImage();
                BufferedImage buf2 = (BufferedImage) imgList.get(0);

                assertEquals(1, imgList.size());
                assertTrue(imageEquals(buf1, buf2));
            } catch (Exception ex) {
                fail(ex.toString());
            }
        }

        @Test
        // This tests maintainAspectRatio with an image wider than the page
        public void testScaleByWidth() {
            try {
                File imageFile = new File("testFiles/inputs/blue.png");
                MultipartFile image = new MockMultipartFile("file.png", "file.png", "image/png", IOUtils.toByteArray(new FileInputStream(imageFile)));
                byte[] pdf = PdfUtils.imageToPdf(new MultipartFile[]{image}, "maintainAspectRatio", false, "color");
                saveFile(pdf, conversionPath + "toPDF/scaleWidth.pdf");

                // Load the pdf and ensure extracted image has the same aspect ratio
                PDDocument generatedPDF = Loader.loadPDF(new File(conversionPath + "toPDF/scaleWidth.pdf"));
                PDImageXObject img = PDImageXObject.createFromFile("testFiles/inputs/blue.png", generatedPDF);

                PDPage page = generatedPDF.getPage(0);
                BufferedImage buf1 = img.getImage();
                BufferedImage buf2 = (BufferedImage) PdfUtils.getAllImages(page.getResources()).get(0);
                float originalRatio = (float) buf1.getWidth() / buf1.getHeight();
                float newRatio = (float) buf2.getWidth() / buf2.getHeight();

                assertEquals(originalRatio, newRatio);
            } catch (Exception ex) {
                fail(ex.toString());
            }
        }

        @Test
        // This tests maintainAspectRatio with an image narrower than the page
        public void testScaleByHeight() {
            try {
                File imageFile = new File("testFiles/inputs/bluePortrait.png");
                MultipartFile image = new MockMultipartFile("file.png", "file.png", "image/png", IOUtils.toByteArray(new FileInputStream(imageFile)));
                byte[] pdf = PdfUtils.imageToPdf(new MultipartFile[]{image}, "maintainAspectRatio", false, "color");
                saveFile(pdf, conversionPath + "toPDF/scaleHeight.pdf");

                // Load the pdf and ensure extracted image has the same aspect ratio
                PDDocument generatedPDF = Loader.loadPDF(new File(conversionPath + "toPDF/scaleHeight.pdf"));
                PDImageXObject img = PDImageXObject.createFromFile("testFiles/inputs/bluePortrait.png", generatedPDF);

                PDPage page = generatedPDF.getPage(0);
                BufferedImage buf1 = img.getImage();
                BufferedImage buf2 = (BufferedImage) PdfUtils.getAllImages(page.getResources()).get(0);
                float originalRatio = (float) buf1.getWidth() / buf1.getHeight();
                float newRatio = (float) buf2.getWidth() / buf2.getHeight();

                assertEquals(originalRatio, newRatio);
            } catch (Exception ex) {
                fail(ex.toString());
            }
        }

        @Tag("fails")
        @Test
        // This is a fault. While we don't have true documentation, this should throw an exception. Instead,
        // it ignores the input (there is an if ... else if, but not "else") and
        public void testInvalidFit() {
            assertThrows(IOException.class, () -> {
                File imageFile = new File("testFiles/inputs/bluePortrait.png");
                MultipartFile image = new MockMultipartFile("file.png", "file.png", "image/png", IOUtils.toByteArray(new FileInputStream(imageFile)));
                byte[] pdf = PdfUtils.imageToPdf(new MultipartFile[]{image}, "invalid", false, "color");
                saveFile(pdf, conversionPath + "toPDF/invalidFit.pdf");
            });
        }

        @Tag("fails")
        @Test
        // This is a fault. It should throw an IO exception, as documented, but throws NPE
        public void testInvalidInput() {
            assertThrows(IOException.class, () -> {
                File imageFile = new File("testFiles/inputs/empty.pdf");
                MultipartFile image = new MockMultipartFile("empty.pdf", IOUtils.toByteArray(new FileInputStream(imageFile)));
                byte[] pdf = PdfUtils.imageToPdf(new MultipartFile[]{image}, "fillPage", false, "color");
                saveFile(pdf, conversionPath + "toPDF/invalidInput.pdf");
            });
        }
    }

    @Nested
    class OverlayImage {
        @Test
        public void testOverlaySingleOnce() {
            try {
                // Copy pdf
                byte[] bytes = FileUtils.readFileToByteArray(new File("testFiles/inputs/imgOnly.pdf"));
                saveFile(bytes, "testFiles/outputs/overlay/overlaySingle1.pdf");

                // Get image bytes
                File imageFile = new File("testFiles/inputs/red.png");
                byte[] imageBytes = IOUtils.toByteArray(new FileInputStream(imageFile));
                byte[] copyBytes = FileUtils.readFileToByteArray(new File("testFiles/outputs/overlay/overlaySingle1.pdf"));

                // Overlay image and save for manual inspection capacity
                byte[] overlayedBytes = PdfUtils.overlayImage(copyBytes, imageBytes, 100, 650, false);
                saveFile(overlayedBytes, "testFiles/outputs/overlay/overlaySingle1.pdf");

                PDDocument doc = Loader.loadPDF(new File("testFiles/outputs/overlay/overlaySingle1.pdf"));
                // Assert image is present on first page
                PDImageXObject img = PDImageXObject.createFromFile("testFiles/inputs/red.png", doc);
                assertTrue(isOverlayedFirst(img.getImage(), doc));

            } catch (Exception ex) {
                fail (ex.toString());
            }
        }

        @Test
        // Overlay on every page for a document with only one page
        public void testOverlaySinglePageAll() {
            try {
                // Copy pdf
                byte[] bytes = FileUtils.readFileToByteArray(new File("testFiles/inputs/imgOnly.pdf"));
                saveFile(bytes, "testFiles/outputs/overlay/overlaySingle2.pdf");

                // Get image bytes
                File imageFile = new File("testFiles/inputs/red.png");
                byte[] imageBytes = IOUtils.toByteArray(new FileInputStream(imageFile));
                byte[] copyBytes = FileUtils.readFileToByteArray(new File("testFiles/outputs/overlay/overlaySingle2.pdf"));

                // Overlay image and save for manual inspection capacity
                byte[] overlayedBytes = PdfUtils.overlayImage(copyBytes, imageBytes, 100, 650, true);
                saveFile(overlayedBytes, "testFiles/outputs/overlay/overlaySingle2.pdf");

                PDDocument doc = Loader.loadPDF(new File("testFiles/outputs/overlay/overlaySingle2.pdf"));
                // Assert image is present on first page
                PDImageXObject img = PDImageXObject.createFromFile("testFiles/inputs/red.png", doc);
                assertTrue(isOverlayedAll(img.getImage(), doc));
            } catch (Exception ex) {
                fail (ex.toString());
            }
        }

        @Test
        // Overlay on every page for a document with only one page
        public void testOverlayManyAll() {
            try {
                // Copy pdf
                byte[] bytes = FileUtils.readFileToByteArray(new File("testFiles/inputs/multiPage.pdf"));
                saveFile(bytes, "testFiles/outputs/overlay/overlayMany.pdf");

                // Get image bytes
                File imageFile = new File("testFiles/inputs/red.png");
                byte[] imageBytes = IOUtils.toByteArray(new FileInputStream(imageFile));
                byte[] copyBytes = FileUtils.readFileToByteArray(new File("testFiles/outputs/overlay/overlayMany.pdf"));

                // Overlay image and save for manual inspection capacity
                byte[] overlayedBytes = PdfUtils.overlayImage(copyBytes, imageBytes, 100, 650, true);
                saveFile(overlayedBytes, "testFiles/outputs/overlay/overlayMany.pdf");

                PDDocument doc = Loader.loadPDF(new File("testFiles/outputs/overlay/overlayMany.pdf"));
                // Assert image is present on first page
                PDImageXObject img = PDImageXObject.createFromFile("testFiles/inputs/red.png", doc);
                assertTrue(isOverlayedAll(img.getImage(), doc));
            } catch (Exception ex) {
                fail (ex.toString());
            }
        }

        private boolean isOverlayedAll(BufferedImage target, PDDocument doc) throws IOException{
            for (PDPage page : doc.getPages()) {
                List<RenderedImage> images = PdfUtils.getAllImages(page.getResources());
                boolean found = false;
                for (RenderedImage img : images) {
                    if (imageEquals(target, (BufferedImage) img)) {
                        found = true;
                        break;
                    };
                }
                if (!found) return false;
            }
            return true;
        }

        private boolean isOverlayedFirst(BufferedImage target, PDDocument doc) throws IOException {
            PDPage page = doc.getPage(0);
            List<RenderedImage> images = PdfUtils.getAllImages(page.getResources());
            for (RenderedImage img : images) {
                if (imageEquals(target, (BufferedImage) img)) return true;
            }
            return false;
        }
    }
}
