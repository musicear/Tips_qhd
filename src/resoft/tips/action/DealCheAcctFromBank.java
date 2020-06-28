package resoft.tips.action;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.StringTokenizer;

public class DealCheAcctFromBank implements CheckInfoReader{
    private String Line=null;
    private FileReader fr = null;
    private BufferedReader br = null;
    private HashMap lineMap=null;
    public boolean next(){//�ж��Ƿ������һ��
    	if(Line!=null){
            lineMap=new HashMap();
            this.putValues(lineMap,Line);
            try {
              Line = br.readLine();
            }
            catch (IOException ex) {
            	return false;
            }
            return true;   		
        }else{
            try{
              br.close(); //�ر�BufferedReader����
              fr.close(); //�ر��ļ�
            }catch (IOException ex) {}
            return false;
          }
    }
    /**
     * �õ����е�ָ����¼
     * */
    public String getString(String key){
    	return (String)lineMap.get(key);
    }

    //��ȡ�ļ�
    public void setFilePath(String filename) throws IOException{
		try {
			fr = new FileReader(filename);//����FileReader���󣬲�ʵ����Ϊfr 
		    br=new BufferedReader(fr);//����BufferedReader���󣬲�ʵ����Ϊbr 
		    Line = br.readLine();//���ļ��м�����ȡһ������ 
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}    	

    }
    private void putValues(HashMap lineMap,String line){//��ÿ�еļ�¼������HashMap��
    	StringTokenizer tokens=new StringTokenizer(line.trim(),"|");
    		lineMap.put("TraDate", tokens.nextToken());
    		lineMap.put("TraTime", tokens.nextToken());
    		lineMap.put("TransNo", tokens.nextToken());
    		lineMap.put("TaxOrgCode", tokens.nextToken());
    		lineMap.put("TaxPayCode", tokens.nextToken());
    		lineMap.put("DebitAcct", tokens.nextToken());
    		lineMap.put("TraAmt", tokens.nextToken());
    }
}