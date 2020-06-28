package resoft.tips.bankImpl.sxbank;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import resoft.basLink.util.UnpackFormatCache;
import resoft.xlink.comm.PackException;
import resoft.xlink.comm.Packager;
import resoft.xlink.core.Message;
import resoft.xlink.impl.DefaultMessage;

public class SXBankPackager implements Packager{
	 private static final Log logger = LogFactory.getLog(SXBankPackager.class);
	 private static final boolean ADD_CRCF = true;
	 java.util.Stack tags = new java.util.Stack();
	
	    private static SAXParserFactory spf = null;
	    
	    static {
	        //初始化SAX解析器
	        spf = SAXParserFactory.newInstance();
	    }
	    
	    public SXBankPackager() {

	    }
 
	    /**
	     * 将字节串解析为Message对象
	     */
	    public Message unpack(byte[] bytes) throws PackException {
	        SAXParser saxParser;
	        try {
	            saxParser = spf.newSAXParser();
	        } catch (Exception e) {
	        	logger.info("解析失败1");
	            throw new RuntimeException(e.getMessage(), e);
	        }
	        //SAX
	        final Message msg = new DefaultMessage();
	        try {
	            InputSource inputSource = new InputSource(new StringReader(new String(bytes, "GBK")));
	            //inputSource.setEncoding("GB2312");
	            saxParser.parse(inputSource, new PackReader(msg));
	        } catch (Exception e) {
	        	logger.info("解析失败2");
	            throw new PackException("解析失败", e);
	        }

	        return msg;
	    }
	    
	    /**
	     * SAX Pack Reader
	     */
    
	    private static class PackReader extends DefaultHandler {
	        PackReader(Message msg) {
	            this.msg = msg;
	        }

	        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
	        	if (qName.equals("HEAD")) {
	                isHead = true;
	            }
	        	if (qName.equals("MSG")){
	        		isMsg = true;
	        	}
	            if(qName.equalsIgnoreCase("body"))
	            {
	            	isBody=true;
	            }
	            if(qName.equalsIgnoreCase("field"))
	            {
	                isfield = true;
	            }
	            if(qName.equalsIgnoreCase("sys-header"))
	            { 
					isSys_header=true;
	            }
	            if (!qName.equals("CFX") && !qName.equals("HEAD") && !qName.equals("MSG") && !qName.equals("PRI")) {
	                if (isHead) {
	                    //msg.setHeadValue(qName,value);
	                    headKey = qName;
	                } 
	                if (isMsg) {
	                	msgKey = qName;
	                 
	                }
	                if(isBody&&qName.equals("data"))
	                {
	                	bodyKey=attributes.getValue(0);
	                }
	                if(isSys_header&&qName.equals("data"))
	                {
	                	Sys_headerkey=attributes.getValue(0);
	                }
	            } 
	        }
	        
	        public void characters(char ch[], int start, int length) throws SAXException {
	        
	        	if (isHead && headKey != null) {
	                String value = new String(ch, start, length);
	                msg.set("head", headKey, value);
	                logger.info("headkey is:"+headKey);
	                logger.info("headkey  is:"+value);
	                headKey = null;
	            }
	        	if (isMsg && msgKey != null){
	        		String value = new String(ch, start, length);
	        		value=value.trim();
	                msg.set("msg", msgKey, value);
	                msg.set(msgKey, value);
	                logger.info("key is:"+msgKey);
	                logger.info("  value is:"+value);
	                msgKey = null;
	        		 
	            }  
	        	if(isfield&&isBody&&bodyKey!=null)
	        	{ 
	        		String value = new String(ch, start, length);
	        		value=value.trim();
	                msg.set("msg", bodyKey, value);
	                msg.set(bodyKey, value);
	                logger.info("bodyKey           is:"+bodyKey);
	                logger.info("value is:"+value);
	                bodyKey = null;
	        	}
	        	if(isSys_header&&isfield&&Sys_headerkey!=null)
	        	{
	        		String value = new String(ch, start, length);
	        		value=value.trim();
	                msg.set("msg", Sys_headerkey, value);
	                msg.set(Sys_headerkey, value);
	                logger.info("Sys_header         is:"+Sys_headerkey);
	                logger.info("value is:"+value);
	                Sys_headerkey = null;
	        	}
	        }

	        public void endElement(String uri, String localName, String qName) throws SAXException {
	            if (qName.equals("HEAD")) {
	                isHead = false;
	            }
	            if(qName.equalsIgnoreCase("body"))
	            {
	            	isBody=false;
	            }
	            if(qName.equalsIgnoreCase("field"))
	            {
	                isfield = false;
	            }
	            if(qName.equalsIgnoreCase("sys-header"))
	            {
	            	isSys_header=false;
	            }
	            
	        }

