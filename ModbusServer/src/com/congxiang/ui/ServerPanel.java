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

	//public JLabel laIpAddress = new JLabel("IP地址:", SwingConstants.RIGHT);
	public JLabel laPort = new JLabel("端口号:", SwingConstants.RIGHT);

	//public JTextField tfIPAddress = new JTextField();
	public JTextField tfPort = new JTextField();

	public JButton btOpenServer = new JButton("开启服务器");
	public JButton btCloseServer = new JButton("关闭服务器");
	
	public JTextArea tainfo = new JTextArea(); // 　文本框
	public JScrollPane jsp = new JScrollPane(tainfo); // 　滚动栏

	// 构造方法
	public ServerPanel() {
		super();

		// 设置边框
		Border etched = BorderFactory.createEtchedBorder();
		Border border = BorderFactory.createTitledBorder(etched, "服务器设置:");
		this.setBorder(border);

		GridBagLayout gridbaglayout = new GridBagLayout();
		this.setLayout(gridbaglayout);

		// 1,1,1,0,0
		// 标签：端口号
		gridbaglayout.setConstraints(laPort, new GBC(0,0,1,1).setWeight(0, 0).setFill(GBC.BOTH).setInsets(5, 5, 5, 5));
		this.add(laPort);

		// 文本框：端口号
		gridbaglayout.setConstraints(tfPort, new GBC(1,0,1,1).setWeight(1, 0).setFill(GBC.BOTH).setInsets(5, 5, 5, 5));
		this.add(tfPort);

		// 按钮：打开服务器
		gridbaglayout.setConstraints(btOpenServer, new GBC(2,0,1,1).setWeight(1, 0).setFill(GBC.BOTH).setInsets(5, 5, 5, 5));
		this.add(btOpenServer);

		// 按钮：关闭服务器
		gridbaglayout.setConstraints(btCloseServer, new GBC(3,0,1,1).setWeight(1, 0).setFill(GBC.BOTH).setInsets(5, 5, 5, 5));
		this.add(btCloseServer);
		
		// 文本框：
		gridbaglayout.setConstraints(jsp, new GBC(0,1,4,6).setWeight(1, 1).setFill(GBC.BOTH).setInsets(5, 5, 5, 5));
		this.add(jsp);
		
		// 设置按钮的初始化状态
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
