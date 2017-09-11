package com.rest.tests.api.frwm.settings;

import com.rest.tests.api.frwm.testcase.Testcase;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by msolosh on 4/7/2016.
 */
public class Tools {

    public static String printFixLineString(String text, String pattern){

//        System.out.println();
        int size = text.length();
        int p = (100 - size - 2);
        String res = "";
        if (p > 0)
        {
            String patt = new String(new char[p]).replace('\0', pattern.toCharArray()[0]);
            res = patt.substring(0, p/2) + " " + text + " " + patt.substring(p/2, patt.length());
        }
        else
        {
            res = text;
        }
        return res;
    }

    public static String PrintHeaders(HttpResponse response, Settings api) {
        String res = printFixLineString("RESPONSE HEADERs", "=") + "\n";
        List<String> list = api.getHeadersForLogging();
        if (list != null) {
            for (String item : list)
            {
                try {

                    Header head = response.getHeaders(item)[0];
                    res = res + head.toString() + "\n";
                } catch (Exception e) {
                    res = res + "There is no headers with name: " + item + "\n";
                }
            }
        }
        return res;
    }

    public static Testcase replaceVaraibles(String file, Testcase test, HashMap variables) throws Exception, VException {
        int ind = test.getNAME().lastIndexOf(":");
        file = file.substring(0, ind);

        Tools.writeToFile(file, "Test: " + test.getNAME() + "\n");
        test.setURL(replaceVariables(test.getURL(), file, variables));
        test.setBODY(replaceVariables(test.getBODY(), file, variables));
        test.setPARAMS(replaceVariables(test.getPARAMS(), file, variables));
        test.setBOUNDARY(replaceVariables(test.getBOUNDARY(), file, variables));
        test.setSourceFile(replaceVariables(test.getSourceFile(), file, variables));

        Tools.writeToFile(file, printFixLineString(test.getNAME(), "-") + "\n");

        String res = String.format("URL: %s\n" +
                        "METHOD: %s\n" +
                        "BODY: %s\n" +
                        "PARAMS: %s\n",

                test.getURL(),
                test.getMETHOD(),
                test.getBODY(),
                test.getPARAMS()
                );

        if (test.getBOUNDARY() != null)
            res = res + "BOUNDARY: " + test.getBOUNDARY().toJSONString() + "\n";

        writeToFile(file, res);
        return test;
    }

    public static String replaceVariables(String expect, String name, HashMap<String, HashMap<String, String>> variables) throws VException {
        String pattern1 = "(%[\\w]*)";
        Pattern p = Pattern.compile(pattern1);


        HashMap<String, String> map = variables.get(name);
        ArrayList<HashMap<String, String>> list = new ArrayList<>();
        if (map != null)
            list.add(map);
        list.add(variables.get("all"));
        if (expect != null)
        {
            try {
                Matcher m = p.matcher(expect);

                while (m.find()) {
                    String variable = m.group();

                    int k = 0;
                    for(HashMap<String, String> lst : list) {
                            String value = lst.get(variable.replace("%",""));
                            if (value == null) {
                                k = k + 1;
                                continue;
                            }
                            expect = expect.replaceAll(variable, value);
                            break;

                    }
                }
            } catch (Exception e) {
//                System.out.println();
            }
        }


        if (expect != null && expect.contains("%") && !expect.startsWith("REGEX"))
        {
            Tools.writeToFile(name, expect);
            throw new VException(name + ":" + expect);
        }
        return expect;
    }

    public static JSONObject replaceVariables(JSONObject expect, String name, HashMap<String, HashMap<String, String>> variables) throws Exception, VException {

        if (expect != null) {
            String exp = expect.toJSONString();
            exp = replaceVariables(exp, name, variables);
            JSONParser parser = new JSONParser();
            expect = (JSONObject) parser.parse(exp);
        }
        return expect;
    }

    public static Comparator<Testcase> ALPHABETICAL_ORDER = new Comparator<Testcase>() {
        @Override
        public int compare(Testcase o1, Testcase o2) {
            int res = String.CASE_INSENSITIVE_ORDER.compare(o1.getNAME(), o2.getNAME());
            if (res == 0) {
                res = o1.getNAME().compareTo(o2.getNAME());
            }
            return res;
        }
    };

