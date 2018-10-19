/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.SerialCalibrationQueued;
import model.SerialData;
import model.SerialDataQueue;
import serialuploader.SerialUploader;
import util.DbConnection;
import util.Strings;

/**
 *
 * @author bachtiar
 */
public class QueueManagement {

  Connection conn = null;

  public void queuedData(SerialData serialData) {
    String insertSerialQueuedQuery = QueryManagement.INSERT_INTO_SERIAL_DATA_QUEUE;

    try {

      conn = DriverManager.getConnection(DbConnection.MYSQL_URL, DbConnection.MYSQL_UNAME, DbConnection.MYSQL_PASSWORD);

      PreparedStatement insertQueue = conn.prepareStatement(insertSerialQueuedQuery);
      insertQueue.setLong(1, serialData.getStartId());
      insertQueue.setLong(2, serialData.getEndId());
      insertQueue.setString(3, serialData.getTicketNumber());
      if(serialData.getStart() == null || serialData.getStart() == "" )
      {
        insertQueue.setString(4, serialData.getFinish());
      }
      else
      {
       insertQueue.setString(4, serialData.getStart()); 
      }
      
      if(serialData.getStart() == null || serialData.getStart() == "" )
      {
        insertQueue.setString(5, serialData.getStart()); 
      }
      else
      {
        insertQueue.setString(5, serialData.getFinish());
      }
      if (serialData.getStartCount() == null || serialData.getStartCount() == "")
      {
        insertQueue.setString(6, "0.0");
      }
      else
      {
        insertQueue.setString(6, serialData.getStartCount());
      }      
      insertQueue.setString(7, serialData.getStartCountUom());
      insertQueue.setString(8, serialData.getEndCount());
      if (serialData.getEndCountUom() == null || serialData.getEndCountUom() == ""){
        insertQueue.setString(9, serialData.getGrossDeliver());
      }
      else
      {
       insertQueue.setString(9, serialData.getEndCountUom()); 
      }      
      if (serialData.getGrossDeliver() == null || serialData.getGrossDeliver() == "" )
      {
        insertQueue.setString(10, serialData.getEndCount());
      }
      else
      {
        insertQueue.setString(10, serialData.getGrossDeliver());
      }
      insertQueue.setString(11, serialData.getGrossDeliverUom());
      insertQueue.setString(12, serialData.getAvgFlowRate());
      insertQueue.setString(13, serialData.getAvgFlowRateUom());
      insertQueue.setString(14, serialData.getAfterAvgFlowRate());
      insertQueue.setString(15, serialData.getSaleNumber());
      insertQueue.setString(16, serialData.getMeterNumber());
      insertQueue.setString(17, serialData.getUnitId());
      insertQueue.setString(18, serialData.getDuplicate());
      insertQueue.setString(19, serialData.getOtherOne());
      insertQueue.setString(20, serialData.getOtherTwo());
      insertQueue.setString(21, serialData.getOtherThree());
      insertQueue.setString(22, serialData.getOtherFour());
      insertQueue.setString(23, serialData.getOtherFive());
      insertQueue.setString(24, serialData.getDataState());

      insertQueue.executeUpdate();

    } catch (SQLException | NumberFormatException e) {
      System.out.println(e);
    } finally {
      try {

        conn.close();
      } catch (SQLException ex) {
        Logger.getLogger(SerialUploader.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }

  public List<SerialDataQueue> getPendingQueue() {
    List<SerialDataQueue> listData = new ArrayList<>();
    String queueResults = QueryManagement.SELECT_ALL_SERIAL_DATA_QUEUE;

    try {
      conn = DriverManager.getConnection(DbConnection.MYSQL_URL, DbConnection.MYSQL_UNAME, DbConnection.MYSQL_PASSWORD);
      Statement stmt = conn.createStatement();
      ResultSet rs = stmt.executeQuery(queueResults);
      while (rs.next()) {
        SerialDataQueue sdq = new SerialDataQueue();
        sdq.setId(rs.getLong(1));
        sdq.setUploaded(rs.getShort(2));
        sdq.setIDstart(rs.getLong(3));
        sdq.setIDend(rs.getLong(4));
        sdq.setDataState(rs.getString(5));
        sdq.setTicketNo(rs.getString(6));
        sdq.setStart(rs.getString(7));
        sdq.setFinish(rs.getString(8));
        sdq.setStartCount(rs.getString(9));
        sdq.setStartCountUom(rs.getString(10));
        sdq.setEndCount(rs.getString(11));
        sdq.setEndCountUom(rs.getString(12));
        sdq.setGrossDeliver(rs.getString(13));
        sdq.setGrossDeliverUom(rs.getString(14));
        sdq.setAvgFlowRate(rs.getString(15));
        sdq.setAvgFlowRateUom(rs.getString(16));
        sdq.setAfterAvgFlowRate(rs.getString(17));
        sdq.setSaleNumber(rs.getString(18));
        sdq.setMeterNumber(rs.getString(19));
        sdq.setUnitId(rs.getString(20));
        sdq.setDuplicate(rs.getString(21));
        sdq.setOtherOne(rs.getString(22));
        sdq.setOtherTwo(rs.getString(23));
        sdq.setOtherThree(rs.getString(24));
        sdq.setOtherFour(rs.getString(25));
        sdq.setOtherFive(rs.getString(26));
        listData.add(sdq);
        if (listData.isEmpty() == false) {

          queueData(listData);
          return listData;

        } else {
          return listData;
        }

      }
    } catch (SQLException e) {
      System.out.println(e);
    } finally {
      try {
        conn.close();

      } catch (SQLException ex) {
        Logger.getLogger(SerialUploader.class
                .getName()).log(Level.SEVERE, null, ex);
      }
    }
    return listData;
  }

  private void queueData(List<SerialDataQueue> uploadList) throws SQLException {
    conn = DriverManager.getConnection(DbConnection.MYSQL_URL, DbConnection.MYSQL_UNAME, DbConnection.MYSQL_PASSWORD);
    String insertSerialQueueQuery = QueryManagement.INSERT_INTO_SERIAL_DATA_UPLOADED;

    Iterator<SerialDataQueue> it = uploadList.iterator();

    while (it.hasNext()) {

      try {
        SerialDataQueue serialData = it.next();
        PreparedStatement insertUpload = conn.prepareStatement(insertSerialQueueQuery);
        Long id = serialData.getId();
        insertUpload.setString(1, Strings.SITE_ID);
        insertUpload.setLong(2, serialData.getIDstart());
        insertUpload.setLong(3, serialData.getIDend());
        insertUpload.setString(4, serialData.getDataState());
        insertUpload.setString(5, serialData.getTicketNo());
        insertUpload.setString(6, serialData.getStart());
        insertUpload.setString(7, serialData.getFinish());
        insertUpload.setString(8, serialData.getStartCount());
        insertUpload.setString(9, serialData.getStartCountUom());
        insertUpload.setString(10, serialData.getEndCount());
        insertUpload.setString(11, serialData.getEndCountUom());
        insertUpload.setString(12, serialData.getGrossDeliver());
        insertUpload.setString(13, serialData.getGrossDeliverUom());
        insertUpload.setString(14, serialData.getAvgFlowRate());
        insertUpload.setString(15, serialData.getAvgFlowRateUom());
        insertUpload.setString(16, serialData.getAfterAvgFlowRate());
        insertUpload.setString(17, serialData.getSaleNumber());
        insertUpload.setString(18, serialData.getMeterNumber());
        insertUpload.setString(19, serialData.getUnitId());
        insertUpload.setString(20, serialData.getDuplicate());
        insertUpload.setString(21, serialData.getOtherOne());
        insertUpload.setString(22, serialData.getOtherTwo());
        insertUpload.setString(23, serialData.getOtherThree());
        insertUpload.setString(24, serialData.getOtherFour());
        insertUpload.setString(25, serialData.getOtherFive());

        FlagManagement fm = new FlagManagement();
        fm.flagQueued(id);
        insertUpload.addBatch();

        int[] numUpdates = null;
        numUpdates = insertUpload.executeBatch();
        for (int d = 0; d < numUpdates.length; d++) {
          if (numUpdates[d] == -2) {
            System.out.println("Execution " + d
                    + ": unknown number of rows queued");

          } else {

          }
        }
      } catch (BatchUpdateException e) {
        System.out.println(e);
      } finally {

      }
    }
  }

  public void calibQueuedData(SerialData serialData) {
    String insertSerialQueuedQuery = QueryManagement.INSERT_INTO_SERIAL_CALIBRATION_QUEUED;

    try {
      conn = DriverManager.getConnection(DbConnection.MYSQL_URL, DbConnection.MYSQL_UNAME, DbConnection.MYSQL_PASSWORD);

      PreparedStatement insertQueue = conn.prepareStatement(insertSerialQueuedQuery);
      insertQueue.setString(1, serialData.getShiftStart());
      insertQueue.setString(2, serialData.getShiftFinish());
      insertQueue.setString(3, serialData.getUnitId());
      insertQueue.setString(4, serialData.getSaleNumber());
      insertQueue.setString(5, serialData.getMeterNumber());
      insertQueue.setString(6, serialData.getCalNum());
      insertQueue.setString(7, serialData.getShiftNet());
      insertQueue.setString(8, serialData.getShiftGross());
      insertQueue.setString(9, serialData.getEndNetTotal());
      insertQueue.setString(10, serialData.getEndTotalizer());
      insertQueue.setString(11, serialData.getDeliveries());  
      insertQueue.executeUpdate();

    } catch (SQLException | NumberFormatException e) {
      System.out.println(e);
    } finally {
      try {

        conn.close();
      } catch (SQLException ex) {
        Logger.getLogger(SerialUploader.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }

  public List<SerialCalibrationQueued> getPendingCalibQueue() {
    List<SerialCalibrationQueued> listData = new ArrayList<>();
    String queueResults = QueryManagement.SELECT_QUEUED_CALIBRATION;

    try {
      conn = DriverManager.getConnection(DbConnection.MYSQL_URL, DbConnection.MYSQL_UNAME, DbConnection.MYSQL_PASSWORD);
      Statement stmt = conn.createStatement();
      ResultSet rs = stmt.executeQuery(queueResults);
      while (rs.next()) {
        SerialCalibrationQueued sdq = new SerialCalibrationQueued();
        sdq.setId(rs.getInt(1));
        sdq.setUploaded(rs.getString(2));
        sdq.setShiftStart(rs.getString(3));
        sdq.setShiftFinish(rs.getString(4));
        sdq.setUnitId(rs.getString(5));
        sdq.setSaleNumber(rs.getString(6));
        sdq.setMeterNumber(rs.getString(7));
        sdq.setCalNum(rs.getString(8));
        sdq.setShiftNet(rs.getString(9));
        sdq.setShiftGross(rs.getString(10));
        sdq.setEndNetTotal(rs.getString(11));
        sdq.setEndTotalizer(rs.getString(12));
        sdq.setDeliveries(rs.getString(13));
        listData.add(sdq);
        if (listData.isEmpty() == false) {
          calibQueueData(listData);
          return listData;

        } else {
          return listData;
        }

      }
    } catch (SQLException e) {
      System.out.println(e);
    } finally {
      try {
        conn.close();

      } catch (SQLException ex) {
        Logger.getLogger(SerialUploader.class
                .getName()).log(Level.SEVERE, null, ex);
      }
    }
    return listData;
  }

  private void calibQueueData(List<SerialCalibrationQueued> uploadCalibList) throws SQLException {
    conn = DriverManager.getConnection(DbConnection.MYSQL_URL, DbConnection.MYSQL_UNAME, DbConnection.MYSQL_PASSWORD);
    String insertSerialQueueQuery = QueryManagement.INSERT_INTO_SERIAL_CALIBRATION_UPLOADED;

    Iterator<SerialCalibrationQueued> it = uploadCalibList.iterator();
    while (it.hasNext()) {
      try {
        SerialCalibrationQueued sdq = it.next();
        PreparedStatement insertUpload = conn.prepareStatement(insertSerialQueueQuery);
        long id = sdq.getId();
       
        insertUpload.setString(1, sdq.getShiftStart());
        insertUpload.setString(2, sdq.getShiftFinish());
        insertUpload.setString(3, sdq.getUnitId());
        insertUpload.setString(4, sdq.getSaleNumber());
        insertUpload.setString(5, sdq.getMeterNumber());
        insertUpload.setString(6, sdq.getCalNum());
        insertUpload.setString(7, sdq.getShiftNet());
        insertUpload.setString(8, sdq.getShiftGross());
        insertUpload.setString(9, sdq.getEndNetTotal());
        insertUpload.setString(10, sdq.getEndTotalizer());
        insertUpload.setString(11, sdq.getDeliveries());

        FlagManagement fm = new FlagManagement();
        fm.flagCalibQueued(id);
        insertUpload.addBatch();

        int[] numUpdates = null;
        numUpdates = insertUpload.executeBatch();
        for (int d = 0; d < numUpdates.length; d++) {
          if (numUpdates[d] == -2) {
            System.out.println("Execution " + d
                    + ": unknown number of rows queued");

          } else {

          }
        }
      } catch (BatchUpdateException e) {
        System.out.println(e);
      } finally {

      }
    }
  }

}
