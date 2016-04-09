package com.congxiang.modbus.dao;
import java.sql.Connection;  
import java.sql.ResultSet;  
import java.sql.SQLException;  
import java.sql.Statement;  
import java.util.ArrayList;  
import java.util.List;  
import java.util.Vector; 
public class SQLiteCRUD {  
	  
    private Connection connection ;  
    public SQLiteCRUD(Connection connection) {  
        this.connection = connection ;  
    }  

    /** 
     * 创建表。 
     * @param sql 
     * @return boolean 
     */  
    public boolean createTable(String sql){  
        //System.out.println(sql);  
        Statement stmt = null ;  
        try{  
            stmt = this.connection.createStatement() ;  
            stmt.executeUpdate(sql) ;  
            return true ;  
        }catch (Exception e) {  
            System.out.println("创建指定表时异常 : " + e.getLocalizedMessage());  
            connectionRollback(connection) ;  
            return false ;  
        }  
    }  
      
    /** 
     * 向指定表中插入一条数据。 
     * @param table 表名 
     * @param params 参数数组 
     * @return boolean 
     */  
    public boolean insert(String table, String[] params){  
        Statement stmt = null ;  
        String sql = "insert into " + table  + " values('";  
        for(int i = 0 ; i < params.length ;i++){  
            if(i == (params.length - 1)){  
                sql += (params[i] + "');") ;  
            }else{  
                sql += (params[i] + "', '") ;  
            }  
        }  
        //System.out.println(sql);  
        try{  
            stmt = this.connection.createStatement() ;  
            stmt.executeUpdate(sql) ;  
            return true ;  
        }catch (Exception e) {  
            System.out.println("向表插入" + table + "数据时异常 : " + e.getLocalizedMessage());  
            connectionRollback(connection) ;  
            return false ;  
        }  
    }  
      
    /** 
     * 修改表中一个元组的数据。 
     * @param table 表名 
     * @param keyParam 要修改的元组的主键值 
     * @param keyField 要修改的元组的主键字段名称 
     * @param fields 要修改的元组的字段名称数组 
     * @param params 要修改的元组的值数组 
     * @return boolean 
     */  
    public boolean update(String table, String keyField, String keyParam, String[] fields, String[] params){  
        Statement stmt = null ;  
        String sql = "update " + table + " set " ;  
        for(int i = 0 ; i < fields.length ; i++){  
            if(i == (fields.length - 1)){  
                sql += (fields[i] + "='" + params[i] + "' where " + keyField + "='" + keyParam +"';") ;  
            }else{  
                sql += (fields[i] + "='" + params[i] + "', ") ;  
            }  
        }  
       //System.out.println(sql);  
        try{  
            stmt = this.connection.createStatement() ;  
            stmt.executeUpdate(sql) ;  
            return true ;  
        }catch (Exception e) {  
            System.out.println("修改表" + table + "数据时异常 : " + e.getLocalizedMessage());  
            connectionRollback(connection) ;  
            return false ;  
        }  
          
    }  
      
    /** 
     * 删除指定表中指定键值的元组。 
     * @param table 
     * @param key 
     * @param keyValue 
     * @return boolean 
     */  
    public boolean delete(String table, String key, String keyValue){  
        Statement stmt = null ;  
        String sql = "delete from " + table + " where " + key + "='" + keyValue + "';" ;  
        //System.out.println(sql);  
        try{  
            stmt = this.connection.createStatement() ;  
            stmt.executeUpdate(sql) ;  
            return true ;  
        }catch (Exception e) {  
            System.out.println("删除表" + table + "数据时异常 : " + e.getLocalizedMessage());  
            connectionRollback(connection) ;  
            return false ;  
        }  
    }  
    
    /** 
     * 删除指定表中指定键值的元组。 
     * @param table 
     * @param key 
     * @param keyValue 
     * @return boolean 
     */  
    public boolean deleteByTwoKeyValue(String table, String key, String keyValue,String keyTwo, String keyValueTwo){  
        Statement stmt = null ;  
		String sql = "delete from " + table + " where " + key + " = '" + keyValue + "' and " + keyTwo + " = '" + keyValueTwo + "' ;";
       // String sql = "delete from " + table + " where " + key + "='" + keyValue + "';" ;  
        //System.out.println(sql);  
        try{  
            stmt = this.connection.createStatement() ;  
            stmt.executeUpdate(sql) ;  
            return true ;  
        }catch (Exception e) {  
            System.out.println("删除表" + table + "数据时异常 : " + e.getLocalizedMessage());  
            connectionRollback(connection) ;  
            return false ;  
        }  
    } 
      
