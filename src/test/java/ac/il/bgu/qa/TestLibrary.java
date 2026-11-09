package ac.il.bgu.qa;

import ac.il.bgu.qa.errors.*;
import ac.il.bgu.qa.services.DatabaseService;
import ac.il.bgu.qa.services.ReviewService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;


public class TestLibrary {


    private static final String VALID_ISBN = "978-0-306-40615-7";
    private static final String VALID_USER_ID = "123456789012";

    @Mock
    DatabaseService databaseServiceMock;

    @Mock
    ReviewService reviewServiceMock;

    @BeforeAll
    static void setupAll() {
        System.out.println("Initializing resources for all the tests");
        // This method will be executed before all test methods of this group (e.g., Database Connection)
    }

    @BeforeEach
    void init(){
        MockitoAnnotations.openMocks(this); // This function init the mocks before each test
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",                    // empty
            "978030640615",        // 12 digits
            "97803064061577",      // 14 digits
            "978030640615X",       // not a digit
            "9780306406158"        // Wrong check-sum
    })
    void givenInValidISBN_whengetBookByISBNThenThrow(String isbn ){
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
    void givenInValidUserID_whengetBookByISBNThenTrow(String user){
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
    void givenBookNotinDB_whengetBookByISBNThenThrows(){
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
    void givenBookIsAlreadyBorrowed_whengetBookByISBNThenThrows(){
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
    void givenAvailableBookAndValidUserAndfailedReviews_thenstillReturnBook(){
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
    void givenAvailableBookAndValidUser_whengetBookByISBN_thenNotifyUserandReturnBook(){
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
    void givenInValidISBN_whennotifyUserWithBookReviews_thenThrows(String isbn){
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
    void givenInValidUserID_whennotifyUserWithBookReviews_thenThrows(String user){
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
    void givenBookNotFound_whenNotifyUserWithBookReviews_thenThrows(){
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
    void givenUserNotRegisted_when_notifyUserWithBookReviews_thenThrows(){
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
    void givenNoReviews_when_notifyUserWithBookReviews_thenThrows(List<String> reviews){
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
    void givenUnavailableReviesService_when_notifyUserWithBookReviews_thenThrows(){
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
    void givenNotificationError_when_notifyUserWithBookReviews_thenThrows(){
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
    void givenValidArguments_when_notifyUserWithBookReviews_thenUserNotified(){
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
}


