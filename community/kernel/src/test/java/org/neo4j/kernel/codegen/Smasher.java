package org.neo4j.kernel.codegen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.neo4j.helpers.collection.Iterables;
import org.neo4j.kernel.impl.api.layer.CacheLayer;
import org.neo4j.kernel.impl.api.layer.ConstraintEnforcingEntityOperations;
import org.neo4j.kernel.impl.api.layer.LockingStatementOperations;
import org.neo4j.kernel.impl.api.layer.SchemaStateConcern;
import org.neo4j.kernel.impl.api.layer.StateHandlingStatementOperations;
import org.neo4j.kernel.impl.api.operations.StatementLayer;
import org.neo4j.kernel.impl.util.FileUtils;

import static java.util.Arrays.asList;
import static org.eclipse.jdt.core.dom.Modifier.ModifierKeyword.FINAL_KEYWORD;
import static org.eclipse.jdt.core.dom.Modifier.ModifierKeyword.PUBLIC_KEYWORD;
import static org.eclipse.jdt.core.dom.TypeDeclaration.NAME_PROPERTY;
import static org.neo4j.helpers.collection.MapUtil.stringMap;
import static org.neo4j.kernel.codegen.ASTType.type;

public class Smasher
{
    public static void main(String ... args) throws Exception
    {
        Generator gen = new Generator(
                new File("/Users/jake/Code/neo4j-root/neo4j-public/community/kernel/src/main/java"),
                new File("/Users/jake/Code/neo4j-root/neo4j-public/community/kernel/src/main/java"),
                "org.neo4j.kernel.impl.api.gen",
                StatementLayer.class );
        gen.stack(
                LockingStatementOperations.class,
                ConstraintEnforcingEntityOperations.class,
                StateHandlingStatementOperations.class,
                SchemaStateConcern.class,
                CacheLayer.class
        ).generate();
    }

    static class Generator
    {
        private final File sourceDir;
        private final File outputDir;
        private final String outputPackage;
        private final Class<?> commonInterface;

        private Class<?>[] layers;

        Generator( File sourceDir, File outputDir, String outputPackage, Class<?> commonInterface )
        {
            this.sourceDir = sourceDir;
            this.outputDir = outputDir;
            this.outputPackage = outputPackage;
            this.commonInterface = commonInterface;
        }

        public Generator stack( Class<?> ... layers )
        {
            this.layers = layers;
            return this;
        }

