package org.eclipse.jetty.openejb.webapp;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.collections.CollectionUtils;
import org.apache.openejb.util.CollectionsUtil;
import org.apache.xbean.finder.Annotated;
import org.apache.xbean.finder.IAnnotationFinder;
import org.eclipse.jetty.annotations.AbstractDiscoverableAnnotationHandler;
import org.eclipse.jetty.annotations.AnnotationParser.ClassInfo;
import org.eclipse.jetty.annotations.AnnotationParser.FieldInfo;
import org.eclipse.jetty.annotations.AnnotationParser.MethodInfo;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.webapp.WebAppContext;

public class JettyWebAppFinder extends AbstractDiscoverableAnnotationHandler implements IAnnotationFinder
{
    private static final Logger LOG = Log.getLogger(JettyWebAppFinder.class);
    private static final Set<String> UNHANDLED_ANNOTATIONS;

    static
    {
        UNHANDLED_ANNOTATIONS = new HashSet<String>();
        UNHANDLED_ANNOTATIONS.add("java.lang.annotation.Target");
        UNHANDLED_ANNOTATIONS.add("java.lang.annotation.Retention");
        UNHANDLED_ANNOTATIONS.add("java.lang.annotation.Documented");
        UNHANDLED_ANNOTATIONS.add("java.lang.annotation.Inherited");
        UNHANDLED_ANNOTATIONS.add("java.lang.SuppressWarnings");
    }

    private final ClassLoader contextClassLoader;
    private final ConcurrentHashMap<String, ConcurrentHashSet<String>> classMap = new ConcurrentHashMap<>();
    private final Map<String, List<FieldInfo>> annotatedFields = new HashMap<>();
    private final Map<String, List<MethodInfo>> annotatedMethods = new HashMap<>();
    private final Map<String, List<ClassInfo>> annotatedClasses = new HashMap<>();
    private final List<String> classesNotLoaded = new ArrayList<>();
    private ArrayList<Class<?>> allClasses;
    private final List<Annotated<Method>> metaAnnotatedMethods = Collections.emptyList();
    private final List<Annotated<Field>> metaAnnotatedFields = Collections.emptyList();
    private final List<Annotated<Class<?>>> metaAnnotatedClasses = Collections.emptyList();

    public JettyWebAppFinder(WebAppContext context)
    {
        super(context);
        contextClassLoader = context.getClassLoader();
    }

    private void addToInheritanceMap(String interfaceOrSuperClassName, String implementingOrExtendingClassName)
    {
        // As it is likely that the interfaceOrSuperClassName is already in the map, try getting it first
        ConcurrentHashSet<String> implementingClasses = classMap.get(interfaceOrSuperClassName);
        // If it isn't in the map, then add it in, but test to make sure that someone else didn't get in
        // first and add it
        if (implementingClasses == null)
        {
            implementingClasses = new ConcurrentHashSet<String>();
            ConcurrentHashSet<String> tmp = classMap.putIfAbsent(interfaceOrSuperClassName,implementingClasses);
            if (tmp != null)
            {
                implementingClasses = tmp;
            }
        }

        implementingClasses.add(implementingOrExtendingClassName);
    }

    /**
     * OpenEJB Annotation Information Access
     */
    @Override
    public List<Class<?>> findAnnotatedClasses(Class<? extends Annotation> annotation)
    {
        LOG.debug("findAnnotatedClasses({})",annotation);
        classesNotLoaded.clear();
        String key = annotation.getName();
        List<ClassInfo> hits = annotatedClasses.get(key);
        List<Class<?>> ret = new ArrayList<>();
        if (hits != null)
        {
            for (ClassInfo info : hits)
            {
                String className = info.getClassName();
                try
                {
                    Class<?> clazz = loadClassNoInit(className);
                    if (clazz.isAnnotationPresent(annotation))
                    {
                        ret.add(clazz);
                    }
                    else
                    {
                        LOG.warn("{} was scanned as having @{} but failed .isAnnotationPresent() test",clazz,annotation.getName());
                    }
                }
                catch (ClassNotFoundException e)
                {
                    classesNotLoaded.add(className);
                }
            }
        }
        return ret;
    }

