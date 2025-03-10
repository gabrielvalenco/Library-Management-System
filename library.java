import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.*;
import org.springframework.ui.Model;
import javax.persistence.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@SpringBootApplication
public class LibraryApplication {
    public static void main(String[] args) {
        SpringApplication.run(LibraryApplication.class, args);
    }
}

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
class Book {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String title;
    private String author;
    private int publicationYear;
    private String isbn;
    private String genre;
    private boolean available = true;
    
    @ManyToMany(mappedBy = "books")
    private List<User> users = new ArrayList<>();
}

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
class User {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String email;
    private String phoneNumber;
    
    @ManyToMany
    @JoinTable(
        name = "Loan",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "book_id")
    )
    private List<Book> books = new ArrayList<>();
}

interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findByTitleContainingIgnoreCase(String title);
    List<Book> findByAuthorContainingIgnoreCase(String author);
    List<Book> findByGenre(String genre);
    List<Book> findByAvailable(boolean available);
}

interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
}

@RestController
@RequestMapping("/api/books")
class BookController {
    @Autowired
    private BookRepository bookRepository;
    
    @GetMapping
    public List<Book> listAll() {
        return bookRepository.findAll();
    }
    
    @GetMapping("/{id}")
    public Book getById(@PathVariable Long id) {
        return bookRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Book not found with id: " + id));
    }
    
    @GetMapping("/search")
    public List<Book> search(@RequestParam(required = false) String title,
                            @RequestParam(required = false) String author,
                            @RequestParam(required = false) String genre) {
        if (title != null) {
            return bookRepository.findByTitleContainingIgnoreCase(title);
        } else if (author != null) {
            return bookRepository.findByAuthorContainingIgnoreCase(author);
        } else if (genre != null) {
            return bookRepository.findByGenre(genre);
        }
        return bookRepository.findAll();
    }
    
    @GetMapping("/available")
    public List<Book> findAvailable() {
        return bookRepository.findByAvailable(true);
    }
    
    @PostMapping
    public Book add(@RequestBody Book book) {
        return bookRepository.save(book);
    }
    
    @PutMapping("/{id}")
    public Book update(@PathVariable Long id, @RequestBody Book bookDetails) {
        Book book = bookRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Book not found with id: " + id));
            
        book.setTitle(bookDetails.getTitle());
        book.setAuthor(bookDetails.getAuthor());
        book.setPublicationYear(bookDetails.getPublicationYear());
        book.setIsbn(bookDetails.getIsbn());
        book.setGenre(bookDetails.getGenre());
        book.setAvailable(bookDetails.isAvailable());
        
        return bookRepository.save(book);
    }
    
    @DeleteMapping("/{id}")
    public Map<String, Boolean> delete(@PathVariable Long id) {
        Book book = bookRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Book not found with id: " + id));
            
        bookRepository.delete(book);
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("deleted", Boolean.TRUE);
        return response;
    }
}

@RestController
@RequestMapping("/api/users")
class UserController {
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private BookRepository bookRepository;
    
    @GetMapping
    public List<User> listAll() {
        return userRepository.findAll();
    }
    
    @GetMapping("/{id}")
    public User getById(@PathVariable Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }
    
    @PostMapping
    public User add(@RequestBody User user) {
        return userRepository.save(user);
    }
    
    @PostMapping("/{userId}/borrow/{bookId}")
    public User borrowBook(@PathVariable Long userId, @PathVariable Long bookId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
            
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new RuntimeException("Book not found with id: " + bookId));
            
        if (!book.isAvailable()) {
            throw new RuntimeException("Book is not available for borrowing");
        }
        
        book.setAvailable(false);
        user.getBooks().add(book);
        
        bookRepository.save(book);
        return userRepository.save(user);
    }
    
    @PostMapping("/{userId}/return/{bookId}")
    public User returnBook(@PathVariable Long userId, @PathVariable Long bookId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
            
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new RuntimeException("Book not found with id: " + bookId));
            
        if (!user.getBooks().contains(book)) {
            throw new RuntimeException("User has not borrowed this book");
        }
        
        book.setAvailable(true);
        user.getBooks().remove(book);
        
        bookRepository.save(book);
        return userRepository.save(user);
    }
}

@Controller
class WebController {
    @Autowired
    private BookRepository bookRepository;
    
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("books", bookRepository.findAll());
        return "index";
    }
    
    @GetMapping("/books")
    public String books(Model model) {
        model.addAttribute("books", bookRepository.findAll());
        return "books";
    }
    
    @GetMapping("/users")
    public String users(Model model) {
        return "users";
    }
}
