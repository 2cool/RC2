package copter.rc2;

import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;

/**
 * Created by 2cool on 06.06.2015.
 */
public class Disk {

    private static FileOutputStream fos=null;
    private static File myFile=null;
    private static String filename=null;


    public static String getIP(String myIP){
        try {

            String ip=myIP.substring(0,myIP.lastIndexOf('.'));
            final File file = new File("/sdcard/RC/ip.set");
            if (!file.exists()) {
               return null;
            }

            InputStream is=new FileInputStream(file);
            BufferedReader buf = new BufferedReader(new InputStreamReader(is));
            String line;
            int n;
            do {
                line = buf.readLine();
                n = line.lastIndexOf(ip);
                if (n==0)
                    break;
            }while(line!=null && line.length()>10);
            buf.close();
            is.close();


            return line;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    public static int createOrOpen(){
        try {

            {
                final File file = new File("/sdcard/RC");
                if (!file.exists()) {
                    if (file.mkdir()) {
                        //System.out.println("Directory is created!");
                    } else {
                        System.out.println("Failed to create directory!");
                    }
                }
            }

            Calendar c = Calendar.getInstance();
            filename = ""+c.getTime();
            filename=filename.replace(':','_').replace(' ','_');
            filename="RC/"+filename+".log";

            if (myFile==null) {
                myFile = new File(Environment.getExternalStorageDirectory(), filename);
                if (!myFile.exists())
                    myFile.createNewFile();
                    fos = new FileOutputStream(myFile , false);

            }


        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        }
        return 0;
    }

    public static int close(){
        try {

            Log.i("MSGG", "disk close");


            if (fos!=null) {
                fos.flush();
                fos.close();
                if (myFile!=null && myFile.exists() && (Telemetry.motorsWasOn==false ||  myFile.length()==0)){
                    myFile.delete();
                }
            }
            fos=null;
            myFile=null;
        }catch (Exception e){
            Log.i("MSGG", "exception");
            e.printStackTrace();
            return 1;
        }
        return 0;
    }
    public static int write(String string) {
        if (fos==null)
           // if (createOrOpen()!=0)
                return 1;

            byte[] data = string.getBytes();
            try {

                fos.write(data);

            } catch (Exception e) {
                e.printStackTrace();
                return 1;
            }


        return 0;
    }









}
