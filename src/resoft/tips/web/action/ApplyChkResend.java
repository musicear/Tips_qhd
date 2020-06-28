package resoft.tips.web.action;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import resoft.basLink.Message;
import resoft.common.web.AbstractAction;
import resoft.tips.util.TransCommUtil;

/**
 * <p/> ��������ļ��ط�9112
 * </p>
 * Author: chenjianping Date: 2007-8-22 Time: 15:19:58
 */
public class ApplyChkResend extends AbstractAction {
	private static final Log logger = LogFactory.getLog(ApplyChkResend.class);

	public String execute() {
		// ����T8002������У���˺�
		Message msg = new Message();
		msg.setValue("OriChkDate", OriChkDate);
		msg.setValue("OriChkAcctOrd", OriChkAcctOrd);
		Message returnData;
		try {
			returnData = TransCommUtil.send("9112", msg);
		} catch (IOException e) {
			setMessage("���Ӻ�̨����ϵͳʧ��");
			logger.error("���Ӻ�̨����ϵͳʧ��", e);
			return ERROR;
		}
		String result = returnData.getValue("Result");
		String addWord = returnData.getValue("AddWord");
		result = result.equals("Y") ? "�ɹ�" : "ʧ��";
		setMessage("���ؽ��:" + result + " ����:" + addWord);
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