    /**
     * OpenEJB Annotation Information Access
     */
    @SuppressWarnings("rawtypes")
    @Override
    public List<Constructor> findAnnotatedConstructors(Class<? extends Annotation> annotation)
    {
        LOG.debug("findAnnotatedConstructors({})",annotation);
        classesNotLoaded.clear();
        List<Class<?>> seenClasses = new ArrayList<>();
        List<Constructor> ret = new ArrayList<>();
        String key = annotation.getName();
        List<MethodInfo> hits = annotatedMethods.get(key);
        if (hits != null)
        {
            for (MethodInfo info : hits)
            {
                String className = info.getClassInfo().getClassName();
                try
                {
                    Class<?> clazz = loadClassNoInit(className);
                    if (seenClasses.contains(clazz))
                    {
                        continue;
                    }
                    seenClasses.add(clazz);
                    for (Constructor ctor : clazz.getConstructors())
                    {
                        if (ctor.isAnnotationPresent(annotation))
                        {
                            ret.add(ctor);
                        }
                    }
                }
                catch (ClassNotFoundException e)
                {
                    classesNotLoaded.add(className);
                }
            }
        }
        return ret;
    }

    /**
     * OpenEJB Annotation Information Access
     */
    @Override
    public List<Field> findAnnotatedFields(Class<? extends Annotation> annotation)
    {
        LOG.debug("findAnnotatedFields({})",annotation);
        classesNotLoaded.clear();
        List<Class<?>> seenClasses = new ArrayList<>();
        List<Field> ret = new ArrayList<>();
        String key = annotation.getName();
        List<FieldInfo> hits = annotatedFields.get(key);
        if (hits != null)
        {
            for (FieldInfo info : hits)
            {
                String className = info.getClassInfo().getClassName();
                try
                {
                    Class<?> clazz = loadClassNoInit(className);
                    if (seenClasses.contains(clazz))
                    {
                        continue;
                    }
                    seenClasses.add(clazz);
                    for (Field ctor : clazz.getDeclaredFields())
                    {
                        if (ctor.isAnnotationPresent(annotation))
                        {
                            ret.add(ctor);
                        }
                    }
                }
                catch (ClassNotFoundException e)
                {
                    classesNotLoaded.add(className);
                }
            }
        }
        return ret;
    }

    /**
     * OpenEJB Annotation Information Access
     */
    @Override
    public List<Method> findAnnotatedMethods(Class<? extends Annotation> annotation)
    {
        LOG.debug("findAnnotatedMethods({})",annotation);
        classesNotLoaded.clear();
        List<Class<?>> seenClasses = new ArrayList<>();
        List<Method> ret = new ArrayList<>();
        String key = annotation.getName();
        List<MethodInfo> hits = annotatedMethods.get(key);
        if (hits != null)
        {
            for (MethodInfo info : hits)
            {
                String className = info.getClassInfo().getClassName();
                try
                {
                    Class<?> clazz = loadClassNoInit(className);
                    if (seenClasses.contains(clazz))
                    {
                        continue;
                    }
                    seenClasses.add(clazz);
                    for (Method method : clazz.getDeclaredMethods())
                    {
                        if (method.isAnnotationPresent(annotation))
                        {
                            ret.add(method);
                        }
                    }
                }
                catch (ClassNotFoundException e)
                {
                    classesNotLoaded.add(className);
                }
            }
        }
        return ret;
    }

    /**
     * OpenEJB Annotation Information Access
     */
    @Override
    public List<Package> findAnnotatedPackages(Class<? extends Annotation> annotation)
    {
        LOG.debug("findAnnotatedPackages({}}",annotation);
        throw new UnsupportedOperationException("Not Implemented Yet");
        // TODO Auto-generated method stub
        // return null;
    }

    /**
     * OpenEJB Annotation Information Access
     */
    @Override
    public List<Class<?>> findClassesInPackage(String packageName, boolean recursive)
    {
        LOG.debug("findClassesInPackage({},{})",packageName,recursive);
        throw new UnsupportedOperationException("Not Implemented Yet");
        // TODO Auto-generated method stub
        // return null;
    }

