/**
 * Created by hakiba on 4/25/2015.
 */
public class MangaChapter{
    public String language;
    public String title;
    public String chapter;
    public MangaChapter(String language, String title, String chapter){
        this.language = language;
        this.title = title;
        this.chapter = chapter;
    }
    public String toString(){
        return "language: " + language + ", title: " + title + ", chapter: " + chapter;
    }
}
