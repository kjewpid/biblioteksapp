package se.yrgo.libraryapp.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import com.radcortez.flyway.test.annotation.H2;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import se.yrgo.libraryapp.entities.LoginInfo;
import se.yrgo.libraryapp.entities.User;
import se.yrgo.libraryapp.entities.UserId;

import static org.assertj.core.api.Assertions.*;

@Tag("integration")
@H2
public class UserDaoIntegrationTest {
    private static DataSource ds;
    private static UserDao userDao;

    @BeforeAll
    static void initDataSource() {
// this way we do not need to create a new datasource every time
        final JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:test");
        UserDaoIntegrationTest.ds = ds;
        userDao = new UserDao(ds);
    }

    // get()
    @Test
    void getUserById() {
// this data comes from the test migration files
        final String username = "test";
        final UserId userId = UserId.of(1);
        Optional<User> maybeUser = userDao.get(Integer.toString(userId.getId()));
        assertThat(maybeUser).isPresent();
        assertThat(maybeUser.get().getName()).isEqualTo(username);
        assertThat(maybeUser.get().getId()).isEqualTo(userId);
    }

    @Test
    void getNonExistingUser() {
        Optional<User> user = userDao.get("999");

        assertThat(user).isEmpty();
    }

    @Test
    void getUserWithNullId() {
        Optional<User> user = userDao.get(null);

        assertThat(user).isEmpty();
    }


    // getLoginInfo()
    @Test
    void getLoginInfo() {
        Optional<LoginInfo> userLoginInfo =
                userDao.getLoginInfo("test");

        assertThat(userLoginInfo).isPresent();
        assertThat(userLoginInfo.get().getUserId())
                .isEqualTo(UserId.of(1));
    }


    @Test
    void getNonExistingUserLoginInfo() {
        Optional<LoginInfo> userLoginInfo =
                userDao.getLoginInfo("nonexisting");

        assertThat(userLoginInfo).isEmpty();
    }

    @Test
    void getNullUserLoginInfo() {
        Optional<LoginInfo> userLoginInfo =
                userDao.getLoginInfo(null);

        assertThat(userLoginInfo).isEmpty();
    }


    // register()
    @Test
    void registerValidUser() {
        userDao.register("name", "realname", "password");

        assertThat(userDao.getLoginInfo("name")).isPresent();
    }

    @Test
    void shouldRollbackWhenRoleDoesNotExist() throws SQLException {
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM user_role WHERE role_id = 2");
            stmt.executeUpdate("DELETE FROM role WHERE id = 2");
        }

        boolean result = userDao.register(
                "name",
                "realname",
                "password"
        );

        assertThat(result).isFalse();
        assertThat(userDao.getLoginInfo("name")).isEmpty();
    }


    @Test
    void addSameUserTwiceShouldBeUnsuccessfull() throws SQLException {
        int usersBefore = getAllUsers().size();

        userDao.register(
                "name",
                "realname",
                "password"
        );

        boolean result = userDao.register(
                "name",
                "anotherrealname",
                "anotherpassword"
        );

        assertThat(result).isFalse();
        assertThat(getAllUsers().size()).isEqualTo(usersBefore + 1);
    }


    private List<User> getAllUsers() throws SQLException {
        List<User> users = new ArrayList<>();

        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM user")) {

            while (rs.next()) {
                User user = new User(
                        UserId.of(rs.getInt("id")),
                        rs.getString("user"),
                        rs.getString("realname"));

                users.add(user);
            }
        }

        return users;
    }

    @Test
    void passwordShouldNotMatchHash() {
        String password = "password";
        userDao.register(
                "name",
                "realname",
                password);

        String hashedPass = userDao.getLoginInfo("name").get().getPasswordHash();

        assertThat(password).isNotEqualTo(hashedPass);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Password",
            "password123",
            "pass-word"
    })
    void wrongPassShouldNotMatch(String password) {
        userDao.register(
                "name",
                "realname",
                "password");

        Argon2PasswordEncoder encoder = new Argon2PasswordEncoder();
        String hashedPass = userDao.getLoginInfo("name").get().getPasswordHash();

        assertThat(encoder.matches(password, hashedPass)).isFalse();
    }

    @Test
    void hashedPasswordShouldMatch() {
        String password = "password";
        userDao.register(
                "name",
                "realname",
                password);

        String hashedPass = userDao.getLoginInfo("name").get().getPasswordHash();

        Argon2PasswordEncoder encoder = new Argon2PasswordEncoder();
        assertThat(encoder.matches(password, hashedPass)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Password123!",
            "My-password@",
            "test_pass",
            "pássword..123",
            "123456789!",
            "Äö_password#123"
    })
    void passwordWithNonLettersShouldHashCorrectly(String password) {
        userDao.register(
                "name",
                "realname",
                password);

        String hashedPass = userDao.getLoginInfo("name").get().getPasswordHash();

        Argon2PasswordEncoder encoder = new Argon2PasswordEncoder();
        assertThat(encoder.matches(password, hashedPass)).isTrue();
    }


    // isNameAvailable()
    @Test
    void nameShouldBeAvailable() {
        boolean result = userDao.isNameAvailable("validname");

        assertThat(result).isTrue();
    }

    @Test
    void existingNameShouldBeUnavailable() {
        boolean result = userDao.isNameAvailable("test");

        assertThat(result).isFalse();
    }

}