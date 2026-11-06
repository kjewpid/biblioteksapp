package se.yrgo.libraryapp.validators;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.fail;

public class UtilsTest {
    @Test
    void removeNonLettersTest() {
        String nonLetters = "!#$%^&*()+=?/\\<>[]{}€1";

        for (char nonLetter : nonLetters.toCharArray()) {
            String testStr = "test" + nonLetter + "str";
            String expected = "teststr";
            assertThat(Utils.onlyLettersAndWhitespace(testStr)).isEqualTo(expected);
        }
    }

    @Test
    void lowerCase() {
        String input = "PetiteTulipe";
        String expected = "petitetulipe";
        assertThat(Utils.onlyLettersAndWhitespace(input)).isEqualTo(expected);
        assertThat(Utils.cleanAndUnLeet(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            " astring",
            "str ing",
            "input  ",
            " ",
            "    ",
            "\n",
            "\t"
    })
    void testWhitespaceLeft(String input) {
        assertThat(Utils.onlyLettersAndWhitespace(input)).isEqualTo(input);
        assertThat(Utils.cleanAndUnLeet(input)).isEqualTo(input);
    }

    @ParameterizedTest
    @CsvSource({
            "1, l",
            "3, e",
            "4, a",
            "5, s",
            "6, b",
            "7, t",
            "8, b",
            "0, o",
            "1234567890, leasbtbo"
    })
    void testLeetMap(String input, String expected) {
        assertThat(Utils.cleanAndUnLeet(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "2",
            "9"
    })
    void testUnmappedNumbers(String input) {
        assertThat(Utils.cleanAndUnLeet(input)).isEqualTo("");
    }

    @ParameterizedTest
    @MethodSource("se.yrgo.libraryapp.validators.UtilsTest#leetExamples")
    void cleansAndConvertsLeet(String input, String expected) {
        assertThat(Utils.cleanAndUnLeet(input))
                .isEqualTo(expected);
    }

    static Stream<Arguments> leetExamples() {
        return Stream.of(
                Arguments.of("h3ll0", "hello"),
                Arguments.of("p37i73", "petite"),
                Arguments.of("g4m3", "game"),
                Arguments.of("p455w0rd", "password"),
                Arguments.of("H3LL0 W0RLD!", "hello world")
        );
    }

    @Test
    void mixedCharacters() {
        assertThat(Utils.cleanAndUnLeet("P37I73! 7ULIP3#%"))
                .isEqualTo("petite tulipe");
    }

    @Test
    void emptyString() {
        assertThat(Utils.onlyLettersAndWhitespace("")).isEqualTo("");
        assertThat(Utils.cleanAndUnLeet("")).isEqualTo("");
    }

    @Test
    void testNullInput() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Utils.cleanAndUnLeet(null));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Utils.cleanAndUnLeet(null));
    }

    @Property
    boolean onlyLettersAndWhiteSpace(@ForAll String text){
        String result = Utils.onlyLettersAndWhitespace(text);

        for (Character c : result.toCharArray()){
            if (!Character.isLowerCase(c) || !Character.isWhitespace(c)){
                return false;
            }
        }
        return true;
    }

}