    /**
     * OpenEJB Annotation Information Access
     */
    @Override
    public <T> List<Class<? extends T>> findImplementations(Class<T> clazz)
    {
        LOG.debug("findImplementations({})",clazz);
        throw new UnsupportedOperationException("Not Implemented Yet");
        // TODO Auto-generated method stub
        // return null;
    }

    /**
     * OpenEJB Annotation Information Access
     */
    @Override
    public List<Class<?>> findInheritedAnnotatedClasses(Class<? extends Annotation> annotation)
    {
        // Not the fastest technique, but hey, this should only run at deploy time so ...
        LOG.debug("findInheritedAnnotatedClasses({})",annotation);

        classesNotLoaded.clear();
        List<Class<?>> ret = new LinkedList<>();
        for (ClassInfo info : annotatedClasses.get(annotation.getName()))
        {
            String className = info.getClassName();
            try
            {
                ret.add(loadClassNoInit(className));
            }
            catch (ClassNotFoundException e)
            {
                LOG.debug("[IGNORED] Unable to load {}",className,e);
            }
        }

        for (Class<?> clazz : ret)
        {
            ConcurrentHashSet<String> classSet = classMap.get(clazz.getName());
            if (classSet != null)
            {
                for (String className : classSet)
                {
                    try
                    {
                        Class<?> c = loadClassNoInit(className);
                        if (!ret.contains(c))
                        {
                            ret.add(c);
                        }
                    }
                    catch (ClassNotFoundException e)
                    {
                        classesNotLoaded.add(className);
                    }
                }
            }
        }

        return ret;
    }

    /**
     * OpenEJB Annotation Information Access
     */
    @Override
    public List<Annotated<Class<?>>> findMetaAnnotatedClasses(Class<? extends Annotation> annotation)
    {
        LOG.debug("findMetaAnnotatedClasses({})",annotation);
        return metaAnnotatedClasses;
    }

    /**
     * OpenEJB Annotation Information Access
     */
    @Override
    public List<Annotated<Field>> findMetaAnnotatedFields(Class<? extends Annotation> annotation)
    {
        LOG.debug("findMetaAnnotatedFields({})",annotation);
        return metaAnnotatedFields;
    }

    /**
     * OpenEJB Annotation Information Access
     */
    @Override
    public List<Annotated<Method>> findMetaAnnotatedMethods(Class<? extends Annotation> annotation)
    {
        LOG.debug("findMetaAnnotatedMethods({})",annotation);
        return metaAnnotatedMethods;
    }

    private <T> void findSubclasses(Class<? extends T> clazz, List<Class<? extends T>> hits, Class<? extends T> pc)
    {
        LOG.debug("findSubclasses({},{},{})",clazz,hits.size(),pc);
        ConcurrentHashSet<String> subset = classMap.get(clazz.getName());
        if (CollectionUtils.isEmpty(subset))
        {
            LOG.debug("No subclasses for {}",clazz.getName());
            return;
        }

        for (String className : subset)
        {
            LOG.debug(" - from map: {}",className);
            Class<? extends T> sub = null;
            try
            {
                Class<?> c = loadClassNoInit(className);
                if (pc.isAssignableFrom(c))
                {
                    sub = c.asSubclass(pc);
                    LOG.debug(" - asSubClass: {}",sub);
                    if (!hits.contains(c))
                    {
                        hits.add(sub);
                    }
                }
                else
                {
                    LOG.debug(" - not assignable");
                }
            }
            catch (ClassNotFoundException e)
            {
                classesNotLoaded.add(className);
            }
            if (sub != null)
            {
                findSubclasses(sub,hits,pc);
            }
        }
    }

    /**
     * OpenEJB Annotation Information Access
     */
    @Override
    public <T> List<Class<? extends T>> findSubclasses(Class<T> clazz)
    {
        LOG.debug("findSubclasses({})",clazz);
        Objects.requireNonNull(clazz,"class cannot be null");

        classesNotLoaded.clear();

        List<Class<? extends T>> ret = new ArrayList<>();

        findSubclasses(clazz,ret,clazz);

        return ret;
    }

