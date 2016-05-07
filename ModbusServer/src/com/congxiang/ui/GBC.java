package com.congxiang.ui;
import java.awt.GridBagConstraints;
import java.awt.Insets;

public class GBC extends GridBagConstraints  
{  

	private static final long serialVersionUID = 1L;

	// ��ʼ��ռ���У�ÿ�е����һ���ؼ�������Ϊ0
	public GBC(int gridwidth) {
		// TODO Auto-generated constructor stub
	      this.gridwidth = gridwidth;  //ռ����
	}
	
	
   // ��ʼ�����Ͻ�λ��  
   public GBC(int gridx, int gridy)  
   {  
      this.gridx = gridx;  
      this.gridy = gridy;  
   }  
  
   // ��ʼ�����Ͻ�λ�ú���ռ����������  
   public GBC(int gridx, int gridy, int gridwidth, int gridheight)  
   {  
      this.gridx = gridx;  //�ڼ���
      this.gridy = gridy;  //�ڼ���
      this.gridwidth = gridwidth;  //ռ����
      this.gridheight = gridheight;  //ռ����
   }  
  
   // ���뷽ʽ  
   public GBC setAnchor(int anchor)  
   {  
      this.anchor = anchor;  
      return this;  
   }  
  
   // �Ƿ����켰���췽��  
   public GBC setFill(int fill)  
   {  
      this.fill = fill;  
      return this;  
   }  
  
   // x��y�����ϵ�����  
   public GBC setWeight(double weightx, double weighty)  
   {  
      this.weightx = weightx;  
      this.weighty = weighty;  
      return this;  
   }  
  
   // �ⲿ���  
   public GBC setInsets(int distance)  
   {  
      this.insets = new Insets(distance, distance, distance, distance);  
      return this;  
   }  
  
   // �����  
   public GBC setInsets(int top, int left, int bottom, int right)  
   {  
      this.insets = new Insets(top, left, bottom, right);  
      return this;  
   }  
  
   //�����  
   public GBC setIpad(int ipadx, int ipady)  
   {  
      this.ipadx = ipadx;  //x�����ϼ�ipadx
      this.ipady = ipady;  //y�����ϼ�ipady
      return this;  
   }  
   

}