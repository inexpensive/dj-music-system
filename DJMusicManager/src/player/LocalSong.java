package player;

import org.apache.commons.io.FilenameUtils;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.mp3.Mp3Parser;
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
            Parser parser = null;
            String ext = FilenameUtils.getExtension(fileLocation);
            if (ext.contentEquals("m4a")) {
                parser = new MP4Parser();
            }
            else if (ext.contentEquals("mp3")){
                parser = new Mp3Parser();
            }
            ParseContext parseContext = new ParseContext();
            if (parser != null) {
                parser.parse(in, handler, metadata, parseContext);
                this.title = metadata.get("dc:title");
                this.artist = metadata.get("xmpDM:artist");
                this.album = metadata.get("xmpDM:album");
            }
            in.close();
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
