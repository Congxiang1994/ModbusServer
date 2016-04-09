package com.congxiang.modbus.dao;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;

public class SQLiteConn implements Serializable {  
    private static final long serialVersionUID = 102400L;  
  
    private String dataFile ;  
  
    public SQLiteConn(String dbfile) {  
        super();  
        this.dataFile = dbfile;  
    }  
      
    /** 
     * ��SQLiteǶ��ʽ���ݿ⽨������ 
     *	@author CongXiang
     */  
    public Connection getConnection() throws Exception {  
        Connection connection = null ;  
        try{  
            Class.forName("org.sqlite.JDBC", true, this.getClass().getClassLoader()) ;  
            connection = DriverManager.getConnection("jdbc:sqlite:" + dataFile);  
        }catch (Exception e) {  
            throw new Exception("" + e.getLocalizedMessage(), new Throwable("�����������ݿ��ļ��ܵ��Ƿ��޸Ļ�ɾ����")) ;  
        }  
        return connection ;  
    }  
}  