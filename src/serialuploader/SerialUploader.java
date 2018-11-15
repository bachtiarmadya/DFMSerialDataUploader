/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package serialuploader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import model.SerialData;
import service.FlagManagement;
import service.QueryManagement;
import service.QueueManagement;
import service.UploadManagement;
import util.DbConnection;
import util.Strings;

/**
 *
 * @author bachtiar
 */
public class SerialUploader implements Runnable {

  static int interval;
  static Timer timer;
  Connection conn = null;

  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) {
    try {
      Class.forName("com.mysql.jdbc.Driver");
    } catch (ClassNotFoundException cnfe) {
      System.out.println(cnfe);
    }
    //Make scheduled programm running
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    Runnable task = new SerialUploader();

    task.run();
    int initialDelay = 4;
    int periodicDelay = 60;

    scheduler.scheduleAtFixedRate(task, initialDelay, periodicDelay,
            TimeUnit.SECONDS);
  }

  //On running programm
  @Override
  public void run() {
    //Initialize model from SerialData
    SerialData serialData = new SerialData();
    //Initialize function from QueuedManagement
    QueueManagement qm = new QueueManagement();
    //Initialize function from UploadManagement
    UploadManagement um = new UploadManagement();
    try {
      conn = DriverManager.getConnection(DbConnection.MYSQL_URL, DbConnection.MYSQL_UNAME, DbConnection.MYSQL_PASSWORD);
      qm.getPendingQueue();
      um.checkPendingUpload();
      qm.getPendingCalibQueue();
      um.checkPendingCalib();

      Calendar calendar = Calendar.getInstance();
      java.sql.Date now = new java.sql.Date(calendar.getTime().getTime());

      // get all data from LCR
      String query = QueryManagement.GETALL_SERIAL_DATA;
      Statement stmt = conn.createStatement();
      ResultSet rs = stmt.executeQuery(query);

      SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
      boolean firstFound = false;
      StringBuilder ids = new StringBuilder();
      StringBuilder message = new StringBuilder();
      ArrayList<SerialData> serialDatas = new ArrayList<>();
      Long avgId = 0l;
      Long ticketId = 0l;
      while (rs.next()) {
        String data = new String(rs.getBytes("data"));
        if (data.indexOf("TICKET NUMBER") > 0) {
          if (!firstFound) {
            firstFound = true;
          } else {
            if (ids.length() > 0) {
              serialData.setIds(ids.toString());
              serialData.setFullMessage(message.toString());
              serialDatas.add(serialData);

              //reset serialData and gangs
              ids.setLength(0);
              message.setLength(0);
              serialData = null;
              serialData = new SerialData();
            }
          }

          String ticketNumber = data.substring(data.indexOf("TICKET NUMBER") + 13, data.length()).trim();
          ticketId = rs.getLong("ID");
          try {
            Long i = Long.valueOf(ticketNumber);
            serialData.setTicketNumber(ticketNumber);
            serialData.setDataState("NORMAL");
          } catch (NumberFormatException nfe) {
            serialData.setDataState("ABNORMAL");
          }
        }

//        if (data.indexOf("VHS") > 0) {
//          String sData = data.substring(data.indexOf("VHS") + 3, data.length()).trim();
//          
//          try {
//            //serialData.setOtherOne("VHS " + sData);
//            serialData.setDataState("NORMAL");
//          } catch (NumberFormatException nfe) {
//            serialData.setDataState("ABNORMAL");
//          }
//        }
//
//        if (data.indexOf("DIPO LOK") > 0) {
//          String sData = data.substring(data.indexOf("DIPO LOK") + 9, data.length()).trim();
//          try {
//            //serialData.setOtherTwo("DIPO LOK " + sData);
//            serialData.setDataState("NORMAL");
//          } catch (NumberFormatException nfe) {
//            serialData.setDataState("ABNORMAL");
//          }
//        }
//
//        if (data.indexOf("FLOWMETER") > 0) {
//          String sData = data.substring(data.indexOf("FLOWMETER") + 10, data.length()).trim();
//          try {
//            //serialData.setOtherThree("FM" + sData);
//            serialData.setDataState("NORMAL");
//          } catch (NumberFormatException nfe) {
//            serialData.setDataState("ABNORMAL");
//          }
//        }

        if (data.indexOf("START COUNT") > 0) {
          String start = data.substring(data.indexOf("START COUNT") + 11, data.length()).trim();
          String[] s = start.split("\\s+");
          if (2 == s.length) {
            serialData.setStartCount(s[0].trim());
            serialData.setStartCountUom(s[1].trim());
          } else {
            serialData.setDataState("ABNORMAL");
          }
        }

        if (data.indexOf("START") > 0) {
          String start = data.substring(data.indexOf("START") + 5, data.length()).trim();
          try {
            Date date = sdf.parse(start);
            serialData.setStart(start);
          } catch (ParseException pe) {
            serialData.setDataState("ABNORMAL");
          }
        }

        if (data.indexOf("FINISH") > 0) {

          String finish = data.substring(data.indexOf("FINISH") + 6, data.length()).trim();
          try {
            Date date = sdf.parse(finish);
            serialData.setFinish(finish);
          } catch (ParseException pe) {
            serialData.setDataState("ABNORMAL");
          }
        }

        if (data.indexOf("END COUNT") > 0) {
          String end = data.substring(data.indexOf("END COUNT") + 9, data.length()).trim();
          String[] s = end.split("\\s+");
          if (2 == s.length) {
            serialData.setEndCount(s[0].trim());
            serialData.setEndCountUom(s[1].trim());
          } else {
            serialData.setDataState("ABNORMAL");
          }

        }

        if (data.indexOf("GROSS DELIVER") > 0) {
          String gross = data.substring(data.indexOf("GROSS DELIVER") + 13, data.length()).trim();
          String[] s = gross.split("\\s+");
          if (2 == s.length) {
            serialData.setGrossDeliver(s[0].trim());
            serialData.setGrossDeliverUom(s[1].trim());
          } else {
            serialData.setDataState("ABNORMAL");
          }
        }

        if ((avgId + 1) == rs.getLong("ID") && (rs.getLong("ID") > 1)) {
          String sData = data.substring(4, data.length()).trim();
          try {
            Integer i = Integer.valueOf(sData);
            serialData.setAfterAvgFlowRate(sData);
          } catch (NumberFormatException nfe) {
            serialData.setDataState("ABNORMAL");
          }
          avgId = 0l;
        }

        if (data.indexOf("AVG FLOW RATE") > 0) {
          avgId = rs.getLong("ID");
          String sdata = data.substring(data.indexOf("AVG FLOW RATE") + 13, data.length()).trim();
          String[] s = sdata.split("\\s+");
          if (2 == s.length) {
            serialData.setAvgFlowRate(s[0].trim());
            serialData.setAvgFlowRateUom(s[1].trim());
          } else {
            serialData.setDataState("ABNORMAL");
          }
        }

        if (data.indexOf("SALE NUMBER") > 0) {
          String sData = data.substring(data.indexOf("SALE NUMBER") + 11, data.length()).trim();
          try {
            Long i = Long.valueOf(sData);
            serialData.setSaleNumber(sData);
          } catch (NumberFormatException nfe) {
            serialData.setDataState("ABNORMAL");
          }
        }

        if (data.indexOf("METER NUMBER") > 0) {
          String sData = data.substring(data.indexOf("METER NUMBER") + 12, data.length()).trim();
          try {
            Long i = Long.valueOf(sData);
            serialData.setMeterNumber(sData);
          } catch (NumberFormatException nfe) {
            System.out.println(rs.getLong("ID") + " <-----> FOUND METER NUMBER --> " + data + " METER NUMBER --> " + sData);
            serialData.setDataState("ABNORMAL");
          }
        }

        if (data.indexOf("UNIT ID") > 0) {
          String sData = data.substring(data.indexOf("UNIT ID") + 7, data.length()).trim();
          if (!sData.equalsIgnoreCase("")) {
            serialData.setUnitId(sData);
          } else {
            serialData.setDataState("ABNORMAL");
          }
        }

        if (data.indexOf("DUPLICATE TICKET") > 0) {
          serialData.setDuplicate("DUPLICATE TICKET");
        }
        if (serialData.getDataState() == null || serialData.getDataState() == "") {
          serialData.setDataState("ABNORMAL");
        }

        if (data.indexOf("SHIFT START") > 0) {
          String sData = data.substring(data.indexOf("SHIFT START") + 11, data.length()).trim();
          try {
            serialData.setShiftStart(sData);
            serialData.setDataState("NORMAL");
          } catch (NumberFormatException nfe) {
            serialData.setDataState("ABNORMAL");
          }
        }

        if (data.indexOf("SHIFT FINISH") > 0) {
          String sData = data.substring(data.indexOf("SHIFT FINISH") + 12, data.length()).trim();
          try {
            serialData.setShiftFinish(sData);
            serialData.setDataState("NORMAL");
          } catch (NumberFormatException nfe) {
            serialData.setDataState("ABNORMAL");
          }
        }

        if (data.indexOf("CALIBRATION NUMBER") > 0) {
          String sData = data.substring(data.indexOf("CALIBRATION NUMBER") + 18, data.length()).trim();
          try {
            
            serialData.setCalNum(sData);
            serialData.setDataState("NORMAL");
          } catch (NumberFormatException nfe) {
            serialData.setDataState("ABNORMAL");
          }
        }

        if (data.indexOf("SHIFT NET") > 0) {
          
          String sData = data.substring(data.indexOf("SHIFT NET") + 9, data.length()).trim();
          String[] s = sData.split("\\s+");
          if (2 == s.length) {           
            serialData.setShiftNet(s[0].trim());
            serialData.setDataState("NORMAL");
          } else {
            serialData.setDataState("ABNORMAL");
          }
        }

        if (data.indexOf("SHIFT GROSS") > 0) {
          String sData = data.substring(data.indexOf("SHIFT GROSS") + 11, data.length()).trim();
          String[] s = sData.split("\\s+");
          if (2 == s.length) {           
            serialData.setShiftGross(s[0].trim());
            serialData.setDataState("NORMAL");
          } else {
            serialData.setDataState("ABNORMAL");
          }
        }

        if (data.indexOf("END NET TOTAL") > 0) {
          String sData = data.substring(data.indexOf("END NET TOTAL") + 13, data.length()).trim();
          String[] s = sData.split("\\s+");
          if (2 == s.length) {           
            serialData.setEndNetTotal(s[0].trim());
            serialData.setDataState("NORMAL");
          } else {
            serialData.setDataState("ABNORMAL");
          }
        }

        if (data.indexOf("END TOTALIZER") > 0) {
          String sData = data.substring(data.indexOf("END TOTALIZER") + 13, data.length()).trim();
          String[] s = sData.split("\\s+");
          if (2 == s.length) {           
            serialData.setEndTotalizer(s[0].trim());
            serialData.setDataState("NORMAL");
          } else {
            serialData.setDataState("ABNORMAL");
          }
        }

        if (data.indexOf("DELIVERIES") > 0) {
          String sData = data.substring(data.indexOf("DELIVERIES") + 10, data.length()).trim();
          try {
            serialData.setDeliveries(sData);
            serialData.setDataState("NORMAL");
          } catch (NumberFormatException nfe) {
            serialData.setDataState("ABNORMAL");
          }
        }
        ids.append(rs.getInt("ID"));
          ids.append(",");
        message.append(data);

        //System.out.println(rs.getInt("ID") + "-->" + data);
      }
      if (ids.length() > 0) {
        serialData.setIds(ids.toString());
        serialData.setFullMessage(message.toString());
        serialDatas.add(serialData);
        System.out.println(Strings.INFO_READY_TO_UPLOAD);
      }
      conn.close();

      for (int i = 0; i < serialDatas.size(); i++) {
        String[] dd = serialDatas.get(i).getIds().replaceAll("^[,\\s]+", "").split("[,\\s]+");
        Long startId = Long.valueOf(dd[0]);
        Long endId = Long.valueOf(dd[dd.length - 1]);
        serialDatas.get(i).setStartId(startId);
        serialDatas.get(i).setEndId(endId);

        if (i == (serialDatas.size() - 1)) {
          if (serialDatas.get(i).getDataState().equalsIgnoreCase("NORMAL")) {
            if (serialDatas.get(i).getUnitId() != null && !serialDatas.get(i).getUnitId().equalsIgnoreCase("")) {
              qm.queuedData(serialData);
              qm.calibQueuedData(serialData);
            } else {
              serialDatas.get(i).setDataState("ABNORMAL");
              qm.queuedData(serialData);
              qm.calibQueuedData(serialData);
            }
          } else {
            if (serialDatas.get(i).getUnitId() == null) {
            } else {
              if (!serialDatas.get(i).getUnitId().equalsIgnoreCase("")) {
                if (serialDatas.get(i).getDuplicate() == null) {
                  qm.queuedData(serialDatas.get(i));
                  qm.calibQueuedData(serialData);
                } else {
                  if (serialDatas.get(i).getDuplicate().equalsIgnoreCase("DUPLICATE TICKET")) {
                    qm.queuedData(serialDatas.get(i));
                    qm.calibQueuedData(serialData);
                  } else {
                    qm.queuedData(serialDatas.get(i));
                    qm.calibQueuedData(serialData);
                  }
                }
              }
            }
          }

        } else {
          if (serialDatas.get(i).getDataState().equalsIgnoreCase("NORMAL")) {
            if (serialDatas.get(i).getUnitId() != null && !serialDatas.get(i).getUnitId().equalsIgnoreCase("")) {
            } else {
              serialDatas.get(i).setDataState("ABNORMAL");
            }
          }
          qm.queuedData(serialDatas.get(i));
          qm.calibQueuedData(serialData);
        }
      }

      for (int i = 0; i < serialDatas.size(); i++) {
        FlagManagement fm = new FlagManagement();
        fm.flagDownloaded(serialDatas.get(i));        
      }
    } catch (SQLException sqle) {
      System.out.println(sqle);
    }
    try {
      //Runnning Progress bar
      for (double progressPercentage = 0.0; progressPercentage < 1.0; progressPercentage += 0.01) {
        runProgress(progressPercentage);
        Thread.sleep(600);
      }
    } catch (InterruptedException e) {
    }
  }

  static void runProgress(double progressPercentage) {
    final int width = 60; // progress bar width in chars

    System.out.print("\r[");
    int i = 0;
    for (; i <= (int) (progressPercentage * width); i++) {
      System.out.print("#");
    }
    for (; i < width; i++) {
      System.out.print(" ");
    }
    System.out.print("]");
  }
}
