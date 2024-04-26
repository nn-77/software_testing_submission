package stirling.software.SPDF.utils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.awt.image.BufferedImage;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ImageProcessingUtilsTest {

    public static Stream<Arguments> provideImagesForColorConversion() {
        BufferedImage sampleImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        BufferedImage sampleImage2 = new BufferedImage(30, 30, BufferedImage.TYPE_BYTE_GRAY);
        BufferedImage sampleImage3 = new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_BINARY);
        return Stream.of(
                Arguments.of(sampleImage, "greyscale", BufferedImage.TYPE_BYTE_GRAY),
                Arguments.of(sampleImage, "blackwhite", BufferedImage.TYPE_BYTE_BINARY),
                Arguments.of(sampleImage, "fullcolor", BufferedImage.TYPE_INT_RGB),
                Arguments.of(sampleImage2, "greyscale", BufferedImage.TYPE_BYTE_GRAY),
                Arguments.of(sampleImage2, "blackwhite", BufferedImage.TYPE_BYTE_BINARY),
                Arguments.of(sampleImage2, "fullcolor", BufferedImage.TYPE_BYTE_GRAY),
                Arguments.of(sampleImage3, "greyscale", BufferedImage.TYPE_BYTE_GRAY),
                Arguments.of(sampleImage3, "blackwhite", BufferedImage.TYPE_BYTE_BINARY),
                Arguments.of(sampleImage3, "fullcolor", BufferedImage.TYPE_BYTE_BINARY)
        );
    }

    @ParameterizedTest
    @MethodSource("provideImagesForColorConversion")
    void testConvertColorType(BufferedImage sourceImage, String colorType, int expectedType) {
        BufferedImage convertedImage = ImageProcessingUtils.convertColorType(sourceImage, colorType);
        assertNotNull(convertedImage, "Result shouldn't be null");
        assertEquals(expectedType, convertedImage.getType(), "");
    }
}
