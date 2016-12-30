import java.io.File;

/**
 * Created by bamibestelt on 2016-11-30.
 */
public class FilenameUtils {

    public static String getBaseName(String absolutePath) {
        return new File(absolutePath).getName();
    }
}
