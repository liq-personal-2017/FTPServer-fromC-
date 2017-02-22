package ftptest

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.Charset;


/**
 * FTP 的摘要说明。  socket 默认绑定本地，端口随系统分配，最好不要设置，否则可能会产生无法绑定的问题
 * -文件名不得包含中文,整个程序中所有发送的命令都是以ascii编码方式发送的，
 * 而文件名是在命令中发送的，所以中文会导致无法编码或者无法解析的情况，ftp服务器会直接报错然后关闭连接
 * @author qli
 *
 */
public class FTPServer {

	//整个程序就是将c#的程序做了一个翻版，功能都测试过了，没问题，注意事项可以查看对应的test程序

	private String remoteHost;
	private int remotePort;
	private String remotePath;
	private String remoteUser;
	private String remotePass;

	/**
	 * 服务器返回的应答信息(包含应答码)  
	 */
	private String strMsg;
	/**
	 * 服务器返回的应答信息(包含应答码)  
	 */
	private String strReply;
	/**
	 * 服务器返回的应答码  
	 */
	private int iReplyCode;
	/**
	 * 进行控制连接的socket  
	 */
	private Socket socketControl;

	private boolean connected;

	/**
	 * 传输模式  
	 */
	private TransferType trType;

	/**
	 * 传输模式:二进制类型、ASCII类型  
	 * @author qli
	 *
	 */
	public enum TransferType {

		Binary,

		ASCII
	};

	/**
	 * 接收和发送数据的缓冲区  
	 */
	private static int BLOCK_SIZE = 512;
	byte[] buffer = new byte[BLOCK_SIZE];

	/**
	 * 缺省构造函数  
	 */
	public FTPServer() {
		remoteHost = "";
		remotePath = "";
		remoteUser = "";
		remotePass = "";
		remotePort = 21;
		connected = false;
	}

	public FTPServer(String remoteHost, String remoteUser, String remotePass) throws IOException {
		super();
		this.remoteHost = remoteHost;
		this.remoteUser = remoteUser;
		this.remotePass = remotePass;
		this.remotePort = 21;
		connect();
	}

	/**
	 * 构造函数  
	 * @param remoteHost
	 * @param remotePath
	 * @param remoteUser
	 * @param remotePass
	 * @param remotePort
	 * @throws IOException
	 */
	public FTPServer(String remoteHost, String remotePath, String remoteUser, String remotePass, int remotePort) throws IOException {
		this.remoteHost = remoteHost;
		this.remotePath = remotePath;
		this.remoteUser = remoteUser;
		this.remotePass = remotePass;
		this.remotePort = remotePort;
		connect();
	}

	/**
	 * 建立连接   
	 * @throws IOException
	 */
	public void connect() throws IOException {
		socketControl = new Socket();
		SocketAddress ep = new InetSocketAddress(remoteHost, remotePort);
		// 链接  
		try {
			socketControl.connect(ep);
		} catch (Exception ex) {
			throw new IOException("连接不上FTP服务器！");
		}
		// 获取应答码  
		readReply();
		if (iReplyCode != 220) {
			disConnect();
			throw new IOException(strReply.substring(4));
		}
		try {
			// 登陆  
			sendCommand("USER " + remoteUser);
			if (!(iReplyCode == 331 || iReplyCode == 230)) {
				closeSocketConnect();//关闭连接  
				throw new IOException(strReply.substring(4));
			}
			if (iReplyCode != 230) {
				sendCommand("PASS " + remotePass);
				if (!(iReplyCode == 230 || iReplyCode == 202)) {
					closeSocketConnect();//关闭连接  
					throw new IOException(strReply.substring(4));
				}
			}
		} catch (Exception e) {
			throw new IOException("登录用户名密码错误!");
		}
		connected = true;

		// 切换到目录  
		chDir(remotePath);
	}

	/**
	 * 关闭连接  
	 * @throws IOException
	 */
	public void disConnect() throws IOException {
		if (socketControl != null) {
			sendCommand("QUIT");
		}
		closeSocketConnect();
	}

