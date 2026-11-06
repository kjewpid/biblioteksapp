package se.yrgo.libraryapp.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import se.yrgo.libraryapp.dao.BookDao;
import se.yrgo.libraryapp.entities.BookEdition;
import se.yrgo.libraryapp.entities.DdsClassification;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookControllerTest {

    @Mock
    private BookDao bookDao;

    private BookController bookController;

    @BeforeEach
    void setUp() {
        bookController = new BookController(bookDao);
    }

    @Test
    void searchByIsbn() {
        String isbn = "9781509302000";
        Set<BookEdition> expected = Set.of(
                new BookEdition(
                        "T-SQL Fundamentals",
                        "Itzik Ben-Gan",
                        isbn
                )
        );

        when(bookDao.find(isbn, null, null)).thenReturn(expected);

        Set<BookEdition> result =
                bookController.search(isbn, null, null, null);

        assertThat(result).isEqualTo(expected);
        verify(bookDao).find(isbn, null, null);
        verify(bookDao, never()).withClass(any(DdsClassification.class));
    }

    @Test
    void searchByTitle() {
        String title = "T-SQL Fundamentals";
        Set<BookEdition> expected = Set.of(
                new BookEdition(
                        title,
                        "Itzik Ben-Gan",
                        "9781509302000"
                )
        );

        when(bookDao.find(null, title, null))
                .thenReturn(expected);

        Set<BookEdition> result =
                bookController.search(null, title, null, null);

        assertThat(result).isEqualTo(expected);
        verify(bookDao).find(null, title, null);
        verify(bookDao, never()).withClass(any(DdsClassification.class));
    }

    @Test
    void searchByAuthor() {
        String author = "Itzik Ben-Gan";
        Set<BookEdition> expected = Set.of(
                new BookEdition(
                        "T-SQL Fundamentals",
                        author,
                        "9781509302000"
                )
        );

        when(bookDao.find(null, null, author)).thenReturn(expected);

        Set<BookEdition> result =
                bookController.search(null, null, author, null);

        assertThat(result).isEqualTo(expected);
        verify(bookDao).find(null, null, author);
        verify(bookDao, never()).withClass(any(DdsClassification.class));
    }

    @Test
    void searchWithClass() {
        Set<BookEdition> expected = Set.of(
                new BookEdition(
                        "T-SQL Fundamentals",
                        "Itzik Ben-Gan",
                        "9781509302000"
                )
        );

        when(bookDao.withClass(any(DdsClassification.class)))
                .thenReturn(expected);

        Set<BookEdition> result = bookController.search(null, null, null, "5");

        assertThat(result).isEqualTo(expected);

        verify(bookDao).withClass(argThat(
                c -> c.getCode() == 5
        ));

        verify(bookDao, never()).find(any(), any(), any());
    }

    @Test
    void searchUsingMultipleCriteria() {
        String title = "T-SQL Fundamentals";
        String isbn = "9781509302000";
        Set<BookEdition> expected = Set.of(
                new BookEdition(
                        title,
                        "Itzik Ben-Gan",
                        isbn
                )
        );

        when(bookDao.find(isbn, title, null)).thenReturn(expected);

        Set<BookEdition> result = bookController.search(isbn, title, null, null);

        assertThat(result).isEqualTo(expected);
        verify(bookDao).find(isbn, title, null);
        verify(bookDao, never()).withClass(any(DdsClassification.class));
    }

    @Test
    void searchUsingTitleAndClass() {
        String title = "T-SQL Fundamentals";
        Set<BookEdition> expected = Set.of(
                new BookEdition(
                        title,
                        "Itzik Ben-Gan",
                        "9781509302000"
                )
        );

        when(bookDao.withClass(any(DdsClassification.class)))
                .thenReturn(expected);

        Set<BookEdition> result = bookController.search(null, title, null, "5");
        assertThat(result).isEqualTo(expected);

        verify(bookDao).withClass(new DdsClassification(5, null));
        verify(bookDao, never()).find(any(), any(), any());
    }

    @Test
    void searchWithNoCriteria() {
        when(bookDao.find(any(), any(), any()))
                .thenReturn(Set.of());

        Set<BookEdition> result = bookController.search(null, null, null, null);

        assertThat(result).isEmpty();
        verify(bookDao).find(null, null, null);
        verify(bookDao, never()).withClass(any(DdsClassification.class));
    }

    @Test
    void searchInvalidIsbn() {
        String isbn = "0000000000";
        when(bookDao.find(isbn, null, null))
                .thenReturn(Set.of());

        Set<BookEdition> result = bookController.search(isbn, null, null, null);

        assertThat(result).isEmpty();
        verify(bookDao).find(isbn, null, null);
    }

    @Test
    void searchInvalidDdsInput() {
        assertThatExceptionOfType(NumberFormatException.class)
                .isThrownBy(() ->
                        bookController.search(null, null, null, "abc"));

        verifyNoInteractions(bookDao);
    }

    @Test
    void searchWithAllCriteriaShouldPrioritizeClass() {
        when(bookDao.withClass(any(DdsClassification.class)))
                .thenReturn(Set.of());


        Set<BookEdition> result =
                bookController.search(
                        "9781509302000",
                        "T-SQL Fundamentals",
                        "Itzik Ben-Gan",
                        "5"
                );

        assertThat(result).isEmpty();

        verify(bookDao).withClass(any(DdsClassification.class));
        verify(bookDao, never()).find(any(), any(), any());
    }

    @Test
    void searchWithEmptyStrings() {
        when(bookDao.find("", "", ""))
                .thenReturn(Set.of());

        Set<BookEdition> result =
                bookController.search("", "", "", null);

        assertThat(result).isEmpty();
        verify(bookDao).find("", "", "");
    }
}