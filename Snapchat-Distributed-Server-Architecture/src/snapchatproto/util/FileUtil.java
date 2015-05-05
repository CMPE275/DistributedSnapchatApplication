package snapchatproto.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import snapchatproto.constants.Constants;

import com.google.protobuf.ByteString;

public class FileUtil {

	private static FileInputStream fis;
	private static FileOutputStream fos;
	protected static Logger logger = LoggerFactory.getLogger("FileUtil");


	public FileUtil() {
	}

	public static ByteString readFile(String fname, String fpath) {
		ByteString bs = null;

		File file = new File(fpath+fname);

		if(file.exists() && file.isFile()) {
			try {
				fis = new FileInputStream(file);
				bs = ByteString.readFrom(fis);
			} catch (IOException e1) {
				logger.error("Invalid byte stream", e1);
				try {
					fis.close();
				} catch (IOException e) {
					logger.error("IOException", e1);
				}
			}
		}
		return bs;
	} 


	public static void writeFile(String fname, String fpath, byte[] buff) {
		fpath = Constants.CLIENT_FILE_SAVE_PATH;

		File file = new File(fpath+fname);

		if(!file.exists()) {
			try {
				fos = new FileOutputStream(file);
				fos.write(buff);
			} catch (FileNotFoundException e1) {
				try {
					fos.close();
				} catch (IOException e) {
					logger.error("IOException", e);
				}
				logger.error("File not found", e1);
			} catch (Exception e) {
				logger.error("Excpetion", e);
			}
		}
	} 
}

