package stirling.software.SPDF.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static stirling.software.SPDF.utils.TestingUtils.*;

import org.junit.jupiter.api.AfterEach;
import java.nio.file.*;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;

@ExtendWith(MockitoExtension.class)
public class GeneralUtilsTest {

    @Nested
    class FilesAndDirs {

        private static Stream<MultipartFile> fileProvider() {
            // Provides different scenarios of MultipartFile
            return Stream.of(
                    new MockMultipartFile("file", "filename.txt", "text/plain", "Hello World".getBytes()),
                    new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]),
                    new MockMultipartFile("file", "binary.bin", "application/octet-stream", new byte[]{0, 1, 2, 3, 4, 5}),
                    new MockMultipartFile("file", "image.png", "image/png", createSamplePng()),
                    new MockMultipartFile("file", "image.jpg", "image/jpeg", createSampleJpg()),
                    new MockMultipartFile("file", "document.pdf", "application/pdf", createSamplePdf())
            );
        }
        @ParameterizedTest
        @MethodSource("fileProvider")
        void testConvertMultipartFileToFile(MultipartFile multipartFile) throws IOException {

            File resultFile = GeneralUtils.convertMultipartFileToFile(multipartFile);
            assertNotNull(resultFile);
            assertTrue(resultFile.exists());
            assertEquals(multipartFile.getSize(), resultFile.length());

            byte[] fileContent = new byte[(int) resultFile.length()];
            try (FileInputStream fis = new FileInputStream(resultFile)) {
                fis.read(fileContent);
            }
            assertArrayEquals(multipartFile.getBytes(), fileContent);
            resultFile.delete();
        }


        Path tempDir;
        @BeforeEach
        void setup() throws IOException {
            tempDir = Files.createTempDirectory("testDir");
            // Set up directories and files based on the scenario
            Files.createDirectories(tempDir.resolve("empty_dir"));
            Files.createDirectories(tempDir.resolve("with_files"));
            Files.createFile(tempDir.resolve("with_files/file1.txt"));
            Files.createDirectories(tempDir.resolve("nested_directories/sub_dir"));
            Files.createFile(tempDir.resolve("nested_directories/sub_dir/file2.txt"));
            Path readOnlyFile = tempDir.resolve("read_only_files/file3.txt");
            Files.createDirectories(readOnlyFile.getParent());
            Files.createFile(readOnlyFile);
            readOnlyFile.toFile().setReadOnly();

            try {
                // Attempt to create a symbolic link
                Path linkTarget = tempDir.resolve("read_only_files/file3.txt");
                Path link = tempDir.resolve("linked_files/sym_link.txt");
                Files.createDirectories(link.getParent());
                Files.createSymbolicLink(link, linkTarget);
            } catch (UnsupportedOperationException | IOException e) {
                System.err.println("Error with symbolic linkss: " + e.getMessage());
            }
        }

        @AfterEach
        void tearDown() throws IOException {
            Files.walkFileTree(tempDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file); // Delete files and symbolic links
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (exc != null) {
                        throw exc;
                    }
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        //equivalence classes and possible fail cases
        // An empty directory
        // Directory with some files
        // Nested directories with files
        // Directory containing read-only files
        // Directory with symbolic links
        private static Stream<Path> provideTestDirsDelete() {
            return Stream.of(
                    Paths.get("empty_dir"),
                    Paths.get("with_files"),
                    Paths.get("nested_directories"),
                    Paths.get("read_only_files"),
                    Paths.get("linked_files")
            );
        }
        @ParameterizedTest
        @MethodSource("provideTestDirsDelete")
        void testDeleteDirectory(Path directoryName) throws IOException {
            Path testPath = tempDir.resolve(directoryName);
            GeneralUtils.deleteDirectory(testPath);
            assertFalse(Files.exists(testPath), "Directory should be deleted: " + testPath);
        }


        //equivalence classes
        //directory that exists
        //directory that doesn't exist
        private static Stream<String> provideTestDirsCreate() {
            return Stream.of(
                    "empty_dir",
                    "new_dir"
            );
        }
        @ParameterizedTest
        @MethodSource("provideTestDirsCreate")
        void testCreateDir(String directoryName) throws IOException {
            Path testPath = tempDir.resolve(Paths.get(directoryName));
            GeneralUtils.createDir(testPath.toString());
            assertTrue(Files.exists(testPath), "Directory should exist: " + directoryName);
            Files.deleteIfExists(testPath);
        }


        public static Stream<Arguments> provideFileNameScenarios() {
            return Stream.of(
                    Arguments.of("filename", "filename"),
                    Arguments.of("file@name", "file_name"),
                    Arguments.of("file name with spaces", "file_name_with_spaces"),
                    Arguments.of("123456789012345678901234567890123456789012345678901234567890", "12345678901234567890123456789012345678901234567890"),
                    Arguments.of("file-name-with-dashes", "file_name_with_dashes"),
                    Arguments.of("", ""),
                    Arguments.of("UPPER_and_lower_12345", "UPPER_and_lower_12345"),
                    Arguments.of("文件名∫π", "_____"),
                    Arguments.of("file.name.ext", "file_name_ext"),
                    Arguments.of(null, "_")
            );
        }

        @Tag("fails") //throws npe when string null
        @ParameterizedTest
        @MethodSource("provideFileNameScenarios")
        void testConvertToFileName(String input, String expected) {
            assertEquals(expected, GeneralUtils.convertToFileName(input));
        }



        public static Stream<Arguments> provideUrlScenarios() {
            return Stream.of(
                    Arguments.of("https://example.com:80/path", true),
                    Arguments.of(null, false),
                    Arguments.of("", false),
                    Arguments.of("http://www.example.com", true),
                    Arguments.of("https://www.example.com", true),

                    Arguments.of("ftp://www.example.com", false),             // Invalid protocol
                    Arguments.of("http://www..com", false) ,                   // URL with misplaced dots
                    Arguments.of("http://example.-com", false) ,               // Invalid domain (dash)
                    Arguments.of("http://example.com/a b", false)              // Space in URL path
            );
        }

        @Tag("fails") //thinks urls with space in them, urls w invalid domain, with two dots, with ivalid protocola(ie not https) are valid,
        @ParameterizedTest
        @MethodSource("provideUrlScenarios")
        void testIsValidURL(String url, boolean expected) {
            assertEquals(expected, GeneralUtils.isValidURL(url));
        }



        private static MultipartFile mockMultipartFile(byte[] content) {
            MultipartFile multipartFile = Mockito.mock(MultipartFile.class);
            try {
                given(multipartFile.getInputStream()).willReturn(new ByteArrayInputStream(content));
                given(multipartFile.getSize()).willReturn((long) content.length);
            } catch (IOException e) {
                fail("error setting up mock");
            }
            return multipartFile;
        }

        public static Stream<MultipartFile> provideMultipartFiles() {
            return Stream.of(
                    mockMultipartFile(new byte[0]),                      // Empty file
                    mockMultipartFile("Simple text content".getBytes()), // Simple text file
                    mockMultipartFile(new byte[1024]),                   // 1 KB size file
                    mockMultipartFile(new byte[10240]),                  // 10 KB size file
                    mockMultipartFile(new byte[102400]),                 // 100 KB size file
                    mockMultipartFile(new byte[1024 * 1024])       // Larger file (e.g., 1 MB)
            );
        }
        @ParameterizedTest
        @MethodSource("provideMultipartFiles")
        void testMultipartToFile(MultipartFile multipartFile) throws IOException {
            File convertedFile = GeneralUtils.multipartToFile(multipartFile);

            assertNotNull(convertedFile, "Converted file should not be null.");
            assertTrue(convertedFile.exists(), "Converted file should exist.");
            assertEquals(multipartFile.getSize(), convertedFile.length(), "File size doesnt match input size");
            assertTrue(convertedFile.delete(), "Temp file not deleted.");
        }


        public static Stream<Arguments> provideSizeScenarios() {
            return Stream.of(
                    Arguments.of("1KB", 1024L),                          // Kilobytes
                    Arguments.of("5 mb", 5242880L),                      // Megabytes, lower case
                    Arguments.of("2 GB ", 2147483648L),                  // Gigabytes, extra space
                    Arguments.of("1024", 1073741824L),                   // Default to MB
                    Arguments.of("not a number KB", null),               // Invalid number
                    Arguments.of("100 XB", null),                        // Invalid unit
                    Arguments.of("", null),                              // Empty string
                    Arguments.of(null, null),                            // Null input
                    Arguments.of("1.5GB", 1610612736L),                  // Gigabytes with decimal
                            // Below added to enhance code coverage
                    Arguments.of("2B", 2L)                               // Bytes (added for coverage)
            );
        }
        @ParameterizedTest
        @MethodSource("provideSizeScenarios")
        void testConvertSizeToBytes(String input, Long expected) {
            assertEquals(expected, GeneralUtils.convertSizeToBytes(input));
        }
    }



    @Nested
    class Pages{

        public static Stream<Arguments> providePageListScenarios() {
            return Stream.of(
                    Arguments.of("1,2,3", 5, true, List.of(1, 2, 3)),        // Normal case, one-based
                    Arguments.of("0,1,2", 5, false, List.of(0, 1, 2)),       // Normal case, zero-based
                    Arguments.of("3,4,5,6", 5, true, List.of(3, 4, 5)),      // Pages exceed total pages, one-based
                    Arguments.of("6,7,8", 5, true, List.of()),               // All pages exceed total pages
                    Arguments.of(null, 5, true, List.of(1)),                 // Null input
                    Arguments.of("1,2,3", 3, true, List.of(1, 2, 3)),        // Exact total pages
                    Arguments.of("2, 3, 4", 5, true, List.of(2, 3, 4)),      // Spaces in input
                    Arguments.of("3", 0, true, List.of()),                   // No total pages available
                    Arguments.of("five,six", 10, true, List.of(1)),           // Non-numeric input
                            // Below added to enhance code coverage

                    Arguments.of("1,2n", 5, true, List.of(1,2,4))           // coverage
//                    Arguments.of("1,-2n", 5, true, List.of(1,2,4))           // coverage
            );
        }
        @Tag("fails") //when "0,1,2" it should return [0,1,2] but returns [0,1], also just like it does with null, if input is invalid it should default to just 1st page
        @ParameterizedTest
        @MethodSource("providePageListScenarios")
        void testParsePageList(String input, int totalPages, boolean oneBased, List<Integer> expected) {
            assertEquals(expected, GeneralUtils.parsePageList(input, totalPages, oneBased));
        }



        public static Stream<Arguments> providePageListArrayScenarios() {
            return Stream.of(
                    Arguments.of(new String[] {"0", "1", "2"}, 5, List.of(0, 1, 2)),
                    Arguments.of(new String[] {"3", "4", "5"}, 5, List.of(3, 4)),
                    Arguments.of(new String[] {"5", "6", "7"}, 5, List.of()),
                    Arguments.of(null, 5, List.of()),
                    Arguments.of(new String[] {"0", "1", "2"}, 0, List.of()),
                    Arguments.of(new String[] {"0", "1", "2"}, 3, List.of(0, 1, 2)),
                    Arguments.of(new String[] {"0"}, 5, List.of(0)),
                    Arguments.of(new String[] {"a", "b"}, 5, List.of())
            );
        }

        @Tag("fails")//similar to previous test's fails
        @ParameterizedTest
        @MethodSource("providePageListArrayScenarios")
        void testParsePageListArray(String[] input, int totalPages, List<Integer> expected) {
            assertEquals(expected, GeneralUtils.parsePageList(input, totalPages));
        }


        public static Stream<Arguments> providePageListComplexScenarios() {
            return Stream.of(
                    Arguments.of(new String[] {"1", "2", "3"}, 5, true, List.of(1, 2, 3)),             // Simple one-based index
                    Arguments.of(new String[] {"0", "1", "2"}, 5, false, List.of(0, 1, 2)),           // Simple zero-based index
                    Arguments.of(new String[] {"all"}, 5, true, List.of(1, 2, 3, 4, 5)),              // "All" keyword, one-based
                    Arguments.of(new String[] {"all"}, 5, false, List.of(0, 1, 2, 3, 4)),             // "All" keyword, zero-based
                    Arguments.of(new String[] {"1,2,3"}, 5, true, List.of(1, 2, 3)),                  // Comma-separated one-based
                    Arguments.of(new String[] {"0-2", "4"}, 5, false, List.of(0, 1, 2, 4)),           // Range and single, zero-based
                    Arguments.of(new String[] {"1-3", "5"}, 5, true, List.of(1, 2, 3, 5)),            // Range and single, one-based
                    Arguments.of(new String[] {"3-1"}, 5, true, List.of()),                           // Invalid range, one-based
                    Arguments.of(new String[] {"3", "3"}, 5, true, List.of(3)),                       // Duplicates, one-based
                    Arguments.of(new String[] {"-1", "6"}, 5, true, List.of()),                       // Out of bounds, one-based
                    Arguments.of(new String[] {"not a number"}, 5, true, List.of()),                  // Non-numeric input
                    Arguments.of(new String[] {}, 5, true, List.of()),                                // Empty array
                    Arguments.of(new String[] {null}, 5, true, List.of())                             // Null entry in array
            );
        }

        @Tag("fails")// fails with cases like  Arguments.of(new String[] {"0-2", "4"}, 5, false, List.of(0, 1, 2, 4)), throws IllegalArgumentExceptionw when negative nums, throws npe when null
        @ParameterizedTest
        @MethodSource("providePageListComplexScenarios")
        void testParsePageListComplex(String[] input, int totalPages, boolean oneBased, List<Integer> expected) {
            assertEquals(expected, GeneralUtils.parsePageList(input, totalPages, oneBased));
        }
    }
}
