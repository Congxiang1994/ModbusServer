package com.congxiang.ui;

import java.awt.GridBagLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

public class ServerPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	//public JLabel laIpAddress = new JLabel("IP��ַ:", SwingConstants.RIGHT);
	public JLabel laPort = new JLabel("�˿ں�:", SwingConstants.RIGHT);

	//public JTextField tfIPAddress = new JTextField();
	public JTextField tfPort = new JTextField();

	public JButton btOpenServer = new JButton("����������");
	public JButton btCloseServer = new JButton("�رշ�����");
	
	public JTextArea tainfo = new JTextArea(); // ���ı���
	public JScrollPane jsp = new JScrollPane(tainfo); // ��������

	// ���췽��
	public ServerPanel() {
		super();

		// ���ñ߿�
		Border etched = BorderFactory.createEtchedBorder();
		Border border = BorderFactory.createTitledBorder(etched, "����������:");
		this.setBorder(border);

		GridBagLayout gridbaglayout = new GridBagLayout();
		this.setLayout(gridbaglayout);

		// 1,1,1,0,0
		// ��ǩ���˿ں�
		gridbaglayout.setConstraints(laPort, new GBC(0,0,1,1).setWeight(0, 0).setFill(GBC.BOTH).setInsets(5, 5, 5, 5));
		this.add(laPort);

		// �ı��򣺶˿ں�
		gridbaglayout.setConstraints(tfPort, new GBC(1,0,1,1).setWeight(1, 0).setFill(GBC.BOTH).setInsets(5, 5, 5, 5));
		this.add(tfPort);

		// ��ť���򿪷�����
		gridbaglayout.setConstraints(btOpenServer, new GBC(2,0,1,1).setWeight(1, 0).setFill(GBC.BOTH).setInsets(5, 5, 5, 5));
		this.add(btOpenServer);

		// ��ť���رշ�����
		gridbaglayout.setConstraints(btCloseServer, new GBC(3,0,1,1).setWeight(1, 0).setFill(GBC.BOTH).setInsets(5, 5, 5, 5));
		this.add(btCloseServer);
		
		// �ı���
		gridbaglayout.setConstraints(jsp, new GBC(0,1,4,6).setWeight(1, 1).setFill(GBC.BOTH).setInsets(5, 5, 5, 5));
		this.add(jsp);
		
		// ���ð�ť�ĳ�ʼ��״̬
		btOpenServer.setEnabled(true);
		btCloseServer.setEnabled(false);
		tfPort.setText("9000");

	}

	public static void main(String[] args) {

		ServerPanel Panel = new ServerPanel();
		JFrame frame = new JFrame();
		frame.add(Panel);

		frame.pack();
		frame.setVisible(true);

		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});

	}

}
