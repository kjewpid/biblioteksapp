package se.yrgo.libraryapp.validators;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.BDDAssertions.as;

public class RealNameTest {
    @ParameterizedTest
    @MethodSource("se.yrgo.libraryapp.validators.RealNameTest#badWords")
    void rejectsBadWords(String word){
        assertThat(RealName.validate(word))
        .as(word + " should be false").isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Petite Tulipe",
            "Petit Papillon",
            "Chataigne mignon",
            "Mademoiselle La Lune"
    })
    void validNames(String input){
        assertThat(RealName.validate(input)).isTrue();
    }


    static Stream<String> badWords(){
        InputStream is = RealNameTest.class.getClassLoader()
                .getResourceAsStream("bad_words.txt");

        return new BufferedReader(new InputStreamReader(is))
                .lines();
    }
}