        public void generate() throws IOException, BadLocationException
        {
            final List<Class<?>> processedLayers = new ArrayList<>();

            final Map<MethodDescriptor, Class<?>> currentDelegates = new HashMap<>();

            for ( final Class<?> layer : Iterables.reverse( asList( layers ) ) )
            {
                final Document document = open( layer );
                final CompilationUnit cu = parse( document );
                final AST ast = cu.getAST();
                final ASTRewrite rewriter = ASTRewrite.create( ast );

                final String generatedClassName = "Generated" + layer.getSimpleName();

                cu.accept( new ASTVisitor()
                {
                    private final ASTRefactor refactor = new ASTRefactor( rewriter, ast );
                    private final ASTBuild build = new ASTBuild( ast );
                    private final Map<MethodDescriptor, Method> requiredMethods = requiredMethods();
                    private final Set<Type> usedTypes = new HashSet<>();
                    private final Set<Type> innerClasses = new HashSet<>();
                    private final Set<Type> imported = new HashSet<>();

                    private Map<String, Type> fieldTypes;
                    private boolean contructorDefined = false;

                    @Override
                    public boolean visit( PackageDeclaration node )
                    {
                        PackageDeclaration pack = ast.newPackageDeclaration();
                        pack.setName( ast.newName( outputPackage ) );
                        rewriter.replace( node, pack, null );
                        return true;
                    }

                    @Override
                    public boolean visit( ImportDeclaration node )
                    {
                        String[] parts = node.getName().getFullyQualifiedName().split( "\\." );
                        imported.add( type(parts[parts.length - 1], ast) );
                        return true;
                    }

                    @Override
                    public boolean visit( TypeDeclaration node )
                    {
                        innerClasses.add( type( node.getName().toString(), ast ));
                        if ( node.getName().toString().equals( layer.getSimpleName() ) )
                        {
                            fieldTypes = build.fieldTypeMap(node);
                            usedTypes.addAll( fieldTypes.values() );
                            refactor.setModifiers( node, PUBLIC_KEYWORD, FINAL_KEYWORD );
                            rewriter.set( node, NAME_PROPERTY, ast.newSimpleName(generatedClassName), null );

                            ListRewrite bodyRewrite = rewriter.getListRewrite( node,
                                    node.getBodyDeclarationsProperty() );
                            for ( Class<?> delegateLayer : processedLayers )
                            {
                                bodyRewrite.insertFirst(
                                        build.newField(
                                                "Generated" + delegateLayer.getSimpleName(),
                                                "__" + delegateLayer.getSimpleName() ), null );
                            }
                        }
                        return true;
                    }

                    @Override
                    public boolean visit( final MethodDeclaration method )
                    {
                        requiredMethods.remove( new MethodDescriptor( method ) );

                        final Map<String, Type> varsInScope = new HashMap<>(fieldTypes);
                        varsInScope.putAll( build.variableTypeMap( method ) );
                        usedTypes.addAll( varsInScope.values() );

                        if ( method.isConstructor() && method.getName().toString().equals( layer.getSimpleName() ) )
                        {
                            contructorDefined = true;
                            rewriter.set( method, MethodDeclaration.NAME_PROPERTY, ast.newSimpleName( generatedClassName ), null );
                            augmentLayerConstructor( method, rewriter, processedLayers, ast );
                        }

                        final Block body = method.getBody();
                        body.accept( new ASTVisitor()
                        {
                            @Override
                            public boolean visit( MethodInvocation node )
                            {
                                Expression expression = node.getExpression();
                                if(expression instanceof MethodInvocation)
                                {
                                    MethodInvocation delegate = (MethodInvocation)expression;
                                    if(delegate.getName().toString().equals( "delegate" ))
                                    {
                                        MethodDescriptor key = new MethodDescriptor( node, varsInScope );
                                        Class<?> delagateLayer = findDelegate( key, method );
                                        rewriter.replace( delegate, build.newFieldAccess( "__" + delagateLayer
                                                .getSimpleName() ), null );
                                    }
                                }
                                return true;
                            }
                        } );
                        currentDelegates.put( new MethodDescriptor(method), layer );

                        return true;
                    }

                    private Class<?> findDelegate( MethodDescriptor methodWeAreDelegatingTo,
                                                   MethodDeclaration methodWeAreDelegatingFrom )
                    {
                        Class<?> delagateLayer = currentDelegates.get( methodWeAreDelegatingTo );
                        if(delagateLayer == null)
                        {
                            throw new IllegalStateException( layer.getSimpleName() +
                                    " requires a delegate layer for " + methodWeAreDelegatingTo + " in " +
                                    new MethodDescriptor( methodWeAreDelegatingFrom ) +
                                    ", but there is no layer below it that implements this method.");
                        }
                        return delagateLayer;
                    }

                    @Override
                    public void endVisit( TypeDeclaration node )
                    {
                        if ( node.getName().toString().equals( layer.getSimpleName() ) )
                        {
                            // Add no-op methods to fulfill parent interface
                            ListRewrite listRewrite = rewriter.getListRewrite( node,
                                    node.getBodyDeclarationsProperty() );
                            for ( Method method : requiredMethods.values() )
                            {
                                listRewrite.insertLast( build.newUnsupportedMethod( method ), null );
                            }

                            // Add a constructor with required delegates, if none exists
                            if ( !contructorDefined )
                            {
                                MethodDeclaration construct = build.newEmptyMethod(generatedClassName, PUBLIC_KEYWORD );
                                construct.setConstructor( true );
                                augmentLayerConstructor( construct, rewriter, processedLayers, ast );

                                listRewrite.insertFirst( construct, null );
                            }
                        }
                    }

                    private void augmentLayerConstructor( MethodDeclaration node, ASTRewrite rewriter, List<Class<?>> processedLayers, AST ast )
                    {
                        ListRewrite params = rewriter.getListRewrite( node,
                                MethodDeclaration.PARAMETERS_PROPERTY );
                        for ( Class<?> processedLayer : processedLayers )
                        {
                            SingleVariableDeclaration vrb = ast.newSingleVariableDeclaration();
                            vrb.setName( ast.newSimpleName( "__" + processedLayer.getSimpleName() ) );
                            vrb.setType( type( "Generated" + processedLayer.getSimpleName(), ast ) );
                            params.insertFirst( vrb, null );

                            Assignment assign = ast.newAssignment();
                            assign.setLeftHandSide( build.newFieldAccess( "__" + processedLayer.getSimpleName() ) );
                            assign.setRightHandSide( ast.newSimpleName( "__" + processedLayer.getSimpleName() )  );

                            ListRewrite body = rewriter.getListRewrite( node.getBody(), Block.STATEMENTS_PROPERTY );
                            body.insertFirst( ast.newExpressionStatement( assign ), null );
                        }
                    }
                } );


                try
                {
                    rewriter.rewriteAST( document, stringMap(
                            DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4",
                            DefaultCodeFormatterConstants.FORMATTER_INDENTATION_SIZE, "4",
                            DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE ) ).apply( document );
                }
                catch ( BadLocationException e )
                {
                    throw new RuntimeException( e );
                }

                processedLayers.add( layer );

                File outputFile = new File(outputDir, outputPackage.replace( ".", File.separator) + File.separator + generatedClassName + ".java");
                FileUtils.writeToFile( outputFile, document.get(), false );
                System.out.println("Generated " + outputFile.getAbsolutePath());
            }
        }

        private Map<MethodDescriptor, Method> requiredMethods()
        {
            Map<MethodDescriptor, Method> requiredMethods = new HashMap<>();
            for ( Method method : commonInterface.getMethods() )
            {
                requiredMethods.put( new MethodDescriptor( method ), method );
            }
            return requiredMethods;
        }

        private CompilationUnit parse( Document document )
        {
            ASTParser parser = ASTParser.newParser( AST.JLS3 );
            parser.setKind( ASTParser.K_COMPILATION_UNIT );
            parser.setSource( document.get().toCharArray() );
            parser.setResolveBindings( true );

            return (CompilationUnit) parser.createAST(null);
        }

        private Document open( Class<?> layer ) throws IOException
        {
            String path = layer.getPackage().getName().replaceAll("\\.", File.separator);
            if(layer.getName().contains( "$" ))
            {
                throw new IllegalArgumentException( "Nested classes are not supported for layer generation, please put " + layer.getName() + " into its own class file." );

            }
            File sourceFile = new File(sourceDir,path + File.separator + layer.getSimpleName() + ".java");

            StringBuilder output = new StringBuilder();
            try(BufferedReader br = new BufferedReader(new FileReader(sourceFile)))
            {
                for(String line; (line = br.readLine()) != null; )
                {
                    output.append( line ).append( "\n" );
                }
            }

            return new Document(output.toString());
        }
    }
}