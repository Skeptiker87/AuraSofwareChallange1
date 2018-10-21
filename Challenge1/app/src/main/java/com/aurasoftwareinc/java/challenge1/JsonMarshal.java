package com.aurasoftwareinc.java.challenge1;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

public class JsonMarshal
{
    public final static String TAG = JsonMarshal.class.getSimpleName();

    public static JSONObject marshalJSON(Object object)
    {
        try
        {
            JSONObject json = new JSONObject();
            Class<?> objectClass = object.getClass();
            json.put("class", objectClass.getName());

            if(object instanceof String)
            {
                json.put("value", (String) object);
                return json;
            }

            for (Field field : objectClass.getDeclaredFields())
            {
                Class<?> type = field.getType();
                if(Modifier.isFinal(field.getModifiers()) && Modifier.isStatic(field.getModifiers()))
                    continue;
                if(!field.isAccessible())
                    field.setAccessible(true);
                if(type.isPrimitive())
                    marshalPrimitiveToJSON(object, json, field, type);
                else if(type.isArray())
                    marshalArrayToJSON(object, json, field);
                else if(type.isEnum())
                {
                }
                else
                    marshalFieldAsObject(object, json, field);
            }
            return json;
        } catch (JSONException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    private static void marshalArrayToJSON(Object object, JSONObject json, Field field) throws JSONException
    {
        try
        {
            Object fieldObject = field.get(object);
            if(fieldObject != null && Array.getLength(fieldObject) > 0)
            {
                JSONObject arrayJSONObj = new JSONObject();
                JSONArray jsonArray = new JSONArray();
                int length = Array.getLength(fieldObject);

                Class arrayClass = fieldObject.getClass();
                arrayJSONObj.put("class", arrayClass.getName());
                arrayJSONObj.put("length", length);
                System.out.println(arrayClass.getName());
                for (int i = 0; i < length; i++)
                {
                    jsonArray.put(Array.get(fieldObject, i));
                }
                arrayJSONObj.put("values", jsonArray);
                json.put(field.getName(), arrayJSONObj);
            }
        } catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
    }

    private static void marshalFieldAsObject(Object object, JSONObject json, Field field)
    {
        try
        {
            Object fieldObject = field.get(object);
            if(fieldObject == null)
                return;
//            if(fieldObject instanceof String)
//                json.put(field.getName(), fieldObject);
            else if(fieldObject instanceof Collection)
            {
                JSONObject collectionJson = new JSONObject();
                collectionJson.put("class", fieldObject.getClass().getName());
                System.out.println("Collection: " + fieldObject.toString());
                Collection collection = (Collection)fieldObject;
                JSONArray jsonArray = new JSONArray();
                for (Object collObject: collection)
                {
                    jsonArray.put(marshalJSON(collObject));
                }
                collectionJson.put("values", jsonArray);
                json.put(field.getName(), collectionJson);
            }
            else if(fieldObject instanceof Map)
            {
                JSONObject mapJson = new JSONObject();
                mapJson.put("class", fieldObject.getClass().getName());
                System.out.println("Map: " + fieldObject.toString());
                Map<?,?> map = (Map)fieldObject;
                JSONArray jsonArray = new JSONArray();
                for (Map.Entry<?,?> mapEntry : map.entrySet())
                {
                    JSONObject subJson = new JSONObject();
                    subJson.put("key", marshalJSON(mapEntry.getKey()));
                    subJson.put("value", marshalJSON(mapEntry.getValue()));
                    jsonArray.put(subJson);
                }
                mapJson.put("values", jsonArray);
                json.put(field.getName(), mapJson);
            }
            else
            {
                json.put(field.getName(), marshalJSON(fieldObject));
            }
        } catch (IllegalAccessException e)
        {
            e.printStackTrace();
        } catch (JSONException e)
        {
            e.printStackTrace();
        }
    }

    private static void marshalPrimitiveToJSON(Object object, JSONObject json, Field field, Class<?> type)
    {
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

    public static boolean unmarshalJSON(Object object, JSONObject json)
    {
        if(object == null || object.getClass() == String.class || isPrimitiveWrapper(object.getClass()))
            return false;

        try
        {
            String className = json.getString("class");
            if(!className.equals(object.getClass().getName()))
            {
                Log.e(TAG, "Object and JSON don't match! " + object.getClass().getName() + " - " + className);
                return false;
            }

            if(object instanceof Collection)
                unmarshalCollectionFromJSON((Collection) object, json);

            else if(object instanceof Map)
                unmarshalMapFromJSON((Map) object, json);

            else
            {
                Class<?> objectClass = object.getClass();
                for (Field field : objectClass.getDeclaredFields())
                {
                    if(Modifier.isFinal(field.getModifiers()) && Modifier.isStatic(field.getModifiers()))
                        continue;

                    if(!json.has(field.getName()))
                    {
                        Log.w(TAG, "Can't find " + objectClass.getSimpleName() + "." + field.getName() + " in json!");
                        continue;
                    }

                    if(!field.isAccessible())
                        field.setAccessible(true);

                    Class<?> type = field.getType();
                    if(type.isPrimitive())
                        unmarshalPrimitiveFromJSON(object, json, field, type);

                    else if(type.isArray())
                        unmarshalArrayFromJSON(object, json, field);

                    else if(type.isEnum())
                    {
                    }
                    else
                    {
                        JSONObject fieldJson = json.getJSONObject(field.getName());
                        try
                        {
                            if(field.get(object) == null)
                                field.set(object, createObjectFromJSON(fieldJson));
                            unmarshalJSON(field.get(object), fieldJson);
                        } catch (IllegalAccessException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
            }
            return true;
        } catch (JSONException e)
        {
            e.printStackTrace();
        }
        return false;
    }

    private static void unmarshalArrayFromJSON(Object object, JSONObject json, Field field) throws JSONException
    {
        try
        {
            JSONObject arrayJSONObj = json.getJSONObject(field.getName());
            Class arrayClass = Class.forName(arrayJSONObj.getString("class"));
            int length = arrayJSONObj.getInt("length");
            Object arrayObj = Array.newInstance(arrayClass.getComponentType(), length);
            JSONArray jsonArray = arrayJSONObj.getJSONArray("values");
            for (int i = 0; i < length; i++)
            {
//                                if(arrayClass == Byte.class)
//                                    Array.set(arrayObj, i, (byte)jsonArray.getInt(i));
//                                else
                Array.set(arrayObj, i, jsonArray.get(i));
            }
            field.set(object, arrayObj);
        } catch (ClassNotFoundException e)
        {
            e.printStackTrace();
        } catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
    }

    private static void unmarshalPrimitiveFromJSON(Object object, JSONObject json, Field field, Class<?> type)
    {
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

    private static Object createObjectFromJSON(JSONObject json)
    {
        try
        {
            Class oClass = Class.forName(json.getString("class"));
            if(oClass == String.class)
                return json.getString("value");
            if(isPrimitiveWrapper(oClass))
            {
                if (!json.has("value"))
                    return null;
                Object primitiveWrapperObject = createPrimitiveWrapper(oClass, json);
                return primitiveWrapperObject;
            }
            else
                try
                {
                    return oClass.newInstance();
                } catch (InstantiationException e){e.printStackTrace();
                }
        } catch (ClassNotFoundException e){e.printStackTrace();
        } catch (IllegalAccessException e){e.printStackTrace();
        } catch (JSONException e){e.printStackTrace();
        }
        return null;
    }

    private static void unmarshalCollectionFromJSON(Collection collection, JSONObject json)
    {
        try
        {
            JSONArray jsonArray = json.getJSONArray("values");
            for (int i = 0; i < jsonArray.length(); i++)
            {
                JSONObject iJson = jsonArray.getJSONObject(i);
                Object iObject = createObjectFromJSON(iJson);
                unmarshalJSON(iObject, iJson);
                collection.add(iObject);
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
    }

    private static void unmarshalMapFromJSON(Map map, JSONObject json)
    {
        try
        {
            JSONArray jsonArray = json.getJSONArray("values");
            for (int i = 0; i < jsonArray.length(); i++)
            {
                JSONObject iJSON = jsonArray.getJSONObject(i);
                JSONObject keyJSON = iJSON.getJSONObject("key");
                Object keyObject = createObjectFromJSON(keyJSON);
                unmarshalJSON(keyObject, keyJSON);

                JSONObject valueJSON = iJSON.getJSONObject("value");
                Object valueObject = createObjectFromJSON(valueJSON);
                unmarshalJSON(valueObject, valueJSON);

                map.put(keyObject, valueObject);
            }
        } catch (JSONException e)
        {
            e.printStackTrace();
        }
    }

    private static Object createPrimitiveWrapper(Type type, JSONObject json)
    {
        try
        {
            if(type == Boolean.class)
                return json.getBoolean("value");
            else if (type == Character.class)
                return (char) json.getInt("value");
            else if (type == Byte.class)
                return (byte) json.getInt("value");
            else if (type == Short.class)
                return (short) json.getInt("value");
            else if (type == Integer.class)
                return json.getInt("value");
            else if (type == Long.class)
                return json.getLong("value");
            else if (type == Float.class)
                return (float) json.getDouble("value");
            else if (type == Double.class)
                return json.getDouble("value");
        } catch (JSONException e)
        {
            e.printStackTrace();
        }
        return null;
    }

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