    /** 
     * 将一个表中满足指定条件的所有元组以Vector<Vector<Object>>的形式返回 ，这里查询条件只有一个
     * @param table 
     * @param key 
     * @param keyValue 
     * @return Vector<Vector<Object>> 
     */  
    public Vector<Vector<Object>> selectVector(String table, String key, String keyValue){  
        Statement stmt = null ;  
        ResultSet rs = null ;  
          
        Vector<Vector<Object>> value = new Vector<Vector<Object>>() ;  
        String sql = "select * from " + table + " where " + key + "='" + keyValue + "';" ;  
        //System.out.println(sql);  
        try{  
            stmt = this.connection.createStatement() ;  
            rs = stmt.executeQuery(sql) ;  
            int columnCounts = getFieldsCounts(rs) ;  
            while(rs.next()){  
                Vector<Object> valueVector = new Vector<Object>() ;  
                for(int i = 1; i <= columnCounts ; i++){  
                    valueVector.addElement(rs.getObject(i)) ;  
                }  
                value.addElement(valueVector) ;  
            }  
            rs.close();
            return value ;  
        }catch (Exception e) {  
            System.out.println("查询表" + table + "数据时异常 : " + e.getLocalizedMessage());  
            return value ;  
        }  
    }  
    
    /** 
     * 将一个表中满足指定条件的所有元组以Vector<Vector<Object>>的形式返回 ，这里查询条件只有一个
     * @param table 
     * @param key 
     * @param keyValue 
     * @return Vector<Vector<Object>> 
     */  
    public Vector<Vector<Object>> selectVectorByTwoKeyValue(String table, String key, String keyValue,String keyTwo, String keyValueTwo){  
        Statement stmt = null ;  
        ResultSet rs = null ;  
          
        Vector<Vector<Object>> value = new Vector<Vector<Object>>() ;  
		String sql = "select * from " + table + " where " + key + " = '" + keyValue + "' and " + keyTwo + " = '" + keyValueTwo + "' ;";
        //System.out.println(sql);  
        try{  
            stmt = this.connection.createStatement() ;  
            rs = stmt.executeQuery(sql) ;  
            int columnCounts = getFieldsCounts(rs) ;  
            while(rs.next()){  
                Vector<Object> valueVector = new Vector<Object>() ;  
                for(int i = 1; i <= columnCounts ; i++){  
                    valueVector.addElement(rs.getObject(i)) ;  
                }  
                value.addElement(valueVector) ;  
            }  
            rs.close();
            return value ;  
        }catch (Exception e) {  
            System.out.println("查询表" + table + "数据时异常 : " + e.getLocalizedMessage());  
            return value ;  
        }  
    }  
    
      
    /** 
     * 返回制定sql语句查询的Vector<Vector<Object>>结果集 
     * @param sql sql语句 
     * @return Vector<Vector<Object>> 
     */  
    public Vector<Vector<Object>> selectVector(String sql){  
        Statement stmt = null ;  
        ResultSet rs = null ;  
          
        Vector<Vector<Object>> value = new Vector<Vector<Object>>() ;  
          
        //System.out.println(sql);  
        try{  
            stmt = this.connection.createStatement() ;  
            rs = stmt.executeQuery(sql) ;  
            int columnCounts = getFieldsCounts(rs) ;  
            while(rs.next()){  
                Vector<Object> valueVector = new Vector<Object>() ;  
                for(int i = 1; i <= columnCounts ; i++){  
                    valueVector.addElement(rs.getObject(i)) ;  
                }  
                value.addElement(valueVector) ;  
            }  
            rs.close();
            return value ;  
        }catch (Exception e) {  
            System.out.println("查询表sql数据时异常 : " + e.getLocalizedMessage());  
            return value ;  
        }  
    }  
      
