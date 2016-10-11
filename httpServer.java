import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class httpServer extends Thread
{
    private final static int PORT = 80;
    private final static String FILEPATH = System.getProperty("user.dir") + File.separator + "src" + File.separator;

    public static void main(String args[]) throws Exception
    {
        ServerSocket serverSocket = new ServerSocket(PORT);

        //noinspection InfiniteLoopStatement
        while(true)
        {
            Socket connectionSocket = serverSocket.accept();

            new Thread(new clientConnection(connectionSocket)).start();
        }
    }

    private static class clientConnection implements Runnable
    {
        protected Socket socket = null;
        BufferedReader in;
        DataOutputStream out;
        String inString;

        public clientConnection(Socket connectionSocket) throws Exception
        {
            socket = connectionSocket;
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new DataOutputStream(socket.getOutputStream());
            inString = in.readLine();

            int b;
            StringBuilder buf = new StringBuilder();
            while ((b = in.read()) != -1)
            {
                buf.append((char) b);
            }

            System.out.println(buf);

            Calendar cal = Calendar.getInstance();
            cal.getTime();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
            String time = "[" + simpleDateFormat.format(cal.getTime()) + "] ";
            System.out.print(time + inString + "\r\n");
        }

        public void run()
        {
            try
            {
                if(inString != null)
                {
                    respondContent(inString, out);
                }

                out.flush();
                out.close();
                in.close();
            }
            catch (Exception e)
            {
                System.out.println("Error flushing and closing");
            }
        }
    }

    private static void respondContent(String inString, DataOutputStream out) throws Exception
    {
        String method = inString.substring(0, inString.indexOf("/") - 1);

        switch (method)
        {
            case "GET":
                if(inString.contains("calc")) { getCalculatorResponse(inString, out); }
                else { getWebsiteResponse(inString, out); }
                break;
            case "POST":
                if(inString.contains("calc")) {  }
                System.out.println("Made it to the POST case.");
                break;
            case "HEAD":
                respondHeader("200", "html", 0, out);
                break;
            default:
                respondHeader("501", "html", 0, out);
                break;
        }
    }

    private static void getCalculatorResponse(String inString, DataOutputStream out) throws Exception
    {
        Calculator calculator = new Calculator();

        String method = inString.substring(10, inString.indexOf("?"));

        String xVariable = inString.substring(inString.indexOf("=") + 1, inString.indexOf("&"));
        String yVariable = inString.substring(inString.lastIndexOf("=") + 1, inString.lastIndexOf("/") - 5);
        String response;

        try
        {
            Double x = Double.parseDouble(xVariable);
            Double y = Double.parseDouble(yVariable);
            Double answer = 0.0;

            boolean methodExists = true;

            switch(method)
            {
                case "add":
                    answer = calculator.add(x, y);
                    break;
                case "subtract":
                    answer = calculator.subtract(x, y);
                    break;
                case "divide":
                    answer = calculator.divide(x, y);
                    break;
                case "multiply":
                    answer = calculator.multiply(x, y);
                    break;
                default:
                    methodExists = false;
            }

            if(methodExists)
            {
                response = "The answer is: " + answer;
                respondHeader("200", "html", response.length(), out);
                out.write(response.getBytes());
            }
            else
            {
                response = "The method " + method + " does not exist on this calculator";
                respondHeader("404", "html", response.length(), out);
                out.write(response.getBytes());
            }
        }
        catch(NumberFormatException nfe)
        {
            response = "Internal Server Error";
            respondHeader("500", "html", response.length(), out);
            out.write(response.getBytes());
        }
    }

    private static void getWebsiteResponse(String inString, DataOutputStream out) throws Exception
    {
        String file = inString.substring(inString.indexOf("/") + 1, inString.lastIndexOf("/") - 5);

        if(file.equals(""))
        {
            file = "index.html";
        }

        String mime = file.substring(file.indexOf(".") + 1);
        try
        {
            byte[] fileBytes;
            InputStream is = new FileInputStream(FILEPATH + file);
            fileBytes = new byte[is.available()];

            //noinspection ResultOfMethodCallIgnored
            is.read(fileBytes);

            respondHeader("200", mime, fileBytes.length, out);

            out.write(fileBytes);
        }
        catch (FileNotFoundException e)
        {
            String responseString = "404 File Not Found";
            respondHeader("404", "html", responseString.length(), out);
            out.write(responseString.getBytes());
        }
    }

    private static void respondHeader(String code, String mime, int length, DataOutputStream out) throws Exception
    {
        out.writeBytes("HTTP/1.0 " + code + " OK\r\n");
        out.writeBytes("Content-Type: " + mimeMap.get(mime) + "\r\n");
        out.writeBytes("Content-Length: " + length + "\r\n");
        out.writeBytes("\r\n");
    }

    private static final Map<String, String> mimeMap = new HashMap<String, String>()
    {
        {
            put("html", "text/html");
            put("css", "text/css");
            put("js", "application/js");
            put("jpg", "image/jpg");
            put("jpeg", "image/jpeg");
            put("png", "image/png");
        }
    };
}