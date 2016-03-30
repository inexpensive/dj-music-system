package player;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.mp4.MP4Parser;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.*;

public class LocalSong extends Song {

    private final String fileLocation;
    private String title, artist, album;

    public LocalSong(String fileLocation){
        this.fileLocation = fileLocation;
        try {
            setSkipped(false);
            InputStream in = new FileInputStream(new File(fileLocation));
            ContentHandler handler = new DefaultHandler();
            Metadata metadata = new Metadata();
            Parser parser = new MP4Parser();
            ParseContext parseContext = new ParseContext();
            parser.parse(in, handler, metadata, parseContext);
            in.close();
            this.title = metadata.get("dc:title");
            this.artist = metadata.get("xmpDM:artist");
            this.album = metadata.get("xmpDM:album");
        } catch (SAXException | IOException | TikaException e) {
            e.printStackTrace();
        }


    }

    @Override
    public Source getSource() {
        return Source.LOCAL;
    }

    String getFileLocation(){
        return fileLocation;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    @Override
    public String getSongDetails() {
        return title + " by " + artist + " on " + album;
    }
}