    /** 
     * 将满足一定条件的指定表中所有元组数据以Object[][]形式返回 
     * @param table 
     * @param key 
     * @param keyValue 
     * @return Object[][] 
     */  
    public Object[][] selectObject(String table, String key, String keyValue){  
        Statement stmt = null ;  
        ResultSet rs = null ;  
          
        int columns = getFieldsCounts(table) ;  
        int rows = getTableCount(table, key, keyValue) ;  
          
        Object[][] tableObject = new Object[rows][columns] ;  
        String sql = "select * from " + table + " where " + key + "='" + keyValue + "';" ;  
        //System.out.println(sql);  
        try{  
            stmt = this.connection.createStatement() ;  
            rs = stmt.executeQuery(sql) ;  
            int i = 0 ;  
            while(rs.next()){  
                for(int j = 0 ; j < columns ; j++){  
                    tableObject[i][j] = rs.getObject(j+1) ;  
                }  
                i++ ;  
            }  
            rs.close();
            return tableObject ;  
        }catch (Exception e) {  
            System.out.println("查询表" + table + "数据时异常 : " + e.getLocalizedMessage());  
            return tableObject ;  
        }  
    }  
      
    /** 
     * 将一个表中所有的元组以Vector<Vector<Object>>的形式返回 
     * @param table 
     * @param key 
     * @param keyValue 
     * @return Vector<Vector<Object>> 
     */  
    public Vector<Vector<Object>> select(String table){  
        Statement stmt = null ;  
        ResultSet rs = null ;  
          
        Vector<Vector<Object>> value = new Vector<Vector<Object>>() ;  
          
        String sql = "select * from " + table + ";" ;  
        //System.out.println(sql);  
        try{  
            stmt = this.connection.createStatement() ;  
            rs = stmt.executeQuery(sql) ;  
            int columnCounts = getFieldsCounts(rs) ;  
            while(rs.next()){  
                Vector<Object> valueVector = new Vector<Object>() ;  
                for(int i = 1; i <= columnCounts ; i++){  
                    valueVector.addElement(rs.getObject(i)) ;  
                }  
                value.addElement(valueVector) ;  
            }  
            rs.close();
            return value ;  
        }catch (Exception e) {  
            System.out.println("查询表" + table + "数据时异常 : " + e.getLocalizedMessage());  
            return value ;  
        }  
    }  

    /** 
     * 将一个表中所有的元组以Object[][]的形式返回 
     * @param table 
     * @return Object[][] 
     */  
    public Object[][] selectObject(String table){  
        Statement stmt = null ;  
        ResultSet rs = null ;  
          
        int columns = getFieldsCounts(table) ;  // 列
        int rows = getTableCount(table) ;  // 行
          
        Object[][] tableObject = new Object[rows][columns] ;  
          
        String sql = "select * from " + table + ";" ;  
        //System.out.println(sql);  
        try{  
            stmt = this.connection.createStatement() ;  
            rs = stmt.executeQuery(sql) ;  
            int i = 0 ;  
            while(rs.next()){  
                for(int j = 0 ; j < columns ; j++){  
                    tableObject[i][j] = rs.getObject(j+1) ;  
                }  
                i++ ;  
            }  
            rs.close();
            return tableObject ;  
        }catch (Exception e) {  
            System.out.println("查询表" + table + "数据时异常 : " + e.getLocalizedMessage());  
            return tableObject ;  
        }  
    }  
    
    /**
     * 将一个表中最新的一条数据以object[]的形式返回
     * @param table
     * @return Object[]
     * */
    
	public Object[] selectLatestObject(String table, String sortCondition) {
		Statement stmt = null;
		ResultSet rs = null;

		int columns = getFieldsCounts(table); // 列

		Object[] tableObject = new Object[columns];

		String sql = "select * from " + table + " order by "+ sortCondition +" DESC ;";
		// System.out.println(sql);
		try {
			stmt = this.connection.createStatement();
			rs = stmt.executeQuery(sql);
			if (rs.next()) {
				for (int j = 0; j < columns; j++) {
					tableObject[j] = rs.getObject(j + 1);
				}
			}
			rs.close();
			return tableObject;
		} catch (Exception e) {
			System.out.println("查询表" + table + "数据时异常 : " + e.getLocalizedMessage());
			return tableObject;
		}
	}
      
