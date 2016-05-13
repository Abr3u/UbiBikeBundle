package pt.ulisboa.tecnico.cmov.ubibike;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

//import pt.ulisboa.tecnico.cmov.ubibike.commons.UserJsonKey;

abstract class JsonHandlerActivity extends ErrorHandlerActivity {

    protected void checkJsonKeys(JSONObject request, UserJsonKey... keys)
            throws InvalidReplyException {
        @SuppressWarnings("rawtypes")
        Iterator iterator = request.keys();
        int size;
        for (size = 0; iterator.hasNext() ; ++size ) {
            iterator.next();
        }

        if (size != keys.length + 1) {
            throw new InvalidReplyException();
        }
    }

    protected void checkJsonKey(JSONObject request, UserJsonKey key)
            throws InvalidReplyException {
        if (!request.has(key.toString())) {
            throw new InvalidReplyException();
        }
    }

    protected String getStringFromJson(JSONObject request, UserJsonKey key)
            throws InvalidReplyException, JSONException {
        checkJsonKey(request, key);
        return request.getString(key.toString());
    }

    protected double getDoubleFromJson(JSONObject request, UserJsonKey key)
            throws InvalidReplyException, JSONException {
        checkJsonKey(request, key);
        return request.getDouble(key.toString());
    }

    protected int getIntFromJson(JSONObject request, UserJsonKey key)
            throws InvalidReplyException, JSONException {
        checkJsonKey(request, key);
        return request.getInt(key.toString());
    }

    protected JSONArray getArrayFromJson(JSONObject request, UserJsonKey key)
            throws InvalidReplyException, JSONException {
        checkJsonKey(request, key);
        return request.getJSONArray(key.toString());
    }

    protected JSONObject getObjectFromJson(JSONObject request, UserJsonKey key)
            throws InvalidReplyException, JSONException {
        checkJsonKey(request, key);
        return request.getJSONObject(key.toString());
    }
}
