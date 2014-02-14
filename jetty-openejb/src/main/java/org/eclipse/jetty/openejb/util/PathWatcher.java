//
//  ========================================================================
//  Copyright (c) 1995-2014 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.openejb.util;

import static java.nio.file.StandardWatchEventKinds.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

/**
 * Basic PathWatcher thread for getting events on changes in paths.
 */
public class PathWatcher extends ContainerLifeCycle implements Runnable
{
    public static class Config
    {
        protected final Path dir;
        protected List<Pattern> includes;
        protected List<Pattern> excludes;

        public Config(File path)
        {
            this(path.toPath());
        }

        public Config(Path path)
        {
            this.dir = path;
            includes = new ArrayList<>();
            excludes = new ArrayList<>();
        }

        public void addExclude(Pattern pattern)
        {
            this.excludes.add(pattern);
        }

        public void addExclude(String regex)
        {
            addExclude(Pattern.compile(regex));
        }

        public void addExcludes(List<String> regexes)
        {
            for (String regex : regexes)
            {
                addExclude(regex);
            }
        }

        public void addInclude(Pattern pattern)
        {
            this.includes.add(pattern);
        }

        public void addInclude(String regex)
        {
            addInclude(Pattern.compile(regex));
        }

        public void addIncludse(List<String> regexes)
        {
            for (String regex : regexes)
            {
                addInclude(regex);
            }
        }

        public Config asSubConfig(Path dir)
        {
            Config subconfig = new Config(dir);
            subconfig.includes = this.includes;
            subconfig.excludes = this.excludes;
            return subconfig;
        }

        private boolean hasMatch(String fullpath, List<Pattern> patterns)
        {
            for (Pattern pat : patterns)
            {
                if (pat.matcher(fullpath).matches())
                {
                    return true;
                }
            }
            return false;
        }

        public boolean isExcluded(Path dir)
        {
            if (excludes.isEmpty())
            {
                // no excludes == everything allowed
                return false;
            }
            // Use 'unixy' path separators
            String fullpath = unixy(dir.normalize());
            return hasMatch(fullpath,excludes);
        }

        public boolean isIncluded(Path dir)
        {
            if (includes.isEmpty())
            {
                // no includes == everything allowed
                return true;
            }
            // Use 'unixy' path separators
            String fullpath = unixy(dir.normalize());
            return hasMatch(fullpath,includes);
        }

        private String unixy(Path path)
        {
            String fullpath = path.toString();
            return fullpath.replaceAll("\\\\","/");
        }
    }

    public static interface Listener
    {
        void onPathAdded(Path path);

        void onPathRemoved(Path path);

        void onPathUpdated(Path path);
    }

    private static final Logger LOG = Log.getLogger(PathWatcher.class);

    @SuppressWarnings("unchecked")
    private static <T> WatchEvent<T> cast(WatchEvent<?> event)
    {
        return (WatchEvent<T>)event;
    }

    private final WatchService watcher;
    private List<Listener> listeners = new ArrayList<>();
    private Map<WatchKey, Config> keys = new HashMap<>();
    private Thread thread;

    public PathWatcher() throws IOException
    {
        this.watcher = FileSystems.getDefault().newWatchService();
    }

    public void addListener(Listener listener)
    {
        listeners.add(listener);
    }

    public void addRoot(final Config root) throws IOException
    {
        Files.walkFileTree(root.dir,new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
            {
                if (Files.isHidden(dir))
                {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                if (root.isExcluded(dir))
                {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                if (root.isIncluded(dir))
                {
                    notifyOnPathAdded(dir);
                    register(dir,root);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
                if (!root.isExcluded(file) && root.isIncluded(file))
                {
                    notifyOnPathAdded(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Override
    public void doStart() throws Exception
    {
        if (thread != null)
        {
            throw new RuntimeException("PathWatcher thread already started!");
        }

        thread = new Thread(this,"PathWatcher@" + Integer.toHexString(hashCode()));
        thread.setDaemon(true);
        thread.start();
        super.doStart();
    }

    @Override
    protected void doStop() throws Exception
    {
        watcher.close();
        super.doStop();
    }

    protected void notifyOnPathAdded(Path path)
    {
        for (Listener listener : listeners)
        {
            try
            {
                listener.onPathAdded(path);
            }
            catch (Throwable t)
            {
                LOG.ignore(t);
            }
        }
    }

    protected void notifyOnPathRemoved(Path path)
    {
        for (Listener listener : listeners)
        {
            try
            {
                listener.onPathRemoved(path);
            }
            catch (Throwable t)
            {
                LOG.ignore(t);
            }
        }
    }

    protected void notifyOnPathUpdated(Path path)
    {
        for (Listener listener : listeners)
        {
            try
            {
                listener.onPathUpdated(path);
            }
            catch (Throwable t)
            {
                LOG.ignore(t);
            }
        }
    }

    private String pathType(Path path)
    {
        if (Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS))
        {
            return "File";
        }
        if (Files.isDirectory(path,LinkOption.NOFOLLOW_LINKS))
        {
            return "Dir";
        }
        if (Files.isSymbolicLink(path))
        {
            return "Symlink";
        }
        return "unknown";
    }

    protected void register(Path dir, Config root) throws IOException
    {
        WatchKey key = dir.register(watcher,ENTRY_CREATE,ENTRY_DELETE,ENTRY_MODIFY);
        keys.put(key,root.asSubConfig(dir));
    }

    @Override
    public void run()
    {
        // Start the java.nio watching
        while (true)
        {
            WatchKey key = null;
            try
            {
                if (LOG.isDebugEnabled())
                {
                    LOG.debug("Taking WatchKey from {}",watcher);
                }
                key = watcher.take();
            }
            catch (InterruptedException e)
            {
                LOG.ignore(e);
                return;
            }

            Config config = keys.get(key);
            if (config == null)
            {
                LOG.warn("WatchKey not recognized: {}",key);
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents())
            {
                @SuppressWarnings("rawtypes")
                WatchEvent.Kind kind = event.kind();

                // Get the path event
                WatchEvent<Path> ev = cast(event);
                Path name = ev.context();
                Path child = config.dir.resolve(name);

                if (kind == ENTRY_CREATE)
                {
                    // handle special case for registering new directories
                    // recursively
                    if (Files.isDirectory(child,LinkOption.NOFOLLOW_LINKS))
                    {
                        try
                        {
                            addRoot(config.asSubConfig(child));
                        }
                        catch (IOException e)
                        {
                            LOG.warn(e);
                        }
                    }
                    else
                    {
                        notifyOnPathAdded(child);
                    }
                }
                else if (kind == ENTRY_DELETE)
                {
                    notifyOnPathRemoved(child);
                }
                else if (kind == ENTRY_MODIFY)
                {
                    notifyOnPathUpdated(child);
                }
                else
                {
                    LOG.warn("Unknown Path Watcher Event [{}] for ({}) {}",event.kind().name(),pathType(child),child);
                }
            }

            if (!key.reset())
            {
                keys.remove(key);
                if (keys.isEmpty())
                {
                    LOG.info("No more paths being watched. PathWatcher stopping.");
                    return; // all done, no longer monitoring anything
                }
            }
        }
    }
}
