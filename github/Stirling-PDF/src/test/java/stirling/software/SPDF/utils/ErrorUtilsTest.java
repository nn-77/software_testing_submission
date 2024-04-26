package stirling.software.SPDF.utils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.ModelAndView;


import java.io.IOException;
import java.util.stream.Stream;

import stirling.software.SPDF.utils.ErrorUtils;

import static org.junit.jupiter.api.Assertions.*;

public class ErrorUtilsTest {

    public static Stream<Exception> provideExceptions() {
        return Stream.of(
                new Exception("Test exception"),
                new NullPointerException("Null pointer exception occurred"),
                new IllegalArgumentException("Illegal argument"),
                new RuntimeException("Runtime exception"),
                new IOException("Input/output exception with a cause", new RuntimeException("Caused by runtime exception"))
        );
    }


    @ParameterizedTest
    @MethodSource("provideExceptions")
    void testExceptionToModel(Exception exception) {
        Model model = new ExtendedModelMap();
        Model returnedModel = ErrorUtils.exceptionToModel(model, exception);

        assertEquals(model, returnedModel, "The returned model should be the same as the input model.");
        assertEquals(exception.getMessage(), model.asMap().get("errorMessage"), "Error message attribute does not match.");
        assertNotNull(model.asMap().get("stackTrace"), "Stack trace should not be null.");
        assertTrue(((String) model.asMap().get("stackTrace")).contains(exception.getClass().getName()), "Stack trace should contain exception class name.");
    }

    @ParameterizedTest
    @MethodSource("provideExceptions")
    void testExceptionToModelView(Exception exception) {
        Model model = new ExtendedModelMap(); // Using ExtendedModelMap for consistency
        ModelAndView modelAndView = ErrorUtils.exceptionToModelView(model, exception);

        assertEquals(exception.getMessage(), modelAndView.getModel().get("errorMessage"), "Error message in ModelAndView does not match.");
        assertNotNull(modelAndView.getModel().get("stackTrace"), "Stack trace in ModelAndView should not be null.");
        assertTrue(((String) modelAndView.getModel().get("stackTrace")).contains(exception.getClass().getName()), "Stack trace in ModelAndView should contain exception class name.");
    }
}
