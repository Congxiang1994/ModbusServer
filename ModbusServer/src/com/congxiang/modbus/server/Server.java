package com.congxiang.modbus.server;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.congxiang.modbus.dao.SQLiteCRUD;
import com.congxiang.modbus.dao.SQLiteConn;
import com.congxiang.modbus.util.ByteUtil;
import com.congxiang.ui.ServerPanel;

public class Server implements ActionListener {

	List<ModbusClient> modbusClientList = new ArrayList<ModbusClient>(); // 保存modbus终端对象
	List<HostClient> hostClientList = new ArrayList<HostClient>(); // 保存modbus终端对象

	boolean connectionStarted; // 标识量，当程序终止的时候，置为false，可以终止主线程
	
	// 面板对象
	public ServerPanel serverPanel = new ServerPanel();

	// 界面控件
	JTextArea tainfo = new JTextArea(); // 　文本框
	JScrollPane jsp = new JScrollPane(tainfo); // 　滚动栏

	// 数据库相关
	Connection conn;
	SQLiteCRUD sqlitecrud; // 工具类的对象
	
	// 主线程
	MainThread mainThread = null;

	// 构造方法，主要是绘制界面上的控件，以及设置监听器
	Server() throws Exception {

		/** 建立数据库、表，并连接数据库------------------------------------------------------------------------------------ */

		this.printInformation(0, "服务器：开始建立数据库、建表、连接数据库");
		
		String dataFile = "modbusServer.db"; // 数据库文件
		SQLiteConn sqliteConn = new SQLiteConn(dataFile); // 创建数据库
		conn = sqliteConn.getConnection(); // 建立数据库连接
		sqlitecrud = new SQLiteCRUD(conn); // 创建一个工具类对象

		/** 创建上下线-历史信息的表 */
		if (sqlitecrud.createTable("create table if not exists State(time datetime, terminalName varchar(50), deviceName varchar(50), state varchar(50));")) {
			this.printInformation(1, "创建数据库表:表State成功！");
		}

		/** 创建上下线-当前状态的表 */
		if (sqlitecrud.createTable("create table if not exists CurrentState(time datetime, terminalName varchar(50), deviceName varchar(50), state varchar(50));")) {
			this.printInformation(1, "创建数据库表:表CurrentState成功！");
		}

		/** 创建 modbus命令的表 --- 这个根据上位机发送过来的消息实时更新，一方面用于判断下位设备哪些是在线的 */
		if (sqlitecrud.createTable("create table if not exists ModbusMsg(modbusMsg varchar(50));")) {
			this.printInformation(1, "创建数据库表:表ModbusMsg成功！");
		}

		/** 创建 modbus数据的表 --- 这个表记录从modbus终端返回的数据 */
		if (sqlitecrud.createTable("create table if not exists MonitorData(time datetime, terminalName varchar(50), deviceName varchar(50), data varchar(100));")) {
			this.printInformation(1, "创建数据库表:表MonitorData成功！");
		}
		/** -------------------------------------------------------------------------------------------------------------- */
		
		/** 显示窗口设计：主要一个文本框，用来实时显示程序运行状态------------------------------------------------------------ */
		JFrame frame = new JFrame();
		Container mainContainer = frame.getContentPane();
		mainContainer.add(serverPanel);
		frame.setTitle("modbus服务器程序");
		
		/* 为面板上的按钮添加消息相应事件 */
		serverPanel.btOpenServer.addActionListener(this);
		serverPanel.btCloseServer.addActionListener(this);
		
		frame.setSize(500, 350); // 设置界面大小
		// this.setLocationRelativeTo(null); // 设置界面在屏幕中央显示
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // 设置单击关闭按钮能够关闭主进程，--------------------------------这里将来要改一下，不能直接关闭，因为这个程序充当服务器小程序
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				/* 编写关闭主程序时的相关的操作 */
				/*
				 * // 关闭所有modbus终端客户端线程 for (int i = 0; i <
				 * modbusClientList.size(); i++) {
				 * modbusClientList.get(i).isReadyToSendMsgToModbusTerminal =
				 * false; // while标识量设为false try {
				 * modbusClientList.get(i).buffInputStream.close();// 关闭输入流
				 * modbusClientList.get(i).buffOutputStream.close(); // 关闭输出流
				 * modbusClientList.get(i).socket.close(); // 关闭socket
				 * printInformation(1, "关闭modbus终端线程中的变量...");
				 * modbusClientList.remove(i); i--; } catch (IOException e1) {
				 * printInformation(-1, "警告，关闭modbus终端线程中的变量失败！！！"); } }
				 */
				System.out.println("现在可以顺利关闭程序。");
				System.exit(0);
			}
		});
		frame.setVisible(true); // 显示窗口
		/** -------------------------------------------------------------------------------------------------------------- */

	}
	/** ----------------------------------------------------------------------------------------------------------------- */
	/** 主线程
	 * @author CongXiang 
	 * 功能：用于接收上位机、modbus终端的连接请求
	 * */
	class MainThread extends Thread {
		private ServerSocket serverSocket = null; // 服务器端的套接字
		private Socket socketClient = null; // 连接套接字
		@Override
		public void run() {
			/** 主要进程:主要用来接收上位机和modbus终端的连接请求 -----------------------------------------------------------------*/
			printInformation(1, "\n" + "主线程即将开始运行......");

			try {
				serverSocket = new ServerSocket(Integer.valueOf(serverPanel.tfPort.getText().trim())); // 创建套接字
			} catch (IOException e) {
				e.printStackTrace();
				printInformation(1, "主线程:创建服务器端套接字失败！");
			}

			printInformation(1, "主线程:服务器套接字创建成功，下一步等待接收客户端的请求连接...");

			connectionStarted = true;

			while (connectionStarted == true) {// 主线程主要是一个死循环，主要接收modbus终端的请求
				printInformation(1, "\n" + "主线程：进入循环,等待新的请求连接...");
				try {
					socketClient = serverSocket.accept(); // 接收客户端请求
					printInformation(1, "主线程:有客户端请求连接，下一步判断是上位机还是modbus终端");

					// 判断客户端是上位机还是modbus终端,这里通过接收的第一个字符来判断
					InputStream inputStream = socketClient.getInputStream();
					OutputStream outputStream = socketClient.getOutputStream();

					BufferedInputStream bufferInputStream = new BufferedInputStream(inputStream);
					BufferedOutputStream buffOutputStream = new BufferedOutputStream(outputStream);
					byte[] bufferID = new byte[1];
					
					/*
					int num = bufferInputStream.read(bufferID);// ---------------------------------------------------------------------read
					*/
					int num  = recvMsg(bufferInputStream, bufferID);
					if (num < 0) {
						connectionStarted = false; // 如果接收出错，则终止主线程
						printInformation(-1, "主线程:警告，接收字符出错！");
						break;
					}

					printInformation(1, "主线程:接收的字符的数量为：" + num + ";接收的字符串为：" + bufferID[0]);
					printInformation(1, "主线程:成功将接收的字符串返回给请求连接的客户端");

					// 判断是哪种类型的客户端上线
					if (bufferID[0] == 0x00) {
						/*
						buffOutputStream.write(bufferID);// ---------------------------------------------------------------------write
						buffOutputStream.flush();
						*/
						sendMsg(buffOutputStream, bufferID,1);
						/** ------上位机请求 */
						printInformation(1, "主线程:是上位机请求连接");
						HostClient hostClient = new HostClient(socketClient, inputStream, outputStream); // 新建一个上位机对象
						hostClientList.add(hostClient); // 将上位机对象添加到List集合中
						printInformation(1, "主线程:将上位机线程添加到线程类集合中");
						new Thread(hostClient).start(); // 启动上位机的线程
						printInformation(1, "主线程:启动上位机线程成功");

					} else if (bufferID[0] == 0x01) {
						/*
						buffOutputStream.write(bufferID);// ---------------------------------------------------------------------write
						buffOutputStream.flush();
						*/
						sendMsg(buffOutputStream, bufferID,1);
						/** ------modbus终端请求 */
						printInformation(1, "主线程:是modbus终端请求连接");
						ModbusClient modbusClient = new ModbusClient(socketClient, inputStream, outputStream); // 新建一个modbus终端对象
						modbusClientList.add(modbusClient); // 将modbus对象添加到List集合中
						new Thread(modbusClient).start(); // 启动modbus终端的线程
					} else {
						/** ------出现未知请求源 */
						socketClient.close();// 关闭此socket连接
						printInformation(-1, "主线程:警告，出现未知请求源，已自动关闭此连接");
					}

					// 将不需要的输入输出流关闭

				} catch (IOException e) {
					// e.printStackTrace();
					printInformation(-1, "主线程:警告，接收客户端连接请求失败！");
					try {
						serverSocket.close();
						// socketClient.close();
					} catch (IOException e1) {
					}
					connectionStarted = false;

					// 关闭其他辅助线程
					//modbusThread.modbusMsgStarted = false;
					break;
				}
			}
			/** -------------------------------------------------------------------------------------------------------------- */
			printInformation(-1, "主线程:主线程结束！！！");

		}
		
	}
	/**上位机客户端 辅助线程------------------------------------------------------------------------------------------------ */
	/** ------------------------------------------------------------------------------------------------------------------ */
	class HostClient implements Runnable {

		private Socket socket; // 套接字
		private InetAddress addressIp; // 客户端的IP
		private int portNum; // 客户端请求的端口号
		boolean hostConnectionStarted;
		private BufferedInputStream buffInputStream;
		private BufferedOutputStream buffOutputStream;

		@Override
		public String toString() {
			return "上位机客户端：HostClient [addressIp=" + addressIp + ", portNum=" + portNum + "]";
		}

		// 构造方法
		public HostClient(Socket s, InputStream inPutStream, OutputStream outPutStream) throws IOException {
			this.socket = s;
			this.portNum = this.socket.getPort();
			this.addressIp = this.socket.getInetAddress();
			this.buffInputStream = new BufferedInputStream(inPutStream);
			this.buffOutputStream = new BufferedOutputStream(outPutStream);

			printInformation(1, this.toString());
		}

		@Override
		public void run() {
			printInformation(1, "上位机客户端：开始进入上位机客户端线程");
			hostConnectionStarted = true;
			
			/* 启动发送modbus命令给上位机的线程  */ 
			SendModbusOrderToHostThread sendModbusOrderToHostThread = new SendModbusOrderToHostThread(buffOutputStream);
			sendModbusOrderToHostThread.start();
			
			while (hostConnectionStarted) {
				printInformation(1, "\n" + "上位机客户端：进入循环，开始接收消息......");
				byte[] buffRecv = new byte[64];// 接收第一个字节的缓冲区
				try {
					/*
					int numRecv = this.buffInputStream.read(buffRecv); // ---------------------------------------------------------------------read
					*/
					int numRecv = recvMsg(this.buffInputStream, buffRecv);
					if (numRecv < 0) {
						printInformation(-1, "上位机客户端：警告，接收消息出错！");
						hostConnectionStarted = false;
						break;
					}
					//printInformation(1, "上位机客户端：接收的字符的数量为：" + numRecv);
					printInformation(1, "上位机客户端：接收的字符的数量为：" + numRecv + ";接收的字符串为：" + ByteUtil.bytesToHexString(buffRecv));

					// 这里将根据firstBuffer的类型来进行不同的数据处理
					printInformation(1, "上位机客户端：接下来，根据接收的字符判断消息类型...");
					switch (buffRecv[0]) {
					
					case 0x06:
						printInformation(1, "上位机客户端：消息类型0x06，上位机发送modbus命令给server服务器程序！");
						/*
						this.buffOutputStream.write(buffRecv[0]);// ---------------------------------------------------------------------write
						this.buffOutputStream.flush();
						*/
						sendMsg(this.buffOutputStream, buffRecv,1);
						/** 上位机发送modbus命令给server服务器 */
						

						// 从消息中取出modbus命令
						String strModbus = ByteUtil.bytesToHexString(buffRecv).substring(2, numRecv * 2).toUpperCase();
						// strModbus = strModbus; //
						// 需要乘2，因为每个字节转换成string,是占用两个字符
						printInformation(1, "上位机客户端：modbus命令为：" + strModbus + "。");

						// 将收到的modbus命令进行拆分，每个modbus命令长为16
						for (int i = 0; i < strModbus.length(); i = i + 16) {
							String strM = strModbus.substring(i, i + 16);

							// 将modbus命令放进数据库，放入之前要查看表中是否已经存在
							String[] str = new String[1];
							str[0] = strM;

							if (sqlitecrud.getTableCount("ModbusMsg", "modbusMsg", strM) > 0) {
								printInformation(1, "上位机客户端：数据库中已经存在这条modbus命令。");
							} else {
								sqlitecrud.insert("ModbusMsg", str);
								printInformation(1, "上位机客户端：添加一条线的modbus命令。");
							}
						}
						break;
						
					case 0x08:
						printInformation(1, "上位机客户端：消息类型0x08，server服务器向上位机发送设备状态数据！");
						/** 上位机向server服务器请求设备状态树 */
						/**
						 * 1.返回的数据内容 1.第一位是消息类型：08
						 * 2.第二位是IP的长度：xx,因为IP地址的长度是不一定，需要指定长度
						 * 3.第三位是IP地址：xxxx(长度由前一个字段指定)
						 * 4.第四位是下位设备号，FF代表没有下位设备：xx(两个字节)
						 * 5.所有IP和设备号在后面顺序连接，一起发送，由上位机组装
						 * 
						 * 注意：IP地址存的不是16进制数，与string之间的转换不用使用工具类
						 * */
/*						
						String strStateData = new String(new byte[] { 0x08 });; 

						int countModbusMsg = sqlitecrud.getTableCount("CurrentState");
						if (countModbusMsg > 0) { // 判断数据库里是否有数据

							// 从数据库中将modbus命令从数据库中取出来
							Object[][] objStateData = sqlitecrud.selectObject("CurrentState"); // 所有状态数据

							// 获取所有数据的数量，这样才能确定循环的次数，
							int numCount = sqlitecrud.getTableCount("CurrentState");

							String strTerminalName; // modbus终端的IP地址
							String strDeviceName; // 下位设备的编号
							String lengthOfTerminalName;

							for (int i = 0; i < numCount; i++) {
								 从数据库中查找的数据不需要排序，在上位机将会进行数据的树状表示 

								strTerminalName = objStateData[i][1].toString(); // 表示modbus终端IP字段
								strDeviceName = objStateData[i][2].toString(); // 表示下位设备编号字段
								lengthOfTerminalName = ByteUtil.intToString(strTerminalName.length()); // IP的长度，一般小于两位数，需要转换
								strStateData = strStateData + lengthOfTerminalName + strTerminalName + strDeviceName;
							}
						}
						printInformation(1, "上位机客户端：发送给上位机的状态数据为：" + strStateData);

						// 发送数据
						byte[] byteStateData = strStateData.getBytes();
						sendMsg(this.buffOutputStream, byteStateData);// ---------------------------------------------------------------------write
*/
						break;
						
					case 0x0B:
						printInformation(1, "上位机客户端：消息类型0x0B，server服务器向上位机发送实时modbusdata监测数据！");
						
						/* 从数据库中取最新的一条数据 ，并发送给上位机客户端*/
/*						
						// 1.从数据库中获取最新的一条记录：功能码 + 时间 + 地址长度 + modbus终端的IP地址 + 下位设备ID + modbusdata
						Object[] objModbusData = sqlitecrud.selectLatestObject("MonitorData","time"); // 以时间降序
						
						String strModbusData = new String(new byte[] {0x0B}) ; // 消息类型码0x08
						if(objModbusData[0] != null){ // 如果查到数据
							// 2.组装modbusdata监测数据，原则：功能码用16进制数表示
							strModbusData = strModbusData + objModbusData[0].toString(); // 时间
							strModbusData = strModbusData + ByteUtil.intToString(objModbusData[1].toString().length()); // IP地址的长度
							strModbusData = strModbusData + objModbusData[1]; // modbus终端的IP地址
							strModbusData = strModbusData + objModbusData[2]; // 下位设备ID
							strModbusData = strModbusData + objModbusData[3]; // modbusdata监测数据
						}
						printInformation(1, "上位机客户端：发送给上位机的实时modbusdata监测数据为：" + strModbusData);
						
						// 3.将modbusdata实时监测数据发送给上位机
						byte[] byteModbusData = strModbusData.getBytes();
						sendMsg(this.buffOutputStream, byteModbusData);// ---------------------------------------------------------------------write
*/									
						break;
						
					case 0x0D:
						printInformation(1, "上位机客户端：消息类型0x0D，向server服务器请求当前设备状态First！");
						/** 上位机向server服务器请求设备状态树 */
						/**
						 * 1.返回的数据内容 1.第一位是消息类型：08
						 * 2.第二位是IP的长度：xx,因为IP地址的长度是不一定，需要指定长度
						 * 3.第三位是IP地址：xxxx(长度由前一个字段指定)
						 * 4.第四位是下位设备号，FF代表没有下位设备：xx(两个字节)
						 * 5.所有IP和设备号在后面顺序连接，一起发送，由上位机组装
						 * 
						 * 注意：IP地址存的不是16进制数，与string之间的转换不用使用工具类
						 * */
						
						String strStateData = new String(new byte[] { 0x0D });; 

						int countModbusMsg = sqlitecrud.getTableCount("CurrentState");
						if (countModbusMsg > 0) { // 判断数据库里是否有数据

							// 从数据库中将modbus命令从数据库中取出来
							Object[][] objStateData = sqlitecrud.selectObject("CurrentState"); // 所有状态数据

							// 获取所有数据的数量，这样才能确定循环的次数，
							int numCount = sqlitecrud.getTableCount("CurrentState");

							String strTerminalName; // modbus终端的IP地址
							String strDeviceName; // 下位设备的编号
							String lengthOfTerminalName;

							for (int i = 0; i < numCount; i++) {
								 //从数据库中查找的数据不需要排序，在上位机将会进行数据的树状表示 
								strTerminalName = objStateData[i][1].toString(); // 表示modbus终端IP字段
								strDeviceName = objStateData[i][2].toString(); // 表示下位设备编号字段
								lengthOfTerminalName = ByteUtil.intToString(strTerminalName.length()); // IP的长度，一般小于两位数，需要转换
								strStateData = strStateData + lengthOfTerminalName + strTerminalName + strDeviceName;
							}
						}
						printInformation(1, "上位机客户端：第一次！！！发送给上位机的状态数据为：" + strStateData);

						// 发送数据
						byte[] byteStateData = strStateData.getBytes();
						sendMsg(this.buffOutputStream, byteStateData, byteStateData.length);// ---------------------------------------------------------------------write
						break;
						
					case 0x0E:
						printInformation(1, "上位机客户端：消息类型0x0E，Server发送了一条modbus命令给上位机！");
						break;
						
					case 0x0F:
						printInformation(1, "上位机客户端：消息类型0x0F:上位机添加modbus命令");
						
						// 1.解析接收到的数据
						String strInsertModbusOrder = new String(buffRecv).substring(1, 17); // 直接将字节数组按照ascll码的方式转换成string类型
						printInformation(1, "上位机客户端：上位机添加modbus命令:"+strInsertModbusOrder);
						
						// 2.查询数据库中是否已经存在这条modbus命令
						if (sqlitecrud.getTableCount("ModbusMsg", "modbusMsg", strInsertModbusOrder) > 0) {
							printInformation(1, "上位机客户端：数据库中已经存在这条modbus命令。");
						} else {
							
							// 3.向数据库中添加一条modbus命令
							String[] strArrayInsertModbusOrder = new String[1];
							strArrayInsertModbusOrder[0] = strInsertModbusOrder;
							sqlitecrud.insert("ModbusMsg", strArrayInsertModbusOrder);
							printInformation(1, "上位机客户端：添加一条新的modbus命令。");
							
							
							/* --->>>将添加modbus命令的消息发送给modbus终端，消息格式：0x11+modbus命令 */
							// 1.判断上线的modbus终端数量是否大于0,建立for循环，
							for (int i = 0; i < modbusClientList.size(); i++) {
								// 2.组装消息：0x11+modbus命令
								byte[] buffsend = ByteUtil.hexStringToBytes("11" + strInsertModbusOrder);
								printInformation(1, "上位机客户端：Server服务器将modbus命令转发给modbus终端："+strInsertModbusOrder);
								// 3.发送消息给modbus终端
								sendMsg(modbusClientList.get(i).buffOutputStream, buffsend, buffsend.length);
							}
							/* -------------------------------------------------------------------- */
						}
						
						// 4.将消息类型0x0F返回给上位机，（消息从哪里来，就到哪里结束）
						byte[] buff0F = new byte[] { 0x0F };
						sendMsg(buffOutputStream, buff0F, 1);//---------------------------------------------------------------------write

						break;
						
					case 0x10:
						printInformation(1, "上位机客户端：消息类型0x10:上位机删除modbus命令");
						
						// 1.解析接收到的数据
						String strDeleteModbusOrder = new String(buffRecv).substring(1, 17); // 直接将字节数组按照ascll码的方式转换成string类型
						printInformation(1, "上位机客户端：上位机删除modbus命令:"+strDeleteModbusOrder);
						
						// 2.从数据库中删除该条命令
						sqlitecrud.delete("ModbusMsg", "modbusMsg", strDeleteModbusOrder);
						
						/* --->>>将删除modbus命令的消息发送给modbus终端，消息格式：0x12+modbus命令 */
						// 1.判断上线的modbus终端数量是否大于0,建立for循环，
						for (int i = 0; i < modbusClientList.size(); i++) {
							// 2.组装消息：0x11+modbus命令
							byte[] buffsend = ByteUtil.hexStringToBytes("12" + strDeleteModbusOrder);
							printInformation(1, "上位机客户端：Server服务器通知modbus终端删除该命令："+strDeleteModbusOrder);
							// 3.发送消息给modbus终端
							sendMsg(modbusClientList.get(i).buffOutputStream, buffsend, buffsend.length);
						}
						/* -------------------------------------------------------------------- */
						
						// 3.将消息类型0x0F返回给上位机，（消息从哪里来，就到哪里结束）
						byte[] buff10 = new byte[] { 0x10 };
						sendMsg(buffOutputStream, buff10, 1);//---------------------------------------------------------------------write

						break;
						
					default:
						break;
					}
				} catch (IOException e) { // 捕获客户端进程结束的异常，关闭这个进程，并且从list集合中清除
					printInformation(-1, "上位机客户端：警告，该连接出现问题，异常处理，关闭套接字输入输出流...");
					hostConnectionStarted = false;
					break;
				}
				printInformation(1, "上位机客户端：处理该消息结束！");
			}
			/*
			 * tainfo.append("上位机客户端：出现异常，本线程将自动结束！"+"\n"); tainfo.selectAll();
			 */

			try {
				this.socket.close();
			} catch (IOException e1) {
			}
			try {
				this.buffInputStream.close();
			} catch (IOException e1) {
			}
			try {
				this.buffOutputStream.close();
			} catch (IOException e1) {
			}
			// 将自己从 list 中删除
			hostClientList.remove(hostClientList.indexOf(this));
			printInformation(-1, "上位机客户端：hostClientList的长度为：" + hostClientList.size());

			printInformation(1, "上位机客户端：该线程出现异常，本线程将自动结束！");
		}
	}

	/**modbus终端客户端 辅助线程------------------------------------------------------------------------------------------- */
	/** ----------------------------------------------------------------------------------------------------------------- */
	class ModbusClient implements Runnable {

		private Socket socket; // 套接字
		private InetAddress addressIp; // 客户端的IP
		private int portNum; // 客户端请求的端口号
		boolean modbusConnectionStarted;
		//boolean isReadyToSendMsgToModbusTerminal = true; // 决定是否可以给modbus终端发送消息
		private BufferedInputStream buffInputStream;
		private BufferedOutputStream buffOutputStream;

		@Override
		public String toString() {
			return "modbus终端客户端：ModbusClient [addressIp=" + addressIp + ", portNum=" + portNum + "]";
		}

		// 构造方法
		public ModbusClient(Socket s, InputStream inPutStream, OutputStream outPutStream) throws IOException {
			this.socket = s;
			this.portNum = this.socket.getPort();
			this.addressIp = this.socket.getInetAddress();
			this.buffInputStream = new BufferedInputStream(inPutStream);
			this.buffOutputStream = new BufferedOutputStream(outPutStream);

			printInformation(1, this.toString());

			/** --- 将modbus终端上线的消息添加到数据库中 */
			// -------------------------------------------------至于下位设备的上线消息，server会再次发送一个询问的消息
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 设置日期格式
			String[] strValuesOfMonitorData = new String[4];
			strValuesOfMonitorData[0] = df.format(new Date()).toString();
			strValuesOfMonitorData[1] = this.addressIp.toString(); // 存modbus终端的IP即可
			strValuesOfMonitorData[2] = "FF";
			strValuesOfMonitorData[3] = "on";
			sqlitecrud.insert("CurrentState", strValuesOfMonitorData);
			sqlitecrud.insert("State", strValuesOfMonitorData);
			updateEquipmentStateToHost(
					strValuesOfMonitorData[0] 
					+ ByteUtil.intToString(strValuesOfMonitorData[1].length()) 
					+ strValuesOfMonitorData[1] 
					+ strValuesOfMonitorData[2] 
					+ strValuesOfMonitorData[3]); // 调用“向上位机更新设备状态的方法，更新设备状态信息”
		}

		@Override
		/**
		 * 主要接收modbus终端返回的消息，根据返回消息的不同类型，进行不同的处理
		 * @author CongXiang
		 * 消息类型：
		 * 	1.配置文件已经收到的确认信息
		 * 	2.状态信息
		 * 	3.数据信息
		 * 	4.仪表信息
		 * 	5.告警信息
		 * 注意：
		 * 	1.采用"标识量"，这样可以用终止主线程的方法终止modbus终端线程
		 * 	2.也是采用死循环，不断接受下位设备发送来的数据
		 * 	3.要根据不同的消息类型采用不同的接收数据的方式，因为数据长短不一，并且还有文本文件
		 *  4.消息类型和消息将一起发送过来，这样的好处是
		 * */
		public void run() {
			
			/**
			 * 启动发送数据给modbus终端的线程，用于发送modbus命令给modbus终端
			 * */
			SendModbusMsgToTreminalThread modbusThread = new SendModbusMsgToTreminalThread(buffOutputStream);
			modbusThread.start();
			printInformation(1, "modbus终端客户端：开始进入modbus终端客户端线程,此线程执行一次后机会结束");

			modbusConnectionStarted = true;
			while (modbusConnectionStarted) {
				printInformation(1, "\n" + "modbus终端客户端：进入循环，开始接收消息...");
				try {
					byte[] buffRecv = new byte[64];// 接收缓冲区，
					/*
					int numRecv = this.buffInputStream.read(buffRecv);// ---------------------------------------------------------------------read
					*/
					int numRecv = recvMsg(this.buffInputStream, buffRecv);
					if (numRecv < 0) {
						printInformation(-1, "modbus终端客户端：警告，接收消息出错！");

						// 在state表中插入一条下线的消息
						SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 设置日期格式
						String[] strValuesOfMonitorData = new String[4];
						strValuesOfMonitorData[0] = df.format(new Date()).toString();
						strValuesOfMonitorData[1] = this.addressIp.toString(); // 存modbus终端的IP即可
						strValuesOfMonitorData[2] = "FF";
						strValuesOfMonitorData[3] = "off";
						sqlitecrud.insert("State", strValuesOfMonitorData);
						// 将currentState表中的数据删除
						sqlitecrud.delete("CurrentState", "terminalName", this.addressIp.toString());
						modbusConnectionStarted = false;
						
						updateEquipmentStateToHost(
								strValuesOfMonitorData[0] 
								+ ByteUtil.intToString(strValuesOfMonitorData[1].length()) 
								+ strValuesOfMonitorData[1] 
								+ strValuesOfMonitorData[2] 
								+ strValuesOfMonitorData[3]); // 调用“向上位机更新设备状态的方法，更新设备状态信息”
						
						break;
					}

					printInformation(1, "modbus终端客户端：接收的字符的数量为：" + numRecv + ";接收的字符串为：" + ByteUtil.bytesToHexString(buffRecv));

					// 这里将根据消息类型来进行不同的数据处理，消息实体已经存放在缓冲区中
					switch (buffRecv[0]) {
					
					case 0x02:
						printInformation(1, "modbus终端客户端：消息类型：0x02，下位设备上线！");

						/* 处理下位设备上线  */ 
						String strDeviceOn = ByteUtil.bytesToHexString(buffRecv).substring(2, 4); // 下位设备ID

						/**
						 * 设备上线：
						 * 1.查询“当前状态表”；
						 * 2.如果设备之前已经在线，则不用处理；
						 * 3.如果设备之前不在线，则在“当前状态表”和“历史状态表”中各添加一条上线消息
						 * */
						Vector<Vector<Object>> vectortCurrentStateOn = sqlitecrud.selectVectorByTwoKeyValue("CurrentState", "terminalName", this.addressIp.toString(), "deviceName", strDeviceOn);
						printInformation(1, "modbus终端客户端：下位设备上线，modbus终端地址："+this.addressIp.toString() +", 设备号为："+ strDeviceOn );
						
						if(vectortCurrentStateOn.isEmpty()){ // 设备不在线
							printInformation(1, "modbus终端客户端：设备本次在线，之前不在线，需要处理" );
							// 在 两个表中各插入一条消息
							SimpleDateFormat dfOn = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 设置日期格式
							String[] strDeviceOnArray = new String[4];
							strDeviceOnArray[0] = dfOn.format(new Date()).toString();
							strDeviceOnArray[1] = this.addressIp.toString(); // 存modbus终端的IP即可
							strDeviceOnArray[2] = strDeviceOn;
							strDeviceOnArray[3] = "on";
							sqlitecrud.insert("CurrentState", strDeviceOnArray);
							sqlitecrud.insert("State", strDeviceOnArray);
							
							updateEquipmentStateToHost(
									strDeviceOnArray[0] 
									+ ByteUtil.intToString(strDeviceOnArray[1].length()) 
									+ strDeviceOnArray[1] 
									+ strDeviceOnArray[2] 
									+ strDeviceOnArray[3]); // 调用“向上位机更新设备状态的方法，更新设备状态信息”
							
						}else{ // 设备在线
							printInformation(1, "modbus终端客户端：设备本次在线，之前也在线，不处理" );
							// do nothing
						}
						
						break;
						
					case 0x03:
						printInformation(1, "modbus终端客户端：消息类型：0x03，下位设备下线！");

						/* 处理下位设备下线 */
						String strDeviceOff = ByteUtil.bytesToHexString(buffRecv).substring(2,4);
						/**
						 * 设备下线：
						 * 1.查询“当前状态表”；
						 * 2.如果设备之前已经下线，则不用处理；
						 * 3.如果设备之前在线，则先删除“当前状态表”中的相关记录，然后在“历史状态表”中添加一条下线消息
						 * */
						Vector<Vector<Object>> vectortCurrentState = sqlitecrud.selectVectorByTwoKeyValue("CurrentState", "terminalName", this.addressIp.toString(), "deviceName", strDeviceOff);
						printInformation(1, "modbus终端客户端：下位设备下线，modbus终端地址："+this.addressIp.toString() +", 设备号为："+ strDeviceOff );
						
						if(vectortCurrentState.isEmpty() == false){ // 设备在线
							printInformation(1, "modbus终端客户端：设备本次不在线，之前在线，需要处理" );
							
							// 将currentState表中的数据删除
							sqlitecrud.deleteByTwoKeyValue("CurrentState", "terminalName", this.addressIp.toString(), "deviceName", strDeviceOff);
							
							// 在历史记录表中添加一条下线的消息
							SimpleDateFormat dfOn = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 设置日期格式
							String[] strDeviceOffArray = new String[4];
							strDeviceOffArray[0] = dfOn.format(new Date()).toString();
							strDeviceOffArray[1] = this.addressIp.toString(); // 存modbus终端的IP即可
							strDeviceOffArray[2] = strDeviceOff;
							strDeviceOffArray[3] = "off";
							sqlitecrud.insert("State", strDeviceOffArray);
							
							updateEquipmentStateToHost(
									strDeviceOffArray[0] 
									+ ByteUtil.intToString(strDeviceOffArray[1].length()) 
									+ strDeviceOffArray[1] 
									+ strDeviceOffArray[2] 
									+ strDeviceOffArray[3]); // 调用“向上位机更新设备状态的方法，更新设备状态信息”
							
						}else{ // 设备不在线
							printInformation(1, "modbus终端客户端：设备本次不在线，之前也不在线，不处理" );
							// do nothing
						}
						

						break;
						
					case 0x04:
						printInformation(1, "modbus终端客户端：消息类型：0x04，打开串口成功！");
						/*
						this.buffOutputStream.write(buffRecv);// ---------------------------------------------------------------------write
						this.buffOutputStream.flush();
						*/
						sendMsg(this.buffOutputStream, buffRecv,1);
						break;
						
					case 0x05:
						printInformation(1, "modbus终端客户端：消息类型：0x05，打开串口失败！");
						/*
						this.buffOutputStream.write(buffRecv);// ---------------------------------------------------------------------write
						this.buffOutputStream.flush();
						*/
						sendMsg(this.buffOutputStream, buffRecv,1);
						break;
						
					case 0x07:
						printInformation(1, "modbus终端客户端：消息类型：0x07，收到Server发送的modbus命令，modbus终端刚上线");
						break;
						
					case 0x09:
						printInformation(1, "modbus终端客户端：消息类型：0x09，收到Server程序发送的系统时间");
						break;
						
					case 0x0A:
						printInformation(1, "modbus终端客户端：消息类型：0x0A，modbus终端发送监测数据给server服务器");
						sendMsg(this.buffOutputStream, buffRecv,1);
						
						/**---解析从modbus终端发送过来的消息
						 * 消息构成：功能码 + 时间 + modbusdata
						 * 消息长度：时间(19个字节)；modbusdata(剩余全是)
						 * */
						// 1.将byte[]转换成string
						String strRecvMsg = new String(buffRecv,0,numRecv); // 直接将字节数组按照ascll码的方式转换成string类型
						
						// 2.截取时间
						String strTime = strRecvMsg.substring(1, 20); // 第一位是功能码，后面的19位是时间2016-04-06 12:12:12
						
						//01031400010000000000000000000100000000000000003F
						//0103140001000000000000000000010000000000000000930b
						
						// 3.截取modbusdata，这里需要转换：string->byte[]->string(16进制格式),!!!这里numRecv-1：因为要减去自己定义的功能码
						//String strModbusData = ByteUtil.bytesToHexString(strRecvMsg.substring(20, numRecv-1).getBytes()).toUpperCase();
						String strModbusData = ByteUtil.bytesToHexString(buffRecv).substring(20*2, numRecv*2).toUpperCase();
						printInformation(1, "modbus终端客户端：消息类型，收到modbus命令0x0A，时间："+strTime+"、数据："+strModbusData);
						
						// 4.将监测数据存放进数据库
						String[] strValuesOfMonitorData = new String[4];
						strValuesOfMonitorData[0] = strTime;
						strValuesOfMonitorData[1] = this.addressIp.toString(); // 存modbus终端的IP即可
						strValuesOfMonitorData[2] = strModbusData.substring(0, 2);
						strValuesOfMonitorData[3] = strModbusData.substring(2, strModbusData.length());
						sqlitecrud.insert("MonitorData", strValuesOfMonitorData);
						
						// 5.将监测数据实时发送给上位机，格式：功能码 + 时间 + 地址长度 + modbus终端的IP地址 + 下位设备ID + modbusdata
						String strModbusDataToHost = new String(new byte[] {0x0B}) ; // 消息类型码0x0B
						strModbusDataToHost = strModbusDataToHost + strValuesOfMonitorData[0];
						strModbusDataToHost = strModbusDataToHost + ByteUtil.intToString(strValuesOfMonitorData[1].length()); // IP地址的长度
						strModbusDataToHost = strModbusDataToHost + strValuesOfMonitorData[1];
						strModbusDataToHost = strModbusDataToHost + strValuesOfMonitorData[2];
						strModbusDataToHost = strModbusDataToHost + strValuesOfMonitorData[3];
						
						printInformation(1, "modbus终端客户端：发送给上位机的实时modbusdata监测数据为：" + strModbusDataToHost);
						
						// 6.将modbusdata实时监测数据发送给上位机
						byte[] byteModbusDataToHost = strModbusDataToHost.getBytes();
						for(int m = 0; m< hostClientList.size(); m++){
							sendMsg(hostClientList.get(m).buffOutputStream, byteModbusDataToHost, byteModbusDataToHost.length);// ---------------------------------------------------------------------write
						}
						break;
						
					case 0x11:
						printInformation(1, "消息类型：0x11:Server服务器程序添加modbus命令成功。");
						break;
						
					case 0x12:
						printInformation(1, "消息类型：0x12:Server服务器程序删除modbus命令成功。");
						break;
						
					default:
						break;
					}
				} catch (IOException e) {// 捕获客户端进程结束的异常，关闭这个进程，并在数据库中写入相关下线信息
					printInformation(-1, "modbus终端客户端：警告，该连接出现问题，异常处理，关闭套接字输入输出流...");

					// 在state表中插入一条下线的消息
					SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 设置日期格式
					printInformation(1, "modbus终端客户端：当前系统时间" + df.format(new Date()));// new
																						// Date()为获取当前系统时间
					String[] strValuesOfMonitorData = new String[4];
					strValuesOfMonitorData[0] = df.format(new Date()).toString();
					strValuesOfMonitorData[1] = this.addressIp.toString(); // 存modbus终端的IP即可
					strValuesOfMonitorData[2] = "FF";
					strValuesOfMonitorData[3] = "off";
					sqlitecrud.insert("State", strValuesOfMonitorData);
					// 将currentState表中的数据删除
					sqlitecrud.delete("CurrentState", "terminalName", this.addressIp.toString());
					modbusConnectionStarted = false;
					
					try {
						updateEquipmentStateToHost(
								strValuesOfMonitorData[0] 
								+ ByteUtil.intToString(strValuesOfMonitorData[1].length()) 
								+ strValuesOfMonitorData[1] 
								+ strValuesOfMonitorData[2] 
								+ strValuesOfMonitorData[3]);// 调用“向上位机更新设备状态的方法，更新设备状态信息”
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} 
					
					break;
				}
				printInformation(1, "modbus终端客户端：处理该消息结束！！！");
			}
			try {
				this.socket.close();
			} catch (IOException e1) {
			}
			try {
				this.buffInputStream.close();
			} catch (IOException e1) {
			}
			try {
				this.buffOutputStream.close();
			} catch (IOException e1) {
			}
			// 将自己从 list 中删除
			modbusClientList.remove(modbusClientList.indexOf(this));
			
			/**
			 * >>>存在的问题：
			 * 1.这里需要将该modbus终端下的modbus仪表设备的状态进行修改
			 * */
			
			printInformation(-1, "modbus终端客户端：modbusclient的长度为:" + modbusClientList.size());

			printInformation(1, "modbus终端客户端：该线程出现异常，本线程将自动结束！");
		}
	}

	
	/**向modbus终端发送modbus命令/系统时间等消息 辅助线程-------------------------------------------------------------------- */
	/** ----------------------------------------------------------------------------------------------------------------- */
	/**
	 * @author CongXiang 
	 * 功能：
	 * 1.消息类型：0x09，获取系统时间->发送给modbus终端
	 * 2.消息类型：0x07，获取modbusorder->发送给modbus终端
	 * */
	class SendModbusMsgToTreminalThread extends Thread {

		//public boolean modbusMsgStarted = true;
		private BufferedOutputStream buff;
		
		public SendModbusMsgToTreminalThread(BufferedOutputStream buffOutputStream) {
			super();
			buff= buffOutputStream;
		}

		@Override
		public void run() {
			try {

				Thread.sleep(1000);

				/* 获取系统时间，并将系统时间发送给modbus终端 */
				// 1.获取系统时间
				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 设置日期格式
				String strCurrentSystemTime = df.format(new Date()).toString();
				// 2.组合消息0x09
				strCurrentSystemTime = new String(new byte[] { 0x09 }) + strCurrentSystemTime; // 前面加上消息类型0x09
				byte[] buffTimeSend = strCurrentSystemTime.getBytes();
				// 3.将消息发送给modbus终端
				sendMsg(buff, buffTimeSend, buffTimeSend.length);

				// -------------------------

				/* 获取modbusorder，并将modbusorder发送给modbus终端 */
				// 1.判断在线终端不为空
				printInformation(1, "//---辅助线程:进入循环[用于将modbus消息发送给modbus终端]...");
				int countModbusMsg = sqlitecrud.getTableCount("ModbusMsg");
				// 2.判断数据库中有modbusorder
				if (countModbusMsg > 0) { // 数据库中有数据，这是就需要将数据发送给modbus终端,
					printInformation(0, "//---辅助线程:数据库中有数据");
					// 3.从数据库中获取modbusorder
					Object[][] objModbusMsg = sqlitecrud.selectObject("ModbusMsg");// 从数据库中将modbus命令从数据库中取出来
					// 4.遍历所有的modbusorder
					for (int i = 0; i < objModbusMsg.length; i++) {
						// 5.组装0x07类型的消息
						byte[] buffsend = ByteUtil.hexStringToBytes("07" + objModbusMsg[i][0].toString());
						printInformation(1, "//---辅助线程:终于发送数据" + objModbusMsg[i][0].toString());
						// 6.将0x07类型的消息发送给modbus终端
						sendMsg(buff, buffsend, buffsend.length);
						Thread.sleep(1000);
					}
				}
			} catch (InterruptedException | IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**向上位机发送modbus命令 辅助线程-------------------------------------------------------------------- */
	/** ----------------------------------------------------------------------------------------------------------------- */
	/**
	 * @author CongXiang 
	 * 功能：
	 * 1.消息类型：0x0E，
	 * 2.消息格式：0x0E+modbus命令，从数据库中取出，依次发送
	 * */
	class SendModbusOrderToHostThread extends Thread {
		private BufferedOutputStream buff;
		
		public SendModbusOrderToHostThread(BufferedOutputStream buffOutputStream) {
			super();
			buff = buffOutputStream;
		}
		
		public void run() {
			try {
				// 1.从数据库中将modbus命令取出
				int countModbusMsg = sqlitecrud.getTableCount("ModbusMsg");
				if (countModbusMsg > 0) { // 数据库中有数据
					Object[][] objModbusMsg = sqlitecrud.selectObject("ModbusMsg");// 从数据库中将modbus命令从数据库中取出来

					// 2.建立一个for循环，依次发送modbus命令
					for (int i = 0; i < objModbusMsg.length; i++) {

						// 3.组装0x0E类型的消息
						String strModbusOrder = new String(new byte[] {0x0E}) + objModbusMsg[i][0].toString(); 
						byte[] buffsend = strModbusOrder.getBytes();

						// 4.发送消息给上位机
						sendMsg(buff, buffsend, buffsend.length);// ---------------------------------------------------------------------write
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * ---向上位机更新设备状态信息
	 * @author CongXiang
	 * @throws IOException 
	 * 
	 * */
	public void updateEquipmentStateToHost(String strModbusSingleStateData) throws IOException{
		/** 上位机向server服务器请求设备状态树 */
		/**
		 * 1.返回的数据内容 1.第一位是消息类型：08
		 * 2.第二位是IP的长度：xx,因为IP地址的长度是不一定，需要指定长度
		 * 3.第三位是IP地址：xxxx(长度由前一个字段指定)
		 * 4.第四位是下位设备号，FF代表没有下位设备：xx(两个字节)
		 * 5.所有IP和设备号在后面顺序连接，一起发送，由上位机组装
		 * 
		 * 注意：IP地址存的不是16进制数，与string之间的转换不用使用工具类
		 * */
		
		String strStateData = new String(new byte[] { 0x08 });; 

		int countModbusMsg = sqlitecrud.getTableCount("CurrentState");
		if (countModbusMsg > 0) { // 判断数据库里是否有数据

			// 从数据库中将modbus命令从数据库中取出来
			Object[][] objStateData = sqlitecrud.selectObject("CurrentState"); // 所有状态数据

			// 获取所有数据的数量，这样才能确定循环的次数，
			int numCount = sqlitecrud.getTableCount("CurrentState");

			String strTerminalName; // modbus终端的IP地址
			String strDeviceName; // 下位设备的编号
			String lengthOfTerminalName;

			for (int i = 0; i < numCount; i++) {
				/* 从数据库中查找的数据不需要排序，在上位机将会进行数据的树状表示 */

				strTerminalName = objStateData[i][1].toString(); // 表示modbus终端IP字段
				strDeviceName = objStateData[i][2].toString(); // 表示下位设备编号字段
				lengthOfTerminalName = ByteUtil.intToString(strTerminalName.length()); // IP的长度，一般小于两位数，需要转换
				strStateData = strStateData + lengthOfTerminalName + strTerminalName + strDeviceName;
			}
		}
		printInformation(1, "【方法】发送给上位机的状态数据为：" + strStateData);

		// 发送数据
		byte[] byteStateData = strStateData.getBytes();
		for(int m = 0; m< hostClientList.size(); m++){
			sendMsg(hostClientList.get(m).buffOutputStream, byteStateData, byteStateData.length);// ---------------------------------------------------------------------write
		}
		
		/** ------------------------------------------------------------------------------------------------------------------ */
		
		/* 发送单条状态数据给上位机 */
		// 1.从数据库中获取最新的一条记录：功能码 + 时间 + 地址长度 + modbus终端的IP地址 + 下位设备ID + 是否在线状态
/*		Object[] objModbusData = sqlitecrud.selectLatestObject("State","time"); // 以时间降序
		
		String strModbusSingleStateData = new String(new byte[] {0x0C}) ; // 消息类型码0x0C
		if(objModbusData[0] != null){ // 如果查到数据
			// 2.组装modbusdata监测数据，原则：功能码用16进制数表示
			strModbusSingleStateData = strModbusSingleStateData + objModbusData[0].toString(); // 时间
			strModbusSingleStateData = strModbusSingleStateData + ByteUtil.intToString(objModbusData[1].toString().length()); // IP地址的长度
			strModbusSingleStateData = strModbusSingleStateData + objModbusData[1]; // modbus终端的IP地址
			strModbusSingleStateData = strModbusSingleStateData + objModbusData[2]; // 下位设备ID
			strModbusSingleStateData = strModbusSingleStateData + objModbusData[3]; // 是否在线的状态
		}
*/
		printInformation(1, "【方法】发送给上位机的实时设备状态信息为：" + strModbusSingleStateData);
		
		// 3.将单条modbusdata实时设备状态消息发送给上位机
		byte[] byteModbusSingleStateData = (new String(new byte[] { 0x0C }) + strModbusSingleStateData).getBytes();
		for(int m = 0; m< hostClientList.size(); m++){
			sendMsg(hostClientList.get(m).buffOutputStream, byteModbusSingleStateData, byteModbusSingleStateData.length);// ---------------------------------------------------------------------write
		}
		
		
	}
	
	
	
	/**
	 * ---重写buffOutputStream.write()方法【为了应对TCP连接的粘包问题】
	 * @author CongXiang
	 * 参数：
	 * 1.BufferedOutputStream buffOutputStream; // 输出流
	 * 2.byte typeOfMsg; // 消息类型,new String(new byte[] {0x09})
	 * 3.string contentOfMsg; // 输出缓冲区
	 * 4.int lengthOfMsg; // 发送字节数
	 * 返回值：
	 * 1:成功发送数据
	 * 0:发送数据失败
	 * @throws IOException 
	 * */
	public int sendMsg(BufferedOutputStream buffOutputStream, byte[] contentOfMsg, int lengthOfMsg) throws IOException {
		// //String type = new String(new byte[] { typeOfMsg }); //
		// 将byte消息类型转换成string，为了方便下面的string.getbytes();
		// //byte[] buffsend = (type + contentOfMsg).getBytes(); // 发送缓冲区
/*
		buffOutputStream.write(contentOfMsg);// ---------------------------------------------------------------------write
		buffOutputStream.flush();
		printInformation(1, "【方法】发送消息:发送的数据类型：" + contentOfMsg[0]);
*/
		
		// 1.定义发送缓冲区，缓冲区的长度为：lengthOfMsg + 1 + 1
		byte[] byteSendBuffArray = new byte[lengthOfMsg + 2];
		
		// 2.在数据缓冲区中添加“消息体长度”字段：1个字节，用16进制数表示
		byteSendBuffArray[0] = (byte) lengthOfMsg; // 强制类型转换
		
		// 3.在数据缓冲区中添加“消息体”：lengthOfMsg个字节
		for (int i = 0; i < lengthOfMsg; i++) {
			byteSendBuffArray[i+1] = contentOfMsg[i];
		}
		
		// 4.在数据缓冲区中添加“停止符号”：1个字节
		byteSendBuffArray[lengthOfMsg+1] = (byte) 0xFF;
		
		// 5.发送缓冲区中所有数据
		buffOutputStream.write(byteSendBuffArray);// ---------------------------------------------------------------------write
		buffOutputStream.flush();
		printInformation(1, "【方法】发送消息:发送的数据类型:" + contentOfMsg[0]+",消息长度为:"+(int)byteSendBuffArray[0]);
		
		return 1;
	}
	
	/**
	 * ---重写bufferInputStream.read()方法【为了应对TCP连接的粘包问题】
	 * @author CongXiang
	 * 参数：
	 * 1.BufferedInputStream bufferInputStream ; // 输入流
	 * 2.byte[] recvBuff; // 接收缓冲区
	 * 3.
	 * 返回值：
	 * recvNum; // 接收的字节数量
	 * @throws IOException 
	 * */
	public int recvMsg(BufferedInputStream bufferInputStream, byte[] recvBuff) throws IOException {
/*		
		int num; // 记录接收的字节数量
		num = bufferInputStream.read(recvBuff);
		return num;
*/
		// 1.读入一个字符：消息体的长度lengthOfMsg
		byte[] byteTemp = new byte[1]; // 一个字节即可
		int lengthOfMsg;
		bufferInputStream.read(byteTemp, 0, 1);
		lengthOfMsg = (int) byteTemp[0];
		
		// 2.读入lengthOfMsg个字符
		int recvNum = bufferInputStream.read(recvBuff, 0, lengthOfMsg);
		
		// 3.再读入一个字符，判断是不是“停止符号0xFF”
		bufferInputStream.read(byteTemp, 0, 1);
		
		if((byteTemp[0] == (byte) 0xFF) && (recvNum == lengthOfMsg)){
			// 4.如果是停止符号，并且recvNum == lengthOfMsg 则说明接收正确
			return recvNum;
		}else{
			// 5.如果不是停止符号，或者recvNum != lengthOfMsg，则说明接收错误。>>>接收错误处理:不停的读入下一个字符，知道读到0xFF为止
			while(bufferInputStream.read(byteTemp, 0, 1) == 1){
				if(byteTemp[0] == 0xFF){
					break;
				}
			}
			return -1; // 标识接收数据出错
		}

	}

	// server面板上的消息相应事件
	@Override
	public void actionPerformed(ActionEvent e) {

		if (e.getSource() == serverPanel.btOpenServer) { /*--- 打开服务器*/
			serverPanel.btOpenServer.setEnabled(false);
			serverPanel.btCloseServer.setEnabled(true);
			// ---------------------------------- 启动主线程
			mainThread = new MainThread();
			mainThread.start();

		} else if (e.getSource() == serverPanel.btCloseServer) { /*--- 关闭服务器*/
			serverPanel.btOpenServer.setEnabled(true);
			serverPanel.btCloseServer.setEnabled(false);
			/* 如何正确关闭所有线程以及其他代码 */

			try {
				// 1.关闭主线程，同时关闭套接字、输入输出流
				mainThread.serverSocket.close();
				mainThread.interrupt();

				// 2.关闭Modbus终端客户端线程
				for (int i = 0; i < modbusClientList.size(); i++) {
					modbusClientList.get(i).socket.close();
				}
				
				// 3.关闭上位机客户端线程
				for (int i = 0; i < hostClientList.size(); i++) {
					hostClientList.get(i).socket.close();
				}
				
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}
	/**
	 * ---打印服务器程序的状态
	 * @author CongXiang 
	 * 思路：根据标识量的不同，选择是“控制台直接输出”还是“在程序界面输出”
	 * 参数：
	 * 1.int systemOrApplication; // 消息答应方式
	 * 2.String strMsg; // 消息内容
	 * */
	public void printInformation(int systemOrApplication, String strMsg) {
/*		
		if (systemOrApplication == 0) { // 不输出

		} else if (systemOrApplication == 1) { // 系统输出
			System.out.println(strMsg.trim());
		} else if (systemOrApplication == 2) { // 程序界面输出
			tainfo.append(strMsg + "\n");
			tainfo.selectAll();
		} else if (systemOrApplication == -1) { // 出错消息输出
			System.err.println(strMsg);
		} else {
			System.out.println("输出方式出错：请检查输出方式，-1是出错消息输出，0是系统输出，1是界面输出");
		}
*/
		switch(systemOrApplication){
		case 0: // 不输出
			break;
			
		case 1:// 系统输出
			System.out.println(strMsg.trim());
			break;
			
		case 2:// 程序界面输出
			tainfo.append(strMsg + "\n");
			tainfo.selectAll();
			break;
			
		case -1:// 出错消息输出
			System.err.println(strMsg);
			break;
		default:
			System.out.println("输出方式出错：请检查输出方式，-1是出错消息输出，1是系统输出，2是界面输出");
			break;
		}
	}

	// 222
	// 主函数方法
	public static void main(String[] args) throws Exception {
		new Server();
	}



}


