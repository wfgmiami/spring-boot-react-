package hello;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SimpleBookRepository implements BookRepository {

    @Override
    @Cacheable("books")
    public Book getByIsbn(String isbn) {
        return new Book(isbn, "Some book");
    }

    @Autowired
    private List<Book> book;

    @Override
    @Cacheable("books")
    public List<Book> getAll() {
        return book;
    }

//    // Don't do this at home
//    private void simulateSlowService() {
//        try {
//            long time = 3000L;
//            Thread.sleep(time);
//        } catch (InterruptedException e) {
//            throw new IllegalStateException(e);
//        }
//    }

}