    public static Comparator<File> FILE_ALPHABETICAL_ORDER = new Comparator<File>() {
        @Override
        public int compare(File f1, File f2) {
            int res = String.CASE_INSENSITIVE_ORDER.compare(f1.getName(), f2.getName());
            if (res == 0) {
                res = f1.getName().compareTo(f2.getName());
            }
            return res;
        }
    };

/***    public static JSONObject replaceVariables(JSONObject expect, String name, HashMap<String, HashMap<String, String>> variables)
    {
        if (expect == null)
            return null;
        String pattern1 = "(%[\\w]*)";
        Pattern p = Pattern.compile(pattern1);

        HashMap<String, String> map = variables.get(name);
        ArrayList<HashMap<String, String>> list = new ArrayList<>();
        if (map != null)
            list.add(map);
        list.add(variables.get("all"));


        for(Iterator iter = expect.keySet().iterator(); iter.hasNext();) {
            String key = (String) iter.next();
            String value = "";
            if (expect.get(key) instanceof String)
            {
                value = (String)expect.get(key);
            }
            else if (expect.get(key) instanceof JSONArray)
            {
                JSONArray array = (JSONArray)expect.get(key);
                value = array.toString();
            }

            if (!key.equalsIgnoreCase("type"))
            {
                try {
                    Matcher m = p.matcher(value);

                    while (m.find()) {
                        String variable = m.group();

                        for (HashMap<String, String> lst : list) {
                            Set set;
                            if (lst != null) {
                                set = lst.entrySet();
                                Iterator iterator = set.iterator();
                                while(iterator.hasNext()) {
                                    Map.Entry mentry = (Map.Entry)iterator.next();
                                    String var = mentry.getKey().toString().toLowerCase();
                                    if (var.equals(variable.substring(1).toLowerCase()))
                                    {
                                        value = value.replace(variable, (CharSequence) mentry.getValue());
                                        expect.put(key, value);
                                        break;
                                    }
                                }
                            }
                            else
                                break;
                        }
                    }
                } catch (Exception e) {}
            }
        }

        return expect;
    }
***/
    public static String generateVariables(String expect)
    {
        String pattern = "\\{(.*?)\\}";
        Pattern p = Pattern.compile(pattern);

        try {
            Matcher m = p.matcher(expect);

            while (m.find()) {
                String variable = m.group();


                    if (variable.toLowerCase().equals("{guid}"))
                    {
                        return expect.replace(variable, UUID.randomUUID().toString().replace("-", ""));
                    }
                    else if (variable.toLowerCase().equals("{emailh}"))
                    {
                        return expect.replace(variable, UUID.randomUUID().toString().replace("-", "").substring(0, 25));
                    }
                    else if (variable.toLowerCase().startsWith("{date("))
                    {
                        String format = variable.substring(6, variable.length() - 2);
                        String timeStamp = new SimpleDateFormat(format).format(new Date());

                        return timeStamp;
                    }
                }
        } catch (Exception e) {}

        return expect;
    }

    public static void writeToFile(String file, String text)
    {
        writeToFile(file, text, false);
    }

    public static void writeToFile(String file, String text, boolean date)
    {
        BufferedWriter bw = null;
        FileWriter fw = null;
        File fl = new File(file + ".log");
        try {
        // if file doesnt exists, then create it
        if (!fl.exists()) {
            fl.createNewFile();
        }

        // true = append file
        fw = new FileWriter(fl.getAbsoluteFile(), true);
        bw = new BufferedWriter(fw);
        String timeStamp = "";
        if (date)
        {
            timeStamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(new Date()) + " ";
        }

        bw.write(timeStamp + text);

    } catch (IOException e) {

        e.printStackTrace();

    } finally {

        try {

            if (bw != null)
                bw.close();

            if (fw != null)
                fw.close();

        } catch (IOException ex) {

            ex.printStackTrace();

        }
    }
    }

    public static String readFile(String file) {

        try
        {
            String fileString = new String(Files.readAllBytes(Paths.get(file)));
            return fileString;
        }
        catch (IOException e)
        {
            return "There is no file " + file;
        }
    }
}
