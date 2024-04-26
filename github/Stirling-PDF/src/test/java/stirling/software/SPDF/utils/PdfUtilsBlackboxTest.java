package stirling.software.SPDF.utils;

import net.jqwik.api.Example;
import net.jqwik.api.Group;
import net.jqwik.api.Property;
import net.jqwik.api.lifecycle.BeforeContainer;
import net.jqwik.api.lifecycle.BeforeTry;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;
import static stirling.software.SPDF.utils.TestingUtils.*;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import java.io.IOException;
import java.util.stream.StreamSupport;

public class PdfUtilsBlackboxTest {
    private static PDDocument txtOnly;

    private static byte[] imageBytes;

    @BeforeAll
    public static void setup() {
        try {

            txtOnly = Loader.loadPDF(new File("testFiles/inputs/txtOnly.pdf"));

            try (PDDocument doc = new PDDocument()) {
                PDImageXObject image1 = PDImageXObject.createFromFile("testFiles/inputs/blue.png", doc);
                BufferedImage bufferedImage = image1.getImage();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "png", baos);
                imageBytes = baos.toByteArray();
            }
        } catch (IOException ex) {
            fail("Could not complete test setup");
        }
    }

    @Nested
    class Size {
        //using all PDF sizes specified in the PDRectangle library
        private static Stream<Arguments> getTextAndRectangle() {
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

        private static Stream<Arguments> invalidSizes() {
            return Stream.of(
                    Arguments.of("random"),
                    Arguments.of("25")
            );
        }
        @ParameterizedTest
        @MethodSource("getTextAndRectangle")
        void testTextToPageSizeSuccess(String size, PDRectangle rect) {
            assertEquals(rect, PdfUtils.textToPageSize(size));
        }

        @ParameterizedTest
        @MethodSource("invalidSizes")
        void testTextToPageSizeThrows(String size) {
            assertThrows(IllegalArgumentException.class, () -> PdfUtils.textToPageSize(size));
        }
    }


    @Nested
    class Image {

        //equivalence classes
        // empty pdf file
        // nonempty pdf file with content but no Image
        // file with only 1 image
        // file with more than 1 image
        static Stream<Arguments> resourceProvider() throws IOException {
            return Stream.of(
                    Arguments.of(createEmptyResources(), 0),
                    Arguments.of(createResourcesWithTextAndGraphicsNoImage(), 0),
                    Arguments.of(createResourcesWithOneImage(), 1),
                    Arguments.of(createResourcesWithTwoImages(), 2)
            );
        }
        @ParameterizedTest
        @MethodSource(value = "resourceProvider")
        void testGetAllImages(PDResources resources, int numImages) {
            try {
                List<RenderedImage> images = PdfUtils.getAllImages(resources);
                Assertions.assertEquals(numImages, images.size(), "Number of images not as expected");

                Map<COSName, RenderedImage> originalImages = extractImagesFromResources(resources);
                for (RenderedImage original : originalImages.values()) {
                    boolean found = images.stream().anyMatch(image -> areImagesEqual(image, original));
                    Assertions.assertTrue(found, "An expected image not found in result");
                }
            } catch (IOException ex) {
                Assertions.fail("Exception should not be thrown: " + ex.toString());
            }
        }


        //equivalence classes for pdfs
        // pdf with no images
        // pdf with image(s) only on 1st page
        // pdf(with more than 1 page) with image(s) on page(s) other than 1st and last
        // pdf(with more than 1 page) with image(s) only on last page
        public static Stream<Arguments> documentProvider() throws IOException {
            return Stream.of(
                    Arguments.of(createEmptyDocument(), "1", false),  // PDF with no images
                    Arguments.of(createDocumentWithImagesOnFirstPage(), "1", true),  // PDF with images only on the 1st page
                    Arguments.of(createDocumentWithImagesOnMiddlePages(), "2,3", true),  // PDF with images on pages other than the 1st and last
                    Arguments.of(createDocumentWithImagesOnMiddlePages(), "0", false),
                    Arguments.of(createDocumentWithImagesOnLastPage(), "3", true),  // PDF with images only on the last page
                   Arguments.of(createDocumentWithImagesOnLastPage(), "0,1", false)  // PDF with images only on the last page
            );
        }
        @Tag("fails") //when the resources are null, ie when asked to check on page that has no resources ie for 4th and 6th stream arg
        @ParameterizedTest
        @MethodSource("documentProvider")
        void testHasImages(PDDocument document, String pagesToCheck, boolean expected) throws IOException {
            try {
                boolean hasImages = PdfUtils.hasImages(document, pagesToCheck);
                Assertions.assertEquals(expected, hasImages, "Expected images were not found");
            } finally {
                if (document != null) {
                    document.close();
                }
            }
        }


        //equivalenceClasses
        // empty page
        // not empty page with no images
        // page with one image
        // page with more than one image
        public static Stream<Arguments> pageImageProvider() throws IOException {
            // Returns PDPage instances directly
            return Stream.of(
                    Arguments.of(createPageWithNoResources(), false),
                    Arguments.of(createPageWithResourcesButNoImages(), false),
                    Arguments.of(createPageWithOneImage(), true),
                    Arguments.of(createPageWithMultipleImages(), true)
            );
        }

        @Tag("fails") //for 1st input because resources are null in a blank page
        @ParameterizedTest
        @MethodSource("pageImageProvider")
        void testHasImagesOnPage(PDPage page, boolean expected) {
            try {
                boolean hasImages = PdfUtils.hasImagesOnPage(page);
                Assertions.assertEquals(expected, hasImages, "Expected images were not found");
            } catch (IOException ex) {
                Assertions.fail("Exception should not be thrown: " + ex.toString());
            }
        }
   }

   @Nested
   class Text {

       //equivalence classes for pdfs
       // pdf with no text
       // pdf with text only on 1st page
       // pdf(with more than 1 page) with text on page(s) other than 1st and last
       // pdf(with more than 1 page) with text only on last page
       public static Stream<Arguments> textDocumentProvider() throws IOException {
           return Stream.of(
                   Arguments.of(createDocumentWithNoText(), "1", "Sample Text", false),
                   Arguments.of(createDocumentWithTextOnFirstPage(), "1", "Sample Text", true),
                   Arguments.of(createDocumentWithTextOnMiddlePages(), "2,3", "Sample Text", true),
                   Arguments.of(createDocumentWithTextOnLastPage(3), "3", "Sample Text", true),
                   Arguments.of(createDocumentWithTextOnLastPage(3), "1,2", "Sample Text", false)
           );
       }
       @ParameterizedTest
       @MethodSource("textDocumentProvider")
       void testHasText(PDDocument document, String pagesToCheck, String phrase, boolean expected) throws IOException {
           try {
               boolean hasText = PdfUtils.hasText(document, pagesToCheck, phrase);
               Assertions.assertEquals(expected, hasText, "Expected text not found");
           } finally {
               if (document != null) {
                   document.close();
               }
           }
       }


       //equivalenceClasses
       // pdf with no text
       // pdf with text
       public static Stream<Arguments> pageTextProvider() throws IOException {
           return Stream.of(
                   Arguments.of(createPageWithNoText(), "Sample Text", false),
                   Arguments.of(txtOnly.getPage(0), "test", false),
                   Arguments.of(txtOnly.getPage(0), "te", true),
                   Arguments.of(txtOnly.getPage(0), "text", true)
           );
       }
       @ParameterizedTest
       @MethodSource("pageTextProvider")
       void testHasTextOnPage(PDPage page, String phrase, boolean expected) throws IOException {

           boolean hasText = PdfUtils.hasTextOnPage(page, phrase);
           Assertions.assertEquals(expected, hasText, "Expected text not found");
       }


       public static Stream<Arguments> textDocumentProvider2() throws IOException {
           return Stream.of(
                   Arguments.of(createDocumentWithNoText(), "Sample Text", "1", false),
                   Arguments.of(createDocumentWithTextOnFirstPage(), "Sample Text", "1", true),
                   Arguments.of(createDocumentWithTextOnMiddlePages(), "Sample Text", "2-3", true),
                   Arguments.of(createDocumentWithTextOnLastPage(4), "Sample Text", "4", true),
                   Arguments.of(createDocumentWithTextOnFirstPage(), "Sample Text", "all", true),
                   Arguments.of(createDocumentWithNoText(), "Sample Text", null, false)
           );
       }

       @ParameterizedTest
       @MethodSource("textDocumentProvider2")
       void testContainsTextInFile(PDDocument document, String text, String pagesToCheck, boolean expected) throws IOException {
           try {
               boolean result = PdfUtils.containsTextInFile(document, text, pagesToCheck);
               Assertions.assertEquals(expected, result, "Text detection mismatch.");
           } finally {
               document.close(); // doc closed after test
           }
       }
   }

   @Nested
    class Page{
       public static Stream<Arguments> pageCountProvider() throws IOException {
           return Stream.of(
                   // Document with no pages
                   Arguments.of(createDocumentWithPages(0), 0, "equal", true),
                   Arguments.of(createDocumentWithPages(0), 0, "greater", false),
                   Arguments.of(createDocumentWithPages(0), 0, "less", false),
                   // Document with one page
                   Arguments.of(createDocumentWithPages(1), 1, "equal", true),
                   Arguments.of(createDocumentWithPages(1), 0, "greater", true),
                   Arguments.of(createDocumentWithPages(1), 2, "less", true),
                   // Document with more than one page (e.g., 3 pages)
                   Arguments.of(createDocumentWithPages(3), 3, "equal", true),
                   Arguments.of(createDocumentWithPages(3), 2, "greater", true),
                   Arguments.of(createDocumentWithPages(3), 4, "less", true)
           );
       }
       @ParameterizedTest
       @MethodSource("pageCountProvider")
       void testPageCount(PDDocument document, int pageCount, String comparator, boolean expected) throws IOException {
           try {
               boolean result = PdfUtils.pageCount(document, pageCount, comparator);
               Assertions.assertEquals(expected, result, "Page count returned not as expected");
           } finally {
               if (document != null) {
                   document.close();
               }
           }
       }

       private static String formatSize(PDRectangle size) {
           return String.format("%.0fx%.0f", size.getWidth(), size.getHeight());
       }
       public static Stream<Arguments> pageSizeProvider() throws IOException {
           return Stream.of(
                   Arguments.of(createDocumentWithPageSize(PDRectangle.A0), formatSize(PDRectangle.A0), true),
                   Arguments.of(createDocumentWithPageSize(PDRectangle.A1), formatSize(PDRectangle.A1), true),
                   Arguments.of(createDocumentWithPageSize(PDRectangle.A2), formatSize(PDRectangle.A2), true),
                   Arguments.of(createDocumentWithPageSize(PDRectangle.A3), formatSize(PDRectangle.A3), true),
                   Arguments.of(createDocumentWithPageSize(PDRectangle.A4), formatSize(PDRectangle.A4), true),
                   Arguments.of(createDocumentWithPageSize(PDRectangle.A5), formatSize(PDRectangle.A5), true),
                   Arguments.of(createDocumentWithPageSize(PDRectangle.A6), formatSize(PDRectangle.A6), true),
                   Arguments.of(createDocumentWithPageSize(PDRectangle.LETTER), formatSize(PDRectangle.LETTER), true),
                   Arguments.of(createDocumentWithPageSize(PDRectangle.LEGAL), formatSize(PDRectangle.LEGAL), true)
           );
       }
       @Tag("fails") //for all sizes A*, difference between exp and actual is always <1.0 for example exp width 2384.0 and actual width 2383.937
       @ParameterizedTest
       @MethodSource("pageSizeProvider")
       void testPageSize(PDDocument document, String expectedPageSize, boolean expected) throws IOException {
           try {
               boolean matches = PdfUtils.pageSize(document, expectedPageSize);
               Assertions.assertEquals(expected, matches, "Page size not as expected");
           } finally {
               if (document != null) {
                   document.close();
               }
           }
       }
   }

   @Nested
    class Conversions {
       public static Stream<Arguments> pdfConversionProvider() throws IOException {
           byte[] txtAndImgPdf = loadPdfAsByteArray("testFiles/inputs/txtAndImg.pdf");
           byte[] imgOnlyPdf = loadPdfAsByteArray("testFiles/inputs/imgOnly.pdf");
           byte[] multiPagePdf = loadPdfAsByteArray("testFiles/inputs/multiPage.pdf");
           byte[] emptyPdf = loadPdfAsByteArray("testFiles/inputs/empty.pdf");
           byte[] txtPdf = loadPdfAsByteArray("testFiles/inputs/empty.pdf");

           return Stream.of(
                   Arguments.of(txtAndImgPdf, "tiff", ImageType.RGB, true, 300, "singlePageTiff"),
                   Arguments.of(txtAndImgPdf, "tif", ImageType.RGB, true, 300, "singlePageTif"),
                   Arguments.of(txtAndImgPdf, "png", ImageType.RGB, true, 300, "singlePagePng"),
                   Arguments.of(txtAndImgPdf, "jpg", ImageType.RGB, true, 300, "singlePageJpg"),

                   Arguments.of(multiPagePdf, "tiff", ImageType.RGB, false, 300, "multiPageTiff"),
                   Arguments.of(multiPagePdf, "tif", ImageType.RGB, false, 300, "multiPageTif"),
                   Arguments.of(multiPagePdf, "png", ImageType.RGB, false, 300, "multiPagePng"),
                   Arguments.of(multiPagePdf, "jpg", ImageType.RGB, false, 300, "multiPageJpg"),

                   Arguments.of(txtAndImgPdf, "tiff", ImageType.GRAY, true, 300, "singlePageTiffGray"),
                   Arguments.of(txtAndImgPdf, "tiff", ImageType.BINARY, true, 300, "singlePageTiffBinary"),
                   Arguments.of(multiPagePdf, "tiff", ImageType.GRAY, false, 300, "multiPageTiffGray"),
                   Arguments.of(multiPagePdf, "tiff", ImageType.BINARY, false, 300, "multiPageTiffBinary"),
                   Arguments.of(imgOnlyPdf, "png", ImageType.GRAY, true, 300, "singlePagePngGray"),
                   Arguments.of(imgOnlyPdf, "png", ImageType.BINARY, true, 300, "singlePagePngBinary"),
                   Arguments.of(multiPagePdf, "png", ImageType.GRAY, false, 300, "multiPagePngGray"),
                   Arguments.of(multiPagePdf, "png", ImageType.BINARY, false, 300, "multiPagePngBinary"),
                   Arguments.of(imgOnlyPdf, "jpg", ImageType.GRAY, true, 300, "singlePageJpgGray"),
                   Arguments.of(imgOnlyPdf, "jpg", ImageType.BINARY, true, 300, "singlePageJpgBinary"),
                   Arguments.of(multiPagePdf, "jpg", ImageType.GRAY, false, 300, "multiPageJpgGray"),
                   Arguments.of(multiPagePdf, "jpg", ImageType.BINARY, false, 300, "multiPageJpgBinary"),

                   // Add more combinations for other PDFs and image types
                   Arguments.of(emptyPdf, "tiff", ImageType.RGB, true, 300, "emptyPageTiff"),
                   Arguments.of(emptyPdf, "png", ImageType.GRAY, true, 150, "emptyPagePng"),
                   Arguments.of(txtPdf, "tiff", ImageType.GRAY, true, 150, "txtPageTiff"),
                   Arguments.of(txtPdf, "png", ImageType.GRAY, true, 150, "txtPagePng")
           );
       }

       //moe robust testing for this was done manually, also in the whitebox tests and in the property tests, because it's hard to compare pdf and img equality with assertions
       @ParameterizedTest
       @MethodSource("pdfConversionProvider")
       void testConvertFromPdf(byte[] pdfData, String imageType, ImageType colorType, boolean singleImage, int DPI, String filename) throws IOException, Exception {
           byte[] result = PdfUtils.convertFromPdf(pdfData, imageType, colorType, singleImage, DPI, filename);
           Assertions.assertNotNull(result, "Conversion should produce some output.");
           Assertions.assertTrue(result.length > 0, "Output should not be empty.");
       }


       public static Stream<Arguments> provideImageToPdfArguments() throws IOException {
           MockMultipartFile mockFileSingle = new MockMultipartFile("blue.png", "blue.png", "image/png", imageBytes);

           return Stream.of(
                   Arguments.of(new MockMultipartFile[]{mockFileSingle}, "Fit", true, "RGB"),
                   Arguments.of(new MockMultipartFile[]{mockFileSingle}, "Fit", false, "Grayscale"),
                   Arguments.of(new MockMultipartFile[]{mockFileSingle}, "Fill", true, "CMYK"),
                   Arguments.of(new MockMultipartFile[]{mockFileSingle}, "MaintainAspectRatio", false, "RGB"),
                   Arguments.of(new MockMultipartFile[]{mockFileSingle, mockFileSingle}, "Fit", true, "RGB"),
                   Arguments.of(new MockMultipartFile[]{mockFileSingle, mockFileSingle}, "Fill", false, "RGB"),
                   Arguments.of(new MockMultipartFile[]{}, "Fit", true, "RGB")  // Case with no files at all
           );
       }

       @Tag("fails") //fails when turning jpeg to pdf, conversion is lossy
       @ParameterizedTest
       @MethodSource("provideImageToPdfArguments")
       void testImageToPdfConversion(MultipartFile[] files, String fitOption, boolean autoRotate, String colorType) throws IOException {
           byte[] actualOutput = PdfUtils.imageToPdf(files, fitOption, autoRotate, colorType);
           Assertions.assertNotNull(actualOutput, "The output should not be null.");

           try (PDDocument doc = Loader.loadPDF(actualOutput)) {

               int expectedPageCount = files.length;
               Assertions.assertEquals(expectedPageCount, doc.getNumberOfPages(), "Document does not have the expected number of pages.");

               for (int i = 0; i < doc.getNumberOfPages()-1; i++) {
                   PDPage page = doc.getPage(i);
                   boolean foundImage = StreamSupport.stream(page.getResources().getXObjectNames().spliterator(), false)
                           .map(cosName -> {
                               try {
                                   return page.getResources().getXObject(cosName);
                               } catch (IOException e) {
                                   throw new RuntimeException("Error accessing page resources.", e);
                               }
                           })
                           .anyMatch(xObject -> xObject instanceof PDImageXObject);
                   Assertions.assertTrue(foundImage, "No image found on page " + (i + 1));
               }
           }
       }
    }
}
