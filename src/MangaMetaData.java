/**
 * Created by hakiba on 4/25/2015.
 */
public class MangaMetaData{
    public String language;
    public String title;
    public String chapter;
    public MangaMetaData(String language, String title, String chapter){
        this.language = language;
        this.title = title;
        this.chapter = chapter;
    }
    public String toString(){
        return "language: " + language + ", title: " + title + ", chapter: " + chapter;
    }
    public void incrementChapterNumber(){
        chapter =  String.valueOf( Integer.parseInt(chapter) + 1 );
    }
}
