package ftptest

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import com.htwd.file.FileUtil;
import com.htwd.ftp.FTPServer;

public class FtpServerTest {
	public static void main(String[] args) throws IOException {
		
		FTPServer ftp = new FTPServer("remotehostaddress","."/*workdir*/,"username","pwd",21);
		ftp.connect();
		
//		ftp.mkDir("abc");
//		ftp.rmDir("abc");
		
//		ftp.put("d:/bbb.txt");
//		FileUtil.deleteFileIfExists("D:/aaa.txt");
//		ftp.get("111.txt", "d:\\", "aaa.txt");
		
//		ftp.mkDir("testmkdir");
//		ftp.chDir("testmkdir");
//		ftp.get("a.jpg", "d:/","b.jpg");
		
//		ftp.chDir("testmkdir");
//		ftp.put("d:/a.jpg");
//		ftp.delete("a.jpg");
//		ftp.chDir("..");
//		ftp.rmDir("testmkdir");
		
//		ftp.mkDir("uploadfiles");
//		ftp.chDir("uploadfiles");
//		ftp.put("D:/abc", "");
//		for (String string : ftp.dir("")) {
////			System.out.print(string);
//			ftp.delete(string.replaceAll("[\r\n]", ""));
//		}
		
//		ftp.rename("testmkdir", "testmkdir1");//error 文件夹会导致错误
//		ftp.rename("111.txt", "112.txt");
		
//		ftp.get("", "d:/abc/bbc/");//注意使用这个方法的时候源文件夹中不能有文件夹，应该能使用第一个参数来控制，我没弄明白
		
		
		
		
		ftp.disConnect();
	}
}
