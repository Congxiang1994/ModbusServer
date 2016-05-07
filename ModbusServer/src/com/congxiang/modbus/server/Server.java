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

	List<ModbusClient> modbusClientList = new ArrayList<ModbusClient>(); // ����modbus�ն˶���
	List<HostClient> hostClientList = new ArrayList<HostClient>(); // ����modbus�ն˶���

	boolean connectionStarted; // ��ʶ������������ֹ��ʱ����Ϊfalse��������ֹ���߳�
	
	// ������
	public ServerPanel serverPanel = new ServerPanel();

	// ����ؼ�
	JTextArea tainfo = new JTextArea(); // ���ı���
	JScrollPane jsp = new JScrollPane(tainfo); // ��������

	// ���ݿ����
	Connection conn;
	SQLiteCRUD sqlitecrud; // ������Ķ���
	
	// ���߳�
	MainThread mainThread = null;

	// ���췽������Ҫ�ǻ��ƽ����ϵĿؼ����Լ����ü�����
	Server() throws Exception {

		/** �������ݿ⡢�����������ݿ�------------------------------------------------------------------------------------ */

		this.printInformation(0, "����������ʼ�������ݿ⡢�����������ݿ�");
		
		String dataFile = "modbusServer.db"; // ���ݿ��ļ�
		SQLiteConn sqliteConn = new SQLiteConn(dataFile); // �������ݿ�
		conn = sqliteConn.getConnection(); // �������ݿ�����
		sqlitecrud = new SQLiteCRUD(conn); // ����һ�����������

		/** ����������-��ʷ��Ϣ�ı� */
		if (sqlitecrud.createTable("create table if not exists State(time datetime, terminalName varchar(50), deviceName varchar(50), state varchar(50));")) {
			this.printInformation(1, "�������ݿ��:��State�ɹ���");
		}

		/** ����������-��ǰ״̬�ı� */
		if (sqlitecrud.createTable("create table if not exists CurrentState(time datetime, terminalName varchar(50), deviceName varchar(50), state varchar(50));")) {
			this.printInformation(1, "�������ݿ��:��CurrentState�ɹ���");
		}

		/** ���� modbus����ı� --- ���������λ�����͹�������Ϣʵʱ���£�һ���������ж���λ�豸��Щ�����ߵ� */
		if (sqlitecrud.createTable("create table if not exists ModbusMsg(modbusMsg varchar(50));")) {
			this.printInformation(1, "�������ݿ��:��ModbusMsg�ɹ���");
		}

		/** ���� modbus���ݵı� --- ������¼��modbus�ն˷��ص����� */
		if (sqlitecrud.createTable("create table if not exists MonitorData(time datetime, terminalName varchar(50), deviceName varchar(50), data varchar(100));")) {
			this.printInformation(1, "�������ݿ��:��MonitorData�ɹ���");
		}
		/** -------------------------------------------------------------------------------------------------------------- */
		
		/** ��ʾ������ƣ���Ҫһ���ı�������ʵʱ��ʾ��������״̬------------------------------------------------------------ */
		JFrame frame = new JFrame();
		Container mainContainer = frame.getContentPane();
		mainContainer.add(serverPanel);
		frame.setTitle("modbus����������");
		
		/* Ϊ����ϵİ�ť�����Ϣ��Ӧ�¼� */
		serverPanel.btOpenServer.addActionListener(this);
		serverPanel.btCloseServer.addActionListener(this);
		
		frame.setSize(500, 350); // ���ý����С
		// this.setLocationRelativeTo(null); // ���ý�������Ļ������ʾ
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // ���õ����رհ�ť�ܹ��ر������̣�--------------------------------���ｫ��Ҫ��һ�£�����ֱ�ӹرգ���Ϊ�������䵱������С����
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				/* ��д�ر�������ʱ����صĲ��� */
				/*
				 * // �ر�����modbus�ն˿ͻ����߳� for (int i = 0; i <
				 * modbusClientList.size(); i++) {
				 * modbusClientList.get(i).isReadyToSendMsgToModbusTerminal =
				 * false; // while��ʶ����Ϊfalse try {
				 * modbusClientList.get(i).buffInputStream.close();// �ر�������
				 * modbusClientList.get(i).buffOutputStream.close(); // �ر������
				 * modbusClientList.get(i).socket.close(); // �ر�socket
				 * printInformation(1, "�ر�modbus�ն��߳��еı���...");
				 * modbusClientList.remove(i); i--; } catch (IOException e1) {
				 * printInformation(-1, "���棬�ر�modbus�ն��߳��еı���ʧ�ܣ�����"); } }
				 */
				System.out.println("���ڿ���˳���رճ���");
				System.exit(0);
			}
		});
		frame.setVisible(true); // ��ʾ����
		/** -------------------------------------------------------------------------------------------------------------- */

	}
	/** ----------------------------------------------------------------------------------------------------------------- */
	/** ���߳�
	 * @author CongXiang 
	 * ���ܣ����ڽ�����λ����modbus�ն˵���������
	 * */
	class MainThread extends Thread {
		private ServerSocket serverSocket = null; // �������˵��׽���
		private Socket socketClient = null; // �����׽���
		@Override
		public void run() {
			/** ��Ҫ����:��Ҫ����������λ����modbus�ն˵��������� -----------------------------------------------------------------*/
			printInformation(1, "\n" + "���̼߳�����ʼ����......");

			try {
				serverSocket = new ServerSocket(Integer.valueOf(serverPanel.tfPort.getText().trim())); // �����׽���
			} catch (IOException e) {
				e.printStackTrace();
				printInformation(1, "���߳�:�������������׽���ʧ�ܣ�");
			}

			printInformation(1, "���߳�:�������׽��ִ����ɹ�����һ���ȴ����տͻ��˵���������...");

			connectionStarted = true;

			while (connectionStarted == true) {// ���߳���Ҫ��һ����ѭ������Ҫ����modbus�ն˵�����
				printInformation(1, "\n" + "���̣߳�����ѭ��,�ȴ��µ���������...");
				try {
					socketClient = serverSocket.accept(); // ���տͻ�������
					printInformation(1, "���߳�:�пͻ����������ӣ���һ���ж�����λ������modbus�ն�");

					// �жϿͻ�������λ������modbus�ն�,����ͨ�����յĵ�һ���ַ����ж�
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
						connectionStarted = false; // ������ճ�������ֹ���߳�
						printInformation(-1, "���߳�:���棬�����ַ�����");
						break;
					}

					printInformation(1, "���߳�:���յ��ַ�������Ϊ��" + num + ";���յ��ַ���Ϊ��" + bufferID[0]);
					printInformation(1, "���߳�:�ɹ������յ��ַ������ظ��������ӵĿͻ���");

					// �ж����������͵Ŀͻ�������
					if (bufferID[0] == 0x00) {
						/*
						buffOutputStream.write(bufferID);// ---------------------------------------------------------------------write
						buffOutputStream.flush();
						*/
						sendMsg(buffOutputStream, bufferID,1);
						/** ------��λ������ */
						printInformation(1, "���߳�:����λ����������");
						HostClient hostClient = new HostClient(socketClient, inputStream, outputStream); // �½�һ����λ������
						hostClientList.add(hostClient); // ����λ��������ӵ�List������
						printInformation(1, "���߳�:����λ���߳���ӵ��߳��༯����");
						new Thread(hostClient).start(); // ������λ�����߳�
						printInformation(1, "���߳�:������λ���̳߳ɹ�");

					} else if (bufferID[0] == 0x01) {
						/*
						buffOutputStream.write(bufferID);// ---------------------------------------------------------------------write
						buffOutputStream.flush();
						*/
						sendMsg(buffOutputStream, bufferID,1);
						/** ------modbus�ն����� */
						printInformation(1, "���߳�:��modbus�ն���������");
						ModbusClient modbusClient = new ModbusClient(socketClient, inputStream, outputStream); // �½�һ��modbus�ն˶���
						modbusClientList.add(modbusClient); // ��modbus������ӵ�List������
						new Thread(modbusClient).start(); // ����modbus�ն˵��߳�
					} else {
						/** ------����δ֪����Դ */
						socketClient.close();// �رմ�socket����
						printInformation(-1, "���߳�:���棬����δ֪����Դ�����Զ��رմ�����");
					}

					// ������Ҫ������������ر�

				} catch (IOException e) {
					// e.printStackTrace();
					printInformation(-1, "���߳�:���棬���տͻ�����������ʧ�ܣ�");
					try {
						serverSocket.close();
						// socketClient.close();
					} catch (IOException e1) {
					}
					connectionStarted = false;

					// �ر����������߳�
					//modbusThread.modbusMsgStarted = false;
					break;
				}
			}
			/** -------------------------------------------------------------------------------------------------------------- */
			printInformation(-1, "���߳�:���߳̽���������");

		}
		
	}
	/**��λ���ͻ��� �����߳�------------------------------------------------------------------------------------------------ */
	/** ------------------------------------------------------------------------------------------------------------------ */
	class HostClient implements Runnable {

		private Socket socket; // �׽���
		private InetAddress addressIp; // �ͻ��˵�IP
		private int portNum; // �ͻ�������Ķ˿ں�
		boolean hostConnectionStarted;
		private BufferedInputStream buffInputStream;
		private BufferedOutputStream buffOutputStream;

		@Override
		public String toString() {
			return "��λ���ͻ��ˣ�HostClient [addressIp=" + addressIp + ", portNum=" + portNum + "]";
		}

		// ���췽��
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
			printInformation(1, "��λ���ͻ��ˣ���ʼ������λ���ͻ����߳�");
			hostConnectionStarted = true;
			
			/* ��������modbus�������λ�����߳�  */ 
			SendModbusOrderToHostThread sendModbusOrderToHostThread = new SendModbusOrderToHostThread(buffOutputStream);
			sendModbusOrderToHostThread.start();
			
			while (hostConnectionStarted) {
				printInformation(1, "\n" + "��λ���ͻ��ˣ�����ѭ������ʼ������Ϣ......");
				byte[] buffRecv = new byte[64];// ���յ�һ���ֽڵĻ�����
				try {
					/*
					int numRecv = this.buffInputStream.read(buffRecv); // ---------------------------------------------------------------------read
					*/
					int numRecv = recvMsg(this.buffInputStream, buffRecv);
					if (numRecv < 0) {
						printInformation(-1, "��λ���ͻ��ˣ����棬������Ϣ����");
						hostConnectionStarted = false;
						break;
					}
					//printInformation(1, "��λ���ͻ��ˣ����յ��ַ�������Ϊ��" + numRecv);
					printInformation(1, "��λ���ͻ��ˣ����յ��ַ�������Ϊ��" + numRecv + ";���յ��ַ���Ϊ��" + ByteUtil.bytesToHexString(buffRecv));

					// ���ｫ����firstBuffer�����������в�ͬ�����ݴ���
					printInformation(1, "��λ���ͻ��ˣ������������ݽ��յ��ַ��ж���Ϣ����...");
					switch (buffRecv[0]) {
					
					case 0x06:
						printInformation(1, "��λ���ͻ��ˣ���Ϣ����0x06����λ������modbus�����server����������");
						/*
						this.buffOutputStream.write(buffRecv[0]);// ---------------------------------------------------------------------write
						this.buffOutputStream.flush();
						*/
						sendMsg(this.buffOutputStream, buffRecv,1);
						/** ��λ������modbus�����server������ */
						

						// ����Ϣ��ȡ��modbus����
						String strModbus = ByteUtil.bytesToHexString(buffRecv).substring(2, numRecv * 2).toUpperCase();
						// strModbus = strModbus; //
						// ��Ҫ��2����Ϊÿ���ֽ�ת����string,��ռ�������ַ�
						printInformation(1, "��λ���ͻ��ˣ�modbus����Ϊ��" + strModbus + "��");

						// ���յ���modbus������в�֣�ÿ��modbus���Ϊ16
						for (int i = 0; i < strModbus.length(); i = i + 16) {
							String strM = strModbus.substring(i, i + 16);

							// ��modbus����Ž����ݿ⣬����֮ǰҪ�鿴�����Ƿ��Ѿ�����
							String[] str = new String[1];
							str[0] = strM;

							if (sqlitecrud.getTableCount("ModbusMsg", "modbusMsg", strM) > 0) {
								printInformation(1, "��λ���ͻ��ˣ����ݿ����Ѿ���������modbus���");
							} else {
								sqlitecrud.insert("ModbusMsg", str);
								printInformation(1, "��λ���ͻ��ˣ����һ���ߵ�modbus���");
							}
						}
						break;
						
					case 0x08:
						printInformation(1, "��λ���ͻ��ˣ���Ϣ����0x08��server����������λ�������豸״̬���ݣ�");
						/** ��λ����server�����������豸״̬�� */
						/**
						 * 1.���ص��������� 1.��һλ����Ϣ���ͣ�08
						 * 2.�ڶ�λ��IP�ĳ��ȣ�xx,��ΪIP��ַ�ĳ����ǲ�һ������Ҫָ������
						 * 3.����λ��IP��ַ��xxxx(������ǰһ���ֶ�ָ��)
						 * 4.����λ����λ�豸�ţ�FF����û����λ�豸��xx(�����ֽ�)
						 * 5.����IP���豸���ں���˳�����ӣ�һ���ͣ�����λ����װ
						 * 
						 * ע�⣺IP��ַ��Ĳ���16����������string֮���ת������ʹ�ù�����
						 * */
/*						
						String strStateData = new String(new byte[] { 0x08 });; 

						int countModbusMsg = sqlitecrud.getTableCount("CurrentState");
						if (countModbusMsg > 0) { // �ж����ݿ����Ƿ�������

							// �����ݿ��н�modbus��������ݿ���ȡ����
							Object[][] objStateData = sqlitecrud.selectObject("CurrentState"); // ����״̬����

							// ��ȡ�������ݵ���������������ȷ��ѭ���Ĵ�����
							int numCount = sqlitecrud.getTableCount("CurrentState");

							String strTerminalName; // modbus�ն˵�IP��ַ
							String strDeviceName; // ��λ�豸�ı��
							String lengthOfTerminalName;

							for (int i = 0; i < numCount; i++) {
								 �����ݿ��в��ҵ����ݲ���Ҫ��������λ������������ݵ���״��ʾ 

								strTerminalName = objStateData[i][1].toString(); // ��ʾmodbus�ն�IP�ֶ�
								strDeviceName = objStateData[i][2].toString(); // ��ʾ��λ�豸����ֶ�
								lengthOfTerminalName = ByteUtil.intToString(strTerminalName.length()); // IP�ĳ��ȣ�һ��С����λ������Ҫת��
								strStateData = strStateData + lengthOfTerminalName + strTerminalName + strDeviceName;
							}
						}
						printInformation(1, "��λ���ͻ��ˣ����͸���λ����״̬����Ϊ��" + strStateData);

						// ��������
						byte[] byteStateData = strStateData.getBytes();
						sendMsg(this.buffOutputStream, byteStateData);// ---------------------------------------------------------------------write
*/
						break;
						
					case 0x0B:
						printInformation(1, "��λ���ͻ��ˣ���Ϣ����0x0B��server����������λ������ʵʱmodbusdata������ݣ�");
						
						/* �����ݿ���ȡ���µ�һ������ �������͸���λ���ͻ���*/
/*						
						// 1.�����ݿ��л�ȡ���µ�һ����¼�������� + ʱ�� + ��ַ���� + modbus�ն˵�IP��ַ + ��λ�豸ID + modbusdata
						Object[] objModbusData = sqlitecrud.selectLatestObject("MonitorData","time"); // ��ʱ�併��
						
						String strModbusData = new String(new byte[] {0x0B}) ; // ��Ϣ������0x08
						if(objModbusData[0] != null){ // ����鵽����
							// 2.��װmodbusdata������ݣ�ԭ�򣺹�������16��������ʾ
							strModbusData = strModbusData + objModbusData[0].toString(); // ʱ��
							strModbusData = strModbusData + ByteUtil.intToString(objModbusData[1].toString().length()); // IP��ַ�ĳ���
							strModbusData = strModbusData + objModbusData[1]; // modbus�ն˵�IP��ַ
							strModbusData = strModbusData + objModbusData[2]; // ��λ�豸ID
							strModbusData = strModbusData + objModbusData[3]; // modbusdata�������
						}
						printInformation(1, "��λ���ͻ��ˣ����͸���λ����ʵʱmodbusdata�������Ϊ��" + strModbusData);
						
						// 3.��modbusdataʵʱ������ݷ��͸���λ��
						byte[] byteModbusData = strModbusData.getBytes();
						sendMsg(this.buffOutputStream, byteModbusData);// ---------------------------------------------------------------------write
*/									
						break;
						
					case 0x0D:
						printInformation(1, "��λ���ͻ��ˣ���Ϣ����0x0D����server����������ǰ�豸״̬First��");
						/** ��λ����server�����������豸״̬�� */
						/**
						 * 1.���ص��������� 1.��һλ����Ϣ���ͣ�08
						 * 2.�ڶ�λ��IP�ĳ��ȣ�xx,��ΪIP��ַ�ĳ����ǲ�һ������Ҫָ������
						 * 3.����λ��IP��ַ��xxxx(������ǰһ���ֶ�ָ��)
						 * 4.����λ����λ�豸�ţ�FF����û����λ�豸��xx(�����ֽ�)
						 * 5.����IP���豸���ں���˳�����ӣ�һ���ͣ�����λ����װ
						 * 
						 * ע�⣺IP��ַ��Ĳ���16����������string֮���ת������ʹ�ù�����
						 * */
						
						String strStateData = new String(new byte[] { 0x0D });; 

						int countModbusMsg = sqlitecrud.getTableCount("CurrentState");
						if (countModbusMsg > 0) { // �ж����ݿ����Ƿ�������

							// �����ݿ��н�modbus��������ݿ���ȡ����
							Object[][] objStateData = sqlitecrud.selectObject("CurrentState"); // ����״̬����

							// ��ȡ�������ݵ���������������ȷ��ѭ���Ĵ�����
							int numCount = sqlitecrud.getTableCount("CurrentState");

							String strTerminalName; // modbus�ն˵�IP��ַ
							String strDeviceName; // ��λ�豸�ı��
							String lengthOfTerminalName;

							for (int i = 0; i < numCount; i++) {
								 //�����ݿ��в��ҵ����ݲ���Ҫ��������λ������������ݵ���״��ʾ 
								strTerminalName = objStateData[i][1].toString(); // ��ʾmodbus�ն�IP�ֶ�
								strDeviceName = objStateData[i][2].toString(); // ��ʾ��λ�豸����ֶ�
								lengthOfTerminalName = ByteUtil.intToString(strTerminalName.length()); // IP�ĳ��ȣ�һ��С����λ������Ҫת��
								strStateData = strStateData + lengthOfTerminalName + strTerminalName + strDeviceName;
							}
						}
						printInformation(1, "��λ���ͻ��ˣ���һ�Σ��������͸���λ����״̬����Ϊ��" + strStateData);

						// ��������
						byte[] byteStateData = strStateData.getBytes();
						sendMsg(this.buffOutputStream, byteStateData, byteStateData.length);// ---------------------------------------------------------------------write
						break;
						
					case 0x0E:
						printInformation(1, "��λ���ͻ��ˣ���Ϣ����0x0E��Server������һ��modbus�������λ����");
						break;
						
					case 0x0F:
						printInformation(1, "��λ���ͻ��ˣ���Ϣ����0x0F:��λ�����modbus����");
						
						// 1.�������յ�������
						String strInsertModbusOrder = new String(buffRecv).substring(1, 17); // ֱ�ӽ��ֽ����鰴��ascll��ķ�ʽת����string����
						printInformation(1, "��λ���ͻ��ˣ���λ�����modbus����:"+strInsertModbusOrder);
						
						// 2.��ѯ���ݿ����Ƿ��Ѿ���������modbus����
						if (sqlitecrud.getTableCount("ModbusMsg", "modbusMsg", strInsertModbusOrder) > 0) {
							printInformation(1, "��λ���ͻ��ˣ����ݿ����Ѿ���������modbus���");
						} else {
							
							// 3.�����ݿ������һ��modbus����
							String[] strArrayInsertModbusOrder = new String[1];
							strArrayInsertModbusOrder[0] = strInsertModbusOrder;
							sqlitecrud.insert("ModbusMsg", strArrayInsertModbusOrder);
							printInformation(1, "��λ���ͻ��ˣ����һ���µ�modbus���");
							
							
							/* --->>>�����modbus�������Ϣ���͸�modbus�նˣ���Ϣ��ʽ��0x11+modbus���� */
							// 1.�ж����ߵ�modbus�ն������Ƿ����0,����forѭ����
							for (int i = 0; i < modbusClientList.size(); i++) {
								// 2.��װ��Ϣ��0x11+modbus����
								byte[] buffsend = ByteUtil.hexStringToBytes("11" + strInsertModbusOrder);
								printInformation(1, "��λ���ͻ��ˣ�Server��������modbus����ת����modbus�նˣ�"+strInsertModbusOrder);
								// 3.������Ϣ��modbus�ն�
								sendMsg(modbusClientList.get(i).buffOutputStream, buffsend, buffsend.length);
							}
							/* -------------------------------------------------------------------- */
						}
						
						// 4.����Ϣ����0x0F���ظ���λ��������Ϣ�����������͵����������
						byte[] buff0F = new byte[] { 0x0F };
						sendMsg(buffOutputStream, buff0F, 1);//---------------------------------------------------------------------write

						break;
						
					case 0x10:
						printInformation(1, "��λ���ͻ��ˣ���Ϣ����0x10:��λ��ɾ��modbus����");
						
						// 1.�������յ�������
						String strDeleteModbusOrder = new String(buffRecv).substring(1, 17); // ֱ�ӽ��ֽ����鰴��ascll��ķ�ʽת����string����
						printInformation(1, "��λ���ͻ��ˣ���λ��ɾ��modbus����:"+strDeleteModbusOrder);
						
						// 2.�����ݿ���ɾ����������
						sqlitecrud.delete("ModbusMsg", "modbusMsg", strDeleteModbusOrder);
						
						/* --->>>��ɾ��modbus�������Ϣ���͸�modbus�նˣ���Ϣ��ʽ��0x12+modbus���� */
						// 1.�ж����ߵ�modbus�ն������Ƿ����0,����forѭ����
						for (int i = 0; i < modbusClientList.size(); i++) {
							// 2.��װ��Ϣ��0x11+modbus����
							byte[] buffsend = ByteUtil.hexStringToBytes("12" + strDeleteModbusOrder);
							printInformation(1, "��λ���ͻ��ˣ�Server������֪ͨmodbus�ն�ɾ�������"+strDeleteModbusOrder);
							// 3.������Ϣ��modbus�ն�
							sendMsg(modbusClientList.get(i).buffOutputStream, buffsend, buffsend.length);
						}
						/* -------------------------------------------------------------------- */
						
						// 3.����Ϣ����0x0F���ظ���λ��������Ϣ�����������͵����������
						byte[] buff10 = new byte[] { 0x10 };
						sendMsg(buffOutputStream, buff10, 1);//---------------------------------------------------------------------write

						break;
						
					default:
						break;
					}
				} catch (IOException e) { // ����ͻ��˽��̽������쳣���ر�������̣����Ҵ�list���������
					printInformation(-1, "��λ���ͻ��ˣ����棬�����ӳ������⣬�쳣�����ر��׽������������...");
					hostConnectionStarted = false;
					break;
				}
				printInformation(1, "��λ���ͻ��ˣ��������Ϣ������");
			}
			/*
			 * tainfo.append("��λ���ͻ��ˣ������쳣�����߳̽��Զ�������"+"\n"); tainfo.selectAll();
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
			// ���Լ��� list ��ɾ��
			hostClientList.remove(hostClientList.indexOf(this));
			printInformation(-1, "��λ���ͻ��ˣ�hostClientList�ĳ���Ϊ��" + hostClientList.size());

			printInformation(1, "��λ���ͻ��ˣ����̳߳����쳣�����߳̽��Զ�������");
		}
	}

	/**modbus�ն˿ͻ��� �����߳�------------------------------------------------------------------------------------------- */
	/** ----------------------------------------------------------------------------------------------------------------- */
	class ModbusClient implements Runnable {

		private Socket socket; // �׽���
		private InetAddress addressIp; // �ͻ��˵�IP
		private int portNum; // �ͻ�������Ķ˿ں�
		boolean modbusConnectionStarted;
		//boolean isReadyToSendMsgToModbusTerminal = true; // �����Ƿ���Ը�modbus�ն˷�����Ϣ
		private BufferedInputStream buffInputStream;
		private BufferedOutputStream buffOutputStream;

		@Override
		public String toString() {
			return "modbus�ն˿ͻ��ˣ�ModbusClient [addressIp=" + addressIp + ", portNum=" + portNum + "]";
		}

		// ���췽��
		public ModbusClient(Socket s, InputStream inPutStream, OutputStream outPutStream) throws IOException {
			this.socket = s;
			this.portNum = this.socket.getPort();
			this.addressIp = this.socket.getInetAddress();
			this.buffInputStream = new BufferedInputStream(inPutStream);
			this.buffOutputStream = new BufferedOutputStream(outPutStream);

			printInformation(1, this.toString());

			/** --- ��modbus�ն����ߵ���Ϣ��ӵ����ݿ��� */
			// -------------------------------------------------������λ�豸��������Ϣ��server���ٴη���һ��ѯ�ʵ���Ϣ
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// �������ڸ�ʽ
			String[] strValuesOfMonitorData = new String[4];
			strValuesOfMonitorData[0] = df.format(new Date()).toString();
			strValuesOfMonitorData[1] = this.addressIp.toString(); // ��modbus�ն˵�IP����
			strValuesOfMonitorData[2] = "FF";
			strValuesOfMonitorData[3] = "on";
			sqlitecrud.insert("CurrentState", strValuesOfMonitorData);
			sqlitecrud.insert("State", strValuesOfMonitorData);
			updateEquipmentStateToHost(
					strValuesOfMonitorData[0] 
					+ ByteUtil.intToString(strValuesOfMonitorData[1].length()) 
					+ strValuesOfMonitorData[1] 
					+ strValuesOfMonitorData[2] 
					+ strValuesOfMonitorData[3]); // ���á�����λ�������豸״̬�ķ����������豸״̬��Ϣ��
		}

		@Override
		/**
		 * ��Ҫ����modbus�ն˷��ص���Ϣ�����ݷ�����Ϣ�Ĳ�ͬ���ͣ����в�ͬ�Ĵ���
		 * @author CongXiang
		 * ��Ϣ���ͣ�
		 * 	1.�����ļ��Ѿ��յ���ȷ����Ϣ
		 * 	2.״̬��Ϣ
		 * 	3.������Ϣ
		 * 	4.�Ǳ���Ϣ
		 * 	5.�澯��Ϣ
		 * ע�⣺
		 * 	1.����"��ʶ��"��������������ֹ���̵߳ķ�����ֹmodbus�ն��߳�
		 * 	2.Ҳ�ǲ�����ѭ�������Ͻ�����λ�豸������������
		 * 	3.Ҫ���ݲ�ͬ����Ϣ���Ͳ��ò�ͬ�Ľ������ݵķ�ʽ����Ϊ���ݳ��̲�һ�����һ����ı��ļ�
		 *  4.��Ϣ���ͺ���Ϣ��һ���͹����������ĺô���
		 * */
		public void run() {
			
			/**
			 * �����������ݸ�modbus�ն˵��̣߳����ڷ���modbus�����modbus�ն�
			 * */
			SendModbusMsgToTreminalThread modbusThread = new SendModbusMsgToTreminalThread(buffOutputStream);
			modbusThread.start();
			printInformation(1, "modbus�ն˿ͻ��ˣ���ʼ����modbus�ն˿ͻ����߳�,���߳�ִ��һ�κ�������");

			modbusConnectionStarted = true;
			while (modbusConnectionStarted) {
				printInformation(1, "\n" + "modbus�ն˿ͻ��ˣ�����ѭ������ʼ������Ϣ...");
				try {
					byte[] buffRecv = new byte[64];// ���ջ�������
					/*
					int numRecv = this.buffInputStream.read(buffRecv);// ---------------------------------------------------------------------read
					*/
					int numRecv = recvMsg(this.buffInputStream, buffRecv);
					if (numRecv < 0) {
						printInformation(-1, "modbus�ն˿ͻ��ˣ����棬������Ϣ����");

						// ��state���в���һ�����ߵ���Ϣ
						SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// �������ڸ�ʽ
						String[] strValuesOfMonitorData = new String[4];
						strValuesOfMonitorData[0] = df.format(new Date()).toString();
						strValuesOfMonitorData[1] = this.addressIp.toString(); // ��modbus�ն˵�IP����
						strValuesOfMonitorData[2] = "FF";
						strValuesOfMonitorData[3] = "off";
						sqlitecrud.insert("State", strValuesOfMonitorData);
						// ��currentState���е�����ɾ��
						sqlitecrud.delete("CurrentState", "terminalName", this.addressIp.toString());
						modbusConnectionStarted = false;
						
						updateEquipmentStateToHost(
								strValuesOfMonitorData[0] 
								+ ByteUtil.intToString(strValuesOfMonitorData[1].length()) 
								+ strValuesOfMonitorData[1] 
								+ strValuesOfMonitorData[2] 
								+ strValuesOfMonitorData[3]); // ���á�����λ�������豸״̬�ķ����������豸״̬��Ϣ��
						
						break;
					}

					printInformation(1, "modbus�ն˿ͻ��ˣ����յ��ַ�������Ϊ��" + numRecv + ";���յ��ַ���Ϊ��" + ByteUtil.bytesToHexString(buffRecv));

					// ���ｫ������Ϣ���������в�ͬ�����ݴ�����Ϣʵ���Ѿ�����ڻ�������
					switch (buffRecv[0]) {
					
					case 0x02:
						printInformation(1, "modbus�ն˿ͻ��ˣ���Ϣ���ͣ�0x02����λ�豸���ߣ�");

						/* ������λ�豸����  */ 
						String strDeviceOn = ByteUtil.bytesToHexString(buffRecv).substring(2, 4); // ��λ�豸ID

						/**
						 * �豸���ߣ�
						 * 1.��ѯ����ǰ״̬����
						 * 2.����豸֮ǰ�Ѿ����ߣ����ô���
						 * 3.����豸֮ǰ�����ߣ����ڡ���ǰ״̬���͡���ʷ״̬���и����һ��������Ϣ
						 * */
						Vector<Vector<Object>> vectortCurrentStateOn = sqlitecrud.selectVectorByTwoKeyValue("CurrentState", "terminalName", this.addressIp.toString(), "deviceName", strDeviceOn);
						printInformation(1, "modbus�ն˿ͻ��ˣ���λ�豸���ߣ�modbus�ն˵�ַ��"+this.addressIp.toString() +", �豸��Ϊ��"+ strDeviceOn );
						
						if(vectortCurrentStateOn.isEmpty()){ // �豸������
							printInformation(1, "modbus�ն˿ͻ��ˣ��豸�������ߣ�֮ǰ�����ߣ���Ҫ����" );
							// �� �������и�����һ����Ϣ
							SimpleDateFormat dfOn = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// �������ڸ�ʽ
							String[] strDeviceOnArray = new String[4];
							strDeviceOnArray[0] = dfOn.format(new Date()).toString();
							strDeviceOnArray[1] = this.addressIp.toString(); // ��modbus�ն˵�IP����
							strDeviceOnArray[2] = strDeviceOn;
							strDeviceOnArray[3] = "on";
							sqlitecrud.insert("CurrentState", strDeviceOnArray);
							sqlitecrud.insert("State", strDeviceOnArray);
							
							updateEquipmentStateToHost(
									strDeviceOnArray[0] 
									+ ByteUtil.intToString(strDeviceOnArray[1].length()) 
									+ strDeviceOnArray[1] 
									+ strDeviceOnArray[2] 
									+ strDeviceOnArray[3]); // ���á�����λ�������豸״̬�ķ����������豸״̬��Ϣ��
							
						}else{ // �豸����
							printInformation(1, "modbus�ն˿ͻ��ˣ��豸�������ߣ�֮ǰҲ���ߣ�������" );
							// do nothing
						}
						
						break;
						
					case 0x03:
						printInformation(1, "modbus�ն˿ͻ��ˣ���Ϣ���ͣ�0x03����λ�豸���ߣ�");

						/* ������λ�豸���� */
						String strDeviceOff = ByteUtil.bytesToHexString(buffRecv).substring(2,4);
						/**
						 * �豸���ߣ�
						 * 1.��ѯ����ǰ״̬����
						 * 2.����豸֮ǰ�Ѿ����ߣ����ô���
						 * 3.����豸֮ǰ���ߣ�����ɾ������ǰ״̬���е���ؼ�¼��Ȼ���ڡ���ʷ״̬�������һ��������Ϣ
						 * */
						Vector<Vector<Object>> vectortCurrentState = sqlitecrud.selectVectorByTwoKeyValue("CurrentState", "terminalName", this.addressIp.toString(), "deviceName", strDeviceOff);
						printInformation(1, "modbus�ն˿ͻ��ˣ���λ�豸���ߣ�modbus�ն˵�ַ��"+this.addressIp.toString() +", �豸��Ϊ��"+ strDeviceOff );
						
						if(vectortCurrentState.isEmpty() == false){ // �豸����
							printInformation(1, "modbus�ն˿ͻ��ˣ��豸���β����ߣ�֮ǰ���ߣ���Ҫ����" );
							
							// ��currentState���е�����ɾ��
							sqlitecrud.deleteByTwoKeyValue("CurrentState", "terminalName", this.addressIp.toString(), "deviceName", strDeviceOff);
							
							// ����ʷ��¼�������һ�����ߵ���Ϣ
							SimpleDateFormat dfOn = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// �������ڸ�ʽ
							String[] strDeviceOffArray = new String[4];
							strDeviceOffArray[0] = dfOn.format(new Date()).toString();
							strDeviceOffArray[1] = this.addressIp.toString(); // ��modbus�ն˵�IP����
							strDeviceOffArray[2] = strDeviceOff;
							strDeviceOffArray[3] = "off";
							sqlitecrud.insert("State", strDeviceOffArray);
							
							updateEquipmentStateToHost(
									strDeviceOffArray[0] 
									+ ByteUtil.intToString(strDeviceOffArray[1].length()) 
									+ strDeviceOffArray[1] 
									+ strDeviceOffArray[2] 
									+ strDeviceOffArray[3]); // ���á�����λ�������豸״̬�ķ����������豸״̬��Ϣ��
							
						}else{ // �豸������
							printInformation(1, "modbus�ն˿ͻ��ˣ��豸���β����ߣ�֮ǰҲ�����ߣ�������" );
							// do nothing
						}
						

						break;
						
					case 0x04:
						printInformation(1, "modbus�ն˿ͻ��ˣ���Ϣ���ͣ�0x04���򿪴��ڳɹ���");
						/*
						this.buffOutputStream.write(buffRecv);// ---------------------------------------------------------------------write
						this.buffOutputStream.flush();
						*/
						sendMsg(this.buffOutputStream, buffRecv,1);
						break;
						
					case 0x05:
						printInformation(1, "modbus�ն˿ͻ��ˣ���Ϣ���ͣ�0x05���򿪴���ʧ�ܣ�");
						/*
						this.buffOutputStream.write(buffRecv);// ---------------------------------------------------------------------write
						this.buffOutputStream.flush();
						*/
						sendMsg(this.buffOutputStream, buffRecv,1);
						break;
						
					case 0x07:
						printInformation(1, "modbus�ն˿ͻ��ˣ���Ϣ���ͣ�0x07���յ�Server���͵�modbus���modbus�ն˸�����");
						break;
						
					case 0x09:
						printInformation(1, "modbus�ն˿ͻ��ˣ���Ϣ���ͣ�0x09���յ�Server�����͵�ϵͳʱ��");
						break;
						
					case 0x0A:
						printInformation(1, "modbus�ն˿ͻ��ˣ���Ϣ���ͣ�0x0A��modbus�ն˷��ͼ�����ݸ�server������");
						sendMsg(this.buffOutputStream, buffRecv,1);
						
						/**---������modbus�ն˷��͹�������Ϣ
						 * ��Ϣ���ɣ������� + ʱ�� + modbusdata
						 * ��Ϣ���ȣ�ʱ��(19���ֽ�)��modbusdata(ʣ��ȫ��)
						 * */
						// 1.��byte[]ת����string
						String strRecvMsg = new String(buffRecv,0,numRecv); // ֱ�ӽ��ֽ����鰴��ascll��ķ�ʽת����string����
						
						// 2.��ȡʱ��
						String strTime = strRecvMsg.substring(1, 20); // ��һλ�ǹ����룬�����19λ��ʱ��2016-04-06 12:12:12
						
						//01031400010000000000000000000100000000000000003F
						//0103140001000000000000000000010000000000000000930b
						
						// 3.��ȡmodbusdata��������Ҫת����string->byte[]->string(16���Ƹ�ʽ),!!!����numRecv-1����ΪҪ��ȥ�Լ�����Ĺ�����
						//String strModbusData = ByteUtil.bytesToHexString(strRecvMsg.substring(20, numRecv-1).getBytes()).toUpperCase();
						String strModbusData = ByteUtil.bytesToHexString(buffRecv).substring(20*2, numRecv*2).toUpperCase();
						printInformation(1, "modbus�ն˿ͻ��ˣ���Ϣ���ͣ��յ�modbus����0x0A��ʱ�䣺"+strTime+"�����ݣ�"+strModbusData);
						
						// 4.��������ݴ�Ž����ݿ�
						String[] strValuesOfMonitorData = new String[4];
						strValuesOfMonitorData[0] = strTime;
						strValuesOfMonitorData[1] = this.addressIp.toString(); // ��modbus�ն˵�IP����
						strValuesOfMonitorData[2] = strModbusData.substring(0, 2);
						strValuesOfMonitorData[3] = strModbusData.substring(2, strModbusData.length());
						sqlitecrud.insert("MonitorData", strValuesOfMonitorData);
						
						// 5.���������ʵʱ���͸���λ������ʽ�������� + ʱ�� + ��ַ���� + modbus�ն˵�IP��ַ + ��λ�豸ID + modbusdata
						String strModbusDataToHost = new String(new byte[] {0x0B}) ; // ��Ϣ������0x0B
						strModbusDataToHost = strModbusDataToHost + strValuesOfMonitorData[0];
						strModbusDataToHost = strModbusDataToHost + ByteUtil.intToString(strValuesOfMonitorData[1].length()); // IP��ַ�ĳ���
						strModbusDataToHost = strModbusDataToHost + strValuesOfMonitorData[1];
						strModbusDataToHost = strModbusDataToHost + strValuesOfMonitorData[2];
						strModbusDataToHost = strModbusDataToHost + strValuesOfMonitorData[3];
						
						printInformation(1, "modbus�ն˿ͻ��ˣ����͸���λ����ʵʱmodbusdata�������Ϊ��" + strModbusDataToHost);
						
						// 6.��modbusdataʵʱ������ݷ��͸���λ��
						byte[] byteModbusDataToHost = strModbusDataToHost.getBytes();
						for(int m = 0; m< hostClientList.size(); m++){
							sendMsg(hostClientList.get(m).buffOutputStream, byteModbusDataToHost, byteModbusDataToHost.length);// ---------------------------------------------------------------------write
						}
						break;
						
					case 0x11:
						printInformation(1, "��Ϣ���ͣ�0x11:Server�������������modbus����ɹ���");
						break;
						
					case 0x12:
						printInformation(1, "��Ϣ���ͣ�0x12:Server����������ɾ��modbus����ɹ���");
						break;
						
					default:
						break;
					}
				} catch (IOException e) {// ����ͻ��˽��̽������쳣���ر�������̣��������ݿ���д�����������Ϣ
					printInformation(-1, "modbus�ն˿ͻ��ˣ����棬�����ӳ������⣬�쳣�����ر��׽������������...");

					// ��state���в���һ�����ߵ���Ϣ
					SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// �������ڸ�ʽ
					printInformation(1, "modbus�ն˿ͻ��ˣ���ǰϵͳʱ��" + df.format(new Date()));// new
																						// Date()Ϊ��ȡ��ǰϵͳʱ��
					String[] strValuesOfMonitorData = new String[4];
					strValuesOfMonitorData[0] = df.format(new Date()).toString();
					strValuesOfMonitorData[1] = this.addressIp.toString(); // ��modbus�ն˵�IP����
					strValuesOfMonitorData[2] = "FF";
					strValuesOfMonitorData[3] = "off";
					sqlitecrud.insert("State", strValuesOfMonitorData);
					// ��currentState���е�����ɾ��
					sqlitecrud.delete("CurrentState", "terminalName", this.addressIp.toString());
					modbusConnectionStarted = false;
					
					try {
						updateEquipmentStateToHost(
								strValuesOfMonitorData[0] 
								+ ByteUtil.intToString(strValuesOfMonitorData[1].length()) 
								+ strValuesOfMonitorData[1] 
								+ strValuesOfMonitorData[2] 
								+ strValuesOfMonitorData[3]);// ���á�����λ�������豸״̬�ķ����������豸״̬��Ϣ��
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} 
					
					break;
				}
				printInformation(1, "modbus�ն˿ͻ��ˣ��������Ϣ����������");
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
			// ���Լ��� list ��ɾ��
			modbusClientList.remove(modbusClientList.indexOf(this));
			
			/**
			 * >>>���ڵ����⣺
			 * 1.������Ҫ����modbus�ն��µ�modbus�Ǳ��豸��״̬�����޸�
			 * */
			
			printInformation(-1, "modbus�ն˿ͻ��ˣ�modbusclient�ĳ���Ϊ:" + modbusClientList.size());

			printInformation(1, "modbus�ն˿ͻ��ˣ����̳߳����쳣�����߳̽��Զ�������");
		}
	}

	
	/**��modbus�ն˷���modbus����/ϵͳʱ�����Ϣ �����߳�-------------------------------------------------------------------- */
	/** ----------------------------------------------------------------------------------------------------------------- */
	/**
	 * @author CongXiang 
	 * ���ܣ�
	 * 1.��Ϣ���ͣ�0x09����ȡϵͳʱ��->���͸�modbus�ն�
	 * 2.��Ϣ���ͣ�0x07����ȡmodbusorder->���͸�modbus�ն�
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

				/* ��ȡϵͳʱ�䣬����ϵͳʱ�䷢�͸�modbus�ն� */
				// 1.��ȡϵͳʱ��
				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// �������ڸ�ʽ
				String strCurrentSystemTime = df.format(new Date()).toString();
				// 2.�����Ϣ0x09
				strCurrentSystemTime = new String(new byte[] { 0x09 }) + strCurrentSystemTime; // ǰ�������Ϣ����0x09
				byte[] buffTimeSend = strCurrentSystemTime.getBytes();
				// 3.����Ϣ���͸�modbus�ն�
				sendMsg(buff, buffTimeSend, buffTimeSend.length);

				// -------------------------

				/* ��ȡmodbusorder������modbusorder���͸�modbus�ն� */
				// 1.�ж������ն˲�Ϊ��
				printInformation(1, "//---�����߳�:����ѭ��[���ڽ�modbus��Ϣ���͸�modbus�ն�]...");
				int countModbusMsg = sqlitecrud.getTableCount("ModbusMsg");
				// 2.�ж����ݿ�����modbusorder
				if (countModbusMsg > 0) { // ���ݿ��������ݣ����Ǿ���Ҫ�����ݷ��͸�modbus�ն�,
					printInformation(0, "//---�����߳�:���ݿ���������");
					// 3.�����ݿ��л�ȡmodbusorder
					Object[][] objModbusMsg = sqlitecrud.selectObject("ModbusMsg");// �����ݿ��н�modbus��������ݿ���ȡ����
					// 4.�������е�modbusorder
					for (int i = 0; i < objModbusMsg.length; i++) {
						// 5.��װ0x07���͵���Ϣ
						byte[] buffsend = ByteUtil.hexStringToBytes("07" + objModbusMsg[i][0].toString());
						printInformation(1, "//---�����߳�:���ڷ�������" + objModbusMsg[i][0].toString());
						// 6.��0x07���͵���Ϣ���͸�modbus�ն�
						sendMsg(buff, buffsend, buffsend.length);
						Thread.sleep(1000);
					}
				}
			} catch (InterruptedException | IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**����λ������modbus���� �����߳�-------------------------------------------------------------------- */
	/** ----------------------------------------------------------------------------------------------------------------- */
	/**
	 * @author CongXiang 
	 * ���ܣ�
	 * 1.��Ϣ���ͣ�0x0E��
	 * 2.��Ϣ��ʽ��0x0E+modbus��������ݿ���ȡ�������η���
	 * */
	class SendModbusOrderToHostThread extends Thread {
		private BufferedOutputStream buff;
		
		public SendModbusOrderToHostThread(BufferedOutputStream buffOutputStream) {
			super();
			buff = buffOutputStream;
		}
		
		public void run() {
			try {
				// 1.�����ݿ��н�modbus����ȡ��
				int countModbusMsg = sqlitecrud.getTableCount("ModbusMsg");
				if (countModbusMsg > 0) { // ���ݿ���������
					Object[][] objModbusMsg = sqlitecrud.selectObject("ModbusMsg");// �����ݿ��н�modbus��������ݿ���ȡ����

					// 2.����һ��forѭ�������η���modbus����
					for (int i = 0; i < objModbusMsg.length; i++) {

						// 3.��װ0x0E���͵���Ϣ
						String strModbusOrder = new String(new byte[] {0x0E}) + objModbusMsg[i][0].toString(); 
						byte[] buffsend = strModbusOrder.getBytes();

						// 4.������Ϣ����λ��
						sendMsg(buff, buffsend, buffsend.length);// ---------------------------------------------------------------------write
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * ---����λ�������豸״̬��Ϣ
	 * @author CongXiang
	 * @throws IOException 
	 * 
	 * */
	public void updateEquipmentStateToHost(String strModbusSingleStateData) throws IOException{
		/** ��λ����server�����������豸״̬�� */
		/**
		 * 1.���ص��������� 1.��һλ����Ϣ���ͣ�08
		 * 2.�ڶ�λ��IP�ĳ��ȣ�xx,��ΪIP��ַ�ĳ����ǲ�һ������Ҫָ������
		 * 3.����λ��IP��ַ��xxxx(������ǰһ���ֶ�ָ��)
		 * 4.����λ����λ�豸�ţ�FF����û����λ�豸��xx(�����ֽ�)
		 * 5.����IP���豸���ں���˳�����ӣ�һ���ͣ�����λ����װ
		 * 
		 * ע�⣺IP��ַ��Ĳ���16����������string֮���ת������ʹ�ù�����
		 * */
		
		String strStateData = new String(new byte[] { 0x08 });; 

		int countModbusMsg = sqlitecrud.getTableCount("CurrentState");
		if (countModbusMsg > 0) { // �ж����ݿ����Ƿ�������

			// �����ݿ��н�modbus��������ݿ���ȡ����
			Object[][] objStateData = sqlitecrud.selectObject("CurrentState"); // ����״̬����

			// ��ȡ�������ݵ���������������ȷ��ѭ���Ĵ�����
			int numCount = sqlitecrud.getTableCount("CurrentState");

			String strTerminalName; // modbus�ն˵�IP��ַ
			String strDeviceName; // ��λ�豸�ı��
			String lengthOfTerminalName;

			for (int i = 0; i < numCount; i++) {
				/* �����ݿ��в��ҵ����ݲ���Ҫ��������λ������������ݵ���״��ʾ */

				strTerminalName = objStateData[i][1].toString(); // ��ʾmodbus�ն�IP�ֶ�
				strDeviceName = objStateData[i][2].toString(); // ��ʾ��λ�豸����ֶ�
				lengthOfTerminalName = ByteUtil.intToString(strTerminalName.length()); // IP�ĳ��ȣ�һ��С����λ������Ҫת��
				strStateData = strStateData + lengthOfTerminalName + strTerminalName + strDeviceName;
			}
		}
		printInformation(1, "�����������͸���λ����״̬����Ϊ��" + strStateData);

		// ��������
		byte[] byteStateData = strStateData.getBytes();
		for(int m = 0; m< hostClientList.size(); m++){
			sendMsg(hostClientList.get(m).buffOutputStream, byteStateData, byteStateData.length);// ---------------------------------------------------------------------write
		}
		
		/** ------------------------------------------------------------------------------------------------------------------ */
		
		/* ���͵���״̬���ݸ���λ�� */
		// 1.�����ݿ��л�ȡ���µ�һ����¼�������� + ʱ�� + ��ַ���� + modbus�ն˵�IP��ַ + ��λ�豸ID + �Ƿ�����״̬
/*		Object[] objModbusData = sqlitecrud.selectLatestObject("State","time"); // ��ʱ�併��
		
		String strModbusSingleStateData = new String(new byte[] {0x0C}) ; // ��Ϣ������0x0C
		if(objModbusData[0] != null){ // ����鵽����
			// 2.��װmodbusdata������ݣ�ԭ�򣺹�������16��������ʾ
			strModbusSingleStateData = strModbusSingleStateData + objModbusData[0].toString(); // ʱ��
			strModbusSingleStateData = strModbusSingleStateData + ByteUtil.intToString(objModbusData[1].toString().length()); // IP��ַ�ĳ���
			strModbusSingleStateData = strModbusSingleStateData + objModbusData[1]; // modbus�ն˵�IP��ַ
			strModbusSingleStateData = strModbusSingleStateData + objModbusData[2]; // ��λ�豸ID
			strModbusSingleStateData = strModbusSingleStateData + objModbusData[3]; // �Ƿ����ߵ�״̬
		}
*/
		printInformation(1, "�����������͸���λ����ʵʱ�豸״̬��ϢΪ��" + strModbusSingleStateData);
		
		// 3.������modbusdataʵʱ�豸״̬��Ϣ���͸���λ��
		byte[] byteModbusSingleStateData = (new String(new byte[] { 0x0C }) + strModbusSingleStateData).getBytes();
		for(int m = 0; m< hostClientList.size(); m++){
			sendMsg(hostClientList.get(m).buffOutputStream, byteModbusSingleStateData, byteModbusSingleStateData.length);// ---------------------------------------------------------------------write
		}
		
		
	}
	
	
	
	/**
	 * ---��дbuffOutputStream.write()������Ϊ��Ӧ��TCP���ӵ�ճ�����⡿
	 * @author CongXiang
	 * ������
	 * 1.BufferedOutputStream buffOutputStream; // �����
	 * 2.byte typeOfMsg; // ��Ϣ����,new String(new byte[] {0x09})
	 * 3.string contentOfMsg; // ���������
	 * 4.int lengthOfMsg; // �����ֽ���
	 * ����ֵ��
	 * 1:�ɹ���������
	 * 0:��������ʧ��
	 * @throws IOException 
	 * */
	public int sendMsg(BufferedOutputStream buffOutputStream, byte[] contentOfMsg, int lengthOfMsg) throws IOException {
		// //String type = new String(new byte[] { typeOfMsg }); //
		// ��byte��Ϣ����ת����string��Ϊ�˷��������string.getbytes();
		// //byte[] buffsend = (type + contentOfMsg).getBytes(); // ���ͻ�����
/*
		buffOutputStream.write(contentOfMsg);// ---------------------------------------------------------------------write
		buffOutputStream.flush();
		printInformation(1, "��������������Ϣ:���͵��������ͣ�" + contentOfMsg[0]);
*/
		
		// 1.���巢�ͻ��������������ĳ���Ϊ��lengthOfMsg + 1 + 1
		byte[] byteSendBuffArray = new byte[lengthOfMsg + 2];
		
		// 2.�����ݻ���������ӡ���Ϣ�峤�ȡ��ֶΣ�1���ֽڣ���16��������ʾ
		byteSendBuffArray[0] = (byte) lengthOfMsg; // ǿ������ת��
		
		// 3.�����ݻ���������ӡ���Ϣ�塱��lengthOfMsg���ֽ�
		for (int i = 0; i < lengthOfMsg; i++) {
			byteSendBuffArray[i+1] = contentOfMsg[i];
		}
		
		// 4.�����ݻ���������ӡ�ֹͣ���š���1���ֽ�
		byteSendBuffArray[lengthOfMsg+1] = (byte) 0xFF;
		
		// 5.���ͻ���������������
		buffOutputStream.write(byteSendBuffArray);// ---------------------------------------------------------------------write
		buffOutputStream.flush();
		printInformation(1, "��������������Ϣ:���͵���������:" + contentOfMsg[0]+",��Ϣ����Ϊ:"+(int)byteSendBuffArray[0]);
		
		return 1;
	}
	
	/**
	 * ---��дbufferInputStream.read()������Ϊ��Ӧ��TCP���ӵ�ճ�����⡿
	 * @author CongXiang
	 * ������
	 * 1.BufferedInputStream bufferInputStream ; // ������
	 * 2.byte[] recvBuff; // ���ջ�����
	 * 3.
	 * ����ֵ��
	 * recvNum; // ���յ��ֽ�����
	 * @throws IOException 
	 * */
	public int recvMsg(BufferedInputStream bufferInputStream, byte[] recvBuff) throws IOException {
/*		
		int num; // ��¼���յ��ֽ�����
		num = bufferInputStream.read(recvBuff);
		return num;
*/
		// 1.����һ���ַ�����Ϣ��ĳ���lengthOfMsg
		byte[] byteTemp = new byte[1]; // һ���ֽڼ���
		int lengthOfMsg;
		bufferInputStream.read(byteTemp, 0, 1);
		lengthOfMsg = (int) byteTemp[0];
		
		// 2.����lengthOfMsg���ַ�
		int recvNum = bufferInputStream.read(recvBuff, 0, lengthOfMsg);
		
		// 3.�ٶ���һ���ַ����ж��ǲ��ǡ�ֹͣ����0xFF��
		bufferInputStream.read(byteTemp, 0, 1);
		
		if((byteTemp[0] == (byte) 0xFF) && (recvNum == lengthOfMsg)){
			// 4.�����ֹͣ���ţ�����recvNum == lengthOfMsg ��˵��������ȷ
			return recvNum;
		}else{
			// 5.�������ֹͣ���ţ�����recvNum != lengthOfMsg����˵�����մ���>>>���մ�����:��ͣ�Ķ�����һ���ַ���֪������0xFFΪֹ
			while(bufferInputStream.read(byteTemp, 0, 1) == 1){
				if(byteTemp[0] == 0xFF){
					break;
				}
			}
			return -1; // ��ʶ�������ݳ���
		}

	}

	// server����ϵ���Ϣ��Ӧ�¼�
	@Override
	public void actionPerformed(ActionEvent e) {

		if (e.getSource() == serverPanel.btOpenServer) { /*--- �򿪷�����*/
			serverPanel.btOpenServer.setEnabled(false);
			serverPanel.btCloseServer.setEnabled(true);
			// ---------------------------------- �������߳�
			mainThread = new MainThread();
			mainThread.start();

		} else if (e.getSource() == serverPanel.btCloseServer) { /*--- �رշ�����*/
			serverPanel.btOpenServer.setEnabled(true);
			serverPanel.btCloseServer.setEnabled(false);
			/* �����ȷ�ر������߳��Լ��������� */

			try {
				// 1.�ر����̣߳�ͬʱ�ر��׽��֡����������
				mainThread.serverSocket.close();
				mainThread.interrupt();

				// 2.�ر�Modbus�ն˿ͻ����߳�
				for (int i = 0; i < modbusClientList.size(); i++) {
					modbusClientList.get(i).socket.close();
				}
				
				// 3.�ر���λ���ͻ����߳�
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
	 * ---��ӡ�����������״̬
	 * @author CongXiang 
	 * ˼·�����ݱ�ʶ���Ĳ�ͬ��ѡ���ǡ�����ֱ̨����������ǡ��ڳ�����������
	 * ������
	 * 1.int systemOrApplication; // ��Ϣ��Ӧ��ʽ
	 * 2.String strMsg; // ��Ϣ����
	 * */
	public void printInformation(int systemOrApplication, String strMsg) {
/*		
		if (systemOrApplication == 0) { // �����

		} else if (systemOrApplication == 1) { // ϵͳ���
			System.out.println(strMsg.trim());
		} else if (systemOrApplication == 2) { // ����������
			tainfo.append(strMsg + "\n");
			tainfo.selectAll();
		} else if (systemOrApplication == -1) { // ������Ϣ���
			System.err.println(strMsg);
		} else {
			System.out.println("�����ʽ�������������ʽ��-1�ǳ�����Ϣ�����0��ϵͳ�����1�ǽ������");
		}
*/
		switch(systemOrApplication){
		case 0: // �����
			break;
			
		case 1:// ϵͳ���
			System.out.println(strMsg.trim());
			break;
			
		case 2:// ����������
			tainfo.append(strMsg + "\n");
			tainfo.selectAll();
			break;
			
		case -1:// ������Ϣ���
			System.err.println(strMsg);
			break;
		default:
			System.out.println("�����ʽ�������������ʽ��-1�ǳ�����Ϣ�����1��ϵͳ�����2�ǽ������");
			break;
		}
	}

	// 222
	// ����������
	public static void main(String[] args) throws Exception {
		new Server();
	}



}


