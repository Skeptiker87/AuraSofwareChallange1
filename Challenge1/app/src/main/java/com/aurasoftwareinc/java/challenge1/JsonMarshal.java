package com.aurasoftwareinc.java.challenge1;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

public class JsonMarshal
{
    public final static String TAG = JsonMarshal.class.getSimpleName();

    /** <p>Generates a {@link JSONObject} with the content of the given {@code Object}.
     *  Scans all fields and if possible writes them to JSON.
     *  If a field is of type {@code Object}, it must directly implement {@link JsonMarshalInterface}.
     *  </p>
     *  supported field types: </br>
     *  <li>primitives</li>
     *  <li>primitive wrapper objects </li>
     *  <li>arrays </li>
     *  <li>{@link String} </li>
     *  <li>{@link JSONObject} </li>
     *  <li>{@link JSONArray} </li>
     *  </br>
     *  All other objects are not supported. Public final fields and NULL objects will not considered.</br>
     *
     * @param object
     *        {@code Object} which would be converted to JSON.
     * @return
     *        converted {@link JSONObject}
     */
    public static JSONObject marshalJSON(Object object)
    {
        JSONObject json = new JSONObject();

        for (Field field : object.getClass().getDeclaredFields())
        {
            if(Modifier.isFinal(field.getModifiers()) && Modifier.isStatic(field.getModifiers()))
                continue;

            Class<?> type = field.getType();
            if(!field.isAccessible())
                field.setAccessible(true);

            try
            {
                // Primitives
                if(type.isPrimitive())
                    marshalPrimitiveToJSON(object, json, field);

                // Objects
                // If object is null, there is nothing to marshal.
                else if(field.get(object) == null)
                    continue;
                else if(type.isArray())
                    marshalArrayToJSON(object, json, field);
                else if(type == String.class)
                    marshallObjectToJSON(object, json, field);
                else if(type == JSONObject.class || type == JSONArray.class)
                    marshallObjectToJSON(object, json, field);
                else if(isPrimitiveWrapper(type))
                    marshalPrimitiveWrapperToJSON(object, json, field);
                else if(isImplementingMarshalInterface(type))
                    marshalJsonMarshalInterfaceToJSON(object, json, field);
                else
                    Log.w(TAG, "Unhandled field: " + Object.class.getSimpleName() + "." + field.getName());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return json;
    }

    /**
     * Simply puts an {@link Object} to the {@link JSONObject}.
     *
     * @param object {@link Object} which contains the field.
     * @param json The object will be put into this {@link JSONObject}.
     * @param field {@link Field} which contains the String.
     */
    private static void marshallObjectToJSON(Object object, JSONObject json, Field field)
    {
        try {
            json.put(field.getName(), field.get(object));
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Put another {@link JSONObject} to the given JSON, which contains Data from the field-object,
     * by calling its marshal-method.
     *
     * @param object {@link Object} which contains the field.
     * @param json The object will be put into this {@link JSONObject}.
     * @param field {@link Field} which contains the object which implements {@link JsonMarshalInterface}.
     */
    private static void marshalJsonMarshalInterfaceToJSON(Object object, JSONObject json, Field field)
    {
        try {
            JsonMarshalInterface marshalInterface = (JsonMarshalInterface)field.get(object);
            if(marshalInterface != null)
                json.put(field.getName(), marshalInterface.marshalJSON());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (ClassCastException e)
        {
            Log.e(TAG, "Field isn't of type JsonMarshalInterface", e);
        }
    }

    /**Checks if the given class or one of it's super classes implements {@link JsonMarshalInterface}
     *
     * @param aClass {@link Class} to check
     * @return {@code true} if aClass or a super class of aClass is implementing {@link JsonMarshalInterface}.
     *         {@code false} if not.
     */
    private static boolean isImplementingMarshalInterface(Class<?> aClass)
    {
        Class classToCheck = aClass;

        while (classToCheck != null)
        {
            Class[] interfaces = classToCheck.getInterfaces();
            for (int i = 0; i < interfaces.length; i++)
                if(interfaces[i] == JsonMarshalInterface.class)
                    return true;
            classToCheck = classToCheck.getSuperclass();
        }

        return false;
    }

    /**Creates a {@link JSONArray} with the same length as the given one,
     * put all values from the given to the JSONArray
     * and the JSONArray to the JSON.
     *
     * @param object {@link Object} which contains the field.
     * @param json The array will be put into this {@link JSONObject}.
     * @param field {@link Field} which contains the array.
     */
    private static void marshalArrayToJSON(Object object, JSONObject json, Field field)
    {
        try
        {
            Object fieldObject = field.get(object);
            if(fieldObject != null && Array.getLength(fieldObject) > 0)
            {
                JSONArray jsonArray = new JSONArray();
                for (int i = 0; i < Array.getLength(fieldObject); i++)
                    jsonArray.put(Array.get(fieldObject, i));
                json.put(field.getName(), jsonArray);
            }
        } catch (IllegalAccessException e)
        {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**Checks the type of the primitive and puts the value to the {@link JSONObject}.
     *
     * @param object {@link Object} which contains the field.
     * @param json The primitive value will be put into this {@link JSONObject}.
     * @param field {@link Field} which contains the primitive value.
     */
    private static void marshalPrimitiveToJSON(Object object, JSONObject json, Field field)
    {
        Class<?> type = field.getType();
        try
        {
            if(type == Boolean.TYPE)
                json.put(field.getName(), field.getBoolean(object));
            else if (type == Character.TYPE)
                json.put(field.getName(), field.getChar(object));
            else if (type == Byte.TYPE)
                json.put(field.getName(), field.getByte(object));
            else if (type == Short.TYPE)
                json.put(field.getName(), field.getShort(object));
            else if (type == Integer.TYPE)
                json.put(field.getName(), field.getInt(object));
            else if (type == Long.TYPE)
                json.put(field.getName(), field.getLong(object));
            else if (type == Float.TYPE)
                json.put(field.getName(), field.getFloat(object));
            else if (type == Double.TYPE)
                json.put(field.getName(), field.getDouble(object));
        } catch (JSONException e)
        {
            e.printStackTrace();
        } catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
    }

    /**Take the value from a primitive wrapper class and puts it to the {@link JSONObject} as primitive.
     *
     * @param object {@link Object} which contains the field.
     * @param json The primitive value will be put into this {@link JSONObject}.
     * @param field {@link Field} which contains the primitive wrapper object.
     */
    private static void marshalPrimitiveWrapperToJSON(Object object, JSONObject json, Field field)
    {
        Class<?> type = field.getType();
        try
        {
            if(type == Boolean.class)
                json.put(field.getName(), ((Boolean)field.get(object)).booleanValue());
            else if (type == Character.class)
                json.put(field.getName(), ((Character)field.get(object)).charValue());
            else if (type == Byte.class)
                json.put(field.getName(), ((Byte)field.get(object)).byteValue());
            else if (type == Short.class)
                json.put(field.getName(), ((Short)field.get(object)).shortValue());
            else if (type == Integer.class)
                json.put(field.getName(), ((Integer)field.get(object)).intValue());
            else if (type == Long.class)
                json.put(field.getName(), ((Long)field.get(object)).longValue());
            else if (type == Float.class)
                json.put(field.getName(), ((Float)field.get(object)).floatValue());
            else if (type == Double.class)
                json.put(field.getName(), ((Double)field.get(object)).doubleValue());
        } catch (JSONException e)
        {
            e.printStackTrace();
        } catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
    }

    /**Recreates an object by reading its fields from the given {@link JSONObject}. The JSON should be created by {@link #marshalJSON(Object)}.
     *
     * @param object {@link Object} which fields should be created/filled from the JSON.
     * @param json {@link JSONObject} to read from.
     * @return {@code false} if an unexpected error occurred.
     *
     * @see {@link #marshalJSON(Object)}
     */
    public static boolean unmarshalJSON(Object object, JSONObject json)
    {
        try {
            for (Field field : object.getClass().getDeclaredFields()) {
                if (Modifier.isFinal(field.getModifiers()) && Modifier.isStatic(field.getModifiers()))
                    continue;

                if (!json.has(field.getName()))
                    continue;

                Class<?> type = field.getType();
                if (!field.isAccessible())
                    field.setAccessible(true);

                if (type.isPrimitive())
                    unmarshalPrimitiveFromJSON(object, json, field);
                else if (type.isArray())
                    unmarshalArrayFromJSON(object, json, field);
                else if (type == String.class)
                    unmarshalStringFromJSON(object, json, field);
                else if (type == JSONObject.class || type == JSONArray.class)
                    unmarshallJSONObjectFromJSON(object, json, field);
                else if (isPrimitiveWrapper(type))
                    unmarshalPrimitiveWrapperFromJSON(object, json, field);
                else if (isImplementingMarshalInterface(type))
                    unmarshalJsonMarshalInterfaceFromJSON(object, json, field);
                else
                    Log.w(TAG, "Unhandled field: " + Object.class.getSimpleName() + "." + field.getName());
            }
            return true;
        }
        catch(Exception e)
        {
            Log.e(TAG, "An unexpected error occurred!", e);
        }
        return false;
    }

    /**Unmarshal a primitive wrapper object from the JSON by reading the corresponding primitive value.
     *
     * @param object {@link Object} which contains the field.
     * @param json The primitive value will be read from this {@link JSONObject}.
     * @param field {@link Field} which contains the primitive wrapper object.
     */
    private static void unmarshalPrimitiveWrapperFromJSON(Object object, JSONObject json, Field field)
    {
        Class<?> type = field.getType();
        try
        {
            if(type == Boolean.class)
                field.set(object, new Boolean(json.getBoolean(field.getName())));
            else if (type == Character.class)
                field.set(object, new Character((char)json.getInt(field.getName())));
            else if (type == Byte.class)
                field.set(object, new Byte((byte)json.getInt(field.getName())));
            else if (type == Short.class)
                field.set(object, new Short((short) json.getInt(field.getName())));
            else if (type == Integer.class)
                field.set(object, new Integer(json.getInt(field.getName())));
            else if (type == Long.class)
                field.set(object, new Long(json.getLong(field.getName())));
            else if (type == Float.class)
                field.set(object, new Float((float)json.getDouble(field.getName())));
            else if (type == Double.class)
                field.set(object, new Double(json.getDouble(field.getName())));
        } catch (JSONException e)
        {
            e.printStackTrace();
        } catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
    }

    /**Read a {@link JSONObject} or {@link JSONArray} and set it.
     *
     * @param object {@link Object} which contains the field.
     * @param json The object will be read from this {@link JSONObject}.
     * @param field {@link Field} which contains {@link JSONObject} or {@link JSONArray}.
     */
    private static void unmarshallJSONObjectFromJSON(Object object, JSONObject json, Field field)
    {
        try {
            if(field.getType() == JSONObject.class)
                field.set(object, json.getJSONObject(field.getName()));
            else if(field.getType() == JSONArray.class)
                field.set(object, json.getJSONArray(field.getName()));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**Read a {@link String} and set it.
     *
     * @param object {@link Object} which contains the field.
     * @param json The {@link String} will be read from this {@link JSONObject}.
     * @param field {@link Field} which contains {@link String}.
     */
    private static void unmarshalStringFromJSON(Object object, JSONObject json, Field field)
    {
        try {
            field.set(object, json.getString(field.getName()));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**Creates the field object, which must implement {@link JsonMarshalInterface} and must provide a standard constructor!
     *
     * @param object {@link Object} which contains the field.
     * @param json The {@link JsonMarshalInterface}-object will be read from this {@link JSONObject}.
     * @param field {@link Field} which contains an object which implements {@link JsonMarshalInterface}.
     */
    private static void unmarshalJsonMarshalInterfaceFromJSON(Object object, JSONObject json, Field field)
    {
        try {
            JsonMarshalInterface marshalInterfaceObject = (JsonMarshalInterface) field.getType().newInstance();
            marshalInterfaceObject.unmarshalJSON(json.getJSONObject(field.getName()));
            field.set(object, marshalInterfaceObject);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (ClassCastException e)
        {
            Log.e(TAG, "Field isn't of type JsonMarshalInterface", e);
        }
    }

    /**Reads a {@link JSONArray} from the given {@link JSONObject}.
     * Creates a new Array with the same length and type specified by the field.
     * Fills the created array with the values from the {@link JSONArray}.
     *
     * @param object {@link Object} which contains the field.
     * @param json The array will be read from this {@link JSONObject}.
     * @param field {@link Field} which contains the array.
     */
    private static void unmarshalArrayFromJSON(Object object, JSONObject json, Field field)
    {
        try
        {
            JSONArray jsonArray = json.getJSONArray(field.getName());
            Object arrayObj = Array.newInstance(field.getType().getComponentType(), jsonArray.length());

            for (int i = 0; i < jsonArray.length(); i++)
                Array.set(arrayObj, i, jsonArray.get(i));

            field.set(object, arrayObj);
        } catch (IllegalAccessException e)
        {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**Reads a primitive value from the JSON specified by the given field-type.
     *
     * @param object {@link Object} which contains the field.
     * @param json The primitive will be read from this {@link JSONObject}.
     * @param field {@link Field} which contains the primitive.
     */
    private static void unmarshalPrimitiveFromJSON(Object object, JSONObject json, Field field)
    {
        Class<?> type = field.getType();
        try
        {
            if(type == Boolean.TYPE)
                field.setBoolean(object, json.getBoolean(field.getName()));
            else if (type == Character.TYPE)
                field.setChar(object, (char)json.getInt(field.getName()));
            else if (type == Byte.TYPE)
                field.setByte(object, (byte)json.getInt(field.getName()));
            else if (type == Short.TYPE)
                field.setShort(object, (short) json.getInt(field.getName()));
            else if (type == Integer.TYPE)
                field.setInt(object, json.getInt(field.getName()));
            else if (type == Long.TYPE)
                field.setLong(object, json.getLong(field.getName()));
            else if (type == Float.TYPE)
                field.setFloat(object, (float)json.getDouble(field.getName()));
            else if (type == Double.TYPE)
                field.setDouble(object, json.getDouble(field.getName()));
        } catch (JSONException e)
        {
            e.printStackTrace();
        } catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
    }

    /**Checks if the given type represents a wrapper for primitives.
     *
     * @param type The {@link Type} to check.
     * @return {@code true} if type is a wrapper for primitive. {@code false} if not.
     */
    private static boolean isPrimitiveWrapper(Type type)
    {
        return (type == Boolean.class
                || type == Byte.class
                || type == Character.class
                || type == Short.class
                || type == Integer.class
                || type == Long.class
                || type == Float.class
                || type == Double.class);
    }
}