package resoft.tips.chqsh;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.zerone.jdbc.QueryUtil;

import resoft.basLink.Configuration;
import resoft.basLink.util.DBUtil;
import resoft.tips.chqxh.ACEPackUtil;
import resoft.tips.util.FTPUtil;
import resoft.xlink.core.Action;
import resoft.xlink.core.Message;

/**
 * <p>��ӡ˰Ʊ��Ϣ</p>
 * Author: fanchengwei
 * Date: 2007-10-22
 * Time: 18:06:06
 */

public class PrintTaxVod implements Action{
	
	private static final Log logger = LogFactory.getLog(PrintTaxVod.class);
	private static Configuration conf = Configuration.getInstance();
	
	SendMsgToBankSystem send = new SendMsgToBankSystem();
	String rcvMsg = "";
	
	private String payOpBkNameSql="select brtbrnam from brt where brtbr=";
	//fan<
	String fileName = "";
    String tmpPath = conf.getProperty("BankSysConfig", "TempFilePath");
	
	String bankOrgCode="",inputTeller="",payOpBkName="",payBkName="";
	String printType="",taxOrgCode="",payAcct="",taxPayCode="",startDate="",endDate="";
	String taxOrgName="";
	String sqlWhere="",tempFileData="",queryBankNamewhere="";
	String tradeStatus="";
	int printCountstatic=-1;
	public int execute(Message msg) throws Exception {
		
		//payAcct=msg.getString("PayAcct");				//�ʺ�
		taxPayCode=msg.getString("TaxPayCode");			//��˰�˱���
		startDate=(msg.getString("BeginDate"));	//��ʼ����
		endDate=(msg.getString("EndDate"));//��������
		if(startDate!=null && !startDate.equals("")&&endDate!=null && !endDate.equals(""))//��˰�˱�����˺�Ϊ�գ���ʱ��ѯĳ��ʱ����ڵ�˰Ʊ��Ϣ
		{
			startDate=startDate.trim();	//��ʼ����
			endDate=endDate.trim();//��������
			sqlWhere+= " and a.taxDate >='"+startDate+"' and a.taxDate<='"+endDate+"'";//���ڵ��жϵ���ȷ�ԣ�ע��
		}
		if(taxPayCode!=null&&!taxPayCode.equals(""))//�жϵļ������ڣ��˴���Ҫ�޸�
		{  
			taxPayCode=taxPayCode.trim(); 
			sqlWhere+=" and a.taxPayCode='"+taxPayCode+"' "; 
			queryBankNamewhere+=" and taxPayCode='"+taxPayCode+"' "; 
		}
		
		bankOrgCode=msg.getString("BranchNo").trim();			//��������
		sqlWhere+=" and a.BRANCHNO='"+ bankOrgCode +"' ";
		inputTeller=msg.getString("UserId").trim();			//��Ա��
		//��ӡ��Ա
		TaxPiaoInfo.printTeller=inputTeller;
				
		taxOrgCode=msg.getString("TaxOrgCode");			//���ջ���

		//startDate=(msg.getString("WorkDate")).trim();	//��������
		
		fileName="TIPS"+bankOrgCode+inputTeller+"000";	
		
		tradeStatus="000";
		
		//���ջ��ش���
		if(taxOrgCode!=null && !taxOrgCode.equals("")){
			taxOrgCode=taxOrgCode.trim();
			sqlWhere+=" and a.taxOrgCode='"+taxOrgCode+"' ";
			queryBankNamewhere+=" and taxOrgCode='"+taxOrgCode+"' ";
			taxOrgName=DBUtil.queryForString("select taxOrgName from TaxOrgMng where EnabledFlag='Y' and taxOrgCode='"+taxOrgCode+"' ");
			tradeStatus="000"; 
		}		
		
		String payOpBkNameStrSQL="select PAYOPBKNAME from proveinfo where  1=1 "+queryBankNamewhere+"and rowNum=1 ";
		payOpBkName=DBUtil.queryForString(payOpBkNameStrSQL);
		logger.info("SQL:"+payOpBkNameStrSQL+"�����˿������У�"+payOpBkName);
		
		if(tradeStatus.equals("000")){
			//�ɹ����˻��� (��TIPS���˳ɹ�)			
			sqlWhere+=" and a.result='90000' and a.checkStatus='1' "; 
			
			//�����ļ�					
			tradeStatus="000";
			try {
				msg.set("ReturnFileName", fileName);
				String piaoCount=makeTaxPiaoDeatil();
				ftpUpload(fileName);
				msg.set("PiaoCount", piaoCount);
				if(recordPrintCount==0)
				{
					tradeStatus="040";
					msg.set("AddWord", "û�з��������ļ�¼");

				}
				if (recordPrintCount!=0&&piaoCount.equals("0")){//û�з��������ļ�¼
					tradeStatus="040";
					msg.set("AddWord", "�ѳ�����ӡ����");
				} 
				
			}catch(Exception e){
				tradeStatus="010";
				msg.set("AddWord", "����˰Ʊ��ӡ�ļ������쳣");
				logger.info("����˰Ʊ��ӡ�ļ������쳣��");
				e.printStackTrace();				
			}
		}else if(!tradeStatus.equals("111")){
			tradeStatus="020";
			msg.set("AddWord", "δ��ȡ�����ջ��ػ򸶿�����к�");
		}
		msg.set("VCResult", tradeStatus);
		
		if (tradeStatus.equals("000")) {//�ɹ�
			msg.set("ReturnResult", "Y");	
			msg.set("ReturnFileName", fileName);
    	}else {
    		msg.set("ReturnResult", "N");
    	}    	    	
		
		return SUCCESS;
	}
	int recordPrintCount=0;
	//���ɲ�ѯ�ļ�
	public String makeTaxPiaoDeatil() throws Exception {
		
        File f = new File(tmpPath, fileName);
        Writer writer = new FileWriter(f);
        
        //��ѯʵʱ
        String realTimeSql="select * from realTimePayMent a where 1=1 "+sqlWhere;
        logger.info("��ѯʵʱ��"+realTimeSql);
        List realTimeList=QueryUtil.queryRowSet(realTimeSql);
        logger.info("################   realTimeList  = " + realTimeList.size());
        
        //��ѯ����
		String batchSql="select * from BatchPackDetail a where 1=1 "+sqlWhere;
		logger.info("��ѯ������"+batchSql);
		List batchList=QueryUtil.queryRowSet(batchSql);
		logger.info("################   batchList  = " + batchList.size());	
		int printcount=-1;
		String printStr="";
		//����ʵʱ
		int realprintcount=0;
		for(int i=0;i<realTimeList.size();i++){
			Map row=(Map)realTimeList.get(i);
			row.put("TaxVodType", "1");			
			TaxPiaoInfo piaoInfo=new TaxPiaoInfo(); 
			String payOpBkNameStrSQL="select PAYOPBKNAME from proveinfo where 1=1"+queryBankNamewhere+"and rowNum=1"; 
			payOpBkName=DBUtil.queryForString(payOpBkNameStrSQL);
			logger.info("SQL:"+payOpBkNameStrSQL+"�����˿������У�"+payOpBkName);
			 
			piaoInfo.packTags.put("PayOpBkName", (String)piaoInfo.packTags.get("PayOpBkName")+payOpBkName);
			piaoInfo.packTags.put("TaxOrgName", (String)piaoInfo.packTags.get("TaxOrgName")+taxOrgName);			
			piaoInfo.packTags.put("PayeeOrgName", (String)piaoInfo.packTags.get("PayeeOrgName")+payBkName);		
//			writer.write(piaoInfo.initTaxPiaoInfo(row));
//			writer.write("\f");
			
		 
				printcount=Integer.parseInt((String)row.get("printTimes"));//�����жϴ�ӡ����
				if(printcount==0)
				{
					writer.write(piaoInfo.initTaxPiaoInfo(row));
					if(realprintcount%2==0&&realprintcount!=0)//ǰ���Ѿ�ʵ��ӡ������
					{
						writer.write("\f");
					}
					else if(realprintcount!=0)//ǰ���Ѿ�ʵ�ʴ�ӡ��ż���Σ���1�δ�ӡ����
					{
						writer.write("\n\n\n\n\n");
					} 
					realprintcount++;
				}
				if(printcount==1||printcount==2)//�ڶ��λ�����δ�ӡ
				{
					//����
					String nullge=piaoInfo.lineStartFormat.substring(0,12);
					printStr=ACEPackUtil.rightStrFormat("83","����"," ");
					printStr+="\n";
					writer.write(printStr+piaoInfo.initTaxPiaoInfo(row)+"\n\n\n\n\n");
					
					if(realprintcount%2==0&&realprintcount!=0)//ǰ���Ѿ�ʵ��ӡ������
					{
						writer.write("\f");
					}
					else if(realprintcount!=0)
					{
						writer.write("\n\n\n\n\n");
					}
					realprintcount++;
				}
				else if(printcount>=3)//�������δ�ӡ,�Թ�
				{ 
					continue;
				}
			 
            piaoInfo=null;            
            writer.flush();            
		}		
		
		int batchPrintcount=-1;
		int batchPrintRealCount=0;
		//��������	
		for(int i=0;i<batchList.size();i++){
			Map row=(Map)batchList.get(i);
			row.put("TaxVodType", "2");
			TaxPiaoInfo piaoInfo=new TaxPiaoInfo();
			String payOpBkNameStrSQL="select PAYOPBKNAME from proveinfo where 1=1"+queryBankNamewhere+"and rowNum=1"; 
			payOpBkName=DBUtil.queryForString(payOpBkNameStrSQL);
			payOpBkName=DBUtil.queryForString(payOpBkNameStrSQL);
			logger.info("SQL:"+payOpBkNameStrSQL+"�����˿������У�"+payOpBkName);
			 
			piaoInfo.packTags.put("PayOpBkName", (String)piaoInfo.packTags.get("PayOpBkName")+payOpBkName);
			piaoInfo.packTags.put("TaxOrgName", (String)piaoInfo.packTags.get("TaxOrgName")+taxOrgName);			
			piaoInfo.packTags.put("PayeeOrgName", (String)piaoInfo.packTags.get("PayeeOrgName")+payBkName);
			
			batchPrintcount=Integer.parseInt((String)row.get("printTimes"));//�����жϴ�ӡ����
			if(batchPrintcount==0)
			{
				writer.write(piaoInfo.initTaxPiaoInfo(row));
				if(batchPrintRealCount%2==0&&batchPrintRealCount!=0)//ǰ���Ѿ�ʵ��ӡ������
				{
					writer.write("\f");
				}
				else if(batchPrintRealCount!=0)//ǰ���Ѿ�ʵ�ʴ�ӡ��ż���Σ���1�δ�ӡ����
				{
					writer.write("\n\n\n\n\n");
				} 
				batchPrintRealCount++;
			}
			if(batchPrintcount==1||batchPrintcount==2)//�ڶ��λ�����δ�ӡ
			{
				//����
				String nullge=piaoInfo.lineStartFormat.substring(0,12);
				printStr=ACEPackUtil.rightStrFormat("83","����"," ");
				printStr+="\n";
				writer.write(printStr+piaoInfo.initTaxPiaoInfo(row)+"\n\n\n\n\n");
				
				if(batchPrintRealCount%2==0&&batchPrintRealCount!=0)//ǰ���Ѿ�ʵ��ӡ������
				{
					writer.write("\f");
				}
				else if(batchPrintRealCount!=0)
				{
					writer.write("\n\n\n\n\n");
				}
				batchPrintRealCount++;
			}
			else if(batchPrintcount>=3)//�������δ�ӡ,�Թ�
			{ 
				continue;
			}
			/*if(printcount==-1)//��һ����¼
			{
				printcount=Integer.parseInt((String)row.get("printTimes"));//�����жϴ�ӡ����
				if(printcount==1||printcount==2)//�ڶ��κ͵����δ�ӡ
				{
					//����
					printStr=ACEPackUtil.rightStrFormat("83","����"," ");
					printStr+="\n";
					writer.write(printStr+piaoInfo.initTaxPiaoInfo(row)+"\n\n\n\n\n");
					 
				}
				else if(printcount>=3)//�������δ�ӡ
				{
					return "error";
				}
			}
			else{
				if(i%2 == 1){ 
					
					writer.write(piaoInfo.initTaxPiaoInfo(row));
					writer.write("\f\n");
				}else{
					writer.write(piaoInfo.initTaxPiaoInfo(row)+"\n\n\n\n\n");
				}
			} 
		*/
            piaoInfo=null;            
            writer.flush();           
		}		
		writer.close();
				
		
		//�޸�ʵʱ��˰�Ĵ�ӡ����
		if (realTimeList.size()>0) {
			DBUtil.executeUpdate("update realTimePayMent a set a.printTimes=a.printTimes+1 where 1=1 "+sqlWhere);
		}
		//�޸�������˰�Ĵ�ӡ����
		if (batchList.size()>0) {
			DBUtil.executeUpdate("update BatchPackDetail a set a.printTimes=a.printTimes+1 where 1=1 "+sqlWhere);
		}
		
		logger.info("��Ϣ�����:" + f.getAbsolutePath());	
		recordPrintCount=realTimeList.size()+batchList.size();
		return ""+(realprintcount+batchPrintRealCount);
	}
	//yangyuanxu add 
	public int getStatus(String BranchNo) throws SQLException{
		int len = 0;
		logger.info("�������ǣ���"+BranchNo);
		len=DBUtil.queryForInt("select count(*) from bank_relation where bankorgcode='"+BranchNo+"'");	
		logger.info("len is "+len );	
		
		if(len > 0)
			return 1;
		else
			return 0;
		
	}
	//yangyuanxu add 
	public String getPayeeBankNo(String BranchNo) throws SQLException{
		String PayeeBankNo = "";
		PayeeBankNo=DBUtil.queryForString("select PayeeBankNo from bank_relation where bankorgcode='"+BranchNo+"'");	
		PayeeBankNo=PayeeBankNo.trim();
		return PayeeBankNo;
	}	
	
	public void ftpUpload(String filename) throws Exception{
		FTPUtil ftp = new FTPUtil();
		ftp.setServer(conf.getProperty("SXBankFtp", "FtpServer"));
		ftp.setPort(Integer.parseInt(conf.getProperty("SXBankFtp", "FtpPort")));
		ftp.setPath(conf.getProperty("SXBankFtp", "FtpPath"));
		ftp.setUser(conf.getProperty("SXBankFtp", "FtpUser"));
		ftp.setPassword(conf.getProperty("SXBankFtp", "FtpPassword"));
		ftp.setLocalpath(conf.getProperty("SXBankFtp", "TempFilePath"));
		ftp.upload(filename);
	}
	
}