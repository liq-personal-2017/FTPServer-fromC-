package ftptest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;



public class FileUtil {
	/**
	 * 存在并且是一个目录，返回true
	 * 	否则返回false
	 * @param directoryPath
	 * @return
	 */
	public static boolean isDirectory(String directoryPath) {
		File file = new File(directoryPath);
		if (file.exists() && file.isDirectory()) {
			return true;
		}
		return false;
	}

	/**
	 * 存在并且是一个文件，返回true
	 *  否则返回false
	 * @param filepath
	 * @return
	 */
	public static boolean isFile(String filepath) {
		File file = new File(filepath);
		if (file.exists() && file.isFile()) {
			return true;
		}
		return false;
	}

	/**
	 * 存在返回true
	 * 	否则返回false
	 * @param path
	 * @return
	 */
	public static boolean isExists(String path) {
		File file = new File(path);
		return file.exists();
	}

	/**
	 * 创建文件
	 * @param filepath
	 * @return
	 * @throws IOException
	 */
	public static boolean createFile(String filepath) throws IOException {
		File file = new File(filepath);
		return file.createNewFile();
	}

	/**
	 * 创建文件夹，仅限一层
	 * @param directoryPath
	 * @return
	 */
	public static boolean createDirectory(String directoryPath) {
		File file = new File(directoryPath);
		//注意file有一个mkdirs的方法，可以创建多层
		return file.mkdir();
	}

	/**
	 * 获取文件夹下的文件列表（仅包含文件）
	 * @param directorypath
	 * @param strFileNameMask 正则表达式过滤（为空|空白表示不过滤）
	 * @return 如果传入的路径是一个文件夹则返回一个数组，否则返回null
	 */
	public static String[] listFilesFromDirectory(String directorypath, String strFileNameMask) {
		//		Pattern pattern = Pattern.compile(strFileNameMask);
		boolean isNeedMask = StringUtil.isNullOrEmpty(strFileNameMask);
		File file = new File(directorypath);
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			List<String> resultlist = new ArrayList<String>(files.length);
			for (File file2 : files) {
				if (file2.isFile() && (isNeedMask || Pattern.matches(strFileNameMask, file2.getAbsolutePath()))) {
					resultlist.add(file2.getAbsolutePath());
				}
			}
			String[] resultArr = new String[resultlist.size()];
			return resultlist.toArray(resultArr);
		}
		return null;
	}
	
	/**
	 * 获取文件夹下的文件列表（仅包含文件夹）
	 * @param directorypath
	 * @param strFileNameMask 正则表达式过滤（为空|空白表示不过滤）
	 * @return 如果传入的路径是一个文件夹则返回一个数组，否则返回null
	 */
	public static String[] listDirectoriesFromDirectory(String directorypath, String strFileNameMask) {
		//		Pattern pattern = Pattern.compile(strFileNameMask);
		boolean isNeedMask = StringUtil.isNullOrEmpty(strFileNameMask);
		File file = new File(directorypath);
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			List<String> resultlist = new ArrayList<String>(files.length);
			for (File file2 : files) {
				if (file2.isDirectory() && (isNeedMask || Pattern.matches(strFileNameMask, file2.getAbsolutePath()))) {
					resultlist.add(file2.getAbsolutePath());
				}
			}
			String[] resultArr = new String[resultlist.size()];
			return resultlist.toArray(resultArr);
		}
		return null;
	}
	
	/**
	 * 获取文件夹下的文件列表（包含文件夹和文件）
	 * @param directorypath
	 * @param strFileNameMask 正则表达式过滤（为空|空白表示不过滤）
	 * @return 如果传入的路径是一个文件夹则返回一个数组，否则返回null
	 */
	public static String[] listFromDirectory(String directorypath, String strFileNameMask) {
		//		Pattern pattern = Pattern.compile(strFileNameMask);
		boolean isNeedMask = StringUtil.isNullOrEmpty(strFileNameMask);
		File file = new File(directorypath);
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			List<String> resultlist = new ArrayList<String>(files.length);
			for (File file2 : files) {
				if ((isNeedMask || Pattern.matches(strFileNameMask, file2.getAbsolutePath()))) {
					resultlist.add(file2.getAbsolutePath());
				}
			}
			String[] resultArr = new String[resultlist.size()];
			return resultlist.toArray(resultArr);
		}
		return null;
	}

	/**
	 * 返回文件名
	 * @param filepath
	 * @return
	 */
	public static String getFileName(String filepath) {
		File file = new File(filepath);
		return file.getName();
	}
	/**
	 * 删除（文件或者文件夹）
	 * @param filepath
	 * @return
	 */
	public static boolean deleteIfExists(String filepath) {
		File file = new File(filepath);
		return file.delete();
	}
}
