package resoft.tips.web.action;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import resoft.basLink.Message;
import resoft.common.web.AbstractAction;
import resoft.tips.util.TransCommUtil;

/**
 * <p/> 申请对账文件重发9112
 * </p>
 * Author: chenjianping Date: 2007-8-22 Time: 15:19:58
 */
public class ApplyChkResend extends AbstractAction {
	private static final Log logger = LogFactory.getLog(ApplyChkResend.class);

	public String execute() {
		// 发送T8002报文以校验账号
		Message msg = new Message();
		msg.setValue("OriChkDate", OriChkDate);
		msg.setValue("OriChkAcctOrd", OriChkAcctOrd);
		Message returnData;
		try {
			returnData = TransCommUtil.send("9112", msg);
		} catch (IOException e) {
			setMessage("连接后台交易系统失败");
			logger.error("连接后台交易系统失败", e);
			return ERROR;
		}
		String result = returnData.getValue("Result");
		String addWord = returnData.getValue("AddWord");
		result = result.equals("Y") ? "成功" : "失败";
		setMessage("返回结果:" + result + " 附言:" + addWord);
		return SUCCESS;
	}

	private String OriChkDate;

	private String OriChkAcctOrd;

	public void setOriChkAcctOrd(String oriChkAcctOrd) {
		OriChkAcctOrd = oriChkAcctOrd;
	}

	public void setOriChkDate(String oriChkDate) {
		OriChkDate = oriChkDate;
	}
}
