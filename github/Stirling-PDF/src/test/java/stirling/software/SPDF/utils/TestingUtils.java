package stirling.software.SPDF.utils;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.mock.web.MockMultipartFile;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TestingUtils {


    public static Map<COSName, RenderedImage> extractImagesFromResources(PDResources resources) throws IOException {
        Map<COSName, RenderedImage> images = new HashMap<>();
        for (COSName name : resources.getXObjectNames()) {
            if (resources.getXObject(name) instanceof PDImageXObject) {
                PDImageXObject imgObj = (PDImageXObject) resources.getXObject(name);
                images.put(name, imgObj.getImage());
            }
        }
        return images;
    }

    public static boolean areImagesEqual(RenderedImage img1, RenderedImage img2) {
        return img1.getWidth() == img2.getWidth() && img1.getHeight() == img2.getHeight();
    }

    public static void addImageToPage(PDDocument doc, PDPage page) throws IOException {
        PDResources resources = new PDResources();
        PDImageXObject img = PDImageXObject.createFromFile("testFiles/blue.png", doc);
        resources.put(COSName.getPDFName("Img1"), img);
        page.setResources(resources);
        doc.addPage(page);
    }

    public static void addTextToPage(PDDocument doc, PDPage page, String text) throws IOException {
        PDPageContentStream contentStream = new PDPageContentStream(doc, page);
        contentStream.beginText();
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
        contentStream.newLineAtOffset(100, 700);
        contentStream.showText(text);
        contentStream.endText();
        contentStream.close();
        doc.addPage(page);
    }


    //PdfUtils
    public static PDResources createEmptyResources() {
        PDResources resources = new PDResources();
        return resources;
    }

    public static PDResources createResourcesWithTextAndGraphicsNoImage() throws IOException {
        PDDocument doc = new PDDocument();
        try {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDResources resources = new PDResources();
            PDPageContentStream contentStream = new PDPageContentStream(doc, page);

            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            contentStream.newLineAtOffset(100, 700);
            contentStream.showText("Hello, PDFBox!"); // Sample text
            contentStream.endText();

            contentStream.addRect(200, 650, 100, 50);
            contentStream.fill();

            contentStream.close();
            return resources;
        } finally {
            doc.close();
        }
    }

    public static PDResources createResourcesWithOneImage() throws IOException {
        PDDocument doc = new PDDocument();
        PDResources resources = new PDResources();
        PDImageXObject img = PDImageXObject.createFromFile("testFiles/inputs/blue.png", doc);
        resources.put(COSName.getPDFName("Img1"), img);
        return resources;
    }

    public static PDResources createResourcesWithTwoImages() throws IOException {
        PDDocument doc = new PDDocument();
        PDResources resources = new PDResources();
        PDImageXObject img = PDImageXObject.createFromFile("testFiles/inputs/blue.png", doc);
        resources.put(COSName.getPDFName("Img1"), img);
        PDImageXObject img2 = PDImageXObject.createFromFile("testFiles/inputs/blue.png", doc);
        resources.put(COSName.getPDFName("Img2"), img2);
        return resources;
    }


    public static PDDocument createEmptyDocument() throws IOException {
        PDDocument doc = new PDDocument();
        doc.addPage(new PDPage());
        return doc;
    }

    public static PDDocument createDocumentWithImagesOnFirstPage() throws IOException {
        PDDocument doc = new PDDocument();
        addImageToPage(doc, new PDPage());
        return doc;
    }

    public static PDDocument createDocumentWithImagesOnMiddlePages() throws IOException {
        PDDocument doc = new PDDocument();
        doc.addPage(new PDPage());  // First page no image
        addImageToPage(doc, new PDPage());  // Middle page image
        addImageToPage(doc, new PDPage());  // Another middle page image
        doc.addPage(new PDPage());  // Last page no image
        return doc;
    }

    public static PDDocument createDocumentWithImagesOnLastPage() throws IOException {
        PDDocument doc = new PDDocument();
        doc.addPage(new PDPage());  // First page no image
        doc.addPage(new PDPage());  // Middle page no image
        addImageToPage(doc, new PDPage());  // Last page with image
        return doc;
    }



    public static PDDocument createDocumentWithNoText() throws IOException {
        return new PDDocument();  // Empty document
    }

    public static PDDocument createDocumentWithTextOnFirstPage() throws IOException {
        PDDocument doc = new PDDocument();
        addTextToPage(doc, new PDPage(), "Sample Text");
        doc.addPage(new PDPage());  // Additional blank page
        return doc;
    }

    public static PDDocument createDocumentWithTextOnMiddlePages() throws IOException {
        PDDocument doc = new PDDocument();
        doc.addPage(new PDPage());  // First page - no text
        addTextToPage(doc, new PDPage(), "Sample Text");  // Middle page w text
        addTextToPage(doc, new PDPage(), "Sample Text");  // Another middle page w text
        return doc;
    }

    public static PDDocument createDocumentWithTextOnLastPage(int totalPages) throws IOException {
        PDDocument doc = new PDDocument();
        for (int i = 0; i < totalPages - 1; i++) {
            doc.addPage(new PDPage());  // Add blank pages
        }
        addTextToPage(doc, new PDPage(), "Sample Text");  // Last page w text
        return doc;
    }


    public static PDPage createPageWithNoResources() {
        return new PDPage();
    }

    public static PDPage createPageWithResourcesButNoImages() {
        PDPage page = new PDPage();
        PDResources resources = new PDResources();
        page.setResources(resources); // no images included
        return page;
    }

    public static PDPage createPageWithOneImage() throws IOException {
        PDDocument doc = new PDDocument();
        try {
            PDPage page = new PDPage();
            PDResources resources = new PDResources();

            PDImageXObject image = PDImageXObject.createFromFile("testFiles/inputs/blue.png", doc);
            resources.put(COSName.getPDFName("Img1"), image);

            page.setResources(resources);
            doc.addPage(page);
            return page;
        } catch (Exception e) {
            doc.close();
            throw e;
        }
    }

    public static PDPage createPageWithMultipleImages() throws IOException {
        PDDocument doc = new PDDocument();
        try {
            PDPage page = new PDPage();
            PDResources resources = new PDResources();

            PDImageXObject image1 = PDImageXObject.createFromFile("testFiles/inputs/blue.png", doc);
            PDImageXObject image2 = PDImageXObject.createFromFile("testFiles/inputs/blue.png", doc);
            resources.put(COSName.getPDFName("Img1"), image1);
            resources.put(COSName.getPDFName("Img2"), image2);

            page.setResources(resources);
            doc.addPage(page);
            return page;
        } catch (Exception e) {
            doc.close();
            throw e;
        }
    }

    public static PDPage createPageWithNoText() {
        return new PDPage();
    }

    public static PDDocument createDocumentWithPages(int numPages) throws IOException {
        PDDocument doc = new PDDocument();
        for (int i = 0; i < numPages; i++) {
            doc.addPage(new PDPage());
        }
        return doc;
    }

    public static PDDocument createDocumentWithPageSize(PDRectangle size) throws IOException {
        PDDocument doc = new PDDocument();
        PDPage page = new PDPage(size);
        doc.addPage(page);
        return doc;
    }

    public static byte[] loadPdfAsByteArray(String filePath) {
        try (PDDocument document = Loader.loadPDF(new File(filePath))) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load PDF file.", e);
        }
    }


    //GeneralUtilsTest
    public static byte[] createSamplePng() {
        return new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0x00};
    }

    public static byte[] createSampleJpg() {
        return new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00};
    }

    public static byte[] createSamplePdf() {
        return "%PDF-1.4".getBytes();
    }


}