	        private boolean isHead = false;
	        private String headKey = null;
	        private boolean isMsg = false;
	        private String msgKey = null;
	        private Message msg;
	        private boolean isBody=false;
	        private String bodyKey=null;
	        boolean isfield=false;
	        private boolean isSys_header;
	        private String Sys_headerkey=null;
	    } 

	    public byte[] pack(Message msg) {
	        if (msg.get("packFile") == null) {
	            return packWithoutFormatFile(msg);
	        } 
	        else if(msg.get("packFile") == "T5100returnsm")
	        {
	        	return ((String)msg.get("sendMsg")).getBytes();
	        }
	        else if(msg.get("packFile")=="T3100returnsm")
	        {
	        	return ((String)msg.get("sendMsg")).getBytes();
	        }else if(msg.get("packFile")=="T9117returnsm")
	        {
	        	return ((String)msg.get("sendMsg")).getBytes();
	        }
	        else {
	            return packByFile(msg,(String) msg.get("packFile"));
	        } 
	    } 
			 

		/**
	     * 没有格式定义文件
	     */
	    private byte[] packWithoutFormatFile(Message msg) {
	        //if(msg.get)
	        StringBuffer sb = new StringBuffer("<CFX>");
	        //HEAD TAG
	        if (msg.findAllKeysByCategory("head").size() > 0) {
	            sb.append("<HEAD>");
	            for (Iterator itr = msg.findAllKeysByCategory("head").iterator(); itr.hasNext();) {
	                String key = (String) itr.next();
	                if (key == null) {
	                    continue;
	                }
	                String value = (String) msg.get("head", key);
	                sb.append("<").append(key).append(">").append(value).append("</").append(key).append(">");
	            }
	            sb.append("</HEAD>");
	        }
	        //MSG TAG

	        sb.append("<MSG>");
	        for (Iterator itr = msg.findAllKeysByCategory(Message.DefaultCategory).iterator(); itr.hasNext();) {
	            String key = itr.next().toString();
	            if (key.startsWith("//") || msg.get(key) == null) {
	                continue;
	            }
	            //String value = (String) msg.get("msg", key);
	            StringBuffer value = new StringBuffer(msg.get(key).toString());
	            //special char
	            replaceAll(value, "&", "&amp;");
	            replaceAll(value, "<", "&lt;");
	            replaceAll(value, ">", "&gt;");
	            replaceAll(value, "\"", "&quot;");
	            //value.replaceAll("&","&amp;").replaceAll("<","&lt;").replaceAll(">","&gt;").replaceAll("\"","&quot;");
	            sb.append("<").append(key).append(">").append(value).append("</").append(key).append(">");
	        }
	        
	        sb.append("</MSG>");

	        sb.append("</CFX>");

	        return sb.toString().getBytes();
	    }


	    /**
	     * 替换StringBuffer中的字符串
	     */
	    private void replaceAll(StringBuffer sb, String findText, String replacement) {
	        int pos = sb.indexOf(findText);
	        if (pos >= 0) {
	            sb.replace(pos, pos + findText.length(), replacement);
	        }
	    }

	   /**
	     * 从文件中取得报文格式定义文件
	     *
	     * @param msg      Message
	     * @param filePath String   格式定义文件路径，指其在ClassPath中的相对路径
	     */
	    private byte[] packByFile(Message msg, String filePath) throws PackException {
	        //filePath = "./conf/pack/" + filePath;

	        Map formatMap = UnpackFormatCache.getInstance().getFormat(filePath);
	        if (formatMap == null) {
	            throw new PackException();
	        }
	        StringBuffer xml = new StringBuffer();
	        xml.append("<CFX>");
	        //HEAD段
	        List headList = (List) formatMap.get("head");
	        if (headList.size() > 0) {
	            xml.append("<HEAD>");
	            for (Iterator itr = headList.iterator(); itr.hasNext();) {
	                String key = (String) itr.next();
	                String value = (String) msg.get("head", key);
	                if (value != null && !value.equals("")) {
	                    //格式类似<SRC>1000</SRC>
	                    xml.append("<").append(key).append(">").append(value).append("</").append(key).append(">");
	                }
	            }
	            xml.append("</HEAD>");
	        }
	        //headList;
	        //MSG段
	        List msgList = (List) formatMap.get("msg");
	        if (msgList.size() > 0) {
	            xml.append("<MSG>");
	            for (Iterator itr = msgList.iterator(); itr.hasNext();) {
	                String key = (String) itr.next();
	                String value = (String) msg.get(key);
	                if (value != null && !value.equals("")) {
	                    //格式类似<纳税人编码 val="34234"/>
	                    xml.append("<").append(key).append(" val=\"").append(value).append("\"/>");
	                }
	            }
	            xml.append("</MSG>");
	        }
	        //msgList = null;
	        //PRI段
	        List priList = (List) formatMap.get("pri");
	        if (priList.size() > 0) {
	            xml.append("<PRI>");
	            for (Iterator itr = priList.iterator(); itr.hasNext();) {
	                String key = (String) itr.next();
	                String value = (String) msg.get(key);
	                if (value != null && !value.equals("")) {
	                    //格式类似<纳税人编码 val="34234"/>
	                    xml.append("<").append(key).append(" val=\"").append(value).append("\"/>");
	                }
	            }
	            xml.append("</PRI>");
	        }
	        xml.append("</CFX>");
	        if (ADD_CRCF) {
	            xml.append("\r\n");
	        }

	        try {
	            return xml.toString().getBytes("GBK");
	        } catch (UnsupportedEncodingException e) {
	            return new byte[]{};
	        }

	    }
}
