package se.yrgo.libraryapp.dao;

import com.radcortez.flyway.test.annotation.H2;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import se.yrgo.libraryapp.entities.*;

import javax.sql.DataSource;
import javax.swing.text.html.Option;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@H2
public class BookDaoIntegrationTest {
    private static DataSource ds;
    private static BookDao bookDao;

    @BeforeAll
    static void initDataSource() {
        final JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:test");
        BookDaoIntegrationTest.ds = ds;
        bookDao = new BookDao(ds);
    }

    @Test
    void getBookWithClass() {
        Set<BookEdition> books =
                bookDao.withClass(new DdsClassification(5, null));

        assertThat(books)
                .hasSize(1)
                .extracting(BookEdition::getTitle)
                .containsExactly("T-SQL Fundamentals");
    }

    @Test
    void getBooksWithMultipleBooksInSameClass() {
        Set<BookEdition> books =
                bookDao.withClass(new DdsClassification(800, null));

        assertThat(books)
                .hasSize(5);
    }

    @Test
    void getBookWithNonExistingClass() {
        Set<BookEdition> books =
                bookDao.withClass(new DdsClassification(999, null));

        assertThat(books).isEmpty();
    }

    @Test
    void getExistingBook() {
        Optional<Book> maybeBook = bookDao.get(BookId.of(1));

        assertThat(maybeBook).isPresent();

        Book book = maybeBook.get();

        assertThat(book.getId()).isEqualTo(BookId.of(1));
        assertThat(book.getEdition().getTitle())
                .isEqualTo("T-SQL Fundamentals");
        assertThat(book.getEdition().getAuthor())
                .isEqualTo("Itzik Ben-Gan");
        assertThat(book.getEdition().getIsbn())
                .isEqualTo("9781509302000");
    }

    @Test
    void getNonExistingBook() {
        Optional<Book> book = bookDao.get(BookId.of(999));

        assertThat(book).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {
            1,
            3,
            5
    })
    void getBookChecksAvailability(int id) {
        Optional<Book> book = bookDao.get(BookId.of(id));

        assertThat(book).isPresent();
        assertThat(book.get().getAvailable()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {
            2,
            4})
    void getBookChecksIfBorrowed(int id) {
        Optional<Book> book = bookDao.get(BookId.of(id));

        assertThat(book).isPresent();
        assertThat(book.get().getAvailable()).isFalse();
    }

    @Test
    void findBookByPartialIsbn() {
        Set<BookEdition> books = bookDao.find("9781", null, null);

        assertThat(books)
                .hasSize(3)
                .extracting(BookEdition::getIsbn)
                .containsExactlyInAnyOrder(
                        "9781784726188",
                        "9781442435858",
                        "9781509302000"
                );
    }

    @Test
    void findBookByFullIsbn() {
        Set<BookEdition> books =
                bookDao.find("9781509302000", null, null);

        assertThat(books)
                .hasSize(1)
                .extracting(BookEdition::getTitle)
                .containsOnly("T-SQL Fundamentals");
    }

    @Test
    void findBookByTitle() {
        Set<BookEdition> books = bookDao.find(null, "T-SQL Fundamentals", null);

        assertThat(books)
                .hasSize(1)
                .extracting(BookEdition::getIsbn)
                .containsOnly("9781509302000");
    }

    @Test
    void findBookByAuthor() {
        Set<BookEdition> books = bookDao.find(null, null, "Dean Koontz");

        assertThat(books)
                .hasSize(2);
    }

    @Test
    void findBookUsingMultipleCriteria() {
        Set<BookEdition> books = bookDao.find("978", "T-SQL Fundamentals", null);

        assertThat(books)
                .hasSize(1)
                .extracting(BookEdition::getIsbn)
                .containsExactly("9781509302000");
    }

    @Test
    void findBookUsingAllCriteria() {
        Set<BookEdition> books = bookDao.find(
                "9781509302000",
                "T-SQL Fundamentals",
                "Itzik Ben-Gan"
        );

        assertThat(books)
                .hasSize(1)
                .extracting(BookEdition::getIsbn)
                .containsExactly("9781509302000");
    }

    @Test
    void findBookWithConflictingCriteria() {
        Set<BookEdition> books = bookDao.find(
                "9781509302000",
                null,
                "Dean Koontz"
        );

        assertThat(books).isEmpty();
    }

    @Test
    void findBookByPartialAuthor() {
        Set<BookEdition> books =
                bookDao.find(null, null, "Koontz");

        assertThat(books)
                .hasSize(2)
                .extracting(BookEdition::getAuthor)
                .containsOnly("Dean Koontz");
    }

    @Test
    void findBookWithWhitespaceAroundTitle() {
        Set<BookEdition> books =
                bookDao.find(null, " T-SQL Fundamentals ", null);

        assertThat(books)
                .hasSize(1)
                .extracting(BookEdition::getTitle)
                .containsOnly("T-SQL Fundamentals");
    }

    @Test
    void findNonExistingBook() {
        Set<BookEdition> books = bookDao.find(null, "ThisBookDoesNotExist", null);

        assertThat(books).isEmpty();
    }

    @Test
    void findWithoutSearchCriteria() {
        Set<BookEdition> books = bookDao.find(null, null, null);

        assertThat(books).isEmpty();
    }

    @Test
    void findWithEmptyStrings() {
        Set<BookEdition> books = bookDao.find("", " ", "");

        assertThat(books).isEmpty();
    }

    @Test
    void userWithLoans() {
        List<BookLoan> loans = bookDao.loans(UserId.of(2));

        assertThat(loans)
                .hasSize(2);
    }

    @Test
    void userWithNoLoans() {
        List<BookLoan> loans = bookDao.loans(UserId.of(1));

        assertThat(loans).isEmpty();
    }
}
