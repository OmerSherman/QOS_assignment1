package ac.il.bgu.qa;

import ac.il.bgu.qa.errors.*;
import ac.il.bgu.qa.services.DatabaseService;
import ac.il.bgu.qa.services.NotificationService;
import ac.il.bgu.qa.services.ReviewService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class TestLibrary {

    private static final String VALID_ISBN = "978-0-306-40615-7";
    private static final String VALID_ISBN_NO_HYPHENS = "9780306406157";
    // valid ISBN whose calculated check digit is 0
    private static final String VALID_ISBN_CHECK_DIGIT_ZERO = "9780306406140";
    // valid ISBN that starts with 0 (must not be treated as a number)
    private static final String VALID_ISBN_LEADING_ZERO = "0123456789012";
    private static final String VALID_USER_ID = "123456789012";
    private static final String VALID_TITLE = "Some Title";
    private static final String VALID_AUTHOR = "John Smith";
    private static final String VALID_USER_NAME = "Some User";

    @Mock
    private DatabaseService databaseServiceMock;

    @Mock
    private ReviewService reviewServiceMock;

    @Mock
    private NotificationService notificationServiceMock;

    private AutoCloseable mocksHandle;
    private Library library;

    @BeforeEach
    void init() {
        // Initialize the @Mock fields before each test and create a fresh Library on top of them
        mocksHandle = MockitoAnnotations.openMocks(this);
        library = new Library(databaseServiceMock, reviewServiceMock);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocksHandle.close();
    }

    // Helpers and data providers

    // Creates a mocked Book with the given properties.
    private Book createBookMock(String isbn, String title, String author, boolean borrowed) {
        Book book = Mockito.mock(Book.class);
        when(book.getISBN()).thenReturn(isbn);
        when(book.getTitle()).thenReturn(title);
        when(book.getAuthor()).thenReturn(author);
        when(book.isBorrowed()).thenReturn(borrowed);
        return book;
    }

    // Creates a mocked, well-formed Book that is not borrowed.
    private Book createValidBookMock() {
        return createBookMock(VALID_ISBN, VALID_TITLE, VALID_AUTHOR, false);
    }

    // Creates a mocked User with the given properties.
    private User createUserMock(String id, String name, NotificationService notificationService) {
        User user = Mockito.mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getName()).thenReturn(name);
        when(user.getNotificationService()).thenReturn(notificationService);
        return user;
    }

    // ISBNs that are not valid ISBN-13 values.
    static Stream<String> invalidIsbnProvider() {
        return Stream.of(
                null,
                "",
                "-------------",        // only hyphens
                "978030640615",         // 12 digits
                "97803064061577",       // 14 digits
                "978030640615X",        // last character is not a digit
                "97803064X6157",        // non digit in the middle
                "9780306406158",        // wrong check digit
                "9780306406141"         // wrong check digit (expected check digit is 0)
        );
    }

    // User ids that are not exactly 12 digits.
    static Stream<String> invalidUserIdProvider() {
        return Stream.of(
                null,
                "",
                "12345678901",          // 11 digits
                "1234567890123",        // 13 digits
                "12345678901a",         // contains a letter
                "12345 789012",         // contains a space
                "-12345678901"          // contains a sign
        );
    }

    // Authors that violate the author-name rules
    static Stream<String> invalidAuthorProvider() {
        return Stream.of(
                null,
                "",
                "1" + VALID_AUTHOR,
                "-" + VALID_AUTHOR,
                " " + VALID_AUTHOR,
                "@" + VALID_AUTHOR,
                "." + VALID_AUTHOR,
                "'" + VALID_AUTHOR,
                VALID_AUTHOR + "-",
                VALID_AUTHOR + ".",
                VALID_AUTHOR + "'",
                VALID_AUTHOR + " ",
                VALID_AUTHOR + "1",
                "John3 Smith",
                "John@ Smith",
                "John--Smith",
                "John''Smith",
                VALID_AUTHOR + "@",
                VALID_AUTHOR + "3",
                VALID_AUTHOR + "''",
                VALID_AUTHOR + "--"
        );
    }

    // Authors that are valid and contain allowed special characters
    static Stream<String> validAuthorProvider() {
        return Stream.of(
                VALID_AUTHOR,
                "J",
                "John-Smith",
                "John'Smith",
                "John.Smith",
                "J. Smith"
        );
    }

    // addBook

    @Test
    void givenNullBook_whenAddBook_thenThrowsInvalidBook() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> library.addBook(null));
        assertEquals("Invalid book.", ex.getMessage());
        verifyNoInteractions(databaseServiceMock);
    }

    @ParameterizedTest
    @MethodSource("invalidIsbnProvider")
    void givenInvalidIsbn_whenAddBook_thenThrowsInvalidIsbn(String isbn) {
        Book book = createBookMock(isbn, VALID_TITLE, VALID_AUTHOR, false);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> library.addBook(book));
        assertEquals("Invalid ISBN.", ex.getMessage());
        verifyNoInteractions(databaseServiceMock);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void givenNullOrEmptyTitle_whenAddBook_thenThrowsInvalidTitle(String title) {
        Book book = createBookMock(VALID_ISBN, title, VALID_AUTHOR, false);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> library.addBook(book));
        assertEquals("Invalid title.", ex.getMessage());
        verifyNoInteractions(databaseServiceMock);
    }

    @ParameterizedTest
    @MethodSource("invalidAuthorProvider")
    void givenInvalidAuthor_whenAddBook_thenThrowsInvalidAuthor(String author) {
        Book book = createBookMock(VALID_ISBN, VALID_TITLE, author, false);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> library.addBook(book));
        assertEquals("Invalid author.", ex.getMessage());
        verifyNoInteractions(databaseServiceMock);
    }

    @Test
    void givenBorrowedBook_whenAddBook_thenThrowsInvalidBorrowedState() {
        Book book = createBookMock(VALID_ISBN, VALID_TITLE, VALID_AUTHOR, true);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> library.addBook(book));
        assertEquals("Book with invalid borrowed state.", ex.getMessage());
        verifyNoInteractions(databaseServiceMock);
    }

    @Test
    void givenBookAlreadyInDatabase_whenAddBook_thenThrowsAndDoesNotAdd() {
        Book book = createValidBookMock();
        when(databaseServiceMock.getBookByISBN(VALID_ISBN)).thenReturn(Mockito.mock(Book.class));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> library.addBook(book));
        assertEquals("Book already exists.", ex.getMessage());
        verify(databaseServiceMock, never()).addBook(anyString(), Mockito.any(Book.class));
    }

    @ParameterizedTest
    @MethodSource("validAuthorProvider")
    void givenValidBook_whenAddBook_thenBookAddedToDatabase(String author) {
        Book book = createBookMock(VALID_ISBN, VALID_TITLE, author, false);
        when(databaseServiceMock.getBookByISBN(VALID_ISBN)).thenReturn(null);
        library.addBook(book);
        verify(databaseServiceMock).addBook(VALID_ISBN, book);
    }

    @ParameterizedTest
    @ValueSource(strings = {VALID_ISBN_NO_HYPHENS, VALID_ISBN_CHECK_DIGIT_ZERO, VALID_ISBN_LEADING_ZERO})
    void givenValidIsbnVariants_whenAddBook_thenBookAddedToDatabase(String isbn) {
        Book book = createBookMock(isbn, VALID_TITLE, VALID_AUTHOR, false);
        library.addBook(book);
        verify(databaseServiceMock).addBook(isbn, book);
    }

    // registerUser

    @Test
    void givenNullUser_whenRegisterUser_thenThrowsInvalidUser() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> library.registerUser(null));
        assertEquals("Invalid user.", ex.getMessage());
        verifyNoInteractions(databaseServiceMock);
    }

    @ParameterizedTest
    @MethodSource("invalidUserIdProvider")
    void givenInvalidUserId_whenRegisterUser_thenThrowsInvalidUserId(String userId) {
        User user = createUserMock(userId, VALID_USER_NAME, notificationServiceMock);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> library.registerUser(user));
        assertEquals("Invalid user Id.", ex.getMessage());
        verifyNoInteractions(databaseServiceMock);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void givenNullOrEmptyUserName_whenRegisterUser_thenThrowsInvalidUserName(String name) {
        User user = createUserMock(VALID_USER_ID, name, notificationServiceMock);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> library.registerUser(user));
        assertEquals("Invalid user name.", ex.getMessage());
        verifyNoInteractions(databaseServiceMock);
    }

    @Test
    void givenNullNotificationService_whenRegisterUser_thenThrowsInvalidNotificationService() {
        User user = createUserMock(VALID_USER_ID, VALID_USER_NAME, null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> library.registerUser(user));
        assertEquals("Invalid notification service.", ex.getMessage());
        verifyNoInteractions(databaseServiceMock);
    }

    @Test
    void givenUserAlreadyExists_whenRegisterUser_thenThrowsAndDoesNotRegister() {
        User user = createUserMock(VALID_USER_ID, VALID_USER_NAME, notificationServiceMock);
        when(databaseServiceMock.getUserById(VALID_USER_ID)).thenReturn(Mockito.mock(User.class));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> library.registerUser(user));
        assertEquals("User already exists.", ex.getMessage());
        verify(databaseServiceMock, never()).registerUser(anyString(), Mockito.any(User.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {VALID_USER_ID, "012345678901", "000000000000"})
    void givenValidNewUser_whenRegisterUser_thenUserRegisteredInDatabase(String userId) {
        User user = createUserMock(userId, VALID_USER_NAME, notificationServiceMock);
        when(databaseServiceMock.getUserById(userId)).thenReturn(null);
        library.registerUser(user);
        verify(databaseServiceMock).registerUser(userId, user);
    }

    // borrowBook

    @ParameterizedTest
    @MethodSource("invalidIsbnProvider")
    void givenInvalidIsbn_whenBorrowBook_thenThrowsInvalidIsbn(String isbn) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> library.borrowBook(isbn, VALID_USER_ID));
        assertEquals("Invalid ISBN.", ex.getMessage());
        verifyNoInteractions(databaseServiceMock);
    }

    @Test
    void givenBookNotInDatabase_whenBorrowBook_thenThrowsBookNotFound() {
        when(databaseServiceMock.getBookByISBN(VALID_ISBN)).thenReturn(null);
        BookNotFoundException ex = assertThrows(BookNotFoundException.class,
                () -> library.borrowBook(VALID_ISBN, VALID_USER_ID));
        assertEquals("Book not found!", ex.getMessage());
        verify(databaseServiceMock, never()).borrowBook(anyString(), anyString());
    }

    @ParameterizedTest
    @MethodSource("invalidUserIdProvider")
    void givenInvalidUserId_whenBorrowBook_thenThrowsInvalidUserId(String userId) {
        Book book = createValidBookMock();
        when(databaseServiceMock.getBookByISBN(VALID_ISBN)).thenReturn(book);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> library.borrowBook(VALID_ISBN, userId));
        assertEquals("Invalid user Id.", ex.getMessage());
        verify(book, never()).borrow();
        verify(databaseServiceMock, never()).borrowBook(anyString(), anyString());
    }

    @Test
    void givenUserNotRegistered_whenBorrowBook_thenThrowsUserNotRegistered() {
        Book book = createValidBookMock();
        when(databaseServiceMock.getBookByISBN(VALID_ISBN)).thenReturn(book);
        when(databaseServiceMock.getUserById(VALID_USER_ID)).thenReturn(null);
        UserNotRegisteredException ex = assertThrows(UserNotRegisteredException.class,
                () -> library.borrowBook(VALID_ISBN, VALID_USER_ID));
        assertEquals("User not found!", ex.getMessage());
        verify(book, never()).borrow();
        verify(databaseServiceMock, never()).borrowBook(anyString(), anyString());
    }

    @Test
    void givenBookAlreadyBorrowed_whenBorrowBook_thenThrowsBookAlreadyBorrowed() {
        Book book = createBookMock(VALID_ISBN, VALID_TITLE, VALID_AUTHOR, true);
        when(databaseServiceMock.getBookByISBN(VALID_ISBN)).thenReturn(book);
        when(databaseServiceMock.getUserById(VALID_USER_ID)).thenReturn(Mockito.mock(User.class));
        BookAlreadyBorrowedException ex = assertThrows(BookAlreadyBorrowedException.class,
                () -> library.borrowBook(VALID_ISBN, VALID_USER_ID));
        assertEquals("Book is already borrowed!", ex.getMessage());
        verify(book, never()).borrow();
        verify(databaseServiceMock, never()).borrowBook(anyString(), anyString());
    }

    @Test
    void givenAvailableBookAndRegisteredUser_whenBorrowBook_thenBookBorrowedAndRecorded() {
        Book book = createValidBookMock();
        when(databaseServiceMock.getBookByISBN(VALID_ISBN)).thenReturn(book);
        when(databaseServiceMock.getUserById(VALID_USER_ID)).thenReturn(Mockito.mock(User.class));
        library.borrowBook(VALID_ISBN, VALID_USER_ID);
        verify(book).borrow();
        verify(databaseServiceMock).borrowBook(VALID_ISBN, VALID_USER_ID);
    }

    // returnBook

    @ParameterizedTest
    @MethodSource("invalidIsbnProvider")
    void givenInvalidIsbn_whenReturnBook_thenThrowsInvalidIsbn(String isbn) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> library.returnBook(isbn));
        assertEquals("Invalid ISBN.", ex.getMessage());
        verifyNoInteractions(databaseServiceMock);
    }

    @Test
    void givenBookNotInDatabase_whenReturnBook_thenThrowsBookNotFound() {
        when(databaseServiceMock.getBookByISBN(VALID_ISBN)).thenReturn(null);
        BookNotFoundException ex = assertThrows(BookNotFoundException.class,
                () -> library.returnBook(VALID_ISBN));
        assertEquals("Book not found!", ex.getMessage());
        verify(databaseServiceMock, never()).returnBook(anyString());
    }

    @Test
    void givenBookNotBorrowed_whenReturnBook_thenThrowsBookNotBorrowed() {
        Book book = createValidBookMock();
        when(databaseServiceMock.getBookByISBN(VALID_ISBN)).thenReturn(book);
        BookNotBorrowedException ex = assertThrows(BookNotBorrowedException.class,
                () -> library.returnBook(VALID_ISBN));
        assertEquals("Book wasn't borrowed!", ex.getMessage());
        verify(book, never()).returnBook();
        verify(databaseServiceMock, never()).returnBook(anyString());
    }

    @Test
    void givenBorrowedBook_whenReturnBook_thenBookReturnedAndRecorded() {
        Book book = createBookMock(VALID_ISBN, VALID_TITLE, VALID_AUTHOR, true);
        when(databaseServiceMock.getBookByISBN(VALID_ISBN)).thenReturn(book);
        library.returnBook(VALID_ISBN);
        verify(book).returnBook();
        verify(databaseServiceMock).returnBook(VALID_ISBN);
    }

    // notifyUserWithBookReviews

    @ParameterizedTest
    @MethodSource("invalidIsbnProvider")
    void givenInvalidIsbn_whenNotifyUserWithBookReviews_thenThrowsInvalidIsbn(String isbn) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> library.notifyUserWithBookReviews(isbn, VALID_USER_ID));
        assertEquals("Invalid ISBN.", ex.getMessage());
        verifyNoInteractions(databaseServiceMock);
        verifyNoInteractions(reviewServiceMock);
    }

    @ParameterizedTest
    @MethodSource("invalidUserIdProvider")
    void givenInvalidUserId_whenNotifyUserWithBookReviews_thenThrowsInvalidUserId(String userId) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> library.notifyUserWithBookReviews(VALID_ISBN, userId));
        assertEquals("Invalid user Id.", ex.getMessage());
        verifyNoInteractions(databaseServiceMock);
        verifyNoInteractions(reviewServiceMock);
    }

    @Test
    void givenBookNotInDatabase_whenNotifyUserWithBookReviews_thenThrowsBookNotFound() {
        when(databaseServiceMock.getBookByISBN(VALID_ISBN)).thenReturn(null);
        BookNotFoundException ex = assertThrows(BookNotFoundException.class,
                () -> library.notifyUserWithBookReviews(VALID_ISBN, VALID_USER_ID));
        assertEquals("Book not found!", ex.getMessage());
        verifyNoInteractions(reviewServiceMock);
    }

    @Test
    void givenUserNotRegistered_whenNotifyUserWithBookReviews_thenThrowsUserNotRegistered() {
        when(databaseServiceMock.getBookByISBN(VALID_ISBN)).thenReturn(createValidBookMock());
        when(databaseServiceMock.getUserById(VALID_USER_ID)).thenReturn(null);
        UserNotRegisteredException ex = assertThrows(UserNotRegisteredException.class,
                () -> library.notifyUserWithBookReviews(VALID_ISBN, VALID_USER_ID));
        assertEquals("User not found!", ex.getMessage());
        verifyNoInteractions(reviewServiceMock);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void givenNoReviews_whenNotifyUserWithBookReviews_thenThrowsNoReviewsFoundAndClosesService(
            List<String> reviews) {
        User user = Mockito.mock(User.class);
        when(databaseServiceMock.getBookByISBN(VALID_ISBN)).thenReturn(createValidBookMock());
        when(databaseServiceMock.getUserById(VALID_USER_ID)).thenReturn(user);
        when(reviewServiceMock.getReviewsForBook(VALID_ISBN)).thenReturn(reviews);
        NoReviewsFoundException ex = assertThrows(NoReviewsFoundException.class,
                () -> library.notifyUserWithBookReviews(VALID_ISBN, VALID_USER_ID));
        assertEquals("No reviews found!", ex.getMessage());
        verify(reviewServiceMock).close();
        verifyNoInteractions(user);
    }

    @Test
    void givenReviewServiceFails_whenNotifyUserWithBookReviews_thenThrowsServiceUnavailableAndClosesService() {
        User user = Mockito.mock(User.class);
        when(databaseServiceMock.getBookByISBN(VALID_ISBN)).thenReturn(createValidBookMock());
        when(databaseServiceMock.getUserById(VALID_USER_ID)).thenReturn(user);
        when(reviewServiceMock.getReviewsForBook(VALID_ISBN))
                .thenThrow(new ReviewException("Review Service Unavailable"));
        ReviewServiceUnavailableException ex = assertThrows(ReviewServiceUnavailableException.class,
                () -> library.notifyUserWithBookReviews(VALID_ISBN, VALID_USER_ID));
        assertEquals("Review service unavailable!", ex.getMessage());
        verify(reviewServiceMock).close();
        verifyNoInteractions(user);
    }

    @Test
    void givenSingleReview_whenNotifyUserWithBookReviews_thenUserNotifiedWithFormattedMessage() {
        User user = Mockito.mock(User.class);
        when(databaseServiceMock.getBookByISBN(VALID_ISBN))
                .thenReturn(createBookMock(VALID_ISBN, "Dune", VALID_AUTHOR, false));
        when(databaseServiceMock.getUserById(VALID_USER_ID)).thenReturn(user);
        when(reviewServiceMock.getReviewsForBook(VALID_ISBN)).thenReturn(List.of("great"));
        library.notifyUserWithBookReviews(VALID_ISBN, VALID_USER_ID);
        verify(user, times(1)).sendNotification("Reviews for 'Dune':\ngreat");
        verify(reviewServiceMock).close();
    }

    @Test
    void givenSeveralReviews_whenNotifyUserWithBookReviews_thenUserNotifiedWithAllReviews() {
        User user = Mockito.mock(User.class);
        when(databaseServiceMock.getBookByISBN(VALID_ISBN))
                .thenReturn(createBookMock(VALID_ISBN, "Dune", VALID_AUTHOR, false));
        when(databaseServiceMock.getUserById(VALID_USER_ID)).thenReturn(user);
        when(reviewServiceMock.getReviewsForBook(VALID_ISBN))
                .thenReturn(List.of("good book", "very interesting", "too long"));
        library.notifyUserWithBookReviews(VALID_ISBN, VALID_USER_ID);
        verify(user, times(1))
                .sendNotification("Reviews for 'Dune':\ngood book\nvery interesting\ntoo long");
        verify(reviewServiceMock).close();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    void givenNotificationFailsFewTimes_whenNotifyUserWithBookReviews_thenRetriesUntilSuccess(
            int failures) {
        User user = Mockito.mock(User.class);
        when(databaseServiceMock.getBookByISBN(VALID_ISBN)).thenReturn(createValidBookMock());
        when(databaseServiceMock.getUserById(VALID_USER_ID)).thenReturn(user);
        when(reviewServiceMock.getReviewsForBook(VALID_ISBN)).thenReturn(List.of("good book"));
        // Fail 'failures' times in a row and then succeed
        NotificationException[] errors = new NotificationException[failures];
        for (int i = 0; i < failures; i++) {
            errors[i] = new NotificationException("error");
        }
        Mockito.doThrow(errors).doNothing().when(user).sendNotification(anyString());
        assertDoesNotThrow(() -> library.notifyUserWithBookReviews(VALID_ISBN, VALID_USER_ID));
        verify(user, times(failures + 1)).sendNotification(anyString());
        verify(reviewServiceMock).close();
    }

    @Test
    void givenNotificationAlwaysFails_whenNotifyUserWithBookReviews_thenThrowsAfterFiveAttempts() {
        User user = Mockito.mock(User.class);
        when(databaseServiceMock.getBookByISBN(VALID_ISBN)).thenReturn(createValidBookMock());
        when(databaseServiceMock.getUserById(VALID_USER_ID)).thenReturn(user);
        when(reviewServiceMock.getReviewsForBook(VALID_ISBN)).thenReturn(List.of("good book"));
        Mockito.doThrow(new NotificationException("error")).when(user).sendNotification(anyString());
        NotificationException ex = assertThrows(NotificationException.class,
                () -> library.notifyUserWithBookReviews(VALID_ISBN, VALID_USER_ID));
        assertEquals("Notification failed!", ex.getMessage());
        verify(user, times(5)).sendNotification(anyString());
        verify(reviewServiceMock).close();
    }

    // getBookByISBN

    @ParameterizedTest
    @MethodSource("invalidIsbnProvider")
    void givenInvalidIsbn_whenGetBookByISBN_thenThrowsInvalidIsbn(String isbn) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> library.getBookByISBN(isbn, VALID_USER_ID));
        assertEquals("Invalid ISBN.", ex.getMessage());
        verifyNoInteractions(databaseServiceMock);
        verifyNoInteractions(reviewServiceMock);
    }

    @ParameterizedTest
    @MethodSource("invalidUserIdProvider")
    void givenInvalidUserId_whenGetBookByISBN_thenThrowsInvalidUserId(String userId) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> library.getBookByISBN(VALID_ISBN, userId));
        assertEquals("Invalid user Id.", ex.getMessage());
        verifyNoInteractions(databaseServiceMock);
        verifyNoInteractions(reviewServiceMock);
    }

    @Test
    void givenBookNotInDatabase_whenGetBookByISBN_thenThrowsBookNotFound() {
        when(databaseServiceMock.getBookByISBN(VALID_ISBN)).thenReturn(null);
        BookNotFoundException ex = assertThrows(BookNotFoundException.class,
                () -> library.getBookByISBN(VALID_ISBN, VALID_USER_ID));
        assertEquals("Book not found!", ex.getMessage());
        verifyNoInteractions(reviewServiceMock);
    }

    @Test
    void givenBookAlreadyBorrowed_whenGetBookByISBN_thenThrowsBookAlreadyBorrowed() {
        Book book = createBookMock(VALID_ISBN, VALID_TITLE, VALID_AUTHOR, true);
        when(databaseServiceMock.getBookByISBN(VALID_ISBN)).thenReturn(book);
        BookAlreadyBorrowedException ex = assertThrows(BookAlreadyBorrowedException.class,
                () -> library.getBookByISBN(VALID_ISBN, VALID_USER_ID));
        assertEquals("Book was already borrowed!", ex.getMessage());
        verifyNoInteractions(reviewServiceMock);
    }

    @Test
    void givenAvailableBookAndRegisteredUser_whenGetBookByISBN_thenUserNotifiedAndBookReturned() {
        Book book = createBookMock(VALID_ISBN, "Dune", VALID_AUTHOR, false);
        User user = Mockito.mock(User.class);
        when(databaseServiceMock.getBookByISBN(VALID_ISBN)).thenReturn(book);
        when(databaseServiceMock.getUserById(VALID_USER_ID)).thenReturn(user);
        when(reviewServiceMock.getReviewsForBook(VALID_ISBN)).thenReturn(List.of("good book"));
        Book returnedBook = library.getBookByISBN(VALID_ISBN, VALID_USER_ID);
        assertSame(book, returnedBook);
        verify(user).sendNotification("Reviews for 'Dune':\ngood book");
        verify(reviewServiceMock).close();
    }

    @Test
    void givenReviewServiceFails_whenGetBookByISBN_thenBookStillReturned() {
        Book book = createValidBookMock();
        User user = Mockito.mock(User.class);
        when(databaseServiceMock.getBookByISBN(VALID_ISBN)).thenReturn(book);
        when(databaseServiceMock.getUserById(VALID_USER_ID)).thenReturn(user);
        when(reviewServiceMock.getReviewsForBook(VALID_ISBN))
                .thenThrow(new ReviewException("Review Service Unavailable"));
        Book returnedBook = library.getBookByISBN(VALID_ISBN, VALID_USER_ID);
        assertSame(book, returnedBook);
        verify(reviewServiceMock).close();
        verifyNoInteractions(user);
    }

    @Test
    void givenNotificationAlwaysFails_whenGetBookByISBN_thenBookStillReturned() {
        Book book = createValidBookMock();
        User user = Mockito.mock(User.class);
        when(databaseServiceMock.getBookByISBN(VALID_ISBN)).thenReturn(book);
        when(databaseServiceMock.getUserById(VALID_USER_ID)).thenReturn(user);
        when(reviewServiceMock.getReviewsForBook(VALID_ISBN)).thenReturn(List.of("good book"));
        Mockito.doThrow(new NotificationException("error")).when(user).sendNotification(anyString());
        Book returnedBook = library.getBookByISBN(VALID_ISBN, VALID_USER_ID);
        assertSame(book, returnedBook);
        verify(user, times(5)).sendNotification(anyString());
    }

    @Test
    void givenUserNotRegistered_whenGetBookByISBN_thenBookStillReturnedWithoutFetchingReviews() {
        Book book = createValidBookMock();
        when(databaseServiceMock.getBookByISBN(VALID_ISBN)).thenReturn(book);
        when(databaseServiceMock.getUserById(VALID_USER_ID)).thenReturn(null);
        Book returnedBook = library.getBookByISBN(VALID_ISBN, VALID_USER_ID);
        assertSame(book, returnedBook);
        verifyNoInteractions(reviewServiceMock);
    }
}
