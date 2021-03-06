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
 * <p>打印税票信息</p>
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
		
		//payAcct=msg.getString("PayAcct");				//帐号
		taxPayCode=msg.getString("TaxPayCode");			//纳税人编码
		startDate=(msg.getString("BeginDate"));	//开始日期
		endDate=(msg.getString("EndDate"));//结束日期
		if(startDate!=null && !startDate.equals("")&&endDate!=null && !endDate.equals(""))//纳税人编码或账号为空，此时查询某个时间段内的税票信息
		{
			startDate=startDate.trim();	//开始日期
			endDate=endDate.trim();//结束日期
			sqlWhere+= " and a.taxDate >='"+startDate+"' and a.taxDate<='"+endDate+"'";//日期的判断的正确性，注意
		}
		if(taxPayCode!=null&&!taxPayCode.equals(""))//判断的记账日期，此处需要修改
		{  
			taxPayCode=taxPayCode.trim(); 
			sqlWhere+=" and a.taxPayCode='"+taxPayCode+"' "; 
			queryBankNamewhere+=" and taxPayCode='"+taxPayCode+"' "; 
		}
		
		bankOrgCode=msg.getString("BranchNo").trim();			//机构代码
		sqlWhere+=" and a.BRANCHNO='"+ bankOrgCode +"' ";
		inputTeller=msg.getString("UserId").trim();			//柜员号
		//打印柜员
		TaxPiaoInfo.printTeller=inputTeller;
				
		taxOrgCode=msg.getString("TaxOrgCode");			//征收机关

		//startDate=(msg.getString("WorkDate")).trim();	//记账日期
		
		fileName="TIPS"+bankOrgCode+inputTeller+"000";	
		
		tradeStatus="000";
		
		//征收机关代码
		if(taxOrgCode!=null && !taxOrgCode.equals("")){
			taxOrgCode=taxOrgCode.trim();
			sqlWhere+=" and a.taxOrgCode='"+taxOrgCode+"' ";
			queryBankNamewhere+=" and taxOrgCode='"+taxOrgCode+"' ";
			taxOrgName=DBUtil.queryForString("select taxOrgName from TaxOrgMng where EnabledFlag='Y' and taxOrgCode='"+taxOrgCode+"' ");
			tradeStatus="000"; 
		}		
		
		String payOpBkNameStrSQL="select PAYOPBKNAME from proveinfo where  1=1 "+queryBankNamewhere+"and rowNum=1 ";
		payOpBkName=DBUtil.queryForString(payOpBkNameStrSQL);
		logger.info("SQL:"+payOpBkNameStrSQL+"付款人开户银行："+payOpBkName);
		
		if(tradeStatus.equals("000")){
			//成功对账划款 (与TIPS对账成功)			
			sqlWhere+=" and a.result='90000' and a.checkStatus='1' "; 
			
			//生成文件					
			tradeStatus="000";
			try {
				msg.set("ReturnFileName", fileName);
				String piaoCount=makeTaxPiaoDeatil();
				ftpUpload(fileName);
				msg.set("PiaoCount", piaoCount);
				if(recordPrintCount==0)
				{
					tradeStatus="040";
					msg.set("AddWord", "没有符合条件的记录");

				}
				if (recordPrintCount!=0&&piaoCount.equals("0")){//没有符合条件的记录
					tradeStatus="040";
					msg.set("AddWord", "已超过打印次数");
				} 
				
			}catch(Exception e){
				tradeStatus="010";
				msg.set("AddWord", "生成税票打印文件出现异常");
				logger.info("生成税票打印文件出现异常：");
				e.printStackTrace();				
			}
		}else if(!tradeStatus.equals("111")){
			tradeStatus="020";
			msg.set("AddWord", "未能取到征收机关或付款开户行行号");
		}
		msg.set("VCResult", tradeStatus);
		
		if (tradeStatus.equals("000")) {//成功
			msg.set("ReturnResult", "Y");	
			msg.set("ReturnFileName", fileName);
    	}else {
    		msg.set("ReturnResult", "N");
    	}    	    	
		
		return SUCCESS;
	}
	int recordPrintCount=0;
	//生成查询文件
	public String makeTaxPiaoDeatil() throws Exception {
		
        File f = new File(tmpPath, fileName);
        Writer writer = new FileWriter(f);
        
        //查询实时
        String realTimeSql="select * from realTimePayMent a where 1=1 "+sqlWhere;
        logger.info("查询实时："+realTimeSql);
        List realTimeList=QueryUtil.queryRowSet(realTimeSql);
        logger.info("################   realTimeList  = " + realTimeList.size());
        
        //查询批量
		String batchSql="select * from BatchPackDetail a where 1=1 "+sqlWhere;
		logger.info("查询批量："+batchSql);
		List batchList=QueryUtil.queryRowSet(batchSql);
		logger.info("################   batchList  = " + batchList.size());	
		int printcount=-1;
		String printStr="";
		//处理实时
		int realprintcount=0;
		for(int i=0;i<realTimeList.size();i++){
			Map row=(Map)realTimeList.get(i);
			row.put("TaxVodType", "1");			
			TaxPiaoInfo piaoInfo=new TaxPiaoInfo(); 
			String payOpBkNameStrSQL="select PAYOPBKNAME from proveinfo where 1=1"+queryBankNamewhere+"and rowNum=1"; 
			payOpBkName=DBUtil.queryForString(payOpBkNameStrSQL);
			logger.info("SQL:"+payOpBkNameStrSQL+"付款人开户银行："+payOpBkName);
			 
			piaoInfo.packTags.put("PayOpBkName", (String)piaoInfo.packTags.get("PayOpBkName")+payOpBkName);
			piaoInfo.packTags.put("TaxOrgName", (String)piaoInfo.packTags.get("TaxOrgName")+taxOrgName);			
			piaoInfo.packTags.put("PayeeOrgName", (String)piaoInfo.packTags.get("PayeeOrgName")+payBkName);		
//			writer.write(piaoInfo.initTaxPiaoInfo(row));
//			writer.write("\f");
			
		 
				printcount=Integer.parseInt((String)row.get("printTimes"));//首先判断打印次数
				if(printcount==0)
				{
					writer.write(piaoInfo.initTaxPiaoInfo(row));
					if(realprintcount%2==0&&realprintcount!=0)//前面已经实打印奇数次
					{
						writer.write("\f");
					}
					else if(realprintcount!=0)//前面已经实际打印了偶数次，第1次打印除外
					{
						writer.write("\n\n\n\n\n");
					} 
					realprintcount++;
				}
				if(printcount==1||printcount==2)//第二次或第三次打印
				{
					//补打
					String nullge=piaoInfo.lineStartFormat.substring(0,12);
					printStr=ACEPackUtil.rightStrFormat("83","补打"," ");
					printStr+="\n";
					writer.write(printStr+piaoInfo.initTaxPiaoInfo(row)+"\n\n\n\n\n");
					
					if(realprintcount%2==0&&realprintcount!=0)//前面已经实打印奇数次
					{
						writer.write("\f");
					}
					else if(realprintcount!=0)
					{
						writer.write("\n\n\n\n\n");
					}
					realprintcount++;
				}
				else if(printcount>=3)//超过三次打印,略过
				{ 
					continue;
				}
			 
            piaoInfo=null;            
            writer.flush();            
		}		
		
		int batchPrintcount=-1;
		int batchPrintRealCount=0;
		//处理批量	
		for(int i=0;i<batchList.size();i++){
			Map row=(Map)batchList.get(i);
			row.put("TaxVodType", "2");
			TaxPiaoInfo piaoInfo=new TaxPiaoInfo();
			String payOpBkNameStrSQL="select PAYOPBKNAME from proveinfo where 1=1"+queryBankNamewhere+"and rowNum=1"; 
			payOpBkName=DBUtil.queryForString(payOpBkNameStrSQL);
			payOpBkName=DBUtil.queryForString(payOpBkNameStrSQL);
			logger.info("SQL:"+payOpBkNameStrSQL+"付款人开户银行："+payOpBkName);
			 
			piaoInfo.packTags.put("PayOpBkName", (String)piaoInfo.packTags.get("PayOpBkName")+payOpBkName);
			piaoInfo.packTags.put("TaxOrgName", (String)piaoInfo.packTags.get("TaxOrgName")+taxOrgName);			
			piaoInfo.packTags.put("PayeeOrgName", (String)piaoInfo.packTags.get("PayeeOrgName")+payBkName);
			
			batchPrintcount=Integer.parseInt((String)row.get("printTimes"));//首先判断打印次数
			if(batchPrintcount==0)
			{
				writer.write(piaoInfo.initTaxPiaoInfo(row));
				if(batchPrintRealCount%2==0&&batchPrintRealCount!=0)//前面已经实打印奇数次
				{
					writer.write("\f");
				}
				else if(batchPrintRealCount!=0)//前面已经实际打印了偶数次，第1次打印除外
				{
					writer.write("\n\n\n\n\n");
				} 
				batchPrintRealCount++;
			}
			if(batchPrintcount==1||batchPrintcount==2)//第二次或第三次打印
			{
				//补打
				String nullge=piaoInfo.lineStartFormat.substring(0,12);
				printStr=ACEPackUtil.rightStrFormat("83","补打"," ");
				printStr+="\n";
				writer.write(printStr+piaoInfo.initTaxPiaoInfo(row)+"\n\n\n\n\n");
				
				if(batchPrintRealCount%2==0&&batchPrintRealCount!=0)//前面已经实打印奇数次
				{
					writer.write("\f");
				}
				else if(batchPrintRealCount!=0)
				{
					writer.write("\n\n\n\n\n");
				}
				batchPrintRealCount++;
			}
			else if(batchPrintcount>=3)//超过三次打印,略过
			{ 
				continue;
			}
			/*if(printcount==-1)//第一条记录
			{
				printcount=Integer.parseInt((String)row.get("printTimes"));//首先判断打印次数
				if(printcount==1||printcount==2)//第二次和第三次打印
				{
					//补打
					printStr=ACEPackUtil.rightStrFormat("83","补打"," ");
					printStr+="\n";
					writer.write(printStr+piaoInfo.initTaxPiaoInfo(row)+"\n\n\n\n\n");
					 
				}
				else if(printcount>=3)//超过三次打印
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
				
		
		//修改实时扣税的打印次数
		if (realTimeList.size()>0) {
			DBUtil.executeUpdate("update realTimePayMent a set a.printTimes=a.printTimes+1 where 1=1 "+sqlWhere);
		}
		//修改批量扣税的打印次数
		if (batchList.size()>0) {
			DBUtil.executeUpdate("update BatchPackDetail a set a.printTimes=a.printTimes+1 where 1=1 "+sqlWhere);
		}
		
		logger.info("消息存放于:" + f.getAbsolutePath());	
		recordPrintCount=realTimeList.size()+batchList.size();
		return ""+(realprintcount+batchPrintRealCount);
	}
	//yangyuanxu add 
	public int getStatus(String BranchNo) throws SQLException{
		int len = 0;
		logger.info("机构码是：："+BranchNo);
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
