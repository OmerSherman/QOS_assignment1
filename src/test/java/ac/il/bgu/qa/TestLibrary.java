package ac.il.bgu.qa;

import ac.il.bgu.qa.errors.*;
import ac.il.bgu.qa.services.DatabaseService;
import ac.il.bgu.qa.services.NotificationService;
import ac.il.bgu.qa.services.ReviewService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;

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
    DatabaseService databaseServiceMock;

    @Mock
    ReviewService reviewServiceMock;

    @Mock
    NotificationService notificationServiceMock;

    private Library library;

    @BeforeAll
    static void setupAll() {
        System.out.println("Initializing resources for all the tests");
        // This method will be executed before all test methods of this group (e.g., Database Connection)
    }

    @BeforeEach
    void init(){
        MockitoAnnotations.openMocks(this); // This function init the mocks before each test
        library = new Library(databaseServiceMock, reviewServiceMock);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",                    // empty
            "978030640615",        // 12 digits
            "97803064061577",      // 14 digits
            "978030640615X",       // not a digit
            "9780306406158"        // Wrong check-sum
    })
    void givenInValidISBN_whenGetBookByISBN_thenThrowsIllegalArgumentException(String isbn ){
        //1. Arrange
        //1.1 create an instance of library with mocks
        Library library = new Library(databaseServiceMock, reviewServiceMock);

        //2. Act
        IllegalArgumentException ex = null;
        try{
            library.getBookByISBN(isbn, "user");
        }catch (IllegalArgumentException exception){
            ex = exception;
        }

        //3.  Assert
        assertNotNull(ex);
        assertEquals("Invalid ISBN.", ex.getMessage());

    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            "",
            "123456789", //to short
            "12345678910111213"// to long
    })
    void givenInValidUserID_whenGetBookByISBN_thenThrowsIllegalArgumentException(String user){
        //1. Arrange
        //1.1 create an instance of library with mocks
        Library library = new Library(databaseServiceMock, reviewServiceMock);

        //2 act
        IllegalArgumentException ex = null;
        try{
            library.getBookByISBN(VALID_ISBN, user);
        }catch (IllegalArgumentException exception){
            ex = exception;
        }

        // 3. Assertion
        assertNotNull(ex);
        Assertions.assertEquals("Invalid user Id." , ex.getMessage());
    }

    @Test
    void givenBookNotinDB_whenGetBookByISBN_thenThrowsBookNotFoundException(){
        //1. Arrange
        //1.1 create an instance of library with mocks
        Library library = new Library(databaseServiceMock, reviewServiceMock);
        //1.2  Stubbing - Define behavior for databaseServiceMock
        Mockito.when(databaseServiceMock.getBookByISBN(VALID_ISBN)).thenReturn(null);

        //2. Act
        //throw new BookNotFoundException("Book not found!");
        BookNotFoundException ex = null;
        try {
            library.getBookByISBN(VALID_ISBN,VALID_USER_ID);
        }catch (BookNotFoundException exception){
            ex = exception;
        }

        //3.Assertion
        assertNotNull(ex);
        assertEquals("Book not found!" , ex.getMessage());
    }

    @Test
    void givenBookIsAlreadyBorrowed_whenGetBookByISBN_thenThrowsBookAlreadyBorrowedException(){
        //1. Arrange
        //1.1 create an instance of library  and book with mocks
        Library library = new Library(databaseServiceMock, reviewServiceMock);
        Book mockBook = Mockito.mock(Book.class);
        //1.2  Stubbing - Define behavior for databaseServiceMock
        Mockito.when(databaseServiceMock.getBookByISBN(VALID_ISBN)).thenReturn(mockBook);
        //1.3  Stubbing - Define behavior for Book
        Mockito.when(mockBook.isBorrowed()).thenReturn(true);

        //2. Act
        BookAlreadyBorrowedException ex = null;
        try {
            library.getBookByISBN(VALID_ISBN, VALID_USER_ID);
        } catch (BookAlreadyBorrowedException exception){
            ex = exception;
        }

        //3.Assertion
        assertNotNull(ex);
        assertEquals("Book was already borrowed!" , ex.getMessage());

    }

    @Test
    void givenAvailableBookAndValidUserAndfailedReviews_whenGetBookByISBN_thenStillReturnBook(){
        //1.Arrange
        Library library = new Library(databaseServiceMock, reviewServiceMock);
        Book mockBook = Mockito.mock(Book.class);
        User mockUser = Mockito.mock(User.class);

        //1.2  Stubbing - Define behavior for databaseServiceMock and ReviewsServices
        Mockito.when(databaseServiceMock.getBookByISBN(VALID_ISBN)).thenReturn(mockBook);
        Mockito.when(databaseServiceMock.getUserById(VALID_USER_ID)).thenReturn(mockUser);
        //1.3  Stubbing - Define behavior for Book
        Mockito.when(mockBook.isBorrowed()).thenReturn(false);

        //2. Act
        Book returnedBook = library.getBookByISBN(VALID_ISBN, VALID_USER_ID);

        //3. Assertion
        assertSame(returnedBook, mockBook);
    }

    @Test
    void givenAvailableBookAndValidUser_whenGetBookByISBN_thenNotifyUserandReturnBook(){
        //1.Arrange
        Library library = new Library(databaseServiceMock, reviewServiceMock);
        Book mockBook = Mockito.mock(Book.class);
        User mockUser = Mockito.mock(User.class);

        //1.2  Stubbing - Define behavior for databaseServiceMock and ReviewsServices
        Mockito.when(databaseServiceMock.getBookByISBN(VALID_ISBN)).thenReturn(mockBook);
        Mockito.when(databaseServiceMock.getUserById(VALID_USER_ID)).thenReturn(mockUser);
        Mockito.when(reviewServiceMock.getReviewsForBook(VALID_ISBN)).thenReturn(List.of("good book", "interesting book"));
        //1.3  Stubbing - Define behavior for Book
        Mockito.when(mockBook.isBorrowed()).thenReturn(false);

        //2. Act
        Book returnedBook = library.getBookByISBN(VALID_ISBN, VALID_USER_ID);

        //3. Assertion
        assertSame(returnedBook, mockBook);
        verify(mockUser).sendNotification(Mockito.anyString());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            "",                    // empty
            "978030640615",        // 12 digits
            "97803064061577",      // 14 digits
            "978030640615X",       // not a digit
            "9780306406158"        // Wrong check-sum
    })
    void givenInValidISBN_whenNotifyUserWithBookReviews_thenThrowsIllegalArgumentException(String isbn){
        //1.Arrange
        //1.1 create an instance of library with mocks
        Library library = new Library(databaseServiceMock, reviewServiceMock);

        //2 act
        IllegalArgumentException ex = null;
        try{
            library.notifyUserWithBookReviews(isbn, VALID_USER_ID);
        }catch (IllegalArgumentException exception){
            ex = exception;
        }

        // 3. Assertion
        assertNotNull(ex);
        Assertions.assertEquals("Invalid ISBN." , ex.getMessage());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            "",
            "123456789", //to short
            "12345678910111213"// to long
    })
    void givenInValidUserID_whenNotifyUserWithBookReviews_thenThrowsIllegalArgumentException(String user){
        //1. Arrange
        //1.1 create an instance of library with mocks
        Library library = new Library(databaseServiceMock, reviewServiceMock);

        //2 act
        IllegalArgumentException ex = null;
        try{
            library.notifyUserWithBookReviews(VALID_ISBN, user);
        }catch (IllegalArgumentException exception){
            ex = exception;
        }

        // 3. Assertion
        assertNotNull(ex);
        Assertions.assertEquals("Invalid user Id." , ex.getMessage());
    }

    @Test
    void givenBookNotFound_whenNotifyUserWithBookReviews_thenThrowsBookNotFoundException(){
        //1.Arrange
        //1.1 create an instance of library with mocks
        Library library = new Library(databaseServiceMock, reviewServiceMock);
        //1.2  Stubbing - Define behavior for databaseServiceMock and ReviewsServices
        Mockito.when(databaseServiceMock.getBookByISBN(VALID_ISBN)).thenReturn(null);

        //2.Act
        BookNotFoundException ex = null;
        try {
            library.notifyUserWithBookReviews(VALID_ISBN,VALID_USER_ID);
        }catch (BookNotFoundException exception){
            ex = exception;
        }

        //3. Assertion
        assertNotNull(ex);
        Assertions.assertEquals("Book not found!" , ex.getMessage());
    }

    @Test
    void givenUserNotRegisted_whenNotifyUserWithBookReviews_thenThrowsUserNotRegisteredException(){
        //1.Arrange
        //1.1 create an instance of library with mocks
        Library library = new Library(databaseServiceMock, reviewServiceMock);
        Book mockBook = Mockito.mock(Book.class);
        //1.2  Stubbing - Define behavior for databaseServiceMock
        Mockito.when(databaseServiceMock.getBookByISBN(VALID_ISBN)).thenReturn(mockBook);
        Mockito.when(databaseServiceMock.getUserById(VALID_USER_ID)).thenReturn(null);

        //2.Act
        UserNotRegisteredException ex = null;
        try {
            library.notifyUserWithBookReviews(VALID_ISBN,VALID_USER_ID);
        }catch (UserNotRegisteredException exception){
            ex = exception;
        }

        //3. Assertion
        assertNotNull(ex);
        Assertions.assertEquals("User not found!" , ex.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void givenNoReviews_whenNotifyUserWithBookReviews_thenThrowsNoReviewsFoundException(List<String> reviews){
        //1.Arrange
        //1.1 create an instance of library with mocks
        Library library = new Library(databaseServiceMock, reviewServiceMock);
        Book mockBook = Mockito.mock(Book.class);
        User mockUser = Mockito.mock(User.class);
        //1.2  Stubbing - Define behavior for databaseServiceMock
        Mockito.when(databaseServiceMock.getBookByISBN(VALID_ISBN)).thenReturn(mockBook);
        Mockito.when(databaseServiceMock.getUserById(VALID_USER_ID)).thenReturn(mockUser);
        Mockito.when(reviewServiceMock.getReviewsForBook(VALID_ISBN)).thenReturn(reviews);
        //2.Act
        NoReviewsFoundException ex = null;
        try {
            library.notifyUserWithBookReviews(VALID_ISBN,VALID_USER_ID);
        }catch (NoReviewsFoundException exception){
            ex = exception;
        }

        //3. Assertion
        assertNotNull(ex);
        assertEquals("No reviews found!" ,ex.getMessage());
        verify(reviewServiceMock).close();


    }
    @Test
    void givenUnavailableReviesService_whenNotifyUserWithBookReviews_thenThrowsReviewServiceUnavailableException(){
        //1.Arrange
        //1.1 create an instance of library with mocks
        Library library = new Library(databaseServiceMock, reviewServiceMock);
        Book mockBook = Mockito.mock(Book.class);
        User mockUser = Mockito.mock(User.class);

        //1.2  Stubbing - Define behavior for databaseServiceMock
        Mockito.when(databaseServiceMock.getBookByISBN(VALID_ISBN)).thenReturn(mockBook);
        Mockito.when(databaseServiceMock.getUserById(VALID_USER_ID)).thenReturn(mockUser);
        Mockito.when(reviewServiceMock.getReviewsForBook(VALID_ISBN)).thenThrow(new ReviewException("Review Service Unavailable"));
        //2.Act
        ReviewServiceUnavailableException ex = null;
        try {
            library.notifyUserWithBookReviews(VALID_ISBN,VALID_USER_ID);
        }catch (ReviewServiceUnavailableException exception){
            ex = exception;
        }

        //3. Assertion
        assertNotNull(ex);
        assertEquals("Review service unavailable!" ,ex.getMessage());
        verify(reviewServiceMock).close();
    }

    @Test
    void givenNotificationError_whenNotifyUserWithBookReviews_thenThrowsNotificationException(){
        //1.Arrange
        //1.1 create an instance of library with mocks
        Library library = new Library(databaseServiceMock, reviewServiceMock);
        Book mockBook = Mockito.mock(Book.class);
        User mockUser = Mockito.mock(User.class);

        //1.2  Stubbing - Define behavior for databaseServiceMock
        Mockito.when(databaseServiceMock.getBookByISBN(VALID_ISBN)).thenReturn(mockBook);
        Mockito.when(databaseServiceMock.getUserById(VALID_USER_ID)).thenReturn(mockUser);
        Mockito.when(reviewServiceMock.getReviewsForBook(VALID_ISBN)).thenReturn(List.of("good book" , "very interseting"));
        Mockito.doThrow(new NotificationException("error"))
                .when(mockUser).sendNotification(Mockito.anyString());
        //2.Act
        NotificationException ex = null;
        try {
            library.notifyUserWithBookReviews(VALID_ISBN,VALID_USER_ID);
        }catch (NotificationException exception){
            ex = exception;
        }

        //3. Assertion
        assertNotNull(ex);
        assertEquals("Notification failed!" ,ex.getMessage());
        verify(reviewServiceMock).close();
        verify(mockUser, Mockito.times(5)).sendNotification(Mockito.anyString());
    }

    @Test
    void givenValidArguments_whenNotifyUserWithBookReviews_thenUserNotified(){
        //1.Arrange
        //1.1 create an instance of library with mocks
        Library library = new Library(databaseServiceMock, reviewServiceMock);
        Book mockBook = Mockito.mock(Book.class);
        User mockUser = Mockito.mock(User.class);

        //1.2  Stubbing - Define behavior for databaseServiceMock
        Mockito.when(databaseServiceMock.getBookByISBN(VALID_ISBN)).thenReturn(mockBook);
        Mockito.when(databaseServiceMock.getUserById(VALID_USER_ID)).thenReturn(mockUser);
        Mockito.when(mockBook.getTitle()).thenReturn("book");
        Mockito.when(reviewServiceMock.getReviewsForBook(VALID_ISBN)).thenReturn(List.of("good book" , "very interseting"));

        //2. Act
        library.notifyUserWithBookReviews(VALID_ISBN, VALID_USER_ID);

        //3.Assert
        Mockito.verify(mockUser, Mockito.times(1))
                .sendNotification("Reviews for 'book':\ngood book\nvery interseting");
        Mockito.verify(reviewServiceMock).close();
    }

    // Helpers and data providers

    private Book createBookMock(String isbn, String title, String author, boolean borrowed) {
        Book book = Mockito.mock(Book.class);
        when(book.getISBN()).thenReturn(isbn);
        when(book.getTitle()).thenReturn(title);
        when(book.getAuthor()).thenReturn(author);
        when(book.isBorrowed()).thenReturn(borrowed);
        return book;
    }

    private Book createValidBookMock() {
        return createBookMock(VALID_ISBN, VALID_TITLE, VALID_AUTHOR, false);
    }

    private User createUserMock(String id, String name, NotificationService notificationService) {
        User user = Mockito.mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getName()).thenReturn(name);
        when(user.getNotificationService()).thenReturn(notificationService);
        return user;
    }

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
    void givenNullBook_whenAddBook_thenThrowsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> library.addBook(null));
        assertEquals("Invalid book.", ex.getMessage());
        verifyNoInteractions(databaseServiceMock);
    }

    @ParameterizedTest
    @MethodSource("invalidIsbnProvider")
    void givenInvalidIsbn_whenAddBook_thenThrowsIllegalArgumentException(String isbn) {
        Book book = createBookMock(isbn, VALID_TITLE, VALID_AUTHOR, false);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> library.addBook(book));
        assertEquals("Invalid ISBN.", ex.getMessage());
        verifyNoInteractions(databaseServiceMock);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void givenNullOrEmptyTitle_whenAddBook_thenThrowsIllegalArgumentException(String title) {
        Book book = createBookMock(VALID_ISBN, title, VALID_AUTHOR, false);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> library.addBook(book));
        assertEquals("Invalid title.", ex.getMessage());
        verifyNoInteractions(databaseServiceMock);
    }

    @ParameterizedTest
    @MethodSource("invalidAuthorProvider")
    void givenInvalidAuthor_whenAddBook_thenThrowsIllegalArgumentException(String author) {
        Book book = createBookMock(VALID_ISBN, VALID_TITLE, author, false);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> library.addBook(book));
        assertEquals("Invalid author.", ex.getMessage());
        verifyNoInteractions(databaseServiceMock);
    }

    @Test
    void givenBorrowedBook_whenAddBook_thenThrowsIllegalArgumentException() {
        Book book = createBookMock(VALID_ISBN, VALID_TITLE, VALID_AUTHOR, true);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> library.addBook(book));
        assertEquals("Book with invalid borrowed state.", ex.getMessage());
        verifyNoInteractions(databaseServiceMock);
    }

    @Test
    void givenBookAlreadyInDatabase_whenAddBook_thenThrowsIllegalArgumentException() {
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
    void givenNullUser_whenRegisterUser_thenThrowsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> library.registerUser(null));
        assertEquals("Invalid user.", ex.getMessage());
        verifyNoInteractions(databaseServiceMock);
    }

    @ParameterizedTest
    @MethodSource("invalidUserIdProvider")
    void givenInvalidUserId_whenRegisterUser_thenThrowsIllegalArgumentException(String userId) {
        User user = createUserMock(userId, VALID_USER_NAME, notificationServiceMock);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> library.registerUser(user));
        assertEquals("Invalid user Id.", ex.getMessage());
        verifyNoInteractions(databaseServiceMock);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void givenNullOrEmptyUserName_whenRegisterUser_thenThrowsIllegalArgumentException(String name) {
        User user = createUserMock(VALID_USER_ID, name, notificationServiceMock);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> library.registerUser(user));
        assertEquals("Invalid user name.", ex.getMessage());
        verifyNoInteractions(databaseServiceMock);
    }

    @Test
    void givenNullNotificationService_whenRegisterUser_thenThrowsIllegalArgumentException() {
        User user = createUserMock(VALID_USER_ID, VALID_USER_NAME, null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> library.registerUser(user));
        assertEquals("Invalid notification service.", ex.getMessage());
        verifyNoInteractions(databaseServiceMock);
    }

    @Test
    void givenUserAlreadyExists_whenRegisterUser_thenThrowsIllegalArgumentException() {
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
    void givenInvalidIsbn_whenBorrowBook_thenThrowsIllegalArgumentException(String isbn) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> library.borrowBook(isbn, VALID_USER_ID));
        assertEquals("Invalid ISBN.", ex.getMessage());
        verifyNoInteractions(databaseServiceMock);
    }

    @Test
    void givenBookNotInDatabase_whenBorrowBook_thenThrowsBookNotFoundException() {
        when(databaseServiceMock.getBookByISBN(VALID_ISBN)).thenReturn(null);
        BookNotFoundException ex = assertThrows(BookNotFoundException.class,
                () -> library.borrowBook(VALID_ISBN, VALID_USER_ID));
        assertEquals("Book not found!", ex.getMessage());
        verify(databaseServiceMock, never()).borrowBook(anyString(), anyString());
    }

    @ParameterizedTest
    @MethodSource("invalidUserIdProvider")
    void givenInvalidUserId_whenBorrowBook_thenThrowsIllegalArgumentException(String userId) {
        Book book = createValidBookMock();
        when(databaseServiceMock.getBookByISBN(VALID_ISBN)).thenReturn(book);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> library.borrowBook(VALID_ISBN, userId));
        assertEquals("Invalid user Id.", ex.getMessage());
        verify(book, never()).borrow();
        verify(databaseServiceMock, never()).borrowBook(anyString(), anyString());
    }

    @Test
    void givenUserNotRegistered_whenBorrowBook_thenThrowsUserNotRegisteredException() {
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
    void givenBookAlreadyBorrowed_whenBorrowBook_thenThrowsBookAlreadyBorrowedException() {
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
    void givenInvalidIsbn_whenReturnBook_thenThrowsIllegalArgumentException(String isbn) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> library.returnBook(isbn));
        assertEquals("Invalid ISBN.", ex.getMessage());
        verifyNoInteractions(databaseServiceMock);
    }

    @Test
    void givenBookNotInDatabase_whenReturnBook_thenThrowsBookNotFoundException() {
        when(databaseServiceMock.getBookByISBN(VALID_ISBN)).thenReturn(null);
        BookNotFoundException ex = assertThrows(BookNotFoundException.class,
                () -> library.returnBook(VALID_ISBN));
        assertEquals("Book not found!", ex.getMessage());
        verify(databaseServiceMock, never()).returnBook(anyString());
    }

    @Test
    void givenBookNotBorrowed_whenReturnBook_thenThrowsBookNotBorrowedException() {
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

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    void givenNotificationFailsFewTimes_whenNotifyUserWithBookReviews_thenRetriesUntilSuccess(
            int failures) {
        User user = Mockito.mock(User.class);
        when(databaseServiceMock.getBookByISBN(VALID_ISBN)).thenReturn(createValidBookMock());
        when(databaseServiceMock.getUserById(VALID_USER_ID)).thenReturn(user);
        when(reviewServiceMock.getReviewsForBook(VALID_ISBN)).thenReturn(List.of("good book"));
        NotificationException[] errors = new NotificationException[failures];
        for (int i = 0; i < failures; i++) {
            errors[i] = new NotificationException("error");
        }
        Mockito.doThrow(errors).doNothing().when(user).sendNotification(anyString());
        assertDoesNotThrow(() -> library.notifyUserWithBookReviews(VALID_ISBN, VALID_USER_ID));
        verify(user, times(failures + 1)).sendNotification(anyString());
        verify(reviewServiceMock).close();
    }
}
