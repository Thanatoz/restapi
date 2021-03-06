package com.rest.tests.api.frmw.response.looking.json;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.rest.tests.api.frmw.response.looking.ILookingObject;
import net.minidev.json.JSONArray;
import org.json.simple.JSONObject;

/**
 * Created by msolosh on 3/29/2016.
 */
public class LookingFactory{

    public static ILookingObject getLookingNode(Object doc, String xpath) {

        Object document = Configuration.defaultConfiguration().jsonProvider().parse((String)doc);
        try {
            JsonPath.read(document, xpath);
        } catch (Exception e) {
            return null;
        }

        if (JsonPath.read(document, xpath) instanceof JSONArray) {
            return new LookingForArray(document, xpath);
        }
        else if (JsonPath.read(document, xpath) instanceof JSONObject)
        {
            return new LookingForObject(document, xpath);
        }
        else if (JsonPath.read(document, xpath) instanceof Integer)
        {
            return new LookingForInteger(document, xpath);
        }
        else if (JsonPath.read(document, xpath) instanceof String)
        {
            return new LookingForString(document, xpath);
        }
        else if (JsonPath.read(document, xpath) instanceof Boolean)
        {
            return new LookingForBoolean(document, xpath);
        }
        else
        {
            return null;
        }
    }
}
