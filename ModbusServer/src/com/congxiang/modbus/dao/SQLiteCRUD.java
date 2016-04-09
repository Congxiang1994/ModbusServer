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
     * ������ 
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
            System.out.println("����ָ����ʱ�쳣 : " + e.getLocalizedMessage());  
            connectionRollback(connection) ;  
            return false ;  
        }  
    }  
      
    /** 
     * ��ָ�����в���һ�����ݡ� 
     * @param table ���� 
     * @param params �������� 
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
            System.out.println("������" + table + "����ʱ�쳣 : " + e.getLocalizedMessage());  
            connectionRollback(connection) ;  
            return false ;  
        }  
    }  
      
    /** 
     * �޸ı���һ��Ԫ������ݡ� 
     * @param table ���� 
     * @param keyParam Ҫ�޸ĵ�Ԫ�������ֵ 
     * @param keyField Ҫ�޸ĵ�Ԫ��������ֶ����� 
     * @param fields Ҫ�޸ĵ�Ԫ����ֶ��������� 
     * @param params Ҫ�޸ĵ�Ԫ���ֵ���� 
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
            System.out.println("�޸ı�" + table + "����ʱ�쳣 : " + e.getLocalizedMessage());  
            connectionRollback(connection) ;  
            return false ;  
        }  
          
    }  
      
    /** 
     * ɾ��ָ������ָ����ֵ��Ԫ�顣 
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
            System.out.println("ɾ����" + table + "����ʱ�쳣 : " + e.getLocalizedMessage());  
            connectionRollback(connection) ;  
            return false ;  
        }  
    }  
    
    /** 
     * ɾ��ָ������ָ����ֵ��Ԫ�顣 
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
            System.out.println("ɾ����" + table + "����ʱ�쳣 : " + e.getLocalizedMessage());  
            connectionRollback(connection) ;  
            return false ;  
        }  
    } 
      
    /** 
     * ��һ����������ָ������������Ԫ����Vector<Vector<Object>>����ʽ���� �������ѯ����ֻ��һ��
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
            System.out.println("��ѯ��" + table + "����ʱ�쳣 : " + e.getLocalizedMessage());  
            return value ;  
        }  
    }  
    
    /** 
     * ��һ����������ָ������������Ԫ����Vector<Vector<Object>>����ʽ���� �������ѯ����ֻ��һ��
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
            System.out.println("��ѯ��" + table + "����ʱ�쳣 : " + e.getLocalizedMessage());  
            return value ;  
        }  
    }  
    
      
    /** 
     * �����ƶ�sql����ѯ��Vector<Vector<Object>>����� 
     * @param sql sql��� 
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
            System.out.println("��ѯ��sql����ʱ�쳣 : " + e.getLocalizedMessage());  
            return value ;  
        }  
    }  
      
    /** 
     * ������һ��������ָ����������Ԫ��������Object[][]��ʽ���� 
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
            System.out.println("��ѯ��" + table + "����ʱ�쳣 : " + e.getLocalizedMessage());  
            return tableObject ;  
        }  
    }  
      
    /** 
     * ��һ���������е�Ԫ����Vector<Vector<Object>>����ʽ���� 
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
            System.out.println("��ѯ��" + table + "����ʱ�쳣 : " + e.getLocalizedMessage());  
            return value ;  
        }  
    }  

    /** 
     * ��һ���������е�Ԫ����Object[][]����ʽ���� 
     * @param table 
     * @return Object[][] 
     */  
    public Object[][] selectObject(String table){  
        Statement stmt = null ;  
        ResultSet rs = null ;  
          
        int columns = getFieldsCounts(table) ;  // ��
        int rows = getTableCount(table) ;  // ��
          
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
            System.out.println("��ѯ��" + table + "����ʱ�쳣 : " + e.getLocalizedMessage());  
            return tableObject ;  
        }  
    }  
    
    /**
     * ��һ���������µ�һ��������object[]����ʽ����
     * @param table
     * @return Object[]
     * */
    
	public Object[] selectLatestObject(String table, String sortCondition) {
		Statement stmt = null;
		ResultSet rs = null;

		int columns = getFieldsCounts(table); // ��

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
			System.out.println("��ѯ��" + table + "����ʱ�쳣 : " + e.getLocalizedMessage());
			return tableObject;
		}
	}
      
    /** 
     * ��һ��ResultSet������е������ֶ���List��ʽ���� 
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
            System.out.println("���ر����ֶ��쳣 ��" + e.getLocalizedMessage());  
            return null ;  
        }  
        return fieldsList ;  
    }  
      
    /** 
     * ��һ�����е������ֶ���List��ʽ���� 
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
            System.out.println("��ѯ��" + table + "����ʱ�쳣 : " + e.getLocalizedMessage());  
        }  
        
        return fieldsList ;  
    }  
      
    /** 
     * ��һ��ResultSet������е������ֶε���Ŀ���� 
     * @param resultSet 
     * @return int 
     */   
    public int getFieldsCounts(ResultSet resultSet){  
        try {  
            return resultSet.getMetaData().getColumnCount();  
        } catch (SQLException e) {  
            System.out.println("���ر����ֶ��쳣 ��" + e.getLocalizedMessage());  
            return 0;  
        }  
    }  
      
    /** 
     * ����һ����������ֶ���Ŀ 
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
            System.out.println("��ѯ��" + table + "����ʱ�쳣 : " + e.getLocalizedMessage());  
        }  
        return counts ;  
    }  
      
    /** 
     * ��ѯһ�����е�����Ԫ����Ŀ 
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
            System.out.println("��ѯ��" + table + "Ԫ����ʱ�쳣 : " + e.getLocalizedMessage());  
            return counts ;  
        }  
    }  
      
    /** 
     * ��ѯһ�����е�����һ������������Ԫ����Ŀ 
     * @param table ���� 
     * @param key �ֶ����� 
     * @param keyValue �ֶ�ֵ 
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
            System.out.println("��ѯ��" + table + "Ԫ����ʱ�쳣 : " + e.getLocalizedMessage());  
            return counts ;  
        }  
    }  
      
    public void connectionRollback(Connection connection){  
        try {  
            connection.rollback() ;  
        } catch (SQLException e) {  
            System.out.println("�쳣ʱ�ع����� : " + e.getLocalizedMessage()) ;  
        }  
    }  
}  
