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
     * 与SQLite嵌入式数据库建立连接 
     *	@author CongXiang
     */  
    public Connection getConnection() throws Exception {  
        Connection connection = null ;  
        try{  
            Class.forName("org.sqlite.JDBC", true, this.getClass().getClassLoader()) ;  
            connection = DriverManager.getConnection("jdbc:sqlite:" + dataFile);  
        }catch (Exception e) {  
            throw new Exception("" + e.getLocalizedMessage(), new Throwable("可能由于数据库文件受到非法修改或删除。")) ;  
        }  
        return connection ;  
    }  
}  