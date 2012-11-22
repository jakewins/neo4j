/**
 * Copyright (c) 2002-2012 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.info;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class Monitors
{
    public interface Monitor
    {
        void monitorCreated( Class<?> monitorClass, String... tags );

        void monitorListenerException( Throwable throwable );

        public class Adapter
                implements Monitor
        {
            @Override
            public void monitorCreated( Class<?> monitorClass, String... tags )
            {
            }

            @Override
            public void monitorListenerException( Throwable throwable )
            {
            }
        }
    }

    private AtomicReference<Map<Method, List<MonitorListenerInvocationHandler>>> methodMonitorListeners = new
            AtomicReference<Map<Method, List<MonitorListenerInvocationHandler>>>( new HashMap<Method,
            List<MonitorListenerInvocationHandler>>() );
    private List<Class<?>> monitoredInterfaces = new ArrayList<Class<?>>();
    private Map<Specification<Method>, MonitorListenerInvocationHandler> monitorListeners = new ConcurrentHashMap<Specification<Method>, MonitorListenerInvocationHandler>();

    private Monitor monitorsMonitor;

    public Monitors()
    {
        monitorsMonitor = newMonitor( Monitor.class );
    }

    public <T> T newMonitor( Class<T> monitorClass, String... tags )
    {
        if ( !monitoredInterfaces.contains( monitorClass ) )
        {
            monitoredInterfaces.add( monitorClass );

            for ( Method method : monitorClass.getMethods() )
            {
                recalculateMethodListeners( method );
            }
        }

        ClassLoader classLoader = monitorClass.getClassLoader();
        MonitorInvocationHandler monitorInvocationHandler = new MonitorInvocationHandler( tags );
        try
        {
            return monitorClass.cast( Proxy.newProxyInstance( classLoader, new Class<?>[]{monitorClass},
                    monitorInvocationHandler ) );
        }
        finally
        {
            if ( monitorsMonitor != null )
            {
                monitorsMonitor.monitorCreated( monitorClass, tags );
            }
        }
    }

    public void addMonitorListener( final Object monitorListener, String... tags )
    {
        MonitorListenerInvocationHandler monitorListenerInvocationHandler = tags.length == 0 ? new
                UntaggedMonitorListenerInvocationHandler( monitorListener ) :
                new TaggedMonitorListenerInvocationHandler( monitorListener );
        for ( Class<?> monitorInterface : getInterfacesOf( monitorListener.getClass() ) )
        {
            for ( final Method method : monitorInterface.getMethods() )
            {
                monitorListeners.put( new Specification<Method>()
                {
                    @Override
                    public boolean satisfiedBy( Method item )
                    {
                        return method.equals( item );
                    }
                }, monitorListenerInvocationHandler );

                recalculateMethodListeners( method );
            }
        }
    }

    public void removeMonitorListener( Object monitorListener )
    {
        Iterator<Map.Entry<Specification<Method>, MonitorListenerInvocationHandler>> iter = monitorListeners.entrySet
                ().iterator();
        while ( iter.hasNext() )
        {
            Map.Entry<Specification<Method>, MonitorListenerInvocationHandler> handlerEntry = iter.next();
            MonitorListenerInvocationHandler monitorListenerInvocationHandler = handlerEntry.getValue();
            if ( monitorListenerInvocationHandler instanceof UntaggedMonitorListenerInvocationHandler )
            {
                UntaggedMonitorListenerInvocationHandler invocationHandler =
                        (UntaggedMonitorListenerInvocationHandler) monitorListenerInvocationHandler;
                if ( invocationHandler.getMonitorListener() == monitorListener )
                {
                    iter.remove();
                }
            }
        }

        recalculateAllMethodListeners();
    }

    public void addMonitorListener( MonitorListenerInvocationHandler invocationHandler,
                                    Specification<Method> methodSpecification )
    {
        monitorListeners.put( methodSpecification, invocationHandler );

        recalculateAllMethodListeners();
    }

    public void removeMonitorListener( MonitorListenerInvocationHandler invocationHandler )
    {
        Iterator<Map.Entry<Specification<Method>, MonitorListenerInvocationHandler>> iter = monitorListeners.entrySet
                ().iterator();
        while ( iter.hasNext() )
        {
            Map.Entry<Specification<Method>, MonitorListenerInvocationHandler> handlerEntry = iter.next();
            if ( handlerEntry.getValue() == invocationHandler )
            {
                iter.remove();
                recalculateAllMethodListeners();
                return;
            }
        }
    }

    private void recalculateMethodListeners( Method method )
    {
        List<MonitorListenerInvocationHandler> listeners = new ArrayList<MonitorListenerInvocationHandler>();
        for ( Map.Entry<Specification<Method>, MonitorListenerInvocationHandler> handlerEntry : monitorListeners
                .entrySet() )
        {
            if ( handlerEntry.getKey().satisfiedBy( method ) )
            {
                listeners.add( handlerEntry.getValue() );
            }
        }
        methodMonitorListeners.get().put( method, listeners );
    }

    private void recalculateAllMethodListeners()
    {
        for ( Method method : methodMonitorListeners.get().keySet() )
        {
            recalculateMethodListeners( method );
        }
    }

    private Iterable<Class<?>> getInterfacesOf( Class<?> aClass )
    {
        List<Class<?>> interfaces = new ArrayList<Class<?>>();
        while ( aClass != null )
        {
            Collections.addAll( interfaces, aClass.getInterfaces() );
            aClass = aClass.getSuperclass();
        }
        return interfaces;
    }

    private static class UntaggedMonitorListenerInvocationHandler implements MonitorListenerInvocationHandler
    {
        private final Object monitorListener;

        public UntaggedMonitorListenerInvocationHandler( Object monitorListener )
        {
            this.monitorListener = monitorListener;
        }

        public Object getMonitorListener()
        {
            return monitorListener;
        }

        @Override
        public void invoke( Object proxy, Method method, Object[] args, String... tags )
                throws Throwable
        {
            method.invoke( monitorListener, args );
        }
    }

    private static class TaggedMonitorListenerInvocationHandler
            extends UntaggedMonitorListenerInvocationHandler
    {
        private String[] tags;

        public TaggedMonitorListenerInvocationHandler( Object monitorListener, String... tags )
        {
            super( monitorListener );
            this.tags = tags;
        }

        @Override
        public void invoke( Object proxy, Method method, Object[] args, String... tags )
                throws Throwable
        {
            required:
            for ( String requiredTag : this.tags )
            {
                for ( String tag : tags )
                {
                    if ( requiredTag.equals( tag ) )
                    {
                        continue required;
                    }
                }
                return; // Not all required tags present
            }

            super.invoke( proxy, method, args, tags );
        }
    }


    private class MonitorInvocationHandler implements InvocationHandler
    {
        private String[] tags;

        public MonitorInvocationHandler( String... tags )
        {
            this.tags = tags;
        }

        @Override
        public Object invoke( Object proxy, Method method, Object[] args ) throws Throwable
        {
            invokeMonitorListeners( proxy, method, args );
            return null;
        }

        private void invokeMonitorListeners( Object proxy, Method method, Object[] args )
        {
            List<MonitorListenerInvocationHandler> handlers = methodMonitorListeners.get().get( method );
            if ( handlers != null )
            {
                for ( MonitorListenerInvocationHandler monitorListenerInvocationHandler : handlers )
                {
                    try
                    {
                        monitorListenerInvocationHandler.invoke( proxy, method, args, tags );
                    }
                    catch ( Throwable e )
                    {
                        if ( !method.getDeclaringClass().equals( Monitor.class ) )
                        {
                            monitorsMonitor.monitorListenerException( e );
                        }
                    }
                }
            }
        }
    }
}
