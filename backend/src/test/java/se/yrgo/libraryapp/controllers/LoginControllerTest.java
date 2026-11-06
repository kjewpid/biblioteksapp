package se.yrgo.libraryapp.controllers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.jooby.StatusCode;
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import io.jooby.Context;

import se.yrgo.libraryapp.dao.RoleDao;
import se.yrgo.libraryapp.dao.SessionDao;
import se.yrgo.libraryapp.entities.Role;
import se.yrgo.libraryapp.entities.UserId;
import se.yrgo.libraryapp.entities.forms.LoginData;
import se.yrgo.libraryapp.services.UserService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class LoginControllerTest {
    private UserService userService;
    private RoleDao roleDao;
    private SessionDao sessionDao;

    private LoginController loginController;
    private Context context;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        roleDao = mock(RoleDao.class);
        sessionDao = mock(SessionDao.class);
        context = mock(Context.class);

        loginController = new LoginController(
                userService,
                roleDao,
                sessionDao
        );
    }

    @Test
    void loginWithWrongPasswordReturnsUnauthorized() {

        LoginData login = new LoginData(
                "name",
                "wrongpassword");

        when(userService.validate("name", "wrongpassword"))
                .thenReturn(Optional.empty());

        List<Role> result = loginController.login(context, null, login);

        assertTrue(result.isEmpty());

        verify(context).setResponseCode(StatusCode.UNAUTHORIZED);
        verify(sessionDao, never()).create(any());
    }

    @Test
    void loginWithExistingSession() {
        String session = UUID.randomUUID().toString();

        when(sessionDao.validate(UUID.fromString(session)))
                .thenReturn(UserId.of(1));

        LoginData login = new LoginData(
                "name",
                "password"
        );

        List<Role> result = loginController.login(context, session, login);

        assertThat(result).isEmpty();
        verify(userService, never()).validate(anyString(), anyString());
    }

    @Test
    void loginWithoutSession() {
        LoginData login = new LoginData(
                "name",
                "password"
        );

        List<Role> result = loginController.login(context, null, login);

        assertThat(result).isEmpty();
        verify(context).setResponseCode(StatusCode.UNAUTHORIZED);
        verify(sessionDao, never()).create(any());
    }

    // orkar inte mer här nej tack usch
}
