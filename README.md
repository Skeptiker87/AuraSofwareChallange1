# challenge.java
Android Studio / Java Challenge new Candidates

###Author
Erik Böckmann
erik.boeckmann@gmail.com

###Final (alternative)
This is an alternative solution for the java challange.
Note that the preffered solution can be found at master: ***

This solution tries to go an more universal way.

It's not documented in java file.

Major differnces:
* The JSON isn't clean. It needs to hold information for Java-Classes. So it's not used in a way, JSON is made for. Here it's just used for persisting java objects.
* JsonMarshalInterface is obsolete.
* Can handle any Java object, but the object must have a default constructor.
* With external libraries it could also hanlde java obects which not have a default constructor. But external libraries are restricted by the challange. (e.g. see https://www.javaspecialists.eu/archive/Issue175.html for a possible solution)
* It can handle Collections and Maps.
* It doesn't need special handling for JSONObject and JSONArray, cause they are treated like any other Java object.
* It also needs special handling for primitives and for String.
* It needs special handling for primitive wrapper objects, cause they haven't a default constructor.
* It currently has no support for enums. But it is possible to implement.

This version is not finished!
At some points, it needs more refactoring. For example:
* The function marshalFieldAsObject() should be merged with marshalJSON.
* Arrays currently just marshalled, if they appear as fields. They should be also handled in Collections.
* much much more...