    /** 
     * 将一个ResultSet结果集中的所有字段以List形式返回 
     * @param resultSet 
     * @return List<String> 
     */  
    public List<String> getFields(ResultSet resultSet){  
        List<String> fieldsList = new ArrayList<String>() ;  
        try {  
            int columnCounts = resultSet.getMetaData().getColumnCount();  
            for(int i = 1 ; i <= columnCounts ; i++){  
                fieldsList.add(resultSet.getMetaData().getColumnName(i)) ;  
            }  
        } catch (SQLException e) {  
            System.out.println("加载表中字段异常 ：" + e.getLocalizedMessage());  
            return null ;  
        }  
        return fieldsList ;  
    }  
      
    /** 
     * 将一个表中的所有字段以List形式返回 
     * @param resultSet 
     * @return List<String> 
     */  
    public List<String> getFields(String table){  
        List<String> fieldsList = new ArrayList<String>() ;  
        Statement stmt = null ;  
        ResultSet rs = null ;  
        String sql = "select * from " + table + ";" ;  
        //System.out.println(sql);  
        try{  
            stmt = this.connection.createStatement() ;  
            rs = stmt.executeQuery(sql) ;  
            fieldsList = getFields(rs) ;  
            rs.close();
        }catch (Exception e) {  
            System.out.println("查询表" + table + "数据时异常 : " + e.getLocalizedMessage());  
        }  
        
        return fieldsList ;  
    }  
      
    /** 
     * 将一个ResultSet结果集中的所有字段的数目返回 
     * @param resultSet 
     * @return int 
     */   
    public int getFieldsCounts(ResultSet resultSet){  
        try {  
            return resultSet.getMetaData().getColumnCount();  
        } catch (SQLException e) {  
            System.out.println("加载表中字段异常 ：" + e.getLocalizedMessage());  
            return 0;  
        }  
    }  
      
    /** 
     * 返回一个表的所有字段数目 
     * @param table 
     * @return int 
     */  
    public int getFieldsCounts(String table){  
        int counts = 0 ;  
        Statement stmt = null ;  
        ResultSet rs = null ;  
        String sql = "select * from " + table + ";" ;  
        //System.out.println(sql);  
        try{  
            stmt = this.connection.createStatement() ;  
            rs = stmt.executeQuery(sql) ;  
            counts = getFieldsCounts(rs) ;  
            rs.close();
        }catch (Exception e) {  
            System.out.println("查询表" + table + "数据时异常 : " + e.getLocalizedMessage());  
        }  
        return counts ;  
    }  
      
    /** 
     * 查询一个表中的所有元组数目 
     * @param table 
     * @return int 
     */  
    public int getTableCount(String table){  
        String sql = "select count(*) from " + table + ";" ;  
        Statement stmt = null ;  
        ResultSet rs = null ;  
        int counts = 0 ;  
        try {  
            stmt = this.connection.createStatement() ;  
            rs = stmt.executeQuery(sql) ;  
            while(rs.next()){  
                counts = rs.getInt(1) ;  
            }  
            rs.close();
            return counts ;  
        } catch (Exception e) {  
            System.out.println("查询表" + table + "元组数时异常 : " + e.getLocalizedMessage());  
            return counts ;  
        }  
    }  
      
    /** 
     * 查询一个表中的满足一定条件的所有元组数目 
     * @param table 表名 
     * @param key 字段名称 
     * @param keyValue 字段值 
     * @return int 
     */  
    public int getTableCount(String table, String key, String keyValue){  
        String sql = "select count(*) from " + table + " where " + key + "='" + keyValue + "';";  
        Statement stmt = null ;  
        ResultSet rs = null ;  
        int counts = 0 ;  
        try {  
            stmt = this.connection.createStatement() ;  
            rs = stmt.executeQuery(sql) ;  
            while(rs.next()){  
                counts = rs.getInt(1) ;  
            }  
            rs.close();
            return counts ;  
        } catch (Exception e) {  
            System.out.println("查询表" + table + "元组数时异常 : " + e.getLocalizedMessage());  
            return counts ;  
        }  
    }  
      
    public void connectionRollback(Connection connection){  
        try {  
            connection.rollback() ;  
        } catch (SQLException e) {  
            System.out.println("异常时回滚错误 : " + e.getLocalizedMessage()) ;  
        }  
    }  
}  