	/**
	 * 设置传输模式  
	 * @param ttType
	 * @throws IOException
	 */
	public void setTransferType(TransferType ttType) throws IOException {
		if (ttType == TransferType.Binary) {
			sendCommand("TYPE I");//binary类型传输  
		} else {
			sendCommand("TYPE A");//ASCII类型传输  
		}
		if (iReplyCode != 200) {
			throw new IOException(strReply.substring(4));
		} else {
			trType = ttType;
		}
	}

	/**
	 * 获得传输模式  
	 * @return
	 */
	public TransferType getTransferType() {
		return trType;
	}

	/**
	 * 获得文件列表  
	 * @param strMask 文件名的匹配字符串
	 * @return
	 * @throws IOException
	 */
	public String[] dir(String strMask) throws IOException {
		// 建立链接  
		if (!connected) {
			connect();
		}

		//建立进行数据连接的socket  
		Socket socketData = createDataSocket();

		//传送命令  
		sendCommand("NLST " + strMask);

		//分析应答代码  
		if (!(iReplyCode == 150 || iReplyCode == 125 || iReplyCode == 226)) {
			return null;
		}

		//获得结果  
		strMsg = "";
		while (true) {
			int iBytes = socketData.getInputStream().read(buffer, 0, buffer.length);
			strMsg += new String(buffer, 0, iBytes);
			if (iBytes < buffer.length) {
				break;
			}
		}
		String seperator = "\n";
		String[] strsFileList = strMsg.split(seperator);
		socketData.close();//数据socket关闭时也会有返回码  
		if (iReplyCode != 226) {
			readReply();
			if (iReplyCode != 226) {
				throw new IOException(strReply.substring(4));
			}
		}

		return strsFileList;
	}

	/**
	 * 获取文件大小  
	 * @param strFileName 文件名
	 * @return 文件大小
	 * @throws IOException
	 */
	//	private long getFileSize(String strFileName) throws IOException {
	//		if (!bConnected) {
	//			connect();
	//		}
	//		sendCommand("SIZE " + (strFileName));
	//		long lSize = 0;
	//		if (iReplyCode == 213) {
	//			lSize = Integer.parseInt(strReply.substring(4));
	//		} else {
	//			throw new IOException(strReply.substring(4));
	//		}
	//		return lSize;
	//	}

	/**
	 * 删除  
	 * @param strFileName 待删除文件名
	 * @throws IOException
	 */
	public void delete(String strFileName) throws IOException {
		if (!connected) {
			connect();
		}
		sendCommand("DELE " + strFileName);
		if (iReplyCode != 250) {
			throw new IOException(strReply.substring(4));
		}
	}

	/**
	 * 重命名(如果新文件名与已有文件重名,将覆盖已有文件)  
	 * @param strOldFileName 旧文件名
	 * @param strNewFileName 新文件名
	 * @throws IOException
	 */
	public void rename(String strOldFileName, String strNewFileName) throws IOException {
		if (!connected) {
			connect();
		}
		sendCommand("RNFR " + strOldFileName);
		if (iReplyCode != 350) {
			throw new IOException(strReply.substring(4));
		}
		// 如果新文件名与原有文件重名,将覆盖原有文件  
		sendCommand("RNTO " + strNewFileName);

		if (iReplyCode != 250) {
			throw new IOException(strReply.substring(4));
		}
	}

	/**
	 * 下载一批文件  
	 * @param strFileNameMask 文件名的匹配字符串
	 * @param strFolder 本地目录(不得以\结束)
	 * @throws IOException
	 */
	public void get(String strFileNameMask, String strFolder) throws IOException {
		if (!connected) {
			connect();
		}
		String[] strFiles = dir(strFileNameMask);
		for (String strFile : strFiles) {
			String strFile1 = strFile.replaceAll("[\\r\\n]", "");
			if (!strFile1.equals(""))//一般来说strFiles的最后一个元素可能是空字符串  
			{
				get(strFile1, strFolder, strFile1);
			}

		}
	}

