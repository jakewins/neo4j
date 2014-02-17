package org.neo4j.kernel.codegen;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;

public class MethodDescriptor
{
    final String methodName;
    final List<String> argTypes = new ArrayList<>();

    public MethodDescriptor( Method method )
    {
        methodName = method.getName();
        for ( Class<?> aClass : method.getParameterTypes() )
        {
            String[] parts = aClass.getName().split( "\\." );
            argTypes.add(parts[parts.length - 1].replace( "$", "." ));
        }
    }

    public MethodDescriptor( MethodDeclaration node )
    {
        methodName = node.getName().toString();
        for ( Object o : node.parameters() )
        {
            argTypes.add( ((SingleVariableDeclaration) o).getType().toString() );
        }
    }

    public MethodDescriptor( MethodInvocation node, Map<String, Type> varsInScope )
    {
        methodName = node.getName().toString();
        for ( Object o : node.arguments() )
        {
            argTypes.add( varsInScope.get(o.toString()).toString() );
        }
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        MethodDescriptor that = (MethodDescriptor) o;

        if ( !argTypes.equals( that.argTypes ) )
        {
            return false;
        }
        if ( !methodName.equals( that.methodName ) )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = methodName.hashCode();
        result = 31 * result + argTypes.hashCode();
        return result;
    }

    @Override
    public String toString()
    {
        return "MethodDescriptor{" + methodName  + argTypes + '}';
    }
}
