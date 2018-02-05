package hello;

import lombok.Data;

@Data
public class Book {

        private String cusip;
        private String price;
        private String coupon;
        private String maturity;
        private String ytm;
        private String sector;
        private String rating;
        private String state;
        private String lastTraded;
        private String ed;
        private String md;
        private String ytw;

        public Book(String cusip, String sector) {
            this.cusip = cusip;
            this.sector = sector;
        }
        public Book(){}

        public String getState(){
            return state;
        }
        public String getCusip(){
            return cusip;
        }

        public String getYtm(){
            return ytm;
        }

        public String getPrice(){
            return price;
        }

//    private String isbn;
//    private String title;
//
//    public Book(String isbn, String title) {
//        this.isbn = isbn;
//        this.title = title;
//    }
//
//    public String getIsbn() {
//        return isbn;
//    }
//
//    public void setIsbn(String isbn) {
//        this.isbn = isbn;
//    }
//
//    public String getTitle() {
//        return title;
//    }
//
//    public void setTitle(String title) {
//        this.title = title;
//    }
//
//    @Override
//    public String toString() {
//        return "Book{" + "isbn='" + isbn + '\'' + ", title='" + title + '\'' + '}';
//    }

}