    /**
     * OpenEJB Annotation Information Access
     */
    @Override
    public List<String> getAnnotatedClassNames()
    {
        LOG.debug("getAnnotatedClassNames()");

        throw new UnsupportedOperationException("Not Implemented Yet");
        // TODO Auto-generated method stub
        // return null;
    }

    /**
     * OpenEJB Annotation Information Access
     */
    @Override
    public List<String> getClassesNotLoaded()
    {
        LOG.debug("getClassesNotLoaded() = {} entries",classesNotLoaded.size());
        return classesNotLoaded;
    }

    @Override
    public void handle(ClassInfo classInfo)
    {
        try
        {
            for (int i = 0; (classInfo.getInterfaces() != null) && (i < classInfo.getInterfaces().length); i++)
            {
                addToInheritanceMap(classInfo.getInterfaces()[i],classInfo.getClassName());
                // _inheritanceMap.add (classInfo.getInterfaces()[i], classInfo.getClassName());
            }
            // To save memory, we don't record classes that only extend Object, as that can be assumed
            if (!"java.lang.Object".equals(classInfo.getSuperName()))
            {
                addToInheritanceMap(classInfo.getSuperName(),classInfo.getClassName());
                // _inheritanceMap.add(classInfo.getSuperName(), classInfo.getClassName());
            }
        }
        catch (Exception e)
        {
            LOG.warn(e);
        }
    }

    /**
     * Jetty Annotation Handler
     */
    @Override
    public void handle(ClassInfo info, String annotationName)
    {
        if (isNotHandled(annotationName))
        {
            return;
        }

        LOG.debug("@{} on class <{}>",annotationName,info.getClassName());

        List<ClassInfo> coll = annotatedClasses.get(annotationName);
        if (coll == null)
        {
            coll = new ArrayList<>();
        }
        coll.add(info);
        annotatedClasses.put(annotationName,coll);
        super.handle(info,annotationName);
    }

    /**
     * Jetty Annotation Handler
     */
    @Override
    public void handle(FieldInfo info, String annotationName)
    {
        if (isNotHandled(annotationName))
        {
            return;
        }

        LOG.debug("@{} on field <{}.{}>",annotationName,info.getClassInfo().getClassName(),info.getSignature());

        List<FieldInfo> coll = annotatedFields.get(annotationName);
        if (coll == null)
        {
            coll = new ArrayList<>();
        }
        coll.add(info);
        annotatedFields.put(annotationName,coll);
        super.handle(info,annotationName);
    }

    /**
     * Jetty Annotation Handler
     */
    @Override
    public void handle(MethodInfo info, String annotationName)
    {
        if (isNotHandled(annotationName))
        {
            return;
        }

        LOG.debug("@{} on method <{}.{}>",annotationName,info.getClassInfo().getClassName(),info.getSignature());

        List<MethodInfo> coll = annotatedMethods.get(annotationName);
        if (coll == null)
        {
            coll = new ArrayList<>();
        }
        coll.add(info);
        annotatedMethods.put(annotationName,coll);
        super.handle(info,annotationName);
    }

    /**
     * OpenEJB Annotation Information Access
     */
    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotation)
    {
        LOG.debug("isAnnotationPresent({})",annotation);
        String key = annotation.getName();

        if (annotatedClasses.containsKey(key))
        {
            return true;
        }

        if (annotatedMethods.containsKey(key))
        {
            return true;
        }
        if (annotatedFields.containsKey(key))
        {
            return true;
        }
        return false;
    }

    private boolean isNotHandled(String annotationName)
    {
        return (annotationName == null) || UNHANDLED_ANNOTATIONS.contains(annotationName);
    }

    private Class<?> loadClassNoInit(String className) throws ClassNotFoundException
    {
        return Class.forName(className,false,contextClassLoader);
    }

    public void onScanComplete()
    {
        LOG.debug("onScanComplete()");
    }
}
