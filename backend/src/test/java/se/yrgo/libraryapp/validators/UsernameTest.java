package se.yrgo.libraryapp.validators;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

public class UsernameTest {
    @ParameterizedTest
    @ValueSource(strings = {
            "bosse",
            "tulipe@",
            "papillon123",
            "da_chef",
            "pink-penguin",
            "french.ie",
            "1234"
    })
    void correctUsername(String username) {
        assertThat(Username.validate(username)).isTrue();
    }

    @Test
    void incorrectUsernameWithSpace() {
        assertThat(Username.validate("user name")).isFalse();
    }

    @Test
    void randomInvalidUsernameSymbols() {
        String symbols = "!#$%^&*()+=?/\\<>[]{}€åäö";

        for (char symbol : symbols.toCharArray()) {
            String username = "test" + symbol;
            assertThat(Username.validate(username)).isFalse();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "1",
            "ab",
            "abc",
            ""
    })
    void usernameTooShort(String username) {
        assertThat(Username.validate(username)).isFalse();
    }


}