	/**
	 * 下载一个文件  
	 * @param strRemoteFileName 要下载的文件名
	 * @param strFolder 本地目录(必须以\结束)
	 * @param strLocalFileName 保存在本地时的文件名
	 * @throws IOException
	 */
	public void get(String strRemoteFileName, String strFolder, String strLocalFileName) throws IOException {
		if (!connected) {
			connect();
		}
		setTransferType(TransferType.Binary);
		if (strLocalFileName.equals("")) {
			strLocalFileName = strRemoteFileName;
		}
		String strOutputPath = strFolder + strLocalFileName;
		if (!FileUtil.isExists(strOutputPath)) {
			FileUtil.createFile(strOutputPath);
		}
		//            System.IO.FileStream output = new System.IO.FileStream(strOutputPath, FileMode.Create);
		Socket socketData = createDataSocket();
		sendCommand("RETR " + strRemoteFileName);
		if (!(iReplyCode == 150 || iReplyCode == 125 || iReplyCode == 226 || iReplyCode == 250)) {
			throw new IOException(strReply.substring(4));
		}
		BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(strOutputPath));
		while (true) {
			int iBytes = socketData.getInputStream().read(buffer, 0, buffer.length);
			//这里调整了一下顺序，原来是在这里写入，可能是因为c#中如果读取到文件流末尾返回的是0(不确定)，但是java中返回的是-1
			//导致这里会报错（数组下标越界，长度为0可以，但是-1就。。。）
			if (iBytes <= 0) {
				break;
			}
			output.write(buffer, 0, iBytes);
		}
		output.close();
		if (socketData.isConnected()) {
			socketData.close();
		}
		if (!(iReplyCode == 226 || iReplyCode == 250)) {
			readReply();
			if (!(iReplyCode == 226 || iReplyCode == 250)) {
				throw new IOException(strReply.substring(4));
			}
		}
	}

	/**
	 * 上传一批文件  s
	 * @param strFolder 本地目录(必须以\结束)
	 * @param strFileNameMask 文件名匹配字符(可以包含*和?)
	 * @throws IOException
	 */
	public void put(final String strFolder, String strFileNameMask) throws IOException {
		String[] strFiles = FileUtil.listFilesFromDirectory(strFolder, strFileNameMask);
		for (String strFile : strFiles) {
			//strFile是完整的文件名(包含路径)  
			put(strFile);
		}
	}

	/**
	 * 上传一个文件  
	 * @param strFileName 本地文件名
	 * @throws IOException
	 */
	public void put(String strFileName) throws IOException {
		if (!connected) {
			connect();
		}
		Socket socketData = createDataSocket();
		sendCommand("STOR " + FileUtil.getFileName(strFileName));
		if (!(iReplyCode == 125 || iReplyCode == 150)) {
			throw new IOException(strReply.substring(4));
		}
		InputStream input = new BufferedInputStream(new FileInputStream(strFileName));
		int iBytes = 0;

		while ((iBytes = input.read(buffer, 0, buffer.length)) > 0) {
			socketData.getOutputStream().write(buffer, 0, iBytes);
		}
		input.close();
		if (socketData.isConnected()) {
			socketData.close();
		}
		if (!(iReplyCode == 226 || iReplyCode == 250)) {
			readReply();
			if (!(iReplyCode == 226 || iReplyCode == 250)) {
				throw new IOException(strReply.substring(4));
			}
		}
	}

	/**
	 * 创建目录  
	 * @param strDirName 目录名
	 * @throws IOException
	 */
	public void mkDir(String strDirName) throws IOException {
		if (!connected) {
			connect();
		}
		sendCommand("MKD " + strDirName);
		if (iReplyCode != 257) {
			throw new IOException(strReply.substring(4));
		}
	}

	/**
	 * 删除目录  
	 * @param strDirName 目录名
	 * @throws IOException
	 */
	public void rmDir(String strDirName) throws IOException {
		if (!connected) {
			connect();
		}
		sendCommand("RMD " + strDirName);
		if (iReplyCode != 250) {
			throw new IOException(strReply.substring(4));
		}
	}

	/**
	 * 改变目录  
	 * @param strDirName 新的工作目录名
	 * @throws IOException
	 */
	public void chDir(String strDirName) throws IOException {
		if (".".equals(strDirName) || StringUtil.isNullOrEmpty(strDirName)) {
			return;
		}
		if (!connected) {
			connect();
		}
		sendCommand("CWD " + strDirName);
		if (iReplyCode != 250) {
			throw new IOException(strReply.substring(4));
		}
		this.remotePath = strDirName;
	}

	/**
	 * 将一行应答字符串记录在strReply和strMsg  
	 * 应答码记录在iReplyCode  
	 * @throws IOException
	 */
	private void readReply() throws IOException {
		strMsg = "";
		strReply = readLine();
		iReplyCode = Integer.parseInt(strReply.substring(0, 3));
	}

	/**
	 * 建立进行数据连接的socket  
	 * @return 数据连接socket
	 * @throws IOException
	 */
	private Socket createDataSocket() throws IOException {
		sendCommand("PASV");
		if (iReplyCode != 227) {
			throw new IOException(strReply.substring(4));
		}
		int index1 = strReply.indexOf('(');
		int index2 = strReply.indexOf(')');
		String ipData = strReply.substring(index1 + 1, index2);
		int[] parts = new int[6];
		int len = ipData.length();
		int partCount = 0;
		String buf = "";
		//这里其实可以改造一下，正确的输入应该是一个六段的地址(162,16,160,13,123,123)，前面的四段表示一个ip地址，后面的两段表示一个端口号，端口号的计算方式为 parts[4]*256+parts[5]
		//原作者可能是考虑这里会有不正确的输入，所以做成了这个样子
		//参照其他代码，原作者应该是知道这里能用split，但是为什么没用我就不知道了，所以这里沿用原来的算法而不进行修改
		for (int i = 0; i < len && partCount <= 6; i++) {
			//			char ch = (ipData.substring(i, i + 1)).charAt(0);
			char ch = (ipData).charAt(i);
			if (Character.isDigit(ch))
				buf += ch;
			else if (ch != ',') {
				throw new IOException("Malformed PASV strReply: " + strReply);
			}
			if (ch == ',' || i + 1 == len) {
				try {
					parts[partCount++] = Integer.parseInt(buf);
					buf = "";
				} catch (Exception e) {
					throw new IOException("Malformed PASV strReply: " + strReply);
				}
			}
		}
		String ipAddress = parts[0] + "." + parts[1] + "." + parts[2] + "." + parts[3];
		int port = (parts[4] << 8) + parts[5];//乘以256
		Socket s = new Socket();
		SocketAddress ep = new InetSocketAddress((ipAddress), port);
		try {
			s.connect(ep);
		} catch (Exception e) {
			throw new IOException("Can't connect to remote server");
		}
		return s;
	}

	/**
	 * 关闭socket连接(用于登录以前)  
	 * @throws IOException
	 */
	private void closeSocketConnect() throws IOException {
		if (socketControl != null) {
			socketControl.close();
			socketControl = null;
		}
		connected = false;
	}

	/**
	 * 读取Socket返回的所有字符串  
	 * @return 包含应答码的字符串行
	 * @throws IOException
	 */
	private String readLine() throws IOException {
		while (true) {
			int iBytes = socketControl.getInputStream().read(buffer, 0, buffer.length);
			strMsg += new String(buffer, Charset.forName("US-ASCII"));
			if (iBytes < buffer.length) {
				break;
			}
		}
		//这里是原来的C#代码，我没搞懂这里是怎么回事，但是直接返回strmsg是没有问题的（不知道原来的作者经过了怎样的非人折磨）
		//		String seperator = "\n";
		//		String[] mess = strMsg.split(seperator);
		//		if (mess.length > 2) {
		//			strMsg = mess[mess.length - 2];
		//			//seperator[0]是10,换行符是由13和0组成的,分隔后10后面虽没有字符串,  
		//			//但也会分配为空字符串给后面(也是最后一个)字符串数组,  
		//			//所以最后一个mess是没用的空字符串  
		//			//但为什么不直接取mess[0],因为只有最后一行字符串应答码与信息之间有空格
		//		} else {
		//			strMsg = mess[0];
		//		}
		//		if (!strMsg.substring(3, 4).equals(" "))//返回字符串正确的是以应答码(如220开头,后面接一空格,再接问候字符串)  
		//		{
		//			return readLine();
		//		}
		return strMsg;
	}

	/**
	 * 发送命令并获取应答码和最后一行应答字符串  
	 * @param strCommand 命令
	 * @throws IOException
	 */
	private void sendCommand(String strCommand) throws IOException {
		byte[] cmdBytes = ((strCommand + "\r\n").getBytes("US-ASCII"));
		socketControl.getOutputStream().write(cmdBytes, 0, cmdBytes.length);
		readReply();
	}

}
