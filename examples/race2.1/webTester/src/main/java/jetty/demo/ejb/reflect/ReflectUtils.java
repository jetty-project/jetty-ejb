package jetty.demo.ejb.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Logger;

public final class ReflectUtils
{
    private static final Logger LOG = Logger.getLogger(ReflectUtils.class.getName());

    public static Class findOptionalClass(String classname)
    {
        try
        {
            return Class.forName(classname);
        }
        catch (ClassNotFoundException e)
        {
            LOG.fine("Optional Class Not Found: " + classname);
            return null;
        }
    }

    public static boolean isInstanceOf(Object obj, Class optionalClass)
    {
        if ((obj == null) || (optionalClass == null))
        {
            return false;
        }
        return optionalClass.isAssignableFrom(obj.getClass());
    }

    public static Object invokeMethod(Object obj, String methodName, Object[] args) throws ReflectiveOperationException
    {
        Class params[] = new Class[args.length];
        for (int i = 0; i < args.length; i++)
        {
            if (args[i] == null)
            {
                params[i] = null;
            }
            else
            {
                params[i] = args[i].getClass();
            }
        }
        return invokeMethod(obj,methodName,params,args);
    }

    public static Object invokeMethod(Object obj, String methodName, Class[] params, Object[] args) throws ReflectiveOperationException
    {
        Method method = obj.getClass().getMethod(methodName,params);
        return method.invoke(obj,args);
    }

    public static Object getField(Object obj, String fieldName) throws ReflectiveOperationException
    {
        Field field = obj.getClass().getField(fieldName);
        return field.get(obj);
    }
}
