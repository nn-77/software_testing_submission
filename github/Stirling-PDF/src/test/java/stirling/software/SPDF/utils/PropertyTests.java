package stirling.software.SPDF.utils;

import net.jqwik.api.Property;
import net.jqwik.api.lifecycle.BeforeContainer;
import org.apache.pdfbox.rendering.ImageType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import static stirling.software.SPDF.utils.TestingUtils.areImagesEqual;

public class PropertyTests {

    static BufferedImage originalPng;
    static BufferedImage originalJpeg;
    static BufferedImage originalTif;

    @BeforeContainer
    static void loadOriginalImages() throws IOException {
        File inputPng = new File("testFiles/inputs/blue.png");
        if (!inputPng.exists()) {
            throw new IOException("File does not exist: " + inputPng.getAbsolutePath());
        }
        originalPng = ImageIO.read(inputPng);
        if (originalPng == null) {
            throw new IOException("Failed to load png: " + inputPng.getAbsolutePath());
        }


        File inputJpeg = new File("testFiles/inputs/blue.jpeg");
        if (!inputJpeg.exists()) {
            throw new IOException("File does not exist: " + inputJpeg.getAbsolutePath());
        }
        originalJpeg = ImageIO.read(inputJpeg);
        if (originalJpeg == null) {
            throw new IOException("Failed to load jpg: " + inputJpeg.getAbsolutePath());
        }


        File inputTif = new File("testFiles/inputs/marbles.tif");
        if (!inputTif.exists()) {
            throw new IOException("File does not exist: " + inputTif.getAbsolutePath());
        }
        originalTif = ImageIO.read(inputTif);
        if (originalTif == null) {
            throw new IOException("Failed to load tif: " + inputTif.getAbsolutePath());
        }
    }

    //checks property that when img1->pdf->img2 then ideally img1=img2, fails because the conversions lead to pixel loss
    @Property
    void testPngToPdfToPng() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(originalPng, "png", baos);
        byte[] imageBytes = baos.toByteArray();

        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "blue.png", "image/png", imageBytes);
        byte[] pdfBytes = PdfUtils.imageToPdf(
                new MultipartFile[]{mockFile}, "fit", false, "RGB");
        byte[] resultImageBytes = PdfUtils.convertFromPdf(
                pdfBytes, "png", ImageType.RGB, true, 300, "outputImage");
        BufferedImage resultImage = ImageIO.read(new ByteArrayInputStream(resultImageBytes));
        Assertions.assertTrue(areImagesEqual(originalPng, resultImage));
    }

    //checks property that when img1->pdf->img2 then ideally img1=img2, fails because the conversions lead to pixel loss
    @Property
    void testJpegToPdfToJpeg() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(originalJpeg, "jpeg", baos);
        byte[] imageBytes = baos.toByteArray();

        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "blue.jpeg", "image/jpeg", imageBytes);
        byte[] pdfBytes = PdfUtils.imageToPdf(
                new MultipartFile[]{mockFile}, "fit", false, "RGB");
        byte[] resultImageBytes = PdfUtils.convertFromPdf(
                pdfBytes, "jpeg", ImageType.RGB, true, 300, "outputImage");
        BufferedImage resultImage = ImageIO.read(new ByteArrayInputStream(resultImageBytes));
        Assertions.assertTrue(areImagesEqual(originalJpeg, resultImage));
    }

    //checks property that when img1->pdf->img2 then ideally img1=img2, fails because the conversions lead to pixel loss
    @Property
    void testTifToPdfToTif() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(originalTif, "tif", baos);
        byte[] imageBytes = baos.toByteArray();

        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "marbles.tif", "image/tif", imageBytes);
        byte[] pdfBytes = PdfUtils.imageToPdf(
                new MultipartFile[]{mockFile}, "fillPage", false, "RGB");
        byte[] resultImageBytes = PdfUtils.convertFromPdf(
                pdfBytes, "tif", ImageType.RGB, true, 300, "outputImage");
        BufferedImage resultImage = ImageIO.read(new ByteArrayInputStream(resultImageBytes));
        Assertions.assertTrue(areImagesEqual(originalTif, resultImage));
    }
}

