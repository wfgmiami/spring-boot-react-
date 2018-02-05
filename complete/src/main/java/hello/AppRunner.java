package hello;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Component
public class AppRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AppRunner.class);

    private final BookRepository bookRepository;

    private static final String CSV_FILE_PATH = "./munis.csv";
    private List<Book> books;

    public AppRunner(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info(".... Fetching books");

        try(
                Reader reader = Files.newBufferedReader(Paths.get(CSV_FILE_PATH));
        ){
            CsvToBean csvToBean = new CsvToBeanBuilder(reader)
                    .withType(Book.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();

            books = csvToBean.parse();
            for(Book b:books){
                System.out.println(b);
//                this.repository.save(m);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

//        logger.info("isbn-1234 -->" + bookRepository.getAll());
        logger.info("isbn-4567 -->" + bookRepository.getByIsbn("isbn-4567"));
//        logger.info("isbn-1234 -->" + bookRepository.getByIsbn("isbn-1234"));
//        logger.info("isbn-4567 -->" + bookRepository.getByIsbn("isbn-4567"));
//        logger.info("isbn-1234 -->" + bookRepository.getByIsbn("isbn-1234"));
//        logger.info("isbn-1234 -->" + bookRepository.getByIsbn("isbn-1234"));
    }

}